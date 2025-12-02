package org.ps5jb.client.payloads.umtx.common;

import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;

public class KernelStackMarkerOffsets {
    public final long OFFSET_RET_FROM_MARKER;
    public final long OFFSET_KBASE_FROM_RET;

    /**
     * Constructor. The firmware version can be obtained
     * by making a call to <code>sceKernelGetProsperoSystemSwVersion</code>
     * method in <code>libkernel</code>. Last two bytes of the result return
     * the minor and the major version of the firmware.
     *
     * @param softwareVersion Firmware version in the form 0x[MAJOR BYTE][MINOR BYTE]
     */
    public KernelStackMarkerOffsets(int softwareVersion) {
        switch (softwareVersion) {
            case 0x0102:
            {
                OFFSET_RET_FROM_MARKER = -0xC4;
                OFFSET_KBASE_FROM_RET = -0x004D4108;
                break;
            }
            case 0x0250:
            {
                OFFSET_RET_FROM_MARKER = -0xD4;
                OFFSET_KBASE_FROM_RET = -0x0049F85E;
                break;
            }
            case 0x0602:
            {
                OFFSET_RET_FROM_MARKER = -0xC4;
                OFFSET_KBASE_FROM_RET = -0x004FC109;
                break;
            }
            case 0x0650:
            {
                OFFSET_RET_FROM_MARKER = -0xC4;
                OFFSET_KBASE_FROM_RET = -0x004FC189;
                break;
            }
            case 0x0720:
                OFFSET_RET_FROM_MARKER = -0xC4;
                OFFSET_KBASE_FROM_RET = -0x004F5C55;
                break;
            default:
                throw new SdkSoftwareVersionUnsupportedException("Firmware not supported: 0x" + Integer.toHexString(softwareVersion));
        }
    }
}
