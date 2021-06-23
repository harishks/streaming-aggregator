package aggregator.source;

/**
 * Represents streaming events that are associated with a corresponding offset in the source.
 * <p>A partition in a stream is assumed to be totally ordered sequence of records and that each record is
 * associated  with a monotonically increasing sequence number or identifier called offsets.
 * Offsets benefit the processors to checkpoint the state of the system, and help the source determine the
 * system lag by comparing the latest offset in the partition/stream and the latest consumed offset at the processors.</p>
 */
public interface StreamOffset {
  /**
   * Offset of the underlying stream event.
   * @return offset.
   */
  long eventOffset();
}
