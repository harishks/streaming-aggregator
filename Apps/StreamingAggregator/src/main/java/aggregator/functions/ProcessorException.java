package aggregator.functions;

/**
 * Exceptions that are encountered while processing the stream record in the pipeline.
 */
public class ProcessorException extends Exception {

  private static final long serialVersionUID = -1300059134851684090L;

  public ProcessorException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProcessorException(String message) {
    super(message);
  }

  public ProcessorException(Throwable cause) {
    super(cause);
  }

}