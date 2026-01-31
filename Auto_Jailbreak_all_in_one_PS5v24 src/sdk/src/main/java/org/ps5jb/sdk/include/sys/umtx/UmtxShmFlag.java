package org.ps5jb.sdk.include.sys.umtx;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Flags for {@link UmtxOpcodeType#UMTX_OP_SHM} opcode.
 */
public final class UmtxShmFlag implements Comparable {
    /**
     * Creates the anonymous shared memory object. If the object associated with the key already exists,
     * it is returned instead of creating a new object.
     */
    public static final UmtxShmFlag UMTX_SHM_CREAT = new UmtxShmFlag(0x0001, "UMTX_SHM_CREAT");
    /**
     * Same as {@link #UMTX_SHM_CREAT}, but if there is no shared memory object associated with the specified key,
     * an error is returned, and no new object is created.
     */
    public static final UmtxShmFlag UMTX_SHM_LOOKUP = new UmtxShmFlag(0x0002, "UMTX_SHM_LOOKUP");
    /**
     * De-associates the shared object with the specified key. The object is destroyed after
     * the last open file descriptor is closed and the last mapping for it is destroyed.
     */
    public static final UmtxShmFlag UMTX_SHM_DESTROY = new UmtxShmFlag(0x0004, "UMTX_SHM_DESTROY");
    /** Checks whether there is a live shared object associated with the supplied key. */
    public static final UmtxShmFlag UMTX_SHM_ALIVE = new UmtxShmFlag(0x0008, "UMTX_SHM_ALIVE");

    /** All possible UmtxShmFlag values. */
    private static final UmtxShmFlag[] values = new UmtxShmFlag[] {
            UMTX_SHM_CREAT,
            UMTX_SHM_LOOKUP,
            UMTX_SHM_DESTROY,
            UMTX_SHM_ALIVE
    };

    private long value;

    private String name;

    /**
     * Default constructor. This class should not be instantiated manually,
     * use provided constants instead.
     *
     * @param value Numeric value of this instance.
     * @param name String representation of the constant.
     */
    private UmtxShmFlag(long value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for UmtxShmFlag.
     *
     * @return Array of UmtxShmFlag possible values.
     */
    public static UmtxShmFlag[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a UmtxShmFlag constant.
     *
     * @param value Number to convert
     * @return UmtxShmFlag constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any UmtxShmFlag.
     */
    public static UmtxShmFlag valueOf(long value) {
        for (UmtxShmFlag shmFlag : values) {
            if (value == shmFlag.value()) {
                return shmFlag;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(UmtxShmFlag.class,"invalidValue", Long.toString(value)));
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public long value() {
        return this.value;
    }

    @Override
    public int compareTo(Object o) {
        return ((int) this.value) - ((int) ((UmtxShmFlag) o).value);
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof UmtxShmFlag) {
            result = value == ((UmtxShmFlag) o).value;
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    @Override
    public String toString() {
        return name;
    }
}
