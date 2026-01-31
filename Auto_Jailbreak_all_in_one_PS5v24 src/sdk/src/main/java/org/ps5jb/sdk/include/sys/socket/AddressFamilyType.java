package org.ps5jb.sdk.include.sys.socket;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Address families. Note that currently not all constants from FreeBSD are mapped.
 */
public final class AddressFamilyType implements Comparable {
    /** Unspecified. */
    public static final AddressFamilyType AF_UNSPEC = new AddressFamilyType(0, "AF_UNSPEC");
    /** Standardized name for {@link #AF_LOCAL}. */
    public static final AddressFamilyType AF_UNIX = new AddressFamilyType(1, "AF_UNIX");
    /** Local to host (pipes, portals). */
    public static final AddressFamilyType AF_LOCAL = new AddressFamilyType(AF_UNIX.value, "AF_LOCAL");
    /** Internetwork: UDP, TCP, etc. */
    public static final AddressFamilyType AF_INET = new AddressFamilyType(2, "AF_INET");
    /** IPv6. */
    public static final AddressFamilyType AF_INET6 = new AddressFamilyType(28, "AF_INET6");

    /** All possible AddressFamilyType values. */
    private static final AddressFamilyType[] values = new AddressFamilyType[] {
            AF_UNSPEC,
            AF_UNIX,
            AF_LOCAL,
            AF_INET,
            AF_INET6
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
    private AddressFamilyType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for AddressFamilyType.
     *
     * @return Array of AddressFamilyType possible values.
     */
    public static AddressFamilyType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a AddressFamilyType constant.
     *
     * @param value Number to convert
     * @return AddressFamilyType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any AddressFamilyType.
     */
    public static AddressFamilyType valueOf(short value) {
        for (AddressFamilyType rtPrioType : values) {
            if (value == rtPrioType.value()) {
                return rtPrioType;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(AddressFamilyType.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((AddressFamilyType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof AddressFamilyType) {
            result = value == ((AddressFamilyType) o).value;
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
