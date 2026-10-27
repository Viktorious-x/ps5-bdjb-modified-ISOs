package org.ps5jb.sdk.include.sys.internal.gc;

import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.PMap;
import org.ps5jb.sdk.include.machine.Param;
import org.ps5jb.sdk.include.machine.VmParam;
import org.ps5jb.sdk.include.sys.internal.gc.pmap.GpuPhysicalMapEntryMask;
import org.ps5jb.sdk.include.sys.internal.gc.vm.GpuVm;
import org.ps5jb.sdk.res.ErrorMessages;

public class GpuPMap {
    /** Size of direct memory pages that are mapped in GPU */
    public static long DIRECT_MEMORY_PAGE_SIZE = 0x200000L;
    /**
     * Offset of physical addresses in page tables
     * compared to their value as returned by {@link #gpu_vtophys(GpuVm, long)}.
     */
    public static long DIRECT_MEMORY_OFFSET = 0x100000000L;

    public static long gpu_pmap_valid_mask(GpuVm gpuVm) {
        return GpuPhysicalMapEntryMask.GPU_PG_V.value();
    }

    private static KernelPointer gpu_pmap_pml4e(GpuVm gpuVm, long va) {
        return new KernelPointer(gpuVm.getPageDirectoryVa() + PMap.pmap_pml4e_index(va) * 8, new Long(8),
                gpuVm.getPointer().isCacheKernelAccessor(), gpuVm.getPointer().getKernelAccessor());
    }

    private static KernelPointer gpu_pmap_pml4e_to_pdpe(KernelPointer pml4e, long va) {
        long pml4eFrame = pml4e.read8() & GpuPhysicalMapEntryMask.GPU_PG_FRAME.value();
        long pdpe = VmParam.PHYS_TO_DMAP(pml4eFrame);
        return new KernelPointer(pdpe + PMap.pmap_pdpe_index(va) * 8, new Long(8),
                pml4e.isCacheKernelAccessor(), pml4e.getKernelAccessor());
    }

    private static KernelPointer gpu_pmap_pdpe(GpuVm gpuVm, long va) {
        long PG_V = gpu_pmap_valid_mask(gpuVm);
        KernelPointer pml4e = gpu_pmap_pml4e(gpuVm, va);
        if ((pml4e.read8() & PG_V) == 0) {
            return KernelPointer.NULL;
        }
        return gpu_pmap_pml4e_to_pdpe(pml4e, va);
    }

    private static KernelPointer gpu_pmap_pdpe_to_pde(KernelPointer pdpe, long va) {
        long pdpeFrame = pdpe.read8() & GpuPhysicalMapEntryMask.GPU_PG_FRAME.value();
        long pde = VmParam.PHYS_TO_DMAP(pdpeFrame);
        return new KernelPointer(pde + PMap.pmap_pde_index(va) * 8, new Long(8),
                pdpe.isCacheKernelAccessor(), pdpe.getKernelAccessor());
    }

    public static KernelPointer gpu_pmap_pde(GpuVm gpuVm, long va) {
        long PG_V = gpu_pmap_valid_mask(gpuVm);
        KernelPointer pdpe = gpu_pmap_pdpe(gpuVm, va);
        if (KernelPointer.NULL.equals(pdpe) || (pdpe.read8() & PG_V) == 0) {
            return KernelPointer.NULL;
        }
        return gpu_pmap_pdpe_to_pde(pdpe, va);
    }

    private static KernelPointer gpu_pmap_pde_to_pte(KernelPointer pde, long va) {
        long pdeFrame = pde.read8() & GpuPhysicalMapEntryMask.GPU_PG_FRAME.value();
        long pte = VmParam.PHYS_TO_DMAP(pdeFrame);
        return new KernelPointer(pte + PMap.pmap_pte_index(va) * 8, new Long(8),
                pde.isCacheKernelAccessor(), pde.getKernelAccessor());
    }

    public static KernelPointer gpu_pmap_pte(GpuVm gpuVm, long va) {
        long PG_V = gpu_pmap_valid_mask(gpuVm);
        KernelPointer pde = gpu_pmap_pde(gpuVm, va);
        if (KernelPointer.NULL.equals(pde) || (pde.read8() & PG_V) == 0) {
            return KernelPointer.NULL;
        }
        return gpu_pmap_pde_to_pte(pde, va);
    }

    /**
     * Convert a virtual address mapped in the given GPU virtual memory space
     * to its physical address.
     *
     * @param gpuVm GPU virtual memory space whose page directory to use for lookup.
     * @param va Virtual address to translate.
     * @return Array where the first component is the determined physical address and
     *   the second component is the page size. May return <code>null</code> if the
     *   physical page is not present.
     * @throws SdkRuntimeException If the supplied virtual address is not in the range.
     */
    public static long[] gpu_vtophys(GpuVm gpuVm, long va) {
        long vmVaStart = gpuVm.getVaStart();
        if (vmVaStart > va) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(GpuPMap.class, "vaInvalid", "0x" + Long.toHexString(va)));
        }

        long vmVaEnd = gpuVm.getVaStart() + gpuVm.getVmSize();
        if (vmVaEnd <= va) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(GpuPMap.class, "vaInvalid", "0x" + Long.toHexString(va)));
        }

        KernelPointer pdAddr = new KernelPointer(gpuVm.getPageDirectoryVa(), null,
                gpuVm.getPointer().isCacheKernelAccessor(), gpuVm.getPointer().getKernelAccessor());
        if (KernelPointer.NULL.equals(pdAddr)) {
            return null;
        }

        long relativeVa = va - vmVaStart;
        KernelPointer pdeAddr = gpu_pmap_pde(gpuVm, relativeVa);
        if (KernelPointer.NULL.equals(pdeAddr)) {
            return null;
        }

        long pde = pdeAddr.read8();
        if ((pde & GpuPhysicalMapEntryMask.GPU_PG_54.value()) != 0) {
            return new long[] { pdeAddr.addr(), DIRECT_MEMORY_PAGE_SIZE };
        }

        // Note, everything below this line has not been tested.

        long PG_59 = GpuPhysicalMapEntryMask.GPU_PG_59.value();
        long PG_61 = GpuPhysicalMapEntryMask.GPU_PG_61.value();
        long PG_59_63 = ((1L << 5L) - 1L) << 59;
        if ((pde & PG_59_63) != PG_61) {
            if ((pde & PG_59_63) != PG_59) {
                return null;
            }

            long pdeFrame = pde & GpuPhysicalMapEntryMask.GPU_PG_FRAME.value();
            long pteBase = VmParam.PHYS_TO_DMAP(pdeFrame);
            long pteIndex = (relativeVa & Param.PDRMASK) >>> 0x0D;
            KernelPointer pteAddr = new KernelPointer(pteBase + pteIndex * 8, new Long(8),
                    gpuVm.getPointer().isCacheKernelAccessor(), gpuVm.getPointer().getKernelAccessor());
            return new long[] { pteAddr.addr(), 0x2000 };
        }

        long pdeFrame = pde & GpuPhysicalMapEntryMask.GPU_PG_FRAME.value();
        long pteBase = VmParam.PHYS_TO_DMAP(pdeFrame);
        long pteIndex = (relativeVa & Param.PDRMASK) >>> 0x10;
        KernelPointer pteAddr = new KernelPointer(pteBase + pteIndex * 8, new Long(8),
                gpuVm.getPointer().isCacheKernelAccessor(), gpuVm.getPointer().getKernelAccessor());
        long pte = pteAddr.read8();
        long PG_V_57 = gpu_pmap_valid_mask(gpuVm) | GpuPhysicalMapEntryMask.GPU_PG_57.value();
        if ((pte & PG_V_57) == PG_V_57) {
            long pteFrame = pte & GpuPhysicalMapEntryMask.GPU_PG_FRAME.value();
            long pa = VmParam.PHYS_TO_DMAP(pteFrame);
            long paIndex = (relativeVa >>> 13) & 7;
            KernelPointer paAddr = new KernelPointer(pa + paIndex * 8, new Long(8),
                    gpuVm.getPointer().isCacheKernelAccessor(), gpuVm.getPointer().getKernelAccessor());
            return new long[] { paAddr.addr(), 0x2000 };
        }

        return new long[] { pteAddr.addr(), 0x10000 };
    }
}
