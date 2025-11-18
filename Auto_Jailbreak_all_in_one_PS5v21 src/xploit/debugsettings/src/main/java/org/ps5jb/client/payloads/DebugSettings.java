package org.ps5jb.client.payloads;

import java.util.StringTokenizer;

import org.ps5jb.client.PayloadConstants;
import org.ps5jb.client.utils.init.KernelBaseUnknownException;
import org.ps5jb.client.utils.init.KernelReadWriteUnavailableException;
import org.ps5jb.client.utils.init.SdkInit;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;
import org.ps5jb.sdk.core.kernel.KernelOffsets;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Activate/deactivate debug settings. Each run flips the settings.
 */
public class DebugSettings implements Runnable {
    private KernelPointer qaFlags;
    private KernelPointer secFlags;
    private KernelPointer utokenFlags;
    private KernelPointer targetId;

    @Override
    public void run() {
        final LibKernel libKernel = new LibKernel();
        try {
            SdkInit sdk = SdkInit.init(true, true);
            KernelPointer kbase = KernelPointer.valueOf(sdk.kernelBaseAddress, false);
            KernelOffsets o = sdk.kernelOffsets;

            qaFlags = kbase.inc(o.OFFSET_KERNEL_DATA + o.OFFSET_KERNEL_DATA_BASE_QA_FLAGS);
            secFlags = kbase.inc(o.OFFSET_KERNEL_DATA + o.OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS);
            utokenFlags = kbase.inc(o.OFFSET_KERNEL_DATA + o.OFFSET_KERNEL_DATA_BASE_UTOKEN_FLAGS);
            targetId = kbase.inc(o.OFFSET_KERNEL_DATA + o.OFFSET_KERNEL_DATA_BASE_TARGET_ID);

            Status.println("Original kernel values:");
            printFlags();

            // Switch to DMA if necessary
            boolean useDma = false;
            if (sdk.switchToAgcKernelReadWrite(true)) {
                useDma = true;
                Status.println("Switched to AGC-based kernel r/w");
            }
            try {
                String origValStr = System.getProperty(PayloadConstants.ORIG_DEBUG_SETTINGS_VAL_PROPERTY);
                if (origValStr != null) {
                    int[] origVals = new int[4];
                    StringTokenizer t = new StringTokenizer(origValStr, ",");
                    for (int i = 0; t.hasMoreElements() && i < origVals.length; ++i) {
                        String nextVal = t.nextToken();
                        origVals[i] = Integer.parseInt(nextVal);
                    }

                    qaFlags.write4(origVals[0]);
                    secFlags.write4(origVals[1]);
                    targetId.write1((byte) (origVals[2] & 0xFF));
                    utokenFlags.write1((byte) (origVals[3] & 0xFF));

                    System.getProperties().remove(PayloadConstants.ORIG_DEBUG_SETTINGS_VAL_PROPERTY);
                    Status.println("Restored original debug settings values" + (useDma ? " using DMA" : ""));
                } else {
                    int qaFlagsVal = qaFlags.read4();
                    qaFlags.write4(qaFlagsVal | 0x10300);

                    int secFlagsVal = secFlags.read4();
                    secFlags.write4(secFlagsVal | 0x14);

                    byte targetIdVal = targetId.read1();
                    targetId.write1((byte) 0x82);

                    byte utokenVal = utokenFlags.read1();
                    utokenFlags.write1((byte) ((utokenVal | 0x01) & 0xFF));

                    String saveVal = qaFlagsVal + "," + secFlagsVal + "," + targetIdVal + "," + utokenVal;
                    System.setProperty(PayloadConstants.ORIG_DEBUG_SETTINGS_VAL_PROPERTY, saveVal);

                    Status.println("Activated debug settings" + (useDma ? " using DMA" : ""));
                }
            } finally {
                sdk.restoreNonAgcKernelReadWrite();
            }

            Status.println("New kernel values:");
            printFlags();
        } catch (KernelReadWriteUnavailableException e) {
            Status.println("Kernel R/W is not available, aborting");
        } catch (KernelBaseUnknownException e) {
            Status.println("KASLR not defeated, aborting");
        } catch (SdkSoftwareVersionUnsupportedException e) {
            Status.println("Unsupported firmware version: " + e.getMessage());
        } catch (Throwable e) {
            Status.printStackTrace("Unexpected error", e);
        } finally {
            libKernel.closeLibrary();
        }
    }

    private void printFlags() {
        Status.println("  QA Flags: 0x" + Integer.toHexString(qaFlags.read4()));
        Status.println("  Security Flags: 0x" + Integer.toHexString(secFlags.read4()));
        Status.println("  Utoken Flags: 0x" + Integer.toHexString(utokenFlags.read1() & 0xFF));
        Status.println("  Target ID: 0x" + Integer.toHexString(targetId.read1() & 0xFF));
    }
}
