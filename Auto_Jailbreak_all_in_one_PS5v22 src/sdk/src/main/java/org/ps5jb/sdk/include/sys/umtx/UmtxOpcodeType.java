package org.ps5jb.sdk.include.sys.umtx;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * (Partial) op codes for <code>_umtx_op</code> syscall.
 */
public final class UmtxOpcodeType implements Comparable {
    /**
     * Manage anonymous POSIX shared memory objects (see <code>shm_open</code>).
     * On FreeBSD, the value of this constant is <code>25</code>.
     */
    public static final UmtxOpcodeType UMTX_OP_SHM = new UmtxOpcodeType(26, "UMTX_OP_SHM");

    /** All possible UmtxOpcodeType values. */
    private static final UmtxOpcodeType[] values = new UmtxOpcodeType[] {
            UMTX_OP_SHM
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
    private UmtxOpcodeType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for UmtxOpcodeType.
     *
     * @return Array of UmtxOpcodeType possible values.
     */
    public static UmtxOpcodeType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a UmtxOpcodeType constant.
     *
     * @param value Number to convert
     * @return UmtxOpcodeType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any UmtxOpcodeType.
     */
    public static UmtxOpcodeType valueOf(int value) {
        for (UmtxOpcodeType opcodeType : values) {
            if (value == opcodeType.value()) {
                return opcodeType;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(UmtxOpcodeType.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((UmtxOpcodeType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof UmtxOpcodeType) {
            result = value == ((UmtxOpcodeType) o).value;
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
