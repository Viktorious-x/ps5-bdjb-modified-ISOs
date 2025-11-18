package org.ps5jb.sdk.include.sys.internal.gc.pmap;

import java.util.ArrayList;
import java.util.List;

public class GpuPhysicalMapEntryMask {
    /*
     * Page-directory and page-table entries mapped to the GPU follow this (partial) format.
     */

    /** P Valid */
    public static final GpuPhysicalMapEntryMask GPU_PG_V = new GpuPhysicalMapEntryMask(0x001, "GPU_PG_V");
    public static final GpuPhysicalMapEntryMask GPU_PG_54 = new GpuPhysicalMapEntryMask(1L << 54, "GPU_PG_54");
    public static final GpuPhysicalMapEntryMask GPU_PG_57 = new GpuPhysicalMapEntryMask(1L << 57, "GPU_PG_57");
    public static final GpuPhysicalMapEntryMask GPU_PG_59 = new GpuPhysicalMapEntryMask(1L << 59, "GPU_PG_59");
    public static final GpuPhysicalMapEntryMask GPU_PG_61 = new GpuPhysicalMapEntryMask(1L << 61, "GPU_PG_61");

    public static final GpuPhysicalMapEntryMask GPU_PG_FRAME = new GpuPhysicalMapEntryMask(0xFFFFFFFFFFC0L, "PG_FRAME");

    /** All known GpuPhysicalMapEntryMask values. */
    private static final GpuPhysicalMapEntryMask[] values = new GpuPhysicalMapEntryMask[] {
            GPU_PG_V,
            GPU_PG_54,
            GPU_PG_57,
            GPU_PG_59,
            GPU_PG_61
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
    private GpuPhysicalMapEntryMask(long value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Convert a numeric value into an array of GpuPhysicalMapEntryMask constants.
     *
     * @param value Number to convert
     * @return Array of GpuPhysicalMapEntryMask constants ORed in the numeric value.
     */
    public static GpuPhysicalMapEntryMask[] valueOf(long value) {
        List result = new ArrayList();
        for (GpuPhysicalMapEntryMask flag : values) {
            if ((value & flag.value) == flag.value) {
                result.add(flag);
            }
        }

        return (GpuPhysicalMapEntryMask[]) result.toArray(new GpuPhysicalMapEntryMask[result.size()]);
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
