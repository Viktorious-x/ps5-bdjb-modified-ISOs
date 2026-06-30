package org.ps5jb.sdk.include.sys.fcntl;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Constants for file open modes.
 */
public final class OpenFlag implements Comparable {
    /** Open for reading only */
    public static final OpenFlag O_RDONLY = new OpenFlag(0x0000, "O_RDONLY");
    /** Open for writing only */
    public static final OpenFlag O_WRONLY = new OpenFlag(0x0001, "O_WRONLY");
    /** Open for reading and writing */
    public static final OpenFlag O_RDWR = new OpenFlag(0x0002, "O_RDWR");
    /** No delay */
    public static final OpenFlag O_NONBLOCK = new OpenFlag(0x0004, "O_NONBLOCK");
    /** Set append mode */
    public static final OpenFlag O_APPEND = new OpenFlag(0x0008, "O_APPEND");
    /** Open with shared file lock */
    public static final OpenFlag O_SHLOCK = new OpenFlag(0x0010, "O_SHLOCK");
    /** Open with exclusive file lock */
    public static final OpenFlag O_EXLOCK = new OpenFlag(0x0020, "O_EXLOCK");
    /** Synchronous writes */
    public static final OpenFlag O_FSYNC = new OpenFlag(0x0080, "O_FSYNC");
    /** POSIX synonym for {@link #O_FSYNC} */
    public static final OpenFlag O_SYNC = new OpenFlag(O_FSYNC.value(), "O_SYNC");;
    /** Don't follow symlinks */
    public static final OpenFlag O_NOFOLLOW = new OpenFlag(0x0100, "O_NOFOLLOW");
    /** Create if nonexistent */
    public static final OpenFlag O_CREAT = new OpenFlag(0x0200, "O_CREAT");
    /** Truncate to zero length */
    public static final OpenFlag O_TRUNC = new OpenFlag(0x0400, "O_TRUNC");
    /** Error if already exists */
    public static final OpenFlag O_EXCL = new OpenFlag(0x0800, "O_EXCL");
    /** Attempt to bypass buffer cache */
    public static final OpenFlag O_DIRECT = new OpenFlag(0x00010000, "O_DIRECT");
    /** Fail if not directory */
    public static final OpenFlag O_DIRECTORY = new OpenFlag(0x00020000, "O_DIRECTORY");
    /** Open for execute only */
    public static final OpenFlag O_EXEC = new OpenFlag(0x00040000, "O_EXEC");
    /** Set FD_CLOEXEC upon open */
    public static final OpenFlag O_CLOEXEC = new OpenFlag(0x00100000, "O_CLOEXEC");

    /** All possible OpenFlag values. */
    private static final OpenFlag[] values = new OpenFlag[] {
            O_RDONLY,
            O_WRONLY,
            O_RDWR,
            O_NONBLOCK,
            O_APPEND,
            O_SHLOCK,
            O_EXLOCK,
            O_FSYNC,
            O_SYNC,
            O_NOFOLLOW,
            O_CREAT,
            O_TRUNC,
            O_EXCL,
            O_DIRECT,
            O_DIRECTORY,
            O_EXEC,
            O_CLOEXEC
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
    private OpenFlag(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for OpenFlag.
     *
     * @return Array of OpenFlag possible values.
     */
    public static OpenFlag[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a OpenFlag constant.
     *
     * @param value Number to convert
     * @return OpenFlag constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any OpenFlag.
     */
    public static OpenFlag valueOf(int value) {
        for (OpenFlag openFlag : values) {
            if (value == openFlag.value()) {
                return openFlag;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(OpenFlag.class,"invalidValue", Integer.toString(value)));
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
    public static int or(OpenFlag... flags) {
        int result = 0;
        for (OpenFlag flag : flags) {
            result |= flag.value;
        }
        return result;
    }

    @Override
    public int compareTo(Object o) {
        return this.value - ((OpenFlag) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof OpenFlag) {
            result = value == ((OpenFlag) o).value;
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
