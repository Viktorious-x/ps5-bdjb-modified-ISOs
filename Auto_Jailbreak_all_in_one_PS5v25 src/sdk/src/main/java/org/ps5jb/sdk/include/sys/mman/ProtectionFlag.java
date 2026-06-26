package org.ps5jb.sdk.include.sys.mman;

import java.util.ArrayList;
import java.util.List;

/**
 * Constants for memory protection flags.
 */
public final class ProtectionFlag implements Comparable {
    /** Pages may not be accessed */
    public static final ProtectionFlag PROT_NONE = new ProtectionFlag(0x0000, "PROT_NONE");
    /** Pages may be read */
    public static final ProtectionFlag PROT_READ = new ProtectionFlag(0x0001, "PROT_READ");
    /** Pages may be written */
    public static final ProtectionFlag PROT_WRITE = new ProtectionFlag(0x0002, "PROT_WRITE");
    /** Pages may be executed */
    public static final ProtectionFlag PROT_EXEC = new ProtectionFlag(0x0004, "PROT_EXEC");
    /** Pages may be read by GPU */
    public static final ProtectionFlag PROT_GPU_READ = new ProtectionFlag(0x0010, "PROT_GPU_READ");
    /** Pages may be written by GPU */
    public static final ProtectionFlag PROT_GPU_WRITE = new ProtectionFlag(0x0020, "PROT_GPU_WRITE");

    /** All possible ProtectionFlag values. */
    private static final ProtectionFlag[] values = new ProtectionFlag[] {
            PROT_NONE,
            PROT_READ,
            PROT_WRITE,
            PROT_EXEC,
            PROT_GPU_READ,
            PROT_GPU_WRITE
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
    private ProtectionFlag(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for ProtectionFlag.
     *
     * @return Array of ProtectionFlag possible values.
     */
    public static ProtectionFlag[] values() {
        return values;
    }

    /**
     * Convert a numeric value into an array of ProtectionFlag constants.
     *
     * @param value Number to convert
     * @return Array of ProtectionFlag constants ORed in the numeric value.
     */
    public static ProtectionFlag[] valueOf(int value) {
        List result = new ArrayList();
        for (ProtectionFlag flag : values) {
            if (!PROT_NONE.equals(flag) && ((value & flag.value) == flag.value)) {
                result.add(flag);
            }
        }

        if (result.isEmpty()) {
            result.add(PROT_NONE);
        }

        return (ProtectionFlag[]) result.toArray(new ProtectionFlag[result.size()]);
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
     * Combines an array of flags using a bitwise OR operator.
     *
     * @param flags Flags to combine.
     * @return Result of taking {@link #value()} of each flag and doing a bitwise OR operator on them.
     */
    public static int or(ProtectionFlag... flags) {
        int result = 0;
        for (ProtectionFlag flag : flags) {
            result |= flag.value;
        }
        return result;
    }

    @Override
    public int compareTo(Object o) {
        return this.value - ((ProtectionFlag) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof ProtectionFlag) {
            result = value == ((ProtectionFlag) o).value;
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
