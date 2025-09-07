package org.ps5jb.sdk.include.sys.pthreadtypes;

import org.ps5jb.sdk.core.Pointer;

/**
 * Wrapper for FreeBSD <code>pthread_t</code> structure.
 */
public class PThreadType {
    private Pointer pthread;

    /**
     * PThreadType constructor.
     *
     * @param pthread Native address of <code>pthread_t</code> structure.
     */
    public PThreadType(Pointer pthread) {
        this.pthread = pthread;
    }

    /**
     * @return PThread structure native memory address.
     */
    public Pointer getPthread() {
        return pthread;
    }
}
