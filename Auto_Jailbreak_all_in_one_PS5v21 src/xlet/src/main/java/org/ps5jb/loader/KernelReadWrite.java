package org.ps5jb.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class managing capability of SDK to read/write the kernel memory.
 */
public final class KernelReadWrite {
    private static final Map kernelAccessorsPerClassLoader = new HashMap();

    /**
     * Kernel accessor state, serialized by calling {@link #saveAccessor(ClassLoader)}.
     * Only one copy is kept at a time even though multiple accessors may be defined
     * by parallel class loaders. This only works because it is assumed that all
     * classloaders use a compatible accessor instance
     * (i.e. they can all deserialize the same accessor correctly).
     */
    private static byte[] kernelAccessorState;

    /** Custom object input stream which can use a specific class loader to find the class */
    private static class ClassLoaderObjectInputStream extends ObjectInputStream {
        ClassLoader classLoader;

        /**
         * Constructor of the class
         *
         * @param in Stream from which to read the objects.
         * @param classLoader Classloader to use for resolving classes.
         * @throws IOException An exception occurred in the underlying stream.
         */
        private ClassLoaderObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        @Override
        protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            Class result = null;

            if (classLoader != null) {
                String className = desc.getName();
                try {
                    result = Class.forName(className, false, classLoader);
                } catch (ClassNotFoundException ex) {
                    Status.printStackTrace("Could not resolve the class " + className + " using classloader " + classLoader, ex);
                }
            }

            if (result == null) {
                result = super.resolveClass(desc);
            }

            return result;
        }
    }

    /**
     * Default constructor
     */
    private KernelReadWrite() {
    }

    /**
     * Register a global instance of a kernel accessor, responsible for
     * reading and writing kernel memory. The global instances are tracked
     * per classloader of the accessor instance.
     *
     * @param kernelAccessor New accessor instance.
     * @throws NullPointerException If the given accessor instance is null.
     */
    public static synchronized void setAccessor(KernelAccessor kernelAccessor) {
        if (kernelAccessor == null) {
            throw new NullPointerException("Accessor instance may not be null");
        }

        kernelAccessorsPerClassLoader.put(kernelAccessor.getClass().getClassLoader(), kernelAccessor);
    }

    /**
     * Unregisters a global instance of a kernel accessor for a given classloader.
     *
     * @param classLoader Classloader whose accessor to remove.
     */
    public static synchronized void removeAccessor(ClassLoader classLoader) {
        kernelAccessorsPerClassLoader.remove(classLoader);
    }

    /**
     * Retrieves the first available kernel accessor instance.
     *
     * @return Instance of a kernel accessor if one is registered or null.
     * @deprecated This method only works if there is not more than one
     *   JAR loaded in parallel. When there are background JARs running,
     *   use {@link #getAccessor(ClassLoader)} instead, passing the
     *   classloader instance that was used to load the JAR file.
     */
    @Deprecated
    public static synchronized KernelAccessor getAccessor() {
        Iterator valIter = kernelAccessorsPerClassLoader.values().iterator();
        KernelAccessor result = null;
        if (valIter.hasNext()) {
            result = (KernelAccessor) valIter.next();
        }
        return result;
    }

    /**
     * Retrieves the kernel accessor for a given class loader. Most often,
     * the caller of this method can use <code>getClass().getClassLoader()</code>
     * to specify the parameter value.
     *
     * @param classLoader Class loader used to load the JAR.
     * @return Instance of a kernel accessor if one is registered or null.
     */
    public static synchronized KernelAccessor getAccessor(ClassLoader classLoader) {
        return (KernelAccessor) kernelAccessorsPerClassLoader.get(classLoader);
    }

    /**
     * Checks whether a serialized kernel accessor is available for payloads.
     *
     * @return True if kernel accessor is not currently active but
     *   kernel access has been acquired by BD-J process and JAR payloads
     *   will be able to use it.
     */
    public static synchronized boolean hasAccessorState() {
        return kernelAccessorState != null;
    }

    /**
     * Saves the current state of the kernel accessor associated with the
     * given classloader. If there is already an existing accessor state
     * replaces it with the new value.
     *
     * @param classLoader ClassLoader for which to save the accessor.
     * @return True there is a saved accessor state available at the end of the execution.
     */
    public static synchronized boolean saveAccessor(ClassLoader classLoader) {
        KernelAccessor kernelAccessor = getAccessor(classLoader);
        if (kernelAccessor != null) {
            // Remove accessor from the map
            kernelAccessorsPerClassLoader.remove(classLoader);

            // Save the serialized state
            try {
                ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream outStr = new ObjectOutputStream(outBytes);
                    try {
                        outStr.writeObject(kernelAccessor);
                        outStr.flush();
                        kernelAccessorState = outBytes.toByteArray();
                    } finally {
                        outStr.close();
                    }
                } finally {
                    outBytes.close();
                }
            } catch (IOException | RuntimeException | Error e) {
                Status.printStackTrace("Exception occurred while saving the kernel accessor state", e);
            }
        } else {
            kernelAccessorState = null;
        }

        return kernelAccessorState != null;
    }

    /**
     * Restores kernel accessor instance from a previously saved state.
     *
     * @param classLoader ClassLoader to use to find the kernel accessor class
     * @return True if kernel accessor was activated. False if there was no previously saved
     *   accessor or if it could not be activated.
     */
    public static synchronized boolean restoreAccessor(ClassLoader classLoader) {
        KernelAccessor kernelAccessor = null;

        if (kernelAccessorState != null) {
            try {
                ByteArrayInputStream inBytes = new ByteArrayInputStream(kernelAccessorState);
                try {
                    ClassLoaderObjectInputStream inStr = new ClassLoaderObjectInputStream(inBytes, classLoader);
                    try {
                        kernelAccessor = (KernelAccessor) inStr.readObject();
                    } finally {
                        inStr.close();
                    }
                } finally {
                    inBytes.close();
                }
                setAccessor(kernelAccessor);
            } catch (ClassNotFoundException | IOException | RuntimeException | Error e) {
                Status.printStackTrace("Exception occurred while restoring the kernel accessor", e);
            }
        }

        return kernelAccessor != null;
    }
}
