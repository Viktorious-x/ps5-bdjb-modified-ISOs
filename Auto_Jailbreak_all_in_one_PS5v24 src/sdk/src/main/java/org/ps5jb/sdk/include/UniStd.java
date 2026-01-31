package org.ps5jb.sdk.include;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.sys.errno.BadFileDescriptorException;
import org.ps5jb.sdk.include.sys.errno.InvalidValueException;
import org.ps5jb.sdk.include.sys.errno.OperationNotPermittedException;
import org.ps5jb.sdk.include.sys.ErrNo;
import org.ps5jb.sdk.include.sys.errno.OutOfMemoryException;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/unistd.h</code> from FreeBSD source.
 */
public class UniStd {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public UniStd(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    public int getuid() {
        return libKernel.getuid();
    }

    public void setuid(int uid) throws OperationNotPermittedException {
        int ret = libKernel.setuid(uid);
        if (ret == -1) {
            SdkException ex = errNo.getLastException(getClass(), "setuid");
            if (ex instanceof OperationNotPermittedException) {
                throw (OperationNotPermittedException) ex;
            } else {
                throw new SdkRuntimeException(ex.getMessage(), ex);
            }
        }
    }

    public int getpid() {
        return libKernel.getpid();
    }

    public int[] pipe() throws SdkException {
        Pointer fildes = Pointer.calloc(8);
        try {
            int ret = libKernel.pipe(fildes);
            if (ret == -1) {
                throw errNo.getLastException(getClass(), "pipe");
            }

            return new int[] { fildes.read4(), fildes.read4(4) };
        } finally {
            fildes.free();
        }
    }

    public void ftruncate(int fd, long length) throws InvalidValueException, BadFileDescriptorException, OutOfMemoryException {
        int ret = libKernel.ftruncate(fd, length);
        if (ret == -1) {
            SdkException ex = errNo.getLastException(getClass(), "ftruncate");
            if (ex instanceof InvalidValueException) {
                throw (InvalidValueException) ex;
            } else if (ex instanceof BadFileDescriptorException) {
                throw (BadFileDescriptorException) ex;
            } else if (ex instanceof OutOfMemoryException) {
                throw (OutOfMemoryException) ex;
            } else {
                throw new SdkRuntimeException(ex.getMessage(), ex);
            }
        }
    }

    public void usleep(long microseconds) throws SdkException {
        int ret = libKernel.usleep(microseconds);
        if (ret == -1) {
            throw errNo.getLastException(getClass(), "usleep");
        }
    }
}
