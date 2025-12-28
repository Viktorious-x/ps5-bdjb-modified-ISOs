package org.ps5jb.sdk.include.machine.pmap;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * FreeBSD <code>pmap_type</code> enum.
 */
public final class PhysicalMapType implements Comparable {
    /** Regular x86 page tables. */
    public static final PhysicalMapType PT_X86 = new PhysicalMapType(0, "PT_X86");
    /** AMD's nested page tables. */
    public static final PhysicalMapType PT_RVI = new PhysicalMapType(2, "PT_RVI");

    /** All possible PhysicalMapType values. */
    private static final PhysicalMapType[] values = new PhysicalMapType[] {
            PT_X86,
            PT_RVI
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
    private PhysicalMapType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for PhysicalMapType.
     *
     * @return Array of PhysicalMapType possible values.
     */
    public static PhysicalMapType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a PhysicalMapType constant.
     *
     * @param value Number to convert
     * @return PhysicalMapType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any PhysicalMapType.
     */
    public static PhysicalMapType valueOf(int value) {
        for (PhysicalMapType type : values) {
            if (value == type.value()) {
                return type;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(PhysicalMapType.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((PhysicalMapType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof PhysicalMapType) {
            result = value == ((PhysicalMapType) o).value;
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
