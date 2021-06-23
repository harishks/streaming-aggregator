package aggregator.functions;

import java.util.Collection;

/**
 * Transform function that can transform the input data into a collection of output records.
 */
public interface TransformFunc<I, O> {

  Collection<O> apply(I input) throws Exception;
}
