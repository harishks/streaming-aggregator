package aggregator;

import aggregator.pipeline.Pipeline;
import aggregator.source.partition.Partition;


/**
 * Partitioning scheme that can be supplied by the user to partition the data before streaming the data through
 * the {@link Pipeline}.
 */
public interface Partitioner<T> {
  /**
   * Computes a partition value for the given key
   */
  Partition getPartition(T data);

  /**
   * Number of partitions used by the partitioner to partition the data.
   * <p>NOTE: It is the responsibility of the partitioning logic to supply the
   * number of partition based on the source stream. Will be cleaner if this context is supplied by the source.</p>
   * @return num partitions used by this partitioner.
   */
  int numPartitions();
}
