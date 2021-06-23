package aggregator;

public interface LifecycleAware {
  enum LifecycleState {
    INIT, START, STOP, ERROR
  }
  /**
   * Starts a service or component.
   */
  void start() throws Exception;

  /**
   * <p>
   * Stops a service or component.
   * </p>
   * <p>
   * Implementations should determine the result of any stop logic and effect
   * the return value of {@link #getLifecycleState()} accordingly.
   * </p>
   */
  void stop();

  /**
   * <p>
   * Return the current state of the service or component.
   * </p>
   */
  LifecycleState getLifecycleState();
}