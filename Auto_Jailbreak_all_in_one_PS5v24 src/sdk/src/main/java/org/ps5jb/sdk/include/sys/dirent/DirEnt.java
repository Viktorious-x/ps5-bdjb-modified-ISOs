package org.ps5jb.sdk.include.sys.dirent;

import java.nio.charset.Charset;

import org.ps5jb.sdk.core.Pointer;

/**
 * Wrapper for FreeBSD <code>dirent</code> structure. This class is not thread-safe.
 */
public class DirEnt {
    private final Pointer ptr;
    private String name = null;

    /**
     * DirEnt constructor.
     *
     * @param pointer Native memory where DirEnt is stored.
     */
    public DirEnt(Pointer pointer) {
        this.ptr = pointer;
    }

    /**
     * Gets the native memory pointer where this DirEnt data is stored.
     *
     * @return Dirent memory pointer.
     */
    public Pointer getPointer() {
        return this.ptr;
    }

    /**
     * If the current pointer is one of the elements in the DirEnt array, return the
     * next element of this array.
     *
     * @param remainingSize Remaining size in the buffer allocated to hold the DirEnt array.
     * @return Next element of the DirEnt array. If the next element is beyond the remaining
     *   size, null is returned.
     */
    public DirEnt next(int remainingSize) {
        long reclen = this.ptr.read2(4) & 0xFFFF;

        DirEnt result;
        if (reclen >= 8 && reclen <= remainingSize) {
            result = new DirEnt(this.ptr.inc(reclen));
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Get a number which is unique for each distinct file in the file system.
     * Files that are linked by hard links have the same number.
     *
     * @return Entry file number.
     */
    public int getFileNo() {
        return this.ptr.read4();
    }

    /**
     * Get the type of the file pointed to by this directory record.
     *
     * @return File type of this directory entry.
     */
    public DirType getDirType() {
        return DirType.valueOf(this.ptr.read1(6));
    }

    /**
     * Get the file name.
     *
     * @return File name of this directory entry.
     */
    public String getName() {
        if (this.name == null) {
            this.name = this.ptr.readString(8, new Integer(this.ptr.read1(7)), Charset.defaultCharset().name());
        }
        return this.name;
    }
}
