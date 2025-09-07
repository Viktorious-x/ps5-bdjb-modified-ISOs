package org.ps5jb.sdk.include.sys.errno;

import org.ps5jb.sdk.core.SdkException;

/**
 * Exception corresponding to FreeBSD <code>EDEADLK</code> error code.
 */
public class DeadlockException extends SdkException {
    private static final long serialVersionUID = 7109044860280702256L;

    /**
     * Default constructor with no message or cause.
     */
    public DeadlockException() {
        super();
    }

    /**
     * Constructor with an error message.
     *
     * @param message Message corresponding to the error condition.
     */
    public DeadlockException(String message) {
        super(message);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause Original exception that prompted this exception to be raised.
     */
    public DeadlockException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with error message and cause.
     *
     * @param message Message corresponding to the error condition.
     * @param cause Original exception that prompted this exception to be raised.
     */
    public DeadlockException(String message, Throwable cause) {
        super(message, cause);
    }
}
