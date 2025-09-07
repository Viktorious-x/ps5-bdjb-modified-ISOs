package org.ps5jb.sdk.include.sys.iovec;

import org.ps5jb.sdk.core.AbstractPointer;
import org.ps5jb.sdk.core.Pointer;

/**
 * Wrapper for FreeBSD <code>iovec</code> structure.
 */
public class IoVecType {
    public static final long SIZE = 16L;
    public static final long OFFSET_BASE = 0;
    public static final long OFFSET_LENGTH = 8;

    private final AbstractPointer ptr;
    private final boolean ownPtr;

    /**
     * IoVecType default constructor.
     */
    public IoVecType() {
        this.ptr = Pointer.calloc(SIZE);
        this.ownPtr = true;
    }

    /**
     * IoVecType constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing IoVecType data.
     */
    public IoVecType(AbstractPointer ptr) {
        this.ptr = ptr;
        this.ownPtr = false;
    }

    /**
     * Base address.
     *
     * @return Returns the value of <code>iov_base</code> field of <code>iovec</code> structure.
     */
    public AbstractPointer getBase() {
        return Pointer.valueOf(this.ptr.read8(OFFSET_BASE));
    }

    /**
     * Set base address.
     *
     * @param val New base address value.
     */
    public void setBase(AbstractPointer val) {
        this.ptr.write8(OFFSET_BASE, val.addr());
    }

    /**
     * Length.
     *
     * @return Returns the value of <code>iov_len</code> field of <code>iovec</code> structure.
     */
    public long getLength() {
        return this.ptr.read8(OFFSET_LENGTH);
    }

    /**
     * Set length.
     *
     * @param val New length.
     */
    public void setLength(long val) {
        this.ptr.write8(OFFSET_LENGTH, val);
    }

    /**
     * Make sure to free the IoVecType buffer during garbage collection.
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
     * Frees the native memory needed for the IoVecType, if it was allocated by the constructor.
     * After <code>free()</code> is called on such IoVecType,
     * using this Java wrapper instance will no longer be possible.
     */
    public void free() {
        if (this.ownPtr && this.ptr instanceof Pointer && this.ptr.addr() != 0) {
            ((Pointer) this.ptr).free();
        }
    }

    /**
     * Gets the native memory pointer where this IoVecType's data is stored.
     *
     * @return IoVecType memory pointer.
     */
    public AbstractPointer getPointer() {
        return this.ptr;
    }
}
