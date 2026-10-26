package org.ps5jb.sdk.include.sys.rtprio;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Process realtime-priority specifications to rtprio.
 */
public final class SchedulingClass implements Comparable {
    /** Real time process. */
    public static final SchedulingClass RTP_PRIO_REALTIME = new SchedulingClass((short) 2, "RTP_PRIO_REALTIME");
    /** Time sharing process. */
    public static final SchedulingClass RTP_PRIO_NORMAL = new SchedulingClass((short) 3, "RTP_PRIO_NORMAL");
    /** Idle process. */
    public static final SchedulingClass RTP_PRIO_IDLE = new SchedulingClass((short) 4, "RTP_PRIO_IDLE");
    /** RTP_PRIO_FIFO is POSIX.1B SCHED_FIFO. */
    public static final SchedulingClass RTP_PRIO_FIFO = new SchedulingClass((short) (8 | RTP_PRIO_REALTIME.value), "RTP_PRIO_FIFO");

    /** All possible SchedulingClass values. */
    private static final SchedulingClass[] values = new SchedulingClass[] {
            RTP_PRIO_REALTIME,
            RTP_PRIO_NORMAL,
            RTP_PRIO_IDLE,
            RTP_PRIO_FIFO
    };

    private short value;

    private String name;

    /**
     * Default constructor. This class should not be instantiated manually,
     * use provided constants instead.
     *
     * @param value Numeric value of this instance.
     * @param name String representation of the constant.
     */
    private SchedulingClass(short value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for SchedulingClass.
     *
     * @return Array of SchedulingClass possible values.
     */
    public static SchedulingClass[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a SchedulingClass constant.
     *
     * @param value Number to convert
     * @return SchedulingClass constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any SchedulingClass.
     */
    public static SchedulingClass valueOf(short value) {
        for (SchedulingClass rtPrioType : values) {
            if (value == rtPrioType.value()) {
                return rtPrioType;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(SchedulingClass.class,"invalidValue", Integer.toString(value)));
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public short value() {
        return this.value;
    }

    @Override
    public int compareTo(Object o) {
        return this.value - ((SchedulingClass) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof SchedulingClass) {
            result = value == ((SchedulingClass) o).value;
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
