package org.ps5jb.client.payloads.umtx.common;

import java.util.concurrent.atomic.AtomicInteger;
import org.ps5jb.sdk.lib.LibKernel;

public class CommandProcessor {
    private final LibKernel libKernel;
    public static final int CMD_NOP = 0;
    public static final int CMD_READ = 1;
    public static final int CMD_WRITE = 2;
    public static final int CMD_EXIT = 3;

    public final AtomicInteger cmd = new AtomicInteger(CMD_NOP);

    public CommandProcessor() {
        this.libKernel = new LibKernel();
    }

    public synchronized void awaitDone() throws InterruptedException {
        while (cmd.get() != CMD_NOP) {
            wait();
        }
    }

    public synchronized void signalDone() {
        cmd.set(CMD_NOP);
        notifyAll();
    }

    public void handleCommands() {
        while (true) {
            int current = cmd.get();
            if (current == CMD_NOP) {
                Thread.yield();
                continue;
            }
            switch (current) {
                case CMD_READ:
                    break;
                case CMD_WRITE:
                    break;
                case CMD_EXIT:
                    libKernel.closeLibrary();
                    return;
                default:
                    throw new IllegalStateException("Unknown command: " + current);
            }
            synchronized (this) {
                cmd.set(CMD_NOP);
                notifyAll();
            }
        }
    }
}