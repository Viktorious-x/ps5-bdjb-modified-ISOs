package org.ps5jb.client.payloads.umtx.impl1;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.cpuset.CpuSetType;
import org.ps5jb.sdk.include.sys.iovec.IoVecType;
import org.ps5jb.sdk.include.sys.timeval.TimevalType;
import org.ps5jb.sdk.include.sys.uio.UioType;

public class State {
    // Pinned cores for each type of created threads.
    public CpuSetType MAIN_THREAD_CORES;
    public CpuSetType[] DESTROYER_THREAD_CORES;
    public CpuSetType LOOKUP_THREAD_CORES;

    public CpuSetType initialMainThreadAffinity;

    //-------------------------------------------------------------------------

    public Pointer scratchBufferAddress = Pointer.NULL;
    public Pointer primarySharedMemoryKeyAddress = Pointer.NULL;
    public Pointer secondarySharedMemoryKeyAddress = Pointer.NULL;
    public Pointer extraSharedMemoryKeyAddress = Pointer.NULL;
    public Pointer statAddress = Pointer.NULL;
    public TimevalType timeoutAddress;
    public Pointer markerPatternAddress = Pointer.NULL;
    public Pointer threadNameAddress = Pointer.NULL;
    public Pointer reclaimJobStatesAddress = Pointer.NULL;

    public List destroyerThreads;
    public Thread lookupThread;
    public List reclaimJobs;
    public List reclaimThreads;
    public ReclaimJob targetReclaimJob;
    public Thread targetReclaimThread;

    public AtomicBoolean raceDoneFlag;
    public AtomicBoolean readyFlag;
    public AtomicBoolean destroyFlag;
    public AtomicBoolean sprayFlag;
    public AtomicBoolean checkDoneFlag;
    public AtomicBoolean doneFlag;

    public AtomicInteger numReadyThreads;
    public AtomicInteger numCompletedThreads;
    public AtomicInteger numFinishedThreads;
    public AtomicInteger numDestructions;
    public AtomicInteger numSprays;

    public int initialOriginalDescriptor;
    public int originalDescriptor;
    public int lookupDescriptor;
    public int winnerDescriptor;
    public int[] reclaimDescriptors;
    public int destroyerThreadIndex;
    public int[] extraDescriptors;

    public Set usedDescriptors;
    public Set mappedKernelStackAddresses;
    public Pointer mappedReclaimKernelStackAddress = Pointer.NULL;

    public MemoryBuffer stackDataBuffer;

    public boolean exploited;

    public KernelPointer threadAddress = KernelPointer.NULL;
    public KernelPointer processAddress = KernelPointer.NULL;
    public KernelPointer ofilesAddress = KernelPointer.NULL;
    public KernelPointer kbaseAddress = KernelPointer.NULL;

    public State() {
        MAIN_THREAD_CORES = new CpuSetType();
        MAIN_THREAD_CORES.set(0);

        DESTROYER_THREAD_CORES = new CpuSetType[] { new CpuSetType(), new CpuSetType() };
        DESTROYER_THREAD_CORES[0].set(1);
        DESTROYER_THREAD_CORES[1].set(2);

        LOOKUP_THREAD_CORES = new CpuSetType();
        LOOKUP_THREAD_CORES.set(3);
    }

    public void free() {
        MAIN_THREAD_CORES.free();
        for (CpuSetType c : DESTROYER_THREAD_CORES) {
            c.free();
        }
        LOOKUP_THREAD_CORES.free();

        if (scratchBufferAddress != null && !scratchBufferAddress.equals(Pointer.NULL)) {
            scratchBufferAddress.free();
            scratchBufferAddress = null;
        }

        if (initialMainThreadAffinity != null) {
            initialMainThreadAffinity.free();
            initialMainThreadAffinity = null;
        }
    }
}
