package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.errno.NotFoundException;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.include.sys.umtx.UmtxOpcodeType;
import org.ps5jb.sdk.include.sys.umtx.UmtxShmFlag;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/sys/umtx.h</code> from FreeBSD source.
 */
public class Umtx {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public Umtx(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    public int userMutexOperation(Pointer obj, UmtxOpcodeType operation, UmtxShmFlag flag, Pointer uaddr, Pointer uaddr2) throws SdkException {
        int ret = this.libKernel._umtx_op(obj, operation.value(), flag.value(), uaddr, uaddr2);
        if (ret == -1) {
            throw errNo.getLastException(getClass(), "userMutexOperation");
        }

        return ret;
    }

    public int userMutexCreate(Pointer key) throws SdkException {
        return userMutexOperation(Pointer.NULL, UmtxOpcodeType.UMTX_OP_SHM, UmtxShmFlag.UMTX_SHM_CREAT, key, Pointer.NULL);
    }

    /**
     * Find the shared memory object by key.
     *
     * @param key Key of the shared memory object for find.
     * @return Found shared memory object.
     *
     * @throws NotFoundException If the key is not found.
     * @throws SdkException If the lookup operation failed.
     */
    public int userMutexLookup(Pointer key) throws SdkException {
        return userMutexOperation(Pointer.NULL, UmtxOpcodeType.UMTX_OP_SHM, UmtxShmFlag.UMTX_SHM_LOOKUP, key, Pointer.NULL);
    }

    public int userMutexDestroy(Pointer key) throws SdkException {
        return userMutexOperation(Pointer.NULL, UmtxOpcodeType.UMTX_OP_SHM, UmtxShmFlag.UMTX_SHM_DESTROY, key, Pointer.NULL);
    }
}
