package aggregator.functions;

import aggregator.codec.TimestampedStreamEvent;
import aggregator.source.StreamEvent;
import aggregator.models.Event;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collection;
import java.util.Collections;


/**
 * Custom parser to parse the incoming string events from the HTTP source.
 */
public class StreamEventParser implements TransformFunc<StreamEvent<String>, TimestampedStreamEvent> {
  private final Gson gson;

  public StreamEventParser() {
    this.gson = new GsonBuilder().create();
  }

  @Override
  public Collection<TimestampedStreamEvent> apply(StreamEvent<String> input) throws Exception {
    try {
      String inputLine = input.data();

      if (inputLine.length() == 0) {
        return Collections.emptyList();
      }

      /* Actual data starts at offset 6 */
      if (inputLine.length() > 6) {
        String data = inputLine.substring(6);
        Event msg = gson.fromJson(data, Event.class);

        /* Input data seems to have "title" values as "busted" that looks like malformed data and
         * can be ignored during the processing pipeline */
        if (msg.getTitle().startsWith("busted data")) {
          throw new ProcessorException("Malformed Event");
        }

        StreamEvent<Event> streamEvent = new StreamEvent<>(msg, input.eventOffset());
        return Collections.singletonList(new TimestampedStreamEvent(streamEvent));
      }
    } catch (Exception e) {
      throw new ProcessorException(e);
    }
    return Collections.emptyList();
  }
}
