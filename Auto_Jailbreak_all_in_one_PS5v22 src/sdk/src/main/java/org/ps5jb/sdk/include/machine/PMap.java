package org.ps5jb.sdk.include.machine;

import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMap;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMapEntryMask;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMapType;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * This class represents <code>include/machine/pmap.h</code> from FreeBSD source
 * and also borrows a few helpful static functions from <code>sys/amd64/amd64/pmap.c</code>.
 *
 * Note that some of the methods in this class are not usable until
 * {@link #refresh(KernelPointer, KernelPointer, KernelPointer)} is called
 * and dynamic offsets are properly updated.
 */
public class PMap {
    /** Maximum number of PML4 entries that will be used to implement the direct map. */
    public static final long NDMPML4E = 1;

    /** Number of kernel PML4 slots. */
    public static final long NKPML4E = 1;

    /** Index of recursive pml4 mapping. */
    public static long PML4PML4I = Param.NPML4EPG / 2;

    /** KVM at highest addresses. */
    public static final long KPML4BASE = Param.NPML4EPG - NKPML4E;

    /** Below KVM. */
    public static long DMPML4I = org.ps5jb.sdk.include.sys.Param.rounddown(KPML4BASE - NDMPML4E, NDMPML4E);

    public static long DMPDPI = 0;

    public static final long KPML4I = Param.NPML4EPG - 1;

    public static final long KPDPI = Param.NPDPEPG - 2;

    public static long addr_PTmap = KVADDR(PML4PML4I, 0, 0, 0);
    public static long addr_PDmap = KVADDR(PML4PML4I, PML4PML4I, 0, 0);
    public static long addr_PDPmap = KVADDR(PML4PML4I, PML4PML4I, PML4PML4I, 0);
    public static long addr_PML4map = KVADDR(PML4PML4I, PML4PML4I, PML4PML4I, PML4PML4I);
    public static long addr_PML4pml4e = addr_PML4map + (PML4PML4I * 8);

    /**
     * Kernel randomizes some parameters at the start. The initial values are simply FreeBSD defaults.
     * Call this method with pointers to the kernel variables storing randomized values
     * to calculate actual variable values.
     *
     * @param dmpmk4iAddress Address of {@link #DMPML4I} value.
     * @param dmpdpiAddress Address of {@link #DMPDPI} value.
     * @param pml4pml4iAddress Address of {@link #PML4PML4I} value.
     */
    public static void refresh(KernelPointer dmpmk4iAddress, KernelPointer dmpdpiAddress, KernelPointer pml4pml4iAddress) {
        DMPML4I = dmpmk4iAddress.read4();
        DMPDPI = dmpdpiAddress.read4();
        PML4PML4I = pml4pml4iAddress.read4();
        addr_PTmap = KVADDR(PML4PML4I, 0, 0, 0);
        addr_PDmap = KVADDR(PML4PML4I, PML4PML4I, 0, 0);
        addr_PDPmap = KVADDR(PML4PML4I, PML4PML4I, PML4PML4I, 0);
        addr_PML4map = KVADDR(PML4PML4I, PML4PML4I, PML4PML4I, PML4PML4I);
        addr_PML4pml4e = addr_PML4map + (PML4PML4I * 8);
        VmParam.DMAP_MIN_ADDRESS = KVADDR(DMPML4I, DMPDPI, 0, 0);
        VmParam.DMAP_MAX_ADDRESS = KVADDR(DMPML4I + NDMPML4E, 0, 0, 0);
    }

    public static long KVADDR(long p14, long p13, long p12, long p11) {
        return (-1L << 47) |
               (p14 << Param.PML4SHIFT) |
               (p13 << Param.PDPSHIFT) |
               (p12 << Param.PDRSHIFT) |
               (p11 << Param.PHYS_PAGE_SHIFT);
    }

    private static PhysicalMapEntryMask pmap_valid_bit(PhysicalMap pmap) {
        PhysicalMapEntryMask mask;
        if (pmap.getType().equals(PhysicalMapType.PT_X86) || pmap.getType().equals(PhysicalMapType.PT_RVI)) {
            mask = PhysicalMapEntryMask.X86_PG_V;
        } else {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(PMap.class, "pmapBitValid", pmap.getType()));
        }
        return mask;
    }

    public static long pmap_valid_mask(PhysicalMap pmap) {
        return pmap_valid_bit(pmap).value() | PhysicalMapEntryMask.SCE_PG_53.value() | PhysicalMapEntryMask.SCE_PG_55.value();
    }

    public static long pmap_pte_index(long va) {
        return ((va >>> Param.PHYS_PAGE_SHIFT) & ((1L << Param.NPTEPGSHIFT) - 1));
    }

    public static long pmap_pde_index(long va) {
        return (va >>> Param.PDRSHIFT) & ((1L << Param.NPDEPGSHIFT) - 1);
    }

    public static long pmap_pdpe_index(long va) {
        return (va >>> Param.PDPSHIFT) & ((1L << Param.NPDPEPGSHIFT) - 1);
    }

    public static long pmap_pml4e_index(long va) {
        return (va >>> Param.PML4SHIFT) & ((1L << Param.NPML4EPGSHIFT) - 1);
    }

    private static KernelPointer pmap_pml4e(PhysicalMap pmap, long va) {
        return new KernelPointer(pmap.getPml4() + pmap_pml4e_index(va) * 8, new Long(8),
                pmap.getPointer().isCacheKernelAccessor(), pmap.getPointer().getKernelAccessor());
    }

    private static KernelPointer pmap_pml4e_to_pdpe(KernelPointer pml4e, long va) {
        long pml4eFrame = pml4e.read8() & PhysicalMapEntryMask.PG_PHYS_FRAME.value();
        long pdpe = VmParam.PHYS_TO_DMAP(pml4eFrame);
        return new KernelPointer(pdpe + pmap_pdpe_index(va) * 8, new Long(8),
                pml4e.isCacheKernelAccessor(), pml4e.getKernelAccessor());
    }

    private static KernelPointer pmap_pdpe(PhysicalMap pmap, long va) {
        long PG_V = pmap_valid_mask(pmap);
        KernelPointer pml4e = pmap_pml4e(pmap, va);
        if ((pml4e.read8() & PG_V) == 0) {
            return KernelPointer.NULL;
        }
        return pmap_pml4e_to_pdpe(pml4e, va);
    }

    private static KernelPointer pmap_pdpe_to_pde(KernelPointer pdpe, long va) {
        long pdpeFrame = pdpe.read8() & PhysicalMapEntryMask.PG_PHYS_FRAME.value();
        long pde = VmParam.PHYS_TO_DMAP(pdpeFrame);
        return new KernelPointer(pde + pmap_pde_index(va) * 8, new Long(8),
                pdpe.isCacheKernelAccessor(), pdpe.getKernelAccessor());
    }

    public static KernelPointer pmap_pde(PhysicalMap pmap, long va) {
        long PG_V = pmap_valid_mask(pmap);
        KernelPointer pdpe = pmap_pdpe(pmap, va);
        if (KernelPointer.NULL.equals(pdpe) || (pdpe.read8() & PG_V) == 0) {
            return KernelPointer.NULL;
        }
        return pmap_pdpe_to_pde(pdpe, va);
    }

    private static KernelPointer pmap_pde_to_pte(KernelPointer pde, long va) {
        long pdeFrame = pde.read8() & PhysicalMapEntryMask.PG_PHYS_FRAME.value();
        long pte = VmParam.PHYS_TO_DMAP(pdeFrame);
        return new KernelPointer(pte + pmap_pte_index(va) * 8, new Long(8),
                pde.isCacheKernelAccessor(), pde.getKernelAccessor());
    }

    public static KernelPointer pmap_pte(PhysicalMap pmap, long va) {
        long PG_V = pmap_valid_mask(pmap);
        KernelPointer pde = pmap_pde(pmap, va);
        if (KernelPointer.NULL.equals(pde) || (pde.read8() & PG_V) == 0) {
            return KernelPointer.NULL;
        }
        return pmap_pde_to_pte(pde, va);
    }

    private static KernelPointer vtopde(KernelPointer va) {
        long mask = (1L << (Param.NPDEPGSHIFT + Param.NPDPEPGSHIFT + Param.NPML4EPGSHIFT)) - 1;
        return new KernelPointer(addr_PDmap + ((va.addr() >> Param.PDRSHIFT) & mask) * 8, new Long(8),
                va.isCacheKernelAccessor(), va.getKernelAccessor());
    }

    private static KernelPointer vtopte(KernelPointer va) {
        long mask = ((1L << (Param.NPTEPGSHIFT + Param.NPDEPGSHIFT + Param.NPDPEPGSHIFT + Param.NPML4EPGSHIFT)) - 1);
        return new KernelPointer(addr_PTmap + ((va.addr() >> Param.PHYS_PAGE_SHIFT) & mask) * 8, new Long(8),
                va.isCacheKernelAccessor(), va.getKernelAccessor());
    }

    public static long pmap_kextract(KernelPointer va) {
        long pa;
        long vaAddr = va.addr();
        if (vaAddr >= VmParam.DMAP_MIN_ADDRESS && vaAddr < VmParam.DMAP_MAX_ADDRESS) {
            pa = VmParam.DMAP_TO_PHYS(vaAddr);
        } else {
            KernelPointer pdeAddr = vtopde(va);
            long pde = pdeAddr.read8();
            if ((pde & PhysicalMapEntryMask.X86_PG_PS.value()) != 0) {
                pa = (pde & PhysicalMapEntryMask.PG_PS_FRAME.value()) | (vaAddr & Param.PDRMASK);
            } else {
                pa = pmap_pde_to_pte(pdeAddr, vaAddr).read8();
                pa = (pa & PhysicalMapEntryMask.PG_PHYS_FRAME.value()) | (vaAddr & Param.PAGE_MASK);
            }
        }
        return pa;
    }
}
