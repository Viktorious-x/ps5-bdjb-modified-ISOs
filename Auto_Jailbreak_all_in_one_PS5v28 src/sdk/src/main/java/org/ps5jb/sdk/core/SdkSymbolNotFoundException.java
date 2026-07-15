package org.ps5jb.sdk.core;

/**
 * Raised by SDK core when a lookup of a symbol address by its name
 * did not produce any results.
 */
public class SdkSymbolNotFoundException extends SdkRuntimeException {
    private static final long serialVersionUID = 2290121626936470596L;

    /**
     * Default constructor with no message or cause.
     */
    public SdkSymbolNotFoundException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public SdkSymbolNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public SdkSymbolNotFoundException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public SdkSymbolNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
