package org.ps5jb.sdk.include.sys.errno;

import org.ps5jb.sdk.core.SdkException;

/**
 * Exception corresponding to FreeBSD <code>EFAULT</code> error code.
 */
public class MemoryFaultException extends SdkException {
    private static final long serialVersionUID = 2381337152984935037L;

    /**
     * Default constructor with no message or cause.
     */
    public MemoryFaultException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public MemoryFaultException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public MemoryFaultException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public MemoryFaultException(String message, Throwable cause) {
        super(message, cause);
    }
}
