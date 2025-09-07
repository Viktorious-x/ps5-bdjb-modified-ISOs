package org.ps5jb.client.utils.init;

import org.ps5jb.client.PayloadConstants;
import org.ps5jb.client.utils.process.ProcessUtils;
import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;
import org.ps5jb.sdk.core.kernel.KernelAccessorAgc;
import org.ps5jb.sdk.core.kernel.KernelOffsets;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.PMap;
import org.ps5jb.sdk.include.sys.internal.gc.vm.GpuVm;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Utility class which JARs can call to initialize
 * all firmware-dependent information in the SDK.
 */
public class SdkInit {
    /** Base address of kernel text segment. May be 0 if this value could not be determined. */
    public final long kernelBaseAddress;
    /** Base address of kernel data segment. May be 0 if this value could not be determined. */
    public final long kernelDataAddress;
    /** Database of important kernel offsets for the current firmware version. May be null. */
    public final KernelOffsets kernelOffsets;
    /** Current process address. May be 0 if this value could not be determined. */
    public final long curProcAddress;

    private static SdkInit instance;

    /**
     * Retrieve the SdkInit singleton instance. This method does not
     * construct the instance. To do so, do a one-time call of
     * {@link #init(boolean, boolean)} method.
     *
     * @return Singleton instance of <code>SdkInit</code> class.
     */
    public static SdkInit instance() {
        if (instance == null) {
            throw new IllegalStateException("Call init() before getting the instance of this class");
        }
        return instance;
    }

    /**
     * Initializes the SdkInit singleton instance. This method should be called once,
     * normally at the beginning of the payload execution.
     *
     * @param requireKernelReadWrite Whether to raise an exception if kernel r/w is not available.
     * @param requireKnownKernelBase Whether to raise an exception if kernel base address is not known.
     * @return Singleton instance of <code>SdkInit</code> class. The same instance can later be retrieved
     *   with {@link #instance()} method.
     * @throws IllegalStateException If instance was already initialized or if one of the
     *   <code>require*</code> parameters are true, but the desired state is not active.
     * @throws SdkSoftwareVersionUnsupportedException If current firmware version is not supported.
     */
    public synchronized static SdkInit init(boolean requireKernelReadWrite, boolean requireKnownKernelBase) {
        if (instance != null) {
            throw new IllegalStateException("Initialization of this class can only occur once");
        }

        instance = new SdkInit(requireKernelReadWrite, requireKnownKernelBase);
        return instance;
    }

    /**
     * Default constructor of SdkInit flag. Performs all the
     * firmware-dependent initialization.
     *
     * @param requireKernelReadWrite Whether to raise an exception if kernel r/w is not available.
     * @param requireKnownKernelBase Whether to raise an exception if kernel base address is not known.
     * @throws KernelReadWriteUnavailableException If requireKernelReadWrite is true,
     *   but kernel r/w is not available.
     * @throws KernelBaseUnknownException If requireKnownKernelBase is true,
     *   but kernel base address is unknown.
     * @throws SdkSoftwareVersionUnsupportedException If current firmware version is not supported.
     */
    private SdkInit(boolean requireKernelReadWrite, boolean requireKnownKernelBase) {
        final LibKernel libKernel = new LibKernel();
        try {
            final int swVer = libKernel.getSystemSoftwareVersion();
            kernelOffsets = new KernelOffsets(swVer);

            final KernelAccessor ka = KernelReadWrite.getAccessor(getClass().getClassLoader());
            if (ka != null) {
                final long kaKernelBase = ka.getKernelBase();
                if (kaKernelBase != 0) {
                    kernelBaseAddress = kaKernelBase;
                    kernelDataAddress = kaKernelBase + kernelOffsets.OFFSET_KERNEL_DATA;
                } else {
                    final String allProcAddrStr = System.getProperty(PayloadConstants.ALLPROC_ADDRESS_PROPERTY);
                    final long sysPropAllProcAddr = (allProcAddrStr != null) ? Long.parseLong(allProcAddrStr) : 0;

                    if (sysPropAllProcAddr != 0) {
                        kernelDataAddress = sysPropAllProcAddr - kernelOffsets.OFFSET_KERNEL_DATA_BASE_ALLPROC;
                        kernelBaseAddress = kernelDataAddress - kernelOffsets.OFFSET_KERNEL_DATA;
                    } else {
                        kernelBaseAddress = 0;
                        kernelDataAddress = 0;
                    }
                }

                KernelPointer kBase = KernelPointer.valueOf(kernelBaseAddress);
                ProcessUtils procUtils = new ProcessUtils(libKernel, kBase, kernelOffsets);
                curProcAddress = procUtils.getCurrentProcess().getPointer().addr();

                if (kernelBaseAddress != 0) {
                    initSdkStructures();
                }
            } else {
                if (requireKernelReadWrite) {
                    throw new KernelReadWriteUnavailableException("Kernel R/W is not available");
                }

                kernelBaseAddress = 0;
                kernelDataAddress = 0;
                curProcAddress = 0;
            }
        } finally {
            libKernel.closeLibrary();
        }

        if (requireKnownKernelBase && kernelBaseAddress == 0) {
            throw new KernelBaseUnknownException("Kernel base address is not known");
        }
    }

    /**
     * Switches active kernel r/w implementation to
     * {@link org.ps5jb.sdk.core.kernel.KernelAccessorAgc}.
     * This accessor is able to bypass write restrictions on
     * certain system software versions.
     *
     * @param onlyIfNecessary If true, the switch will occur only if
     *   the current software version requires AGC implementation for writes.
     *   If false, the AGC implementation will be switched to unconditionally.
     * @return True if kernel r/w is AGC-based after the call to this method.
     */
    public boolean switchToAgcKernelReadWrite(boolean onlyIfNecessary) {
        boolean result = false;
        synchronized (KernelReadWrite.class) {
            KernelAccessor ka = KernelReadWrite.getAccessor(getClass().getClassLoader());
            if (!(ka instanceof KernelAccessorAgc)) {
                // Note: the DMA writes may only be necessary on 7.xx.
                if (!onlyIfNecessary || kernelOffsets.SOFTWARE_VERSION >= 0x0700) {
                    KernelReadWrite.setAccessor(new KernelAccessorAgc(KernelPointer.valueOf(curProcAddress)));
                    result = true;
                }
            } else {
                result = true;
            }
        }
        return result;
    }

    /**
     * If the currently active implementation of kernel r/w is AGC,
     * revert to non-AGC implementation.
     */
    public void restoreNonAgcKernelReadWrite() {
        synchronized (KernelReadWrite.class) {
            KernelAccessor ka = KernelReadWrite.getAccessor(getClass().getClassLoader());
            if (ka instanceof KernelAccessorAgc) {
                KernelAccessorAgc kaAgc = (KernelAccessorAgc) ka;
                KernelReadWrite.setAccessor(kaAgc.getInitialKernelAccessor());
                kaAgc.free();
            }
        }
    }

    /**
     * Some SDK structures are firmware dependent, initialize them right away.
     */
    private void initSdkStructures() {
        KernelPointer kernelBase = KernelPointer.valueOf(kernelBaseAddress);
        Process curProc = new Process(KernelPointer.valueOf(curProcAddress));

        // CurProc offsets are dynamic
        curProc.getTitleId();

        // PMap and GPU VM id offsets are dynamic
        curProc.getVmSpace().getPhysicalMap();
        curProc.getVmSpace().getGpuVmId();

        // GPU VM struct size is dynamic and offset to VM id in the process is dynamic too
        new GpuVm(kernelBase.inc(kernelOffsets.OFFSET_KERNEL_DATA + kernelOffsets.OFFSET_KERNEL_DATA_BASE_GBASE_VM));

        // PMap params are dynamic
        if (PMap.DMPDPI == 0) {
            KernelPointer kernelPmap = kernelBase.inc(kernelOffsets.OFFSET_KERNEL_DATA + kernelOffsets.OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE);
            PMap.refresh(
                    kernelPmap.inc(kernelOffsets.OFFSET_PMAP_STORE_DMPML4I),
                    kernelPmap.inc(kernelOffsets.OFFSET_PMAP_STORE_DMPDPI),
                    kernelPmap.inc(kernelOffsets.OFFSET_PMAP_STORE_PML4PML4I)
            );
        }
    }
}
