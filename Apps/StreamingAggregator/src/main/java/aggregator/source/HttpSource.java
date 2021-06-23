package aggregator.source;

import aggregator.Partitioner;
import aggregator.source.partition.Partition;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;


/**
 * HTTP based streaming source.
 */
public class HttpSource implements UnPartitionedStreamSource<StreamEvent<String>> {
  private BufferedReader stream;
  private long _offset = 0;
  private final String _streamResource;
  private final Partitioner<StreamEvent<String>> _rePartitioner;

  public HttpSource(String streamResource, Partitioner<StreamEvent<String>> rePartitionFunc) {
    this._rePartitioner = rePartitionFunc;
    this._streamResource = streamResource;
  }

  @Override
  public StreamEvent<String> read() throws IOException {
    return new StreamEvent<>(stream.readLine(), _offset++);
  }

  @Override
  public void init() throws Exception {
    URL url = new URL(_streamResource);
    this.stream = new BufferedReader(new InputStreamReader(url.openStream()));
  }

  @Override
  public void close() throws Exception {
    stream.close();
  }

  @Override
  public int numPartitions() {
    return _rePartitioner.numPartitions();
  }

  @Override
  public Partition rePartition(StreamEvent<String> event) {
    return _rePartitioner.getPartition(event);
  }

}
