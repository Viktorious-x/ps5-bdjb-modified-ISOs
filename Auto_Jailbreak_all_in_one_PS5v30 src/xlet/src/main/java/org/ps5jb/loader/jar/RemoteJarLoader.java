package org.ps5jb.loader.jar;

import org.ps5jb.loader.LoaderXlet;
import org.ps5jb.loader.SocketListener;
import org.ps5jb.loader.Status;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Thread that creates a server socket and listens for the incoming connections sending the JARs for execution.
 */
public class RemoteJarLoader extends SocketListener implements JarLoader {
    /**
     * JarLoader constructor.
     *
     * @param port Port number on which to listen for incoming JARs.
     * @throws IOException When server socket constructor throws this exception.
     * @see ServerSocket#ServerSocket(int)
     */
    public RemoteJarLoader(int port) throws IOException {
        super("JAR Loader v" + LoaderXlet.getXletImplementationVersion(), port);
    }

    /**
     * Called when a client connection sending a JAR file is accepted.
     *
     * @param clientSocket Client socket.
     * @throws Exception Exception that occurred during JAR execution.
     */
    @Override
    protected void acceptClient(Socket clientSocket) throws Exception {
        InputStream jarStream = clientSocket.getInputStream();
        try {
            // Generate a unique JAR filename in the temporary directory.
            Path jarPath = Files.createTempFile("jarLoader", ".jar");
            File jarFile = jarPath.toFile();
            try {
                // Delete temp jar on exit if it is not cleaned up manually for some reason
                jarFile.deleteOnExit();

                // Receive JAR data and save it to temporary file
                Status.println("Receiving JAR data to: " + jarFile);
                byte[] buf = new byte[8192];
                int readCount;
                int totalSize = 0;
                OutputStream jarOut = Files.newOutputStream(jarPath);
                try {
                    while ((readCount = jarStream.read(buf)) != -1) {
                        jarOut.write(buf, 0, readCount);

                        Status.println("Received " + totalSize + " bytes...", totalSize != 0);
                        totalSize += readCount;
                    }
                } finally {
                     jarOut.close();
                }
                Status.println("Received " + totalSize + " bytes...Done", true);

                this.loadJar(jarFile, true);
            } catch (IOException | RuntimeException | Error e) {
                deleteTempJar(jarFile);
                throw e;
            }
        } finally {
            jarStream.close();
        }
    }

    /**
     * Handles exceptions declared as throwable by {@link #acceptClient(Socket)} by
     * printing a status message and a stack trace.
     *
     * @param ex Handled exception.
     */
    @Override
    protected void handleException(Throwable ex) {
        if (ex instanceof InvocationTargetException) {
            Status.printStackTrace("Execution of JAR threw an exception", ((InvocationTargetException) ex).getTargetException());
        } else if (ex instanceof NoSuchMethodException) {
            Status.printStackTrace("Main class does not contain 'main(String[])' method", ex);
        } else {
            super.handleException(ex);
        }
    }

    @Override
    public void terminate() throws IOException {
        super.terminate();
    }
}
