package org.ps5jb.client.payloads.umtx.impl1;

import java.util.Arrays;

import org.ps5jb.client.utils.memory.MemoryDumper;
import org.ps5jb.sdk.core.AbstractPointer;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.kernel.KernelPointer;

public class MemoryBuffer {
    private final AbstractPointer ptr;

    private byte[] snapshot;

    public MemoryBuffer(AbstractPointer ptr, long size) {
        if (ptr.size() == null || ptr.size().longValue() != size) {
            if (ptr instanceof Pointer) {
                this.ptr = new Pointer(ptr.addr(), new Long(size));
            } else {
                this.ptr = new KernelPointer(ptr.addr(), new Long(size));
            }
        } else {
            this.ptr = ptr;
        }
    }

    public long getAddr() {
        return this.ptr.addr();
    }

    public long getSize() {
        return this.ptr.size().longValue();
    }

    public byte read8(long offset) {
        return this.ptr.read1(offset);
    }

    public short read16(long offset) {
        return this.ptr.read2(offset);
    }

    public long read64(long offset) {
        return this.ptr.read8(offset);
    }

    public void dump() {
        MemoryDumper.dump(this.ptr, getSize(), true);
    }

    public void snapshot() {
        long size = getSize();

        if (snapshot == null) {
            snapshot = new byte[(int) size];
        }

        for (int i = 0; i < size; i += 8) {
            if ((i + 8) <= size) {
                long val = read64(i);
                for (int j = 0; j < 8; ++j) {
                    snapshot[i + j] = (byte) ((val >> (j * 8)) & 0xFF);
                }
            } else {
                for (int j = 0; (i + j) < size; ++j) {
                    byte val = read8(i + j);
                    snapshot[i + j] = val;
                }
            }
        }
    }

    public void clearSnapshot() {
        snapshot = null;
    }

    public long readSnapshot64(long offset) {
        long result = 0;
        if (snapshot != null) {
            long size = getSize();
            for (long i = 0; i < 8; ++i) {
                if ((offset + i) < size) {
                    result = result | (((long) (snapshot[(int) (offset + i)] & 0xFF)) << (i * 8));
                } else {
                    break;
                }
            }
        }
        return result;
    }

    public long find(AbstractPointer pattern, int size) {
        byte[] val = pattern.read(size);

        // TODO: this can probably be searching with alignment in mind rather than byte-by-byte?
        byte[] nextVal = new byte[size];
        long curOffset = 0;
        while ((curOffset + size) <= getSize()) {
            this.ptr.read(curOffset, nextVal, 0, size);
            if (Arrays.equals(val, nextVal)) {
                return curOffset;
            }
            curOffset++;
        }

        return -1;
    }
}
