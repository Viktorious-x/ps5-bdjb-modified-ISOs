package org.ps5jb.client.payloads.umtx.common;

import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.CpuSet;
import org.ps5jb.sdk.include.sys.RtPrio;
import org.ps5jb.sdk.include.sys.cpuset.CpuSetType;
import org.ps5jb.sdk.include.sys.rtprio.RtPrioType;
import org.ps5jb.sdk.include.sys.rtprio.SchedulingClass;
import org.ps5jb.sdk.lib.LibKernel;

public class Executor {
    public interface FinishEvaluator {
        boolean isFinished(Runnable job);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    public static void runInNewThread(String threadName, Runnable job, FinishEvaluator finishEvaluator,
                                      int parentThreadCore, short parentThreadPriority) {

        LibKernel libKernel = new LibKernel();
        CpuSet cpuSet = new CpuSet(libKernel);
        RtPrio rtPrio = new RtPrio(libKernel);

        CpuSetType initialRootAffinity = null;
        RtPrioType initialRootPriority = null;

        RtPrioType newParentPriority = new RtPrioType(SchedulingClass.RTP_PRIO_REALTIME, parentThreadPriority);
        CpuSetType newParentAffinity = new CpuSetType();
        try {
            initialRootAffinity = cpuSet.getCurrentThreadAffinity();
            initialRootPriority = rtPrio.lookupRtPrio(0);

            newParentAffinity.set(parentThreadCore);
            cpuSet.setCurrentThreadAffinity(newParentAffinity);
            rtPrio.setRtPrio(0, newParentPriority);
        } catch (SdkException | RuntimeException | Error e) {
            DebugStatus.error("Unable to set parent thread core or priority", e);
        } finally {
            newParentAffinity.free();
        }

        // Start job in the new thread and wait
        Thread mainThread = new Thread(job, threadName);
        mainThread.start();

        sleep(1000L);

        while (!finishEvaluator.isFinished(job)) {
            sleep(2000L);
        }

        try {
            if (initialRootAffinity != null) {
                cpuSet.setCurrentThreadAffinity(initialRootAffinity);
            }
            if (initialRootPriority != null) {
                rtPrio.setRtPrio(0, initialRootPriority);
            }
        } catch (SdkException | RuntimeException | Error e) {
            DebugStatus.error("Unable to recover parent thread core or priority", e);
        }
    }
}
