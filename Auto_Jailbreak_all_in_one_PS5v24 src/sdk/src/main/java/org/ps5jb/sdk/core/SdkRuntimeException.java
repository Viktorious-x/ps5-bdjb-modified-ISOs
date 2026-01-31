package org.ps5jb.sdk.core;

/**
 * Base class for all unchecked exceptions raised by the SDK.
 */
public class SdkRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 7145867944382620029L;

    private final Throwable cause;

    /**
     * Default constructor. Builds an exception instance without a message or a cause.
     */
    public SdkRuntimeException() {
        this(null, null);
    }

    /**
     * Constructor which builds an exception with an error message.
     *
     * @param message Error message associated with this exception. May be null.
     */
    public SdkRuntimeException(String message) {
        this(message, null);
    }

    /**
     * Constructor which builds an exception with a chained cause exception.
     *
     * @param cause Exception which caused this instance to be thrown. May be null.
     */
    public SdkRuntimeException(Throwable cause) {
        this(null, cause);
    }

    /**
     * Constructor which builds an exception with an error message and a chained cause exception.
     *
     * @param message Error message associated with this exception. May be null.
     * @param cause Throwable instance which caused this exception to occur. May be null.
     */
    public SdkRuntimeException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    /**
     * Get the cause of this exception.
     *
     * @return Throwable instance which caused this exception to occur or null if there is no chained cause.
     */
    public Throwable getCause() {
        return this.cause;
    }
}
