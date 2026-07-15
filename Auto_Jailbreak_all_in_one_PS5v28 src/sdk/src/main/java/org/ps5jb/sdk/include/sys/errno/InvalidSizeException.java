package org.ps5jb.sdk.include.sys.errno;

import org.ps5jb.sdk.core.SdkException;

/**
 * Exception corresponding to FreeBSD <code>ERANGE</code> error code.
 */
public class InvalidSizeException extends SdkException {
    private static final long serialVersionUID = 8682353427481561476L;

    /**
     * Default constructor with no message or cause.
     */
    public InvalidSizeException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public InvalidSizeException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public InvalidSizeException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public InvalidSizeException(String message, Throwable cause) {
        super(message, cause);
    }
}
