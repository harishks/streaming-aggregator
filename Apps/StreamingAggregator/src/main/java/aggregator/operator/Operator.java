package aggregator.operator;

import aggregator.sink.ExternalSinkOperator;
import aggregator.watermark.Watermark;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;


/**
 * An operator is a transformation of one or many streams to another stream.
 * Operators can fall into multiple categories:
 * <li>Simple one-to-one operators like map, filter, window </li>
 * <li>one-to-many operator that can split the input stream into 2 or more output streams</li>
 * <li>many-to-one operator that can merge multiple streams into 1 stream output like join</li>
 *
 * Base operator impl that can be extended by subclasses to define concrete operator functionalities.
 */
public abstract class Operator<I, O> {
  private final AtomicLong eventsProcessed;
  private final Set<Operator<O, ?>> _nextOperatorSet; // Ordered set of operators down the chain
  private final Set<Operator<?, I>> _prevOperatorSet; // Ordered set of operators up the chain
  private long currentWatermark = 0;

  public Operator() {
    this._nextOperatorSet = new LinkedHashSet<>();
    this._prevOperatorSet = new LinkedHashSet<>();
    this.eventsProcessed = new AtomicLong(0);
  }

  public void registerNextOp(Operator<O, ?> operator) {
    _nextOperatorSet.add(operator);
  }

  public void registerPrevOp(Operator<?, I> operator) {
    _prevOperatorSet.add(operator);
  }

  /**
   * Currently, the event processing logic is synchronous in nature. The underlying impl can be changed to use an
   * asynchronous processing logic.
   */
  public CompletionStage<Void> onEvent(I event) {
    try {
      eventsProcessed.incrementAndGet();
      CompletionStage<Collection<O>> completableResults = handleEvent(event);

      if (!getNextOperatorSet().isEmpty()) {
        return completableResults.thenCompose(results -> CompletableFuture.allOf(
            results.stream().flatMap(r -> this._nextOperatorSet.stream().map(op -> op.onEvent(r))).toArray(CompletableFuture[]::new)));
      } else {
        return CompletableFuture.completedFuture(null);
      }
    } catch (Exception e) {
      return CompletableFuture.completedFuture(null);
    }
  }

  public long numEventsProcessed() {
    return eventsProcessed.get();
  }

  /**
   * Handle each incoming event that passes through the pipeline. Concrete operators can define their
   * behaviour by running their specialized operator defined functions on the incoming event.
   */
  protected abstract CompletionStage<Collection<O>> handleEvent(I event);

  public CompletionStage<Void> onWatermark(Watermark watermark) {
    final long inputWatermarkMin;

    if (_prevOperatorSet.isEmpty()) {
      // for input operator, use the watermark time coming from the source input
      inputWatermarkMin = watermark.getTimestamp();
    } else {
      inputWatermarkMin = _prevOperatorSet.stream().map(Operator::getWaterMark).min(Long::compare).get();
    }

    CompletionStage<Void> watermarkFuture = CompletableFuture.completedFuture(null);
    if (currentWatermark < inputWatermarkMin) {
      /* advance the watermarkTime of this operator */
      currentWatermark = inputWatermarkMin;

      handleWatermark(new Watermark(currentWatermark));
      if (!getNextOperatorSet().isEmpty()) {
        watermarkFuture = CompletableFuture.allOf(
            getNextOperatorSet().stream().map(op ->
                op.onWatermark(new Watermark(currentWatermark))).toArray(CompletableFuture[]::new));
      }
    }
    return watermarkFuture;
  }

  public long getWaterMark() {
    return currentWatermark;
  }

  public void registerExternalSink(ExternalSinkOperator<O> externalSinkOperator) {
    externalSinkOperator.init();
    _nextOperatorSet.add(externalSinkOperator);
  }

  public Set<Operator<O, ?>> getNextOperatorSet() {
    return _nextOperatorSet;
  }

  /**
   * Maximum event timestamp seen by this operator during its lifecycle.
   */
  public abstract long getMaxEventTimestamp();

  /**
   * Stateful operators can use the watermark to materialize states and emit the results to downstream operators in the chain.
   */
  public abstract void handleWatermark(Watermark watermark);
}
