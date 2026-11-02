package gamemaster.llm;

/** Anything that goes wrong talking to the model: bad config, network, HTTP error, unusable reply. */
public class LLMException extends RuntimeException {
    public LLMException(String message) { super(message); }
    public LLMException(String message, Throwable cause) { super(message, cause); }
}
