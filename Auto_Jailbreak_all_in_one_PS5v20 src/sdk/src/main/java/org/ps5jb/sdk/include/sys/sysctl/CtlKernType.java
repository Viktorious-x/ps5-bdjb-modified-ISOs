package org.ps5jb.sdk.include.sys.sysctl;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * {@link CtlType#CTL_KERN} identifiers.
 */
public final class CtlKernType implements Comparable {
    /** string: system version */
    public static final CtlKernType KERN_OSTYPE = new CtlKernType(1, "KERN_OSTYPE");
    /** string: system release */
    public static final CtlKernType KERN_OSRELEASE = new CtlKernType(2, "KERN_OSRELEASE");
    /** int: system revision */
    public static final CtlKernType KERN_OSREV = new CtlKernType(3, "KERN_OSREV");
    /** string: compile time info */
    public static final CtlKernType KERN_VERSION = new CtlKernType(4, "KERN_VERSION");
    /** int: max vnodes */
    public static final CtlKernType KERN_MAXVNODES = new CtlKernType(5, "KERN_MAXVNODES");
    /** int: max processes */
    public static final CtlKernType KERN_MAXPROC = new CtlKernType(6, "KERN_MAXPROC");
    /** int: max open files */
    public static final CtlKernType KERN_MAXFILES = new CtlKernType(7, "KERN_MAXFILES");
    /** int: max arguments to exec */
    public static final CtlKernType KERN_ARGMAX = new CtlKernType(8, "KERN_ARGMAX");
    /** int: system security level */
    public static final CtlKernType KERN_SECURELVL = new CtlKernType(9, "KERN_SECURELVL");
    /** string: hostname */
    public static final CtlKernType KERN_HOSTNAME = new CtlKernType(10, "KERN_HOSTNAME");
    /** int: host identifier */
    public static final CtlKernType KERN_HOSTID = new CtlKernType(11, "KERN_HOSTID");
    /** struct: struct clockrate */
    public static final CtlKernType KERN_CLOCKRATE = new CtlKernType(12, "KERN_CLOCKRATE");
    /** struct: vnode structures */
    public static final CtlKernType KERN_VNODE = new CtlKernType(13, "KERN_VNODE");
    /** struct: process entries */
    public static final CtlKernType KERN_PROC = new CtlKernType(14, "KERN_PROC");
    /** struct: file entries */
    public static final CtlKernType KERN_FILE = new CtlKernType(15, "KERN_FILE");
    /** node: kernel profiling info */
    public static final CtlKernType KERN_PROF = new CtlKernType(16, "KERN_PROF");
    /** int: POSIX.1 version */
    public static final CtlKernType KERN_POSIX1 = new CtlKernType(17, "KERN_POSIX1");
    /** int: # of supplemental group ids */
    public static final CtlKernType KERN_NGROUPS = new CtlKernType(18, "KERN_NGROUPS");
    /** int: is job control available */
    public static final CtlKernType KERN_JOB_CONTROL = new CtlKernType(19, "KERN_JOB_CONTROL");
    /** int: saved set-user/group-ID */
    public static final CtlKernType KERN_SAVED_IDS = new CtlKernType(20, "KERN_SAVED_IDS");
    /** struct: time kernel was booted */
    public static final CtlKernType KERN_BOOTTIME = new CtlKernType(21, "KERN_BOOTTIME");
    /** string: YP domain name */
    public static final CtlKernType KERN_NISDOMAINNAME = new CtlKernType(22, "KERN_NISDOMAINNAME");
    /** int: update process sleep time */
    public static final CtlKernType KERN_UPDATEINTERVAL = new CtlKernType(23, "KERN_UPDATEINTERVAL");
    /** int: kernel release date */
    public static final CtlKernType KERN_OSRELDATE = new CtlKernType(24, "KERN_OSRELDATE");
    /** node: NTP PLL control */
    public static final CtlKernType KERN_NTP_PLL = new CtlKernType(25, "KERN_NTP_PLL");
    /** string: name of booted kernel */
    public static final CtlKernType KERN_BOOTFILE = new CtlKernType(26, "KERN_BOOTFILE");
    /** int: max open files per proc */
    public static final CtlKernType KERN_MAXFILESPERPROC = new CtlKernType(27, "KERN_MAXFILESPERPROC");
    /** int: max processes per uid */
    public static final CtlKernType KERN_MAXPROCPERUID = new CtlKernType(28, "KERN_MAXPROCPERUID");
    /** struct cdev *: device to dump on */
    public static final CtlKernType KERN_DUMPDEV = new CtlKernType(29, "KERN_DUMPDEV");
    /** node: anything related to IPC */
    public static final CtlKernType KERN_IPC = new CtlKernType(30, "KERN_IPC");
    /** unused */
    public static final CtlKernType KERN_DUMMY = new CtlKernType(31, "KERN_DUMMY");
    /** int: address of PS_STRINGS */
    public static final CtlKernType KERN_PS_STRINGS = new CtlKernType(32, "KERN_PS_STRINGS");
    /** int: address of USRSTACK */
    public static final CtlKernType KERN_USRSTACK = new CtlKernType(33, "KERN_USRSTACK");
    /** int: do we log sigexit procs? */
    public static final CtlKernType KERN_LOGSIGEXIT = new CtlKernType(34, "KERN_LOGSIGEXIT");
    /** int: value of UIO_MAXIOV */
    public static final CtlKernType KERN_IOV_MAX = new CtlKernType(35, "KERN_IOV_MAX");
    /** string: host UUID identifier */
    public static final CtlKernType KERN_HOSTUUID = new CtlKernType(36, "KERN_HOSTUUID");
    /** int: from arc4rand() */
    public static final CtlKernType KERN_ARND = new CtlKernType(37, "KERN_ARND");

    /** All possible CtlKernType values. */
    private static final CtlKernType[] values = new CtlKernType[] {
            KERN_OSTYPE,
            KERN_OSRELEASE,
            KERN_OSREV,
            KERN_VERSION,
            KERN_MAXVNODES,
            KERN_MAXPROC,
            KERN_MAXFILES,
            KERN_ARGMAX,
            KERN_SECURELVL,
            KERN_HOSTNAME,
            KERN_HOSTID,
            KERN_CLOCKRATE,
            KERN_VNODE,
            KERN_PROC,
            KERN_FILE,
            KERN_PROF,
            KERN_POSIX1,
            KERN_NGROUPS,
            KERN_JOB_CONTROL,
            KERN_SAVED_IDS,
            KERN_BOOTTIME,
            KERN_NISDOMAINNAME,
            KERN_UPDATEINTERVAL,
            KERN_OSRELDATE,
            KERN_NTP_PLL,
            KERN_BOOTFILE,
            KERN_MAXFILESPERPROC,
            KERN_MAXPROCPERUID,
            KERN_DUMPDEV,
            KERN_IPC,
            KERN_DUMMY,
            KERN_PS_STRINGS,
            KERN_USRSTACK,
            KERN_LOGSIGEXIT,
            KERN_IOV_MAX,
            KERN_HOSTUUID,
            KERN_ARND
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
    private CtlKernType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for CtlKernType.
     *
     * @return Array of CtlKernType possible values.
     */
    public static CtlKernType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a CtlKernType constant.
     *
     * @param value Number to convert
     * @return CtlKernType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any CtlKernType.
     */
    public static CtlKernType valueOf(short value) {
        for (CtlKernType ctlKern : values) {
            if (value == ctlKern.value()) {
                return ctlKern;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(CtlKernType.class,"invalidValue", Integer.toString(value)));
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
        return this.value - ((CtlKernType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof CtlKernType) {
            result = value == ((CtlKernType) o).value;
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
