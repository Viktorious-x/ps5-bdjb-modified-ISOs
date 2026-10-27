package org.ps5jb.sdk.io;

import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.sys.Stat;
import org.ps5jb.sdk.include.sys.dirent.DirEnt;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.include.sys.stat.FileStatusMode;
import org.ps5jb.sdk.include.sys.stat.StatType;
import org.ps5jb.sdk.lib.LibKernel;

public class File extends java.io.File {
    private static final long serialVersionUID = -34161568608618887L;

    private boolean reacheable;
    private long size;
    private Set modes;
    private long modificationTime;
    private int uid;
    private int gid;
    private int blockSize;

    public File(java.io.File parent, String child) {
        super(parent, child);
        disableProxy();
        refresh();
    }

    public File(String parent, String child) {
        super(parent, child);
        disableProxy();
        refresh();
    }

    public File(String pathname) {
        super(pathname);
        disableProxy();
        refresh();
    }

    private void disableProxy() {
        try {
            Field proxyField = java.io.File.class.getDeclaredField("proxy");
            proxyField.setAccessible(true);
            proxyField.set(this, null);
        } catch (NoSuchFieldException e) {
            // Ignore
        } catch (IllegalAccessException e) {
            throw new SdkRuntimeException(e);
        }
    }

    public void refresh() {
        LibKernel libKernel = new LibKernel();
        try {
            if (libKernel.sceKernelCheckReachability(this.getAbsolutePath()) == 0) {
                reacheable = true;

                Stat stat = new Stat(libKernel);

                try {
                    StatType statType = stat.getStatus(getAbsolutePath());
                    try {
                        size = statType.getSize();
                        modes = new HashSet(Arrays.asList(statType.getMode()));
                        modificationTime = statType.getMtim().getSec() * 1000L + statType.getMtim().getNsec() / 1000000L;
                        uid = statType.getUid();
                        gid = statType.getGid();
                        blockSize = statType.getBlkSize();
                    } finally {
                        statType.free();
                    }
                } catch (SdkException e) {
                    Status.printStackTrace(e.getMessage(), e);
                    reacheable = false;
                }
            }
        } finally {
            libKernel.closeLibrary();
        }
    }

    public int getOwner() {
        return uid;
    }

    public int getGroup() {
        return gid;
    }

    public Set getModes() {
        return new HashSet(modes);
    }

    @Override
    public long lastModified() {
        return modificationTime;
    }

    @Override
    public boolean exists() {
        return reacheable;
    }

    @Override
    public long length() {
        return size;
    }

    @Override
    public boolean isDirectory() {
        if (modes != null) {
            return modes.contains(FileStatusMode.S_IFDIR);
        }

        return super.isDirectory();
    }

    public boolean isDevice() {
        if (modes != null) {
            return modes.contains(FileStatusMode.S_IFCHR) || modes.contains(FileStatusMode.S_IFBLK);
        }
        return false;
    }

    @Override
    public boolean isFile() {
        if (modes != null) {
            return modes.contains(FileStatusMode.S_IFREG);
        }

        return super.isFile();
    }

    @Override
    public File getParentFile() {
        String parent = getParent();
        return parent == null ? null : new File(parent);
    }

    @Override
    public File getAbsoluteFile() {
        String absolutePath = getAbsolutePath();
        return new File(absolutePath);
    }

    @Override
    public File getCanonicalFile() throws IOException {
        String canonicalPath = getCanonicalPath();
        return new File(canonicalPath);
    }

    @Override
    public String[] list() {
        String[] result = null;

        int BUF_SIZE = Math.max(16 * 1024, blockSize);
        Pointer db = Pointer.malloc(BUF_SIZE);
        try {
            LibKernel libKernel = new LibKernel();
            try {
                int fd = libKernel.open(getAbsolutePath(), OpenFlag.or(OpenFlag.O_RDONLY, OpenFlag.O_DIRECTORY));
                if (fd != -1) {
                    List dirEnts = new ArrayList();
                    try {
                        DirEnt dirEnt = null;
                        int remainingSize = libKernel.getdents(fd, db, BUF_SIZE);
                        if (remainingSize != -1) {
                            while (remainingSize > 0 && remainingSize <= BUF_SIZE) {
                                if (dirEnt == null) {
                                    dirEnt = new DirEnt(db);
                                }

                                String dirEntName = dirEnt.getName();
                                if (!dirEntName.equals(".") && !dirEntName.equals("..") && !dirEntName.equals("")) {
                                    dirEnts.add(dirEntName);
                                }

                                long oldAddr = dirEnt.getPointer().addr();
                                dirEnt = dirEnt.next(remainingSize);
                                if (dirEnt == null) {
                                    remainingSize = libKernel.getdents(fd, db, BUF_SIZE);
                                } else {
                                    remainingSize -= (int) (dirEnt.getPointer().addr() - oldAddr);
                                }
                            }
                        }
                    } finally {
                        libKernel.close(fd);
                    }
                    result = (String[]) dirEnts.toArray(new String[0]);
                }
            } finally {
                libKernel.closeLibrary();
            }
        } finally {
            db.free();
        }

        return result;
    }

    @Override
    public java.io.File[] listFiles() {
        String[] fileList = list();
        if (fileList == null) {
            return null;
        }

        File[] result = new File[fileList.length];
        for (int i = 0; i < fileList.length; i++) {
            result[i] = new File(this, fileList[i]);
        }
        return result;
    }

    @Override
    public java.io.File[] listFiles(FilenameFilter filter) {
        String[] fileList = list(filter);
        if (fileList == null) {
            return null;
        }

        File[] result = new File[fileList.length];
        for (int i = 0; i < fileList.length; i++) {
            result[i] = new File(this, fileList[i]);
        }
        return result;
    }

    @Override
    public java.io.File[] listFiles(FileFilter filter) {
        String[] fileList = list();
        if (fileList == null) {
            return null;
        }

        List result = new ArrayList();
        for (int i = 0; i < fileList.length; i++) {
            File file = new File(this, fileList[i]);
            if (filter == null || filter.accept(file)) {
                result.add(file);
            }
        }

        return (java.io.File[]) result.toArray(new File[0]);
    }
}
