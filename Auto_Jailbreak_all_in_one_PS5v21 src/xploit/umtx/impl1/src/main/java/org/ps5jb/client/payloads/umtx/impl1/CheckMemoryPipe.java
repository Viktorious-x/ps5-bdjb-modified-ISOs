package org.ps5jb.client.payloads.umtx.impl1;

import org.ps5jb.client.payloads.umtx.common.DebugStatus;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.ErrNo;
import org.ps5jb.sdk.include.UniStd;
import org.ps5jb.sdk.lib.LibKernel;

class CheckMemoryPipe {
    private static final long CHECK_SIZE = 1;

    private static volatile CheckMemoryPipe INSTANCE;

    private final LibKernel libKernel;
    private final ErrNo errNo;

    private final int[] checkMemoryPipe;
    private final Pointer checkMemoryBuf;

    CheckMemoryPipe() throws SdkException {
        libKernel = new LibKernel();
        errNo = new ErrNo(libKernel);

        UniStd uniStd = new UniStd(libKernel);
        checkMemoryPipe = uniStd.pipe();
        checkMemoryBuf = Pointer.calloc(Config.MAX_PIPE_BUFFER_SIZE);
    }

    void free() {
        if (checkMemoryBuf != null) {
            checkMemoryBuf.free();
        }

        if (checkMemoryPipe != null && checkMemoryPipe.length > 1) {
            libKernel.close(checkMemoryPipe[0]);
            libKernel.close(checkMemoryPipe[1]);
        }

        libKernel.closeLibrary();
    }

    boolean checkMemoryAccessible(Pointer ptr) {
        long checkSize = CHECK_SIZE;
        long actualWriteSize = this.libKernel.write(checkMemoryPipe[1], ptr, checkSize);
        boolean result = actualWriteSize == checkSize;
        if (!result) {
            DebugStatus.error("Memory read check of size " + checkSize + " bytes failed for address " + ptr + "; Error code: " + this.errNo.getLastError() + "; Return value: " + actualWriteSize);
        }

        if (actualWriteSize > 0) {
            long actualReadSize = this.libKernel.read(checkMemoryPipe[0], checkMemoryBuf, checkSize);
            if (actualReadSize != actualWriteSize) {
                DebugStatus.error("Pipe read failed during memory check of address " + ptr + "; Error code: " + this.errNo.getLastError() + "; Return value: " + actualReadSize);
            }
        }

        return result;
    }

    static CheckMemoryPipe getInstance() {
        if (INSTANCE == null) {
            synchronized (CheckMemoryPipe.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = new CheckMemoryPipe();
                    } catch (SdkException e) {
                        throw new SdkRuntimeException(e);
                    }
                }
            }
        }
        return INSTANCE;
    }
}
