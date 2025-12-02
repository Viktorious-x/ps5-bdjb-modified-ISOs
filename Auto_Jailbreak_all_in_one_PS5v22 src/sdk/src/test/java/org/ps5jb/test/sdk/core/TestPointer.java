package org.ps5jb.test.sdk.core;

import org.ps5jb.sdk.core.AbstractPointer;

/**
 * Pointer implementation backed by an internal byte array.
 * Used in unit tests to validate other methods of
 * AbstractPointer class that depend on the read/write primitives.
 */
public class TestPointer extends AbstractPointer {
    private byte[] testBuffer;

    public TestPointer(long addr, Long size) {
        super(addr, size);
        populateTestBuffer();
    }

    public TestPointer(long addr) {
        super(addr);
        populateTestBuffer();
    }

    protected TestPointer(long addr, Long size, byte[] buffer) {
        super(addr, size);
        this.testBuffer = buffer;
    }

    private void populateTestBuffer() {
        int testSize;
        if (this.size == null) {
            testSize = 0x1000;
        } else {
            testSize = (int) this.size.longValue();
        }

        testBuffer = new byte[testSize];
        for (int i = 0; i < testSize; ++i) {
            testBuffer[i] = (byte) ((i + 1) & 0xFF);
        }
    }

    @Override
    protected byte read1impl(long offset) {
        return testBuffer[(int) offset];
    }

    @Override
    protected short read2impl(long offset) {
        int result = 0;
        for (int i = 0; i < 2; ++i) {
            result |= (testBuffer[(int) (offset + i)] & 0xFF) << (i * 8);
        }
        return (short) result;
    }

    @Override
    protected int read4impl(long offset) {
        int result = 0;
        for (int i = 0; i < 4; ++i) {
            result |= (testBuffer[(int) (offset + i)] & 0xFF) << (i * 8);
        }
        return result;
    }

    @Override
    protected long read8impl(long offset) {
        long result = 0;
        for (int i = 0; i < 8; ++i) {
            result |= (((long) (testBuffer[(int) (offset + i)] & 0xFF)) << (i * 8));
        }
        return result;
    }

    @Override
    protected void write1impl(long offset, byte value) {
        testBuffer[(int) offset] = value;
    }

    @Override
    protected void write2impl(long offset, short value) {
        for (int i = 0; i < 2; ++i) {
            testBuffer[(int) (offset + i)] = (byte) ((value >> (i * 8)) & 0xFF);
        }
    }

    @Override
    protected void write4impl(long offset, int value) {
        for (int i = 0; i < 4; ++i) {
            testBuffer[(int) (offset + i)] = (byte) ((value >> (i * 8)) & 0xFF);
        }
    }

    @Override
    protected void write8impl(long offset, long value) {
        for (int i = 0; i < 8; ++i) {
            testBuffer[(int) (offset + i)] = (byte) ((value >> (i * 8)) & 0xFF);
        }
    }

    public TestPointer copy(long delta) {
        byte[] newBuffer = new byte[this.testBuffer.length - ((int) delta)];
        System.arraycopy(this.testBuffer, (int) delta, newBuffer, 0, newBuffer.length);
        return new TestPointer(addr() + delta, size() == null ? null : new Long(size().longValue() - delta), newBuffer);
    }
}
