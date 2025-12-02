package org.ps5jb.test.sdk.core.dataview;

import java.nio.charset.Charset;

import jdk.internal.misc.Unsafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ps5jb.sdk.core.dataview.ByteViewPointer;

public class ByteViewPointerTestCase {
    private ByteViewPointer ptr;

    @BeforeEach
    protected void setUpTest() {
        byte[] initData = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A };

        ptr = new ByteViewPointer(initData.length);
        System.arraycopy(initData, 0, ptr.dataView(),0, initData.length);
    }

    @AfterEach
    protected void tearDownTest() {
        if (ptr != null) {
            ptr.free();
        }
    }

    @Test
    public void testCreation() {
        Assertions.assertEquals(new Long(10), ptr.size());
        Assertions.assertNotEquals(0L, ptr.addr());
    }

    @Test
    public void testReadUnderflow() {
        try {
            ptr.read1(-1);
            Assertions.fail("Should have thrown IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException ex) {
            Assertions.assertEquals("-1", ex.getMessage());
        }
    }

    @Test
    public void testReadOverflow() {
        try {
            ptr.read1(0x20);
            Assertions.fail("Should have thrown IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException ex) {
            Assertions.assertEquals("33", ex.getMessage());
        }
    }

    @Test
    public void testRead1() {
        byte res = ptr.read1();
        Assertions.assertEquals((byte) 0x01, res);
    }

    @Test
    public void testRead1Offset() {
        byte res = ptr.read1(5);
        Assertions.assertEquals((byte) 0x06, res);
    }

    @Test
    public void testRead2() {
        short res = ptr.read2();
        Assertions.assertEquals((short) 0x0201, res);
    }

    @Test
    public void testRead2Offset() {
        short res = ptr.read2(7);
        Assertions.assertEquals((short) 0x0908, res);
    }

    @Test
    public void testRead4() {
        int res = ptr.read4();
        Assertions.assertEquals(0x04030201, res);
    }

    @Test
    public void testRead4Offset() {
        int res = ptr.read4(3);
        Assertions.assertEquals(0x07060504, res);
    }

    @Test
    public void testRead8() {
        long res = ptr.read8();
        Assertions.assertEquals(0x0807060504030201L, res);
    }

    @Test
    public void testRead8Offset() {
        long res = ptr.read8(2);
        Assertions.assertEquals(0x0A09080706050403L, res);
    }

    @Test
    public void testReadSize() {
        byte[] res = ptr.read(3);
        Assertions.assertArrayEquals(new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x03 }, res);
    }

    @Test
    public void testReadStringMaxLength() {
        ptr.write(new byte[] { 0x41, 0x42, 0x43, 0x44, 0x00 });
        String res = ptr.readString(new Integer(3));
        Assertions.assertEquals("ABC", res);
    }

    @Test
    public void testReadStringUnlimited() {
        ptr.write(new byte[] { 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x00 });
        String res = ptr.readString(2, null, Charset.defaultCharset().name());
        Assertions.assertEquals("CDEF", res);
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
        final long offset = 0x2;
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
        final long offset = 0x5;
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
        final long offset = 0x3;
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
        final long offset = 0x1;
        final long value = 0;
        ptr.write8(offset, value);
        long res = ptr.read8(offset);
        Assertions.assertEquals(value, res);
    }

    @Test
    public void testWriteBuffer() {
        byte[] buf = new byte[] {
                (byte) 0xC5, 0x11, 0x33, (byte) 0xD1, 0x57, 0x4A
        };
        ptr.write(buf);
        byte[] result = ptr.read(buf.length);
        Assertions.assertArrayEquals(buf, result);
    }

    @Test
    public void testWriteString() {
        String test = "Test str";
        ptr.writeString(test);
        byte[] res = ptr.read(test.length() + 2);
        Assertions.assertArrayEquals(new byte[] {
                0x54, 0x65, 0x73, 0x74, 0x20, 0x73, 0x74, 0x72, 0x00, 0x0A
        }, res);
    }

    @Test
    public void testGarbageCollection() {
        Object[] val = new Object[1];
        long origAddr = 0;
        for (int i = 0; i < 100; ++i) {
            byte[] dataView = ptr.dataView();
            val[0] = dataView;
            long addr = Unsafe.getUnsafe().getLong(val, Unsafe.ARRAY_OBJECT_BASE_OFFSET);

            if (origAddr == 0) {
                origAddr = addr;
            } else {
                // Make sure that data view always points to the same address
                Assertions.assertEquals(origAddr, addr);
            }

            System.gc();
        }
    }

    @Test
    public void testDataViewSpeed() {
        int attemptCount = 10000000;
        long addr = ptr.addr();
        int size = ptr.size().intValue();

        for (int j = 0; j < 2; ++j) {
            Unsafe unsafe = Unsafe.getUnsafe();
            long startUnsafe = System.currentTimeMillis();
            for (int i = 0; i < attemptCount; ++i) {
                unsafe.putByte(addr + (i % size), (byte) i);
            }
            long durationUnsafe = System.currentTimeMillis() - startUnsafe;

            long startDataView = System.currentTimeMillis();
            byte[] dataView = ptr.dataView();
            for (int i = 0; i < attemptCount; ++i) {
                dataView[i % size] = (byte) i;
            }
            // Required on non-PS5 to avoid GC crash
            dataView = null;
            long durationDataView = System.currentTimeMillis() - startDataView;

            System.gc();

            if (durationUnsafe < durationDataView) {
                // Don't fail with an assertion but do print out unexpected timing
                System.err.println("Unexpected data view speed test results. Data View: " + durationDataView + "ms; Data View with Init: " + durationDataView + "ms; Unsafe: " + durationUnsafe + "ms");
            }
        }
    }
}
