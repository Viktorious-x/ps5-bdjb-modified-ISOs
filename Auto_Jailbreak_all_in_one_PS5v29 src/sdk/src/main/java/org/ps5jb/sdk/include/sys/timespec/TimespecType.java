package org.ps5jb.sdk.include.sys.timespec;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.include.sys.uio.UioReadWrite;
import org.ps5jb.sdk.include.sys.uio.UioSegmentFlag;

/**
 * Wrapper for FreeBSD Kernel <code>timespec</code> structure.
 */
public class TimespecType {
    public static final long SIZE = 16L;
    public static final long OFFSET_TV_SEC = 0L;
    public static final long OFFSET_TV_NSEC = 8L;

    private final Pointer ptr;
    private final boolean ownPtr;

    /**
     * TimespecType default constructor.
     */
    public TimespecType() {
        this.ptr = Pointer.calloc(SIZE);
        this.ownPtr = true;
    }

    /**
     * TimespecType constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing TimespecType data.
     */
    public TimespecType(Pointer ptr) {
        this.ptr = ptr;
        this.ownPtr = false;
    }

    /**
     * Seconds.
     *
     * @return Returns the value of <code>tv_sec</code> field of <code>timespec</code> structure.
     */
    public long getSec() {
        return this.ptr.read8(OFFSET_TV_SEC);
    }

    /**
     * Nanoseconds.
     *
     * @return Returns the value of <code>tv_nsec</code> field of <code>timespec</code> structure.
     */
    public long getNsec() {
        return this.ptr.read8(OFFSET_TV_NSEC);
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
     * Set nanoseconds.
     *
     * @param val New nanoseconds.
     */
    public void setNsec(long val) {
        this.ptr.write8(OFFSET_TV_NSEC, val);
    }

    /**
     * Make sure to free the TimespecType buffer during garbage collection.
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
     * Frees the native memory needed for the TimespecType, if it was allocated by the constructor.
     * After <code>free()</code> is called on such TimespecType,
     * using this Java wrapper instance will no longer be possible.
     */
    public void free() {
        if (this.ownPtr && this.ptr != null && this.ptr.addr() != 0) {
            this.ptr.free();
        }
    }

    /**
     * Gets the native memory pointer where this Timespec's data is stored.
     *
     * @return Timespec memory pointer.
     */
    public Pointer getPointer() {
        return this.ptr;
    }
}
