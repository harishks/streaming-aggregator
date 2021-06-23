package aggregator.sink;

import java.util.ArrayList;
import java.util.List;


/**
 * Simple terminal sink operator that writes the events to a console output.
 */
public class ConsoleOutputSinkOperator extends ExternalSinkOperator<String> {
  private List<String> outputList;
  private final int batchSize;

  public ConsoleOutputSinkOperator(int batchSize) {
    this.batchSize = batchSize;
  }

  @Override
  public void init() {
    outputList = new ArrayList<>(batchSize);
  }

  @Override
  public void accumulate(String input) {
    outputList.add(input);
  }

  @Override
  public void flush() {
    for (String output : outputList) {
      System.out.println(output);
    }
    outputList.clear();
  }

  @Override
  public void cleanup() {
    /* Flush the buffered elements */
    if (outputList.size() > 0) {
      flush();
    }
  }

  @Override
  public int batchSize() {
    return batchSize;
  }

  @Override
  public long getMaxEventTimestamp() {
    return 0;
  }
}
