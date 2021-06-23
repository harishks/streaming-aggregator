package aggregator.taskmgmt;

import aggregator.pipeline.Pipeline;
import aggregator.source.TimestampEvent;
import aggregator.watermark.Watermark;
import java.util.concurrent.CompletionStage;


/**
 * A {@link Task} represents an independent unit of execution for a given {@link Pipeline}. Note that, a single pipeline
 * can be executed by multiple tasks in the system. In general, the source is expected to be partitioned, and each task
 * can be assigned to operate on a single partition of the streaming source.
 */
public class Task<T extends TimestampEvent, U> {
  private final Pipeline<U, T> _pipeline;
  private String _jobName;

  public Task(Pipeline<U, T> pipeline) {
    this._pipeline = pipeline;
  }

  public CompletionStage<Void> processMsg(U event) {
    return _pipeline.getRootOperator().onEvent(event);
  }

  public CompletionStage<Void> processWatermark(Watermark watermark) {
    return _pipeline.getRootOperator().onWatermark(watermark);
  }

  public void init(String jobName) {
    try {
      this._jobName = jobName;
      _pipeline.getSource().init();
    } catch (Exception e) {
      System.err.println("Failed to initialize Task for Job " + jobName);
    }
  }

  /**
   * Max event timestamp seen thus far by the task.
   */
  public long getMaxEventTimestamp() {
    return _pipeline.getRootOperator().getMaxEventTimestamp();
  }
}
