package org.ps5jb.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Encapsulates the Xlet configuration.
 */
public class Config {
    private static final Properties props;

    static {
        props = new Properties();
        try {
            // The config file is located at the same level as the class itself.
            // Note that is operation will fail if security manager is active.
            // In this case, "props" will be empty and all getters will return the default values.
            String pkgName = Config.class.getPackage().getName().replace('.', '/');
            String configFileName = pkgName + "/config.properties";
            InputStream propsInput = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName);
            if (propsInput == null) {
                throw new InternalError("Config file " + configFileName + " not found");
            }

            try {
                props.load(propsInput);
            } finally {
                propsInput.close();
            }
        } catch (Throwable e) {
            Screen.println("Config loading error, using default values");
            Screen.getInstance().printStackTrace(e);
        }
    }

    /**
     * Default constructor. This class should be used statically, so the constructor is declared as private.
     */
    private Config() {
        super();
    }

    /**
     * Get the address where the network logger will send the messages.
     *
     * @return The hostname or IP of the remote logger listener. If null or empty, remote logger will be disabled.
     */
    public static String getLoggerHost() {
        return props.getProperty("logger.host", "");
    }

    /**
     * Get the port number where the network logger will send the messages.
     *
     * @return Port number used by remote logger listener. If 0, remote logger will be disabled.
     */
    public static int getLoggerPort() {
        return Integer.parseInt(props.getProperty("logger.port", "18194"));
    }

    /**
     * Get the socket timeout in milliseconds on remote logger operations. If timeout is reached, logger will be disabled for the rest of the execution.
     *
     * @return Remote logger timeout.
     */
    public static int getLoggerTimeout() {
        return Integer.parseInt(props.getProperty("logger.timeout", "5000"));
    }

    /**
     * Get the port number on which the JAR loader listens for incoming connections.
     *
     * @return Loader port number.
     */
    public static int getLoaderPort() {
        return Integer.parseInt(props.getProperty("loader.port", "9025"));
    }

    /**
     * Get the horizontal resolution of the Xlet screen.
     *
     * @return Number of horizontal pixels.
     */
    public static int getLoaderResolutionWidth() {
        return Integer.parseInt(props.getProperty("loader.resolution.width", "1920"));
    }

    /**
     * Get the vertical resolution of the Xlet screen.
     *
     * @return Number of vertical pixels.
     */
    public static int getLoaderResolutionHeight() {
        return Integer.parseInt(props.getProperty("loader.resolution.height", "1080"));
    }

    /**
     * Get the absolute path to the directory on disc where the payloads will be searched
     *
     * @return Absolute path to the directory on disc with JARs included in the disc assembly
     *   (possibly non-existent if no JARs were included).
     */
    public static File getLoaderPayloadPath() {
        File discRoot;
        try {
            discRoot = getDiscRootPath();
        } catch (IOException e) {
            throw new RuntimeException("Payload root path could not be retrieved due to I/O error", e);
        }
        return new File(discRoot, props.getProperty("loader.payload.root", "jar-payloads"));
    }

    /**
     * Returns the path to the root of Blu-ray disc directory.
     *
     * @return Path to the root of Blu-ray disc or null if the
     *   path could not be determined.
     * @throws IOException If path could not be determined due to U/O error.
     * @throws RuntimeException If exception occurs that is due
     *   to usage of reflection in the implementation of this method.
     */
    public static File getDiscRootPath() throws IOException {
        ClassLoader cl = Config.class.getClassLoader();

        try {
            Field ucpField = cl.getClass().getDeclaredField("ucp");
            ucpField.setAccessible(true);
            Object ucp = ucpField.get(cl);

            Field lmapField = ucp.getClass().getDeclaredField("lmap");
            lmapField.setAccessible(true);
            Map lmap = (Map) lmapField.get(ucp);

            Set urlSet = lmap.keySet();
            Iterator urlIter = urlSet.iterator();
            while (urlIter.hasNext()) {
                URL url = (URL) urlIter.next();
                if (url.getProtocol().equals("file") && url.getPath().endsWith("00000.jar")) {
                    File xletJarFile = new File(new URI(url.toString()));
                    if (xletJarFile.isFile()) {
                        File discDirectory = new File(xletJarFile.getParentFile(), "../..").getCanonicalFile();
                        if (discDirectory.isDirectory() && discDirectory.getName().equals("disc")) {
                            return discDirectory;
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        throw new FileNotFoundException("Disc root directory could not be detected");
    }
}
