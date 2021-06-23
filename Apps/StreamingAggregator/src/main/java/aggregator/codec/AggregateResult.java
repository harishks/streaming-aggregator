package aggregator.codec;

import java.io.Serializable;
import lombok.EqualsAndHashCode;


/**
 * In-memory representation of an aggregated row.
 */
@EqualsAndHashCode
public class AggregateResult implements Serializable {
  private AggregateKey _aggregateKey;
  private long _aggregateValue;

  public AggregateResult(AggregateKey aggregateKey, long aggregateValue) {
    this._aggregateKey = aggregateKey;
    this._aggregateValue = aggregateValue;
  }

  /**
   * Update value for this aggregation result.
   * @param value new value to be used for this resultset.
   */
  public void updateValue(long value) {
    this._aggregateValue = value;
  }

  /**
   * Get the current state of the aggregation value.
   * @return current aggregation value.
   */
  public long getAggregateValue() {
    return _aggregateValue;
  }

  @Override
  public String toString() {
    return "{" + _aggregateKey + "," + "sps:" + _aggregateValue + '}';
  }
}
