package org.ps5jb.sdk.include.sys.ucred;

import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.jail.Prison;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Incomplete wrapper for FreeBSD <code>ucred</code> structure.
 */
public class UCred {
    public static final int XU_NGROUPS = 16;

    public static final long OFFSET_CR_REF = 0L;
    public static final long OFFSET_CR_UID = OFFSET_CR_REF + 4L;
    public static final long OFFSET_CR_RUID = OFFSET_CR_UID + 4L;
    public static final long OFFSET_CR_SVUID = OFFSET_CR_RUID + 4L;
    public static final long OFFSET_CR_NGROUPS = OFFSET_CR_SVUID + 4L;
    public static final long OFFSET_CR_RGID = OFFSET_CR_NGROUPS + 4L;
    public static final long OFFSET_CR_SVGID = OFFSET_CR_RGID + 4L;
    public static final long OFFSET_CR_UIDINFO = OFFSET_CR_SVGID + 8L;
    public static final long OFFSET_CR_RUIDINFO = OFFSET_CR_UIDINFO + 8L;
    public static final long OFFSET_CR_PRISON = OFFSET_CR_RUIDINFO + 8L;
    public static final long OFFSET_CR_LOGINCLASS = OFFSET_CR_PRISON + 8L;
    public static final long OFFSET_CR_SCE_AUTH_ID = OFFSET_CR_LOGINCLASS + 32L;
    public static final long OFFSET_CR_SCE_CAPS1 = OFFSET_CR_SCE_AUTH_ID + 8L;
    public static final long OFFSET_CR_SCE_CAPS2 = OFFSET_CR_SCE_CAPS1 + 8L;
    public static final long OFFSET_CR_SCE_ATTRS = OFFSET_CR_SCE_CAPS2 + 24L;
    public static final long OFFSET_CR_GROUPS = OFFSET_CR_SCE_ATTRS + 152L;
    public static final long OFFSET_CR_AGROUPS = OFFSET_CR_GROUPS + 8L;
    public static final long OFFSET_CR_SMALL_GROUPS = OFFSET_CR_AGROUPS + 4L;

    public static final long SIZE = OFFSET_CR_SMALL_GROUPS + XU_NGROUPS * 4 + 4;

    private final KernelPointer ptr;

    /**
     * Process constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing UCred data.
     */
    public UCred(KernelPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Reference count.
     *
     * @return Returns the value of <code>cr_ref</code> field of <code>ucred</code> structure.
     */
    public int getRef() {
        return ptr.read4(OFFSET_CR_REF);
    }

    /**
     * Effective user id.
     *
     * @return Returns the value of <code>cr_uid</code> field of <code>ucred</code> structure.
     */
    public int getUid() {
        return ptr.read4(OFFSET_CR_UID);
    }

    /**
     * Set effective user id.
     *
     * @param uid New value for <code>cr_uid</code> field of <code>ucred</code> structure.
     */
    public void setUid(int uid) {
        ptr.write4(OFFSET_CR_UID, uid);
    }

    /**
     * Real user id.
     *
     * @return Returns the value of <code>cr_ruid</code> field of <code>ucred</code> structure.
     */
    public int getRuid() {
        return ptr.read4(OFFSET_CR_RUID);
    }

    /**
     * Set real user id.
     *
     * @param ruid New value for <code>cr_ruid</code> field of <code>ucred</code> structure.
     */
    public void setRuid(int ruid) {
        ptr.write4(OFFSET_CR_RUID, ruid);
    }

    /**
     * Saved user id.
     *
     * @return Returns the value of <code>cr_svuid</code> field of <code>ucred</code> structure.
     */
    public int getSvuid() {
        return ptr.read4(OFFSET_CR_SVUID);
    }

    /**
     * Set saved user id.
     *
     * @param svuid New value for <code>cr_svuid</code> field of <code>ucred</code> structure.
     */
    public void setSvuid(int svuid) {
        ptr.write4(OFFSET_CR_SVUID, svuid);
    }

    /**
     * Number of groups.
     *
     * @return Returns the value of <code>cr_ngroups</code> field of <code>ucred</code> structure.
     */
    public int getNgroups() {
        return ptr.read4(OFFSET_CR_NGROUPS);
    }

    /**
     * Set number of groups.
     *
     * @param ngroups New value for <code>cr_ngroups</code> field of <code>ucred</code> structure.
     * @throws IndexOutOfBoundsException If ngroups is invalid.
     * @throws UnsupportedOperationException If groups pointer does not point to
     *   smallgroups array inside proc structure.
     */
    public void setNgroups(int ngroups) {
        // Make sure groups points to smallgroups
        long groups = ptr.read8(OFFSET_CR_GROUPS);
        long smallGroups = ptr.addr() + OFFSET_CR_SMALL_GROUPS;
        if (groups != smallGroups) {
            throw new UnsupportedOperationException(ErrorMessages.getClassErrorMessage(UCred.class,"unableToChangeNgroups",
                    "0x" + Long.toHexString(groups), "0x" + Long.toHexString(smallGroups)));
        }

        if ((ngroups < 0) || (ngroups > getAvailableGroups())) {
            throw new IndexOutOfBoundsException(Integer.toString(ngroups));
        }

        ptr.write4(OFFSET_CR_NGROUPS, ngroups);
    }

    /**
     * Real group id.
     *
     * @return Returns the value of <code>cr_rgid</code> field of <code>ucred</code> structure.
     */
    public int getRgid() {
        return ptr.read4(OFFSET_CR_RGID);
    }

    /**
     * Set real group id.
     *
     * @param rgid New value for <code>cr_rgid</code> field of <code>ucred</code> structure.
     */
    public void setRgid(int rgid) {
        ptr.write4(OFFSET_CR_RGID, rgid);
    }

    /**
     * Saved group id.
     *
     * @return Returns the value of <code>cr_svgid</code> field of <code>ucred</code> structure.
     */
    public int getSvgid() {
        return ptr.read4(OFFSET_CR_SVGID);
    }

    /**
     * Set saved group id.
     *
     * @param svgid New value for <code>cr_svgid</code> field of <code>ucred</code> structure.
     */
    public void setSvgid(int svgid) {
        ptr.write4(OFFSET_CR_SVGID, svgid);
    }

    /**
     * Jail.
     *
     * @return Returns the value of <code>cr_prison</code> field of <code>ucred</code> structure.
     *   Returns <code>null</code> if prison pointer is {@link KernelPointer#NULL}.
     */
    public Prison getPrison() {
        KernelPointer prisonPtr = ptr.pptr(OFFSET_CR_PRISON);
        if (KernelPointer.NULL.equals(prisonPtr)) {
            return null;
        }
        return new Prison(prisonPtr);
    }

    /**
     * Set new jail.
     *
     * @param prison New jail value.
     */
    public void setPrison(Prison prison) {
        ptr.write8(OFFSET_CR_PRISON, prison == null ? 0 : prison.getPointer().addr());
    }

    /**
     * Auth ID
     *
     * @return Returns the PS5-specific auth ID value in <code>ucred</code> structure.
     */
    public long getSceAuthId() {
        return ptr.read8(OFFSET_CR_SCE_AUTH_ID);
    }

    /**
     * Set auth ID
     *
     * @param authId New auth ID value.
     */
    public void setSceAuthId(long authId) {
        ptr.write8(OFFSET_CR_SCE_AUTH_ID, authId);
    }

    /**
     * Capability flags (1st set)
     *
     * @return Returns the PS5-specific capability flags in <code>ucred</code> structure.
     */
    public long getSceCaps1() {
        return ptr.read8(OFFSET_CR_SCE_CAPS1);
    }

    /**
     * Set 1st set of capability flags
     *
     * @param caps1 New caps value.
     */
    public void setSceCaps1(long caps1) {
        ptr.write8(OFFSET_CR_SCE_CAPS1, caps1);
    }

    /**
     * Capability flags (2nd set)
     *
     * @return Returns the PS5-specific capability flags in <code>ucred</code> structure.
     */
    public long getSceCaps2() {
        return ptr.read8(OFFSET_CR_SCE_CAPS2);
    }

    /**
     * Set 2nd set of capability flags
     *
     * @param caps2 New caps value.
     */
    public void setSceCaps2(long caps2) {
        ptr.write8(OFFSET_CR_SCE_CAPS2, caps2);
    }

    /**
     * PS5-specific attributes
     *
     * @return Returns the PS5-specific attributes in <code>ucred</code> structure.
     */
    public byte[] getSceAttrs() {
        byte[] attrs = new byte[4];
        ptr.read(OFFSET_CR_SCE_ATTRS, attrs, 0, attrs.length);
        return attrs;
    }

    /**
     * Set PS5-specific attributes
     *
     * @param attrs New attrs value.
     */
    public void setSceAttrs(byte[] attrs) {
        ptr.write(OFFSET_CR_SCE_ATTRS, attrs, 0, 4);
    }

    /**
     * Groups (normally points to the address of the first element in small groups array).
     *
     * @return Returns the value of <code>cr_groups</code> field of <code>ucred</code> structure.
     */
    public KernelPointer getGroups() {
        return ptr.pptr(OFFSET_CR_GROUPS);
    }

    /**
     * Available groups (number of possible entries in small groups array).
     *
     * @return Returns the value of <code>cr_agroups</code> field of <code>ucred</code> structure.
     */
    public int getAvailableGroups() {
        return ptr.read4(OFFSET_CR_AGROUPS);
    }

    /**
     * Get group id at <code>index</code>.
     *
     * @return Returns the value of the element at <code>index</code> in
     *   the array <code>cr_smallgroups</code> of <code>ucred</code> structure.
     * @throws IndexOutOfBoundsException If index is invalid.
     */
    public int getSmallGroup(int index) {
        if ((index < 0) || (index > getAvailableGroups())) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }

        return ptr.read4(OFFSET_CR_SMALL_GROUPS + index * 4L);
    }

    /**
     * Set group id at <code>index</code>.
     *
     * @param index Index of the group.
     * @param gid New group id value.
     * @throws IndexOutOfBoundsException If index is invalid.
     */
    public int setSmallGroup(int index, int gid) {
        if ((index < 0) || (index > getAvailableGroups())) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }

        return ptr.read4(OFFSET_CR_SMALL_GROUPS + index * 4L);
    }

    /**
     * Gets the native memory pointer where this UCred's data is stored.
     *
     * @return UCred memory pointer.
     */
    public KernelPointer getPointer() {
        return this.ptr;
    }
}
