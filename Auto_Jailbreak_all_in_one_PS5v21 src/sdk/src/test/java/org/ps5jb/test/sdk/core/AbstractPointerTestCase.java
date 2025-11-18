package org.ps5jb.test.sdk.core;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ps5jb.sdk.core.AbstractPointer;
import org.ps5jb.sdk.core.Pointer;

public class AbstractPointerTestCase {
    private TestPointer ptr;

    @BeforeEach
    protected void setUpTest() {
        ptr = new TestPointer(0x400000, new Long(0x1000L));
    }

    @Test
    public void testReadUnderflow() {
        try {
            ptr.read1(-5);
            Assertions.fail("Should have thrown IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException ex) {
            Assertions.assertEquals("-5", ex.getMessage());
        }
    }

    @Test
    public void testReadOverflow() {
        try {
            ptr.read1(0x1001);
            Assertions.fail("Should have thrown IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException ex) {
            Assertions.assertEquals("4098", ex.getMessage());
        }
    }

    @Test
    public void testReadNoOverflowWithoutSize() {
        TestPointer test = new TestPointer(10);
        AbstractPointer.overflow(test, 0,0xFFFFFFFFL);
    }

    @Test
    public void testReadNull() {
        try {
            TestPointer test = new TestPointer(0);
            test.read(1);;
            Assertions.fail("Should have thrown NullPointerException when reading from a null pointer");
        } catch (NullPointerException ex) {
            Assertions.assertNull(ex.getMessage());
        }
    }

    @Test
    public void testNullZero() {
        final String msg = "pointer is null";
        try {
            AbstractPointer.nonNull(new TestPointer(0), msg);
            Assertions.fail("Should have thrown NullPointerException");
        } catch (NullPointerException ex) {
            Assertions.assertEquals(msg, ex.getMessage());
        }
    }

    @Test
    public void testNull() {
        final String msg = "null";
        try {
            AbstractPointer.nonNull(null, msg);
        } catch (NullPointerException ex) {
            Assertions.assertEquals(msg, ex.getMessage());
        }
    }

    @Test
    public void testNonNullEqualsSelf() {
        TestPointer test = new TestPointer(1);
        Assertions.assertEquals(AbstractPointer.nonNull(test, null), test);
    }

    @Test
    public void testRead1() {
        byte res = ptr.read1();
        Assertions.assertEquals((byte) 1, res);
    }

    @Test
    public void testRead1Offset() {
        final long offset = 0x353;
        byte res = ptr.read1(offset);
        Assertions.assertEquals((byte) ((offset + 1) & 0xFF), res);
    }

    @Test
    public void testRead2() {
        short res = ptr.read2();
        Assertions.assertEquals((short) 0x0201, res);
    }

    @Test
    public void testRead2Offset() {
        final long offset = 0x253;
        short res = ptr.read2(offset);
        Assertions.assertEquals((short) 0x5554, res);
    }

    @Test
    public void testRead4() {
        int res = ptr.read4();
        Assertions.assertEquals(0x04030201, res);
    }

    @Test
    public void testRead4Offset() {
        final long offset = 0x9B0;
        int res = ptr.read4(offset);
        Assertions.assertEquals(0xB4B3B2B1, res);
    }

    @Test
    public void testRead8() {
        long res = ptr.read8();
        Assertions.assertEquals(0x0807060504030201L, res);
    }

    @Test
    public void testRead8Offset() {
        final long offset = 0x3C4;
        long res = ptr.read8(offset);
        Assertions.assertEquals(0xCCCBCAC9C8C7C6C5L, res);
    }

    @Test
    public void testReadSize() {
        byte[] res = ptr.read(7);
        Assertions.assertArrayEquals(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 }, res);
    }

    @Test
    public void testReadBuffer() {
        final long offset = 0xCED;
        byte[] result = new byte[19];
        ptr.read(offset, result, 3, 15);
        Assertions.assertArrayEquals(new byte[] {
                0x00, 0x00, 0x00, (byte) 0xEE, (byte) 0xEF, (byte) 0xF0, (byte) 0xF1, (byte) 0xF2, (byte) 0xF3,
                (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFB,
                (byte) 0xFC, 0x00
        }, result);
    }

    @Test
    public void testReadStringMaxLength() {
        String res = ptr.copy(0x41).readString(new Integer(15));
        Assertions.assertEquals("BCDEFGHIJKLMNOP", res);
    }

    @Test
    public void testReadStringUnlimited() {
        TestPointer ptrCopy = ptr.copy(0);
        ptrCopy.write1(0x45, (byte) 0);
        String res = ptrCopy.readString(0x35, null, Charset.defaultCharset().name());
        Assertions.assertEquals("6789:;<=>?@ABCDE", res);
    }

    @Test
    public void testWrite1() {
        final byte value = 0x10;
        ptr.write1(value);
        byte res = ptr.read1();
        Assertions.assertEquals(value, res);
    }

    @Test
    public void testWrite1Offset() {
        final long offset = 0x353;
        final byte value = (byte) 0xC3;
        ptr.write1(offset, value);
        byte res = ptr.read1(offset);
        Assertions.assertEquals(value, res);
    }

    @Test
    public void testWrite2() {
        final byte value = 0x10;
        ptr.write2(value);
        short res = ptr.read2();
        Assertions.assertEquals(value, res);
    }

    @Test
    public void testWrite2Offset() {
        final long offset = 0xA7;
        final short value = (short) 0xF1C7;
        ptr.write2(offset, value);
        short res = ptr.read2(offset);
        Assertions.assertEquals(value, res);
    }

    @Test
    public void testWrite4() {
        final int value = 0xDEADBEEF;
        ptr.write4(value);
        int res = ptr.read4();
        Assertions.assertEquals(value, res);
    }

    @Test
    public void testWrite4Offset() {
        final long offset = 0xFC3;
        final int value = 0xFFFF01D5;
        ptr.write4(offset, value);
        int res = ptr.read4(offset);
        Assertions.assertEquals(value, res);
    }

    @Test
    public void testWrite8() {
        final long value = 0x0000FFD4564419A0L;
        ptr.write8(value);
        long res = ptr.read8();
        Assertions.assertEquals(value, res);
    }

    @Test
    public void testWrite8Offset() {
        final long offset = 0xA05;
        final long value = 0;
        ptr.write8(offset, value);
        long res = ptr.read8(offset);
        Assertions.assertEquals(value, res);
    }

    @Test
    public void testWriteBuffer() {
        byte[] buf = new byte[] {
                (byte) 0xC5, 0x11, 0x33, (byte) 0xD1, 0x57, 0x4A, 0x55,
                (byte) 0xA0, (byte) 0x87, (byte) 0xFF, 0x00, 0x42,
                (byte) 0xE0, 0x49, (byte) 0xD0
        };
        ptr.write(buf);
        byte[] result = ptr.read(buf.length);
        Assertions.assertArrayEquals(buf, result);
    }

    @Test
    public void testWriteString() {
        String test = "Test string";
        ptr.writeString(test);
        byte[] res = ptr.read(test.length() + 2);
        Assertions.assertArrayEquals(new byte[] {
                0x54, 0x65, 0x73, 0x74, 0x20, 0x73, 0x74, 0x72, 0x69, 0x6E, 0x67, 0x00, 0x0D
        }, res);
    }

    @Test
    public void testCopyToSuccess() {
        TestPointer newPtr = new TestPointer(0x200000, new Long(0x1000L));
        ptr.copyTo(newPtr, 5, 5);
        byte[] res = newPtr.read(10);
        Assertions.assertArrayEquals(new byte[] {
                0x06, 0x07, 0x08, 0x09, 0x0A, 0x06, 0x07, 0x08, 0x09, 0x0A
        }, res);
    }

    @Test
    public void testCopyOverflowSource() {
        try {
            TestPointer newPtr = new TestPointer(0x200000, new Long(0x1000L));
            ptr.copyTo(newPtr, 0, 0x10000);
        } catch (IndexOutOfBoundsException ex) {
            Assertions.assertEquals("65536", ex.getMessage());
        }
    }

    @Test
    public void testCopyOverflowDest() {
        try {
            TestPointer newPtr = new TestPointer(0x200000, new Long(0x4L));
            ptr.copyTo(newPtr, 0, 0x10);
        } catch (IndexOutOfBoundsException ex) {
            Assertions.assertEquals("16", ex.getMessage());
        }
    }

    @Test
    public void testNotEqualsDifferentType() {
        Assertions.assertNotEquals("0x400000", ptr);
    }

    @Test
    public void testEqualsByAddress() {
        Assertions.assertEquals(new Pointer(ptr.addr()), ptr);
    }

    @Test
    public void testContainsInHashset() {
        Set set = new HashSet();
        set.add(ptr);
        Assertions.assertTrue(set.contains(new Pointer(ptr.addr())));
    }

    @Test
    public void testToString8() {
        Assertions.assertEquals("0x00400000", ptr.toString());
    }

    @Test
    public void testToString16() {
        Assertions.assertEquals("0x0000ff0000000000", (new TestPointer(0xFF0000000000L)).toString());
    }
}
