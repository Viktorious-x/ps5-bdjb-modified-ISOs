package org.ps5jb.sdk.include.sys.cpuset;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * FreeBSD <code>cpulevel_t</code> type.
 */
public final class CpuLevelType implements Comparable {
    /** All system cpus. */
    public static final CpuLevelType CPU_LEVEL_ROOT = new CpuLevelType(1, "CPU_LEVEL_ROOT");
    /** Available cpus for which. */
    public static final CpuLevelType CPU_LEVEL_CPUSET = new CpuLevelType(2, "CPU_LEVEL_CPUSET");
    /** Actual mask/id for which. */
    public static final CpuLevelType CPU_LEVEL_WHICH = new CpuLevelType(3, "CPU_LEVEL_WHICH");

    /** All possible CpuLevelType values. */
    private static final CpuLevelType[] values = new CpuLevelType[] {
            CPU_LEVEL_ROOT,
            CPU_LEVEL_CPUSET,
            CPU_LEVEL_WHICH
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
    private CpuLevelType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for CpuLevelType.
     *
     * @return Array of CpuLevelType possible values.
     */
    public static CpuLevelType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a CpuLevelType constant.
     *
     * @param value Number to convert
     * @return CpuLevelType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any CpuLevelType.
     */
    public static CpuLevelType valueOf(int value) {
        for (CpuLevelType cpuLevel : values) {
            if (value == cpuLevel.value()) {
                return cpuLevel;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(CpuLevelType.class,"invalidValue", Integer.toString(value)));
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public int value() {
        return this.value;
    }

    @Override
    public int compareTo(Object o) {
        return this.value - ((CpuLevelType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof CpuLevelType) {
            result = value == ((CpuLevelType) o).value;
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
