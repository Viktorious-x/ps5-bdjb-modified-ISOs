package org.ps5jb.sdk.io;

import java.io.FileDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;
import java.util.Arrays;

import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.sys.FCntl;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.lib.LibKernel;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Factory to create instances of {@link FileDescriptor} classes and
 * call its protected methods
 */
public class FileDescriptorFactory {
    /**
     * Private constructor. All methods in this class are static.
     */
    private FileDescriptorFactory() {
    }

    /**
     * Create a new instance of FileDescriptor with a given fd value.
     *
     * @param fd File descriptor number from a native call.
     * @return FileDescriptor instance.
     * @throws SdkRuntimeException If an error occurs while creating the FileDescriptor instance.
     */
    public static FileDescriptor createFileDescriptor(int fd) {
        try {
            Constructor constr = FileDescriptor.class.getDeclaredConstructor(new Class[] { int.class });
            constr.setAccessible(true);
            FileDescriptor result = (FileDescriptor) constr.newInstance(new Object[] { new Integer(fd) });
            disableFileDescriptorProxy(result);
            return result;
        } catch (InvocationTargetException e) {
            throw new SdkRuntimeException(e.getTargetException().getMessage(), e.getTargetException());
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Uses native call to open a file and return a file descriptor for it.
     * The caller is responsible for closing the opened file descriptor.
     *
     * @param path File to open
     * @param openFlags Flags to use to open the file.
     * @return Non-proxied FileDescriptor instance.
     * @throws SdkException One of the subclasses of this exception will be
     *   thrown depending on the error code. See {@link FCntl#open(String, OpenFlag...)}.
     */
    public static FileDescriptor openFile(String path, OpenFlag ... openFlags) throws SdkException {
        LibKernel libKernel = new LibKernel();
        FCntl fcntl = new FCntl(libKernel);
        try {
            FileDescriptor fd = new FileDescriptor();
            return openFile(fcntl, fd, path, openFlags);
        } finally {
            libKernel.closeLibrary();
        }
    }

    /**
     * Uses native call to open a file and return a file descriptor for it.
     * This variant of <code>openFile</code> uses an existing FCntl instance
     * and a pre-created empty FileDescriptor instance rather
     * than allocating a new ones. The caller is responsible
     * for closing the file descriptor.
     *
     * @param fcntl Instance of FCntl to use for open call.
     * @param fileDescriptor Preallocated file descriptor instance.
     * @param path File to open
     * @param openFlags Flags to use to open the file.
     * @return Same <code>fileDescriptor</code> instance, initialized with the opened file.
     * @throws SdkException One of the subclasses of this exception will be
     *   thrown depending on the error code. See {@link FCntl#open(String, OpenFlag...)}.
     */
    public static FileDescriptor openFile(FCntl fcntl, FileDescriptor fileDescriptor,
                                          String path, OpenFlag ... openFlags)
            throws SdkException {

        int fd = fcntl.open(path, openFlags);

        try {
            Method setMethod = java.io.FileDescriptor.class.getDeclaredMethod("set", new Class[]{int.class});
            setMethod.setAccessible(true);
            setMethod.invoke(fileDescriptor, new Object[] { new Integer(fd) });

            if (openFlags != null && Arrays.asList(openFlags).contains(OpenFlag.O_APPEND)) {
                Field appendField = java.io.FileDescriptor.class.getDeclaredField("append");
                appendField.setAccessible(true);
                appendField.setBoolean(fileDescriptor, true);
            }
        } catch (NoSuchFieldException e) {
            // Ignore
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new SdkRuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new SdkRuntimeException(e.getTargetException());
        }

        // Just in case passed descriptor had a proxy

        return fileDescriptor;
    }

    /**
     * Disables a BD-J proxy of a FileDescriptor instance.
     *
     * @param fileDescriptor FileDescriptor instance whose proxy to disable.
     * @throws SdkRuntimeException If an error occurs while disabling the proxy.
     */
    public static void disableFileDescriptorProxy(FileDescriptor fileDescriptor) {
        try {
            Field proxyField = java.io.FileDescriptor.class.getDeclaredField("proxy");
            proxyField.setAccessible(true);
            proxyField.set(fileDescriptor, null);
        } catch (NoSuchFieldException e) {
            // Ignore
        } catch (IllegalAccessException e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns the file descriptor value suitable for native calls.
     *
     * @param fileDescriptor FileDescriptor instance whose fd to return.
     * @return Integer that can be used in native calls to do I/O with
     *   this file descriptor.
     * @throws ProxiedFileDescriptorException Most native ways to open files
     *   result in a proxied file descriptor number. Such descriptor cannot
     *   be used to for native calls. When the given descriptor is proxies,
     *   this exception is raised.
     */
    public static int getFd(FileDescriptor fileDescriptor) throws ProxiedFileDescriptorException {
        // BD-J proxies real file descriptors by adding this increment to them.
        final int PROXY_FD_INCREMENT = 4096;

        try {
            Field fdField = java.io.FileDescriptor.class.getDeclaredField("fd");
            fdField.setAccessible(true);
            int fd = fdField.getInt(fileDescriptor);
            if (fd >= PROXY_FD_INCREMENT) {
                throw new ProxiedFileDescriptorException(ErrorMessages.getClassErrorMessage(FileDescriptorFactory.class,
                        "proxiedFileDescriptor", new Integer(fd)));
            }
            return fd;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }

    public static FileDescriptor getSocketFileDescriptor(Socket socket) {
        return getSocketFileDescriptor(Socket.class, socket);
    }

    public static FileDescriptor getSocketFileDescriptor(ServerSocket socket) {
        return getSocketFileDescriptor(ServerSocket.class, socket);
    }

    public static FileDescriptor getSocketFileDescriptor(DatagramSocket socket) {
        return getSocketFileDescriptor(DatagramSocket.class, socket);
    }

    private static FileDescriptor getSocketFileDescriptor(Class socketClass, Object socket) {
        try {
            Method getImplMethod = socketClass.getDeclaredMethod("getImpl", new Class[0]);
            getImplMethod.setAccessible(true);
            Object socketImpl = getImplMethod.invoke(socket, new Object[0]);

            Field socketFdField;
            if (socketImpl instanceof SocketImpl) {
                socketFdField = SocketImpl.class.getDeclaredField("fd");
            } else {
                socketFdField = DatagramSocketImpl.class.getDeclaredField("fd");
            }
            socketFdField.setAccessible(true);
            return (FileDescriptor) socketFdField.get(socketImpl);
        } catch (InvocationTargetException e) {
            throw new SdkRuntimeException(e.getTargetException().getMessage(), e.getTargetException());
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }
}
