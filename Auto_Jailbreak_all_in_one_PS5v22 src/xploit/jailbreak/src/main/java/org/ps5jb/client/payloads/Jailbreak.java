package org.ps5jb.client.payloads;

import java.io.File;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Set;
import java.util.jar.JarFile;

import org.ps5jb.client.utils.init.KernelBaseUnknownException;
import org.ps5jb.client.utils.init.KernelReadWriteUnavailableException;
import org.ps5jb.client.utils.init.SdkInit;
import org.ps5jb.client.utils.process.ProcessUtils;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.OpenModuleAction;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This payload removes various restrictions on the BD-J process
 * and patches some kernel variables for more freedom:
 * <ul>
 *     <li>Process user is switched to root.</li>
 *     <li>Process is pointed to root of the filesystem rather than in sandbox.</li>
 *     <li>Process security flags are elevated.</li>
 *     <li>Various "interesting" kernel flags enabled.</li>
 * </ul>
 * It requires an active Kernel R/W
 */
public class Jailbreak implements Runnable {
    private static final String SANDBOX_PATH = "/mnt/sandbox/" + Process.BDJ_PROCESS_P_TITLE_ID + "_000";
    private static final String ORIG_JAVA_HOME = "/app0/cdc/";
    private static final String ORIG_DOWNLOAD_0 = "/OS/HDD/download0/";
    private static final String NEW_JAVA_HOME = SANDBOX_PATH + ORIG_JAVA_HOME;
    private static final String NEW_DOWNLOAD_0 = SANDBOX_PATH + "/download0/";

    private static final String BDMV_PLAYER_COMM = "BdmvPlayerCore.elf";

    private static final boolean VERBOSE = false;

    private ProcessUtils procUtils;

    @Override
    public void run() {
        final LibKernel libKernel = new LibKernel();
        try {
            SdkInit sdk = SdkInit.init(true, true);

            procUtils = new ProcessUtils(libKernel, KernelPointer.valueOf(sdk.kernelBaseAddress), sdk.kernelOffsets);
            KernelPointer kdataAddress = KernelPointer.valueOf(sdk.kernelDataAddress);

            int curUid = libKernel.getuid();
            int curPid = libKernel.getpid();
            boolean isSandbox = libKernel.is_in_sandbox();
            println("Current UID: " + curUid + ". Current PID: " + curPid);
            println("In sandbox? " + (isSandbox ? "Yes" : "No"));

            if (curUid == 0 && !isSandbox) {
                println("Already jailbroken. Aborting!");
                return;
            }

            Process curProc = new Process(KernelPointer.valueOf(sdk.curProcAddress));
            String curProcName = curProc.getName();
            println("Found current process at " + curProc.getPointer() + " named " + curProcName, true);

            Process bdmvPlayerProcess = findBdPlayerProcess();
            if (bdmvPlayerProcess == null) {
                println("BdmvPlayerCore process could not be found. Aborting!");
                return;
            }

            KernelPointer rootvnode = kdataAddress.pptr(sdk.kernelOffsets.OFFSET_KERNEL_DATA_BASE_ROOTVNODE);

            // Patch the bd-j process creds
            patchProcess(curProc, rootvnode);
            // Patch the bdmv process creds (if not done, loading of other jars stops working)
            patchProcess(bdmvPlayerProcess, rootvnode);

            // Check root
            curUid = libKernel.getuid();
            println("New UID: " + curUid + (curUid == 0 ? " (root!)" : ""));

            // Check sandbox again
            println("In Sandbox? " + (libKernel.is_in_sandbox() ? "Yes" : "No"));

            // Update java home
            redirectJavaHome();

            // Reset JAR file factory which caches open JARs
            resetJarFileFactory();

            // Update classpath of xlet classloader
            redirectXletClassLoader();

            // Update classpath of boot classloader
            redirectBootLoader();
        } catch (KernelReadWriteUnavailableException | KernelBaseUnknownException e) {
            Status.println("Kernel R/W is not available or kernel data address has not been determined, aborting.");
        } catch (Throwable e) {
            Status.printStackTrace(e.getMessage(), e);
        } finally {
            libKernel.closeLibrary();
        }
    }

    private KernelPointer[] patchProcess(Process process, KernelPointer rootVnode) {
        // Patch ucred
        procUtils.setUserGroup(process, new int[] {
            0, // cr_uid
            0, // cr_ruid
            0, // cr_svuid
            1, // cr_ngroups
            0, // cr_rgid
            0, // cr_svid
            0  // gid0
        });

        // Escalate sony privs
        long[] oldPrivs = procUtils.getPrivs(process);
        procUtils.setPrivs(process, new long[] {
                oldPrivs[0],         // cr_sceAuthId
                0xFFFFFFFFFFFFFFFFL, // cr_sceCaps[0]
                0xFFFFFFFFFFFFFFFFL, // cr_sceCaps[1]
                0x80                 // cr_sceAttr[0]
        });

        // Remove dynlib and syscall restrictions (credits to Specter and cheburek3000)
        KernelPointer dynlibAddr = process.getDynLib();
        dynlibAddr.write4(0x118, 0);
        dynlibAddr.write8(0x18, 1);
        dynlibAddr.write8(0xF0, 0);
        dynlibAddr.write8(0xF8, 0xFFFFFFFFFFFFFFFFL);

        // Escape sandbox
        KernelPointer[] result = new KernelPointer[2];
        result[0] = process.getOpenFiles().getRootDir();
        result[1] = process.getOpenFiles().getJailDir();
        process.getOpenFiles().setRootDir(rootVnode);
        process.getOpenFiles().setJailDir(rootVnode);

        // Return original values
        return result;
    }

    private Process findBdPlayerProcess() {
        return procUtils.searchProcess((proc) -> {
            return Process.BDJ_PROCESS_P_TITLE_ID.equals(proc.getTitleId()) &&
                    BDMV_PLAYER_COMM.equals(proc.getName());
        });
    }

    private String replacePattern(String value, String oldPattern, String newPattern) {
        if (value != null) {
            int patternPos = value.indexOf(oldPattern);
            if (patternPos != -1) {
                return value.substring(0, patternPos) +
                        newPattern +
                        value.substring(patternPos + oldPattern.length());
            }
        }
        return value;
    }

    private void replaceSystemProperty(String propertyName, String oldPattern, String newPattern) {
        String oldValue = System.getProperty(propertyName);
        String newValue = replacePattern(oldValue, oldPattern, newPattern);
        System.setProperty(propertyName, newValue);
        println("Redirected system property '" + propertyName + "': '" + oldValue + "' => '" + newValue + "'", true);
    }

    private void replaceNonPublicField(String className, String fieldName, Object instance, Object value)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {

        Class clazz = Class.forName(className);
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        if (Modifier.isFinal(field.getModifiers())) {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        }
        Object oldValue = field.get(instance);
        if (oldValue instanceof String[]) {
            oldValue = Arrays.asList((String[]) oldValue);
        } else if (oldValue instanceof SoftReference) {
            oldValue = ((SoftReference) oldValue).get();
        }
        field.set(instance, value);
        println("Redirected field value " + (instance == null ? className : instance.getClass().getName()) + "." + fieldName +
                ": '" + oldValue + "' => '" + value + "'", true);
    }

    /**
     * Sets properties of certain built-in classes to point to absolute and not sandboxed
     * JVM location.
     */
    private void redirectJavaHome() {
        final String STATIC_PROPERTY_CLASS_NAME = "jdk.internal.util.StaticProperty";
        final String BOOT_LOADER_CLASS_NAME = "jdk.internal.loader.BootLoader";
        final String CLASS_LOADERS_CLASS_NAME = "jdk.internal.loader.ClassLoaders";
        final String BUILT_IN_CLASS_LOADER_CLASS_NAME = "jdk.internal.loader.BuiltinClassLoader";
        final String JRT_FILE_SYSTEM_PROVIDER_CLASS_NAME = "jdk.internal.jrtfs.JrtFileSystemProvider";
        final String FILE_TEMP_DIR_CLASS_NAME = "java.io.File$TempDirectory";
        final String TEMP_FILE_HELPER_CLASS_NAME = "java.nio.file.TempFileHelper";
        final String CLASS_LOADER_CLASS_NAME = ClassLoader.class.getName();

        // Replace system properties
        replaceSystemProperty("java.home", ORIG_JAVA_HOME, NEW_JAVA_HOME);
        replaceSystemProperty("java.library.path", ORIG_JAVA_HOME, NEW_JAVA_HOME);
        replaceSystemProperty("sun.boot.library.path", ORIG_JAVA_HOME, NEW_JAVA_HOME);
        replaceSystemProperty("bluray.bindingunit.root", ORIG_DOWNLOAD_0, NEW_DOWNLOAD_0);
        replaceSystemProperty("dvb.persistent.root", ORIG_DOWNLOAD_0, NEW_DOWNLOAD_0);
        replaceSystemProperty("java.io.tmpdir", ORIG_DOWNLOAD_0, NEW_DOWNLOAD_0);

        // Open non-exported modules
        try {
            OpenModuleAction.execute(STATIC_PROPERTY_CLASS_NAME);
            OpenModuleAction.execute(BOOT_LOADER_CLASS_NAME);
            OpenModuleAction.execute(JRT_FILE_SYSTEM_PROVIDER_CLASS_NAME);
        } catch (PrivilegedActionException e) {
            Status.printStackTrace("Unable to open JDK internal modules", e);
            return;
        }

        // Use reflection to substitute internal properties of various JVM classes
        try {
            replaceNonPublicField(STATIC_PROPERTY_CLASS_NAME, "JAVA_HOME", null, NEW_JAVA_HOME);
            replaceNonPublicField(BOOT_LOADER_CLASS_NAME, "JAVA_HOME", null, NEW_JAVA_HOME);
            replaceNonPublicField(FILE_TEMP_DIR_CLASS_NAME, "tmpdir", null, new File(System.getProperty("java.io.tmpdir")));
            replaceNonPublicField(TEMP_FILE_HELPER_CLASS_NAME, "tmpdir", null, Path.of(System.getProperty("java.io.tmpdir")));
            replaceNonPublicField(CLASS_LOADER_CLASS_NAME, "usr_paths", null, null);
            replaceNonPublicField(CLASS_LOADER_CLASS_NAME, "sys_paths", null, null);

            Class classLoadersClass = Class.forName(CLASS_LOADERS_CLASS_NAME);
            Method bootLoaderMethod = classLoadersClass.getDeclaredMethod("bootLoader", new Class[0]);
            bootLoaderMethod.setAccessible(true);
            ClassLoader bootLoader = (ClassLoader) bootLoaderMethod.invoke(null, new Object[0]);
            replaceNonPublicField(BUILT_IN_CLASS_LOADER_CLASS_NAME, "resourceCache", bootLoader, null);

            Method platformLoaderMethod = classLoadersClass.getDeclaredMethod("platformClassLoader", new Class[0]);
            platformLoaderMethod.setAccessible(true);
            ClassLoader platformLoader = (ClassLoader) platformLoaderMethod.invoke(null, new Object[0]);
            replaceNonPublicField(BUILT_IN_CLASS_LOADER_CLASS_NAME, "resourceCache", platformLoader, null);

            Method appLoaderMethod = classLoadersClass.getDeclaredMethod("appClassLoader", new Class[0]);
            appLoaderMethod.setAccessible(true);
            ClassLoader appLoader = (ClassLoader) appLoaderMethod.invoke(null, new Object[0]);
            replaceNonPublicField(BUILT_IN_CLASS_LOADER_CLASS_NAME, "resourceCache", appLoader, null);

            Field fileFsField = File.class.getDeclaredField("fs");
            fileFsField.setAccessible(true);
            Object fileFs = fileFsField.get(null);
            replaceNonPublicField(fileFs.getClass().getName(), "javaHome", fileFs, NEW_JAVA_HOME);

            Iterator installedFsProviders = FileSystemProvider.installedProviders().iterator();
            while (installedFsProviders.hasNext()) {
                FileSystemProvider provider = (FileSystemProvider) installedFsProviders.next();
                if (provider.getClass().getName().equals(JRT_FILE_SYSTEM_PROVIDER_CLASS_NAME)) {
                    replaceNonPublicField(provider.getClass().getName(), "theFileSystem", provider, null);
                }
            }

            println("Java home redirected");
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            Status.printStackTrace("Unable to redirect java.home", e);
        } catch (InvocationTargetException e) {
            Status.printStackTrace("Unable to redirect java.home", e.getTargetException());
        }
    }

    /**
     * Replaces the sandbox-relative JAR paths in the context class loader by the full path.
     */
    private void redirectXletClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        while (!"com.sony.bdjstack.core.XletClassLoader".equals(cl.getClass().getName())) {
            cl = cl.getParent();
        }
        if (cl == null) {
            return;
        }

        try {
            Field ucpField = cl.getClass().getDeclaredField("ucp");
            ucpField.setAccessible(true);
            Object ucp = ucpField.get(cl);

            Field urlsField = ucp.getClass().getDeclaredField("urls");
            urlsField.setAccessible(true);
            Stack urls = (Stack) urlsField.get(ucp);

            Method notifyAppCacheUpdatedMethod = ucp.getClass().getDeclaredMethod("notifyAppCacheUpdated", new Class[0]);
            notifyAppCacheUpdatedMethod.setAccessible(true);
            notifyAppCacheUpdatedMethod.invoke(null, new Object[0]);

            List newUrls = new ArrayList();
            while (!urls.empty()) {
                URL url = (URL) urls.pop();
                String newUrl = replacePattern(url.toString(), ORIG_JAVA_HOME, NEW_JAVA_HOME);
                newUrls.add(newUrl);
                println("Redirected URL in Xlet Classloader: '" + url + "' => '" + newUrl + "'", true);
            }
            for (int i = newUrls.size() - 1; i >= 0; --i) {
                urls.push(new URL((String) newUrls.get(i)));
            }

            Field superUcpField = cl.getClass().getSuperclass().getDeclaredField("ucp");
            superUcpField.setAccessible(true);
            Object superUcp = superUcpField.get(cl);
            resetUcp(superUcp, cl);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | MalformedURLException e) {
            Status.printStackTrace("Unable to redirect the context class loader", e);
        } catch (InvocationTargetException e) {
            Status.printStackTrace("Unable to redirect the context class loader", e.getTargetException());
        }
    }

    private ClassLoader getBootLoader() throws
            ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {

        Class classLoadersClass = Class.forName("jdk.internal.loader.ClassLoaders");
        Method bootLoaderMethod = classLoadersClass.getDeclaredMethod("bootLoader", new Class[0]);
        bootLoaderMethod.setAccessible(true);
        return (ClassLoader) bootLoaderMethod.invoke(null, new Object[0]);
    }

    private void redirectBootLoader() {
        try {
            ClassLoader cl = getBootLoader();

            Field ucpField = cl.getClass().getSuperclass().getDeclaredField("ucp");
            ucpField.setAccessible(true);
            Object ucp = ucpField.get(cl);
            resetUcp(ucp, cl);
        } catch (NoSuchFieldException | IllegalAccessException | MalformedURLException | NoSuchMethodException | ClassNotFoundException e) {
            Status.printStackTrace("Unable to redirect boot class loader", e);
        } catch (InvocationTargetException e) {
            Status.printStackTrace("Unable to redirect boot class loader", e.getTargetException());
        }
    }

    private void resetUcp(Object ucp, ClassLoader cl) throws
            NoSuchFieldException, NoSuchMethodException, IllegalAccessException,
            MalformedURLException, InvocationTargetException {

        Field ucpPathField = ucp.getClass().getDeclaredField("path");
        ucpPathField.setAccessible(true);
        Method ucpAddUrlMethod = ucp.getClass().getDeclaredMethod("addURL", new Class[] { URL.class });
        ucpAddUrlMethod.setAccessible(true);

        List path = (List) ucpPathField.get(ucp);
        if (!path.isEmpty()) {
            Iterator pathIter = path.iterator();
            List newPath = new ArrayList();
            while (pathIter.hasNext()) {
                URL nextPath = (URL) pathIter.next();
                if (nextPath.toString().indexOf(ORIG_JAVA_HOME) != -1) {
                    URL newUrl = new URL(replacePattern(nextPath.toString(), ORIG_JAVA_HOME, NEW_JAVA_HOME));
                    newPath.add(newUrl);
                    pathIter.remove();
                    println("Redirected URL cache of classloader " + cl + ": '" + nextPath + "' => '" + newUrl + "'", true);
                } else {
                    println("Skipping URL cache of classloader " + cl + ": " + nextPath, true);
                }
            }
            for (int i = 0; i < newPath.size(); ++i) {
                ucpAddUrlMethod.invoke(ucp, new Object[] { newPath.get(i) });
            }
        } else {
            println("Skipping classloader " + cl + " as it has no classpath", true);
        }

        Field ucpLoadersField = ucp.getClass().getDeclaredField("loaders");
        ucpLoadersField.setAccessible(true);
        List loaders = (List) ucpLoadersField.get(ucp);
        if (!loaders.isEmpty()) {
            Method loaderCloseMethod = null;
            Iterator loadersIter = loaders.iterator();
            while (loadersIter.hasNext()) {
                Object loader = loadersIter.next();
                if (loaderCloseMethod == null) {
                    loaderCloseMethod = loader.getClass().getDeclaredMethod("close", new Class[0]);
                    loaderCloseMethod.setAccessible(true);
                }
                loaderCloseMethod.invoke(loader, new Object[0]);
            }
            println("Cleared " + loaders.size() + " loaders in classloader " + cl, true);
            loaders.clear();
        }

        Field ucpLmapField = ucp.getClass().getDeclaredField("lmap");
        ucpLmapField.setAccessible(true);
        Map lmap = (Map) ucpLmapField.get(ucp);
        Iterator lmapIter = lmap.keySet().iterator();
        if (lmapIter.hasNext()) {
            while (lmapIter.hasNext()) {
                String lkey = (String) lmapIter.next();
                println("Clearing key " + lkey + " from cache in classloader " + cl, true);
            }
            lmap.clear();
        }
    }

    private void resetJarFileFactory() {
        final String JAR_FILE_FACTORY_CLASS_NAME = "sun.net.www.protocol.jar.JarFileFactory";

        try {
            OpenModuleAction.execute(JAR_FILE_FACTORY_CLASS_NAME);
        } catch (PrivilegedActionException e) {
            Status.printStackTrace("Unable to open JDK internal modules", e);
            return;
        }

        try {
            Class jarFileFactory = Class.forName(JAR_FILE_FACTORY_CLASS_NAME);

            Method instanceMethod = jarFileFactory.getDeclaredMethod("getInstance", new Class[0]);
            instanceMethod.setAccessible(true);
            Object instance = instanceMethod.invoke(null, new Object[0]);

            Method closeMethod = jarFileFactory.getDeclaredMethod("close", new Class[] { JarFile.class });
            closeMethod.setAccessible(true);

            synchronized (instance) {
                Field urlCacheField = jarFileFactory.getDeclaredField("urlCache");
                urlCacheField.setAccessible(true);
                HashMap urlCache = (HashMap) urlCacheField.get(null);

                Set jarFileSet = urlCache.keySet();
                Iterator jarFileIter = jarFileSet.iterator();
                while (jarFileIter.hasNext()) {
                    JarFile jarFile = (JarFile) jarFileIter.next();
                    closeMethod.invoke(instance, new Object[] { jarFile });
                    println("Removed JAR file from global cache: " + jarFile.getName(), true);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
            Status.printStackTrace("Unable to reset JAR file factory", e);
        } catch (InvocationTargetException e) {
            Status.printStackTrace("Unable to reset JAR file factory", e.getTargetException());
        }
    }

    private void println(String message) {
        println(message, false);
    }

    /**
     * Conditionally prints information.
     *
     * @param message Message to print.
     * @param verbose Whether the given message is verbose. If true, the
     *   then the message will only be printed if {@link #VERBOSE} is also true.
     */
    private void println(String message, boolean verbose) {
        if (!verbose || VERBOSE) {
            Status.println(message);
        }
    }
}
