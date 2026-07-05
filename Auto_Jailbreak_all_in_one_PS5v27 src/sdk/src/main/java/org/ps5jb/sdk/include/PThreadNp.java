package org.ps5jb.sdk.include;

import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.sys.ErrNo;
import org.ps5jb.sdk.include.sys.errno.NotFoundException;
import org.ps5jb.sdk.include.sys.pthreadtypes.PThreadType;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/pthread_np.h</code> from FreeBSD source.
 */
public class PThreadNp {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public PThreadNp(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    /**
     * Sets internal name for thread specified by tid argument
     * to string value specified by name argument.
     *
     * @param tid Thread to rename.
     * @param name New thread name.
     * @throws NotFoundException Thread with given tid not found.
     */
    public void rename(PThreadType tid, String name) throws NotFoundException {
        int ret = libKernel.pthread_rename_np(tid.getPthread(), name);
        if (ret != 0) {
            SdkException ex = errNo.getLastException(getClass(), "pthread_rename_np");
            if (ex instanceof NotFoundException) {
                throw (NotFoundException) ex;
            } else {
                throw new SdkRuntimeException(ex.getMessage(), ex);
            }
        }
    }

    public int getThreadId() {
        return libKernel.pthread_getthreadid_np();
    }
}
