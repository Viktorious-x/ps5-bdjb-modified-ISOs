package org.ps5jb.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Utility methods to work with JAR manifests.
 */
public class ManifestUtils {
    /**
     * Default constructor.
     */
    private ManifestUtils() {
    }

    /**
     * Searches for manifest on a classpath whose implementation name entry matches the given value.
     *
     * @param classLoader Classloader used to search the classpath
     * @param implementationName Value of {@link java.util.jar.Attributes.Name#IMPLEMENTATION_TITLE}
     *   attribute to search for.
     * @return Loaded manifest instance or null if manifest was not found.
     * @throws IOException If I/O exception occured during loading.
     */
    public static Manifest loadClasspathManifest(ClassLoader classLoader, String implementationName) throws IOException {
        return loadClasspathManifest(classLoader, Attributes.Name.IMPLEMENTATION_TITLE.toString(), implementationName);
    }

    /**
     * Searches for manifest on a classpath with a given attribute and optionally a matching value of this attribute
     *
     * @param classLoader Classloader used to search the classpath
     * @param attributeName Name of the attribute to search for.
     * @param attributeValue Value of the searched attribute to match. If null, any value will be accepted.
     * @return Loaded manifest instance or null if manifest was not found.
     * @throws IOException If I/O exception occured during loading.
     */
    public static Manifest loadClasspathManifest(ClassLoader classLoader, String attributeName, String attributeValue) throws IOException {
        Enumeration manifests = classLoader.getResources(JarFile.MANIFEST_NAME);
        while (manifests.hasMoreElements()) {
            URL manifestUrl = (URL) manifests.nextElement();
            URLConnection con = manifestUrl.openConnection();
            InputStream manifestStream = con.getInputStream();
            try {
                Manifest mf = new Manifest(manifestStream);
                String manifestAttrValue = mf.getMainAttributes().getValue(attributeName);
                if (manifestAttrValue != null && (attributeValue == null || attributeValue.equals(manifestAttrValue))) {
                    return mf;
                }
            } finally {
                manifestStream.close();
            }
        }

        return null;
    }

    /**
     * Loads the manifest of a given JAR file.
     *
     * @param jarFile Jar file whose manifest to retrieve.
     * @return Manifest of a given JAR file.
     * @throws IOException If I/O error occurs during manifest reading.
     */
    public static Manifest loadJarManifest(File jarFile) throws IOException {
        Manifest manifest;

        JarFile jar = new JarFile(jarFile);
        try {
            manifest = jar.getManifest();
        } finally {
            jar.close();
        }

        return manifest;
    }

    /**
     * Get the value of Value of {@link java.util.jar.Attributes.Name#IMPLEMENTATION_VERSION}
     * from the manifest.
     *
     * @param manifest manifest to read.
     * @return Manifest implementation version or null if attribute is not present.
     */
    public static String getImplementationVersion(Manifest manifest) {
        return manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
    }

    /**
     * Returns the implementation version value for the manifest of the JAR containing the given class.
     *
     * @param clazz Class whose JAR manifest to read
     * @param implementationName Name of the implementation to match in the manifest to confirm that
     *   the class is contained in the given JAR.
     * @return Manifest implementation version.
     * @throws IOException If I/O error occurs during manifest reading.
     */
    public static String getClassImplementationVersion(Class clazz, String implementationName) throws IOException {
        final Manifest manifest = loadClasspathManifest(clazz.getClassLoader(), implementationName);
        if (manifest == null) {
            throw new IOException("Version could not be read from the manifest");
        }
        return getImplementationVersion(manifest);
    }
}
