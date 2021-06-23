package aggregator.source;

import aggregator.source.partition.Partition;
import aggregator.taskmgmt.Task;


/**
 * This abstraction helps source operators to re-partition the incoming data stream based
 * on a custom partitioning scheme.
 *
 * <p>Some streaming source operators might not natively support partitioned data streams. In such cases,
 * it will be useful for the source operators to re-partition the data stream before allowing the streams
 * to be processed by a {@link Task}, thereby allowing efficiently parallel processing of the pipeline.</p>
 */
public interface UnPartitionedStreamSource<T> extends StreamSource<T> {
  /**
   * Allows sources to re-partition the incoming data stream based on a custom partitioning logic that can help
   * parallelize the stream processing pipeline by shuffling the keys.
   *
   * The returned partition count should be bounded by the max number of source partitions.
   *
   * NOTE: In real-world multi-node/multi-processor task distribution model, this function can cause the output data
   * stream to be sent to a task on a difference host. This will result in network I/O. The transport mechanism needs to
   * be a reliable channel that can efficiently transfer the re-partitioned data in a reliable manner (E.g, Kafka).
   *
   * @param event input key that can be used to partition the stream data.
   * @return partition whose partitionId is upper bounded by the max source partitions.
   */
  Partition rePartition(T event);
}
