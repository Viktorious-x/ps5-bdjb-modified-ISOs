package org.ps5jb.sdk.include.sys.stat;

import java.util.ArrayList;
import java.util.List;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Constants for file status mode values.
 */
public final class FileStatusMode implements Comparable {
    /** X for other */
    public static final FileStatusMode S_IXOTH = new FileStatusMode((short) 0000001, "S_IXOTH");
    /** W for other */
    public static final FileStatusMode S_IWOTH = new FileStatusMode((short) 0000002, "S_IWOTH");
    /** R for other */
    public static final FileStatusMode S_IROTH = new FileStatusMode((short) 0000004, "S_IROTH");
    /** X for group */
    public static final FileStatusMode S_IXGRP = new FileStatusMode((short) 0000010, "S_IXGRP");
    /** W for group */
    public static final FileStatusMode S_IWGRP = new FileStatusMode((short) 0000020, "S_IWGRP");
    /** R for group */
    public static final FileStatusMode S_IRGRP = new FileStatusMode((short) 0000040, "S_IRGRP");
    /** X for owner */
    public static final FileStatusMode S_IXUSR = new FileStatusMode((short) 0000100, "S_IXUSR");
    /** W for owner */
    public static final FileStatusMode S_IWUSR = new FileStatusMode((short) 0000200, "S_IWUSR");
    /** R for owner */
    public static final FileStatusMode S_IRUSR = new FileStatusMode((short) 0000400, "S_IRUSR");
    /** Save swapped text even after use */
    public static final FileStatusMode S_ISVTX = new FileStatusMode((short) 0001000, "S_ISVTX");
    /** Sticky bit */
    public static final FileStatusMode S_ISTXT = new FileStatusMode((short) 0001000, "S_ISTXT");
    /** Set group id on execution */
    public static final FileStatusMode S_ISGID = new FileStatusMode((short) 0002000, "S_ISGID");
    /** Set user id on execution */
    public static final FileStatusMode S_ISUID = new FileStatusMode((short) 0004000, "S_ISUID");
    /** Whiteout */
    public static final FileStatusMode S_IFWHT = new FileStatusMode((short) 0160000, "S_IFWHT");
    /** Socket */
    public static final FileStatusMode S_IFSOCK = new FileStatusMode((short) 0140000, "S_IFSOCK");
    /** Symbolic link */
    public static final FileStatusMode S_IFLNK = new FileStatusMode((short) 0120000, "S_IFLNK");
    /** Regular */
    public static final FileStatusMode S_IFREG = new FileStatusMode((short) 0100000, "S_IFREG");
    /** Block special */
    public static final FileStatusMode S_IFBLK = new FileStatusMode((short) 0060000, "S_IFBLK");
    /** Directory */
    public static final FileStatusMode S_IFDIR = new FileStatusMode((short) 0040000, "S_IFDIR");
    /** Character special */
    public static final FileStatusMode S_IFCHR = new FileStatusMode((short) 0020000, "S_IFCHR");
    /** Named pipe (fifo) */
    public static final FileStatusMode S_IFIFO = new FileStatusMode((short) 0010000, "S_IFIFO");

    /** RWX mask for owner */
    public static final FileStatusMode S_IRWXU = new FileStatusMode((short) 0000700, "S_IRWXU");

    /** RWX mask for group */
    public static final FileStatusMode S_IRWXG = new FileStatusMode((short) 0000070, "S_IRWXG");

    /** RWX mask for other */
    public static final FileStatusMode S_IRWXO = new FileStatusMode((short) 0000007, "S_IRWXO");

    /** Type of file mask */
    public static final FileStatusMode S_IFMT = new FileStatusMode((short) 0170000, "S_IFMT");

    /** All possible FileStatusMode values. */
    private static final FileStatusMode[] values = new FileStatusMode[] {
            S_IXOTH,
            S_IWOTH,
            S_IROTH,
            S_IXGRP,
            S_IWGRP,
            S_IRGRP,
            S_IXUSR,
            S_IWUSR,
            S_IRUSR,
            S_ISVTX,
            S_ISTXT,
            S_ISGID,
            S_ISUID,
            S_IFWHT,
            S_IFSOCK,
            S_IFLNK,
            S_IFREG,
            S_IFBLK,
            S_IFDIR,
            S_IFCHR,
            S_IFIFO
    };

    private final short value;

    private final String name;

    /**
     * Default constructor. This class should not be instantiated manually,
     * use provided constants instead.
     *
     * @param value Numeric value of this instance.
     * @param name String representation of the constant.
     */
    private FileStatusMode(short value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for FileStatusMode.
     *
     * @return Array of FileStatusMode possible values.
     */
    public static FileStatusMode[] values() {
        return values;
    }

    /**
     * Convert a numeric value into an array of FileStatusMode constants.
     *
     * @param value Number to convert
     * @return Array of FileStatusMode constants ORed in the numeric value.
     */
    public static FileStatusMode[] valueOf(short value) {
        List result = new ArrayList();
        for (FileStatusMode mode : values) {
            if ((value & mode.value) == mode.value) {
                result.add(mode);
            }
        }

        return (FileStatusMode[]) result.toArray(new FileStatusMode[result.size()]);
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public short value() {
        return this.value;
    }

    /**
     * Combines an array of modes using a bitwise OR operator.
     *
     * @param modes Modes to combine.
     * @return Result of taking {@link #value()} of each mode and doing a bitwise OR operator on them.
     */
    public static short or(FileStatusMode... modes) {
        short result = 0;
        for (FileStatusMode mode : modes) {
            result |= mode.value;
        }
        return result;
    }

    @Override
    public int compareTo(Object o) {
        return this.value - ((FileStatusMode) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof FileStatusMode) {
            result = value == ((FileStatusMode) o).value;
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
