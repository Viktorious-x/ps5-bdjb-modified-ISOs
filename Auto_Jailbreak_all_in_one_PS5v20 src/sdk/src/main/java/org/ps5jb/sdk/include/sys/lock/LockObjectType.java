package org.ps5jb.sdk.include.sys.lock;

import java.nio.charset.Charset;

import org.ps5jb.sdk.core.AbstractPointer;

/**
 * Wrapper for FreeBSD Kernel <code>lock_object</code> structure.
 */
public class LockObjectType {
    public static final long OFFSET_LO_NAME = 0;
    public static final long OFFSET_LO_FLAGS = OFFSET_LO_NAME + 8L;
    public static final long OFFSET_LO_DATA = OFFSET_LO_FLAGS + 4L;
    public static final long OFFSET_LO_WITNESS = OFFSET_LO_DATA + 4L;
    public static final long SIZE = OFFSET_LO_WITNESS + 8L;

    private final AbstractPointer ptr;

    /**
     * LockObjectType constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing LockObjectType data.
     */
    public LockObjectType(AbstractPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Individual lock name.
     *
     * @return Returns the value of <code>lo_name</code> field of <code>lock_object</code> structure.
     */
    public String getName() {
        return this.ptr.readString(OFFSET_LO_NAME, null, Charset.defaultCharset().name());
    }

    /**
     * Flags.
     *
     * @return Returns the value of <code>lo_flags</code> field of <code>lock_object</code> structure.
     */
    public int getFlags() {
        return this.ptr.read4(OFFSET_LO_FLAGS);
    }

    /**
     * General class specific data.
     *
     * @return Returns the value of <code>lo_data</code> field of <code>lock_object</code> structure.
     */
    public int getData() {
        return this.ptr.read4(OFFSET_LO_DATA);
    }

    /**
     * Data for witness.
     *
     * @return Returns the value of <code>lo_witness</code> field of <code>lock_object</code> structure.
     */
    public long getWitness() {
        return this.ptr.read8(OFFSET_LO_WITNESS);
    }

    /**
     * Gets the native memory pointer where this LockObjectType's data is stored.
     *
     * @return LockObjectType memory pointer.
     */
    public AbstractPointer getPointer() {
        return this.ptr;
    }
}
