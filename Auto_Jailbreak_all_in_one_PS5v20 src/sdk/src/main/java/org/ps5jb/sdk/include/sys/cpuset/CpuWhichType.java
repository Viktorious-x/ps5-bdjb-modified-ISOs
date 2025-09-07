package org.ps5jb.sdk.include.sys.cpuset;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * FreeBSD <code>cpuwhich_t</code> type.
 */
public final class CpuWhichType implements Comparable {
    /** Specifies a thread id. */
    public static final CpuWhichType CPU_WHICH_TID = new CpuWhichType(1, "CPU_WHICH_TID");
    /** Specifies a process id. */
    public static final CpuWhichType CPU_WHICH_PID = new CpuWhichType(2, "CPU_WHICH_PID");
    /** Specifies a set id. */
    public static final CpuWhichType CPU_WHICH_CPUSET = new CpuWhichType(3, "CPU_WHICH_CPUSET");
    /** Specifies an irq #. */
    public static final CpuWhichType CPU_WHICH_IRQ = new CpuWhichType(4, "CPU_WHICH_IRQ");
    /** Specifies a jail id. */
    public static final CpuWhichType CPU_WHICH_JAIL = new CpuWhichType(5, "CPU_WHICH_JAIL");
    /** Specifies a NUMA domain id. */
    public static final CpuWhichType CPU_WHICH_DOMAIN = new CpuWhichType(6, "CPU_WHICH_DOMAIN");

    /** All possible CpuWhichType values. */
    private static final CpuWhichType[] values = new CpuWhichType[] {
            CPU_WHICH_TID,
            CPU_WHICH_PID,
            CPU_WHICH_CPUSET,
            CPU_WHICH_IRQ,
            CPU_WHICH_JAIL,
            CPU_WHICH_DOMAIN
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
    private CpuWhichType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for CpuWhichType.
     *
     * @return Array of CpuWhichType possible values.
     */
    public static CpuWhichType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a CpuWhichType constant.
     *
     * @param value Number to convert
     * @return CpuWhichType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any CpuWhichType.
     */
    public static CpuWhichType valueOf(int value) {
        for (CpuWhichType cpuWhich : values) {
            if (value == cpuWhich.value()) {
                return cpuWhich;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(CpuWhichType.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((CpuWhichType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof CpuWhichType) {
            result = value == ((CpuWhichType) o).value;
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
