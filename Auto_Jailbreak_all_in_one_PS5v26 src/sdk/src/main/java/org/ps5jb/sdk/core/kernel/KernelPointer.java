package org.ps5jb.sdk.core.kernel;

import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.sdk.core.AbstractPointer;

/**
 * Abstraction over memory pointer operations in kernel-space.
 * This class is only usable if a global kernel accessor has been installed
 * by calling {@link KernelReadWrite#setAccessor(KernelAccessor)}.
 */
public class KernelPointer extends AbstractPointer {
    private static final long serialVersionUID = 3445279334363239515L;

    /** Start of kernel address space. See machine/vmparam.h */
    private static final long KERNEL_ADDR_MASK = 0xFFFF800000000000L;

    /** Flag which makes sure that range of kernel space is validated prior to accessing the memory. */
    private boolean rangeValidated = false;

    /** Whether kernel accessor should be cached on first access, for the duration of this pointer instance. */
    private boolean cacheKernelAccessor = false;

    /** Cached kernel accessor. Note that if this pointer deserialized then the cached accessor is not restored. */
    private transient KernelAccessor kernelAccessor = null;

    /**
     * Returns a kernel pointer instance with a cached kernel accessor. Note that
     * cached accessor instance does not survive serialization.
     *
     * @param addr Address to convert to a Pointer instance.
     * @return Pointer instance representing the given native memory address.
     */
    public static KernelPointer valueOf(long addr) {
        return valueOf(addr, true);
    }

    /**
     * Returns a pointer instance equivalent to the given native memory address.
     *
     * @param addr Address to convert to a Pointer instance.
     * @param cacheAccessor Whether to cache the kernel accessor instance active
     *   at the time of this pointer creation. Doing so improves performance
     *   but if the application changes to a different accessor implementation,
     *   this pointer instance will not see this change.
     * @return Pointer instance representing the given native memory address.
     */
    public static KernelPointer valueOf(long addr, boolean cacheAccessor) {
        return addr == 0 ? NULL : new KernelPointer(addr, null, cacheAccessor);
    }

    /**
     * Validates that the given kernel pointer has the correct kernel-space range.
     *
     * @param pointer Pointer to validate.
     * @return Same pointer instance.
     * @throws IllegalAccessError If the pointer is invalid (including NULL).
     */
    public static KernelPointer validRange(KernelPointer pointer) {
        if ((((pointer.addr() & KERNEL_ADDR_MASK) != KERNEL_ADDR_MASK) || pointer.addr() == -1)) {
            throw new IllegalAccessError(pointer.toString());
        }
        return pointer;
    }

    /** Static constant for NULL pointer. */
    public static final KernelPointer NULL = new KernelPointer(0, null, false);

    /**
     * Constructor of kernel pointer from a memory address. The size
     * will be unbounded and the kernel accessor will be cached.
     *
     * @param addr Start address of this pointer.
     */
    public KernelPointer(long addr) {
        this(addr, null);
    }

    /**
     * Constructor of kernel pointer from a memory address and
     * a given size. Access beyond the range defined by addr + size
     * will result in an exception, if size is non null. Instance
     * of kernel accessor will be cached.
     *
     * @param addr Start address of this pointer.
     * @param size Size of the area.
     */
    public KernelPointer(long addr, Long size) {
        this(addr, size, true);
    }

    /**
     * Constructor of kernel pointer from a memory address and
     * a given size. Access beyond the range defined by addr + size
     * will result in an exception, if size is non null. Caching
     * of kernel accessor instance can be controlled with
     * <code>cacheAccessor</code> parameter.
     *
     * @param addr Start address of this pointer.
     * @param size Size of the area.
     * @param cacheAccessor Whether to cache the kernel accessor
     *   for the lifetime of this pointer instance. Note that
     *   the accessor is not acquired on pointer instantiation.
     *   It's cached at the time of first access to memory.
     */
    public KernelPointer(long addr, Long size, boolean cacheAccessor) {
        this(addr, size, cacheAccessor, null);
    }

    /**
     * Constructor of kernel pointer from a memory address and
     * a given size. Access beyond the range defined by addr + size
     * will result in an exception, if size is non null. Control
     * whether kernel accessor will be cached, and if so, which
     * accessor instance is cached.
     *
     * @param addr Start address of this pointer.
     * @param size Size of the area.
     * @param kernelAccessor Kernel accessor instance to use and cache.
     */
    public KernelPointer(long addr, Long size, boolean cacheAccessor, KernelAccessor kernelAccessor) {
        super(addr, size);
        this.cacheKernelAccessor = cacheAccessor;
        if (this.cacheKernelAccessor) {
            this.kernelAccessor = kernelAccessor;
        }
    }

    /**
     * Retrieves the kernel accessor instance used by this pointer.
     * If pointer is set to cache the accessor, then the first call
     * to this method will acquire the currently set global
     * accessor for this class's class loader and cache this instance.
     *
     * @return Kernel accessor instance used by this pointer.
     */
    public KernelAccessor getKernelAccessor() {
        KernelAccessor result = kernelAccessor;
        if (result == null) {
            result = KernelReadWrite.getAccessor(getClass().getClassLoader());
            if (cacheKernelAccessor) {
                kernelAccessor = result;
            }
        }
        return result;
    }

    /**
     * Whether the current pointer instance is caching the kernel accessor.
     * If accessor is not cached, then the pointer will use the new
     * accessor instance if switches globally.
     *
     * @return Whether accessor is cached or not.
     */
    public boolean isCacheKernelAccessor() {
        return cacheKernelAccessor;
    }

    /**
     * Make sure the current pointer has a valid kernel space range.
     */
    protected void checkRange() {
        if (!rangeValidated) {
            validRange(this);
            rangeValidated = true;
        }
    }

    @Override
    public byte read1(long offset) {
        checkRange();
        return super.read1(offset);
    }

    @Override
    public short read2(long offset) {
        checkRange();
        return super.read2(offset);
    }

    @Override
    public int read4(long offset) {
        checkRange();
        return super.read4(offset);
    }

    @Override
    public long read8(long offset) {
        checkRange();
        return super.read8(offset);
    }

    @Override
    public void read(long offset, byte[] value, int valueIndex, int count) {
        checkRange();

        KernelAccessor ka = getKernelAccessor();
        // When using specialized accessors, use a more efficient read method
        // rather that writing 8-bytes at a time.
        if (ka instanceof KernelAccessorIPv6) {
            KernelAccessorIPv6 kaIpv6 = (KernelAccessorIPv6) ka;
            kaIpv6.read(this.addr + offset, value, valueIndex, count);
        } else if (ka instanceof KernelAccessorAgc) {
            KernelAccessorAgc kaAgc = (KernelAccessorAgc) ka;
            kaAgc.read(this.addr + offset, value, valueIndex, count);
        } else {
            super.read(offset, value, valueIndex, count);
        }
    }

    @Override
    public void write1(long offset, byte value) {
        checkRange();
        super.write1(offset, value);
    }

    @Override
    public void write2(long offset, short value) {
        checkRange();
        super.write2(offset, value);
    }

    @Override
    public void write4(long offset, int value) {
        checkRange();
        super.write4(offset, value);
    }

    @Override
    public void write8(long offset, long value) {
        checkRange();
        super.write8(offset, value);
    }

    @Override
    public void write(long offset, byte[] value, int valueIndex, int count) {
        checkRange();

        KernelAccessor ka = getKernelAccessor();
        // When using specialized accessors, use a more efficient write method
        // rather that writing 8-bytes at a time.
        if (ka instanceof KernelAccessorIPv6) {
            KernelAccessorIPv6 kaIpv6 = (KernelAccessorIPv6) ka;
            kaIpv6.write(this.addr + offset, value, valueIndex, count);
        } else if (ka instanceof KernelAccessorAgc) {
            KernelAccessorAgc kaAgc = (KernelAccessorAgc) ka;
            kaAgc.write(this.addr + offset, value, valueIndex, count);
        } else {
            super.write(offset, value, valueIndex, count);
        }
    }

    @Override
    protected byte read1impl(long offset) {
        return getKernelAccessor().read1(this.addr + offset);
    }

    @Override
    protected short read2impl(long offset) {
        return getKernelAccessor().read2(this.addr + offset);
    }

    @Override
    protected int read4impl(long offset) {
        return getKernelAccessor().read4(this.addr + offset);
    }

    @Override
    protected long read8impl(long offset) {
        return getKernelAccessor().read8(this.addr + offset);
    }

    @Override
    protected void write1impl(long offset, byte value) {
        getKernelAccessor().write1(this.addr + offset, value);
    }

    @Override
    protected void write2impl(long offset, short value) {
        getKernelAccessor().write2(this.addr + offset, value);
    }

    @Override
    protected void write4impl(long offset, int value) {
        getKernelAccessor().write4(this.addr + offset, value);
    }

    @Override
    protected void write8impl(long offset, long value) {
        getKernelAccessor().write8(this.addr + offset, value);
    }

    /**
     * Increment the pointer by a given offset. The <code>delta</code> parameter may be negative.
     *
     * @param delta Offset from the current address of this pointer.
     * @return New pointer instance whose size is unknown and whose address is the address of
     *   this pointer shifted by <code>delta</code> bytes. If this instance has a cached
     *   kernel accessor then the new pointer instance will also have it cached.
     */
    public KernelPointer inc(long delta) {
        long newAddr = this.addr + delta;

        // Size is intentionally left unknown
        return new KernelPointer(newAddr, null, this.cacheKernelAccessor, this.getKernelAccessor());
    }

    /**
     * Same as {@link #pptr(long, Long)} with no size.
     *
     * @param offset Offset from he start of this pointer
     *   at which to read the pointer address.
     * @return Equivalent of calling {@link  #pptr(long, Long) pptr(offset, null)}.
     */
    public KernelPointer pptr(long offset) {
        return pptr(offset, null);
    }

    /**
     * Read a long value at the given offset and treat it as a kernel
     * pointer using the same accessor caching parameters as the
     * current pointer.
     *
     * @param offset Offset from he start of this pointer
     *   at which to read the pointer address.
     * @param size Size of the returned pointer. May be null
     *   to disable overflow checks.
     * @return New pointer instance whose accessor caching parameters are the
     *   same as this pointer instance. If the address read from
     *   the offset is 0, the {@link KernelPointer#NULL} is returned.
     */
    public KernelPointer pptr(long offset, Long size) {
        long newAddr = this.read8(offset);

        KernelPointer result;
        if (newAddr == 0) {
            result = KernelPointer.NULL;
        } else {
            result = new KernelPointer(newAddr, null, this.cacheKernelAccessor, this.getKernelAccessor());
        }

        return result;
    }
}
