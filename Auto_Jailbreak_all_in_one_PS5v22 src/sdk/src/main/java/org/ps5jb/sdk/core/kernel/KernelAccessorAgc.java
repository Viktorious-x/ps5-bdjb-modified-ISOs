package org.ps5jb.sdk.core.kernel;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.machine.PMap;
import org.ps5jb.sdk.include.machine.Param;
import org.ps5jb.sdk.include.sys.MMan;
import org.ps5jb.sdk.include.sys.errno.InvalidValueException;
import org.ps5jb.sdk.include.sys.errno.OperationNotPermittedException;
import org.ps5jb.sdk.include.sys.internal.gc.GpuPMap;
import org.ps5jb.sdk.include.sys.internal.gc.vm.GpuVm;
import org.ps5jb.sdk.include.sys.mman.ProtectionFlag;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.lib.LibGnmAwt;
import org.ps5jb.sdk.lib.LibKernel;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Kernel read/write accessor which uses PS5 AGC graphics API.
 * This API has direct memory access (DMA) functions that can
 * access kernel memory when mapped into GPU.
 * Using this accessor instance is necessary to perform
 * writes into certain regions of kernel data on firmware 6.00+.
 *
 * Technique suggested by flat_z.
 * Requires an existing kernel accessor on creation.
 */
public class KernelAccessorAgc implements KernelAccessor {
    private static final long serialVersionUID = -7160442787863971327L;

    private static final ProtectionFlag[] protRO = new ProtectionFlag[] {
            ProtectionFlag.PROT_READ,
            ProtectionFlag.PROT_WRITE,
            ProtectionFlag.PROT_GPU_READ
    };
    private static final ProtectionFlag[] protRW = new ProtectionFlag[] {
            ProtectionFlag.PROT_READ,
            ProtectionFlag.PROT_WRITE,
            ProtectionFlag.PROT_GPU_READ,
            ProtectionFlag.PROT_GPU_WRITE
    };

    private static final long DMA_SYNC_TIMEOUT_MS = 1000;

    private transient LibKernel libKernel;
    private transient LibGnmAwt libGnmAwt;
    private transient MMan mman;

    private KernelAccessor initialKernelAccessor;

    private KernelPointer curProcAddress;
    private KernelPointer kernelBase;
    private KernelPointer gvmAddress;

    private transient Process curProc;
    private transient KernelOffsets offsets;

    private Pointer[] victimBuffer;
    private Pointer[] transferBuffer;

    private KernelPointer victimBufferPteAddr;
    private long victimBufferPteInitialRW;
    private long victimBufferPteMaskRO;

    public KernelAccessorAgc(KernelPointer curProcAddress) {
        // Sanity checks
        if (curProcAddress == null || KernelPointer.NULL.equals(curProcAddress)) {
            throw new NullPointerException(ErrorMessages.getClassErrorMessage(getClass(), "curProcNull"));
        }

        this.initialKernelAccessor = curProcAddress.getKernelAccessor();
        if (this.initialKernelAccessor.getKernelBase() == 0) {
            throw new NullPointerException(ErrorMessages.getClassErrorMessage(getClass(), "kernelBaseNull"));
        }

        // Save input as cached pointers to use the initial kernel accessor for all the reads
        this.kernelBase = new KernelPointer(this.initialKernelAccessor.getKernelBase(), null, true, this.initialKernelAccessor);
        this.curProcAddress = new KernelPointer(curProcAddress.addr(), null, true, this.initialKernelAccessor);
        this.curProc = new Process(this.curProcAddress);

        // Init libkernel, its wrappers and determine offsets
        try {
            this.libKernel = new LibKernel();
            this.mman = new MMan(this.libKernel);
            offsets = new KernelOffsets(libKernel.getSystemSoftwareVersion());

            // Load Agc library
            this.initAwtGnmLib();

            // Init GPU vm space
            this.initGpuVm();

            // Allocate DMA transfer buffers
            this.initGpuBuffers();
        } catch (RuntimeException | Error e) {
            // Make sure to free what's been allocated if there is an initialization error
            free();

            throw e;
        }
    }

    public synchronized void free() {
        freeDirectMemory(transferBuffer, GpuPMap.DIRECT_MEMORY_PAGE_SIZE);
        freeDirectMemory(victimBuffer, GpuPMap.DIRECT_MEMORY_PAGE_SIZE);

        if (libGnmAwt != null) {
            libGnmAwt.closeLibrary();
            libGnmAwt = null;
        }

        if (libKernel != null) {
            libKernel.closeLibrary();
            libKernel = null;
        }
    }

    private void initAwtGnmLib() {
        // Refresh dmap parameters
        if (PMap.DMPDPI == 0) {
            KernelPointer kernelPmap = this.kernelBase.inc(this.offsets.OFFSET_KERNEL_DATA + this.offsets.OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE);
            PMap.refresh(kernelPmap.inc(this.offsets.OFFSET_PMAP_STORE_DMPML4I), kernelPmap.inc(this.offsets.OFFSET_PMAP_STORE_DMPDPI), kernelPmap.inc(this.offsets.OFFSET_PMAP_STORE_PML4PML4I));
        }

        // Load the library and resolve internal functions
        this.libGnmAwt = new LibGnmAwt();
        this.libGnmAwt.resolveInternal(this.curProc);
    }

    private void initGpuVm() {
        if (gvmAddress == null) {
            // Resolve GPU VM space
            gvmAddress = this.kernelBase.inc(this.offsets.OFFSET_KERNEL_DATA + this.offsets.OFFSET_KERNEL_DATA_BASE_GBASE_VM);
        }

        if (GpuVm.SIZE < 0) {
            // Construct the initial GpuVm object to determine the structure size
            new GpuVm(gvmAddress);
        }
    }

    private void initGpuBuffers() {
        // Allocate the buffers for DMA
        victimBuffer = allocGpuMappedDirectMemory(GpuPMap.DIRECT_MEMORY_PAGE_SIZE);
        transferBuffer = allocGpuMappedDirectMemory(GpuPMap.DIRECT_MEMORY_PAGE_SIZE);

        // Obtain and save PTE entry of the victim buffer from GPU VM space
        GpuVm gpuVm = new GpuVm(new KernelPointer(gvmAddress.addr() + curProc.getVmSpace().getGpuVmId() * GpuVm.SIZE, new Long(GpuVm.SIZE), true, gvmAddress.getKernelAccessor()));
        long[] victimBufferPhys = GpuPMap.gpu_vtophys(gpuVm, victimBuffer[0].addr());
        if (victimBufferPhys == null || victimBufferPhys[1] != GpuPMap.DIRECT_MEMORY_PAGE_SIZE) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(), "vtophysError", victimBuffer[0].toString()));
        }

        try {
            victimBufferPteAddr = new KernelPointer(victimBufferPhys[0], null, true, this.initialKernelAccessor);
            mman.memoryProtect(victimBuffer[0], GpuPMap.DIRECT_MEMORY_PAGE_SIZE, protRO);
            victimBufferPteMaskRO = victimBufferPteAddr.read8() & ~(victimBuffer[1].addr() + GpuPMap.DIRECT_MEMORY_OFFSET);

            mman.memoryProtect(victimBuffer[0], GpuPMap.DIRECT_MEMORY_PAGE_SIZE, protRW);
            victimBufferPteInitialRW = victimBufferPteAddr.read8();
        } catch (InvalidValueException | OperationNotPermittedException e) {
            throw new SdkRuntimeException(e);
        }
    }

    /**
     * Allocates the direct memory and maps it for R/W by the CPU and the GPU
     *
     * @param size Size of the memory to allocate
     * @return Array where the first component is the mapped address
     *   and the second component is the direct memory address.
     */
    private Pointer[] allocGpuMappedDirectMemory(long size) {
        Pointer[] result = new Pointer[] { Pointer.NULL, Pointer.NULL };

        int ret;
        Pointer dirMemStore = Pointer.calloc(8);
        try {
            if ((ret = libKernel.sceKernelAllocateMainDirectMemory(size, size, 12, dirMemStore)) == 0) {
                Pointer dirMem = new Pointer(dirMemStore.read8(), new Long(size));
                try {
                    result[1] = dirMem;

                    dirMemStore.write8(0);
                    if ((ret = libKernel.sceKernelMapDirectMemory(dirMemStore, size, ProtectionFlag.or(protRW), 0, dirMem, size)) == 0) {
                        Pointer dirMemMap = new Pointer(dirMemStore.read8(), new Long(size));
                        result[0] = dirMemMap;
                    } else {
                        throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(), "mapDirectMemErrorCode", "0x" + Integer.toHexString(ret)));
                    }
                } catch (RuntimeException | Error e) {
                    libKernel.sceKernelReleaseDirectMemory(dirMem, size);
                    throw e;
                }
            } else {
                throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(), "allocDirectMemErrorCode", "0x" + Integer.toHexString(ret)));
            }
        } finally {
            dirMemStore.free();
        }

        return result;
    }

    private void freeDirectMemory(Pointer[] buffer, long bufferSize) {
        if (buffer != null) {
            int ret;
            if (!Pointer.NULL.equals(buffer[0])) {
                if ((ret = libKernel.sceKernelMunmap(buffer[0], bufferSize)) != 0) {
                    throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(), "unmapDirectMemErrorCode", "0x" + Integer.toHexString(ret)));
                }
            }

            if (!Pointer.NULL.equals(buffer[1])) {
                if ((ret = libKernel.sceKernelReleaseDirectMemory(buffer[1], bufferSize)) != 0) {
                    throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(), "releaseDirectMemErrorCode", "0x" + Integer.toHexString(ret)));
                }
            }

            buffer[0] = Pointer.NULL;
            buffer[1] = Pointer.NULL;
        }
    }

    /**
     * Returns the initial kernel accessor instance that was active when
     * this accessor was constructed. This instance is used as part of
     * DMA transfer for calls to {@link #vtophys(long)}.
     *
     * @return Initial kernel accessor instance.
     */
    public KernelAccessor getInitialKernelAccessor() {
        return this.initialKernelAccessor;
    }

    /**
     * Uses the initial kernel r/w to resolve the virtual address to physical
     * if the current accessor instance is set as global.
     *
     * @param kernelAddress Virtual address to resolve.
     * @return Physical address.
     */
    private long vtophys(long kernelAddress) {
        return PMap.pmap_kextract(new KernelPointer(kernelAddress, null, true, this.initialKernelAccessor));
    }

    /**
     * Makes sure that the range of physical pages is contiguous.
     * Currently, this accessor only implements contiguous writes.
     *
     * @param kernelAddress Start address of the range.
     * @param size Number of bytes in the range.
     */
    private void checkRange(long kernelAddress, long size) {
        final long pageCount = Param.atop(size);
        final long paStart = vtophys(kernelAddress);
        final long paEnd = vtophys(kernelAddress + Param.ptoa(pageCount));

        final long actualPageCount = Param.atop(paEnd - paStart);
        if (actualPageCount != pageCount) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(),
                    "nonContiguousRange",
                    "0x" + Long.toHexString(kernelAddress),
                    "0x" + Long.toHexString(size),
                    Long.toString(pageCount),
                    Long.toString(actualPageCount)));
        }
    }

    @Override
    public byte read1(long kernelAddress) {
        final int size = 1;

        final DmaValueBuffer buf = new DmaValueBuffer();

        final long physAddress = vtophys(kernelAddress);
        if (!dmaTransfer(physAddress, buf, size, true)) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(),
                    "dmaRead.count",
                    new Integer(size), "0x" + Long.toHexString(physAddress), "0x" + Long.toHexString(kernelAddress)));
        }

        return buf.byteVal;
    }

    @Override
    public short read2(long kernelAddress) {
        final int size = 2;
        checkRange(kernelAddress, size);

        final DmaValueBuffer buf = new DmaValueBuffer();

        final long physAddress = vtophys(kernelAddress);
        if (!dmaTransferPaged(physAddress, buf, size, true)) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(),
                    "dmaRead.count",
                    new Integer(size), "0x" + Long.toHexString(physAddress), "0x" + Long.toHexString(kernelAddress)));
        }

        return buf.shortVal;
    }

    @Override
    public int read4(long kernelAddress) {
        final int size = 4;
        checkRange(kernelAddress, size);

        final DmaValueBuffer buf = new DmaValueBuffer();

        final long physAddress = vtophys(kernelAddress);
        if (!dmaTransferPaged(physAddress, buf, size, true)) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(),
                    "dmaRead.count",
                    new Integer(size), "0x" + Long.toHexString(physAddress), "0x" + Long.toHexString(kernelAddress)));
        }

        return buf.intVal;
    }

    @Override
    public long read8(long kernelAddress) {
        final int size = 8;
        checkRange(kernelAddress, size);

        final DmaValueBuffer buf = new DmaValueBuffer();

        final long physAddress = vtophys(kernelAddress);
        if (!dmaTransferPaged(physAddress, buf, size, true)) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(),
                    "dmaRead.count",
                    new Integer(size), "0x" + Long.toHexString(physAddress), "0x" + Long.toHexString(kernelAddress)));
        }

        return buf.longVal;
    }

    public void read(long kernelAddress, byte[] buffer, int offset, int length) {
        if ((buffer.length - offset) < length) {
            throw new IndexOutOfBoundsException();
        }

        checkRange(kernelAddress, length);

        final DmaValueBuffer buf = new DmaValueBuffer();
        buf.byteArrayVal = buffer;
        buf.arrayOffset = offset;

        final long physAddress = vtophys(kernelAddress);
        if (!dmaTransferPaged(physAddress, buf, length, true)) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(),
                    "dmaRead.count",
                    new Integer(length), "0x" + Long.toHexString(physAddress), "0x" + Long.toHexString(kernelAddress)));
        }
    }

    @Override
    public void write1(long kernelAddress, byte value) {
        final int size = 1;

        DmaValueBuffer buf = new DmaValueBuffer();
        buf.byteVal = value;

        long physAddress = vtophys(kernelAddress);
        if (!dmaTransfer(physAddress, buf, size, false)) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(),
                    "dmaWrite.count",
                    new Integer(size), "0x" + Long.toHexString(physAddress), "0x" + Long.toHexString(kernelAddress)));
        }
    }

    @Override
    public void write2(long kernelAddress, short value) {
        final int size = 2;
        checkRange(kernelAddress, size);

        DmaValueBuffer buf = new DmaValueBuffer();
        buf.shortVal = value;

        long physAddress = vtophys(kernelAddress);
        if (!dmaTransferPaged(physAddress, buf, size, false)) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(),
                    "dmaWrite.count",
                    new Integer(size), "0x" + Long.toHexString(physAddress), "0x" + Long.toHexString(kernelAddress)));
        }
    }

    @Override
    public void write4(long kernelAddress, int value) {
        final int size = 4;
        checkRange(kernelAddress, size);

        DmaValueBuffer buf = new DmaValueBuffer();
        buf.intVal = value;

        long physAddress = vtophys(kernelAddress);
        if (!dmaTransferPaged(physAddress, buf, size, false)) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(),
                    "dmaWrite.count",
                    new Integer(size), "0x" + Long.toHexString(physAddress), "0x" + Long.toHexString(kernelAddress)));
        }
    }

    @Override
    public void write8(long kernelAddress, long value) {
        final int size = 8;
        checkRange(kernelAddress, size);

        DmaValueBuffer buf = new DmaValueBuffer();
        buf.longVal = value;

        long physAddress = vtophys(kernelAddress);
        if (!dmaTransferPaged(physAddress, buf, size, false)) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(),
                    "dmaWrite.count",
                    new Integer(size), "0x" + Long.toHexString(physAddress), "0x" + Long.toHexString(kernelAddress)));
        }
    }

    public void write(long kernelAddress, byte[] buffer, int offset, int length) {
        if ((buffer.length - offset) < length) {
            throw new IndexOutOfBoundsException();
        }

        checkRange(kernelAddress, length);

        DmaValueBuffer buf = new DmaValueBuffer();
        buf.byteArrayVal = buffer;
        buf.arrayOffset = offset;

        long physAddress = vtophys(kernelAddress);
        if (!dmaTransferPaged(physAddress, buf, length, false)) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(),
                    "dmaWrite.count",
                    new Integer(length), "0x" + Long.toHexString(physAddress), "0x" + Long.toHexString(kernelAddress)));
        }
    }

    @Override
    public long getKernelBase() {
        return kernelBase.addr();
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();

        // If there is no current r/w, restore the initial one because it's needed for the rest of initialization.
        synchronized (KernelReadWrite.class) {
            if (KernelReadWrite.getAccessor(initialKernelAccessor.getClass().getClassLoader()) == null) {
                KernelReadWrite.setAccessor(initialKernelAccessor);
            }
        }

        // Make sure to set all stored kernel pointers to use the initial accessor
        curProcAddress = new KernelPointer(curProcAddress.addr(), curProcAddress.size(), true, initialKernelAccessor);
        kernelBase = new KernelPointer(kernelBase.addr(), kernelBase.size(), true, initialKernelAccessor);
        gvmAddress = new KernelPointer(gvmAddress.addr(), gvmAddress.size(), true, initialKernelAccessor);
        victimBufferPteAddr = new KernelPointer(victimBufferPteAddr.addr(), victimBufferPteAddr.size(), true, initialKernelAccessor);

        // Make sure to restore transient state when deserializing.
        libKernel = new LibKernel();
        mman = new MMan(libKernel);

        curProc = new Process(curProcAddress);
        offsets = new KernelOffsets(libKernel.getSystemSoftwareVersion());

        this.initAwtGnmLib();
        this.initGpuVm();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (victimBuffer != null && Pointer.NULL.equals(victimBuffer[0])) {
            // Don't allow to serialize if freed.
            throw new NotActiveException(ErrorMessages.getClassErrorMessage(getClass(), "nonSerializable"));
        }

        stream.defaultWriteObject();
    }

    /**
     * Helper class which allows to DMA transfer using Java types.
     */
    private static class DmaValueBuffer {
        protected byte byteVal;
        protected short shortVal;
        protected int intVal;
        protected long longVal;
        protected byte[] byteArrayVal;
        protected short[] shortArrayVal;
        protected int[] intArrayVal;
        protected long[] longArrayVal;
        protected int arrayOffset;

        protected void readVal(Pointer src, int size) {
            if (byteArrayVal != null) {
                src.read(0, byteArrayVal, arrayOffset, size);
            } else if ((size % 2) == 0 && shortArrayVal != null) {
                src.read(0, shortArrayVal, arrayOffset, size / 2);
            } else if ((size % 4) == 0 && intArrayVal != null) {
                src.read(0, intArrayVal, arrayOffset, size / 4);
            } else if ((size % 8) == 0 && longArrayVal != null) {
                src.read(0, longArrayVal, arrayOffset, size / 8);
            } else if (size == 1) {
                byteVal = src.read1();
            } else if (size == 2) {
                shortVal = src.read2();
            } else if (size == 4) {
                intVal = src.read4();
            } else if (size == 8) {
                longVal = src.read8();
            } else {
                throw new IllegalArgumentException("Invalid combination of user buffer and size");
            }
        }

        protected void readValFromByteBuffer(ByteBuffer src, int size) {
            if (byteArrayVal != null) {
                src.get(byteArrayVal, arrayOffset, size);
            } else if ((size % 2) == 0 && shortArrayVal != null) {
                src.asShortBuffer().get(shortArrayVal, arrayOffset, size / 2);
                src.position(src.position() + size);
            } else if ((size % 4) == 0 && intArrayVal != null) {
                src.asIntBuffer().get(intArrayVal, arrayOffset, size / 4);
                src.position(src.position() + size);
            } else if ((size % 8) == 0 && longArrayVal != null) {
                src.asLongBuffer().get(longArrayVal, arrayOffset, size / 8);
                src.position(src.position() + size);
            } else if (size == 1) {
                byteVal = src.get();
            } else if (size == 2) {
                shortVal = src.getShort();
            } else if (size == 4) {
                intVal = src.getInt();
            } else if (size == 8) {
                longVal = src.getLong();
            } else {
                throw new IllegalArgumentException("Invalid combination of user buffer and size");
            }
        }

        protected void writeVal(Pointer dst, int size) {
            if (byteArrayVal != null) {
                dst.write(0, byteArrayVal, arrayOffset, size);
            } else if ((size % 2) == 0 && shortArrayVal != null) {
                dst.write(0, shortArrayVal, arrayOffset, size / 2);
            } else if ((size % 4) == 0 && intArrayVal != null) {
                dst.write(0, intArrayVal, arrayOffset, size / 4);
            } else if ((size % 8) == 0 && longArrayVal != null) {
                dst.write(0, longArrayVal, arrayOffset, size / 8);
            } else if (size == 1) {
                dst.write1(byteVal);
            } else if (size == 2) {
                dst.write2(shortVal);
            } else if (size == 4) {
                dst.write4(intVal);
            } else if (size == 8) {
                dst.write8(longVal);
            } else {
                throw new IllegalArgumentException("Invalid combination of user buffer and size");
            }
        }

        protected void writeValToByteBuffer(ByteBuffer dst, int size) {
            if (byteArrayVal != null) {
                dst.put(byteArrayVal, arrayOffset, size);
            } else if ((size % 2) == 0 && shortArrayVal != null) {
                dst.asShortBuffer().put(shortArrayVal, arrayOffset, size / 2);
                dst.position(dst.position() + size);
            } else if ((size % 4) == 0 && intArrayVal != null) {
                dst.asIntBuffer().put(intArrayVal, arrayOffset, size / 4);
                dst.position(dst.position() + size);
            } else if ((size % 8) == 0 && longArrayVal != null) {
                dst.asLongBuffer().put(longArrayVal, arrayOffset, size / 8);
                dst.position(dst.position() + size);
            } else if (size == 1) {
                dst.put(byteVal);
            } else if (size == 2) {
                dst.putShort(shortVal);
            } else if (size == 4) {
                dst.putInt(intVal);
            } else if (size == 8) {
                dst.putLong(longVal);
            } else {
                throw new IllegalArgumentException("Invalid combination of user buffer and size");
            }
        }
    }

    /**
     * Perform DMA transfer. This method only works when the transfer is contained
     * within one page. It should only be used when this is guaranteed.
     *
     * @param physAddress Physical address targeted by DMA.
     * @param valueBuffer Buffer containing user data targeted by DMA.
     * @param size Size of the memory transfer.
     * @param isRead When true, the data is read from <code>physAddress</code> to <code>valueBuffer</code>.
     *   When false, the data is written from <code>valueBuffer</code> to <code>physAddress</code>.
     * @return True if the transfer is successful. False if after the {@link #DMA_SYNC_TIMEOUT_MS}
     *   the DMA transfer did not occur.
     */
    private synchronized boolean dmaTransfer(long physAddress, DmaValueBuffer valueBuffer, int size, boolean isRead) {
        boolean success;

        final long physAddressPageStart = physAddress & -GpuPMap.DIRECT_MEMORY_PAGE_SIZE;
        final long pageOffset = physAddress - physAddressPageStart;

        if (size <= 0 || (pageOffset + size) > GpuPMap.DIRECT_MEMORY_PAGE_SIZE) {
            throw new IndexOutOfBoundsException(Integer.toString(size));
        }

        // Determine the direction in which to copy memory
        final Pointer dst;
        final Pointer src;
        if (isRead) {
            dst = transferBuffer[0];
            src = Pointer.valueOf(victimBuffer[0].addr() + pageOffset);
        } else {
            dst = Pointer.valueOf(victimBuffer[0].addr() + pageOffset);
            src = transferBuffer[0];
        }

        // Copy the value to be written into the transfer buffer
        if (!isRead) {
            valueBuffer.writeVal(src, size);
        }

        try {
            // Remap PTE of the victim buffer to the target physical address
            mman.memoryProtect(victimBuffer[0], GpuPMap.DIRECT_MEMORY_PAGE_SIZE, protRO);
            this.victimBufferPteAddr.write8(this.victimBufferPteMaskRO | physAddressPageStart);
            mman.memoryProtect(victimBuffer[0], GpuPMap.DIRECT_MEMORY_PAGE_SIZE, protRW);

            try {
                // Do DMA
                libGnmAwt.internalLock();
                try {
                    int oldSyncValue = libGnmAwt.getSyncValue();
                    libGnmAwt.internalDirectMemoryCopy(dst, src, size);
                    success = libGnmAwt.waitSync(oldSyncValue + 1, DMA_SYNC_TIMEOUT_MS);
                } finally {
                    libGnmAwt.internalUnlock();
                }
            } finally {
                // Restore original PTE
                this.victimBufferPteAddr.write8(this.victimBufferPteInitialRW);
                mman.memoryProtect(victimBuffer[0], GpuPMap.DIRECT_MEMORY_PAGE_SIZE, protRW);
            }
        } catch (InvalidValueException | OperationNotPermittedException e) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(getClass(), "remapError"));
        }

        // For read, copy from transfer buffer to user buffer
        if (success && isRead) {
            valueBuffer.readVal(dst, size);
        }

        return success;
    }

    /**
     * Since DMA transfer uses buffers that are page size long,
     * it's important to respect page boundaries during transfers.
     *
     * This method calculates proper page boundaries and invokes
     * chunked transfers of data, recomposing the value at the end.
     *
     * @param physAddress Physical address targeted by DMA.
     * @param valueBuffer Buffer containing user data targeted by DMA.
     * @param size Size of the memory transfer.
     * @param isRead When true, the data is read from <code>physAddress</code> to <code>valueBuffer</code>.
     *   When false, the data is written from <code>valueBuffer</code> to <code>physAddress</code>.
     * @return True if the transfer is successful. False if after the {@link #DMA_SYNC_TIMEOUT_MS}
     *   the DMA transfer did not occur.
     */
    private synchronized boolean dmaTransferPaged(long physAddress, DmaValueBuffer valueBuffer, int size, boolean isRead) {
        boolean success = true;

        long pageStart = physAddress & -GpuPMap.DIRECT_MEMORY_PAGE_SIZE;
        long firstPageOffset = (int) (physAddress - pageStart);
        long transferPhysEnd = physAddress + size;

        long curPhysPos = pageStart + firstPageOffset;
        int transferSize = (int) Math.min(GpuPMap.DIRECT_MEMORY_PAGE_SIZE - firstPageOffset, transferPhysEnd - curPhysPos);

        // Shortcut to non-paged transfer if all fits in one page
        if (transferSize == size) {
            return dmaTransfer(physAddress, valueBuffer, size, isRead);
        }

        // Allocate temp buffer that will be used for paged transfer.
        // Note, this allocates the totality of transfer size, which may not be optimal memory usage.
        ByteBuffer totalBuffer = ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
        totalBuffer.mark();

        // Move the value to be written from input buffer into the temp buffer
        if (!isRead) {
            valueBuffer.writeValToByteBuffer(totalBuffer, size);
            totalBuffer.reset();
        }

        // Page buffer is used for DMA transfer of a page.
        // It points to the array view of the temp buffer.
        DmaValueBuffer pageBuffer = new DmaValueBuffer();
        pageBuffer.byteArrayVal = totalBuffer.array();

        while (transferSize > 0) {
            // Reposition array view at the start of each page.
            pageBuffer.arrayOffset = totalBuffer.arrayOffset() + totalBuffer.position();

            // Do a transfer of the page.
            success = dmaTransfer(curPhysPos, pageBuffer, transferSize, isRead);
            if (!success) {
                break;
            }

            // Move to next page.
            totalBuffer.position(totalBuffer.position() + transferSize);
            curPhysPos += transferSize;
            transferSize = (int) Math.min(GpuPMap.DIRECT_MEMORY_PAGE_SIZE, transferPhysEnd - curPhysPos);
        }

        // Move the value that was read with DMA from the temp buffer to the final value buffer.
        if (success && isRead) {
            totalBuffer.reset();
            valueBuffer.readValFromByteBuffer(totalBuffer, size);
        }

        return success;
    }
}
