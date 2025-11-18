package org.ps5jb.sdk.include.sys.errno;

import org.ps5jb.sdk.core.SdkException;

/**
 * Exception corresponding to FreeBSD <code>EPERM</code> error code.
 */
public class OperationNotPermittedException extends SdkException {
    private static final long serialVersionUID = 3168161600843242728L;

    /**
     * Default constructor with no message or cause.
     */
    public OperationNotPermittedException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public OperationNotPermittedException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public OperationNotPermittedException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public OperationNotPermittedException(String message, Throwable cause) {
        super(message, cause);
    }
}
