package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.rtprio.RtPrioType;
import org.ps5jb.sdk.include.sys.rtprio.SchedulingClass;
import org.ps5jb.sdk.include.sys.stat.StatType;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/sys/stat.h</code> from FreeBSD source.
 */
public class Stat {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public Stat(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    /**
     * Obtains information about the about an open file
     * known by the file descriptor <code>fd</code>.
     *
     * @param fd File descriptor to query.
     * @return Newly-allocated StatType object with information regarding the file.
     *   This result should be freed when no longer needed.
     * @throws SdkException If file status could not be obtained.
     */
    public StatType getFileStatus(int fd) throws SdkException {
        Pointer buf = Pointer.calloc(StatType.SIZE);
        try {
            int ret = libKernel.fstat(fd, buf);
            if (ret == -1) {
                throw errNo.getLastException(getClass(), "getFileStatus");
            }

            return new StatType(buf);
        } catch (SdkException | RuntimeException | Error e) {
            buf.free();
            throw e;
        }
    }

    /**
     * Obtains information about the about file pointed to by <code>path</code>.
     * Read, write or execute permission of the named file is not required,
     * but all directories listed in the path name leading to the file must be searchable.
     *
     * @param path File path to query.
     * @return Newly-allocated StatType object with information regarding the file.
     *   This result should be freed when no longer needed.
     * @throws SdkException If file status could not be obtained.
     */
    public StatType getStatus(String path) throws SdkException {
        Pointer buf = Pointer.calloc(StatType.SIZE);
        try {
            int ret = libKernel.stat(path, buf);
            if (ret == -1) {
                throw errNo.getLastException(getClass(), "getStatus");
            }

            return new StatType(buf);
        } catch (SdkException | RuntimeException | Error e) {
            buf.free();
            throw e;
        }
    }
}
