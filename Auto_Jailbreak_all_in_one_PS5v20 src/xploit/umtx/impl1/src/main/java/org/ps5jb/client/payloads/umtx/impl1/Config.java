package org.ps5jb.client.payloads.umtx.impl1;

import org.ps5jb.sdk.include.sys.Pipe;
import org.ps5jb.sdk.include.sys.rtprio.RtPrioType;
import org.ps5jb.sdk.include.sys.rtprio.SchedulingClass;

public class Config {
    // Configuration.
    public static final boolean dumpKernelStackPartially = false;
    public static final boolean dumpKernelStackOfReclaimThread = false;

    public static final boolean toggleSetThreadPriorities = false;
    public static final boolean toggleEnableThreadPriorityForReclaimThreads = false;
    public static final boolean toggleStoppingWorkingThreadsBeforeRemap = true;
    public static final boolean toggleReclaimCpuAffinityMask = false;
    public static final boolean toggleDestroyerAffinityOnReclaimThread = false;
    public static final boolean toggleUnmappingOnFailure = true;
    public static final boolean toggleSprayOnDestroyThread = true;
    public static final boolean toggleMainThreadWait = false;
    public static final boolean toggleLoggingUi = false;

    // Common parameters.
    public static final int MAX_EXPLOITATION_ATTEMPTS = 100000;
    public static final int MAX_RACING_ITERATIONS = 50000;
    public static int MAX_DUMMY_SHARED_MEMORY_OBJECTS = 0;
    public static final int MAX_DESTROYER_THREADS = 2;
    public static int MAX_SPRAY_MUTEXES_PER_THREAD = 35;
    public static int MAX_RECLAIM_THREADS = 250;
    public static final int MAX_SEARCH_LOOP_INVOCATIONS = 2;
    public static final int MAX_EXTRA_USER_MUTEXES = 0;
    public static final int MAX_DESCRIPTORS = 0x3FF;

    // Amounts of milliseconds we need to wait at different steps.
    public static final long INITIAL_WAIT_PERIOD = 50; // 50
    public static final long KERNEL_STACK_WAIT_PERIOD = 100;
    public static final long TINY_WAIT_PERIOD = 50; // 50

    // Special marker to determine victim thread's ID.
    public static final int RECLAIM_THREAD_MARKER_BASE = 0x00001337;

    public static final long MAX_PIPE_BUFFER_SIZE = Pipe.PIPE_MINDIRECT / 2;

    // State size for reclaim threads.
    public static final long MARKER_SIZE = 8;
    public static final long STATE_SIZE = 2 * MARKER_SIZE;

    // Priorities for such threads. `RTP_PRIO_FIFO` should also work.
    public static RtPrioType MAIN_THREAD_PRIORITY = new RtPrioType(SchedulingClass.RTP_PRIO_REALTIME, (short) 256);
    public static RtPrioType DESTROYER_THREAD_PRIORITY = new RtPrioType(SchedulingClass.RTP_PRIO_REALTIME, (short) 256); // 256
    public static RtPrioType LOOKUP_THREAD_PRIORITY = new RtPrioType(SchedulingClass.RTP_PRIO_REALTIME, (short) 767); // 767, 400
    public static RtPrioType RECLAIM_THREAD_PRIORITY = new RtPrioType(SchedulingClass.RTP_PRIO_REALTIME, (short) 450); // 450

    // Max length of reclaim thread name.
    public static int MAX_RECLAIM_THREAD_NAME_SIZE = 0x10;
}
