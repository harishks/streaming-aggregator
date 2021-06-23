package aggregator.functions;

import aggregator.codec.AggregateResult;
import java.util.Collection;
import java.util.Collections;
import lombok.NonNull;


/**
 * Mapper function that translates a POJO to a user-friendly string representation.
 */
public class RowToString implements TransformFunc<AggregateResult, String> {
  @Override
  public Collection<String> apply(@NonNull AggregateResult input) throws Exception {
    return Collections.singletonList(input.toString());
  }
}
