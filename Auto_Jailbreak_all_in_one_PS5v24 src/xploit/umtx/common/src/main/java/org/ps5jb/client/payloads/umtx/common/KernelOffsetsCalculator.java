package org.ps5jb.client.payloads.umtx.common;

import java.nio.charset.Charset;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;
import org.ps5jb.sdk.core.kernel.KernelOffsets;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.VmParam;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.include.sys.proc.Thread;

/**
 * Calculator of important absolute kernel addresses.
 */
public class KernelOffsetsCalculator {
    private static final long OFFSET_FDESCENTTBL_FDT_OFILES = 0x08L;

    public static final int MAX_RECLAIM_THREAD_NAME = 0x10;
    /**
     * Number of times kernel thread's heap pointer should occur in kernel stack to
     * distinguish it from other values on stack.
     */
    public static final int DEFAULT_KERNEL_THREAD_POINTER_OCCURRENCE_THRESHOLD = 10;

    /** Start of kernel .text */
    public KernelPointer kernelAddressBase = KernelPointer.NULL;
    /** Start of kernel .data */
    public KernelPointer kernelDataBase = KernelPointer.NULL;
    /** struct thread of the reclaim thread */
    public KernelPointer threadAddress = KernelPointer.NULL;
    /** struct proc of BD-J process */
    public KernelPointer processAddress = KernelPointer.NULL;
    /** allproc pointer in kernel data */
    public KernelPointer allProcAddress = KernelPointer.NULL;
    /** Value of ofiles pointer in the process structure */
    public KernelPointer processOpenFilesAddress = KernelPointer.NULL;

    /**
     * Default constructor
     */
    public KernelOffsetsCalculator() {
    }

    /**
     * Calculates important kernel addresses. Full capabilities
     * are based on firmware, but some addresses are populated
     * in a firmware-agnostic fashion.
     *
     * @param swVer Software version in form 0x[MAJOR][MINOR].
     * @param classifier Kernel address klassifier created right after UMTX exploit completed.
     * @param reclaimThreadName Name of the winner reclaim thread to use for
     *   verification of correct detection of the {@link #threadAddress}.
     *   This name should have been set using
     *   {@link org.ps5jb.sdk.lib.LibKernel#pthread_rename_np(Pointer, String)}
     *   and should not exceed {@link #MAX_RECLAIM_THREAD_NAME}.
     * @return True if at least some addresses could be calculated.
     */
    public boolean calculate(int swVer, KernelAddressClassifier classifier, String reclaimThreadName) {
        boolean result = false;

        // Note, there is also a way of doing it from the cookie
        final Long potentialThreadAddress = classifier.getMostOccuredHeapAddress(DEFAULT_KERNEL_THREAD_POINTER_OCCURRENCE_THRESHOLD);
        if (potentialThreadAddress != null) {
            final KernelPointer threadAddressPtr = KernelPointer.valueOf(potentialThreadAddress.longValue(), false);
            final String threadNameCheck = threadAddressPtr.readString(Thread.OFFSET_TD_NAME, new Integer(MAX_RECLAIM_THREAD_NAME - 1), Charset.defaultCharset().name());
            if (threadNameCheck.equals(reclaimThreadName)) {
                threadAddress = threadAddressPtr;
                processAddress = KernelPointer.valueOf(threadAddress.read8(Thread.OFFSET_TD_PROC), false);

                final KernelPointer p_fd = KernelPointer.valueOf(processAddress.read8(Process.OFFSET_P_FD), false);
                processOpenFilesAddress = KernelPointer.valueOf(p_fd.read8() + OFFSET_FDESCENTTBL_FDT_OFILES, false);

                allProcAddress = calculateAllProcAddress(processAddress);

                try {
                    final KernelOffsets kernelOffsets = new KernelOffsets(swVer);
                    kernelDataBase = allProcAddress.inc(-kernelOffsets.OFFSET_KERNEL_DATA_BASE_ALLPROC);
                    kernelAddressBase = kernelDataBase.inc(-kernelOffsets.OFFSET_KERNEL_DATA);
                } catch (SdkSoftwareVersionUnsupportedException e) {
                    // Ignore KASLR not fully defeated
                }

                result = true;
            }
        }

        return result;
    }

    /**
     * Traverse the process list to get to allproc.
     *
     * @param processAddress Current process address.
     * @return Address of allproc pointer in kernel data section.
     */
    private KernelPointer calculateAllProcAddress(KernelPointer processAddress) {
        KernelPointer allproc = processAddress;

        final long KDATA_MASK = VmParam.VM_MIN_KERNEL_ADDRESS;

        while (!KernelPointer.NULL.equals(allproc) && ((allproc.addr() & KDATA_MASK) != KDATA_MASK)) {
            try {
                allproc = KernelPointer.valueOf(allproc.read8(0x8), false); // proc->p_list->le_prev
            } catch (IllegalAccessError e) {
                // Ignore
                allproc = KernelPointer.NULL;
            }
        }

        return allproc;
    }
}
