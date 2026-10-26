package org.ps5jb.sdk.include.sys.uio;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Uio operation values.
 */
public final class UioReadWrite implements Comparable {
    /** Read operation. */
    public static final UioReadWrite UIO_READ = new UioReadWrite(0, "UIO_READ");
    /** Write operation. */
    public static final UioReadWrite UIO_WRITE = new UioReadWrite(1, "UIO_WRITE");

    /** All possible UioReadWrite values. */
    private static final UioReadWrite[] values = new UioReadWrite[] {
            UIO_READ,
            UIO_WRITE
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
    private UioReadWrite(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for UioReadWrite.
     *
     * @return Array of UioReadWrite possible values.
     */
    public static UioReadWrite[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a UioReadWrite constant.
     *
     * @param value Number to convert
     * @return OpenFlag constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any UioReadWrite.
     */
    public static UioReadWrite valueOf(int value) {
        for (UioReadWrite urw : values) {
            if (value == urw.value()) {
                return urw;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(UioReadWrite.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((UioReadWrite) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof UioReadWrite) {
            result = value == ((UioReadWrite) o).value;
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
