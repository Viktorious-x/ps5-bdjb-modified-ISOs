package org.ps5jb.sdk.include.sys.sysctl;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * {@link CtlKernType#KERN_PROC} identifiers.
 */
public final class CtlKernProcType implements Comparable {
    /** Everything */
    public static final CtlKernProcType KERN_PROC_ALL = new CtlKernProcType(0, "KERN_PROC_ALL");
    /** By process id */
    public static final CtlKernProcType KERN_PROC_PID = new CtlKernProcType(1, "KERN_PROC_PID");
    /** By process group id */
    public static final CtlKernProcType KERN_PROC_PGRP = new CtlKernProcType(2, "KERN_PROC_PGRP");
    /** By session of pid */
    public static final CtlKernProcType KERN_PROC_SESSION = new CtlKernProcType(3, "KERN_PROC_SESSION");
    /** By controlling tty */
    public static final CtlKernProcType KERN_PROC_TTY = new CtlKernProcType(4, "KERN_PROC_TTY");
    /** By effective uid */
    public static final CtlKernProcType KERN_PROC_UID = new CtlKernProcType(5, "KERN_PROC_UID");
    /** By real uid */
    public static final CtlKernProcType KERN_PROC_RUID = new CtlKernProcType(6, "KERN_PROC_RUID");
    /** Get/set arguments/proctitle */
    public static final CtlKernProcType KERN_PROC_ARGS = new CtlKernProcType(7, "KERN_PROC_ARGS");
    /** Only return procs */
    public static final CtlKernProcType KERN_PROC_PROC = new CtlKernProcType(8, "KERN_PROC_PROC");
    /** Get syscall vector name */
    public static final CtlKernProcType KERN_PROC_SV_NAME = new CtlKernProcType(9, "KERN_PROC_SV_NAME");
    /** By real group id */
    public static final CtlKernProcType KERN_PROC_RGID = new CtlKernProcType(10, "KERN_PROC_RGID");
    /** By effective group id */
    public static final CtlKernProcType KERN_PROC_GID = new CtlKernProcType(11, "KERN_PROC_GID");
    /** Path to executable */
    public static final CtlKernProcType KERN_PROC_PATHNAME = new CtlKernProcType(12, "KERN_PROC_PATHNAME");
    /** Old VM map entries for process */
    public static final CtlKernProcType KERN_PROC_OVMMAP = new CtlKernProcType(13, "KERN_PROC_OVMMAP");
    /** Old file descriptors for process */
    public static final CtlKernProcType KERN_PROC_OFILEDESC = new CtlKernProcType(14, "KERN_PROC_OFILEDESC");
    /** Kernel stacks for process */
    public static final CtlKernProcType KERN_PROC_KSTACK = new CtlKernProcType(15, "KERN_PROC_KSTACK");
    /** Modifier for pid, pgrp, tty, uid, ruid, gid, rgid and proc. This effectively uses 16-31 */
    public static final CtlKernProcType KERN_PROC_INC_THREAD = new CtlKernProcType(0x10, "KERN_PROC_INC_THREAD");
    /** VM map entries for process */
    public static final CtlKernProcType KERN_PROC_VMMAP = new CtlKernProcType(32, "KERN_PROC_VMMAP");
    /** File descriptors for process */
    public static final CtlKernProcType KERN_PROC_FILEDESC = new CtlKernProcType(33, "KERN_PROC_FILEDESC");
    /** Process groups */
    public static final CtlKernProcType KERN_PROC_GROUPS = new CtlKernProcType(34, "KERN_PROC_GROUPS");
    /** Get environment */
    public static final CtlKernProcType KERN_PROC_ENV = new CtlKernProcType(35, "KERN_PROC_ENV");
    /** Get ELF auxiliary vector */
    public static final CtlKernProcType KERN_PROC_AUXV = new CtlKernProcType(36, "KERN_PROC_AUXV");
    /** Process resource limits */
    public static final CtlKernProcType KERN_PROC_RLIMIT = new CtlKernProcType(37, "KERN_PROC_RLIMIT");
    /** Get ps_strings location */
    public static final CtlKernProcType KERN_PROC_PS_STRINGS = new CtlKernProcType(38, "KERN_PROC_PS_STRINGS");
    /** Process umask */
    public static final CtlKernProcType KERN_PROC_UMASK = new CtlKernProcType(39, "KERN_PROC_UMASK");
    /** Osreldate for process binary */
    public static final CtlKernProcType KERN_PROC_OSREL = new CtlKernProcType(40, "KERN_PROC_OSREL");
    /** Signal trampoline location */
    public static final CtlKernProcType KERN_PROC_SIGTRAMP = new CtlKernProcType(41, "KERN_PROC_SIGTRAMP");
    /** Process current working directory */
    public static final CtlKernProcType KERN_PROC_CWD = new CtlKernProcType(42, "KERN_PROC_CWD");
    /** Number of open file descriptors */
    public static final CtlKernProcType KERN_PROC_NFDS = new CtlKernProcType(43, "KERN_PROC_NFDS");

    /** All possible CtlKernProcType values. */
    private static final CtlKernProcType[] values = new CtlKernProcType[] {
            KERN_PROC_ALL,
            KERN_PROC_PID,
            KERN_PROC_PGRP,
            KERN_PROC_SESSION,
            KERN_PROC_TTY,
            KERN_PROC_UID,
            KERN_PROC_RUID,
            KERN_PROC_ARGS,
            KERN_PROC_PROC,
            KERN_PROC_SV_NAME,
            KERN_PROC_RGID,
            KERN_PROC_GID,
            KERN_PROC_PATHNAME,
            KERN_PROC_OVMMAP,
            KERN_PROC_OFILEDESC,
            KERN_PROC_KSTACK,
            KERN_PROC_INC_THREAD,
            KERN_PROC_VMMAP,
            KERN_PROC_FILEDESC,
            KERN_PROC_GROUPS,
            KERN_PROC_ENV,
            KERN_PROC_AUXV,
            KERN_PROC_RLIMIT,
            KERN_PROC_PS_STRINGS,
            KERN_PROC_UMASK,
            KERN_PROC_OSREL,
            KERN_PROC_SIGTRAMP,
            KERN_PROC_CWD,
            KERN_PROC_NFDS
    };

    private final int value;

    private final String name;

    /**
     * Default constructor. This class should not be instantiated manually,
     * use provided constants instead.
     *
     * @param value Numeric value of this instance.
     * @param name String representation of the constant.
     */
    private CtlKernProcType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for CtlKernProcType.
     *
     * @return Array of CtlKernProcType possible values.
     */
    public static CtlKernProcType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a CtlKernProcType constant.
     *
     * @param value Number to convert
     * @return CtlKernProcType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any CtlKernProcType.
     */
    public static CtlKernProcType valueOf(short value) {
        for (CtlKernProcType ctlKernProc : values) {
            if (value == ctlKernProc.value()) {
                return ctlKernProc;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(CtlKernProcType.class,"invalidValue", Integer.toString(value)));
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public int value() {
        return this.value;
    }

    @Override
    public int compareTo(Object o) {
        return this.value - ((CtlKernProcType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof CtlKernProcType) {
            result = value == ((CtlKernProcType) o).value;
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }
}
