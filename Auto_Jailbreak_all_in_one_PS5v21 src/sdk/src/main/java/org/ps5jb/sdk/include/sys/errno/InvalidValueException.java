package org.ps5jb.sdk.include.sys.errno;

import org.ps5jb.sdk.core.SdkException;

/**
 * Exception corresponding to FreeBSD <code>EINVAL</code> error code.
 */
public class InvalidValueException extends SdkException {
    private static final long serialVersionUID = 8067711595828305814L;

    /**
     * Default constructor with no message or cause.
     */
    public InvalidValueException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public InvalidValueException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public InvalidValueException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public InvalidValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
