package aggregator.codec;

import aggregator.operator.WindowOperator;
import java.io.Serializable;
import lombok.EqualsAndHashCode;


/**
 * Model event based aggregation key used in the aggregation of partitioned data by {@link WindowOperator}.
 */
@EqualsAndHashCode
public class AggregateKey implements Serializable {
  private final String device;
  private final String title;
  private final String country;

  public AggregateKey(String device, String title, String country) {
    this.device = device;
    this.title = title;
    this.country = country;
  }

  @Override
  public String toString() {
    return "\"device\":" + "\"" + device  + "\"" + ",\"title\":" + "\"" + title + "\"" + ",\"country\":"
        + "\"" + country + "\"";
  }
}
