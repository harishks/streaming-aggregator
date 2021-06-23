package aggregator.codec;

import aggregator.models.Event;
import aggregator.source.StreamEvent;
import aggregator.source.TimestampEvent;


/**
 * Augmented incoming stream event with the event timestamp.
 */
public class TimestampedStreamEvent implements TimestampEvent {
  private final StreamEvent<Event> _event;

  public TimestampedStreamEvent(StreamEvent<Event> event) {
    this._event = event;
  }

  @Override
  public long eventTimestamp() {
    return _event.data().getTime();
  }

  public Event getData() {
    return _event.data();
  }
}
