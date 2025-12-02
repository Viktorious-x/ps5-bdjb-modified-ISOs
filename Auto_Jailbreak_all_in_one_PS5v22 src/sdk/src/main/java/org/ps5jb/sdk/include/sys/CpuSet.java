package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.sys.cpuset.CpuLevelType;
import org.ps5jb.sdk.include.sys.cpuset.CpuSetType;
import org.ps5jb.sdk.include.sys.cpuset.CpuWhichType;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/sys/cpuset.h</code> from FreeBSD source.
 */
public class CpuSet {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public CpuSet(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    public CpuSetType getAffinity(CpuLevelType cpuLevel, CpuWhichType cpuWhich, int id) throws SdkException {
        CpuSetType result = new CpuSetType();
        try {
            int ret = libKernel.cpuset_getaffinity(cpuLevel.value(), cpuWhich.value(), id, result.getSize(), result.getPointer());
            if (ret == -1) {
                throw errNo.getLastException(getClass(), "getAffinity");
            }
            result.refresh();
            return result;
        } catch (SdkException | RuntimeException | Error e) {
            result.free();
            throw e;
        }
    }

    public void setAffinity(CpuLevelType cpuLevel, CpuWhichType cpuWhich, int id, CpuSetType cpuSetType) throws SdkException {
        int ret = libKernel.cpuset_setaffinity(cpuLevel.value(), cpuWhich.value(), id, cpuSetType.getSize(), cpuSetType.getPointer());
        if (ret == -1) {
            throw errNo.getLastException(getClass(), "setAffinity");
        }
    }

    public CpuSetType getCurrentThreadAffinity() throws SdkException {
        return getAffinity(CpuLevelType.CPU_LEVEL_WHICH, CpuWhichType.CPU_WHICH_TID, -1);
    }

    public void setCurrentThreadAffinity(CpuSetType affinity) throws SdkException {
        setAffinity(CpuLevelType.CPU_LEVEL_WHICH, CpuWhichType.CPU_WHICH_TID, -1, affinity);
    }

    public void setCurrentThreadCore(int core) throws SdkException {
        CpuSetType affinity = new CpuSetType();
        try {
            affinity.set(core);
            setCurrentThreadAffinity(affinity);
        } finally {
            affinity.free();
        }
    }
}
