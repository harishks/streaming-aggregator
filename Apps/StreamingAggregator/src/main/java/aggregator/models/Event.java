package aggregator.models;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


/**
 * Model object that captures the stream-start events from Netflix's HTTP source.
 */
@EqualsAndHashCode
@NoArgsConstructor
@ToString
public class Event {
  private String device;
  private String sev;
  private String title;
  private String country;
  private long time;

  public String getDevice() {
    return device;
  }

  public String getSev() {
    return sev;
  }

  public String getTitle() {
    return title;
  }

  public String getCountry() {
    return country;
  }

  public long getTime() {
    return time;
  }

}
