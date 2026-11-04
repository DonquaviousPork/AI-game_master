package gamemaster.save;

/** A save or load failed: bad slot name, missing file, damaged contents, or a disk error. */
public class SaveException extends RuntimeException {
    public SaveException(String message) { super(message); }
    public SaveException(String message, Throwable cause) { super(message, cause); }
}
