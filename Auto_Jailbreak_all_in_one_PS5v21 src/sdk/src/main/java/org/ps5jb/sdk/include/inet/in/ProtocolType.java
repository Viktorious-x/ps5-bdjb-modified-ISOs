package org.ps5jb.sdk.include.inet.in;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Protocol types. Note that currently not all constants from FreeBSD are mapped.
 */
public final class ProtocolType implements Comparable {
    /* Protocols common to RFC 1700, POSIX, and X/Open. */

    /** dummy for IP. */
    public static final ProtocolType IPPROTO_IP = new ProtocolType(0, "IPPROTO_IP");
    /** control message protocol. */
    public static final ProtocolType IPPROTO_ICMP = new ProtocolType(1, "IPPROTO_ICMP");
    /** tcp. */
    public static final ProtocolType IPPROTO_TCP = new ProtocolType(6, "IPPROTO_TCP");
    /** user datagram protocol. */
    public static final ProtocolType IPPROTO_UDP = new ProtocolType(17, "IPPROTO_UDP");

    /** IP6 header. */
    public static final ProtocolType IPPROTO_IPV6 = new ProtocolType(41, "IPPROTO_IPV6");
    /** Raw IP packet. */
    public static final ProtocolType IPPROTO_RAW = new ProtocolType(255, "IPPROTO_RAW");

    /** All possible ProtocolType values. */
    private static final ProtocolType[] values = new ProtocolType[] {
            IPPROTO_IP,
            IPPROTO_ICMP,
            IPPROTO_TCP,
            IPPROTO_UDP,
            IPPROTO_IPV6,
            IPPROTO_RAW
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
    private ProtocolType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for ProtocolType.
     *
     * @return Array of ProtocolType possible values.
     */
    public static ProtocolType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a ProtocolType constant.
     *
     * @param value Number to convert
     * @return ProtocolType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any ProtocolType.
     */
    public static ProtocolType valueOf(int value) {
        for (ProtocolType protocolType : values) {
            if (value == protocolType.value()) {
                return protocolType;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(ProtocolType.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((ProtocolType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof ProtocolType) {
            result = value == ((ProtocolType) o).value;
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
