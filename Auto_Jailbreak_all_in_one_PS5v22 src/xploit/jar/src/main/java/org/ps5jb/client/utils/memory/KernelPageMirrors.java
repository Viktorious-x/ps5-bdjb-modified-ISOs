package org.ps5jb.client.utils.memory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.PMap;
import org.ps5jb.sdk.include.machine.Param;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMap;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMapEntryMask;
import org.ps5jb.sdk.include.sys.MMan;
import org.ps5jb.sdk.include.sys.errno.InvalidValueException;
import org.ps5jb.sdk.include.sys.mman.MappingFlag;
import org.ps5jb.sdk.include.sys.mman.ProtectionFlag;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.lib.LibKernel;
import org.ps5jb.client.utils.process.ProcessUtils;

/**
 * Class for managing mirrors of read-only kernel pages, mapped as read-write in user space.
 * Implementation is based on the equivalent C code from Byepervisor repository:
 * https://github.com/PS5Dev/Byepervisor/blob/main/src/mirror.cpp
 */
public class KernelPageMirrors {
    /**
     * Internal class for storing page mirror state
     */
    private static class PageMirror {
        /** User space address where the page is mapped */
        private Pointer userAddr;
        /** Kernel virtual address of the mapped page */
        private KernelPointer kernelVa;
        /** Kernel physical address of the mapped page */
        private long kernelPa;
        /** Original physical address of the user space address, before remapping */
        private long origPa;
    }

    private final Process curProc;
    private final LibKernel libKernel;
    private final MMan mman;

    private final Map mirroredPages;

    /**
     * Constructor
     *
     * @param curProc Address of the proc structure in kernel for the current process.
     *   Can be obtained using {@link ProcessUtils#getCurrentProcess()} or
     *   {@link org.ps5jb.client.utils.init.SdkInit#curProcAddress}.
     */
    public KernelPageMirrors(Process curProc) {
        this.libKernel = new LibKernel();
        this.mman = new MMan(this.libKernel);
        this.curProc = curProc;

        this.mirroredPages = new HashMap();
    }

    /**
     * Resets all the mapped mirrors and frees all the resources. After this operation,
     * this instance can no longer be used.
     */
    public void free() {
        reset();
        this.libKernel.closeLibrary();
    }

    /**
     * Creates a mirror page in user space of for a given kernel
     * virtual address.
     *
     * @param kernelVa Kernel virtual address to mirror.
     * @return State object containing the information about the created mirror.
     * @throws SdkException If kernel page could not be mapped.
     */
    private PageMirror mirrorPage(KernelPointer kernelVa) throws SdkException {
        PhysicalMap pmap = curProc.getVmSpace().getPhysicalMap();

        // Mask virtual address to page alignment and extract physical address
        long kernelPa = PMap.pmap_kextract(getKernelVaPageStart(kernelVa));

        // Map a user page
        final ProtectionFlag[] prot = new ProtectionFlag[] { ProtectionFlag.PROT_READ, ProtectionFlag.PROT_WRITE };
        final MappingFlag[] flags = new MappingFlag[] { MappingFlag.MAP_ANONYMOUS, MappingFlag.MAP_PRIVATE, MappingFlag.MAP_PREFAULT_READ };
        Pointer userMirror = mman.memoryMap(Pointer.NULL, Param.PAGE_SIZE, prot, flags, -1, 0);
        try {
            // Prefault page
            userMirror.write8(0x40404040L);
            userMirror.read8();

            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                // Ignore
            }

            long origPa = remapPage(pmap, userMirror.addr(), kernelPa);

            PageMirror pageMirror = new PageMirror();
            pageMirror.userAddr = userMirror;
            pageMirror.kernelVa = kernelVa;
            pageMirror.kernelPa = kernelPa;
            pageMirror.origPa = origPa;

            mirroredPages.put(new Long(kernelPa), pageMirror);

            return pageMirror;
        } catch (RuntimeException | Error e) {
            try {
                mman.memoryUnmap(userMirror, Param.PAGE_SIZE);
            } catch (Throwable e2) {
                Status.println("Unable to unmap mirrored page for " + kernelVa);
            }
            throw e;
        }
    }

    /**
     * Make sure the virtual address is aligned to page start.
     *
     * @param kernelVa Virtual address.
     * @return Same kernel pointer instance if it is already aligned to page start,
     *   or a new kernel pointer instance with the aligned address.
     */
    private KernelPointer getKernelVaPageStart(KernelPointer kernelVa) {
        long pageStartAddr = kernelVa.addr() & PhysicalMapEntryMask.PG_PHYS_FRAME.value();
        if (pageStartAddr == kernelVa.addr()) {
            return kernelVa;
        }
        return new KernelPointer(pageStartAddr, null,
                kernelVa.isCacheKernelAccessor(), kernelVa.getKernelAccessor());
    }

    /**
     * Convenience method to retrieve the user space pointer for a given kernel virtual address.
     * If a mirror already exists for the page containing this kernel address, it will be used.
     * Otherwise, a new mirror will be created.
     *
     * @param kernelVa Kernel virtual address to mirror.
     * @return User space address mapped to the given kernel page.
     * @throws SdkException If kernel page could not be mapped.
     */
    public Pointer getMirroredAddress(KernelPointer kernelVa) throws SdkException {
        long kernelPa = PMap.pmap_kextract(getKernelVaPageStart(kernelVa));
        PageMirror pageMirror = (PageMirror) mirroredPages.get(new Long(kernelPa));
        if (pageMirror == null) {
            pageMirror = mirrorPage(kernelVa);
        }
        return new Pointer(pageMirror.userAddr.addr() | (kernelVa.addr() & Param.PHYS_PAGE_MASK), new Long(Param.PHYS_PAGE_SIZE));
    }

    /**
     * Remaps a given user page to a new physical address.
     *
     * @param pmap Physical map of the current process.
     * @param va User space address to remap.
     * @param newPa New physical address to remap the page to.
     * @return Original physical address of the user page.
     */
    private long remapPage(PhysicalMap pmap, long va, long newPa) {
        KernelPointer pteAddr = PMap.pmap_pte(pmap, va);
        try {
            long pte = pteAddr.read8();
            long origPa = pte & PhysicalMapEntryMask.PG_PHYS_FRAME.value();
            long newPte = setPageTableAddress(pte, newPa);
            pteAddr.write8(newPte);
            return origPa;
        } catch (RuntimeException e) {
            Status.println("Unable to read PTE 0x" + Long.toHexString(pteAddr.addr()));
        }

        KernelPointer pdeAddr = PMap.pmap_pde(pmap, va);
        try {
            long pde = pdeAddr.read8();
            if ((pde & PhysicalMapEntryMask.X86_PG_PS.value()) != 0) {
                long origPa = pde & PhysicalMapEntryMask.PG_PHYS_FRAME.value();
                long newPde = setPageTableAddress(pde, newPa);
                pdeAddr.write8(newPde);
                return origPa;
            }
        } catch (RuntimeException e) {
            Status.println("Unable to read PDE 0x" + Long.toHexString(pteAddr.addr()));
        }

        throw new SdkRuntimeException("Remap of page 0x" + Long.toHexString(va) + " to 0x" + Long.toHexString(newPa) + " failed.");
    }

    /**
     * Changes the page table mask for the physical address.
     *
     * @param value Old page table value.
     * @param newAddr New address to set.
     * @return New page table value.
     */
    private long setPageTableAddress(long value, long newAddr) {
        long result = value & ~PhysicalMapEntryMask.PG_PHYS_FRAME.value();
        result |= (newAddr & PhysicalMapEntryMask.PG_PHYS_FRAME.value());
        return result;
    }

    /**
     * Unmaps and clears all active user space mirrors.
     */
    public void reset() {
        PhysicalMap pmap = curProc.getVmSpace().getPhysicalMap();

        Iterator mirrorsIter = mirroredPages.values().iterator();
        while (mirrorsIter.hasNext()) {
            PageMirror pageMirror = (PageMirror) mirrorsIter.next();
            remapPage(pmap, pageMirror.userAddr.addr(), pageMirror.origPa);
            try {
                mman.memoryUnmap(pageMirror.userAddr, Param.PAGE_SIZE);
            } catch (InvalidValueException | RuntimeException | Error e) {
                Status.println("Unable to unmap mirrored page for " + pageMirror.kernelVa);
            }
        }

        mirroredPages.clear();
    }
}
