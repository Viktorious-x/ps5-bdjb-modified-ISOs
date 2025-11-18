package org.ps5jb.sdk.include.sys.filedesc;

import org.ps5jb.sdk.core.kernel.KernelPointer;

/**
 * Incomplete wrapper for FreeBSD <code>filedesc</code> structure.
 */
public class FileDesc {
    public static final long OFFSET_FD_FILES = 0L;
    public static final long OFFSET_FD_CDIR = OFFSET_FD_FILES + 8L;
    public static final long OFFSET_FD_RDIR = OFFSET_FD_CDIR + 8L;
    public static final long OFFSET_FD_JDIR = OFFSET_FD_RDIR + 8L;

    private final KernelPointer ptr;

    /**
     * Process constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing FileDesc data.
     */
    public FileDesc(KernelPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Open files table.
     *
     * @return Returns the value of <code>fd_files</code> field of <code>filedesc</code> structure.
     */
    public KernelPointer getFiles() {
        return ptr.pptr(OFFSET_FD_FILES);
    }

    /**
     * Current directory vnode.
     *
     * @return Returns the value of <code>fd_cdir</code> field of <code>filedesc</code> structure.
     */
    public KernelPointer getCurDir() {
        return ptr.pptr(OFFSET_FD_CDIR);
    }

    /**
     * Set new current directory vnode.
     *
     * @param curDir New current directory value.
     */
    public void setCurDir(KernelPointer curDir) {
        ptr.write8(OFFSET_FD_CDIR, curDir.addr());
    }

    /**
     * Root directory vnode.
     *
     * @return Returns the value of <code>fd_rdir</code> field of <code>filedesc</code> structure.
     */
    public KernelPointer getRootDir() {
        return ptr.pptr(OFFSET_FD_CDIR);
    }

    /**
     * Set new root directory vnode.
     *
     * @param rootDir New rootDir value.
     */
    public void setRootDir(KernelPointer rootDir) {
        ptr.write8(OFFSET_FD_RDIR, rootDir.addr());
    }

    /**
     * Jail root directory vnode.
     *
     * @return Returns the value of <code>fd_jdir</code> field of <code>filedesc</code> structure.
     */
    public KernelPointer getJailDir() {
        return ptr.pptr(OFFSET_FD_JDIR);
    }

    /**
     * Set new jail root directory vnode.
     *
     * @param jailDir New jailDir value.
     */
    public void setJailDir(KernelPointer jailDir) {
        ptr.write8(OFFSET_FD_JDIR, jailDir.addr());
    }

    /**
     * Gets the native memory pointer where this FileDesc's data is stored.
     *
     * @return FileDesc memory pointer.
     */
    public KernelPointer getPointer() {
        return this.ptr;
    }
}
