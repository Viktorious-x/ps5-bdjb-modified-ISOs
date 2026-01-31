package org.ps5jb.sdk.include.vm.map;

import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMap;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Incomplete wrapper for FreeBSD <code>vmspace</code> structure.
 */
public class VmSpace {
    public static final long OFFSET_VM_MAP = 0;
    /** Firmware-dependent offset to pmap from the start of the VmSpace pointer. If -1, then the offset could not be determined for the current firmware version. */
    public static long OFFSET_VM_PMAP = -1;
    /** Firmware-dependent offset to GPU vmid from the start of the VmSpace pointer. If -1, then the offset could not be determined for the current firmware version. */
    public static long OFFSET_GPU_VMID = -1;

    private final KernelPointer ptr;

    /**
     * VmSpace constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing VmSpace data.
     */
    public VmSpace(KernelPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * VM address map.
     *
     * @return Returns the value of <code>vm_map</code> field of <code>vmspace</code> structure.
     */
    public KernelPointer getMap() {
        return this.ptr.pptr(OFFSET_VM_MAP);
    }

    /**
     * Regular or nested tables.
     *
     * @return Returns the value of <code>vm_pmap</code> field of <code>pmap</code> structure.
     */
    public PhysicalMap getPhysicalMap() {
        detectVmPmapOffset();
        return new PhysicalMap(getPointer().inc(OFFSET_VM_PMAP));
    }

    /**
     * GPU VM space id.
     *
     * @return Returns the id of the GPU vm space assigned to this process.
     */
    public int getGpuVmId() {
        detectGpuVmIdOffset();
        return this.ptr.read4(OFFSET_GPU_VMID);
    }

    /**
     * Gets the native memory pointer where this VmSpace's data is stored.
     *
     * @return VmSpace memory pointer.
     */
    public KernelPointer getPointer() {
        return this.ptr;
    }

    /**
     * Offset to <code>vm_pmap</code> is firmware dependent. This method does a one-time detection
     * of this value.
     */
    private void detectVmPmapOffset() {
        if (OFFSET_VM_PMAP == -1) {
            synchronized (VmSpace.class) {
                if (OFFSET_VM_PMAP == -1) {
                    // Note, this is the offset of vm_space.vm_map.pmap on 1.xx.
                    // It is assumed that on higher firmwares it's only increasing.
                    long curScanOffset = 0x1C8;
                    for (int i = 0; i < 5; ++i) {
                        long scanVal = this.ptr.read8(curScanOffset + i * 8L);
                        long offsetDiff = scanVal - this.ptr.addr();
                        // See if the value is an offset within VmSpace structure.
                        // The expected offset range below is confirmed to be accurate up to 7.xx.
                        if (offsetDiff >= 0x2C0 && offsetDiff <= 0x2F0) {
                            OFFSET_VM_PMAP = offsetDiff;
                            break;
                        }
                    }
                }
            }

            if (OFFSET_VM_PMAP == -1) {
                throw new SdkSoftwareVersionUnsupportedException(ErrorMessages.getClassErrorMessage(VmSpace.class,"offsetVmPmapUnknown"));
            }
        }
    }

    /**
     * Offset to GPU vmid is firmware dependent. This method does a one-time detection of this value.
     */
    private void detectGpuVmIdOffset() {
        if (OFFSET_GPU_VMID == -1) {
            synchronized (VmSpace.class) {
                if (OFFSET_GPU_VMID == -1) {
                    // Note, this is the offset of vm_space.vm_map.pmap on 1.xx.
                    // It is assumed that on higher firmwares it's only increasing.
                    long curScanOffset = 0x1D4;
                    for (int i = 0; i < 7; ++i) {
                        long scanOffset = curScanOffset + i * 4L;
                        int scanVal = this.ptr.read4(scanOffset);
                        if (scanVal > 0 && scanVal < 0x10) {
                            OFFSET_GPU_VMID = scanOffset;
                            break;
                        }
                    }
                }
            }

            if (OFFSET_GPU_VMID == -1) {
                throw new SdkSoftwareVersionUnsupportedException(ErrorMessages.getClassErrorMessage(VmSpace.class,"offsetGpuVmIdUnknown"));
            }
        }
    }
}
