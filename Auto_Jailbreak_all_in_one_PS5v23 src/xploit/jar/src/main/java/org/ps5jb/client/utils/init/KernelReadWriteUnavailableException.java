package org.ps5jb.client.utils.init;

import org.ps5jb.sdk.core.SdkRuntimeException;

/**
 * Thrown when kernel read/write primitive is required but is not available.
 */
public class KernelReadWriteUnavailableException extends SdkRuntimeException {
    private static final long serialVersionUID = -1338085216196466972L;

    /**
     * Default constructor with no message or cause.
     */
    public KernelReadWriteUnavailableException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public KernelReadWriteUnavailableException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public KernelReadWriteUnavailableException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public KernelReadWriteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
