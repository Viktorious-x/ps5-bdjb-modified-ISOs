package org.ps5jb.sdk.include.sys.socket;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Socket types
 */
public final class SocketType implements Comparable {
    /** Stream socket. */
    public static final SocketType SOCK_STREAM = new SocketType(1, "SOCK_STREAM");
    /** Datagram socket. */
    public static final SocketType SOCK_DGRAM = new SocketType(2, "SOCK_DGRAM");
    /** Raw-protocol interface. */
    public static final SocketType SOCK_RAW = new SocketType(3, "SOCK_RAW");
    /** Reliably-delivered message. */
    public static final SocketType SOCK_RDM = new SocketType(4, "SOCK_RDM");
    /** Sequenced packet stream. */
    public static final SocketType SOCK_SEQPACKET = new SocketType(5, "SOCK_SEQPACKET");

    /** All possible SocketType values. */
    private static final SocketType[] values = new SocketType[] {
            SOCK_STREAM,
            SOCK_DGRAM,
            SOCK_RAW,
            SOCK_RDM,
            SOCK_SEQPACKET
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
    private SocketType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for SocketType.
     *
     * @return Array of SocketType possible values.
     */
    public static SocketType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a SocketType constant.
     *
     * @param value Number to convert
     * @return SocketType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any SocketType.
     */
    public static SocketType valueOf(int value) {
        for (SocketType socketType : values) {
            if (value == socketType.value()) {
                return socketType;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(SocketType.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((SocketType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof SocketType) {
            result = value == ((SocketType) o).value;
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
