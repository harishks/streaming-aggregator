package aggregator.sink;

import com.atlassian.guava.common.util.concurrent.RateLimiter;


/**
 * Simple rate-limited sink to throttle writes before pushing the events to the sink.
 */
public abstract class RateLimitedSinkOperator<O> extends ExternalSinkOperator<O> {
  private final int _peakRate;
  private final int _maxWorkers;
  private int _perWorkerRate;
  private RateLimiter rateLimiter;

  public RateLimitedSinkOperator(int peakRate, int numWorkers) {
    this._peakRate = peakRate;
    this._maxWorkers = numWorkers;
  }

  @Override
  public void init() {
    this._perWorkerRate = Math.floorDiv(_peakRate, _maxWorkers);
    this.rateLimiter = RateLimiter.create(_perWorkerRate);
  }

  public int getPeakRatePerWorker() {
    return _perWorkerRate;
  }

  @Override
  public void flush() {
    try {
      rateLimiter.acquire(_perWorkerRate);
    } catch (Exception e) {
      System.err.println("Failed to rate limit the batch flush request - " +  e.getMessage());
    }
  }
}
