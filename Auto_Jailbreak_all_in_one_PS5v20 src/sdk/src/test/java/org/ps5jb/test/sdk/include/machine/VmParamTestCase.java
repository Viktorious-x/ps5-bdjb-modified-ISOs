package org.ps5jb.test.sdk.include.machine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ps5jb.sdk.include.machine.VmParam;

public class VmParamTestCase {
    @Test
    public void testVmMinKernelAddress() {
        Assertions.assertEquals(0xFFFFFF8000000000L, VmParam.VM_MIN_KERNEL_ADDRESS);
    }
    @Test
    public void testVmKernBase() {
        Assertions.assertEquals(0xffffffff80000000L, VmParam.KERN_BASE);
    }
}
