package aggregator.operator;

import aggregator.functions.TransformFunc;
import aggregator.source.TimestampEvent;
import aggregator.watermark.Watermark;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Root operator that operates directly on the source of the events. Users can supply a custom deserializer
 * in the form of {@link TransformFunc}, which can map the wire data to a deserialized request that can be used
 * by the upper layers of the operator chain in the pipeline.
 */
public class InputOperator<I, O extends TimestampEvent> extends Operator<I, O> {
  private final TransformFunc<I, O> _transformFn;
  private final AtomicBoolean _processingData; // Checks if the operator is active
  private final AtomicLong _failedEvents;
  private long maxEventTimestamp = -1;

  public InputOperator(TransformFunc<I, O> transformFn) {
    this._transformFn = transformFn;
    this._processingData = new AtomicBoolean(false);
    this._failedEvents = new AtomicLong(0);
  }

  @Override
  protected CompletionStage<Collection<O>> handleEvent(I event) {
    _processingData.set(true);
    try {
      Collection<O> timestampEvent = this._transformFn.apply(event);
      for (TimestampEvent ev : timestampEvent) {
        maxEventTimestamp = Math.max(maxEventTimestamp, ev.eventTimestamp());
      }
      return CompletableFuture.completedFuture(timestampEvent);
    } catch (Exception e) {
      /* Emit these metrics to external monitoring systems */
      _failedEvents.incrementAndGet();
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
  }

  @Override
  public long getMaxEventTimestamp() {
    return _processingData.get() ? maxEventTimestamp : 0;
  }

  @Override
  public void handleWatermark(Watermark watermark) {
    // No-op
  }
}
