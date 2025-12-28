package org.ps5jb.sdk.include.sys.proc;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.VmParam;
import org.ps5jb.sdk.include.sys.Param;
import org.ps5jb.sdk.include.sys.SysLimits;
import org.ps5jb.sdk.include.sys.filedesc.FileDesc;
import org.ps5jb.sdk.include.sys.mutex.MutexType;
import org.ps5jb.sdk.include.sys.ucred.UCred;
import org.ps5jb.sdk.include.vm.map.VmSpace;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Incomplete wrapper for FreeBSD <code>proc</code> structure.
 */
public class Process {
    public static final String BDJ_PROCESS_P_TITLE_ID = "NPXS40140";
    public static final String BDJ_PROCESS_P_COMM = "bdj.elf";
    public static final String BDJ_PROCESS_P_SERVICE_LABEL = "IV9999";

    public static final long OFFSET_P_LIST_LE_NEXT = 0L;
    public static final long OFFSET_P_LIST_LE_PREV = OFFSET_P_LIST_LE_NEXT + 8L;
    public static final long OFFSET_P_THREADS_TQH_FIRST = OFFSET_P_LIST_LE_PREV + 8L;
    public static final long OFFSET_P_THREADS_TQH_LAST = OFFSET_P_THREADS_TQH_FIRST + 8L;
    public static final long OFFSET_P_SLOCK = OFFSET_P_THREADS_TQH_LAST + 8L;
    public static final long OFFSET_P_UCRED = OFFSET_P_SLOCK + MutexType.SIZE;
    public static final long OFFSET_P_FD = OFFSET_P_UCRED + 8L;
    public static final long OFFSET_P_PID = 188L;
    public static final long OFFSET_P_PGLIST_LE_NEXT = 208L;
    public static final long OFFSET_P_PGLIST_LE_PREV = OFFSET_P_PGLIST_LE_NEXT + 8L;
    public static final long OFFSET_P_PPTR = OFFSET_P_PGLIST_LE_PREV + 8L;
    public static final long OFFSET_P_SIBLING_LE_NEXT = OFFSET_P_PPTR + 8L;
    public static final long OFFSET_P_SIBLING_LE_PREV = OFFSET_P_SIBLING_LE_NEXT + 8L;
    public static final long OFFSET_P_CHILDREN = OFFSET_P_SIBLING_LE_PREV + 8L;
    public static final long OFFSET_P_REAPER = OFFSET_P_CHILDREN + 8L;
    public static final long OFFSET_P_REAPLIST = OFFSET_P_REAPER + 8L;
    public static final long OFFSET_P_REAPSIBLING_LE_NEXT = OFFSET_P_REAPLIST + 8L;
    public static final long OFFSET_P_REAPSIBLING_LE_PREV = OFFSET_P_REAPSIBLING_LE_NEXT + 8L;
    public static final long OFFSET_P_VM_SPACE = 512L;
    public static final long OFFSET_P_DYNLIB = 1000L;
    public static long OFFSET_P_TITLE_ID = -1;
    public static long OFFSET_P_CONTENT_ID = -1;
    public static long OFFSET_P_COMM = -1;
    public static long OFFSET_P_PATHNAME = -1;
    public static long OFFSET_P_SYSENT = -1;
    public static long OFFSET_P_ARGS = -1;

    private final KernelPointer ptr;
    private MutexType slock;
    private VmSpace vmSpace;
    private UCred ucred;
    private FileDesc fd;
    private KernelPointer allProc;

    /**
     * Process constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing Process data.
     */
    public Process(KernelPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Previous process in the kernel process tree.
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_list.le_prev</code> field of <code>proc</code> structure.
     *   If <code>le_prev</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getPreviousProcess() {
        final long MASK = VmParam.VM_MIN_KERNEL_ADDRESS;

        KernelPointer prevPtr = this.ptr.pptr(OFFSET_P_LIST_LE_PREV);
        if (KernelPointer.NULL.equals(prevPtr) || (prevPtr.addr() & MASK) == MASK) {
            return null;
        }

        return new Process(prevPtr.inc(-OFFSET_P_LIST_LE_NEXT));
    }

    /**
     * Next process in the kernel process tree.
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_list.le_next</code> field of <code>proc</code> structure.
     *   If <code>le_next</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getNextProcess() {
        KernelPointer next = this.ptr.pptr(OFFSET_P_LIST_LE_NEXT);
        if (KernelPointer.NULL.equals(next)) {
            return null;
        }
        return new Process(next);
    }

    /**
     * Parent process.
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_pptr</code> field of <code>proc</code> structure.
     *   If <code>p_pptr</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getParentProcess() {
        KernelPointer pptr = this.ptr.pptr(OFFSET_P_PPTR);
        if (KernelPointer.NULL.equals(pptr)) {
            return null;
        }
        return new Process(pptr);
    }

    /**
     * Next child process.
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_children.lh_first</code> field of <code>proc</code> structure.
     *   If <code>lh_first</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getNextChildProcess() {
        KernelPointer nextChild = this.ptr.pptr(OFFSET_P_CHILDREN);
        if (KernelPointer.NULL.equals(nextChild)) {
            return null;
        }
        return new Process(nextChild);
    }

    /**
     * Next sibling process.
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_sibling.le_next</code> field of <code>proc</code> structure.
     *   If <code>le_next</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getNextSiblingProcess() {
        KernelPointer next = this.ptr.pptr(OFFSET_P_SIBLING_LE_NEXT);
        if (KernelPointer.NULL.equals(next)) {
            return null;
        }
        return new Process(next);
    }

    /**
     * Previous sibling process.
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_sibling.le_prev</code> field of <code>proc</code> structure.
     *   If <code>le_prev</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getPreviousSiblingProcess() {
        KernelPointer prevPtr = this.ptr.pptr(OFFSET_P_SIBLING_LE_PREV);
        if (KernelPointer.NULL.equals(prevPtr)) {
            return null;
        }

        Process parent = getParentProcess();
        if (parent == null || (prevPtr.addr() == (parent.ptr.addr() + OFFSET_P_CHILDREN))) {
            return null;
        }

        return new Process(prevPtr.inc(-OFFSET_P_SIBLING_LE_NEXT));
    }

    /**
     * Reaper process.
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_reaper</code> field of <code>proc</code> structure.
     */
    public Process getReaperProcess() {
        KernelPointer next = this.ptr.pptr(OFFSET_P_REAPER);
        if (KernelPointer.NULL.equals(next)) {
            return null;
        }
        return new Process(next);
    }

    /**
     * Next process in reap list (my descendants, if I am reaper).
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_reaplist.le_next</code> field of <code>proc</code> structure.
     *   If <code>le_next</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getNextProcessInReapList() {
        KernelPointer next = this.ptr.pptr(OFFSET_P_REAPLIST);
        if (KernelPointer.NULL.equals(next)) {
            return null;
        }
        return new Process(next);
    }

    /**
     * Next reap list sibling process (descendant of the same reaper).
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_reapsibling.le_next</code> field of <code>proc</code> structure.
     *   If <code>le_next</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getNextReapListSiblingProcess() {
        KernelPointer next = this.ptr.pptr(OFFSET_P_REAPSIBLING_LE_NEXT);
        if (KernelPointer.NULL.equals(next)) {
            return null;
        }
        return new Process(next);
    }

    /**
     * Previous reap sibling process (descendant of the same reaper).
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_reapsibling.le_prev</code> field of <code>proc</code> structure.
     *   If <code>le_prev</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getPreviousReapListSiblingProcess() {
        KernelPointer prevPtr = this.ptr.pptr(OFFSET_P_REAPSIBLING_LE_PREV);
        if (KernelPointer.NULL.equals(prevPtr)) {
            return null;
        }

        Process reaper = getReaperProcess();
        if (reaper == null || (prevPtr.addr() == (reaper.ptr.addr() + OFFSET_P_REAPLIST))) {
            return null;
        }

        return new Process(prevPtr.inc(-OFFSET_P_REAPSIBLING_LE_NEXT));
    }

    /**
     * Next process in process group.
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_pglist.le_next</code> field of <code>proc</code> structure.
     *   If <code>le_next</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getNextProcessInGroup() {
        KernelPointer pgNext = this.ptr.pptr(OFFSET_P_PGLIST_LE_NEXT);
        if (KernelPointer.NULL.equals(pgNext)) {
            return null;
        }
        return new Process(pgNext);
    }

    /**
     * First process thread.
     *
     * @return Returns the Thread pointed to by the value of
     *   <code>p_threads.tqh_first</code> field of <code>proc</code> structure.
     */
    public Thread getFirstThread() {
        return new Thread(this.ptr.pptr(OFFSET_P_THREADS_TQH_FIRST));
    }

    /**
     * Process spin lock.
     *
     * @return Returns the value of <code>p_slock</code> field of <code>proc</code> structure.
     */
    public MutexType getSpinLock() {
        if (slock == null) {
            slock = new MutexType(this.ptr.pptr(OFFSET_P_SLOCK, new Long(MutexType.SIZE)));
        }
        return slock;
    }

    /**
     * Process owner's identity.
     *
     * @return Returns the value of <code>p_ucred</code> field of <code>proc</code> structure.
     * @deprecated Prefer {@link #getUserCredentials()}
     */
    @Deprecated
    public KernelPointer getUCred() {
        return this.ptr.pptr(OFFSET_P_UCRED);
    }

    /**
     * Process owner's identity.
     *
     * @return Returns the wrapper over the value of <code>p_ucred</code> field of <code>proc</code> structure.
     */
    public UCred getUserCredentials() {
        if (ucred == null) {
            ucred = new UCred(this.ptr.pptr(OFFSET_P_UCRED));
        }
        return ucred;
    }

    /**
     * Open files.
     *
     * @return Returns the value of <code>p_fd</code> field of <code>proc</code> structure.
     * @deprecated Prefer {@link #getOpenFiles()}
     */
    @Deprecated
    public KernelPointer getFd() {
        return this.ptr.pptr(OFFSET_P_FD);
    }

    /**
     * Open files.
     *
     * @return Returns the wrapper over the value of <code>p_fd</code> field of <code>proc</code> structure.
     */
    public FileDesc getOpenFiles() {
        if (fd == null) {
            fd = new FileDesc(this.ptr.pptr(OFFSET_P_FD));
        }
        return fd;
    }

    /**
     * Process dynamic library info (PS5-specific structure).
     *
     * @return Returns the pointer to dynamic library info structure of the process.
     */
    public KernelPointer getDynLib() {
        return this.ptr.pptr(OFFSET_P_DYNLIB);
    }

    /**
     * Process identifier.
     *
     * @return Returns the value of <code>p_pid</code> field of <code>proc</code> structure.
     */
    public int getPid() {
        return ptr.read4(OFFSET_P_PID);
    }

    /**
     * Address space.
     *
     * @return Returns the value of <code>p_vmspace</code> field of <code>proc</code> structure.
     */
    public VmSpace getVmSpace() {
        if (vmSpace == null) {
            vmSpace = new VmSpace(this.ptr.pptr(OFFSET_P_VM_SPACE));
        }
        return vmSpace;
    }

    /**
     * Process name.
     *
     * @return Returns the value of <code>p_comm</code> field of <code>proc</code> structure.
     */
    public String getName() {
        detectOffsets();
        return ptr.readString(OFFSET_P_COMM, new Integer(Param.MAXCOMLEN + 1), Charset.defaultCharset().name());
    }

    /**
     * Title ID.
     *
     * @return Returns the title ID stored in <code>proc</code> structure.
     */
    public String getTitleId() {
        detectOffsets();
        return ptr.readString(OFFSET_P_TITLE_ID, new Integer(10), Charset.defaultCharset().name());
    }

    /**
     * Content ID.
     *
     * @return Returns the content ID stored in <code>proc</code> structure.
     */
    public String getContentId() {
        detectOffsets();
        return ptr.readString(OFFSET_P_CONTENT_ID, new Integer(60), Charset.defaultCharset().name());
    }

    /**
     * Path name.
     *
     * @return Returns the path name to the process executable stored in <code>proc</code> structure.
     */
    public String getPathName() {
        detectOffsets();
        return ptr.readString(OFFSET_P_PATHNAME, new Integer(SysLimits.PATH_MAX), Charset.defaultCharset().name());
    }

    /**
     * Process arguments.
     *
     * @return Returns the value of <code>p_args</code> field of <code>proc</code> structure.
     */
    public List getArguments() {
        detectOffsets();

        List result = new ArrayList();
        KernelPointer args = this.ptr.pptr(OFFSET_P_ARGS);
        if (!KernelPointer.NULL.equals(args)) {
            int count = args.read4(4);
            int lastPos = 0;
            byte[] buf = new byte[count];
            args.read(8, buf, 0, count);

            for (int i = 0; i < buf.length; ++i) {
                if (buf[i] == 0) {
                    try {
                        String arg = new String(buf, lastPos, i - lastPos, Charset.defaultCharset().name());
                        result.add(arg);

                        lastPos = i + 1;
                    } catch (UnsupportedEncodingException e) {
                        throw new SdkRuntimeException(e);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns the pointer to kernel allproc variable which can be used to
     * iterate over all the processes.
     *
     * @return AllProc kernel address.
     */
    public KernelPointer getAllProc() {
        if (allProc == null) {
            final long MASK = VmParam.VM_MIN_KERNEL_ADDRESS;

            allProc = this.ptr.pptr(OFFSET_P_LIST_LE_PREV);
            while (!KernelPointer.NULL.equals(allProc) && ((allProc.addr() & MASK) != MASK)) {
                allProc = allProc.pptr(OFFSET_P_LIST_LE_PREV);
            }
        }
        return allProc;
    }

    /**
     * Gets the native memory pointer where this Process's data is stored.
     *
     * @return Process memory pointer.
     */
    public KernelPointer getPointer() {
        return this.ptr;
    }

    /**
     * Offsets in this structure are firmware dependent. This method does a one-time detection of this value.
     */
    private void detectOffsets() {
        if (OFFSET_P_COMM == -1) {
            synchronized (Process.class) {
                if (OFFSET_P_COMM != -1) {
                    return;
                }

                Process bdjProcess = this;
                boolean rewind = true;

                while (OFFSET_P_COMM == -1 && bdjProcess != null) {
                    if (OFFSET_P_TITLE_ID == -1) {
                        OFFSET_P_TITLE_ID = scanStringOffset(bdjProcess, 0x438, 0x38, BDJ_PROCESS_P_TITLE_ID);
                    }

                    if (OFFSET_P_TITLE_ID != -1) {
                        if (OFFSET_P_CONTENT_ID == -1) {
                            OFFSET_P_CONTENT_ID = scanStringOffset(bdjProcess, OFFSET_P_TITLE_ID + 0x54, 0x30, BDJ_PROCESS_P_SERVICE_LABEL);
                        }

                        if (OFFSET_P_CONTENT_ID != -1) {
                            OFFSET_P_COMM = scanStringOffset(bdjProcess, OFFSET_P_CONTENT_ID + 0xD8, 0x8, BDJ_PROCESS_P_COMM);
                        }
                    }

                    if (OFFSET_P_COMM == -1) {
                        if (rewind) {
                            // Backtrack to allproc and start the search
                            bdjProcess = new Process(getAllProc().pptr(0));
                            // If current process is already the first one, start scan on the next one
                            if (bdjProcess.getPid() == this.getPid()) {
                                bdjProcess = bdjProcess.getNextProcess();
                            }
                            rewind = false;
                        } else {
                            bdjProcess = bdjProcess.getNextProcess();
                        }
                    }
                }

                if (OFFSET_P_COMM == -1) {
                    throw new SdkSoftwareVersionUnsupportedException(ErrorMessages.getClassErrorMessage(Process.class,"offsetsUnknown"));
                }

                OFFSET_P_PATHNAME = OFFSET_P_COMM + 0x20;
                OFFSET_P_SYSENT = OFFSET_P_PATHNAME + SysLimits.PATH_MAX + 4;
                OFFSET_P_ARGS = OFFSET_P_SYSENT + 8;
            }
        }
    }

    /**
     * Scans up to <code>scanSize</code> bytes from <code>scanStart</code> offset relative
     * to structure start and tries to find a string that matches <code>scanMatch</code>.
     *
     * @param proc Process to scan in.
     * @param scanStart Scan start offset.
     * @param scanSize Length in bytes to scan (inclusive).
     * @param scanMatch String to match. The string can be partial, does not have to be null terminated.
     * @return Offset from structure start where match was found or -1 if there is no match.
     */
    private long scanStringOffset(Process proc, long scanStart, long scanSize, String scanMatch) {
        long result = -1;

        final Integer scanMatchSize = new Integer(scanMatch.length());
        for (long i = 0; i <= scanSize; i += 4) {
            long scanOffset = scanStart + i;
            String scanVal = proc.ptr.readString(scanOffset, scanMatchSize, Charset.defaultCharset().name());

            if (scanMatch.equals(scanVal)) {
                result = scanOffset;
                break;
            }
        }

        return result;
    }
}
