package org.ps5jb.sdk.core;

/**
 * Raised by SDK when a certain functionality requires firmware-specific knowledge,
 * but it is not available.
 */
public class SdkSoftwareVersionUnsupportedException extends SdkRuntimeException {
    private static final long serialVersionUID = -2958319099920522L;

    /**
     * Default constructor with no message or cause.
     */
    public SdkSoftwareVersionUnsupportedException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public SdkSoftwareVersionUnsupportedException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public SdkSoftwareVersionUnsupportedException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public SdkSoftwareVersionUnsupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
