package org.ps5jb.sdk.include.sys.internal.gc.vm;

import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Wrapper for FreeBSD Kernel structure representing GPU virtual memory address space.
 */
public class GpuVm {
    /** Maximum number of possible GPU address spaces. */
    public static final int MAX_VM_COUNT = 16;
    /** Value of {@link #getVmId()} when the space is not allocated. */
    private static final int UNALLOCATED_VMID = 32;

    public static final long OFFSET_GVM_ID = 0;
    public static final long OFFSET_GVM_VA_START = OFFSET_GVM_ID + 8L;
    public static final long OFFSET_GVM_VM_SIZE = OFFSET_GVM_VA_START + 8L;
    public static final long OFFSET_GVM_PAGE_DIRECTORY_PA = 40L;
    /** On VM allocation, this field is initialized to the same value as {@link #OFFSET_GVM_PAGE_DIRECTORY_PA}. */
    public static final long OFFSET_GVM_UNKN = OFFSET_GVM_PAGE_DIRECTORY_PA + 8L;
    public static final long OFFSET_GVM_PAGE_DIRECTORY_VA = OFFSET_GVM_UNKN + 8L;
    public static final long OFFSET_GVM_PID = OFFSET_GVM_PAGE_DIRECTORY_VA + 8L;
    public static final long OFFSET_GVM_PROC = OFFSET_GVM_PID + 8L;

    /**
     * Firmware-dependent size of GpuVm structure. If -1, then the size could not be determined
     * for the current firmware version.
     */
    public static long SIZE = -1;

    private final KernelPointer ptr;

    /**
     * GpuVm constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing GpuVm data.
     *   When the first instance of GpuVm is constructed, the structure size
     *   detection is performed. To do so, the {@link KernelPointer#size()}
     *   of this pointer should be <code>NULL</code>.
     * @throws IndexOutOfBoundsException If <code>ptr</code> size is too
     *   small to perform GpuVm size detection.
     */
    public GpuVm(KernelPointer ptr) {
        this.ptr = ptr;
        detectGpuVmSize();
    }

    /**
     * VM ID.
     *
     * @return Returns the VM id of this address space.
     */
    public int getVmId() {
        return this.ptr.read4(OFFSET_GVM_ID);
    }

    /**
     * VM space size.
     *
     * @return Returns the size of this address space.
     */
    public long getVmSize() {
        return this.ptr.read8(OFFSET_GVM_VM_SIZE);
    }

    /**
     * Virtual address start.
     *
     * @return Returns the start of the virtual addresses for this space.
     */
    public long getVaStart() {
        return this.ptr.read8(OFFSET_GVM_VA_START);
    }

    /**
     * Virtual address of the level 4 page table.
     *
     * @return Virtual address of the level 4 page table.
     */
    public long getPageDirectoryVa() {
        return this.ptr.read8(OFFSET_GVM_PAGE_DIRECTORY_VA);
    }

    /**
     * Physical address of the level 4 page table.
     *
     * @return Physical address of the level 4 page table.
     */
    public long getPageDirectoryPa() {
        return this.ptr.read8(OFFSET_GVM_PAGE_DIRECTORY_PA);
    }

    /**
     * Pid.
     *
     * @return Returns the id of the process associated with this space.
     */
    public int getPid() {
        return this.ptr.read4(OFFSET_GVM_PID);
    }

    /**
     * Process.
     *
     * @return Returns the process associated with this space.
     *   If null, it's a kernel process or the address space
     *   is not allocated.
     */
    public Process getProcess() {
        Process result = null;
        KernelPointer ptr = this.ptr.pptr(OFFSET_GVM_PROC);
        if (!KernelPointer.NULL.equals(ptr)) {
            result = new Process(ptr);
        }
        return result;
    }

    /**
     * Check whether the given address space has been allocated.
     *
     * @return True if address space is allocated and has an ID; false otherwise.
     */
    public boolean isAllocated() {
        int id = getVmId();
        return id >= 0 && id < MAX_VM_COUNT;
    }

    /**
     * Gets the native memory pointer where this GpuVm's data is stored.
     *
     * @return GpuVm memory pointer.
     */
    public KernelPointer getPointer() {
        return this.ptr;
    }

    /**
     * Size of GpuVm structure is firmware dependent. This method does a one-time detection of this value.
     */
    private void detectGpuVmSize() {
        if (SIZE == -1) {
            synchronized (GpuVm.class) {
                if (SIZE == -1) {
                    // On fw 1.xx, the size of the struct is one long shorter.
                    // The core idea of this detection is to read that value.
                    // For smaller-size struct, this value should be the vmid
                    // of the next space, value of UNALLOCATED_VMID,
                    // or, in case of read on the last vm space some other data.
                    long nextScanOffset = 0xF8;
                    int nextInt = this.ptr.read4(nextScanOffset);
                    if ((nextInt == UNALLOCATED_VMID) || (nextInt == (getVmId() + 1))) {
                        SIZE = nextScanOffset;
                    } else {
                        nextScanOffset += 8L;
                        nextInt = this.ptr.read4(nextScanOffset);
                        if ((nextInt == UNALLOCATED_VMID) || (nextInt == (getVmId() + 1))) {
                            SIZE = nextScanOffset;
                        }
                    }
                }
            }

            if (SIZE == -1) {
                throw new SdkSoftwareVersionUnsupportedException(ErrorMessages.getClassErrorMessage(GpuVm.class,"sizeGpuVmUnknown"));
            }
        }
    }
}
