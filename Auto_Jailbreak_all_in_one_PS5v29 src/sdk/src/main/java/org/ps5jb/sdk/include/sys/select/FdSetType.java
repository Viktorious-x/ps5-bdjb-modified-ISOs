package org.ps5jb.sdk.include.sys.select;

import java.util.Arrays;

import org.ps5jb.sdk.core.Pointer;

/**
 * Wrapper for FreeBSD fd_set operations. The methods closely follow
 * the macros defined in FreeBSD <code>include/sys/select.h</code> header.
 */
public class FdSetType {
    /**
     * Total size of one fdset in bits.
     * Internally, select uses bit masks of file descriptors in longs.
     */
    public static final int FD_SETSIZE = 1024;

    private static final int NFDBITS = 8 * 8;

    private final Pointer ptr;
    private final long[] __fds_bits;

    /**
     * FdSetType constructor.
     */
    public FdSetType() {
        final int length = (FD_SETSIZE + (NFDBITS - 1)) / NFDBITS;
        this.__fds_bits = new long[length];

        this.ptr = Pointer.calloc(getSize());

        this.refresh();
    }

    /**
     * Make sure to free the fdset buffer during garbage collection.
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
     * Frees the native memory needed for the fdset. After <code>free()</code> is called,
     * using this Java wrapper instance will no longer be possible.
     */
    public void free() {
        if (getPointer() != null && getPointer().addr() != 0) {
            getPointer().free();
        }
    }

    private long __fdset_mask(int n) {
        return 1L << (n % NFDBITS);
    }

    /**
     * Returns the size in bytes occupied by this fdset.
     *
     * @return Fdset size in bytes.
     */
    public long getSize() {
        return this.__fds_bits.length * 8L;
    }

    /**
     * Determines whether the specified file descriptor is set.
     *
     * @param fd File descriptor to check.
     * @return True if the file descriptor specified by <code>fd</code> is included in this fdset.
     */
    public boolean isSet(int fd) {
        return (this.__fds_bits[fd / NFDBITS] & __fdset_mask(fd)) != 0;
    }

    /**
     * Include the specified file descriptor in the set.
     *
     * @param fd Index of the file descriptor to add.
     */
    public void set(int fd) {
        int index = fd / NFDBITS;
        this.__fds_bits[index] |= __fdset_mask(fd);
        this.ptr.write8(index, this.__fds_bits[index]);
    }

    /**
     * Removes the specified file descriptor from the set.
     *
     * @param fd File descriptor to remove.
     */
    public void unset(int fd) {
        int index = fd / NFDBITS;
        this.__fds_bits[index] &= ~__fdset_mask(fd);
        this.ptr.write8(index, this.__fds_bits[index]);
    }

    /**
     * Clear all file descriptors.
     */
    public void zero() {
        Arrays.fill(this.__fds_bits, 0L);
        this.ptr.write(0, this.__fds_bits, 0, this.__fds_bits.length);
    }

    /**
     * Copies the contents of one fd set to another.
     *
     * @param source Fdset to copy from.
     */
    public void copy(FdSetType source) {
        // Note this assumes that source is fresh and does not refresh its pointer data
        System.arraycopy(source.__fds_bits, 0, this.__fds_bits, 0, this.__fds_bits.length);
        this.ptr.write(0, this.__fds_bits, 0, this.__fds_bits.length);
    }

    /**
     * Gets the native memory pointer where this fdset data is stored.
     *
     * @return Fdset memory pointer.
     */
    public Pointer getPointer() {
        return this.ptr;
    }

    /**
     * Updates the value of this bitset in case the native memory was changed externally.
     */
    public void refresh() {
        ptr.read(0, this.__fds_bits, 0, this.__fds_bits.length);
    }
}
