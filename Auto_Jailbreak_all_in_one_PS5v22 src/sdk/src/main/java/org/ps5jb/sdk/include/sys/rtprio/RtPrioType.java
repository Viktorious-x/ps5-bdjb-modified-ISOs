package org.ps5jb.sdk.include.sys.rtprio;

/**
 * Wrapper for FreeBSD <code>rtprio</code> structure.
 */
public class RtPrioType {
    public static final short RTP_PRIO_MIN = 0;
    public static final short RTP_PRIO_MAX = 31;

    public static final int RTP_LOOKUP = 0;
    public static final int RTP_SET = 1;

    private SchedulingClass type;
    private short priority;

    /**
     * RtPrioType constructor.
     *
     * @param type Scheduling class.
     * @param priority Priority value.
     */
    public RtPrioType(SchedulingClass type, short priority) {
        this.type = type;
        this.priority = priority;
    }

    /**
     * @return Scheduling class.
     */
    public SchedulingClass getType() {
        return type;
    }

    /**
     * @return Priority value.
     */
    public short getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return getType().toString() + ": " + getPriority();
    }
}
