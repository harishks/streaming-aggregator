package aggregator.source.partition;

import lombok.EqualsAndHashCode;

/**
 * Partition information per stream/topic.
 */
@EqualsAndHashCode
public class Partition {
  private String _streamName;
  private int _partitionId;

  public Partition(int partitionId) {
    this._partitionId = partitionId;
  }

  public Partition(String streamName, int partitionId) {
    this._streamName = streamName;
    this._partitionId = partitionId;
  }

  public String getStreamName() {
    return _streamName;
  }

  public void setStreamName(String streamName) {
    this._streamName = streamName;
  }

  public int getPartitionId() {
    return _partitionId;
  }

  public void setPartitionId(int partitionId) {
    this._partitionId = partitionId;
  }
}