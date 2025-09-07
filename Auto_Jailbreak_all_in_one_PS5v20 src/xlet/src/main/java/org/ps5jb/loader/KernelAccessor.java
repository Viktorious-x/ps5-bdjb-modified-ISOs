package org.ps5jb.loader;

import java.io.Serializable;

/**
 * <p>
 * Interface for accessing Kernel memory. Various exploits able to
 * do so should implement this interface.
 * </p>
 * <p>
 * The kernel accessor implementations should be Serializable. This is because
 * Jar Loader has a different classloader than a JAR that will activate the kernel access.
 * So when a JAR ends execution, Kernel Accessor state will be serialized rather than being
 * stored as class instance. Upon execution of another JAR, the kernel accessor will
 * be deserialized and activated again. Assuming that all JARs contain the same kernel
 * accessor implementation, the kernel accessor will remain in place for subsequent JAR
 * executions after it is activated initially.
 * </p>
 */
public interface KernelAccessor extends Serializable {
    byte read1(long kernelAddress);
    short read2(long kernelAddress);
    int read4(long kernelAddress);
    long read8(long kernelAddress);

    void write1(long kernelAddress, byte value);
    void write2(long kernelAddress, short value);
    void write4(long kernelAddress, int value);
    void write8(long kernelAddress, long value);

    long getKernelBase();
}
