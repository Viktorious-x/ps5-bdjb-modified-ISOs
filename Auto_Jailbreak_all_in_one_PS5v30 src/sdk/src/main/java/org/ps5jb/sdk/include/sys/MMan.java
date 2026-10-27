package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.sys.errno.InvalidValueException;
import org.ps5jb.sdk.include.sys.errno.MemoryFaultException;
import org.ps5jb.sdk.include.sys.errno.OperationNotPermittedException;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.include.sys.mman.MappingFlag;
import org.ps5jb.sdk.include.sys.mman.ProtectionFlag;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/sys/mman.h</code> from FreeBSD source.
 */
public class MMan {
    /** Constant used internally to create anonymous shared memory. */
    private static final Pointer SHM_ANON = Pointer.valueOf(1);
    /** Constant used internally to check for error when mapping memory. */
    private static final Pointer MAP_FAILED = Pointer.valueOf(-1);

    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public MMan(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    /**
     * Opens (or optionally creates) a POSIX shared memory object named path.
     *
     * @param path Name of the shared memory.
     * @param mode The shared memory object is created with mode <code>mode</code>, subject to the process' umask value.
     * @param flags An access mode of either {@link OpenFlag#O_RDONLY} or {@link OpenFlag#O_RDWR} must be included in flags.
     *   The optional flags {@link OpenFlag#O_CREAT}, {@link OpenFlag#O_EXCL}, and {@link OpenFlag#O_TRUNC}
     *   may also be specified.
     * @return Returns a non-negative integer.
     * @throws SdkException Shared memory opening failed.
     */
    public int sharedMemoryOpen(String path, int mode, OpenFlag ... flags) throws SdkException {
        Pointer pathPtr = Pointer.fromString(path);
        try {
            return shmOpen(pathPtr, flags, mode);
        } finally {
            pathPtr.free();
        }
    }

    /**
     * Create an anonymous shared memory. In this  case, an unnamed shared
     * memory object is created. Since the object has no name, it cannot be
     * removed via a subsequent call to {@link #sharedMemoryUnlink(String)}.
     * Instead, the shared memory object will be garbage collected
     * when the last reference to the shared memory object is removed.
     *
     * @param mode The shared  memory object is created with mode <code>mode</code>,
     *   subject to the process' umask value.
     * @param flags An access mode of either {@link OpenFlag#O_RDONLY} or {@link OpenFlag#O_RDWR} must be included in flags.
     *   The optional flags {@link OpenFlag#O_CREAT}, {@link OpenFlag#O_EXCL}, and {@link OpenFlag#O_TRUNC}
     *   may also be specified.
     * @return Returns a non-negative integer.
     * @throws SdkException Shared memory opening failed.
     */
    public int sharedMemoryOpenAnonymous(int mode, OpenFlag ... flags) throws SdkException {
        return shmOpen(SHM_ANON, flags, mode);
    }

    private int shmOpen(Pointer path, OpenFlag[] flags, int mode) throws SdkException {
        int ret = this.libKernel.shm_open(path, OpenFlag.or(flags), mode);
        if (ret == -1) {
            throw errNo.getLastException(getClass(), "shmOpen");
        }

        return ret;
    }

    /**
     * Removes a shared memory object named path.
     *
     * @param path Path to remove.
     * @throws MemoryFaultException The path argument points outside the process' allocated  address space.
     * @throws OperationNotPermittedException The required permissions are denied. This function
     *     requires write permission to the shared memory object.
     * @throws SdkException Shared memory removal failed.
     */
    public void sharedMemoryUnlink(String path) throws SdkException {
        Pointer pathPtr = Pointer.fromString(path);
        try {
            int ret = this.libKernel.shm_unlink(pathPtr);
            if (ret == -1) {
                throw errNo.getLastException(getClass(), "sharedMemoryUnlink");
            }
        } finally {
            pathPtr.free();
        }
    }

    public Pointer memoryMap(Pointer addr, long len, ProtectionFlag[] prot, MappingFlag[] flags, int fd, long offset) throws SdkException {
        Pointer ret = this.libKernel.mmap(addr, len, ProtectionFlag.or(prot), MappingFlag.or(flags), fd, offset);
        if (MAP_FAILED.equals(ret)) {
            throw errNo.getLastException(getClass(), "memoryMap");
        }
        return ret;
    }

    public void memoryUnmap(Pointer addr, long len) throws InvalidValueException {
        long ret = this.libKernel.munmap(addr, len);
        if (ret == -1) {
            SdkException ex = errNo.getLastException(getClass(), "memoryUnmap");
            if (ex instanceof InvalidValueException) {
                throw (InvalidValueException) ex;
            } else {
                throw new SdkRuntimeException(ex.getMessage(), ex);
            }
        }
    }

    public void memoryProtect(Pointer addr, long len, ProtectionFlag ... prot) throws InvalidValueException, OperationNotPermittedException {
        long ret = this.libKernel.mprotect(addr, len, ProtectionFlag.or(prot));
        if (ret == -1) {
            SdkException ex = errNo.getLastException(getClass(), "memoryProtect");
            if (ex instanceof InvalidValueException) {
                throw (InvalidValueException) ex;
            } else if (ex instanceof OperationNotPermittedException) {
                throw (OperationNotPermittedException) ex;
            } else {
                throw new SdkRuntimeException(ex.getMessage(), ex);
            }
        }
    }
}
