package org.ps5jb.sdk.include.machine.pmap;

import java.util.ArrayList;
import java.util.List;

public class PhysicalMapEntryMask {
    /*
     * Page-directory and page-table entries follow this format, with a few
     * of the fields not present here and there, depending on a lot of things.
     */

    /** P Valid */
    public static final PhysicalMapEntryMask X86_PG_V = new PhysicalMapEntryMask(0x001, "X86_PG_V");
    /** R/W Read/Write */
    public static final PhysicalMapEntryMask X86_PG_RW = new PhysicalMapEntryMask(0x002, "X86_PG_RW");
    /** U/S User/Supervisor */
    public static final PhysicalMapEntryMask X86_PG_U = new PhysicalMapEntryMask(0x004, "X86_PG_U");
    /** PWT Write through */
    public static final PhysicalMapEntryMask X86_PG_NC_PWT = new PhysicalMapEntryMask(0x008, "X86_PG_NC_PWT");
    /** PCD Cache disable */
    public static final PhysicalMapEntryMask X86_PG_NC_PCD = new PhysicalMapEntryMask(0x010, "X86_PG_NC_PCD");
    /** A Accessed */
    public static final PhysicalMapEntryMask X86_PG_A = new PhysicalMapEntryMask(0x020, "X86_PG_A");
    /** D Dirty */
    public static final PhysicalMapEntryMask X86_PG_M = new PhysicalMapEntryMask(0x040, "X86_PG_M");
    /** PS Page size (0=4k,1=2M) */
    public static final PhysicalMapEntryMask X86_PG_PS = new PhysicalMapEntryMask(0x080, "X86_PG_PS");
    /** G Global */
    public static final PhysicalMapEntryMask X86_PG_G = new PhysicalMapEntryMask(0x100, "X86_PG_G");
    /** Proprietary flag used to check page validity */
    public static final PhysicalMapEntryMask SCE_PG_53 = new PhysicalMapEntryMask(1L << 53, "SCE_PG_53");
    /** Proprietary flag used to check page validity */
    public static final PhysicalMapEntryMask SCE_PG_55 = new PhysicalMapEntryMask(1L << 55, "SCE_PG_55");
    /** XO Execute-only */
    public static final PhysicalMapEntryMask SCE_PG_XO = new PhysicalMapEntryMask(1L << 58, "SCE_PG_XO");
    /** NX No-execute */
    public static final PhysicalMapEntryMask X86_PG_NX = new PhysicalMapEntryMask(1L << 63, "X86_PG_NX");

    /** "Wired" pseudoflag */
    public static final PhysicalMapEntryMask PG_W = new PhysicalMapEntryMask(0x800, "PG_W");
    public static final PhysicalMapEntryMask PG_MANAGED = new PhysicalMapEntryMask(0x400, "PG_MANAGED");
    public static final PhysicalMapEntryMask PG_PHYS_FRAME = new PhysicalMapEntryMask(0x000ffffffffff000L, "PG_PHYS_FRAME");
    public static final PhysicalMapEntryMask PG_FRAME = new PhysicalMapEntryMask(0x000fffffffffc000L, "PG_FRAME");
    public static final PhysicalMapEntryMask PG_PS_FRAME = new PhysicalMapEntryMask(0x000fffffffe00000L, "PG_PS_FRAME");

    /** All possible PhysicalMapEntryMask values. */
    private static final PhysicalMapEntryMask[] values = new PhysicalMapEntryMask[] {
            X86_PG_V,
            X86_PG_RW,
            X86_PG_U,
            X86_PG_NC_PWT,
            X86_PG_NC_PCD,
            X86_PG_A,
            X86_PG_M,
            X86_PG_PS,
            X86_PG_G,
            SCE_PG_53,
            SCE_PG_55,
            SCE_PG_XO,
            X86_PG_NX
    };

    private long value;

    private String name;

    /**
     * Default constructor. This class should not be instantiated manually,
     * use provided constants instead.
     *
     * @param value Numeric value of this instance.
     * @param name String representation of the constant.
     */
    private PhysicalMapEntryMask(long value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Convert a numeric value into an array of PhysicalMapEntryMask constants.
     *
     * @param value Number to convert
     * @return Array of PhysicalMapEntryMask constants ORed in the numeric value.
     */
    public static PhysicalMapEntryMask[] valueOf(long value) {
        List result = new ArrayList();
        for (PhysicalMapEntryMask flag : values) {
            if ((value & flag.value) == flag.value) {
                result.add(flag);
            }
        }

        return (PhysicalMapEntryMask[]) result.toArray(new PhysicalMapEntryMask[result.size()]);
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public long value() {
        return this.value;
    }

    @Override
    public String toString() {
        return name;
    }
}
