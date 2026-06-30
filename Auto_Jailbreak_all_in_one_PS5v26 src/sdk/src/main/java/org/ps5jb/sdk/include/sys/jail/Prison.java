package org.ps5jb.sdk.include.sys.jail;

import java.nio.charset.Charset;

import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.Param;
import org.ps5jb.sdk.include.sys.mutex.MutexType;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Incomplete wrapper for FreeBSD <code>prison</code> structure.
 */
public class Prison {
    public static final int HOSTUUIDLEN = 64;
    public static final int OSRELEASELEN = 32;

    public static final long OFFSET_PR_LIST_TQE_NEXT = 0L;
    public static final long OFFSET_PR_LIST_TQE_PREV = OFFSET_PR_LIST_TQE_NEXT + 8L;
    public static final long OFFSET_PR_ID = OFFSET_PR_LIST_TQE_PREV + 8L;
    public static final long OFFSET_PR_REF = OFFSET_PR_ID + 4L;
    public static final long OFFSET_PR_UREF = OFFSET_PR_REF + 4L;
    public static final long OFFSET_PR_FLAGS = OFFSET_PR_UREF + 4L;
    public static final long OFFSET_PR_CHILDREN = OFFSET_PR_FLAGS + 4L;
    public static final long OFFSET_PR_SIBLING_LE_NEXT = OFFSET_PR_CHILDREN + 8L;
    public static final long OFFSET_PR_SIBLING_LE_PREV = OFFSET_PR_SIBLING_LE_NEXT + 8L;
    public static final long OFFSET_PR_PARENT = OFFSET_PR_SIBLING_LE_PREV + 8L;
    public static final long OFFSET_PR_MTX = OFFSET_PR_PARENT + 8L;
    public static final long OFFSET_PR_ROOT = OFFSET_PR_MTX + MutexType.SIZE + 64L;
    public static final long OFFSET_PR_IP4S = OFFSET_PR_ROOT + 8L;
    public static final long OFFSET_PR_IP6S = OFFSET_PR_IP4S + 4L;
    public static final long OFFSET_PR_IP4 = OFFSET_PR_IP6S + 4L;
    public static final long OFFSET_PR_IP6 = OFFSET_PR_IP4 + 8L;
    public static final long OFFSET_PR_OSRELDATE = OFFSET_PR_IP6 + 8L + 84L;
    public static final long OFFSET_PR_HOSTID = OFFSET_PR_OSRELDATE + 4L;
    public static final long OFFSET_PR_NAME = OFFSET_PR_HOSTID + 8L;
    public static final long OFFSET_PR_PATH = OFFSET_PR_NAME + Param.MAXHOSTNAMELEN;
    public static final long OFFSET_PR_HOSTNAME = OFFSET_PR_PATH + Param.MAXPATHLEN;
    public static final long OFFSET_PR_DOMAINNAME = OFFSET_PR_HOSTNAME + Param.MAXHOSTNAMELEN;
    public static final long OFFSET_PR_HOSTUUID = OFFSET_PR_DOMAINNAME + Param.MAXHOSTNAMELEN;
    public static final long OFFSET_PR_OSRELEASE = OFFSET_PR_HOSTUUID + HOSTUUIDLEN;

    public static final long SIZE = OFFSET_PR_OSRELEASE + OSRELEASELEN;

    private final KernelPointer ptr;

    /**
     * Process constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing Prison data.
     */
    public Prison(KernelPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Reference count.
     *
     * @return Returns the value of <code>pr_ref</code> field of <code>prison</code> structure.
     */
    public int getRef() {
        return ptr.read4(OFFSET_PR_REF);
    }

    /**
     * User (alive) reference count.
     *
     * @return Returns the value of <code>pr_uref</code> field of <code>prison</code> structure.
     */
    public int getUref() {
        return ptr.read4(OFFSET_PR_UREF);
    }

    /**
     * Vnode to rdir.
     *
     * @return Returns the value of <code>pr_root</code> field of <code>prison</code> structure.
     */
    public KernelPointer getRoot() {
        return ptr.pptr(OFFSET_PR_ROOT);
    }

    /**
     * Admin jail name.
     *
     * @return Returns the value of <code>pr_name</code> field of <code>prison</code> structure.
     */
    public String getName() {
        return ptr.readString(OFFSET_PR_NAME, new Integer(Param.MAXHOSTNAMELEN), Charset.defaultCharset().name());
    }

    /**
     * Path of chroot.
     *
     * @return Returns the value of <code>pr_path</code> field of <code>prison</code> structure.
     */
    public String getPath() {
        return ptr.readString(OFFSET_PR_PATH, new Integer(Param.MAXPATHLEN), Charset.defaultCharset().name());
    }

    /**
     * Jail hostname.
     *
     * @return Returns the value of <code>pr_hostname</code> field of <code>prison</code> structure.
     */
    public String getHostname() {
        return ptr.readString(OFFSET_PR_HOSTNAME, new Integer(Param.MAXHOSTNAMELEN), Charset.defaultCharset().name());
    }

    /**
     * Jail domain name.
     *
     * @return Returns the value of <code>pr_domainname</code> field of <code>prison</code> structure.
     */
    public String getDomainName() {
        return ptr.readString(OFFSET_PR_DOMAINNAME, new Integer(Param.MAXHOSTNAMELEN), Charset.defaultCharset().name());
    }

    /**
     * Jail hostuuid.
     *
     * @return Returns the value of <code>pr_hostuuid</code> field of <code>prison</code> structure.
     */
    public String getHostUuid() {
        return ptr.readString(OFFSET_PR_HOSTUUID, new Integer(HOSTUUIDLEN), Charset.defaultCharset().name());
    }

    /**
     * Value of kern.osrelease.
     *
     * @return Returns the value of <code>pr_osrelease</code> field of <code>prison</code> structure.
     */
    public String getOsRelease() {
        return ptr.readString(OFFSET_PR_OSRELEASE, new Integer(OSRELEASELEN), Charset.defaultCharset().name());
    }

    /**
     * Value of kern.osreldate.
     *
     * @return Returns the value of <code>pr_osreldate</code> field of <code>prison</code> structure.
     */
    public int getOsRelDate() {
        return ptr.read4(OFFSET_PR_OSRELDATE);
    }

    /**
     * v4 IPs of jail.
     *
     * @return Returns the value of <code>pr_ipv4</code> field of <code>prison</code> structure.
     */
    public KernelPointer getIpv4() {
        return ptr.pptr(OFFSET_PR_IP4);
    }

    /**
     * Number of v4 IPs.
     *
     * @return Returns the value of <code>pr_ipv4s</code> field of <code>prison</code> structure.
     */
    public int getIpv4Count() {
        return ptr.read4(OFFSET_PR_IP4S);
    }

    /**
     * Gets the native memory pointer where this Prison's data is stored.
     *
     * @return Prison memory pointer.
     */
    public KernelPointer getPointer() {
        return this.ptr;
    }
}
