package aggregator.pipeline;

import aggregator.operator.InputOperator;
import aggregator.operator.Operator;
import aggregator.source.TimestampEvent;
import aggregator.source.UnPartitionedStreamSource;


/**
 * A pipeline defines the sequence of operators that work on the input data stream. A single pipeline can be
 * executed in parallel by different tasks by operating on non-overlapping partitions of the input stream.
 * A pipeline in essence is a DAG that needs be executed on a given partition of the stream.
 */
public class Pipeline<T, U extends TimestampEvent> {
  private final InputOperator<T, U> _rootOperator;
  private final UnPartitionedStreamSource<T> _source;

  public Pipeline(InputOperator<T, U> rootOp, UnPartitionedStreamSource<T> source) {
    this._rootOperator = rootOp;
    this._source = source;
  }

  /**
   * Root operator of the pipeline. This would usually be the {@link InputOperator}.
   * @return root operator.
   */
  public Operator<T, U> getRootOperator() {
    return _rootOperator;
  }

  public UnPartitionedStreamSource<T> getSource() {
    return _source;
  }
}
