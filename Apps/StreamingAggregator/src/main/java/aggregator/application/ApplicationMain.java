package aggregator.application;

import aggregator.HashPartitioner;
import aggregator.LifecycleAware;
import aggregator.Partitioner;
import aggregator.codec.TimestampedStreamEvent;
import aggregator.functions.PartitionKeyExtractorFunc;
import aggregator.source.HttpSource;
import aggregator.source.StreamEvent;
import aggregator.source.UnPartitionedStreamSource;
import aggregator.taskmgmt.JobExecutionEngine;


/**
 * Simple streaming application backed by a local stream processing engine capable of supporting common
 * operators like filter, map, and windowing.
 *
 * The backing engine can support streaming events from HTTP source, and support buffered writes to console sink.
 */
public class ApplicationMain implements LifecycleAware {

  private static final String _streamSource = "https://tweet-service.herokuapp.com/sps";
  private UnPartitionedStreamSource<StreamEvent<String>> _unPartitionedStreamSource;
  private LifecycleState _lifecycleState;

  public static void main(String[] args) throws Exception {
    ApplicationMain applicationMain = new ApplicationMain();
    applicationMain.start();
    Runtime.getRuntime().addShutdownHook(new Thread(applicationMain::stop));
  }

  @Override
  public void start() throws Exception {
    _lifecycleState = LifecycleState.START;

    Partitioner<StreamEvent<String>> streamEventPartitioner =
        new HashPartitioner<>(4, new PartitionKeyExtractorFunc());

    this._unPartitionedStreamSource =
        new HttpSource(_streamSource, streamEventPartitioner);

    StreamsPerSecJobBuilder aggregationJob = new StreamsPerSecJobBuilder(_unPartitionedStreamSource);

    JobExecutionEngine<StreamEvent<String>, TimestampedStreamEvent> executionEngine =
        new JobExecutionEngine<>(aggregationJob, _unPartitionedStreamSource);

    executionEngine.start();
  }

  @Override
  public void stop() {
    _lifecycleState = LifecycleState.STOP;
    try {
      if (_unPartitionedStreamSource != null) {
        _unPartitionedStreamSource.close();
      }
    } catch (Exception e) {
      System.out.println("Failed to close the source");
    }
  }

  @Override
  public LifecycleState getLifecycleState() {
    return _lifecycleState;
  }
}
