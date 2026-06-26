package org.ps5jb.sdk.core.kernel;

import java.text.MessageFormat;

import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;

/**
 * Class which is able to return various interesting offsets in kernel based on the console firmware version.
 * Note that currently not all firmware versions are supported.
 * Specter's Webkit UMTX repo was used as source of these offsets.
 */
public class KernelOffsets {
    // Kernel text-relative offsets

    public final long OFFSET_KERNEL_DATA;

    // Kernel data-relative offsets

    public final long OFFSET_KERNEL_DATA_BASE_DYNAMIC;
    public final long OFFSET_KERNEL_DATA_BASE_TO_DYNAMIC;

    public final long OFFSET_KERNEL_DATA_BASE_ALLPROC;
    public final long OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS;
    public final long OFFSET_KERNEL_DATA_BASE_TARGET_ID;
    public final long OFFSET_KERNEL_DATA_BASE_QA_FLAGS;
    public final long OFFSET_KERNEL_DATA_BASE_UTOKEN_FLAGS;
    public final long OFFSET_KERNEL_DATA_BASE_ROOTVNODE;

    public final long OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE;
    public final long OFFSET_KERNEL_DATA_BASE_DATA_CAVE;

    public final long OFFSET_KERNEL_DATA_BASE_GBASE_VM;

    // Kernel PMap Store-relative offsets

    public final long OFFSET_PMAP_STORE_PML4PML4I;
    public final long OFFSET_PMAP_STORE_DMPML4I;
    public final long OFFSET_PMAP_STORE_DMPDPI;

    // Non offsets

    /** Readable size of kernel data section. */
    public final long SIZE_KERNEL_DATA;

    /** PS5 software version of this offset database */
    public final int SOFTWARE_VERSION;

    /**
     * Constructor. The firmware version can be obtained
     * by making a call to <code>sceKernelGetProsperoSystemSwVersion</code>
     * method in <code>libkernel</code>. Last two bytes of the result return
     * the minor and the major version of the firmware.
     *
     * @param softwareVersion Firmware version in the form 0x[MAJOR BYTE][MINOR BYTE]
     */
    public KernelOffsets(int softwareVersion) {
        this.SOFTWARE_VERSION = softwareVersion;

        switch (softwareVersion) {
            case 0x0100:
            case 0x0101:
            case 0x0102:
            {
                OFFSET_KERNEL_DATA = 0x01B40000;
                SIZE_KERNEL_DATA = 0x08631930;

                OFFSET_KERNEL_DATA_BASE_DYNAMIC = 0x00000000;
                OFFSET_KERNEL_DATA_BASE_TO_DYNAMIC = 0x0658BB58;
                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x026D1BF8;
                OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS = 0x06241074;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x06565540;
                OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE = 0x02F9F2B8;
                OFFSET_KERNEL_DATA_BASE_DATA_CAVE = 0x05F20000;

                OFFSET_KERNEL_DATA_BASE_GBASE_VM = 0x06202E70;

                OFFSET_PMAP_STORE_PML4PML4I = -0x1C;
                OFFSET_PMAP_STORE_DMPML4I = 0x288;
                OFFSET_PMAP_STORE_DMPDPI = 0x28C;
                break;
            }
            case 0x0105:
            case 0x0110:
            case 0x0111:
            case 0x0112:
            case 0x0113:
            case 0x0114:
            {
                OFFSET_KERNEL_DATA = 0x01B40000;
                SIZE_KERNEL_DATA = 0x08631930;

                OFFSET_KERNEL_DATA_BASE_DYNAMIC = 0x00000000;
                OFFSET_KERNEL_DATA_BASE_TO_DYNAMIC = 0x0658BB58;
                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x026D1C18;
                OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS = 0x06241074;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x06565540;
                OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE = 0x02F9F328;
                OFFSET_KERNEL_DATA_BASE_DATA_CAVE = 0x05F20000;

                OFFSET_KERNEL_DATA_BASE_GBASE_VM = 0x06202E70;

                OFFSET_PMAP_STORE_PML4PML4I = -0x1C;
                OFFSET_PMAP_STORE_DMPML4I = 0x288;
                OFFSET_PMAP_STORE_DMPDPI = 0x28C;
                break;
            }
            case 0x0200:
            case 0x0220:
            case 0x0225:
            case 0x0226:
            case 0x0230:
            case 0x0250:
            case 0x0270:
            {
                OFFSET_KERNEL_DATA = 0x01B80000;
                SIZE_KERNEL_DATA = 0x087E1930;

                OFFSET_KERNEL_DATA_BASE_DYNAMIC = 0x00000000;
                OFFSET_KERNEL_DATA_BASE_TO_DYNAMIC = 0x06739B88;
                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x02701C28;
                OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS = 0x063E1274;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x067134C0;
                OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE = 0x031338C8;
                OFFSET_KERNEL_DATA_BASE_DATA_CAVE = 0x060C0000;  // Use same as Specter's Byepervisor repo for interop

                OFFSET_KERNEL_DATA_BASE_GBASE_VM = 0x063A2EB0;

                OFFSET_PMAP_STORE_PML4PML4I = -0x1C;
                OFFSET_PMAP_STORE_DMPML4I = 0x288;
                OFFSET_PMAP_STORE_DMPDPI = 0x28C;
                break;
            }
            case 0x0300:
            case 0x0310:
            case 0x0320:
            case 0x0321:
            {
                OFFSET_KERNEL_DATA = 0x0BD0000;
                SIZE_KERNEL_DATA = 0x08871930;

                OFFSET_KERNEL_DATA_BASE_DYNAMIC = 0x00010000;
                OFFSET_KERNEL_DATA_BASE_TO_DYNAMIC = 0x067D1B90;
                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x0276DC58;
                OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS = 0x06466474;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x067AB4C0;
                OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE = 0x031BE218;
                OFFSET_KERNEL_DATA_BASE_DATA_CAVE = 0x06140000;  // Unconfirmed

                OFFSET_KERNEL_DATA_BASE_GBASE_VM = 0x06423F80;

                OFFSET_PMAP_STORE_PML4PML4I = -0x1C;
                OFFSET_PMAP_STORE_DMPML4I = 0x288;
                OFFSET_PMAP_STORE_DMPDPI = 0x28C;
                break;
            }
            case 0x0400:
            case 0x0402:
            case 0x0403:
            case 0x0450:
            case 0x0451:
            {
                OFFSET_KERNEL_DATA = 0x0C00000;
                SIZE_KERNEL_DATA = 0x087B1930;

                OFFSET_KERNEL_DATA_BASE_DYNAMIC = 0x00010000;
                OFFSET_KERNEL_DATA_BASE_TO_DYNAMIC = 0x0670DB90;
                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x027EDCB8;
                OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS = 0x06506474;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x066E74C0;
                OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE = 0x03257A78;
                OFFSET_KERNEL_DATA_BASE_DATA_CAVE = 0x06C01000;  // Unconfirmed

                OFFSET_KERNEL_DATA_BASE_GBASE_VM = 0x064C3F80;

                OFFSET_PMAP_STORE_PML4PML4I = -0x1C;
                OFFSET_PMAP_STORE_DMPML4I = 0x288;
                OFFSET_PMAP_STORE_DMPDPI = 0x28C;
                break;
            }
            case 0x0500:
            case 0x0502:
            case 0x0510:
            {
                OFFSET_KERNEL_DATA = 0x0C40000;
                SIZE_KERNEL_DATA = 0x08921930;

                OFFSET_KERNEL_DATA_BASE_DYNAMIC = 0x00010000;
                OFFSET_KERNEL_DATA_BASE_TO_DYNAMIC = 0x06879C00;
                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x0291DD00;
                OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS = 0x066466EC;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x06853510;
                OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE = 0x03398A88;
                OFFSET_KERNEL_DATA_BASE_DATA_CAVE = 0x06320000;  // Unconfirmed

                OFFSET_KERNEL_DATA_BASE_GBASE_VM = 0x06603FB0;

                OFFSET_PMAP_STORE_PML4PML4I = -0x105C;
                OFFSET_PMAP_STORE_DMPML4I = 0x29C;
                OFFSET_PMAP_STORE_DMPDPI = 0x2A0;
                break;
            }
            case 0x0550:
            {
                OFFSET_KERNEL_DATA = 0x0C40000;
                SIZE_KERNEL_DATA = 0x08921930;

                OFFSET_KERNEL_DATA_BASE_DYNAMIC = 0x00010000;
                OFFSET_KERNEL_DATA_BASE_TO_DYNAMIC = 0x06879C00;
                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x0291DD00;
                OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS = 0x066466EC;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x06853510;
                OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE = 0x03394A88;
                OFFSET_KERNEL_DATA_BASE_DATA_CAVE = 0x06320000;  // Unconfirmed

                OFFSET_KERNEL_DATA_BASE_GBASE_VM = 0x06603FB0;

                OFFSET_PMAP_STORE_PML4PML4I = -0x105C;
                OFFSET_PMAP_STORE_DMPML4I = 0x29C;
                OFFSET_PMAP_STORE_DMPDPI = 0x2A0;
                break;
            }
            case 0x0600:
            case 0x0602:
            case 0x0650:
            {
                OFFSET_KERNEL_DATA = 0x0C60000;
                SIZE_KERNEL_DATA = 0x08861930;

                OFFSET_KERNEL_DATA_BASE_DYNAMIC = 0x00010000;
                OFFSET_KERNEL_DATA_BASE_TO_DYNAMIC = 0x067C5C10;
                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x02869D20;
                OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS = 0x065968EC;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x0679F510;
                OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE = 0x032E4358;
                OFFSET_KERNEL_DATA_BASE_DATA_CAVE = 0x06270000;  // Unconfirmed

                OFFSET_KERNEL_DATA_BASE_GBASE_VM = 0x065540F0;

                OFFSET_PMAP_STORE_PML4PML4I = -0x105C;
                OFFSET_PMAP_STORE_DMPML4I = 0x29C;
                OFFSET_PMAP_STORE_DMPDPI = 0x2A0;
                break;
            }
            case 0x0700:
            case 0x0701:
            case 0x0720:
            case 0x0740:
            case 0x0760:
            case 0x0761:
            {
                OFFSET_KERNEL_DATA = 0x0C50000;
                SIZE_KERNEL_DATA = 0x05191930;

                OFFSET_KERNEL_DATA_BASE_DYNAMIC = 0x00010000;
                OFFSET_KERNEL_DATA_BASE_TO_DYNAMIC = 0x030EDC40;
                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x02859D50;
                OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS = 0x00AC8064;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x030C7510;
                OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE = 0x02E2C848;
                OFFSET_KERNEL_DATA_BASE_DATA_CAVE = 0x050A1000;  // Unconfirmed

                OFFSET_KERNEL_DATA_BASE_GBASE_VM = 0x02E76090;

                OFFSET_PMAP_STORE_PML4PML4I = -0x10AC;
                OFFSET_PMAP_STORE_DMPML4I = 0x29C;
                OFFSET_PMAP_STORE_DMPDPI = 0x2A0;
                break;
            }
            default:
                String strSwVersion = MessageFormat.format(
                        "{0,number,#0}.{1,number,00}",
                        new Object[] {
                                new Integer((softwareVersion >> 8) & 0xFF),
                                new Integer(softwareVersion & 0xFF)
                        }
                );
                throw new SdkSoftwareVersionUnsupportedException(strSwVersion);
        }

        OFFSET_KERNEL_DATA_BASE_TARGET_ID = OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS + 0x09;
        OFFSET_KERNEL_DATA_BASE_QA_FLAGS = OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS + 0x24;
        OFFSET_KERNEL_DATA_BASE_UTOKEN_FLAGS = OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS + 0x8C;
    }
}
