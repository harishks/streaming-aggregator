package aggregator.pipeline;

import aggregator.source.TimestampEvent;


/**
 * Allows applications to define the data pipeline.
 */
public interface PipelineBuilder<T, U extends TimestampEvent> {
  /**
   * User-applications can define their data processing logic using this method.
   */
  Pipeline<T, U> build();

  /**
   * Unique job name to identify this pipeline in the system.
   */
  String pipelineJobName();
}
