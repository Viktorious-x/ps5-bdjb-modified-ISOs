package org.ps5jb.loader.jar;

import org.ps5jb.loader.ManifestUtils;
import org.ps5jb.loader.Status;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public interface JarLoader extends Runnable {
    String MANIFEST_BACKGROUND_KEY = "PS5JB-Client-Background-Thread-Name";

    default void loadJar(final File jarFile, final boolean deleteJar) throws Exception {
        Status.println("Reading JAR Manifest...");
        Manifest mf = ManifestUtils.loadJarManifest(jarFile);

        String mainClassName = mf.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        if (mainClassName == null) {
            throw new ClassNotFoundException("Main class not defined in the JAR");
        }

        String backgroundThreadName = mf.getMainAttributes().getValue(MANIFEST_BACKGROUND_KEY);
        if ("".equals(backgroundThreadName)) {
            backgroundThreadName = null;
        }

        Status.println("Reading JAR Manifest... Main Class: " + mainClassName, true);

        // Load the JAR in a new classloader and execute the main method.
        // When classloader is garbage collected, all classes will be unloaded as well.
        // This is important to be able to load the modified copy or a parallel copy of the same class in another JAR.
        Status.println("Loading the JAR" + (backgroundThreadName != null ? " in a background thread [" + backgroundThreadName + "]" : "") + "...");

        ClassLoader parentLoader = getClass().getClassLoader();
        ClassLoader bypassRestrictionsLoader = new URLClassLoader(new URL[0], parentLoader) {
            @Override
            protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
                // Bypass various restrictions in BD-J classloader
                if (name.startsWith("java.nio") || name.startsWith("javax.security.auth") || name.startsWith("javax.net.ssl")) {
                    return findSystemClass(name);
                }
                return super.loadClass(name, resolve);
            }
        };
        java.net.URLClassLoader contextLoader = java.net.URLClassLoader.newInstance(new java.net.URL[] { jarFile.toURL() }, bypassRestrictionsLoader);

        final Class mainClass = contextLoader.loadClass(mainClassName);
        final Method mainMethod = mainClass.getDeclaredMethod("main", new Class[] { String[].class });

        Runnable jarMain = () -> {
            try {
                mainMethod.invoke(null, new Object[] { new String[0] });
                if (deleteJar) {
                    deleteTempJar(jarFile);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        };

        if (backgroundThreadName != null) {
            Thread jarThread = new Thread(jarMain, backgroundThreadName);
            jarThread.start();
        } else {
            try {
                jarMain.run();
            } catch (RuntimeException e) {
                // Unwrap runtime exception from runnable
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
                throw e;
            }
        }
    }

    default void deleteTempJar(File jarFile) {
        // Delete the file containing the temporary JAR
        if (!jarFile.delete()) {
            // Assume the system temp path was changed by the JAR payload.
            // Try the new temp path.
            boolean displaced = false;
            String tempPath = System.getProperty("java.io.tmpdir");
            if (jarFile.getAbsolutePath().indexOf(tempPath) != 0) {
                jarFile = new File(tempPath, jarFile.getName());
                if (jarFile.delete()) {
                    displaced = true;
                }
            }

            if (!displaced) {
                Status.println("Failed to delete the temporary JAR");
            }
        }
    }

    void terminate() throws IOException;
}
