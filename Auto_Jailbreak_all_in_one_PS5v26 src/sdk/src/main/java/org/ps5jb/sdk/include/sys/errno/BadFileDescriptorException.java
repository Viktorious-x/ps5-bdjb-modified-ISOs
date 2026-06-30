package org.ps5jb.sdk.include.sys.errno;

import org.ps5jb.sdk.core.SdkException;

/**
 * Exception corresponding to FreeBSD <code>EBADF</code> error code.
 */
public class BadFileDescriptorException extends SdkException {
    private static final long serialVersionUID = -6609560787806393785L;

    /**
     * Default constructor with no message or cause.
     */
    public BadFileDescriptorException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public BadFileDescriptorException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public BadFileDescriptorException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public BadFileDescriptorException(String message, Throwable cause) {
        super(message, cause);
    }
}
