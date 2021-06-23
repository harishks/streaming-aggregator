package aggregator.application;

import aggregator.codec.AggregateKey;
import aggregator.codec.AggregateResult;
import aggregator.codec.TimestampedStreamEvent;
import aggregator.functions.FilterFunc;
import aggregator.functions.RowToString;
import aggregator.functions.StreamEventParser;
import aggregator.functions.WindowAggregateFunc;
import aggregator.models.Event;
import aggregator.operator.FilterOperator;
import aggregator.operator.InputOperator;
import aggregator.operator.MapOperator;
import aggregator.operator.Operator;
import aggregator.operator.WindowOperator;
import aggregator.pipeline.Pipeline;
import aggregator.pipeline.PipelineBuilder;
import aggregator.sink.ConsoleOutputSinkOperator;
import aggregator.sink.ExternalSinkOperator;
import aggregator.source.StreamEvent;
import aggregator.source.UnPartitionedStreamSource;
import java.time.Duration;
import java.util.Collections;


/**
 * This class describes the desired application by constructing a streaming data pipeline.
 * <p>The goal of this application is to consume events from a HTTP event source and compute the per-second
 * streams-start-per-second metric for a distinct combination of fields matching certain filtering criteria.</p>
 */
public class StreamsPerSecJobBuilder implements PipelineBuilder<StreamEvent<String>, TimestampedStreamEvent> {
  private final UnPartitionedStreamSource<StreamEvent<String>> _unPartitionedStreamSource;

  public StreamsPerSecJobBuilder(UnPartitionedStreamSource<StreamEvent<String>> source) {
    this._unPartitionedStreamSource = source;
  }

  @Override
  public Pipeline<StreamEvent<String>, TimestampedStreamEvent> build() {
    ExternalSinkOperator<String> externalSinkOperator = new ConsoleOutputSinkOperator(5);

    InputOperator<StreamEvent<String>, TimestampedStreamEvent> inputOp = new InputOperator<>(new StreamEventParser());
    Operator<TimestampedStreamEvent, TimestampedStreamEvent> filterOp = new FilterOperator<>(
        (FilterFunc<TimestampedStreamEvent>) message -> message.getData().getSev().equals("success"));

    inputOp.registerNextOp(filterOp);
    filterOp.registerPrevOp(inputOp);

    Operator<TimestampedStreamEvent, AggregateResult> windowOp = new WindowOperator<>(Duration.ofSeconds(1), input -> {
      Event event = input.getData();
      return Collections.singletonList(new AggregateKey(event.getDevice(), event.getTitle(), event.getCountry()));
    }, new WindowAggregateFunc<>() {
      @Override
      public void aggregate(AggregateResult value) {
        long curVal = value.getAggregateValue();
        value.updateValue(curVal + 1);
      }

      @Override
      public AggregateResult supply(AggregateKey key) {
        return new AggregateResult(key, 1);
      }
    });

    windowOp.registerPrevOp(filterOp);
    filterOp.registerNextOp(windowOp);

    Operator<AggregateResult, String> mapperOp = new MapOperator<>(new RowToString());
    windowOp.registerNextOp(mapperOp);
    mapperOp.registerPrevOp(windowOp);

    mapperOp.registerExternalSink(externalSinkOperator);
    return new Pipeline<>(inputOp, _unPartitionedStreamSource);
  }

  @Override
  public String pipelineJobName() {
    return "Streaming-Windowed-Aggregation";
  }
}
