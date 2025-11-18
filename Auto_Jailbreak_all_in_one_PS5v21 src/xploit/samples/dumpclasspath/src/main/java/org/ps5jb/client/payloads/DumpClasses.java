package org.ps5jb.client.payloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.PrivilegedActionException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.ps5jb.loader.ManifestUtils;
import org.ps5jb.loader.SocketListener;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.OpenModuleAction;

/**
 * <p>
 *   This sample creates a zip archive in the temp directory
 *   with all the known classes in the current thread's classloader.
 * </p>
 * <p>
 *   To use, simply send this class to the PS5 for execution in the JAR Loader.
 *   Then use `nc` or another tool to connect to the PS5:<br>
 *   <code>nc [PS5 IP] 9125 &gt; classpath.zip</code>
 * </p>
 * <p>
 * Upon connection, the classpath will be dumped and sent back to `nc`.
 * Depending on OS, `nc` may not terminate by itself. When PS5 reports that
 * the dump is finished, simply terminate it by force.
 * </p>
 */
public class DumpClasses extends SocketListener {
    /**
     * Default constructor. Listens on port 9125.
     *
     * @throws IOException If listening socket could not be created.
     */
    public DumpClasses() throws IOException {
        this(9125);
    }

    /**
     * Constructor for creating a dump listener on a custom port.
     *
     * @param port Port number to listen on.
     * @throws IOException If listening socket could not be created.
     */
    public DumpClasses(int port) throws IOException {
        super("Classpath Dumper v" + ManifestUtils.getClassImplementationVersion(DumpClasses.class, "dumpclasspath"), port);
    }

    /**
     * Executes the classpath dump of the current thread's classloader.
     *
     * @return Created zip file.
     * @throws Exception Re-throw any exception.
     */
    public File dumpClasspath() throws Exception {
        File dumpZip = File.createTempFile("classpath", ".zip");
        dumpZip.deleteOnExit();
        Status.println("Dumping class path to: " + dumpZip.getAbsolutePath());

        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(dumpZip));
        try {
            // Dump current thread classloader, make sure to iterate over all class hierarchy
            Set dumpedEntries = new HashSet();
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            ClassLoader prevCl = null;
            while (cl != null && !cl.equals(prevCl)) {
                prevCl = cl;

                Status.println("Dumping " + cl);
                dumpClassLoader(cl, zip, dumpedEntries);
                cl = cl.getParent();
            }

            // Dump bdj specific jars. They are at fixed location.
            // These seem to be added to boot class loader,
            // not sure yet how to get to them using generic dumper above.
            File pbpJar = new File("/app0/cdc/lib/pbp.jar");
            if (pbpJar.exists()) {
                Status.println("Dumping " + pbpJar);
                dumpJarFile(new JarFile(pbpJar), zip, new HashSet());
            }
            File bdjstackJar = new File("/app0/cdc/bdjstack.jar");
            if (bdjstackJar.exists()) {
                Status.println("Dumping " + bdjstackJar);
                dumpJarFile(new JarFile(bdjstackJar), zip, new HashSet());
            }

            zip.finish();
        } finally {
            zip.close();
        }

        return dumpZip;
    }

    /**
     * Dumps the classes of a given class loader to a zip stream
     *
     * @param cl Classloader to dump
     * @param zip Zip output stream where class files will be dumped.
     * @param dumpedEntries Set which will be updated with all the dumped entries. If there are duplicates, then
     *   only the first one will be dumped.
     * @throws Exception Re-throw any exception.
     */
    protected void dumpClassLoader(ClassLoader cl, ZipOutputStream zip, Set dumpedEntries) throws Exception {
        Class prevClass = null;
        Class c = cl.getClass();
        String indent = "";
        while (c != null && !c.equals(prevClass)) {
            prevClass = c;

            // Attempt to dump using built-in method. If does not work, try JDK ucp method.
            // Stop hierarchy traversal when something is dumped.
            if (!tryDumpBuiltinClassLoader(cl, c, zip, dumpedEntries)) {
                if (!tryDumpJdkUcpClassLoader(cl, c, zip, dumpedEntries)) {
                    Status.println(indent + "    Unrecognized class loader " + c.getName() + ". Skipping to parent");
                    c = c.getSuperclass();
                    indent += "  ";
                } else {
                    Status.println(indent + "  Dumped class loader " + c.getName());
                }
            } else {
                Status.println(indent + "  Dumped class loader " + c.getName());
            }
        }
    }

    /**
     * Use reflection to traverse all the resources of the given class loader and dump them
     * to a zip output stream.
     *
     * @param cl Class loader that is expected to have <code>ucp</code> field containing URLs of the JARs
     *   owned by this class loader.
     * @param clClass Class on which to perform the reflection operations. Must be the class of the class loader
     *   or one of its superclasses.
     * @param zip Zip output stream where the found resources will be written.
     * @param dumpedEntries Set which will be updated with all the dumped entries. If there are duplicates, then
     *   only the first one will be dumped.
     * @return True if dumping was successful; false if the specified class loader cannot be dumped.
     * @throws Exception Any exception occurred during dumping is re-thrown.
     */
    protected boolean tryDumpJdkUcpClassLoader(ClassLoader cl, Class clClass, ZipOutputStream zip, Set dumpedEntries) throws Exception {
        try {
            OpenModuleAction.execute("jdk.internal.loader.URLClassPath");

            Field ucpField = clClass.getDeclaredField("ucp");
            ucpField.setAccessible(true);

            Object ucpObject = ucpField.get(cl);
            if (ucpObject != null && (ucpObject instanceof jdk.internal.loader.URLClassPath)) {
                jdk.internal.loader.URLClassPath ucp = (jdk.internal.loader.URLClassPath) ucpObject;

                Method getLoaderMethod = ucp.getClass().getDeclaredMethod("getLoader", new Class[] { int.class });
                getLoaderMethod.setAccessible(true);

                Object loader = null;
                int i;
                for (i = 0; (loader = getLoaderMethod.invoke(ucp, new Object[] { new Integer(i) })) != null; ++i) {
                    Field jarfileField = loader.getClass().getDeclaredField("jarfile");
                    jarfileField.setAccessible(true);
                    JarFile jarFile = (JarFile) jarfileField.get(loader);
                    if (jarFile != null) {
                        Status.println("    Dumping " + jarFile.getName());
                        try {
                            dumpJarFile(jarFile, zip, dumpedEntries);
                        } catch (IOException e) {
                            Status.printStackTrace("Skipping due to error", e);
                        }
                    } else {
                        Status.println("    Skipping loader " + loader + " since it does not have a JAR file");
                    }
                }

                return true;
            }
        } catch (NoSuchFieldException | PrivilegedActionException e) {
            // Ignore, not a `ucp` class loader
        }

        return false;
    }

    /**
     * Use reflection to traverse all the resources of the given class loader and dump them
     * to a zip output stream.
     *
     * @param cl Class loader that is expected to be a descendant of {@link jdk.internal.loader.BuiltinClassLoader}.
     * @param clClass Class on which to perform the reflection operations. Must be the class of the class loader
     *   or one of its superclasses.
     * @param zip Zip output stream where the found resources will be written.
     * @param dumpedEntries Set which will be updated with all the dumped entries. If there are duplicates, then
     *   only the first one will be dumped.
     * @return True if dumping was successful; false if the specified class loader cannot be dumped.
     * @throws Exception Any exception occurred during dumping is re-thrown.
     */
    protected boolean tryDumpBuiltinClassLoader(ClassLoader cl, Class clClass, ZipOutputStream zip, Set dumpedEntries) throws Exception {
        try {
            OpenModuleAction.execute("java.lang.module.ModuleReference");
            OpenModuleAction.execute("jdk.internal.module.SystemModuleFinders");

            // On PS5, stream API is put away in jdk.internal.util package.
            // Because of this, all the calls to iterate over modules below have to use reflection.
            try {
                OpenModuleAction.execute("jdk.internal.util.Optional");
                OpenModuleAction.execute("jdk.internal.util.stream.Stream");
            } catch (PrivilegedActionException e) {
                Status.println("Error while opening PS5-specific jdk.internal.util package. " +
                        "Assuming this package does not exist in the current execution environment. " +
                        "Error: " + e.getException().getClass() + "; " +
                        "Message: " + e.getException().getMessage());
            }

            Field ptmField = clClass.getDeclaredField("packageToModule");
            ptmField.setAccessible(true);
            Map ptm = (Map) ptmField.get(cl);

            Set processed = new HashSet();
            Iterator modules = ptm.values().iterator();
            while (modules.hasNext()) {
                Object loadedModule = modules.next();
                if (!processed.contains(loadedModule)) {
                    Field mrefField = loadedModule.getClass().getDeclaredField("mref");
                    mrefField.setAccessible(true);
                    ModuleReference mref = (ModuleReference) mrefField.get(loadedModule);

                    Status.println("    Dumping " + mref.descriptor().name());

                    final ModuleReader mr = mref.open();
                    try {
                        Method listMethod = getMethod(mr.getClass(), "list", new Class[0]);

                        Method openMethod = getMethod(mr.getClass(), "open", new Class[] { String.class });

                        Object resourceStream = listMethod.invoke(mr, new Object[0]);
                        Method toArrayMethod = getMethod(resourceStream.getClass(), "toArray", new Class[0]);

                        Object[] resources = (Object[]) toArrayMethod.invoke(resourceStream, new Object[0]);
                        Status.println("      Processing " + resources.length + " resources");
                        for (Object res : resources) {
                            String resName = mref.descriptor().name() + "/" + res;
                            if (!dumpedEntries.contains(resName)) {
                                Object isOptional = openMethod.invoke(mr, new Object[] { res });

                                Method Optional_isPresentMethod = getMethod(isOptional.getClass(), "isPresent", new Class[0]);
                                Method Optional_getMethod = getMethod(isOptional.getClass(), "get", new Class[0]);

                                if (((Boolean) Optional_isPresentMethod.invoke(isOptional, new Object[0])).booleanValue()) {
                                    InputStream is = (InputStream) Optional_getMethod.invoke(isOptional, new Object[0]);
                                    try {
                                        createZipEntry(is, resName, zip);
                                        dumpedEntries.add(resName);
                                    } finally {
                                        is.close();
                                    }
                                } else {
                                    Status.println("    Failed to open classpath resource: " + res);
                                }
                            }
                        }
                    } finally {
                        mr.close();
                    }

                    processed.add(loadedModule);
                }
            }

            return true;
        } catch (NoSuchFieldException e) {
            // Ignore, not a built-in class loader.
        }

        return false;
    }

    /**
     * Returns the method by reflection and makes it accessible. If method is not found,
     * prints the existing methods of the class for debugging purposes.
     *
     * @param cl Class whose method to return.
     * @param methodName Name of the method.
     * @param parameterTypes Parameter types of the method.
     * @return Reflective reference to the method.
     * @throws NoSuchMethodException If the method could not be found.
     * @see Class#getMethod(String, Class[])
     * @see Method#setAccessible(boolean) 
     */
    protected Method getMethod(Class cl, String methodName, Class[] parameterTypes) throws NoSuchMethodException {
        try {
            Method result = cl.getMethod(methodName, parameterTypes);
            result.setAccessible(true);

            return result;
        } catch (NoSuchMethodException e) {
            // Print methods to debug why its not found
            printClassMethods(cl, "");
            throw e;
        }
    }

    /**
     * Prints the all the methods of the given class and its descendants.
     *
     * @param cl Class whose methods to print.
     * @param indent Indent to use when printing.
     */
    protected void printClassMethods(Class cl, String indent) {
        Method[] methods = cl.getDeclaredMethods();
        String nextIndent = indent + "  ";
        if (methods.length > 0) {
            Status.println(indent + cl.getName() + ":");
            for (Method method : methods) {
                Status.println(nextIndent + method.toString());
            }
        }

        if (cl.getSuperclass() != null) {
            printClassMethods(cl.getSuperclass(), nextIndent);
        }
    }

    /**
     * Dumps a JarFile to the zip output stream.
     *
     * @param jarFile Jar file to dump.
     * @param zip Zip output stream where Jar file will be dumped.
     * @param dumpedEntries Set which will be updated with all the dumped entries. If there are duplicates, then
     *   only the first one will be dumped.
     * @throws IOException Any exception occurred during dumping is re-thrown.
     */
    protected void dumpJarFile(JarFile jarFile, ZipOutputStream zip, Set dumpedEntries) throws IOException {
        Enumeration jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) jarEntries.nextElement();
            if (!jarEntry.isDirectory()) {
                File urlPath = new File(jarFile.getName());
                String resName = urlPath.getName() + "/" + jarEntry.getName();
                if (!dumpedEntries.contains(resName)) {
                    InputStream is = jarFile.getInputStream(jarEntry);
                    try {
                        createZipEntry(is, resName, zip);
                        dumpedEntries.add(resName);
                    } finally {
                        is.close();
                    }
                }
            }
        }
    }

    /**
     * Create a new zip entry with the contents of the given input stream in the specified output stream.
     *
     * @param is Input stream containing data for the new zip entry.
     * @param entryName File name inside the zip.
     * @param zip Zip output stream.
     * @throws IOException If I/O error occurs.
     */
    protected void createZipEntry(InputStream is, String entryName, ZipOutputStream zip) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);
        zip.putNextEntry(zipEntry);
        try {
            writeResource(is, zip);
        } finally {
            zip.closeEntry();
        }
    }

    /**
     * Read the given resource from the input stream and write it to the target output stream.
     *
     * @param is Input stream to read the resource from.
     * @param out Output stream where the resource will be copied.
     * @throws IOException If I/O error occurs.
     */
    protected void writeResource(InputStream is, OutputStream out) throws IOException {
        if (is != null) {
            try {
                byte[] buf = new byte[8 * 1024];
                int r;
                while ((r = is.read(buf)) > 0) {
                    out.write(buf, 0, r);
                }
            } finally {
                is.close();
            }
        }
    }

    /**
     * Dump the classpath and send it back to the client over network.
     *
     * @param clientSocket Socket where the dumped classpath will be sent.
     * @throws Exception Any exception thrown during the execution.
     */
    @Override
    public void acceptClient(Socket clientSocket) throws Exception {
        File dumpZip = dumpClasspath();

        Status.println("Sending the dump to: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
        try {
            OutputStream out = clientSocket.getOutputStream();
            try {
                InputStream is = new FileInputStream(dumpZip);
                try {
                    writeResource(is, out);
                } finally {
                    is.close();
                }
            } finally {
                out.close();
            }
            Status.println("Classpath dump sent successfully");

            // Default implementation of SocketListener listens infinitely.
            // We terminate the listener after the dump is complete.
            terminate();
        } finally {
            if (!dumpZip.delete()) {
                Status.println("Failed to delete the temporary classpath dump");
            }
        }
    }

    /**
     * Handle any exception during dumping by terminating the listener thread.
     *
     * @param ex Exception to handle.
     */
    @Override
    public void handleException(Throwable ex) {
        super.handleException(ex);

        // Terminate dumper on error
        try {
            terminate();
        } catch (IOException e) {
            // Ignore
        }
    }
}
