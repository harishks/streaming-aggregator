package aggregator.functions;

/**
 * Value aggregation function that allows users to define folding logic on values stored in operator state.
 * For eg., during windowed aggregation, users can supply the logic needed to fold values matching the same key.
 */
public interface WindowAggregateFunc<K, V> {
  /**
   * Folding function to aggregate values that belongs to the same key.
   * @param value previous aggregation value.
   */
  void aggregate(V value);

  /**
   * Supplies the initial value to be used when the operator does not have a state for this {@code Key}.
   * @param key key used to track the state.
   * @return aggregation value.
   */
  V supply(K key);
}
