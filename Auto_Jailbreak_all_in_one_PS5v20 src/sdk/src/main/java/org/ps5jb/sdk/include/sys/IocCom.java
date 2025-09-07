package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/sys/ioccom.h</code> from FreeBSD source.
 */
public class IocCom {
    /** Number of bits for ioctl size. */
    public static final long IOCPARM_SHIFT = 13;
    public static final long IOCPARM_MASK = (1 << IOCPARM_SHIFT) - 1;

    /** No parameters. */
    public static final long IOC_VOID = 0x20000000;
    /** Copy out parameters. */
    public static final long IOC_OUT = 0x40000000;
    /** Copy in parameters. */
    public static final long IOC_IN = 0x80000000;

    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public IocCom(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    public static long _IOC(long inout, long group, long num, long len) {
        return inout | ((len & IOCPARM_MASK) << 16) | (group << 8) | num;
    }

    public static long _IOW(long group, long num, long type_size) {
        return _IOC(IOC_IN, group, num, type_size);
    }

    public int ioctl(int fd, long request, long argp) throws SdkException {
        int ret = libKernel.ioctl(fd, request, argp);
        if (ret == -1) {
            throw errNo.getLastException(getClass(), "ioctl");
        }
        return ret;
    }
}
