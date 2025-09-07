package org.ps5jb.sdk.io;

import org.ps5jb.sdk.core.SdkRuntimeException;

/**
 * Occurs when retrieval of a native file descriptor from
 * a {@link java.io.FileDescriptor} instance cannot be performed.
 * Files opened using pure Java calls have proxied descriptors. To open
 * a file without a proxy, it is necessary to use
 * {@link org.ps5jb.sdk.lib.LibKernel#open(String, int)} directly
 * then call {@link FileDescriptorFactory#createFileDescriptor(int)}
 * to obtain a FileDescriptor instance that can be used to instantiate
 * Java input and output streams.
 */
public class ProxiedFileDescriptorException extends SdkRuntimeException {
    private static final long serialVersionUID = 6698000017921188641L;

    /**
     * Default constructor. Builds an exception instance without a message or a cause.
     */
    public ProxiedFileDescriptorException() {
        super();
    }

    /**
     * Constructor which builds an exception with an error message.
     *
     * @param message Error message associated with this exception. May be null.
     */
    public ProxiedFileDescriptorException(String message) {
        super(message);
    }

    /**
     * Constructor which builds an exception with a chained cause exception.
     *
     * @param cause Exception which caused this instance to be thrown. May be null.
     */
    public ProxiedFileDescriptorException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor which builds an exception with an error message and a chained cause exception.
     *
     * @param message Error message associated with this exception. May be null.
     * @param cause Throwable instance which caused this exception to occur. May be null.
     */
    public ProxiedFileDescriptorException(String message, Throwable cause) {
        super(message, cause);
    }
}
