package aggregator.watermark;

/**
 * Watermarks help upstream stateful operators to decide on when to materialize the results.
 * <p>Watermarks are expected to be emitted by the streaming source, which would then be propagated down the
 * operator chain. Assumption here is that events with timestamp lesser than the watermark should not be seen in the system.
 * Those events that show up whose event times are before the watermark are considered "late" events, and the policy
 * enforced by the operators dictate the handling logic for such late arrivals.</p>
 */
public class Watermark {
  private final long timestamp;

  public Watermark(long timestamp) {
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
