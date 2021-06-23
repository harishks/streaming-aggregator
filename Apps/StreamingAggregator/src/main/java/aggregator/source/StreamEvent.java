package aggregator.source;

/**
 * Stream event that tracks the underlying offset from the streaming source along with the data element from the stream.
 * <p>Offset backed streams can help in efficient checkpointing and state-management upon restarts and task failures</p>
 */
public class StreamEvent<T> implements StreamOffset {
  private final T event;
  private final long offset;

  public StreamEvent(T event, long offset) {
    this.event = event;
    this.offset = offset;
  }

  public T data() {
    return event;
  }

  @Override
  public long eventOffset() {
    return offset;
  }
}
