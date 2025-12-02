package org.ps5jb.sdk.include.netinet6.in6;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Options for use with getsockopt/setsockopt at the IPV6 level.
 * Note that currently not all constants from FreeBSD are mapped.
 */
public final class OptionIPv6 implements Comparable {
    /** buf/ip6_opts; set/get IP6 options. */
    public static final OptionIPv6 IPV6_OPTIONS = new OptionIPv6(1, "IPV6_OPTIONS");
    /** in6_pktinfo; send if, src addr. */
    public static final OptionIPv6 IPV6_PKTINFO = new OptionIPv6(46, "IPV6_PKTINFO");

    /** All possible OptionsIPv6 values. */
    private static final OptionIPv6[] values = new OptionIPv6[] {
            IPV6_OPTIONS,
            IPV6_PKTINFO
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
    private OptionIPv6(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for OptionIPv6.
     *
     * @return Array of OptionIPv6 possible values.
     */
    public static OptionIPv6[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a OptionIPv6 constant.
     *
     * @param value Number to convert
     * @return OptionIPv6 constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any OptionIPv6.
     */
    public static OptionIPv6 valueOf(int value) {
        for (OptionIPv6 opt : values) {
            if (value == opt.value()) {
                return opt;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(OptionIPv6.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((OptionIPv6) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof OptionIPv6) {
            result = value == ((OptionIPv6) o).value;
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
