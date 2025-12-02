package org.ps5jb.sdk.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Root parent for any class that implements a pointer to a memory.
 */
public abstract class AbstractPointer implements Serializable {
    private static final long serialVersionUID = 5085573430112354495L;

    /**
     * Wrap the given pointer in a non-null check. Returns the same pointer if it is not NULL.
     *
     * @param pointer Pointer to verify.
     * @param errorMessage Error message assigned to the exception if the pointer is NULL.
     * @return Same <code>pointer</code> value if it is not NULL.
     * @throws NullPointerException If the input pointer is NULL or points to the address 0.
     */
    public static <T extends AbstractPointer> T nonNull(T pointer, String errorMessage) {
        if (pointer == null || pointer.addr() == 0) {
            throw new NullPointerException(errorMessage);
        }
        return pointer;
    }

    /**
     * Check that the pointer operation will result in an overflow.
     *
     * @param pointer Pointer to verify.
     * @param offset Offset at which the pointer operation is started.
     * @param size Size of the pointer operation.
     * @throws IndexOutOfBoundsException If the operation is out of bounds.
     */
    public static void overflow(AbstractPointer pointer, long offset, long size) {
        // All overflow checks are disabled when size is unknown
        if (pointer.size != null) {
            if (offset < 0) {
                throw new IndexOutOfBoundsException(Long.toString(offset));
            }

            if (((offset + size) > pointer.size.longValue())) {
                throw new IndexOutOfBoundsException(Long.toString(offset + size));
            }
        }

        // Do not allow to read from null pointer, even with offset
        nonNull(pointer, null);
    }

    /** Native memory address pointed to by this instance. */
    protected long addr;

    /** Size of the memory pointed to by this instance. May be null if the size is unknown. */
    protected Long size;

    /**
     * Constructor of a pointer without a known size.
     *
     * @param addr Memory address of the pointer.
     */
    protected AbstractPointer(long addr) {
        this(addr, null);
    }

    /**
     * Constructor of a pointer where the size is known.
     *
     * @param addr Memory address of the pointer.
     * @param size Size of the memory.
     */
    protected AbstractPointer(long addr, Long size) {
        this.addr = addr;
        this.size = size;
    }

    /**
     * Read 1 byte at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    public byte read1(long offset) {
        overflow(this, offset, 1);
        return read1impl(offset);
    }

    /**
     * Read 1 byte from the address pointed to by this pointer instance.
     *
     * @return Value read from the memory.
     */
    public byte read1() {
        return read1(0);
    }

    /**
     * Internal implementation of reading 1 byte from this pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    protected abstract byte read1impl(long offset);

    /**
     * Read 2 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    public short read2(long offset) {
        overflow(this, offset, 2);
        return read2impl(offset);
    }

    /**
     * Read 2 bytes from the address pointed to by this pointer instance.
     *
     * @return Value read from the memory.
     */
    public short read2() {
        return read2(0);
    }

    /**
     * Internal implementation of reading 2 bytes from this pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    protected abstract short read2impl(long offset);

    /**
     * Read 4 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    public int read4(long offset) {
        overflow(this, offset, 4);
        return read4impl(offset);
    }

    /**
     * Read 4 bytes from the address pointed to by this pointer instance.
     *
     * @return Value read from the memory.
     */
    public int read4() {
        return read4(0);
    }

    /**
     * Internal implementation of reading 4 bytes from this pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    protected abstract int read4impl(long offset);

    /**
     * Read 8 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    public long read8(long offset) {
        overflow(this, offset, 8);
        return read8impl(offset);
    }

    /**
     * Read 8 bytes from the address pointed to by this pointer instance.
     *
     * @return Value read from the memory.
     */
    public long read8() {
        return read8(0);
    }

    /**
     * Internal implementation of reading 8 bytes from this pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    protected abstract long read8impl(long offset);

    /**
     * Read the given number of bytes from the address pointed to by this pointer instance.
     *
     * @param count Number of bytes to read.
     * @return Value read from the memory as an array of bytes.
     */
    public byte[] read(int count) {
        byte[] result = new byte[count];
        read(0, result, 0, count);
        return result;
    }

    /**
     * Read the given number of bytes at the specified offset from the pointer.
     *
     * @param offset Offset in bytes relative to {@link #addr}.
     * @param value Buffer to read the value into.
     * @param valueIndex Starting index in the buffer where to place the read value.
     * @param count Number of bytes to read.
     * @throws IndexOutOfBoundsException If the buffer is not large enough to hold the value
     *   of the specified size.
     */
    public void read(long offset, byte[] value, int valueIndex, int count) {
        overflow(this, offset, count);

        long buffer;
        int bufferLen;

        for (int i = 0; i < count; i += bufferLen) {
            if ((i + 8) <= count) {
                buffer = read8impl(offset + i);
                bufferLen = 8;
            } else if ((i + 4) <= count) {
                buffer = read4impl(offset + i);
                bufferLen = 4;
            } else if ((i + 2) <= count) {
                buffer = read2impl(offset + i);
                bufferLen = 2;
            } else {
                buffer = read1impl(offset + i);
                bufferLen = 1;
            }

            for (int j = 0; j < bufferLen; ++j) {
                value[valueIndex + i + j] = (byte) ((buffer >> (j * 8)) & 0xFF);
            }
        }
    }

    /**
     * Read the data at the specified offset from the pointer assuming it is a native
     * null-terminated string.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param maxLength Maximum number of bytes to read. If <code>null</code>,
     *   the memory will be read until the null character is encountered.
     *   If this pointer has {@link #size() size}, then the read
     *   will not go beyond the pointer boundary.
     * @param charset Charset to use to convert the native string into a Java
     *   string.
     * @return Value of the memory as a Java string.
     * @throws SdkRuntimeException If the string could not be read, for example
     *   if the bytes cannot be converted to the target charset.
     */
    public String readString(long offset, Integer maxLength, String charset) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try {
                int curSize = 0;
                byte[] buffer;
                if (maxLength == null) {
                    buffer = new byte[1];
                } else {
                    buffer = new byte[8];
                }

                while ((maxLength == null) || (maxLength.intValue() > curSize)) {
                    int readLen;
                    if (maxLength == null) {
                        readLen = 1;
                    } else if (this.size != null && ((offset + curSize + 8) >= this.size.longValue())) {
                        readLen = (int) (this.size.longValue() - offset - curSize);
                    } else {
                        readLen = Math.min(8, maxLength.intValue() - curSize);
                    }
                    read(offset + curSize, buffer, 0, readLen);

                    boolean eos = false;
                    for (int i = 0; i < readLen; ++i) {
                        if (buffer[i] == 0) {
                            eos = true;
                            break;
                        }
                        buf.write(buffer[i]);
                        curSize++;
                    }

                    if (eos) break;
                }

                return buf.toString(charset);
            } finally {
                buf.close();
            }
        } catch (IOException e) {
            throw new SdkRuntimeException(e);
        }
    }

    /**
     * Read the current value of the pointer assuming the data is a native
     * null-terminated string. The bytes in the native memory are converted to
     * Java string using the system default charset.
     *
     * @param maxLength Maximum number of bytes to read. If <code>null</code>,
     *   the memory will be read until the null character is encountered.
     * @return Value of the memory as a Java string.
     */
    public String readString(Integer maxLength) {
        return readString(0, maxLength, Charset.defaultCharset().name());
    }

    /**
     * Write 1 byte at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    public void write1(long offset, byte value) {
        overflow(this, offset, 1);
        write1impl(offset, value);
    }

    /**
     * Write 1 byte to the address pointed to by this pointer instance.
     *
     * @param value Value to write.
     */
    public void write1(byte value) {
        write1(0, value);
    }

    /**
     * Internal implementation of writing 1 byte to this pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    protected abstract void write1impl(long offset, byte value);

    /**
     * Write 2 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    public void write2(long offset, short value) {
        overflow(this, offset, 2);
        write2impl(offset, value);
    }

    /**
     * Write 2 bytes to the address pointed to by this pointer instance.
     *
     * @param value Value to write.
     */
    public void write2(short value) {
        write2(0, value);
    }

    /**
     * Internal implementation of writing 2 bytes to this pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    protected abstract void write2impl(long offset, short value);

    /**
     * Write 4 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    public void write4(long offset, int value) {
        overflow(this, offset, 4);
        write4impl(offset, value);
    }

    /**
     * Write 4 bytes to the address pointed to by this pointer instance.
     *
     * @param value Value to write.
     */
    public void write4(int value) {
        write4(0, value);
    }

    /**
     * Internal implementation of writing 4 bytes to this pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    protected abstract void write4impl(long offset, int value);

    /**
     * Write 8 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    public void write8(long offset, long value) {
        overflow(this, offset, 8);
        write8impl(offset, value);
    }

    /**
     * Write 8 bytes to the address pointed to by this pointer instance.
     *
     * @param value Value to write.
     */
    public void write8(long value) {
        write8(0, value);
    }

    /**
     * Internal implementation of writing 8 bytes to this pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    protected abstract void write8impl(long offset, long value);

    /**
     * Write the given number of bytes to the address pointed to by this pointer instance.
     *
     * @param value Value to write.
     */
    public void write(byte[] value) {
        this.write(0, value, 0, value.length);
    }

    /**
     * Write the given number of bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Buffer to write.
     * @param valueIndex Index in the buffer from which to start writing.
     * @param count Number of bytes to write.
     * @throws IndexOutOfBoundsException If the buffer or the native memory
     *   are not large enough for the given values of <code>offset</code>,
     *   <code>valueIndex</code> and <code>count</code>.
     */
    public void write(long offset, byte[] value, int valueIndex, int count) {
        overflow(this, offset, count);

        long buffer;
        int bufferLen;

        for (int i = 0; i < count; i += bufferLen) {
            if ((i + 8) <= count) {
                bufferLen = 8;
            } else if ((i + 4) <= count) {
                bufferLen = 4;
            } else if ((i + 2) <= count) {
                bufferLen = 2;
            } else {
                bufferLen = 1;
            }

            buffer = 0;
            for (int j = 0; j < bufferLen; ++j) {
                buffer |= (((long) (value[valueIndex + i + j] & 0xFF)) << (j * 8));
            }

            if (bufferLen == 8) {
                write8impl(offset + i, buffer);
            } else if (bufferLen == 4) {
                write4impl(offset + i, (int) (buffer & 0xFFFFFFFFL));
            } else if (bufferLen == 2) {
                write2impl(offset + i, (short) (buffer & 0xFFFFL));
            } else {
                write1impl(offset + i, (byte) (buffer & 0xFFL));
            }
        }
    }

    /**
     * Write a given Java string at the specified offset from the pointer as
     * a native null-terminated string.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param string String value to write.
     * @param charset Charset to use to convert the native string into a Java
     *   string.
     * @throws SdkRuntimeException If the string could not be written, for example
     *   if the bytes cannot be converted to the target charset.
     * @throws IndexOutOfBoundsException If the write beyond the pointer size occurs.
     */
    public void writeString(long offset, String string, String charset) {
        byte[] stringBuffer;
        try {
            stringBuffer = string.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new SdkRuntimeException(e);
        }

        write(offset, stringBuffer, 0, stringBuffer.length);
        write1(offset + stringBuffer.length, (byte) 0);
    }

    public void writeString(String string) {
        writeString(0, string, Charset.defaultCharset().name());
    }

    /**
     * Copies values in native memory associated with this pointer to a pointer specified by <code>dest</code>.
     *
     * @param dest Memory to copy the data to. The data will always be copied starting at offset 0 in <code>dest</code>.
     * @param offset Offset in this memory to read the data from.
     * @param size Size of data to copy.
     * @throws IndexOutOfBoundsException If the read or the write beyond one of the two pointers' sizes occurs.
     */
    public void copyTo(AbstractPointer dest, long offset, int size) {
        byte[] data = new byte[size];
        read(offset, data, 0, size);
        dest.write(0, data, 0, size);
    }

    /**
     * Get the native memory address of this pointer.
     *
     * @return Native memory address of this pointer.
     */
    public long addr() {
        return this.addr;
    }

    /**
     * Get the size of the allocated native memory pointed to by this instance.
     *
     * @return Size of the memory pointed to by this pointer. May be null if the size is unknown.
     */
    public Long size() {
        return this.size;
    }

    /**
     * Compares the address of this pointer with the address of another pointer.
     * Note that size does not play a role in equality of two pointers.
     *
     * @param obj Other pointer to compare this pointer to.
     *   If this object is not a pointer, the comparison returns <code>false</code>.
     * @return True if <code>obj</code> is a pointer and its address is the same
     *   as this instance's address.
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof AbstractPointer) {
            // Size is not considered when evaluating equality
            result = ((AbstractPointer) obj).addr == this.addr;
        }
        return result;
    }

    /**
     * Computes a hashcode for this pointer.
     *
     * @return This pointer's hashcode.
     */
    @Override
    public int hashCode() {
        return (new Long(this.addr)).hashCode();
    }

    /**
     * String representation of the pointer
     *
     * @return Hexadecimal address of the pointer
     */
    @Override
    public String toString() {
        return toString(this.addr);
    }

    /**
     * Represent a pointer address as a hex string.
     *
     * @param addr Pointer address.
     * @return Hexadecimal representation of the address.
     */
    public static String toString(long addr) {
        int padLength;
        if (addr > 0xFFFFFFFFL) {
            padLength = 16;
        } else {
            padLength = 8;
        }

        StringBuffer buf = new StringBuffer(padLength);
        buf.append("0x");
        String hexAddr = Long.toHexString(addr);
        int padCount = padLength - hexAddr.length();
        for (int i = 0; i < padCount; ++i) {
            buf.append("0");
        }
        buf.append(hexAddr);

        return buf.toString();
    }
}
