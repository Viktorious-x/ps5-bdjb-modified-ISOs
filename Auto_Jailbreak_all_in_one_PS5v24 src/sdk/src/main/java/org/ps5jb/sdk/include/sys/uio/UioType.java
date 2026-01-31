package org.ps5jb.sdk.include.sys.uio;

import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.sdk.core.AbstractPointer;
import org.ps5jb.sdk.core.kernel.KernelPointer;

/**
 * Wrapper for FreeBSD Kernel <code>uio</code> structure.
 */
public class UioType {
    public static final long SIZE = 48L;
    public static final long OFFSET_IOV = 0L;
    public static final long OFFSET_IOV_COUNT = 8L;
    public static final long OFFSET_OFFSET = 16L;
    public static final long OFFSET_RESIDUAL_SIZE = 24L;
    public static final long OFFSET_SEGMENT_FLAG = 32L;
    public static final long OFFSET_READ_WRITE = 36L;
    public static final long OFFSET_OWNER = 40L;

    private final AbstractPointer ptr;

    /**
     * UioType constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing UioType data.
     */
    public UioType(AbstractPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Scatter/gather list.
     *
     * @return Returns the value of <code>uio_iov</code> field of <code>uio</code> structure,
     *   which is a pointer to a list of {@link org.ps5jb.sdk.include.sys.iovec.IoVecType} structures.
     */
    public KernelPointer getIov() {
        KernelPointer result;
        if (this.ptr instanceof KernelPointer) {
            result = ((KernelPointer) ptr).pptr(OFFSET_IOV);
        } else {
            result = new KernelPointer(this.ptr.read8(OFFSET_IOV));
        }
        return result;
    }

    /**
     * Length of scatter/gather list.
     *
     * @return Returns the value of <code>uio_iovcnt</code> field of <code>uio</code> structure.
     */
    public int getIovCount() {
        return this.ptr.read4(OFFSET_IOV_COUNT);
    }

    /**
     * Offset in target object.
     *
     * @return Returns the value of <code>uio_offset</code> field of <code>uio</code> structure.
     */
    public long getOffset() {
        return this.ptr.read8(OFFSET_OFFSET);
    }

    /**
     * Remaining bytes to process.
     *
     * @return Returns the value of <code>uio_resid</code> field of <code>uio</code> structure.
     */
    public long getResidualSize() {
        return this.ptr.read8(OFFSET_RESIDUAL_SIZE);
    }

    /**
     * Address space.
     *
     * @return Returns the value of <code>uio_segflg</code> field of <code>uio</code> structure.
     */
    public UioSegmentFlag getSegmentFlag() {
        return UioSegmentFlag.valueOf(this.ptr.read4(OFFSET_SEGMENT_FLAG));
    }

    /**
     * Set address space.
     *
     * @param val New address space.
     */
    public void setSegmentFlag(UioSegmentFlag val) {
        this.ptr.write4(OFFSET_SEGMENT_FLAG, val.value());
    }

    /**
     * Operation.
     *
     * @return Returns the value of <code>uio_rw</code> field of <code>uio</code> structure.
     */
    public UioReadWrite getReadWrite() {
        return UioReadWrite.valueOf(this.ptr.read4(OFFSET_READ_WRITE));
    }

    /**
     * Owner thread.
     *
     * @return Returns the value of <code>uio_td</code> field of <code>uio</code> structure.
     */
    public KernelPointer getOwner() {
        KernelPointer result;
        if (this.ptr instanceof KernelPointer) {
            result = ((KernelPointer) ptr).pptr(OFFSET_OWNER);
        } else {
            result = new KernelPointer(this.ptr.read8(OFFSET_OWNER));
        }
        return result;
    }

    /**
     * Gets the native memory pointer where this UioType's data is stored.
     *
     * @return UioType memory pointer.
     */
    public AbstractPointer getPointer() {
        return this.ptr;
    }
}
