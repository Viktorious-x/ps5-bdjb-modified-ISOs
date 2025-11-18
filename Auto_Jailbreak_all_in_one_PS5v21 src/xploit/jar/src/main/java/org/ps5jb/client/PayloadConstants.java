package org.ps5jb.client;

/**
 * Various constants which can be used by multiple payloads.
 */
public class PayloadConstants {
    /** System property containing the address of the the kernel "allproc" variable. Value of this system property may be null.  */
    public static final String ALLPROC_ADDRESS_PROPERTY = "org.ps5jb.client.KERNEL_ADDRESS_ALLPROC";

    /** System property containing the original debug settings values of the system.  */
    public static final String ORIG_DEBUG_SETTINGS_VAL_PROPERTY = "org.ps5jb.client.KERNEL_ORIG_DEBUG_SETTINGS";

    private PayloadConstants() {
    }
}
