package aggregator;

import aggregator.functions.TransformFunc;
import aggregator.source.partition.Partition;
import java.util.Collection;


/**
 * Hash key based partitioning logic.
 */
public class HashPartitioner<T, K> implements Partitioner<T> {
  private static final Partition DEFAULT_PARTITION_ID = new Partition(0);
  private final int _partitions;
  private final TransformFunc<T, K> _partitionKeyFunc;

  public HashPartitioner(int maxPartitions, TransformFunc<T, K> partitionKeyFunc) {
    this._partitions = maxPartitions;
    this._partitionKeyFunc = partitionKeyFunc;
  }

  /* Sends a default partition ID if the partitioner fails to partition the input data */
  @Override
  public Partition getPartition(T event) {
    try {
      Collection<K> keyCollection = _partitionKeyFunc.apply(event);

      if (keyCollection.stream().findFirst().isPresent()) {
        int partitionId = Math.abs(keyCollection.stream().findFirst().get().hashCode() % _partitions);
        return new Partition(partitionId);
      }
    } catch (Exception e) {
      System.out.println("Failed to partition data");
    }
    return DEFAULT_PARTITION_ID;
  }

  @Override
  public int numPartitions() {
    return _partitions;
  }
}
