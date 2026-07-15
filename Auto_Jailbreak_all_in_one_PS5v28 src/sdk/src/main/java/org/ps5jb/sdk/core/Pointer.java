package org.ps5jb.sdk.core;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.security.PrivilegedActionException;

import jdk.internal.misc.Unsafe;

/**
 * Abstraction over memory pointer operations in user-space.
 */
public class Pointer extends AbstractPointer {
    private static final long serialVersionUID = 2230199156786175115L;

    protected static final Unsafe UNSAFE;

    static {
        try {
            OpenModuleAction.execute("jdk.internal.misc.Unsafe");

            UNSAFE = Unsafe.getUnsafe();
        } catch (PrivilegedActionException e) {
            if (e.getException() instanceof InvocationTargetException) {
                throw new SdkRuntimeException(((InvocationTargetException) e.getException()).getTargetException());
            }
            throw new SdkRuntimeException(e.getException());
        } catch (RuntimeException | Error e) {
            throw new SdkRuntimeException(e);
        }
    }

    /**
     * Allocate the native memory of a given size and return a pointer to it.
     *
     * @param size Allocation size.
     * @return Pointer to the allocated memory.
     * @throws OutOfMemoryError if the allocation is refused by the system.
     */
    public static Pointer malloc(long size) {
        return new Pointer(UNSAFE.allocateMemory(size), new Long(size));
    }

    /**
     *  Allocate the native memory of a given size, initialize it with zeroes
     *  and return a pointer to it.
     *
     * @param size Allocation size.
     * @return Pointer to the allocated memory.
     * @throws OutOfMemoryError if the allocation is refused by the system.
     */
    public static Pointer calloc(long size) {
        Pointer result = malloc(size);
        try {
            int i = 0;
            while ((i + 8) <= size) {
                result.write8(i, 0);
                i += 8;
            }
            while (i < size) {
                result.write1(i, (byte) 0);
                ++i;
            }
        } catch (RuntimeException | Error e) {
            result.free();
            throw e;
        }
        return result;
    }

    /**
     * Allocate a new native memory buffer and copy the contents
     * of the given Java string into it, converting characters
     * to bytes using the default system charset. The resulting
     * native string is null-terminated.
     *
     * @param string String to convert to a native null-terminated string.
     * @return Pointer to the allocated buffer.
     * @throws OutOfMemoryError if the allocation is refused by the system.
     */
    public static Pointer fromString(String string) {
        return fromString(string, Charset.defaultCharset().name());
    }

    /**
     * Allocate a new native memory buffer and copy the contents
     * of the given Java string into it, converting characters
     * to bytes using the specified charset. The resulting
     * native string is null-terminated.
     *
     * @param string String to convert to a native null-terminated string.
     * @param charset Character set to use to convert from native bytes to a Java string.
     * @return Pointer to the allocated buffer.
     * @throws OutOfMemoryError if the allocation is refused by the system.
     */
    public static Pointer fromString(String string, String charset) {
        byte[] stringBuffer;
        try {
            stringBuffer = string.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new SdkRuntimeException(e);
        }

        Pointer result = malloc(stringBuffer.length + 1);
        result.write(stringBuffer);
        result.write1(stringBuffer.length, (byte) 0);
        return result;
    }

    /**
     * Return the native address of a given object.
     *
     * @param object Object whose native address to retrieve.
     * @return The native address of the given object
     *   suitable for creating a {@link Pointer} instance.
     */
    protected static long addrOf(Object object) {
        Object[] val = new Object[] { object };
        long result = UNSAFE.getLong(val, Unsafe.ARRAY_OBJECT_BASE_OFFSET);
        val[0] = null;
        return result;
    }

    /**
     * Returns a pointer instance equivalent to the given native memory address.
     *
     * @param addr Address to convert to a Pointer instance.
     * @return Pointer instance representing the given native memory address.
     */
    public static Pointer valueOf(long addr) {
        return addr == 0 ? NULL : new Pointer(addr);
    }

    /** Static constant for NULL pointer. */
    public static final Pointer NULL = new Pointer(0);

    public Pointer(long addr) {
        super(addr);
    }

    public Pointer(long addr, Long size) {
        super(addr, size);
    }

    @Override
    protected byte read1impl(long offset) {
        return UNSAFE.getByte(this.addr + offset);
    }

    @Override
    protected short read2impl(long offset) {
        return UNSAFE.getShort(this.addr + offset);
    }

    @Override
    protected int read4impl(long offset) {
        return UNSAFE.getInt(this.addr + offset);
    }

    @Override
    protected long read8impl(long offset) {
        return UNSAFE.getLong(this.addr + offset);
    }

    @Override
    public void read(long offset, byte[] value, int valueIndex, int count) {
        readImpl(offset, value, valueIndex, count, Unsafe.ARRAY_BYTE_BASE_OFFSET, Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    /**
     * Read the given number of shorts at the specified offset from the pointer.
     *
     * @param offset Offset in bytes relative to {@link #addr}.
     * @param value Buffer to read the value into.
     * @param valueIndex Starting index in the buffer where to place the read value.
     * @param count Number of shorts to read.
     * @throws IndexOutOfBoundsException If the buffer is not large enough to hold the value
     *   of the specified size.
     */
    public void read(long offset, short[] value, int valueIndex, int count) {
        readImpl(offset, value, valueIndex, count, Unsafe.ARRAY_SHORT_BASE_OFFSET, Unsafe.ARRAY_SHORT_INDEX_SCALE);
    }

    /**
     * Read the given number of ints at the specified offset from the pointer.
     *
     * @param offset Offset in bytes relative to {@link #addr}.
     * @param value Buffer to read the value into.
     * @param valueIndex Starting index in the buffer where to place the read value.
     * @param count Number of ints to read.
     * @throws IndexOutOfBoundsException If the buffer is not large enough to hold the value
     *   of the specified size.
     */
    public void read(long offset, int[] value, int valueIndex, int count) {
        readImpl(offset, value, valueIndex, count, Unsafe.ARRAY_INT_BASE_OFFSET, Unsafe.ARRAY_INT_INDEX_SCALE);
    }

    /**
     * Read the given number of longs at the specified offset from the pointer.
     *
     * @param offset Offset in bytes relative to {@link #addr}.
     * @param value Buffer to read the value into.
     * @param valueIndex Starting index in the buffer where to place the read value.
     * @param count Number of longs to read.
     * @throws IndexOutOfBoundsException If the buffer is not large enough to hold the value
     *   of the specified size.
     */
    public void read(long offset, long[] value, int valueIndex, int count) {
        readImpl(offset, value, valueIndex, count, Unsafe.ARRAY_LONG_BASE_OFFSET, Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    /**
     * Read a number of arbitrary size elements from the pointer and write them to a Java heap array.
     *
     * @param offset Offset in bytes relative to {@link #addr}.
     * @param value Buffer to read the value into. Must be a properly sized array of the same type as
     *  the scale specified by <code>indexScale</code>
     * @param valueIndex Starting index in the buffer where to place the read value.
     * @param count Number of elements to read.
     * @param baseOffset Base offset of data start in the Java heap array. Can be obtained from
     *   {@link Unsafe#arrayBaseOffset(Class)}.
     * @param indexScale Size of each element in the Java heap array. Can be obtained from
     *   {@link Unsafe#arrayIndexScale(Class)}.
     * @throws IndexOutOfBoundsException If the buffer is not large enough to hold the value
     *   of the specified size.
     */
    protected void readImpl(long offset, Object value, int valueIndex, int count, int baseOffset, int indexScale) {
        long readSize = ((long) count) * indexScale;
        overflow(this, offset, readSize);

        long writeAddr = addrOf(value) + baseOffset;
        long writeStart = ((long) valueIndex) * indexScale;

        UNSAFE.copyMemory(this.addr + offset, writeAddr + writeStart, readSize);
    }

    @Override
    protected void write1impl(long offset, byte value) {
        UNSAFE.putByte(this.addr + offset, value);
    }

    @Override
    protected void write2impl(long offset, short value) {
        UNSAFE.putShort(this.addr + offset, value);
    }

    @Override
    protected void write4impl(long offset, int value) {
        UNSAFE.putInt(this.addr + offset, value);
    }

    @Override
    protected void write8impl(long offset, long value) {
        UNSAFE.putLong(this.addr + offset, value);
    }

    @Override
    public void write(long offset, byte[] value, int valueIndex, int count) {
        writeImpl(offset, value, valueIndex, count, Unsafe.ARRAY_BYTE_BASE_OFFSET, Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    /**
     * Write the given number of shorts at the specified offset from the pointer.
     *
     * @param offset Offset in bytes relative to {@link #addr}.
     * @param value Buffer to write.
     * @param valueIndex Index in the buffer from which to start writing.
     * @param count Number of shorts to write.
     * @throws IndexOutOfBoundsException If the buffer or the native memory
     *   are not large enough for the given values of <code>offset</code>,
     *   <code>valueIndex</code> and <code>count</code>.
     */
    public void write(long offset, short[] value, int valueIndex, int count) {
        writeImpl(offset, value, valueIndex, count, Unsafe.ARRAY_SHORT_BASE_OFFSET, Unsafe.ARRAY_SHORT_INDEX_SCALE);
    }

    /**
     * Write the given number of ints at the specified offset from the pointer.
     *
     * @param offset Offset in bytes relative to {@link #addr}.
     * @param value Buffer to write.
     * @param valueIndex Index in the buffer from which to start writing.
     * @param count Number of ints to write.
     * @throws IndexOutOfBoundsException If the buffer or the native memory
     *   are not large enough for the given values of <code>offset</code>,
     *   <code>valueIndex</code> and <code>count</code>.
     */
    public void write(long offset, int[] value, int valueIndex, int count) {
        writeImpl(offset, value, valueIndex, count, Unsafe.ARRAY_INT_BASE_OFFSET, Unsafe.ARRAY_INT_INDEX_SCALE);
    }

    /**
     * Write the given number of longs at the specified offset from the pointer.
     *
     * @param offset Offset in bytes relative to {@link #addr}.
     * @param value Buffer to write.
     * @param valueIndex Index in the buffer from which to start writing.
     * @param count Number of longs to write.
     * @throws IndexOutOfBoundsException If the buffer or the native memory
     *   are not large enough for the given values of <code>offset</code>,
     *   <code>valueIndex</code> and <code>count</code>.
     */
    public void write(long offset, long[] value, int valueIndex, int count) {
        writeImpl(offset, value, valueIndex, count, Unsafe.ARRAY_LONG_BASE_OFFSET, Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    /**
     * Write a number of arbitrary size elements at the specified offset from the pointer.
     *
     * @param offset Offset in bytes relative to {@link #addr}.
     * @param value Buffer to write.
     * @param valueIndex Index in the buffer from which to start writing.
     * @param count Number of elements to write.
     * @param baseOffset Base offset of data start in the <code>value</code>. Can be obtained from
     *   {@link Unsafe#arrayBaseOffset(Class)}.
     * @param indexScale Size of each element in the <code>value</code>. Can be obtained from
     *   {@link Unsafe#arrayIndexScale(Class)}.
     * @throws IndexOutOfBoundsException If the buffer or the native memory
     *   are not large enough for the given values of <code>offset</code>,
     *   <code>valueIndex</code> and <code>count</code>.
     */
    protected void writeImpl(long offset, Object value, int valueIndex, int count, int baseOffset, int indexScale) {
        long writeSize = ((long) count) * indexScale;
        overflow(this, offset, writeSize);

        long readAddr = addrOf(value) + baseOffset;
        long readStart = ((long) valueIndex) * indexScale;

        UNSAFE.copyMemory(readAddr + readStart, this.addr + offset, writeSize);
    }

    @Override
    public void copyTo(AbstractPointer dest, long offset, int size) {
        if (dest instanceof Pointer) {
            UNSAFE.copyMemory(addr + offset, dest.addr, size);
        } else {
            super.copyTo(dest, offset, size);
        }
    }

    /**
     * Free the native memory associated with this pointer.
     * After the call to this method, this pointer should not be used.
     * Pointers not created using {@link #malloc(long)} or {@link #calloc(long)}
     * should not be freed using this method.
     */
    public void free() {
        UNSAFE.freeMemory(this.addr);
        this.addr = 0;
        this.size = null;
    }

    /**
     * Increment the pointer by a given offset. The <code>delta</code> parameter may be negative.
     *
     * @param delta Offset from the current address of this pointer.
     * @return New pointer instance whose size is unknown and whose address is the address of
     *   this pointer shifted by <code>delta</code> bytes.
     */
    public Pointer inc(long delta) {
        // Size is intentionally left unknown
        return Pointer.valueOf(this.addr + delta);
    }
}
