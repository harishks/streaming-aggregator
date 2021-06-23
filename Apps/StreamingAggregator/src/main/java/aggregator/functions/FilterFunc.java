package aggregator.functions;

import java.io.Serializable;


/**
 * User-defined filter function that can be applied on the incoming events from the stream to decide whether this
 * event need to be sent up the {@link aggregator.pipeline.Pipeline}.
 */
public interface FilterFunc<M> extends Serializable {

  /**
   * Returns a boolean indicating whether this message should be retained or filtered out.
   *
   * @param message  the input message to be checked
   * @return  true if {@code message} should be retained
   */
  boolean apply(M message);
}
