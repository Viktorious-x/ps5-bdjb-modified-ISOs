package org.ps5jb.sdk.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Implements a wrapper class for a PS5 native library. This class is not thread-safe and each
 * thread must instantiate its own instance of the library. It is also advisable to call
 * {@link #closeLibrary()} method after the library is no longer needed to free up the allocated resources.
 * The class does not implement {@link java.io.Closeable Closeable} or {@link AutoCloseable} interfaces
 * because they do not exist on PS5.
 */
public class Library {
    private static Constructor NativeLibrary_new;
    private static Method NativeLibrary_findEntry;
    private static Method NativeLibrary_load;
    private static Field NativeLibrary_handle;

    static {
        initNativeLibrary();
    }

    private static void initNativeLibrary() {
        try {
            Class nativeLibraryClass = Class.forName("java.lang.ClassLoader$NativeLibrary");

            NativeLibrary_new = nativeLibraryClass.getDeclaredConstructors()[0];
            NativeLibrary_new.setAccessible(true);

            NativeLibrary_findEntry = nativeLibraryClass.getDeclaredMethod("findEntry", new Class[] { String.class });
            NativeLibrary_findEntry.setAccessible(true);

            NativeLibrary_load = nativeLibraryClass.getDeclaredMethod("load", new Class[0]);
            NativeLibrary_load.setAccessible(true);

            NativeLibrary_handle = nativeLibraryClass.getDeclaredField("handle");
            NativeLibrary_handle.setAccessible(true);
        } catch (NoSuchFieldException | NoSuchMethodException | ClassNotFoundException | RuntimeException | Error e) {
            throw new SdkRuntimeException(e);
        }
    }

    private final Object nativeLibraryInstance;

    private CallContext callContext;

    /**
     * Constructs a wrapper for a native library with a given handle.
     *
     * @param handle Identifier of the library.
     * @throws SdkRuntimeException When there is any error instantiating the library wrapper.
     */
    public Library(long handle) {
        Long h = new Long(handle);
        try {
            nativeLibraryInstance = NativeLibrary_new.newInstance(new Object[] { this.getClass(), h.toString(), Boolean.TRUE });
            NativeLibrary_handle.set(nativeLibraryInstance, h);
        } catch (InvocationTargetException e) {
            throw new SdkRuntimeException(e.getTargetException().getMessage(), e.getTargetException());
        } catch (IllegalAccessException | InstantiationException | RuntimeException | Error e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Constructs a wrapper for a native library with a given path on disk.
     *
     * @param path File path of the native library.
     * @throws SdkRuntimeException When there is any error instantiating the library wrapper.
     */
    public Library(String path) {
        try {
            nativeLibraryInstance = NativeLibrary_new.newInstance(new Object[] { this.getClass(), path, Boolean.FALSE });
            NativeLibrary_load.invoke(nativeLibraryInstance, new Object[0]);
        } catch (InvocationTargetException e) {
            throw new SdkRuntimeException(e.getTargetException().getMessage(), e.getTargetException());
        } catch (IllegalAccessException | InstantiationException | RuntimeException | Error e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Attempt to close the library during garbage collection if it was not done manually.
     *
     * @throws Throwable Re-throws any exception.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            closeLibrary();
        } finally {
            super.finalize();
        }
    }

    /**
     * Should be called when the library wrapper is no longer needed to free up the
     * allocated resources.
     *
     * @see CallContext#close()
     */
    public void closeLibrary() {
        if (this.callContext != null) {
            this.callContext.close();
            this.callContext = null;
        }
    }

    /**
     * Call a function at a given native memory address in this library.
     *
     * @param function Pointer to the function. Can be retrieved from a function name using {@link #addrOf(String)}.
     * @param args Arguments to pass to the function.
     * @return Value returned by the native call.
     * @see CallContext#execute(Pointer, long...)
     */
    protected long call(Pointer function, long ... args) {
        if (this.callContext == null) {
            this.callContext = new CallContext();
        }

        return this.callContext.execute(function, args);
    }

    /**
     * Get this library's handle.
     *
     * @return Value of the library handle. It's stable and unique in the system;
     *   though can vary for the same library in different PS5 firmware revisions.
     * @throws SdkRuntimeException If any exception occurs during this operation.
     */
    public long getHandle() {
        try {
            return ((Long) NativeLibrary_handle.get(nativeLibraryInstance)).longValue();
        } catch (IllegalAccessException | RuntimeException | Error e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Lookup of the symbol address by its name.
     *
     * @param symbolName Name of the symbol to lookup.
     * @return Native memory address of the symbol.
     * @throws SdkSymbolNotFoundException When symbol could not be found.
     * @throws SdkRuntimeException For any other exception occurring during this operation.
     */
    public Pointer addrOf(String symbolName) {
        try {
            Long symbolAddr = (Long) NativeLibrary_findEntry.invoke(nativeLibraryInstance, new Object[] { symbolName });
            if (symbolAddr == null || symbolAddr.longValue() == 0) {
                throw new SdkSymbolNotFoundException(ErrorMessages.getClassErrorMessage(Library.class,"symbolNotFound",symbolName, "0x" + Long.toHexString(getHandle())));
            }
            return Pointer.valueOf(symbolAddr.longValue());
        } catch (SdkRuntimeException e) {
            throw e;
        } catch (InvocationTargetException e) {
            throw new SdkRuntimeException(e.getTargetException().getMessage(), e.getTargetException());
        } catch (IllegalAccessException | RuntimeException | Error e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }
}
