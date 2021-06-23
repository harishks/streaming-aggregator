package aggregator.operator;

import aggregator.functions.TransformFunc;
import aggregator.functions.WindowAggregateFunc;
import aggregator.source.TimestampEvent;
import aggregator.watermark.Watermark;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


/**
 * WindowOperator is a stateful operator that stores the window states in a balanced BST ordered by
 * window event timestamp. The idea is to efficiently lookup the BST while materializing the results upon
 * receiving the watermark.
 *
 * NOTE: We might want to support user-defined {@code Triggers} and {@code Timer} functionality in WindowOperator.
 */
public class WindowOperator<T extends TimestampEvent, W, V> extends Operator<T, V> {
  private final Duration _duration;
  private final Map<W, NavigableMap<Long, V>> _opWindowState;
  private final TransformFunc<T, W> _keyFn;
  private final WindowAggregateFunc<W, V> _valueFunc;
  private long maxEventTimestamp = Long.MIN_VALUE;

  public WindowOperator(Duration duration, TransformFunc<T, W> keyFn, WindowAggregateFunc<W, V> valueFunc) {
    this._duration = duration;
    this._keyFn = keyFn;
    this._valueFunc = valueFunc;
    this._opWindowState = new HashMap<>();
  }

  @Override
  protected CompletionStage<Collection<V>> handleEvent(T event) {
    try {
      maxEventTimestamp = Math.max(maxEventTimestamp, event.eventTimestamp());

      /* Discard late arrivals */
      if (isLateArrival(event.eventTimestamp())) {
        return CompletableFuture.completedFuture(Collections.emptyList());
      }

      Collection<W> keyCollection = _keyFn.apply(event);

      if (keyCollection.size() > 1 || keyCollection.stream().findFirst().isEmpty()) {
        System.out.println("Invalid Key");
        throw new Exception("Invalid Window Key");
      }

      W key = keyCollection.stream().findFirst().get();

      _opWindowState.compute(key, (w, map) -> {
        long windowTimestamp = getWindowTimestamp(event.eventTimestamp());
        NavigableMap<Long, V> sortedMap = map;
        if (sortedMap == null) {
          //System.out.println("Creating a new Map for key " + w);
          sortedMap = new TreeMap<>();
        }
        if (sortedMap.containsKey(windowTimestamp)) {
          V val = sortedMap.get(windowTimestamp);
          _valueFunc.aggregate(val);
        } else {
          sortedMap.put(windowTimestamp, _valueFunc.supply(key));
        }
        return sortedMap;
      });
    } catch (Exception e) {
      System.err.println("Failed to process event in Window Op" + e.getMessage());
    }
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public long getMaxEventTimestamp() {
    return maxEventTimestamp;
  }

  @Override
  public void handleWatermark(Watermark watermark) {
    List<V> results = new ArrayList<>();
    try {
      for (Map.Entry<W, NavigableMap<Long, V>> windowEntry : _opWindowState.entrySet()) {
        long watermarkTimestamp = watermark.getTimestamp();
        NavigableMap<Long, V> navigableMap = windowEntry.getValue();

        SortedMap<Long, V> headMap = navigableMap.headMap(watermarkTimestamp);

        Iterator<Map.Entry<Long, V>> iterator = headMap.entrySet().iterator();

        while (iterator.hasNext()) {
          Map.Entry<Long, V> entry = iterator.next();
          results.add(entry.getValue());
          iterator.remove();
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to handle Watermark in WindowOp " + e.getMessage());
    }

    CompletableFuture.allOf(results.stream()
          .flatMap(r -> getNextOperatorSet().stream().map(op -> op.onEvent(r)))
          .toArray(CompletableFuture[]::new));
  }

  /**
   * Bucketize the events to the nearest floor timestamp depending on the window size.
   * @param timestamp input event timestamp.
   * @return bucketized event timestamp.
   */
  private long getWindowTimestamp(long timestamp) {
    return Math.floorDiv(timestamp, _duration.toMillis());
  }

  private boolean isLateArrival(long timestamp) {
    if (timestamp < getWaterMark()) {
      System.out.println("Late Arrival - event timestamp " + timestamp + " Watermark " + getWaterMark());
    }
    return timestamp < getWaterMark();
  }
}
