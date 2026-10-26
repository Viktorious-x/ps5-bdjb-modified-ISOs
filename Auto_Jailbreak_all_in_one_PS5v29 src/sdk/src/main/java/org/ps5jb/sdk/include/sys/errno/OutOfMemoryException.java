package org.ps5jb.sdk.include.sys.errno;

import org.ps5jb.sdk.core.SdkException;

/**
 * Exception corresponding to FreeBSD <code>ENOMEM</code> error code.
 */
public class OutOfMemoryException extends SdkException {
    private static final long serialVersionUID = 2381337152984935037L;

    /**
     * Default constructor with no message or cause.
     */
    public OutOfMemoryException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public OutOfMemoryException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public OutOfMemoryException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public OutOfMemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
