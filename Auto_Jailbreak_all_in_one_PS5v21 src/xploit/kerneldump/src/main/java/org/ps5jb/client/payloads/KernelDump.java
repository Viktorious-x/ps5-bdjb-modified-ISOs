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
import org.ps5jb.client.PayloadConstants;
import org.ps5jb.client.utils.init.SdkInit;
import org.ps5jb.loader.ManifestUtils;
import org.ps5jb.loader.SocketListener;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.AbstractPointer;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;
import org.ps5jb.sdk.core.kernel.KernelOffsets;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.Param;
import org.ps5jb.sdk.include.machine.VmParam;
import org.ps5jb.sdk.include.sys.errno.MemoryFaultException;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * <p>
 *   Sends the contents of the kernel over network connection.
 *   For firmware with unknown offsets, it's not possible to know when to stop reading.
 *   Therefore, the dump will happen until console crashes.
 * </p>
 * <p>
 *   To use, execute a payload that obtains kernel r/w.
 *   Then send this class to the PS5 for execution in the JAR Loader.
 *   On a computer, use `nc` or another tool to connect to the PS5:<br>
 *   <code>nc [PS5 IP] 5656 &gt; data.bin</code>
 * </p>
 * <p>
 * Upon connection, the kernel will be dumped and sent back to `nc`.
 * Depending on OS, `nc` may not terminate by itself. When PS5 crashes,
 * simply terminate it by force.
 * </p>
 * <p>
 * Kernel text segment is unreadable by default. It can be made readable
 * by using Byepervisor payload prior to executing the dump. If it is detected
 * that text segment is readable, it will be included in the dump. Otherwise,
 * only data segment will be sent.
 * </p>
 */
public class KernelDump extends SocketListener implements UserEventListener {
    private LibKernel libKernel;
    private KernelPointer kbaseAddress = KernelPointer.NULL;
    private KernelPointer kdataAddress = KernelPointer.NULL;
    private KernelOffsets offsets;

    public KernelDump() throws IOException {
        // Use same port as webkit implementation from Specter
        super("Kernel Data Dumper v" + ManifestUtils.getClassImplementationVersion(KernelDump.class, "kerneldump"), 5656);
    }

    @Override
    public void run() {
        libKernel = new LibKernel();
        try {
            SdkInit sdk;
            try {
                sdk = SdkInit.init(true, false);

                offsets = sdk.kernelOffsets;
                kdataAddress = new KernelPointer(sdk.kernelDataAddress, new Long(sdk.kernelOffsets.SIZE_KERNEL_DATA));
                kbaseAddress = new KernelPointer(sdk.kernelBaseAddress, new Long(sdk.kernelOffsets.OFFSET_KERNEL_DATA + sdk.kernelOffsets.SIZE_KERNEL_DATA));

                // If switched to AGC, need to switch back because AGC is not able to read text segment
                sdk.restoreNonAgcKernelReadWrite();
            } catch (SdkSoftwareVersionUnsupportedException e) {
                // Ignore, scan will be performed to determine data base
            }

            if (KernelPointer.NULL.equals(kdataAddress)) {
                kdataAddress = getKnownKDataAddress();
                if (KernelPointer.NULL.equals(kdataAddress)) {
                    KernelPointer kdataPtr = getKdataPtr();
                    if (KernelPointer.NULL.equals(kdataPtr)) {
                        Status.println("No kernel addresses have been exposed. Aborting.");
                        return;
                    }

                    kdataAddress = scanKdataStartFromPtr(kdataPtr);
                    if (!KernelPointer.NULL.equals(kdataAddress)) {
                        Status.println("Known pointer offset from data start: " + KernelPointer.valueOf(kdataPtr.addr() - kdataAddress.addr()));
                    }
                }
            }

            if (KernelPointer.NULL.equals(kdataAddress)) {
                Status.println("Kernel data address could not be determined. Aborting.");
                return;
            }

            Status.println("Kernel data address: " + kdataAddress);
            if (!KernelPointer.NULL.equals(kbaseAddress)) {
                Status.println("Kernel text address: " + kbaseAddress);
            }

            // Listen for controller input
            EventManager.getInstance().addUserEventListener(this, new OverallRepository());
            Status.println("Press Red square to abort");

            // Listen for connection
            super.run();
        } finally {
            free();

            EventManager.getInstance().removeUserEventListener(this);
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

    @Override
    protected void acceptClient(Socket clientSocket) throws Exception {
        // Check if kernel text is also readable
        boolean isKtextReadable = offsets != null && kdataAddress.read4(offsets.OFFSET_KERNEL_DATA_BASE_DATA_CAVE) == 0x00001337;

        OutputStream out = clientSocket.getOutputStream();
        try {
            byte[] buffer = new byte[(int) Param.PHYS_PAGE_SIZE];
            long pageRatio = Param.PAGE_SIZE / buffer.length;
            long printInterval = pageRatio * 0x100;

            KernelPointer kernelSpace;
            String clientAddress = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
            if (isKtextReadable) {
                kernelSpace = new KernelPointer(kbaseAddress.addr(), new Long(offsets.OFFSET_KERNEL_DATA + offsets.SIZE_KERNEL_DATA));
                Status.println("Dumping kernel text and data to " + clientAddress + ". Start: " + kernelSpace + "; size: 0x" + Long.toHexString(kernelSpace.size().longValue()));
            } else if (kdataAddress.size() != null) {
                kernelSpace = kdataAddress;
                Status.println("Dumping kernel data to " + clientAddress + ". Start: " + kernelSpace + "; size: 0x" + Long.toHexString(kernelSpace.size().longValue()));
            } else {
                // 0x3000 pages is an arbitrary limit which will likely crash the console
                kernelSpace = new KernelPointer(kdataAddress.addr(), new Long(0x3000 * Param.PAGE_SIZE));
                Status.println("Dumping kernel data until crash to " + clientAddress + ". Start: " + kernelSpace);
            }

            long pageCount = 0;
            while ((pageCount * buffer.length) < kernelSpace.size().longValue()) {
                Arrays.fill(buffer, (byte) 0);

                int readSize = (int) Math.min(buffer.length, kernelSpace.size().longValue() - pageCount * buffer.length);
                try {
                    kernelSpace.read(pageCount * buffer.length, buffer, 0, readSize);
                } catch (SdkRuntimeException e) {
                    if (e.getCause() instanceof MemoryFaultException) {
                        if (pageCount == 0) {
                            Status.println("Kernel is not readable. Aborting.");
                            break;
                        }
                        Status.println("Address " + AbstractPointer.toString(kernelSpace.addr() + pageCount * buffer.length) + " not accessible. Skipped.");
                    } else {
                        throw e;
                    }
                }
                out.write(buffer, 0, readSize);

                ++pageCount;

                if (pageCount % printInterval == 0) {
                    Status.println("Dumped 0x" + Long.toHexString(pageCount / pageRatio) + " pages");
                }
            }
        } finally {
            out.close();
        }

        terminate();
    }

    private KernelPointer getKnownKDataAddress() {
        KernelPointer kdataAddress = KernelPointer.NULL;
        if (!KernelPointer.NULL.equals(kbaseAddress) && offsets != null) {
            kdataAddress = new KernelPointer(kbaseAddress.addr() + offsets.OFFSET_KERNEL_DATA, new Long(offsets.SIZE_KERNEL_DATA));
        }

        return kdataAddress;
    }

    /**
     * Get the address of the kernel variable as saved in system properties
     * by a different payload.
     *
     * @return Pointer to a value in kernel data section.
     */
    private KernelPointer getKdataPtr() {
        KernelPointer result = KernelPointer.NULL;
        String resultStr = System.getProperty(PayloadConstants.ALLPROC_ADDRESS_PROPERTY);
        if (resultStr != null) {
            try {
                result = KernelPointer.valueOf(Long.parseLong(resultStr));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        return result;
    }

    /**
     * Scan kernel data pages starting from a known address in an effort to determine data
     * segment start. If the expected pattern is not found, the scan may cause a page fault
     * and crash the console.
     *
     * @param kdataPtr Address of a known value in kernel data section.
     * @return Address of the kernel data start.
     */
    private KernelPointer scanKdataStartFromPtr(KernelPointer kdataPtr) {
        // Page-align data pointer
        KernelPointer kdataPage = KernelPointer.valueOf(Param.ptoa(Param.atop(kdataPtr.addr())));
        Status.println("Kernel data address is not known for the current firmware. Scanning starting at " + kdataPage + ". This may crash if unsuccessful...");

        KernelPointer kdataAddress = KernelPointer.NULL;

        // Scan for a known pattern at the beginning of the data section
        while (!KernelPointer.NULL.equals(kdataPage)) {
            long val1 = kdataPage.read8();
            if (val1 == 0x0000000100000001L) {
                long val2 = kdataPage.read8(0x8);
                if (val2 == 0x0000000000000000L) {
                    boolean check = true;
                    for (long i = 0; i < 4; ++i) {
                        long valCheck = kdataPage.read8(0x10 + i * 8);
                        if (((valCheck & VmParam.VM_MIN_KERNEL_ADDRESS) != VmParam.VM_MIN_KERNEL_ADDRESS) && valCheck != 8) {
                            check = false;
                            break;
                        }
                    }

                    if (check) {
                        kdataAddress = kdataPage;
                        break;
                    }
                }
            }

            kdataPage = kdataPage.inc(-Param.PAGE_SIZE);
        }

        return kdataAddress;
    }

    @Override
    public void userEventReceived(UserEvent userEvent) {
        if (userEvent.getType() == HRcEvent.KEY_RELEASED) {
            switch (userEvent.getCode()) {
                case HRcEvent.VK_COLORED_KEY_0:
                    try {
                        terminate();
                    } catch (IOException e) {
                        Status.printStackTrace("Failed to terminate the listener due to I/O exception", e);
                    }
                    break;
            }
        }
    }
}
