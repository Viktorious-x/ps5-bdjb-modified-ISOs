package org.ps5jb.sdk.include.sys.timeval;

import org.ps5jb.sdk.core.Pointer;

/**
 * Wrapper for FreeBSD Kernel <code>timeval</code> structure.
 */
public class TimevalType {
    public static final long SIZE = 16L;
    public static final long OFFSET_TV_SEC = 0L;
    public static final long OFFSET_TV_USEC = 8L;

    private final Pointer ptr;
    private final boolean ownPtr;

    /**
     * TimevalType default constructor.
     */
    public TimevalType() {
        this.ptr = Pointer.calloc(SIZE);
        this.ownPtr = true;
    }

    /**
     * TimevalType constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing TimevalType data.
     */
    public TimevalType(Pointer ptr) {
        this.ptr = ptr;
        this.ownPtr = false;
    }

    /**
     * Seconds.
     *
     * @return Returns the value of <code>tv_sec</code> field of <code>timeval</code> structure.
     */
    public long getSec() {
        return this.ptr.read8(OFFSET_TV_SEC);
    }

    /**
     * Microseconds.
     *
     * @return Returns the value of <code>tv_usec</code> field of <code>timeval</code> structure.
     */
    public long getUsec() {
        return this.ptr.read8(OFFSET_TV_USEC);
    }

    /**
     * Set seconds.
     *
     * @param val New seconds.
     */
    public void setSec(long val) {
        this.ptr.write8(OFFSET_TV_SEC, val);
    }

    /**
     * Set microseconds.
     *
     * @param val New microseconds.
     */
    public void setUsec(long val) {
        this.ptr.write8(OFFSET_TV_USEC, val);
    }

    /**
     * Make sure to free the TimevalType buffer during garbage collection.
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
     * Frees the native memory needed for the TimevalType, if it was allocated by the constructor.
     * After <code>free()</code> is called on such TimevalType,
     * using this Java wrapper instance will no longer be possible.
     */
    public void free() {
        if (this.ownPtr && this.ptr != null && this.ptr.addr() != 0) {
            this.ptr.free();
        }
    }

    /**
     * Gets the native memory pointer where this TimevalType's data is stored.
     *
     * @return TimevalType memory pointer.
     */
    public Pointer getPointer() {
        return this.ptr;
    }
}
