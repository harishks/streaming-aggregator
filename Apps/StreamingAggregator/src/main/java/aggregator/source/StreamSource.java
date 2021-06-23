package aggregator.source;

/**
 * Data source that can stream data into the system continuously.
 */
public interface StreamSource<T> {
  /**
   * Each read results in fetching one unit of data from the source.
   * @return single unit of data.
   * @throws Exception when the read fails.
   */
  T read() throws Exception;

  /**
   * Initialization routine that needs to be invoked before consuming data from the source.
   * @throws Exception when source initialization fails.
   */
  void init() throws Exception;

  /**
   * Cleanly shutdown the data consumption from the source.
   * @throws Exception when source shutdown fails.
   */
  void close() throws Exception;

  /**
   * In general, streaming data is assumed to be partitioned. Partitioned streams allow processing the data
   * efficiently in parallel.
   * 
   * @return number of partioned per stream.
   */
  int numPartitions();
}
