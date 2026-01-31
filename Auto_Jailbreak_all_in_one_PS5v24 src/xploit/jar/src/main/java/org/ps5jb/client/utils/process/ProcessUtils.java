package org.ps5jb.client.utils.process;

import org.ps5jb.client.PayloadConstants;
import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.core.kernel.KernelOffsets;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.RtPrio;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.include.sys.rtprio.RtPrioType;
import org.ps5jb.sdk.include.sys.rtprio.SchedulingClass;
import org.ps5jb.sdk.include.sys.ucred.UCred;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Utility class for various operations on kernel processes.
 */
public class ProcessUtils {
    private final KernelPointer kbaseAddress;
    private final KernelOffsets offsets;
    private final LibKernel libKernel;

    /**
     * Constructor taking only libKernel instance as the argument.
     * The instance of this class should only be used by the same thread
     * as the thread that is using the supplied LibKernel instance.
     *
     * @param libKernel Instance of LibKernel native library.
     */
    public ProcessUtils(LibKernel libKernel) {
        KernelAccessor kernelAccessor = KernelReadWrite.getAccessor(getClass().getClassLoader());
        if (kernelAccessor == null) {
            throw new SdkRuntimeException("Kernel R/W is required to instantiate this class.");
        }

        this.libKernel = libKernel;
        this.kbaseAddress = KernelPointer.valueOf(kernelAccessor.getKernelBase());

        int sw = libKernel.getSystemSoftwareVersion();
        this.offsets = new KernelOffsets(sw);
    }

    /**
     * Constructor taking existing instances of the required utility classes.
     * The instance of this class should only be used by the same thread
     * as the thread that is using the supplied LibKernel instance.
     *
     * @param libKernel An instance of LibKernel library.
     * @param kbaseAddress Base address of the kernel.
     * @param offsets Kernel offsets calculated for the current firmware.
     */
    public ProcessUtils(LibKernel libKernel, KernelPointer kbaseAddress, KernelOffsets offsets) {
        this.libKernel = libKernel;
        this.offsets = offsets;
        this.kbaseAddress = kbaseAddress;
    }

    /**
     * Get <code>proc</code> structure of the current process.
     *
     * @return Current process' <code>proc</code> structure.
     */
    public Process getCurrentProcess() {
        int curPid = libKernel.getpid();
        return getProcessByPid(curPid);
    }

    /**
     * Interface to search processes.
     */
    public interface MatchProcess {
        boolean isMatching(Process proc);
    }

    /**
     * Search process using a custom matching logic.
     *
     * @param procMatch Object which determines whether a given process is a match.
     * @return The matching process or <code>null</code> if no matches are found.
     * @throws NullPointerException If kernel's allproc offset is not known (null).
     */
    public Process searchProcess(MatchProcess procMatch) {
        KernelPointer allproc = KernelPointer.NULL;
        if (KernelPointer.NULL.equals(kbaseAddress) || kbaseAddress == null) {
            String allProcAddrStr = System.getProperty(PayloadConstants.ALLPROC_ADDRESS_PROPERTY);
            if (allProcAddrStr != null) {
                allproc = KernelPointer.valueOf(Long.parseLong(allProcAddrStr));
            }
        } else {
            KernelPointer kdataAddress = kbaseAddress.inc(offsets.OFFSET_KERNEL_DATA);
            allproc = kdataAddress.inc(offsets.OFFSET_KERNEL_DATA_BASE_ALLPROC);
        }

        KernelPointer.nonNull(allproc, "Kernel allproc address could not be determined");

        Process curProc = new Process(allproc.pptr(0));
        while (curProc != null) {
            if (procMatch.isMatching(curProc)) {
                break;
            }
            curProc = curProc.getNextProcess();
        }
        return curProc;
    }

    /**
     * Get <code>proc</code> structure of the process with a given PID.
     *
     * @param pid {@link Process#getPid() PID} to search for.
     * @return Process' <code>proc</code> structure or null if
     *   the process with the given PID is not found.
     * @throws NullPointerException If kernel's allproc offset is not known (null).
     */
    public Process getProcessByPid(final int pid) {
        return searchProcess((proc) -> proc.getPid() == pid);
    }

    /**
     * Get <code>proc</code> structure of the process with a given name.
     *
     * @param name {@link Process#getName() Name} to search for.
     * @return Process' <code>proc</code> structure or null if
     *   the process with the given name is not found.
     * @throws NullPointerException If kernel's allproc offset is not known (null) or
     *   if <code>name</code> is null.
     */
    public Process getProcessByName(final String name) {
        return searchProcess((proc) -> name.equals(proc.getName()));
    }

    /**
     * Returns the ids for the user and group of a given process.
     *
     * @param proc Process whose user/group to set.
     * @return Array of ids for user/group values in that order:
     *   uid, ruid, svuid, ngroups, rgid, svgid, gid1, ... gidN
     *   where N is the number of groups (<code>ngroups</code>).
     */
    public int[] getUserGroup(Process proc) {
        UCred ucred = proc.getUserCredentials();
        KernelPointer groups = ucred.getGroups();
        int groupCount = ucred.getNgroups();

        int[] result = new int[6 + groupCount];
        result[0] = ucred.getUid();
        result[1] = ucred.getRuid();
        result[2] = ucred.getSvuid();
        result[3] = groupCount;
        result[4] = ucred.getRgid();
        result[5] = ucred.getSvgid();
        for (int groupIndex = 0; groupIndex < groupCount; ++groupIndex) {
            result[6 + groupIndex] = groups.read4(groupIndex * 4L);
        }

        return result;
    }

    /**
     * Set the ids for the user and group of a given process.
     *
     * @param proc Process whose user/group to set.
     * @param ids Array of ids to set in that order:
     *   uid, ruid, svuid, ngroups, rgid, svgid, gid1, ... gidN.
     *   For backwards compatibility reasons, it's not necessary to
     *   specify svgid or actual group ids. If the array lenth is
     *   5, only the first 5 elements will be applied.
     * @return Value of ids before the modification.
     */
    public int[] setUserGroup(Process proc, int[] ids) {
        int[] prevValue = getUserGroup(proc);

        UCred ucred = proc.getUserCredentials();
        ucred.setUid(ids[0]);
        ucred.setRuid(ids[1]);
        ucred.setSvuid(ids[2]);
        ucred.setNgroups(ids[3]);
        ucred.setRgid(ids[4]);

        // Compatibility with older implementation, which didn't need svgid or gids
        if (ids.length > 5) {
            ucred.setSvgid(ids[5]);
            KernelPointer groups = ucred.getGroups();
            for (int groupIndex = 0; groupIndex < ids[3]; ++groupIndex) {
                groups.write4(groupIndex * 4L, ids[6 + groupIndex]);
            }
        }

        return prevValue;
    }

    /**
     * Gets PS5-specific privileges for a given process.
     *
     * @param proc Process whose privileges to get.
     * @return Array of privileges in that order: authId, caps1, caps2, attrs[3].
     */
    public long[] getPrivs(Process proc) {
        UCred ucred = proc.getUserCredentials();

        long[] result = new long[4];
        result[0] = ucred.getSceAuthId();
        result[1] = ucred.getSceCaps1();
        result[2] = ucred.getSceCaps2();
        result[3] = ucred.getSceAttrs()[3];

        return result;
    }

    /**
     * Sets PS5-specific privileges for a given process.
     *
     * @param proc Process whose privileges to set.
     * @param privs Array of privileges in that order: authId, caps0, caps1, attrs[3].
     * @return Value of privs before the modification.
     */
    public long[] setPrivs(Process proc, long[] privs) {
        long[] prevValue = getPrivs(proc);

        UCred ucred = proc.getUserCredentials();
        ucred.setSceAuthId(privs[0]);
        ucred.setSceCaps1(privs[1]);
        ucred.setSceCaps2(privs[2]);

        // Attrs are set this way for backwards compatibility.
        byte[] attrs = ucred.getSceAttrs();
        attrs[3] = (byte) privs[3];
        ucred.setSceAttrs(attrs);

        return prevValue;
    }

    /**
     * This method can be used to set the priority of Java thread and of the
     * corresponding native thread.
     *
     * @param javaPriority Priority to set for the java thread. See {@link Thread#setPriority(int)}.
     *   If null, Java thread priority will not be touched.
     * @param rtPrioValue Priority of the native thread. See {@link RtPrio#setRtPrio(int, RtPrioType)}.
     * @throws SdkException If native priority value is not null and native call failed.
     */
    public void setCurrentThreadPriority(Integer javaPriority, Short rtPrioValue) throws SdkException {
        if (javaPriority != null) {
            Thread.currentThread().setPriority(javaPriority.intValue());
        }

        if (rtPrioValue != null) {
            RtPrio rtPrio = new RtPrio(libKernel);
            rtPrio.setRtPrio(0, new RtPrioType(SchedulingClass.RTP_PRIO_REALTIME, rtPrioValue.shortValue()));
        }
    }
}
