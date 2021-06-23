package aggregator.taskmgmt;

import aggregator.LifecycleAware;
import aggregator.pipeline.PipelineBuilder;
import aggregator.source.StreamOffset;
import aggregator.source.TimestampEvent;
import aggregator.source.UnPartitionedStreamSource;
import aggregator.source.partition.Partition;
import aggregator.watermark.Watermark;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Task execution engine that is responsible for executing a given streaming data pipeline.
 * <p>Future Work : Ideally, we would want a common execution engine per node or a standalone task coordinator which can
 * operate across nodes to schedule tasks with desired degree of parallelism depending on the number of partitions
 * in the source stream/topic.</p>
 */
public class JobExecutionEngine<U extends StreamOffset, T extends TimestampEvent> implements LifecycleAware {
  private static final int WATERMARK_OFFSET = 5;
  private final List<ExecutorService> _executors;
  private final List<Task<T, U>> _tasks;
  private final PipelineBuilder<U, T> _pipelineBuilder;
  private final int _numPartitions;
  private final ScheduledExecutorService _watermarkEmitter;
  private final UnPartitionedStreamSource<U> _streamSource;
  private LifecycleState _lifecycleState;

  /**
   * Manages the execution of a pipeline.
   */
  public JobExecutionEngine(PipelineBuilder<U, T> pipelineBuilder, UnPartitionedStreamSource<U> source) {
    this._numPartitions = source.numPartitions();
    this._executors = new ArrayList<>(_numPartitions);
    this._tasks = new ArrayList<>(_numPartitions);
    this._pipelineBuilder = pipelineBuilder;
    this._watermarkEmitter = Executors.newSingleThreadScheduledExecutor();
    this._streamSource = source;
  }

  private void init() {
    for (int i = 0; i < _numPartitions; i++) {
      _executors.add(Executors.newSingleThreadExecutor());
      Task<T, U> task = new Task<>(_pipelineBuilder.build());
      _tasks.add(task);
      task.init(_pipelineBuilder.pipelineJobName());
    }
    _watermarkEmitter.scheduleAtFixedRate(this::emitWatermark, 5, 5, TimeUnit.SECONDS);
  }

  private void execute() {
    try {
      while (true) {
        U rawEvent = _streamSource.read();

        /* NOTE: Re-partitioning is NOT a mandatory phase while processing the source data. In a real-world scenario,
         * we would deploy a multistage processing pipeline, wherein there should be a notion of a "Split" phase,
         * which would re-partition the data based on the desired key-based partitioning scheme. This "Split" phase can
         * write the partitioned data into an output stream, which could be used as the source for
         * `StreamsPerSecJobBuilder` job, that relies on data being already partitioned before executing the Pipeline.
         */
        Partition partition = _streamSource.rePartition(rawEvent);
        _executors.get(partition.getPartitionId()).submit(() -> {
          _tasks.get(partition.getPartitionId()).processMsg(rawEvent);
        });
      }
    } catch (Exception e) {
      System.err.println("Failed to execute job" + e);
    }
  }

  @Override
  public void start() throws Exception {
    init();
    execute();
    this._lifecycleState = LifecycleState.START;
  }

  public void stop() {
    /* Might want to send a signal to checkpoint the task state before killing the task */
    for (ExecutorService executorService : _executors) {
      executorService.shutdown();
    }
    this._lifecycleState = LifecycleState.STOP;
  }

  /**
   * While dealing with standard streaming sources, we would want the watermarks to be emitted
   * by the source operator. The role of this task execution engine is to propagate the watermark down the
   * operator chain.
   *
   * <p>NOTE: This is a heuristics based watermark generation scheme, wherein the maxEventTimeStamp from each of the
   * active tasks for this stream is probed, and the minimum value among them is used as an indicator of the event
   * time freshness. The watermark is then a quantity that is {@code WATERMARK_OFFSET} units lesser
   * than the global min of maxEventTimestamp among all the tasks.
   * </p>
   */
  public void emitWatermark() {
    long minEventTimestamp = Long.MAX_VALUE;
    for (Task<T, U> task : _tasks) {
      minEventTimestamp = Math.min(task.getMaxEventTimestamp(), minEventTimestamp);
    }

    final long watermarkTimestamp = minEventTimestamp - WATERMARK_OFFSET;
    for (int i = 0; i < _numPartitions; i++) {
      int index = i;
      _executors.get(index).submit(() -> {
        _tasks.get(index).processWatermark(new Watermark(watermarkTimestamp));
      });
    }
  }

  @Override
  public LifecycleState getLifecycleState() {
    return _lifecycleState;
  }
}
