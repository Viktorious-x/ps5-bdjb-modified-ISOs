package org.ps5jb.client.utils.init;

import org.ps5jb.sdk.core.SdkRuntimeException;

/**
 * Thrown when kernel base address is required but is not available.
 */
public class KernelBaseUnknownException extends SdkRuntimeException {
    private static final long serialVersionUID = -8252130383156532185L;

    /**
     * Default constructor with no message or cause.
     */
    public KernelBaseUnknownException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public KernelBaseUnknownException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public KernelBaseUnknownException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public KernelBaseUnknownException(String message, Throwable cause) {
        super(message, cause);
    }
}
