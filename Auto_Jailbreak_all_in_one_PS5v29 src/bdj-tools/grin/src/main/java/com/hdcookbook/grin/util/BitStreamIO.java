
/*  
 * Copyright (c) 2010, Sun Microsystems, Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of Sun Microsystems nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 *  Note:  In order to comply with the binary form redistribution 
 *         requirement in the above license, the licensee may include 
 *         a URL reference to a copy of the required copyright notice, 
 *         the list of conditions and the disclaimer in a human readable 
 *         file with the binary form of the code that is subject to the
 *         above license.  For example, such file could be put on a 
 *         Blu-ray disc containing the binary form of the code or could 
 *         be put in a JAR file that is broadcast via a digital television 
 *         broadcast medium.  In any event, you must include in any end 
 *         user licenses governing any code that includes the code subject 
 *         to the above license (in source and/or binary form) a disclaimer 
 *         that is at least as protective of Sun as the disclaimers in the 
 *         above license.
 * 
 *         A copy of the required copyright notice, the list of conditions and
 *         the disclaimer will be maintained at 
 *         https://hdcookbook.dev.java.net/misc/license.html .
 *         Thus, licensees may comply with the binary form redistribution
 *         requirement with a text file that contains the following text:
 * 
 *             A copy of the license(s) governing this code is located
 *             at https://hdcookbook.dev.java.net/misc/license.html
 */

package com.hdcookbook.grin.util;

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This contains utility methods to help read and write bit streams.
 * This can be handy, for example, to read and write things like the
 * BDJO data structure (part 3-2 section 10.2.1.2) or the index.bdmv
 * structure (part 3-2 section 5.2.3.2). For example, a structure like
 * this:
 * <pre>
 *      isFirst                  1 uimsbf
 *      isPretty                 1 uimsbf
 *      randomFlags              3 uimsbf
 *      reserved_for_word_align 11 bslbf
 *      someNameLength           8 uimsbf
 *      for (i = 0; i < someNameLength; i++) {
 *          someNameByte         8 uimsbf
 *      }
 *
 *  where someName is encoded in UTF8
 * </pre>
 * Could be read with:
 * <pre>
 *    DataInputStream dis = ...;
 *    BitStreamIO bio = new BitStreamIO();
 *
 *    boolean isFirst = (bio.readBits(dis, 1)) != 0;
 *    boolean isPretty = (bio.readBits(dis, 1)) != 0;
 *    int someBits = bio.readBits(dis, 3);      // 0 <= someBits < 8
 *    bio.readBits(dis, 11);
 *    if (Debug.ASSERT) {
 *        bio.assertByteAligned(1);
 *    }
 *    byte[] buf = new byte[bio.readBits(dis, 8)];
 *    dis.read(buf);
 *    String someName = new String(buf, "UTF-8");
 * </pre>
 * And it could be written with:
 * <pre>
 *    DataOutputStream dos= ...;
 *    BitStreamIO bio = new BitStreamIO();
 *
 *    bio.writeBits(dos, 1, (isFirst ? 1 : 0));
 *    bio.writeBits(dos, 1, (isPretty ? 1 : 0));
 *    bio.writeBits(dos, 3, someBits);
 *    bio.writeBits(dos, 11, 0);
 *    if (Debug.ASSERT) {
 *        bio.assertByteAligned(1);
 *    }
 *    byte[] buf = someName.getBytes("UTF-8");
 *    bio.writeBits(dos, 8, buf.length);
 *    dos.write(buf);
 * </pre>
 *
 * Note how you use the underlying data stream directly when reading and writing
 * byte quantities, and only use the bio helper when dealing with bits.  
 * A more elegant and OO design would have been to make a BitStreamReader
 * and BitStreamWriter class that extended DataInputStream and DataOutputStream,
 * but that would have been bigger and slower to classload.  On Blu-ray 
 * players, there's a substantial penalty (on the order of 30ms) per class 
 * loaded, so it's good for performance to minimize the number of classes.
 * <p>
 * This code is probably a bit slower than hand-written shifts and
 * masks, but it's simpler, and perfectly adequate for the small
 * binary data structures that are typical of Blu-ray and many other
 * environments.
 *
 * @author Bill Foote (http://jovial.com)
 */

public class BitStreamIO {

    private int bitsProcessed = 0;
    private int buf = 0;        // Only used to hold one byte
    private final static int[] maskArr = { 0x00, 0x01, 0x03, 0x07, 0x0f,
                                                 0x1f, 0x3f, 0x7f, 0xff };

    /** 
     * Create a new BitStreamIO helper.  A given helper should be used
     * for either reading or writing, but not both.
     **/
    public BitStreamIO() {
    }

    /**
     * Read the given number of bits from dis.  No locking is performed; it
     * is assumed a BitStreamIO object is only used by one thread at a time.
     * Some bits will be stored in the helper if the read isn't byte-aligned
     * after this call.
     *
     *  @param  dis     The stream to read from.
     *  @param  numBits The number of bits to read, 0 <= numBits <= 32
     **/
    public int readBits(DataInputStream dis, int numBits) 
            throws IOException 
    {
        if (Debug.ASSERT && (numBits < 0 || numBits > 32)) {
            Debug.assertFail();
        }
        int result = 0;
        int bitsInBuf = ( 8 - (bitsProcessed & 7)) & 7;
        bitsProcessed += numBits;
        while (numBits > 0) {
            if (bitsInBuf == 0) {
                buf = ((int) dis.readByte()) & 0xff;
                bitsInBuf = 8;
            }
            // Now 0 < bitsInBuf <= 8
            if (numBits >= bitsInBuf) {
                    // We take all the bits down to the lsb
                result <<= bitsInBuf;
                int mask = maskArr[bitsInBuf];
                result |= (buf & mask);
                numBits -= bitsInBuf;
                bitsInBuf = 0;
            } else {    // numBits < bitsInBuf
                bitsInBuf -= numBits;
                result <<= numBits;
                int buf2 = buf >> bitsInBuf;    // That's the updated bitsInBuf
                int mask = maskArr[numBits];
                result |= buf2 & mask;
                numBits = 0;
            }
        }
        return result;
    }

    /**
     * Read the given number of bits from dis.  No locking is performed; it
     * is assumed a BitStreamIO object is only used by one thread at a time.
     * Some bits will be stored in the helper if the read isn't byte-aligned
     * after this call.
     *
     *  @param  dis     The stream to read from.
     *  @param  numBits The number of bits to read, 0 <= numBits <= 64
     **/
    public long readBitsLong(DataInputStream dis, int numBits) 
            throws IOException
    {
        if (Debug.ASSERT && (numBits < 0 || numBits > 64)) {
            Debug.assertFail();
        }
        long result = 0;
        int bitsInBuf = ( 8 - (bitsProcessed & 7)) & 7;
        bitsProcessed += numBits;
        while (numBits > 0) {
            if (bitsInBuf == 0) {
                buf = ((int) dis.readByte()) & 0xff;
                bitsInBuf = 8;
            }
            // Now 0 < bitsInBuf <= 8
            if (numBits >= bitsInBuf) {
                    // We take all the bits down to the lsb
                result <<= bitsInBuf;
                int mask = maskArr[bitsInBuf];
                result |= (buf & mask);
                numBits -= bitsInBuf;
                bitsInBuf = 0;
            } else {    // numBits < bitsInBuf
                bitsInBuf -= numBits;
                result <<= numBits;
                int buf2 = buf >> bitsInBuf;    // That's the updated bitsInBuf
                int mask = maskArr[numBits];
                result |= buf2 & mask;
                numBits = 0;
            }
        }
        return result;
    }

    /**
     * Write the given bits to dos.  No locking is performed; it
     * is assumed a BitStreamIO object is only used by one thread 
     * at a time. Some bits will be stored in the helper
     * if the result isn't byte-aligned at the end of this write.
     *
     * @param   dos     The stream to write to
     * @param   numBits The number of bits, 0 <= numBits <= 32
     * @param   value   The value to read (in the lower-order bits)
     **/
    public void writeBits(DataOutputStream dos, int numBits, int value) 
            throws IOException
    {
        if (Debug.ASSERT && (numBits < 0 || numBits > 32)) {
            Debug.assertFail();
        }
        int bitsInBuf = (bitsProcessed & 7);
        bitsProcessed += numBits;
        while (numBits > 0) {
            if (bitsInBuf > 0) {
                int available = 8 - bitsInBuf;  // 0 < available < 8
                if (available <= numBits) {     // Shift available into buf
                    int mask = maskArr[available];
                    buf |= (value >> (numBits - available)) & mask;
                    numBits -= available;
                    bitsInBuf += available;
                } else {                        // Shift numBits into buf
                    // 0 < numBits < available < 8
                    int mask = maskArr[numBits];
                    int buf2 = value & mask;
                    buf |= buf2 << (available - numBits);
                    bitsInBuf += numBits;
                    numBits = 0;
                }
            } else if (numBits >= 8) {
                buf = value >> (numBits - 8);
                    // Don't need "& 0xff" because dos.write() does that for us
                numBits -= 8;
                    // Don't need to mask off the higher-order bits we just
                    // put into buf
                bitsInBuf = 8;
            } else {
                // numBits < 8, bitsInBuf = 0;
                int mask = maskArr[numBits];
                int buf2 = value & mask;
                buf |= buf2 << (8 - numBits);
                bitsInBuf = numBits;
                numBits = 0;
            }
            if (bitsInBuf == 8) {
                dos.write(buf);
                buf = 0;
                bitsInBuf = 0;
            }
        }
    }

    public void writeBitsLong(DataOutputStream dos, int numBits, long value) 
            throws IOException 
    {
        if (Debug.ASSERT && (numBits < 0 || numBits > 64)) {
            Debug.assertFail();
        }
        int bitsInBuf = (bitsProcessed & 7);
        bitsProcessed += numBits;
        while (numBits > 0) {
            if (bitsInBuf > 0) {
                int available = 8 - bitsInBuf;  // 0 < available < 8
                if (available <= numBits) {     // Shift available into buf
                    int mask = maskArr[available];
                    buf |= (value >> (numBits - available)) & mask;
                    numBits -= available;
                    bitsInBuf += available;
                } else {                        // Shift numBits into buf
                    // 0 < numBits < available < 8
                    long mask = maskArr[numBits];
                    int buf2 = (int) (value & mask);
                    buf |= buf2 << (available - numBits);
                    bitsInBuf += numBits;
                    numBits = 0;
                }
            } else if (numBits >= 8) {
                buf = (int) (value >> (numBits - 8));
                    // Don't need "& 0xff" because dos.write() does that for us
                numBits -= 8;
                    // Don't need to mask off the higher-order bits we just
                    // put into buf
                bitsInBuf = 8;
            } else {
                // numBits < 8, bitsInBuf = 0;
                int mask = maskArr[numBits];
                int buf2 = (int) (value & mask);
                buf |= buf2 << (8 - numBits);
                bitsInBuf = numBits;
                numBits = 0;
            }
            if (bitsInBuf == 8) {
                dos.write(buf);
                buf = 0;
                bitsInBuf = 0;
            }
        }
    }

    /** 
     * Generate an assertion failure if the BitStreamIO helper isn't
     * aligned ot the given number of bytes.  assertByteAligned(1) asserts
     * it's byte aligned, assertByteAligned(4) asserts its word-aligned,
     * etc.
     **/
    public void assertByteAligned(int numBytes) {
        if (Debug.ASSERT) {
            int numBits = numBytes * 8;
            int bits = bitsProcessed % numBits;
            if (bits != 0) {
                Debug.assertFail(bits + " stray bits.");
            }
        }
    }

    /**
     * Get the number of bits processed by this helper.  This can be
     * useful for checking consisgtency, or for padding on byte or word
     * boundaries.
     **/
    public int getBitsProcessed() {
        return bitsProcessed;
    }
}
