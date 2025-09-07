package org.ps5jb.sdk.include.sys.proc;

import java.nio.charset.Charset;

import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.Param;
import org.ps5jb.sdk.include.sys.mutex.MutexType;
import org.ps5jb.sdk.include.sys.ucred.UCred;

/**
 * Incomplete wrapper for FreeBSD <code>thread</code> structure.
 */
public class Thread {
    public static final long OFFSET_TD_MTX = 0L;
    public static final long OFFSET_TD_PROC = OFFSET_TD_MTX + 8L;
    public static final long OFFSET_TD_PLIST_TQE_NEXT = OFFSET_TD_PROC + 8L;
    public static final long OFFSET_TD_PLIST_TQE_PREV = OFFSET_TD_PLIST_TQE_NEXT + 8L;
    public static final long OFFSET_TD_TID = 156L;
    public static final long OFFSET_TD_UCRED = 320L;
    public static final long OFFSET_TD_NAME = 660L;
    public static final long OFFSET_TD_KSTACK_OBJ = 1128L;
    public static final long OFFSET_TD_KSTACK = OFFSET_TD_KSTACK_OBJ + 8L;
    public static final long OFFSET_TD_KSTACK_PAGES = OFFSET_TD_KSTACK + 8L;

    private final KernelPointer ptr;
    private MutexType mtx;
    private UCred ucred;

    /**
     * Process constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing Thread data.
     */
    public Thread(KernelPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Next thread in process.
     *
     * @return Returns the Thread pointed to by the value of
     *   <code>td_plist.tqe_next</code> field of <code>thread</code> structure.
     *   If <code>tqe_next</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Thread getNextThread() {
        KernelPointer thNext = this.ptr.pptr(OFFSET_TD_PLIST_TQE_NEXT);
        if (KernelPointer.NULL.equals(thNext)) {
            return null;
        }
        return new Thread(thNext);
    }

    /**
     * Thread mutex (replaces sched lock).
     *
     * @return Returns the value of <code>td_mtx</code> field of <code>thread</code> structure.
     */
    public MutexType getMtx() {
        if (mtx == null) {
            mtx = new MutexType(this.ptr.pptr(OFFSET_TD_MTX, new Long(MutexType.SIZE)));
        }
        return mtx;
    }

    /**
     * Thread identifier.
     *
     * @return Returns the value of <code>td_tid</code> field of <code>thread</code> structure.
     */
    public int getTid() {
        return ptr.read4(OFFSET_TD_TID);
    }

    /**
     * Thread name.
     *
     * @return Returns the value of <code>td_name</code> field of <code>thread</code> structure.
     */
    public String getName() {
        return ptr.readString(OFFSET_TD_NAME, new Integer(Param.MAXCOMLEN + 1), Charset.defaultCharset().name());
    }

    /**
     * Reference to credentials.
     *
     * @return Returns the wrapper over the value of <code>td_ucred</code> field of <code>thread</code> structure.
     */
    public UCred getUserCredentials() {
        if (ucred == null) {
            ucred = new UCred(this.ptr.pptr(OFFSET_TD_UCRED));
        }
        return ucred;
    }

    /**
     * Kstack object.
     *
     * @return Returns the value of <code>td_kstack_obj</code> field of <code>thread</code> structure.
     */
    public KernelPointer getKstackObj() {
        return this.ptr.pptr(OFFSET_TD_KSTACK_OBJ);
    }

    /**
     * Kernel VA of kstack.
     *
     * @return Returns the value of <code>td_kstack</code> field of <code>thread</code> structure.
     */
    public KernelPointer getKstack() {
        return this.ptr.pptr(OFFSET_TD_KSTACK);
    }

    /**
     * Size of the kstack.
     *
     * @return Returns the value of <code>td_kstack_pages</code> field of <code>thread</code> structure.
     */
    public int getKstackPages() {
        return ptr.read4(OFFSET_TD_KSTACK_PAGES);
    }

    /**
     * Gets the native memory pointer where this Thread's data is stored.
     *
     * @return Thread memory pointer.
     */
    public KernelPointer getPointer() {
        return this.ptr;
    }
}
