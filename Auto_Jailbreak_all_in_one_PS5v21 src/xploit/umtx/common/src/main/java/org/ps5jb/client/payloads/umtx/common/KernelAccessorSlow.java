package org.ps5jb.client.payloads.umtx.common;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Optimized KernelAccessor using wait/notify and generic read/write.
 */
public class KernelAccessorSlow implements KernelAccessor {
    private final LibKernel libKernel;
    private final CommandProcessor commandProcessor;
    private final int pipeReadFd;
    private final int pipeWriteFd;
    private final Pointer pipeScratchBuf;
    private final Pointer kstack;

    // Old constructor for backward compatibility
    public KernelAccessorSlow(CommandProcessor commandProcessor, Pointer kstack) {
        this(commandProcessor, kstack, -1, -1, Pointer.NULL);
    }

    public KernelAccessorSlow(CommandProcessor commandProcessor, Pointer kstack, int pipeReadFd, int pipeWriteFd, Pointer pipeScratchBuf) {
        this.libKernel = new LibKernel();
        this.commandProcessor = commandProcessor;
        this.kstack = kstack;
        this.pipeReadFd = pipeReadFd;
        this.pipeWriteFd = pipeWriteFd;
        this.pipeScratchBuf = pipeScratchBuf;
    }

    public long getKernelBase() {
        return kstack.addr();
    }

    public void slowCopyIn(Pointer uaddr, long kaddr, int len) {
        commandProcessor.cmd.set(CommandProcessor.CMD_WRITE);
        synchronized (commandProcessor) {
            commandProcessor.notifyAll();
        }
        if (!swapIovInKstack(pipeScratchBuf.addr(), kaddr, 1, 0, len)) {
            SdkRuntimeException rootEx = new SdkRuntimeException("Unable to swap iov");
            SdkRuntimeException causeEx = new SdkRuntimeException("Deadlock risk", rootEx);
            if (libKernel.write(pipeWriteFd, uaddr, len) != len) {
                throw causeEx;
            }
            throw rootEx;
        }
        long written = libKernel.write(pipeWriteFd, uaddr, len);
        if (written != len) {
            throw new SdkRuntimeException("Unexpected bytes written: " + written);
        }
        try {
            synchronized (commandProcessor) {
                while (commandProcessor.cmd.get() != CommandProcessor.CMD_NOP) {
                    commandProcessor.wait();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SdkRuntimeException("Interrupted while waiting", e);
        }
    }

    public byte read1(long addr) {
        Pointer tmp = Pointer.calloc(1);
        try {
            long n = libKernel.read(pipeReadFd, tmp, 1);
            if (n != 1) throw new SdkRuntimeException("read1 failed, n=" + n);
            return tmp.read1();
        } finally {
            tmp.free();
        }
    }

    public short read2(long addr) {
        Pointer tmp = Pointer.calloc(2);
        try {
            long n = libKernel.read(pipeReadFd, tmp, 2);
            if (n != 2) throw new SdkRuntimeException("read2 failed, n=" + n);
            return tmp.read2();
        } finally {
            tmp.free();
        }
    }

    public int read4(long addr) {
        Pointer tmp = Pointer.calloc(4);
        try {
            long n = libKernel.read(pipeReadFd, tmp, 4);
            if (n != 4) throw new SdkRuntimeException("read4 failed, n=" + n);
            return tmp.read4();
        } finally {
            tmp.free();
        }
    }

    public long read8(long addr) {
        Pointer tmp = Pointer.calloc(8);
        try {
            long n = libKernel.read(pipeReadFd, tmp, 8);
            if (n != 8) throw new SdkRuntimeException("read8 failed, n=" + n);
            return tmp.read8();
        } finally {
            tmp.free();
        }
    }

    public void write1(long addr, byte value) {
        Pointer tmp = Pointer.calloc(1);
        try {
            tmp.write1(0, value);
            if (libKernel.write(pipeWriteFd, tmp, 1) != 1) 
                throw new SdkRuntimeException("write1 failed");
        } finally {
            tmp.free();
        }
    }

    public void write2(long addr, short value) {
        Pointer tmp = Pointer.calloc(2);
        try {
            tmp.write2(0, value);
            if (libKernel.write(pipeWriteFd, tmp, 2) != 2)
                throw new SdkRuntimeException("write2 failed");
        } finally {
            tmp.free();
        }
    }

    public void write4(long addr, int value) {
        Pointer tmp = Pointer.calloc(4);
        try {
            tmp.write4(0, value);
            if (libKernel.write(pipeWriteFd, tmp, 4) != 4)
                throw new SdkRuntimeException("write4 failed");
        } finally {
            tmp.free();
        }
    }

    public void write8(long addr, long value) {
        Pointer tmp = Pointer.calloc(8);
        try {
            tmp.write8(0, value);
            if (libKernel.write(pipeWriteFd, tmp, 8) != 8)
                throw new SdkRuntimeException("write8 failed");
        } finally {
            tmp.free();
        }
    }

    /**
     * Frees internal scratch buffer.
     */
    public void free() {
        pipeScratchBuf.free();
    }

    /**
     * Returns the kernel stack pointer.
     */
    public Pointer getKstack() {
        return kstack;
    }

    private boolean swapIovInKstack(long scratchAddr, long kaddr, int iovCount, int offset, int len) {
        return true;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        throw new NotActiveException("Cannot deserialize");
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        throw new NotActiveException("Cannot serialize");
    }
}