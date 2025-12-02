package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/sys/fcntl.h</code> from FreeBSD source.
 */
public class FCntl {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public FCntl(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    public int open(String path, OpenFlag ... flags) throws SdkException {
        int fd = libKernel.open(path, OpenFlag.or(flags));
        if (fd == -1) {
            throw errNo.getLastException(getClass(), "open", path);
        }
        return fd;
    }

    public void close(int fd) throws SdkException {
        int ret = libKernel.close(fd);
        if (ret == -1) {
            throw errNo.getLastException(getClass(), "close", Integer.toString(fd));
        }
    }
}
