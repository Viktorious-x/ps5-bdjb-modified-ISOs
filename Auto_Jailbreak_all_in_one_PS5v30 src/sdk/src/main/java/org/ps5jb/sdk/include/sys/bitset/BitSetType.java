package org.ps5jb.sdk.include.sys.bitset;

import org.ps5jb.sdk.core.Pointer;

/**
 * Wrapper for FreeBSD bitset operations. The methods closely follow
 * the macros defined in FreeBSD <code>include/sys/bitset.h</code> header.
 */
public class BitSetType {
    private static final int _BITSET_BITS = 8 * 8;

    private final Pointer ptr;
    private final long[] __bits;
    private final int __bitset_words;

    /**
     * Bitset constructor.
     *
     * @param pointer Native memory where bitset data is to be stored.
     * @param size Size of the bitset in bytes.
     */
    public BitSetType(Pointer pointer, int size) {
        this.ptr = pointer;
        this.__bitset_words = (size + (_BITSET_BITS - 1)) / _BITSET_BITS;
        this.__bits = new long[this.__bitset_words];
        this.refresh();
    }

    private long __bitset_mask(int n) {
        return 1L << ((__bitset_words == 1) ? n : (n % _BITSET_BITS));
    }

    private int __bitset_word(int n) {
        return (__bitset_words == 1) ? 0 : (n / _BITSET_BITS);
    }

    private long __bitcountl(long _x) {
        _x = (_x & 0x5555555555555555L) + ((_x & 0xaaaaaaaaaaaaaaaaL) >> 1);
        _x = (_x & 0x3333333333333333L) + ((_x & 0xccccccccccccccccL) >> 2);
        _x = (_x + (_x >> 4)) & 0x0f0f0f0f0f0f0f0fL;
        _x = (_x + (_x >> 8));
        _x = (_x + (_x >> 16));
        _x = (_x + (_x >> 32)) & 0x000000ff;
        return _x;
    }

    /**
     * Returns the size in bytes occupied by this bit set.
     *
     * @return Bitset size in bytes.
     */
    public long getSize() {
        return this.__bitset_words * 8L;
    }

    /**
     * Determines whether the specified bit is set.
     *
     * @param bitIndex Index of the bit to check.
     * @return True if the bit specified by <code>bitIndex</code> is not zero.
     */
    public boolean isSet(int bitIndex) {
        long word = this.__bits[__bitset_word(bitIndex)];
        return (word & __bitset_mask(bitIndex)) != 0;
    }

    /**
     * Sets the specified bit of the set.
     *
     * @param bitIndex Index of the bit to set.
     */
    public void set(int bitIndex) {
        int wordIndex = __bitset_word(bitIndex);
        this.__bits[wordIndex] |= __bitset_mask(bitIndex);
        this.ptr.write8(8L * wordIndex, this.__bits[wordIndex]);
    }

    /**
     * Clear the specified bit of the set.
     *
     * @param bitIndex Index of the bit to clear.
     */
    public void unset(int bitIndex) {
        int wordIndex = __bitset_word(bitIndex);
        this.__bits[wordIndex] &= ~__bitset_mask(bitIndex);
        this.ptr.write8(8L * wordIndex, this.__bits[wordIndex]);
    }

    /**
     * Clear all bits of the set.
     */
    public void zero() {
        for (int i = 0; i < __bitset_words; ++i) {
            this.__bits[i] = 0L;
            this.ptr.write8(8L * i, this.__bits[i]);
        }
    }

    /**
     * Counts the number of set bits.
     *
     * @return Number of set bits.
     */
    public int getCount() {
        long count = 0;
        for (int i = 0; i < this.__bitset_words; ++i) {
            count += __bitcountl(this.__bits[i]);
        }
        return (int) count;
    }

    /**
     * Gets the native memory pointer where this bit set's data is stored.
     *
     * @return Bitset memory pointer.
     */
    public Pointer getPointer() {
        return this.ptr;
    }

    /**
     * Updates the value of this bitset in case the native memory was changed externally.
     */
    public void refresh() {
        for (int i = 0; i < this.__bitset_words; ++i) {
            this.__bits[i] = ptr.read8(i * 8L);
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        boolean hasNonZero = false;
        for (int i = this.__bitset_words - 1; i >= 0; --i) {
            if (this.__bits[i] != 0) {
                hasNonZero = true;
            }

            if (this.__bits[i] != 0 || hasNonZero) {
                String binString = Long.toBinaryString(this.__bits[i]);
                if (!hasNonZero) {
                    for (int j = binString.length(); j < 64; ++j) {
                        sb.append("0");
                    }
                }
                sb.append(binString);
            }
        }
        return sb.toString();
    }
}
