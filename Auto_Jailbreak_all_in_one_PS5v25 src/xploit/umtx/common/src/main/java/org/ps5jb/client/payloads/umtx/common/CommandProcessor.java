package org.ps5jb.client.payloads.umtx.common;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.lib.LibKernel;

public class CommandProcessor {
    private final LibKernel libKernel;

    public static final int CMD_NOP = 0;
    public static final int CMD_READ = 1;
    public static final int CMD_WRITE = 2;
    public static final int CMD_EXIT = 3;

    public AtomicBoolean exitSignal = new AtomicBoolean();
    public AtomicInteger cmd = new AtomicInteger(CMD_NOP);
    public AtomicInteger len = new AtomicInteger();
    public AtomicLong readCounter = new AtomicLong();
    public AtomicLong writeCounter = new AtomicLong();

    public CommandProcessor() {
        libKernel = new LibKernel();
    }

    public void handleCommands() {
        int pipeReadFd = -1;
        Pointer pipeScratchBuf = Pointer.NULL;
        int pipeWriteFd = -1;

        while (!exitSignal.get()) {
            int cmd = this.cmd.get();
            if (cmd == CMD_NOP) {
                Thread.yield();
                continue;
            }
            long len = this.len.get();
            if (Pointer.NULL.equals(pipeScratchBuf)) {
                KernelAccessorSlow ka = (KernelAccessorSlow) KernelReadWrite.getAccessor(getClass().getClassLoader());
                pipeReadFd = ka.pipeReadFd;
                pipeWriteFd = ka.pipeWriteFd;
                pipeScratchBuf = ka.pipeScratchBuf;
            }

            switch (cmd) {
                case CMD_READ:
                    if (DebugStatus.isDebugEnabled()) {
                        DebugStatus.debug("Command processor: blocking to write " + len + " bytes for READ command");
                    }
                    long read = this.libKernel.write(pipeWriteFd, pipeScratchBuf, len);
                    this.readCounter.incrementAndGet();
                    if (DebugStatus.isDebugEnabled()) {
                        DebugStatus.debug("Command processor: written " + read + " bytes");
                    }
                    break;

                case CMD_WRITE:
                    if (DebugStatus.isDebugEnabled()) {
                        DebugStatus.debug("Command processor: blocking to read " + len + " bytes for WRITE command");
                    }
                    long write = this.libKernel.read(pipeReadFd, pipeScratchBuf, len);
                    this.writeCounter.incrementAndGet();
                    if (DebugStatus.isDebugEnabled()) {
                        DebugStatus.debug("Command processor: read " + write + " bytes");
                    }
                    break;

                case CMD_EXIT:
                    // Do nothing
                    DebugStatus.debug("Command processor: exiting");
                    exitSignal.set(true);
                    break;

                default:
                    DebugStatus.error("Command processor: unknown command");
            }

            this.cmd.set(CMD_NOP);

            DebugStatus.debug("Command processor: resetting");
        }

        libKernel.closeLibrary();

        DebugStatus.debug("Command processor: finished and cannot be reused");
    }
}
