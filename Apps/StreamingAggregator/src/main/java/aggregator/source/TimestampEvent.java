package aggregator.source;

/**
 * Timestamped event that can be used by processors to bucketize events based on event timestamp.
 */
public interface TimestampEvent {
  /**
   * Event timestamp.
   * @return timestamp.
   */
  long eventTimestamp();
}