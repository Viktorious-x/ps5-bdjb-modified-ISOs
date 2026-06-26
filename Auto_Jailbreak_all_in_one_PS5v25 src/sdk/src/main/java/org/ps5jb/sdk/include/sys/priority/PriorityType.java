package org.ps5jb.sdk.include.sys.priority;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Process priority specifications.
 */
public final class PriorityType implements Comparable {
    /** Interrupt thread. */
    public static final PriorityType PRI_ITHD = new PriorityType(1, "PRI_ITHD");
    /** Real time process. */
    public static final PriorityType PRI_REALTIME = new PriorityType(2, "PRI_REALTIME");
    /** Time sharing process. */
    public static final PriorityType PRI_TIMESHARE = new PriorityType(3, "PRI_TIMESHARE");
    /** Idle process. */
    public static final PriorityType PRI_IDLE = new PriorityType(4, "PRI_IDLE");

    public static final PriorityType PRI_FIFO_BIT = new PriorityType(8, "PRI_FIFO_BIT");
    public static final PriorityType PRI_FIFO = new PriorityType(PRI_FIFO_BIT.value() | PRI_REALTIME.value(), "PRI_FIFO");

    /** All possible PriorityType values. */
    private static final PriorityType[] values = new PriorityType[] {
            PRI_ITHD,
            PRI_REALTIME,
            PRI_TIMESHARE,
            PRI_IDLE,
            PRI_FIFO
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
    private PriorityType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for PriorityType.
     *
     * @return Array of PriorityType possible values.
     */
    public static PriorityType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a PriorityType constant.
     *
     * @param value Number to convert
     * @return PriorityType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any PriorityType.
     */
    public static PriorityType valueOf(int value) {
        for (PriorityType priorityType : values) {
            if (value == priorityType.value()) {
                return priorityType;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(PriorityType.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((PriorityType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof PriorityType) {
            result = value == ((PriorityType) o).value;
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
