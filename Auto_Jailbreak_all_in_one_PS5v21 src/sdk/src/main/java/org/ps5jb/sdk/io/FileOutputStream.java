package org.ps5jb.sdk.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.ErrNo;
import org.ps5jb.sdk.include.sys.FCntl;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.lib.LibKernel;
import org.ps5jb.sdk.res.ErrorMessages;

public class FileOutputStream extends java.io.FileOutputStream {
    public FileOutputStream(String name) throws FileNotFoundException {
        this(name, false);
    }

    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        super(new FileDescriptor());
        disableProxies();
        openFile(new java.io.File(name), append);
    }

    public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }

    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        super(new FileDescriptor());
        disableProxies();
        openFile(file, append);
    }

    public FileOutputStream(FileDescriptor fileDescriptor) {
        super(fileDescriptor);
        disableProxies();
    }

    private void disableProxies() {
        try {
            Field proxyField = java.io.FileOutputStream.class.getDeclaredField("proxy");
            proxyField.setAccessible(true);
            proxyField.set(this, null);
        } catch (NoSuchFieldException e) {
            // Ignore
        } catch (IllegalAccessException e) {
            throw new SdkRuntimeException(e);
        }

        FileDescriptor fd = getFd();
        FileDescriptorFactory.disableFileDescriptorProxy(fd);
    }

    public FileDescriptor getFd() {
        try {
            Field fdField = java.io.FileOutputStream.class.getDeclaredField("fd");
            fdField.setAccessible(true);
            return (FileDescriptor) fdField.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new SdkRuntimeException(e);
        }
    }

    private void openFile(java.io.File file, boolean append) throws FileNotFoundException {
        LibKernel libKernel = new LibKernel();
        FCntl fcntl = new FCntl(libKernel);
        ErrNo errNo = new ErrNo(libKernel);
        try {
            List openFlagList = new ArrayList(2);
            openFlagList.add(OpenFlag.O_WRONLY);
            if (append) {
                openFlagList.add(OpenFlag.O_APPEND);
            } else {
                openFlagList.add(OpenFlag.O_TRUNC);
            }
            OpenFlag[] openFlags = (OpenFlag[]) openFlagList.toArray(new OpenFlag[openFlagList.size()]);
            FileDescriptorFactory.openFile(fcntl, getFd(), file.getAbsolutePath(), openFlags);
        } catch (SdkException e) {
            FileNotFoundException ex = new FileNotFoundException(ErrorMessages.getClassErrorMessage(getClass(),
                    "fileOpenException", file.getAbsolutePath(), errNo.getLastError()));
            ex.initCause(e);
            throw ex;
        } finally {
            libKernel.closeLibrary();
        }
    }
}
