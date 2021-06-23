package aggregator.functions;

import aggregator.codec.AggregateKey;
import aggregator.models.Event;
import aggregator.source.StreamEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collection;
import java.util.Collections;


/**
 * Generates a partition key from the input raw stream record. Allows source to re-partition the data before sending the
 * data through the operator chain.
 */
public class PartitionKeyExtractorFunc implements TransformFunc<StreamEvent<String>, AggregateKey> {
  private final Gson _gsonParser;

  public PartitionKeyExtractorFunc() {
    this._gsonParser = new GsonBuilder().create();
  }

  @Override
  public Collection<AggregateKey> apply(StreamEvent<String> input)
      throws Exception {
    try {
      String msg = input.data();
      if (msg.length() > 6) {
        String data = msg.substring(6);
        Event event = _gsonParser.fromJson(data, Event.class);
        AggregateKey key = new AggregateKey(event.getDevice(), event.getTitle(), event.getCountry());
        return Collections.singletonList(key);
      } else {
        return Collections.emptyList();
      }
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
