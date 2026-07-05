package org.ps5jb.sdk.include.sys.uio;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Segment flag values.
 */
public final class UioSegmentFlag implements Comparable {
    /** From user data space. */
    public static final UioSegmentFlag UIO_USERSPACE = new UioSegmentFlag(0, "UIO_USERSPACE");
    /** From system space. */
    public static final UioSegmentFlag UIO_SYSSPACE = new UioSegmentFlag(1, "UIO_SYSSPACE");
    /** Don't copy, already in object. */
    public static final UioSegmentFlag UIO_NOCOPY = new UioSegmentFlag(2, "UIO_NOCOPY");

    /** All possible UioSegmentFlag values. */
    private static final UioSegmentFlag[] values = new UioSegmentFlag[] {
            UIO_USERSPACE,
            UIO_SYSSPACE,
            UIO_NOCOPY
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
    private UioSegmentFlag(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for UioSegmentFlag.
     *
     * @return Array of UioSegmentFlag possible values.
     */
    public static UioSegmentFlag[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a UioSegmentFlag constant.
     *
     * @param value Number to convert
     * @return OpenFlag constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any UioSegmentFlag.
     */
    public static UioSegmentFlag valueOf(int value) {
        for (UioSegmentFlag segFlag : values) {
            if (value == segFlag.value()) {
                return segFlag;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(UioSegmentFlag.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((UioSegmentFlag) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof UioSegmentFlag) {
            result = value == ((UioSegmentFlag) o).value;
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
