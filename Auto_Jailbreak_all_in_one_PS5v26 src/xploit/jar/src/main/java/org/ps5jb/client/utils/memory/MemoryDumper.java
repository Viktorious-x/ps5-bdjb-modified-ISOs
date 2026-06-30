package org.ps5jb.client.utils.memory;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.AbstractPointer;

/**
 * Utility class to dump memory in hexadecimal format.
 */
public class MemoryDumper {
    /**
     * Dump <code>size</code> bytes of memory starting at address <code>buf</code>.
     *
     * @param buf Starting address for the dump.
     * @param size Number of bytes to dump.
     * @param relative If true, printed offsets will start at 0.
     *   If false, offsets will show actual memory address.
     */
    public static void dump(AbstractPointer buf, long size, boolean relative) {
        StringBuffer sb = new StringBuffer(110);
        for (int j = 0; j < size; j += 0x10) {
            sb.append(AbstractPointer.toString(relative ? j : buf.addr() + j));
            sb.append(":   ");

            for (int i = 0; i < 2; ++i) {
                if ((j + i * 8 + 0x8) <= size) {
                    long value = buf.read8(j + i * 8);
                    for (int k = 0; k < 8; ++k) {
                        String hex = Long.toHexString((value >> (k * 8)) & 0xFF);
                        if (k != 0) {
                            sb.append(" ");
                        } else if (i == 1) {
                            sb.append("      ");
                        }
                        if (hex.length() == 1) {
                            sb.append("0");
                        }
                        sb.append(hex);
                    }
                } else {
                    for (int k = 0; (j + i * 8 + k) < size; ++k) {
                        byte val = buf.read1(j + i * 8 + k);
                        String hex = Integer.toHexString(val & 0xFF);
                        if (k != 0) {
                            sb.append(" ");
                        } else if (i == 1) {
                            sb.append("      ");
                        }
                        if (hex.length() == 1) {
                            sb.append("0");
                        }
                        sb.append(hex);
                    }
                }
            }

            Status.println(sb.toString());
            sb.setLength(0);
        }
    }
}
