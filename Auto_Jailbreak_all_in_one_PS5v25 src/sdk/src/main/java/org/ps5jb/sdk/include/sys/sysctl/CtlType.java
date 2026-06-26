package org.ps5jb.sdk.include.sys.sysctl;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Top-level identifiers.
 */
public final class CtlType implements Comparable {
    /** Unused */
    public static final CtlType CTL_UNSPEC = new CtlType(0, "CTL_UNSPEC");
    /** "High kernel": proc, limits */
    public static final CtlType CTL_KERN = new CtlType(1, "CTL_KERN");
    /** Virtual memory */
    public static final CtlType CTL_VM = new CtlType(2, "CTL_VM");
    /** Filesystem, mount type is next */
    public static final CtlType CTL_VFS = new CtlType(3, "CTL_VFS");
    /** Network, see socket.h */
    public static final CtlType CTL_NET = new CtlType(4, "CTL_NET");
    /** Debugging parameters */
    public static final CtlType CTL_DEBUG = new CtlType(5, "CTL_DEBUG");
    /** Generic cpu/io */
    public static final CtlType CTL_HW = new CtlType(6, "CTL_HW");
    /** Machine dependent */
    public static final CtlType CTL_MACHDEP = new CtlType(7, "CTL_MACHDEP");
    /** User-level */
    public static final CtlType CTL_USER = new CtlType(8, "CTL_USER");
    /** POSIX 1003.1B */
    public static final CtlType CTL_P1003_1B = new CtlType(9, "CTL_P1003_1B");

    /** All possible CtlType values. */
    private static final CtlType[] values = new CtlType[] {
            CTL_UNSPEC,
            CTL_KERN,
            CTL_VM,
            CTL_VFS,
            CTL_NET,
            CTL_DEBUG,
            CTL_HW,
            CTL_MACHDEP,
            CTL_USER,
            CTL_P1003_1B
    };

    private final int value;

    private final String name;

    /**
     * Default constructor. This class should not be instantiated manually,
     * use provided constants instead.
     *
     * @param value Numeric value of this instance.
     * @param name String representation of the constant.
     */
    private CtlType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for CtlType.
     *
     * @return Array of CtlType possible values.
     */
    public static CtlType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a CtlType constant.
     *
     * @param value Number to convert
     * @return CtlType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any CtlType.
     */
    public static CtlType valueOf(short value) {
        for (CtlType ctl : values) {
            if (value == ctl.value()) {
                return ctl;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(CtlType.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((CtlType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof CtlType) {
            result = value == ((CtlType) o).value;
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
