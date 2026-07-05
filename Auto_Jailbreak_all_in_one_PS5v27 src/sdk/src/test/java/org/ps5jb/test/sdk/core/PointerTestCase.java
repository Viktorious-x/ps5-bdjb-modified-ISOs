package org.ps5jb.test.sdk.core;

import jdk.internal.misc.Unsafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ps5jb.sdk.core.Pointer;

public class PointerTestCase {
    private Pointer ptr;

    @BeforeEach
    protected void setUpTest() {
        ptr = Pointer.calloc(1000);
    }

    @AfterEach
    protected void tearDownTest() {
        if (!Pointer.NULL.equals(ptr) && ptr != null) {
            ptr.free();
            ptr = null;
        }
    }

    @Test
    public void testCalloc() {
        for (int i = 0; i < ptr.size().intValue() / 8; ++i) {
            long val = ptr.read8(i * 8);
            Assertions.assertEquals(0, val, "Memory allocated with 'calloc' should be zeroed");
        }
    }

    @Test
    public void testFree() {
        ptr.free();
        Assertions.assertEquals(Pointer.NULL, ptr, "Freed pointer should be equal to NULL");
        ptr = null;
    }

    @Test
    public void testFromStringNoCharset() {
        Pointer ptr2 = Pointer.fromString("abc");
        Assertions.assertEquals(4, ptr2.size().longValue());
        Assertions.assertEquals((byte) 'a', ptr2.read1());
        Assertions.assertEquals((byte) 'b', ptr2.read1(1));
        Assertions.assertEquals((byte) 'c', ptr2.read1(2));
        Assertions.assertEquals((byte) 0, ptr2.read1(3));
    }

    @Test
    public void testFromString() {
        Pointer ptr2 = Pointer.fromString("\u00e6\u00f2", "UTF-8");
        Assertions.assertEquals(5, ptr2.size().longValue());
        Assertions.assertEquals((byte) 0xC3, ptr2.read1());
        Assertions.assertEquals((byte) 0xA6, ptr2.read1(1));
        Assertions.assertEquals((byte) 0xC3, ptr2.read1(2));
        Assertions.assertEquals((byte) 0xB2, ptr2.read1(3));
        Assertions.assertEquals((byte) 0, ptr2.read1(4));
    }

    @Test
    public void testValueOf() {
        Pointer ptr2 = Pointer.valueOf(0x111222);
        Assertions.assertEquals(0x111222, ptr2.addr());
        Assertions.assertNull(ptr2.size(), "Size of the pointer produced by 'valueOf' should be null");
    }

    @Test
    public void testInc() {
        Pointer ptr2 = ptr.inc(0x1000);
        Assertions.assertEquals(ptr.addr() + 0x1000, ptr2.addr());
        Assertions.assertNull(ptr2.size(), "Size of the pointer produced by 'inc' should be null");
    }

    @Test
    public void testReadWrite1() {
        ptr.write1((byte) 5);
        Assertions.assertEquals((byte) 5, ptr.read1());
    }

    @Test
    public void testReadWrite1Offset() {
        ptr.write1(3, (byte) 5);
        Assertions.assertEquals((byte) 5, ptr.read1(3));
    }

    @Test
    public void testReadWrite2() {
        ptr.write2((short) 0x3475);
        Assertions.assertEquals((short) 0x3475, ptr.read2());
    }

    @Test
    public void testReadWrite2Offset() {
        ptr.write2(16, (short) 0x3475);
        Assertions.assertEquals((short) 0x3475, ptr.read2(16));
    }

    @Test
    public void testReadWrite4() {
        ptr.write4(0xF1D43345);
        Assertions.assertEquals(0xF1D43345, ptr.read4());
    }

    @Test
    public void testReadWrite4Offset() {
        ptr.write4(32, 0xF1D43345);
        Assertions.assertEquals(0xF1D43345, ptr.read4(32));
    }

    @Test
    public void testReadWrite8() {
        ptr.write8(0xFFFF8000ABCDEFL);
        Assertions.assertEquals(0xFFFF8000ABCDEFL, ptr.read8());
    }

    @Test
    public void testReadWrite8Offset() {
        ptr.write8(48, 0xFFFF8000ABCDEFL);
        Assertions.assertEquals(0xFFFF8000ABCDEFL, ptr.read8(48));
    }

    @Test
    public void testReadWriteByteArray() {
        byte[] writeValue = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A };
        ptr.write(128, writeValue, 0, writeValue.length);

        byte[] readValue = new byte[writeValue.length];
        ptr.read(128, readValue, 2, writeValue.length - 2);

        Assertions.assertArrayEquals(new byte[] {
                0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08
        }, readValue);
    }

    @Test
    public void testReadWriteShortArray() {
        short[] writeValue = new short[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A };
        ptr.write(192, writeValue, 0, writeValue.length);

        short[] readValue = new short[6];
        ptr.read(194, readValue, 3, 2);

        Assertions.assertArrayEquals(new short[] {
                0x00, 0x00, 0x00, 0x02, 0x03, 0x00
        }, readValue);
    }

    @Test
    public void testReadWriteIntArray() {
        int[] writeValue = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A };
        ptr.write(256, writeValue, 0, writeValue.length);

        int[] readValue = new int[writeValue.length];
        ptr.read(256, readValue, 0, writeValue.length);

        Assertions.assertArrayEquals(writeValue, readValue);
    }

    @Test
    public void testReadWriteLongArray() {
        long[] writeValue = new long[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A };
        ptr.write(256, writeValue, 0, writeValue.length);

        long[] readValue = new long[4];
        ptr.read(256 + 8 * 3, readValue, 1, 1);

        Assertions.assertArrayEquals(new long[] {
                0x00, 0x04, 0x00, 0x00
        }, readValue);
    }

    @Test
    public void testCopyToSuccess() {
        byte[] writeValue = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A };
        ptr.write(0, writeValue, 0, writeValue.length);

        Pointer newPtr = Pointer.calloc(ptr.size().longValue());
        ptr.copyTo(newPtr, 5, 5);
        byte[] res = newPtr.read(10);
        Assertions.assertArrayEquals(new byte[] {
                0x06, 0x07, 0x08, 0x09, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00
        }, res);
    }

    @Test
    public void testPerformance() {
        final int iterationCount = 10000000;
        final int elemCount = ptr.size().intValue() / 8;
        final long elemStart = ptr.addr();

        // Write values using Unsafe
        Unsafe unsafe = Unsafe.getUnsafe();
        long unsafeStart = System.currentTimeMillis();
        for (int i = 0; i < iterationCount; ++i) {
            // Don't use ptr.write8 to avoid the overhead of overflow checks
            unsafe.putLong(elemStart + (i % elemCount) * 8L, i);
        }
        long unsafeDuration = System.currentTimeMillis() - unsafeStart;

        // Read the value generated by unsafe for future comparison
        long[] unsafeBytes = new long[elemCount];
        ptr.read(0, unsafeBytes, 0, elemCount);

        // Zero memory to make sure
        for (int i = 0; i < elemCount; ++i) {
            unsafe.putLong(elemStart + i * 8L, 0);
        }

        // Now write values by making it in Java then using copyMemory
        long[] copyBytes = new long[elemCount];
        long copyStart = System.currentTimeMillis();
        for (int i = 0; i < iterationCount; ++i) {
            copyBytes[i % elemCount] = i;
        }
        ptr.write(0, copyBytes, 0, elemCount);
        long copyDuration = System.currentTimeMillis() - copyStart;

        // Make sure both values are equal
        Assertions.assertArrayEquals(unsafeBytes, copyBytes);

        // Doing single copy memory should be faster
        if (unsafeDuration < copyDuration) {
            System.err.println("Copy memory should not be slower than individual writes: " + unsafeDuration + "ms >= " + copyDuration + "ms");
        }
    }
}
