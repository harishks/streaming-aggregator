package aggregator.operator;

import lombok.EqualsAndHashCode;
import lombok.ToString;


/**
 * A simple fixed window that holds the value along with the interval.
 */
@EqualsAndHashCode
@ToString
public class FixedWindow {
  private final long _start;
  private final long _end;
  private long _value;

  public FixedWindow(long start, long end) {
    this._start = start;
    this._end = end;
    this._value = 0L;
  }

  public void add(int value) {
    _value += value;
  }

  public long get() {
    return _value;
  }

  public long getStart() {
    return _start;
  }

  public long getEnd() {
    return _end;
  }
}
