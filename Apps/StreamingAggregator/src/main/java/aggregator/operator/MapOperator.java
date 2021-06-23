package aggregator.operator;

import aggregator.functions.TransformFunc;
import aggregator.watermark.Watermark;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


/**
 * Map operator allows user-defined transformation logic to be defined on each of the incoming
 * data in the stream.
 */
public class MapOperator<I, O> extends Operator<I, O> {
  private final TransformFunc<I, O> _mapperFunc;

  public MapOperator(TransformFunc<I, O> mapFunc) {
    this._mapperFunc = mapFunc;
  }

  @Override
  protected CompletionStage<Collection<O>> handleEvent(I event) {
    try {
      return CompletableFuture.completedFuture(_mapperFunc.apply(event));
    } catch (Exception e) {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
  }

  @Override
  public long getMaxEventTimestamp() {
    return 0;
  }

  @Override
  public void handleWatermark(Watermark watermark) {

  }
}
