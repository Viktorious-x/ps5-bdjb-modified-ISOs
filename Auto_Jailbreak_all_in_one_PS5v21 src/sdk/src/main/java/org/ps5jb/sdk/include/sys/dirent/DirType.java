package org.ps5jb.sdk.include.sys.dirent;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Constants for file types in DirEntType structure
 */
public final class DirType implements Comparable {
    /** The type is unknown. Only some filesystems have full support to return the type of the file, others might always return this value. */
    public static final DirType DT_UNKNOWN = new DirType(0, "DT_UNKNOWN");
    /** A named pipe, or FIFO. */
    public static final DirType DT_FIFO = new DirType(1, "DT_FIFO");
    /** A character device. */
    public static final DirType DT_CHR = new DirType(2, "DT_CHR");
    /** A directory. */
    public static final DirType DT_DIR = new DirType(4, "DT_DIR");
    /** A block device. */
    public static final DirType DT_BLK = new DirType(6, "DT_BLK");
    /** A regular file. */
    public static final DirType DT_REG = new DirType(8, "DT_REG");
    /** A symbolic link. */
    public static final DirType DT_LNK = new DirType(10, "DT_LNK");
    /** A local-domain socket. */
    public static final DirType DT_SOCK = new DirType(12, "DT_SOCK");
    /** Whiteout.  */
    public static final DirType DT_WHT = new DirType(14, "DT_WHT");

    /** All possible CpuLevelType values. */
    private static final DirType[] values = new DirType[] {
            DT_UNKNOWN,
            DT_FIFO,
            DT_CHR,
            DT_DIR,
            DT_BLK,
            DT_REG,
            DT_LNK,
            DT_SOCK,
            DT_WHT

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
    private DirType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for DirType.
     *
     * @return Array of DirType possible values.
     */
    public static DirType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a DirType constant.
     *
     * @param value Number to convert
     * @return DirType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any DirType.
     */
    public static DirType valueOf(int value) {
        for (DirType dirType : values) {
            if (value == dirType.value()) {
                return dirType;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(DirType.class,"invalidValue", Integer.toString(value)));
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public int value() {
        return this.value;
    }

    /**
     * Combines an array of flags using a bitwise OR operator.
     *
     * @param flags Flags to combine.
     * @return Result of taking {@link #value()} of each flag and doing a bitwise OR operator on them.
     */
    public static int or(DirType... flags) {
        int result = 0;
        for (DirType flag : flags) {
            result |= flag.value;
        }
        return result;
    }

    @Override
    public int compareTo(Object o) {
        return this.value - ((DirType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof DirType) {
            result = value == ((DirType) o).value;
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
