package org.ps5jb.sdk.include.sys.stat;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.include.sys.timespec.TimespecType;

/**
 * Wrapper for FreeBSD Kernel <code>stat</code> structure.
 */
public class StatType {
    public static final long OFFSET_ST_DEV = 0L;
    public static final long OFFSET_ST_INO = OFFSET_ST_DEV + 4;
    public static final long OFFSET_ST_MODE = OFFSET_ST_INO + 4;
    public static final long OFFSET_ST_NLINK = OFFSET_ST_MODE + 2;
    public static final long OFFSET_ST_UID = OFFSET_ST_NLINK + 2;
    public static final long OFFSET_ST_GID = OFFSET_ST_UID + 4;
    public static final long OFFSET_ST_RDEV = OFFSET_ST_GID + 4;
    public static final long OFFSET_ST_ATIM = OFFSET_ST_RDEV + 4;
    public static final long OFFSET_ST_MTIM = OFFSET_ST_ATIM + TimespecType.SIZE;
    public static final long OFFSET_ST_CTIM = OFFSET_ST_MTIM + TimespecType.SIZE;
    public static final long OFFSET_ST_SIZE = OFFSET_ST_CTIM + TimespecType.SIZE;
    public static final long OFFSET_ST_BLOCKS = OFFSET_ST_SIZE + 8;
    public static final long OFFSET_ST_BLKSIZE = OFFSET_ST_BLOCKS + 8;
    public static final long OFFSET_ST_FLAGS = OFFSET_ST_BLKSIZE + 4;
    public static final long OFFSET_ST_GEN = OFFSET_ST_FLAGS + 4;
    public static final long OFFSET_ST_LSPARE = OFFSET_ST_GEN + 4;
    public static final long OFFSET_ST_BIRTHTIM = OFFSET_ST_LSPARE + 4;
    public static final long SIZE = OFFSET_ST_BIRTHTIM + TimespecType.SIZE;

    private final Pointer ptr;
    private final boolean ownPtr;

    private TimespecType atim;
    private TimespecType mtim;
    private TimespecType ctim;
    private TimespecType birthtim;

    /**
     * StatType default constructor.
     */
    public StatType() {
        this.ptr = Pointer.calloc(SIZE);
        this.ownPtr = true;
        createTimespecs();
    }

    /**
     * StatType constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing StatType data.
     */
    public StatType(Pointer ptr) {
        this.ptr = ptr;
        this.ownPtr = false;
        createTimespecs();
    }

    /**
     * Helper method to create wrappers for TimespecType fields.
     */
    private void createTimespecs() {
        atim = new TimespecType(this.ptr.inc(OFFSET_ST_ATIM));
        mtim = new TimespecType(this.ptr.inc(OFFSET_ST_MTIM));
        ctim = new TimespecType(this.ptr.inc(OFFSET_ST_CTIM));
        birthtim = new TimespecType(this.ptr.inc(OFFSET_ST_BIRTHTIM));
    }

    /**
     * Inode's device.
     *
     * @return Inode's device.
     */
    public int getDev() {
        return this.ptr.read4(OFFSET_ST_DEV);
    }

    /**
     * Inode's number.
     *
     * @return Inode's number.
     */
    public int getIno() {
        return this.ptr.read4(OFFSET_ST_INO);
    }

    /**
     * Inode protection mode.
     *
     * @return Inode protection mode.
     */
    public FileStatusMode[] getMode() {
        return FileStatusMode.valueOf(this.ptr.read2(OFFSET_ST_MODE));
    }

    /**
     * Number of hard links.
     *
     * @return Number of hard links.
     */
    public short getNlink() {
        return this.ptr.read2(OFFSET_ST_NLINK);
    }

    /**
     * User ID of the file's owner.
     *
     * @return User ID of the file's owner.
     */
    public int getUid() {
        return this.ptr.read4(OFFSET_ST_UID);
    }

    /**
     * Group ID of the file's group.
     *
     * @return Group ID of the file's group.
     */
    public int getGid() {
        return this.ptr.read4(OFFSET_ST_GID);
    }

    /**
     * Device type.
     *
     * @return Device type.
     */
    public int getRdev() {
        return this.ptr.read4(OFFSET_ST_RDEV);
    }

    /**
     * Time of last access.
     *
     * @return Time of last access.
     */
    public TimespecType getAtim() {
        return atim;
    }

    /**
     * Time of last data modification.
     *
     * @return Time of last data modification.
     */
    public TimespecType getMtim() {
        return mtim;
    }

    /**
     * Time of last file status change.
     *
     * @return Time of last file status change.
     */
    public TimespecType getCtim() {
        return ctim;
    }

    /**
     * File size, in bytes.
     *
     * @return File size, in bytes.
     */
    public long getSize() {
        return this.ptr.read8(OFFSET_ST_SIZE);
    }

    /**
     * Blocks allocated for file.
     *
     * @return Blocks allocated for file.
     */
    public long getBlocks() {
        return this.ptr.read8(OFFSET_ST_BLOCKS);
    }

    /**
     * Optimal blocksize for I/O.
     *
     * @return Optimal blocksize for I/O.
     */
    public int getBlkSize() {
        return this.ptr.read4(OFFSET_ST_BLKSIZE);
    }

    /**
     * User defined flags for file.
     *
     * @return User defined flags for file.
     */
    public int getFlags() {
        return this.ptr.read4(OFFSET_ST_FLAGS);
    }

    /**
     * File generation number.
     *
     * @return File generation number.
     */
    public int getGen() {
        return this.ptr.read4(OFFSET_ST_GEN);
    }

    public int getLSpare() {
        return this.ptr.read4(OFFSET_ST_LSPARE);
    }

    /**
     * Time of file creation.
     *
     * @return Time of file creation.
     */
    public TimespecType getBirthtim() {
        return birthtim;
    }

    /**
     * Make sure to free the StatType buffer during garbage collection.
     *
     * @throws Throwable If finalization failed.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            free();
        } finally {
            super.finalize();
        }
    }

    /**
     * Frees the native memory needed for the StatType, if it was allocated by the constructor.
     * After <code>free()</code> is called on such StatType,
     * using this Java wrapper instance will no longer be possible.
     */
    public void free() {
        if (this.ownPtr && this.ptr != null && this.ptr.addr() != 0) {
            this.ptr.free();
        }
    }

    /**
     * Gets the native memory pointer where this StatType's data is stored.
     *
     * @return StatType memory pointer.
     */
    public Pointer getPointer() {
        return this.ptr;
    }
}
