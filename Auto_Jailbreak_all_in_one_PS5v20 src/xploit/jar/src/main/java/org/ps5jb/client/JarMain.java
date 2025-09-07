package org.ps5jb.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.Manifest;

import org.ps5jb.loader.Config;
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.loader.ManifestUtils;
import org.ps5jb.loader.Status;

/**
 * Main class of the JAR that will be executed remotely on the PS5. It has two roles:
 * <ol>
 *     <li>Utility to send the whole JAR to the PS5 when {@link #main(String[])} is invoked with specific parameters.</li>
 *     <li>Execution of {@link #execute()} when {@code main(String[])} is called with no parameters (presumably by the JAR loading Xlet).</li>
 * </ol>
 */
public class JarMain {
    private static final String MANIFEST_PAYLOAD_KEY = "PS5JB-Client-Payload";
    private static final String MANIFEST_LOGGER_HOST = "PS5JB-Client-Logger-Host";
    private static final String MANIFEST_LOGGER_PORT = "PS5JB-Client-Logger-Port";

    /**
     * JAR entry point. Has one of the 3 behaviours depending on the arguments:
     * <ul>
     *   <li>If no arguments are provided, executes {@link #execute()}</li>
     *   <li>If first argument is "--help", prints usage information</li>
     *   <li>
     *       In other cases, assume that the first argument is the host name of the JAR loader
     *       and the optional second argument is the port number of the JAR loader.
     *       Send the current JAR to the loader.
     *   </li>
     * </ul>
     *
     * @param args Command-line arguments.
     * @throws Exception Any exception in {@code main(String[])} will be rethrown.
     */
    public static void main(String[] args) throws Exception {
        JarMain main = new JarMain();

        if (args == null || args.length == 0) {
            // No args => execute exploit
            main.execute();
        } else if (args[0].equals("--help")) {
            // First arg is help, show usage
            Path jarPath = JarUtils.discoverJar(null);
            String jarName = jarPath == null ? "[payload.jar]" : jarPath.getFileName().toString();
            System.out.println("Usage: java -jar " + jarName + " <address> [<port>]");
        } else {
            main.sendJar(args);
        }
    }

    /**
     * Execute the JAR code. This method will be executed on the PS5.
     *
     * This method will try to find the key {@code #MANIFEST_PAYLOAD_KEY} in the JAR's manifest.
     * This key should contain a full classname of a payload to execute. It can also contain a short
     * classname if the payload is placed in <code>org.ps5jb.client.payloads</code> package.
     *
     * The payload should implement a {@link Runnable} interface and have a no-argument public constructor.
     * However, unlike usual usage of <code>Runnable</code>, the payload's code will not be executed in
     * a separate thread. The method {@link Runnable#run()} will be invoked directly.
     *
     * @throws Exception Any exception during JAR execution is rethrown.
     */
    protected void execute() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        boolean foundPayload = false;

        // Search manifests on the classpath
        Manifest mf = ManifestUtils.loadClasspathManifest(classLoader, MANIFEST_PAYLOAD_KEY, null);
        if (mf != null) {
            String payloadName = mf.getMainAttributes().getValue(MANIFEST_PAYLOAD_KEY);
            if (!"".equals(payloadName)) {
                foundPayload = true;
                if (payloadName.indexOf(".") == -1) {
                    // When just class name is specified, assume it's from "payloads" package
                    payloadName = getClass().getPackage().getName() + ".payloads." + payloadName;
                }

                boolean restoreLogger = false;
                try {
                    Class payloadClass = Class.forName(payloadName);
                    String payloadVersion = ManifestUtils.getImplementationVersion(mf);
                    Status.println("Executing payload: " + payloadName + (payloadVersion == null ? "" : " v" + payloadVersion));

                    // Activate Kernel accessor, if any
                    if (!KernelReadWrite.restoreAccessor(classLoader)) {
                        Status.println("Kernel R/W not available");
                    } else {
                        Status.println("Kernel R/W restored");
                    }

                    // See if we need to override logging
                    try {
                        String overrideLoggerHost = mf.getMainAttributes().getValue(MANIFEST_LOGGER_HOST);
                        if (overrideLoggerHost.equals("")) {
                            overrideLoggerHost = null;
                        }

                        if (overrideLoggerHost != null) {
                            String overrideLoggerPortStr = mf.getMainAttributes().getValue(MANIFEST_LOGGER_PORT);
                            if (overrideLoggerPortStr != null && !"".equals(overrideLoggerPortStr)) {
                                int overrideLoggerPort = Integer.parseInt(overrideLoggerPortStr);

                                Status.resetLogger(overrideLoggerHost, overrideLoggerPort, Config.getLoggerTimeout());
                                restoreLogger = true;
                                Status.println("Remote logging server set to " + overrideLoggerHost + ":" + overrideLoggerPort);
                            }
                        }
                    } catch (NumberFormatException e) {
                        Status.println("Logger port configuration is invalid: " +  " Skipping");
                    }

                    Runnable payload = (Runnable) payloadClass.newInstance();
                    payload.run();
                } catch (ClassNotFoundException e) {
                    Status.println("Unable to determine the payload to execute because the value of the attribute '" + MANIFEST_PAYLOAD_KEY + "' is not recognized: " + payloadName);
                } catch (ClassCastException e) {
                    Status.printStackTrace("Unable to execute the payload. Make sure it implements the " + Runnable.class.getName() + " interface", e);
                } finally {
                    if (KernelReadWrite.getAccessor(classLoader) != null && KernelReadWrite.saveAccessor(classLoader)) {
                        Status.println("Kernel R/W is active and is saved for future payloads");
                    }

                    if (restoreLogger) {
                        Status.println("Restoring default remote logging configuration");
                        Status.resetLogger(Config.getLoggerHost(), Config.getLoggerPort(), Config.getLoggerTimeout());
                    }
                }
            }
        }

        if (mf == null) {
            Status.println("Unable to determine payload to execute because the JAR manifest could not be opened.");
        } else if (!foundPayload) {
            Status.println("Unable to determine the payload to execute because the value of the attribute '" + MANIFEST_PAYLOAD_KEY + "' is empty");
        }
    }

    /**
     * Send the current JAR to the host:port specified by the first two arguments.
     *
     * @param args Array of arguments where first argument is the host and the second argument is the port.
     *   The latter is optional and will default to 9025.
     * @throws Exception Any exception thrown by methods used to send the JAR will be rethrown.
     */
    protected void sendJar(String[] args) throws Exception {
        // Any other case, attempt to send the jar
        String host = args[0];
        String port = args.length > 1 ? args[1] : "9025";

        Path jarPath = JarUtils.discoverJar(null);
        if (jarPath == null) {
            throw new FileNotFoundException("Jar file path could not be determined from the classloader");
        }

        JarUtils.sendJar(host, port, jarPath);
    }
}
