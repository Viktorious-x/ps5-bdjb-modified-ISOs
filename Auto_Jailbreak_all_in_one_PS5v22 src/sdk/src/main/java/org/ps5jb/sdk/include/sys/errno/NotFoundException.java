package org.ps5jb.sdk.include.sys.errno;

import org.ps5jb.sdk.core.SdkException;

/**
 * Exception corresponding to FreeBSD <code>ESRCH</code> error code.
 */
public class NotFoundException extends SdkException {
    private static final long serialVersionUID = 7555246672766984540L;

    /**
     * Default constructor with no message or cause.
     */
    public NotFoundException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public NotFoundException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
