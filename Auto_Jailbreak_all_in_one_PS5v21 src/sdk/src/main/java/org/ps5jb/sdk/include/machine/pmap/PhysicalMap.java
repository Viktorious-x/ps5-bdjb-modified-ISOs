package org.ps5jb.sdk.include.machine.pmap;

import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.mutex.MutexType;

/**
 * Incomplete wrapper for FreeBSD <code>pmap</code> structure.
 */
public class PhysicalMap {
    public static final long OFFSET_PM_MTX = 0;
    public static final long OFFSET_PM_PML4 = OFFSET_PM_MTX + MutexType.SIZE;
    public static final long OFFSET_PM_CR3 = OFFSET_PM_PML4 + 8;
    public static final long OFFSET_PM_TYPE = 72;
    public static final long OFFSET_PM_FLAGS = 120;

    private final KernelPointer ptr;
    private MutexType mutex;

    /**
     * PhysicalMap constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing PhysicalMap data.
     */
    public PhysicalMap(KernelPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Mutex.
     *
     * @return Returns the value of <code>pm_mtx</code> field of <code>pmap</code> structure.
     */
    public MutexType getMutex() {
        if (mutex == null) {
            mutex = new MutexType(new KernelPointer(ptr.addr() + OFFSET_PM_MTX, new Long(MutexType.SIZE),
                    ptr.isCacheKernelAccessor(), ptr.getKernelAccessor()));
        }
        return mutex;
    }

    /**
     * KVA of level 4 page table.
     *
     * @return Returns the value of <code>pm_pml4</code> field of <code>pmap</code> structure.
     */
    public long getPml4() {
        return this.ptr.read8(OFFSET_PM_PML4);
    }

    /**
     * Physical address of level 4 page table.
     *
     * @return Returns the value of <code>pm_cr3</code> field of <code>pmap</code> structure.
     */
    public long getCr3() {
        return this.ptr.read8(OFFSET_PM_CR3);
    }

    /**
     * Regular or nested tables.
     *
     * @return Returns the value of <code>pm_type</code> field of <code>pmap</code> structure.
     */
    public PhysicalMapType getType() {
        return PhysicalMapType.valueOf(this.ptr.read4(OFFSET_PM_TYPE));
    }

    /**
     * Flags.
     *
     * @return Returns the value of <code>pm_flags</code> field of <code>pmap</code> structure.
     */
    public PhysicalMapFlag[] getFlags() {
        return PhysicalMapFlag.valueOf(this.ptr.read4(OFFSET_PM_FLAGS));
    }

    /**
     * Gets the native memory pointer where this PhysicalMap's data is stored.
     *
     * @return PhysicalMap memory pointer.
     */
    public KernelPointer getPointer() {
        return this.ptr;
    }
}
