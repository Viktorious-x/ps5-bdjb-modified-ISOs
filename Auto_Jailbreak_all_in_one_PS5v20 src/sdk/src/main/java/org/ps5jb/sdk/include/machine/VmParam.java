package org.ps5jb.sdk.include.machine;

/**
 * This class represents <code>include/machine/vmparam.h</code> from FreeBSD source.
 */
public class VmParam {
    public static final long VM_MIN_KERNEL_ADDRESS = PMap.KVADDR(PMap.KPML4BASE, 0, 0, 0);
    public static long DMAP_MIN_ADDRESS = PMap.KVADDR(PMap.DMPML4I, PMap.DMPDPI, 0, 0);
    public static long DMAP_MAX_ADDRESS = PMap.KVADDR(PMap.DMPML4I + PMap.NDMPML4E, 0, 0, 0);

    public static final long KERN_BASE = PMap.KVADDR(PMap.KPML4I, PMap.KPDPI, 0, 0);

    public static final long PHYS_TO_DMAP(long x) {
        return DMAP_MIN_ADDRESS + x;
    }

    public static final long DMAP_TO_PHYS(long x) {
        return x - DMAP_MIN_ADDRESS;
    }
}
