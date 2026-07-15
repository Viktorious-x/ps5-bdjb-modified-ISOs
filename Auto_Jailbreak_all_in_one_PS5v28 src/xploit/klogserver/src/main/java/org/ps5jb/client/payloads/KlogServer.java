package org.ps5jb.client.payloads;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import org.dvb.event.EventManager;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.havi.ui.event.HRcEvent;
import org.ps5jb.client.utils.init.SdkInit;
import org.ps5jb.client.utils.process.ProcessUtils;
import org.ps5jb.loader.ManifestUtils;
import org.ps5jb.loader.SocketListener;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.CpuSet;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.include.sys.FCntl;
import org.ps5jb.sdk.include.sys.Select;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.include.sys.select.FdSetType;
import org.ps5jb.sdk.include.sys.timeval.TimevalType;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * <p>
 *   Sends the contents of the klog file over network connection.
 * </p>
 * <p>
 *   To use, execute a payload that obtains kernel r/w and a payload that jailbreaks the process.
 *   Then send this class to the PS5 for execution in the JAR Loader.
 *   On a computer, use `nc` or another tool to connect to the PS5:<br>
 *   <code>nc [PS5 IP] 3232 &gt; data.bin</code>
 * </p>
 */
public class KlogServer extends SocketListener implements UserEventListener {
    private LibKernel libKernel;
    private Select selectWrapper;

    private int exitConfirmCount;

    private int[] oldUserIds;
    private long[] oldPrivs;

    private int origQaFlags = -1;
    private int origSecurityFlags = -1;

    private KernelPointer qaFlags;
    private KernelPointer securityFlags;

    public KlogServer() throws IOException {
        // Use same port as webkit implementation from sb
        super("Klog Server v" + ManifestUtils.getClassImplementationVersion(KlogServer.class, "klogserver"), 3232);

        // Subscribe to all events
        EventManager eventManager = EventManager.getInstance();
        if (eventManager != null) {
            eventManager.addUserEventListener(this, new OverallRepository());
        }
    }

    @Override
    public void run() {
        libKernel = new LibKernel();
        selectWrapper = new Select(libKernel);
        try {
            SdkInit sdk = SdkInit.init(true, true);

            KernelPointer kdata = KernelPointer.valueOf(sdk.kernelDataAddress, false);
            securityFlags = kdata.inc(sdk.kernelOffsets.OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS);
            qaFlags = kdata.inc(sdk.kernelOffsets.OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS);

            // When a connection is established, for some reason the rest of the system starts to lag.
            // Sending parallel JARs locks randomly for multiple seconds then resumes.
            // Setting the priority to lowest and pinning to a single core aleviates the problem somewhat,
            // but it does not resolve it entirely.
            // Disconnecting the client connection removes the lagging.

            // Set thread to a lower priority
            ProcessUtils procUtils = new ProcessUtils(libKernel, KernelPointer.valueOf(sdk.kernelBaseAddress), sdk.kernelOffsets);
            procUtils.setCurrentThreadPriority(new Integer(Thread.MIN_PRIORITY), new Short((short) 767));
            Process curProc = procUtils.getCurrentProcess();

            // Pin to a single CPU
            CpuSet cpuSet = new CpuSet(libKernel);
            cpuSet.setCurrentThreadCore(1);

            // Set user id to root
            if (libKernel.getuid() != 0) {
                oldUserIds = procUtils.getUserGroup(procUtils.getCurrentProcess());
                procUtils.setUserGroup(curProc, new int[] {
                        0, // cr_uid
                        0, // cr_ruid
                        0, // cr_svuid
                        1, // cr_ngroups
                        0, // cr_rgid
                        0, // cr_svid
                        0, // gid0
                });
                Status.println("Set process uid => " + libKernel.getuid());
            }
            try {
                // Set permissive privs
                oldPrivs = procUtils.getPrivs(curProc);
                final long[] newPrivs = new long[] {
                        oldPrivs[0],         // cr_sceAuthId
                        0xFFFFFFFFFFFFFFFFL, // cr_sceCaps[0]
                        0xFFFFFFFFFFFFFFFFL, // cr_sceCaps[1]
                        0x80                 // cr_sceAttr[0]
                };
                if (!Arrays.equals(oldPrivs, newPrivs)) {
                    procUtils.setPrivs(curProc, newPrivs);
                    Status.println("Relaxed process privileges");
                }

                sdk.switchToAgcKernelReadWrite(true);
                try {
                    origSecurityFlags = securityFlags.read4();
                    int newSecurityFlags = origSecurityFlags | 0x14;
                    if (origSecurityFlags != newSecurityFlags) {
                        Status.println("Security flags: 0x" + Integer.toHexString(origSecurityFlags) + " => 0x" + Integer.toHexString(newSecurityFlags));
                        securityFlags.write4(newSecurityFlags);
                    } else {
                        origSecurityFlags = -1;
                    }

                    origQaFlags = qaFlags.read4();
                    int newQaFlags = origQaFlags | 0x10300;
                    if (origQaFlags != newQaFlags) {
                        Status.println("QA flags: 0x" + Integer.toHexString(origQaFlags) + " => 0x" + Integer.toHexString(newQaFlags));
                        qaFlags.write4(newQaFlags);
                    } else {
                        origQaFlags = -1;
                    }
                } finally {
                    sdk.restoreNonAgcKernelReadWrite();
                }

                Status.println("Use netcat to receive klog over network: nc -q 1 " + getNetAddress() + " " + serverSocket.getLocalPort());
                Status.println("Disclamer: there is a known issue where the BD-J process becomes sluggish when the network client is connect.");
                Status.println("Disconnecting the client (terminating netcat) restores the performance.");
                Status.println("To exit " + this.listenerName + ", press Blue Square button 3 times in a row.");

                // Listen for connection
                super.run();

                // Unsubscribe from events
                EventManager eventManager = EventManager.getInstance();
                if (eventManager != null) {
                    eventManager.removeUserEventListener(this);
                }
            } finally {
                if (oldUserIds != null) {
                    procUtils.setUserGroup(curProc, oldUserIds);
                    Status.println("Restored process user to " + libKernel.getuid());
                }

                if (oldPrivs != null) {
                    procUtils.setPrivs(curProc, oldPrivs);
                    Status.println("Restored process privileges");
                }

                if (origQaFlags != -1 || origSecurityFlags != -1) {
                    sdk.switchToAgcKernelReadWrite(true);
                    try {
                        if (origSecurityFlags != -1) {
                            securityFlags.write4(origSecurityFlags);
                            Status.println("Restored security flags");
                        }

                        if (origQaFlags != -1) {
                            qaFlags.write4(origQaFlags);
                            Status.println("Restored QA flags");
                        }
                    } finally {
                        sdk.restoreNonAgcKernelReadWrite();
                    }
                }
            }
        } catch (Throwable e) {
            handleException(e);
        } finally {
            free();
        }
    }

    @Override
    protected void free() {
        super.free();

        if (libKernel != null) {
            libKernel.closeLibrary();
            libKernel = null;
        }
    }

    private void sendKlog(int readFd, FdSetType fdSet, TimevalType timeval, Pointer inBuf, byte[] outBuf, OutputStream out) throws SdkException, IOException {
        // Set fd
        fdSet.zero();
        fdSet.set(readFd);

        // Unblock after a second
        timeval.setSec(1);
        timeval.setUsec(0);

        int selectCount = selectWrapper.select(readFd + 1, fdSet, null, null, timeval);

        if (selectCount > 0 && fdSet.isSet(readFd)) {
            int readCount = (int) libKernel.read(readFd, inBuf, inBuf.size().longValue());
            if (readCount > 0) {
                inBuf.read(0, outBuf, 0, readCount);
                out.write(outBuf, 0, readCount);
            }
        }
    }

    @Override
    protected void acceptClient(Socket clientSocket) throws Exception {
        OutputStream out = clientSocket.getOutputStream();
        try {
            final int bufSize = 0x4000;
            Pointer inBuf = Pointer.calloc(bufSize);
            byte[] outBuf = new byte[bufSize];
            try {
                FCntl fcnctl = new FCntl(libKernel);
                int fd = fcnctl.open("/dev/klog", OpenFlag.O_RDONLY);
                try {
                    FdSetType fdSet = new FdSetType();
                    TimevalType timeval = new TimevalType();
                    try {
                        Thread sendThread = new Thread(() -> {
                            while (!terminated) {
                                try {
                                    sendKlog(fd, fdSet, timeval, inBuf, outBuf, out);
                                } catch (Throwable e) {
                                    handleException(e);
                                }
                            }
                        }, "Klog Reader");

                        sendThread.start();
                        while (sendThread.isAlive()) {
                            Thread.yield();
                        }
                    } finally {
                        timeval.free();
                        fdSet.free();
                    }
                } finally {
                    fcnctl.close(fd);
                }
            } finally {
                inBuf.free();
            }
        } finally {
            out.close();
        }
    }

    @Override
    public void userEventReceived(UserEvent userEvent) {
        if (userEvent.getFamily() == UserEvent.UEF_KEY_EVENT) {
            if (userEvent.getType() == HRcEvent.KEY_RELEASED) {
                switch (userEvent.getCode()) {
                    case HRcEvent.VK_COLORED_KEY_2:
                        if (exitConfirmCount == 2) {
                            try {
                                terminate();
                            } catch (IOException e) {
                                Status.printStackTrace(e.getMessage(), e);
                                exitConfirmCount = 0;
                            }
                        } else {
                            if (exitConfirmCount == 1) {
                                Status.println("Are you sure you want to terminate the " + this.listenerName + "? Press the same key one last time to confirm or any other key to cancel.");
                            }
                            ++exitConfirmCount;
                        }
                        break;
                    default:
                        if (exitConfirmCount > 1) {
                            Status.println("Termination request cancelled.");
                            exitConfirmCount = 0;
                        }
                }
            }
        }
    }
}
