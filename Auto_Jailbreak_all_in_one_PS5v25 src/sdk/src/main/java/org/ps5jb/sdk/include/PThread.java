package org.ps5jb.sdk.include;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.include.sys.ErrNo;
import org.ps5jb.sdk.include.sys.pthreadtypes.PThreadType;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/pthread.h</code> from FreeBSD source.
 */
public class PThread {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public PThread(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    /**
     * Get thread ID of the calling thread.
     *
     * @return Thread ID of the calling thread.
     */
    public PThreadType self() {
        return new PThreadType(libKernel.pthread_self());
    }

    /**
     * Returns the value currently bound to the specified key on behalf of the calling thread.
     *
     * @param key Key to retrieve.
     * @return Thread-specific data value associated with the given key.
     *   If no thread-specific data value is associated with key,
     *   then the value {@link Pointer#NULL} is returned.
     */
    public Pointer getSpecific(int key) {
        return libKernel.pthread_getspecific(key);
    }
}
