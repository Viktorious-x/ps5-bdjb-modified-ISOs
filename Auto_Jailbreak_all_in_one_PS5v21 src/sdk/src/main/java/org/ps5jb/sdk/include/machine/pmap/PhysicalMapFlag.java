package org.ps5jb.sdk.include.machine.pmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Values for {@link PhysicalMap#getFlags()}.
 */
public final class PhysicalMapFlag implements Comparable {
    public static final PhysicalMapFlag PMAP_NESTED_IPIMASK = new PhysicalMapFlag(0xFF, "PMAP_NESTED_IPIMASK");
    /** Supports 2MB superpages. */
    public static final PhysicalMapFlag PMAP_PDE_SUPERPAGE = new PhysicalMapFlag(1 << 8, "PMAP_PDE_SUPERPAGE");
    /** Needs A/D bits emulation. */
    public static final PhysicalMapFlag PMAP_EMULATE_AD_BITS = new PhysicalMapFlag(1 << 9, "PMAP_EMULATE_AD_BITS");
    /** Execute only mappings ok. */
    public static final PhysicalMapFlag PMAP_SUPPORTS_EXEC_ONLY = new PhysicalMapFlag(1 << 10, "PMAP_SUPPORTS_EXEC_ONLY");

    /** All possible PageMapFlag values. */
    private static final PhysicalMapFlag[] values = new PhysicalMapFlag[] {
            PMAP_NESTED_IPIMASK,
            PMAP_PDE_SUPERPAGE,
            PMAP_EMULATE_AD_BITS,
            PMAP_SUPPORTS_EXEC_ONLY
    };

    private int value;

    private String name;

    /**
     * Default constructor. This class should not be instantiated manually,
     * use provided constants instead.
     *
     * @param value Numeric value of this instance.
     * @param name String representation of the constant.
     */
    private PhysicalMapFlag(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for PhysicalMapFlag.
     *
     * @return Array of PhysicalMapFlag possible values.
     */
    public static PhysicalMapFlag[] values() {
        return values;
    }

    /**
     * Convert a numeric value into an array of PhysicalMapFlag constants.
     *
     * @param value Number to convert
     * @return Array of PhysicalMapFlag constants ORed in the numeric value.
     */
    public static PhysicalMapFlag[] valueOf(int value) {
        List result = new ArrayList();
        for (PhysicalMapFlag flag : values) {
            if ((value & flag.value) == flag.value) {
                result.add(flag);
            }
        }

        return (PhysicalMapFlag[]) result.toArray(new PhysicalMapFlag[result.size()]);
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public int value() {
        return this.value;
    }

    /**
     * Combines an array of modes using a bitwise OR operator.
     *
     * @param flags Flags to combine.
     * @return Result of taking {@link #value()} of each mode and doing a bitwise OR operator on them.
     */
    public static short or(PhysicalMapFlag... flags) {
        short result = 0;
        for (PhysicalMapFlag flag : flags) {
            result |= flag.value;
        }
        return result;
    }

    @Override
    public int compareTo(Object o) {
        return this.value - ((PhysicalMapFlag) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof PhysicalMapFlag) {
            result = value == ((PhysicalMapFlag) o).value;
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }
}
