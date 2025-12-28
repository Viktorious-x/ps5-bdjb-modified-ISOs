package org.ps5jb.sdk.include.sys.mutex;

import org.ps5jb.sdk.core.AbstractPointer;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.lock.LockObjectType;

/**
 * Wrapper for FreeBSD Kernel <code>mtx</code> structure.
 */
public class MutexType {
    public static final long OFFSET_MTX_LOCK_OBJECT = 0;
    public static final long OFFSET_MTX_LOCK = OFFSET_MTX_LOCK_OBJECT + LockObjectType.SIZE;
    public static final long SIZE = OFFSET_MTX_LOCK + 8;

    private final AbstractPointer ptr;
    private LockObjectType lockObjectType;

    /**
     * MutexType constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing MutexType data.
     */
    public MutexType(AbstractPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Common lock properties.
     *
     * @return Returns the value of <code>lock_object</code> field of <code>mtx</code> structure.
     */
    public LockObjectType getLockObject() {
        if (lockObjectType == null) {
            AbstractPointer lockPointer;
            Long lockPointerSize = new Long(LockObjectType.SIZE);
            if (ptr instanceof KernelPointer) {
                lockPointer = ((KernelPointer) ptr).pptr(OFFSET_MTX_LOCK_OBJECT, lockPointerSize);
            } else {
                lockPointer = new Pointer(ptr.read8(OFFSET_MTX_LOCK_OBJECT), lockPointerSize);
            }
            lockObjectType = new LockObjectType(lockPointer);
        }
        return lockObjectType;
    }

    /**
     * Owner and flags.
     *
     * @return Returns the value of <code>mtx_lock</code> field of <code>mtx</code> structure.
     */
    public long getLock() {
        return this.ptr.read8(OFFSET_MTX_LOCK);
    }

    /**
     * Gets the native memory pointer where this MutexType's data is stored.
     *
     * @return MutexType memory pointer.
     */
    public AbstractPointer getPointer() {
        return this.ptr;
    }
}
