package org.ps5jb.sdk.include.sys.cpuset;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.include.sys.bitset.BitSetType;

/**
 * FreeBSD <code>cpuset_t</code> type.
 */
public class CpuSetType extends BitSetType {
    /** Size in bits of the cpuset_t native data type */
    public static final int CPU_SETSIZE = 128;

    /**
     * Constructor for CPU set. Allocates the native memory
     * used to store its value. When this cpu set is no longer
     * needed, the resources should be freed by calling {@link #free()}.
     */
    public CpuSetType() {
        super(Pointer.calloc(CPU_SETSIZE / 8), CPU_SETSIZE);
    }

    /**
     * Make sure to free the CPU set buffer during garbage collection.
     *
     * @throws Throwable If finalization failed.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            free();
        } finally {
            super.finalize();
        }
    }

    /**
     * Frees the native memory needed for the CPU set. After <code>free()</code> is called,
     * using this Java wrapper instance will no longer be possible.
     */
    public void free() {
        if (getPointer() != null && getPointer().addr() != 0) {
            getPointer().free();
        }
    }
}
