package aggregator.operator;

import aggregator.functions.FilterFunc;
import aggregator.source.TimestampEvent;
import aggregator.watermark.Watermark;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


/**
 * Filter operator allow users to submit user-defined filters to operate on
 * incoming data in the stream.
 */
public class FilterOperator<T extends TimestampEvent> extends Operator<T, T> {
  private final FilterFunc<T> _filterFunc;
  private long maxEventTimestamp = 0;

  public FilterOperator(FilterFunc<T> filterFunc) {
    this._filterFunc = filterFunc;
  }

  @Override
  public CompletionStage<Collection<T>> handleEvent(T event) {
    maxEventTimestamp = Math.max(maxEventTimestamp, event.eventTimestamp());
    if (_filterFunc.apply(event)) {
      return CompletableFuture.completedFuture(Collections.singletonList(event));
    }
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public long getMaxEventTimestamp() {
    return maxEventTimestamp;
  }

  @Override
  public void handleWatermark(Watermark watermark) {
    /* No-Op */
  }
}
