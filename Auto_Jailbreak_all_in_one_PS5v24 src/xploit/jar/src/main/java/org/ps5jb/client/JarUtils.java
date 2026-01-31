package org.ps5jb.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivilegedActionException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jdk.internal.loader.URLClassPath;
import org.ps5jb.sdk.core.OpenModuleAction;

/**
 * Utility functions to determine the path of the JAR file on disc and to send the JAR file to a remote loader.
 */
public class JarUtils {
    /**
     * Sends the jar to the remote host.
     *
     * @param hostArg Hostname or IP address of the machine receiving the JAR.
     * @param portArg Port on which the machine is listening for the connection.
     * @param jarPath Path to the JAR file to send.
     * @throws IOException If I/O exception occurs during sending.
     */
    public static void sendJar(String hostArg, String portArg, Path jarPath) throws IOException {
        InetAddress addr = InetAddress.getByName(hostArg);
        int port = Integer.parseInt(portArg);

        Socket socket = new Socket(addr, port);
        socket.setSoTimeout(5000);

        InputStream jarStream = Files.newInputStream(jarPath);
        try {
            OutputStream out = socket.getOutputStream();
            try {
                byte[] buf = new byte[8192];
                int readCount;
                while ((readCount = jarStream.read(buf)) != -1) {
                    out.write(buf, 0, readCount);
                }

                System.out.println("JAR successfully sent");
            } finally {
                out.close();
            }
        } finally {
            jarStream.close();
        }
    }

    /**
     * Uses the class classloader and reflection to determine the path to own JAR file.
     *
     * @param classLoader Classloader used to load the JAR file. If null, the classloader of this class will be used.
     * @return Path to the JAR file containing this class. Returns {@code null} if JAR path could not be determined.
     * @throws NoSuchFieldException If the class classloader does not have "ucp" field. The classloader is assumed to
     *   be a subclass of {@link jdk.internal.loader.BuiltinClassLoader}.
     * @throws IllegalAccessException If access to the classloader state is denied through reflection.
     * @throws IOException If I/O error occurs while reading the JAR file.
     * @throws PrivilegedActionException If access to the classloader module is denied through reflection.
     */
    public static Path discoverJar(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException, IOException, PrivilegedActionException {
        Path jarPath = null;

        // Make sure we have access to the needed internal JDK class
        OpenModuleAction.execute("jdk.internal.loader.URLClassPath");

        // Try to determine the JAR file from classloader
        if (classLoader == null) {
            classLoader = JarUtils.class.getClassLoader();
        }
        Field field = classLoader.getClass().getDeclaredField("ucp");
        field.setAccessible(true);
        URLClassPath ucp = (URLClassPath) field.get(classLoader);
        for (URL url : ucp.getURLs()) {
            if (url.getProtocol().equals("file")) {
                Path path;
                try {
                    path = Path.of(new URI(url.toString()));
                } catch (URISyntaxException e) {
                    // Should not happen in practice
                    throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
                }
                JarFile jar = new JarFile(path.toFile());
                String classEntryName = JarMain.class.getName().replace('.', '/') + ".class";
                JarEntry jarEntry = jar.getJarEntry(classEntryName);
                if (jarEntry != null) {
                    jarPath = path;
                    break;
                }
            }
        }

        return jarPath;
    }
}
