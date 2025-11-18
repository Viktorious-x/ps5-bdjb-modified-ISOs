package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.include.sys.errno.InvalidValueException;
import org.ps5jb.sdk.include.sys.errno.BadFileDescriptorException;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.select.FdSetType;
import org.ps5jb.sdk.include.sys.timeval.TimevalType;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/sys/select.h</code> from FreeBSD source.
 */
public class Select {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public Select(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    /**
     * Examines the I/O  descriptor  sets  whose  addresses are  passed
     * in <code>readFds</code>, <code>writeFds</code>, and <code>exceptFds</code> to see if some
     * of their descriptors are ready for reading, are ready for writing, or
     * have an exceptional condition pending, respectively. The only exceptional
     * condition detectable is out-of-band data received on a socket.
     * Up to <code>maxFd</code> descriptors are checked in each set; i.e.,
     * the descriptors from <code>0</code> through <code>maxFd-1</code>
     * in the descriptor sets are examined.
     *
     * On return, <code>select()</code> replaces the given descriptor sets with subsets
     * consisting of those descriptors that are ready for the requested operation.
     *
     * If <code>select()</code> returns with an error, including one due
     * to an interrupted system call, the descriptor sets will be unmodified.
     *
     * @param maxFd Maximum file descriptor to analyze.
     * @param readFds Descriptors to test for being ready for reading.
     * @param writeFds Descriptors to test for being ready for writing.
     * @param exceptFds Descriptors to test for exceptional condition.
     * @param timeout Specifies the maximum interval to wait for the selection to complete.
     *   System activity can lengthen the interval by an indeterminate amount.
     *   If <code>timeout</code> is <code>null</code>, the select	blocks indefinitely.
     * @return The total number of ready descriptors in all the sets.
     * @throws BadFileDescriptorException One of the descriptor sets specified an invalid descriptor.
     * @throws InvalidValueException The specified <code>timeout</code> or <code>fdCount</code> is invalid.
     * @throws SdkException If any other exception occurs.
     */
    public int select(int maxFd, FdSetType readFds, FdSetType writeFds, FdSetType exceptFds, TimevalType timeout)
            throws SdkException {

        int ret = libKernel.select(maxFd,
                readFds == null ? Pointer.NULL : readFds.getPointer(),
                writeFds == null ? Pointer.NULL : writeFds.getPointer(),
                exceptFds == null ? Pointer.NULL : exceptFds.getPointer(),
                timeout == null ? Pointer.NULL : timeout.getPointer());

        if (ret == -1) {
            throw errNo.getLastException(getClass(), "select");
        }

        return ret;
    }
}
