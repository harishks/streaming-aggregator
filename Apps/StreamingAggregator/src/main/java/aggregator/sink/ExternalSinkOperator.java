package aggregator.sink;

import aggregator.operator.Operator;
import aggregator.watermark.Watermark;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


/**
 * External Sink Operator that can write to external sinks in a batched fashion.
 * <p>As a next step, it would make sense to implement a rate-limited sink, if the write throughput is high,
 * that can throttle writes to external sinks and can introduce backpressure to the source operator.</p>
 */
public abstract class ExternalSinkOperator<T> extends Operator<T, Void> {
  private int elements;

  /**
   * Initialize collections used in accumulating the batch elements.
   */
  public abstract void init();

  @Override
  protected CompletionStage<Collection<Void>> handleEvent(T event) {
    accumulate(event);
    if (++elements == batchSize()) {
      flush();
      elements = 0;
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void handleWatermark(Watermark watermark) {
    /* No-Op */
  }

  /**
   * Invoked upon processing individual elements.
   * <p>Implementing classes can use this method to batch the resulting collection before
   * invoking a flush to transfer the batch to external entities.</p>
   */
  public abstract void accumulate(T event);

  /**
   * Invoked at the end of each batch to flush the entries to external entity.
   */
  public abstract void flush();

  public abstract void cleanup();

  public abstract int batchSize();
}

