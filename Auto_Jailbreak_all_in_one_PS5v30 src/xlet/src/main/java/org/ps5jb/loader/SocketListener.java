package org.ps5jb.loader;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Base class that can be used to listen for incoming network connections and send back the data.
 */
public abstract class SocketListener implements Runnable {
    /** Used to control when to stop the listener. */
    protected boolean terminated;
    /** Established server socket. */
    protected ServerSocket serverSocket;
    /** Name of this listener, used in the output of various messages. */
    protected String listenerName;

    /** Determined local IP address on which the server socket is listening. */
    protected static String netAddress;

    static {
        // Determine network interface address to print it on screen
        try {
            netAddress = determineNetAddress();
        } catch (Throwable e) {
            // Ignore timeout, network address will remain undetermined
            Status.println("Warning, IP address could not be determined. Exception: " + e.getClass().getName() + ". Message: " + e.getMessage());
        }
    }

    /**
     * Socket listener constructor.
     *
     * @param listenerName Name of this listener, to identify it in output messages.
     * @param port Port number on which to listen for incoming connection.
     * @throws IOException When server socket constructor throws this exception.
     * @see ServerSocket#ServerSocket(int)
     */
    public SocketListener(String listenerName, int port) throws IOException {
        this.listenerName = listenerName;

        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(5000);
    }

    /**
     * Determines network IP address.
     *
     * @throws SocketException If socket exception occurs.
     * @throws UnknownHostException If a hostname resolution exception occurs.
     */
    protected static String determineNetAddress() throws SocketException, UnknownHostException {
        final DatagramSocket tempSocket = new DatagramSocket();
        try {
            tempSocket.setSoTimeout(1000);
            tempSocket.connect(InetAddress.getByName("8.8.8.8"), 53);
            return tempSocket.getLocalAddress().getHostAddress();
        } finally {
            tempSocket.close();
        }
    }

    /**
     * Get the IPv4 address on which the server is listening.
     *
     * @return IPv4 address of the server.
     */
    public static String getNetAddress() {
        return netAddress;
    }

    /**
     * Socket listener thread entry point.
     */
    @Override
    public void run() {
        boolean printWaiting = true;

        while (!terminated) {
            try {
                if (printWaiting) {
                    if (netAddress == null) {
                        Status.println(this.listenerName + " waits for incoming connections on port " + serverSocket.getLocalPort() +
                                ", but the IPv4 address could not be determined. Double-check the network connectivity...");
                    } else {
                        Status.println(this.listenerName + " is waiting for incoming connections on " + netAddress + ":" + serverSocket.getLocalPort() + "...");
                    }
                }
                printWaiting = true;

                // Listen for new incoming connection
                Socket clientSocket = serverSocket.accept();
                try {
                    clientSocket.setSoTimeout(5000);
                    acceptClient(clientSocket);
                } finally {
                    disposeClient(clientSocket);
                }
            } catch (SocketException e) {
                // Ignore under expected conditions (external termination or client disconnect)
                if (!terminated && !serverSocket.isClosed()) {
                    handleException(e);
                }
            } catch (InterruptedIOException e) {
                // Do nothing, this is expected due to socket timeout.
                // Listening will just restart until terminate() is called.
                printWaiting = false;
            } catch (Throwable e) {
                handleException(e);
            }
        }

        free();

        Status.println(this.listenerName + " terminated!");
    }

    /**
     * Method to implement when listener accepts a client connection.
     *
     * @param clientSocket Socket created for the client connection.
     * @throws Exception Any exception thrown by the implementation.
     */
    protected abstract void acceptClient(Socket clientSocket) throws Exception;

    /**
     * Invoked when {@link #acceptClient(Socket)} returns. Default implementation
     * closes the socket.
     *
     * @param clientSocket Socket to dispose.
     * @throws Exception Any exception thrown by the implementation.
     */
    protected void disposeClient(Socket clientSocket) throws Exception {
        try {
            clientSocket.close();
        } catch (IOException closeEx) {
            // Ignore, socket may have been closed.
        }
    }

    /**
     * Method called to handle an exception thrown by {@link #acceptClient(Socket)}.
     * Note that {@link InterruptedIOException}
     * throwable class is handled by the base implementation and will not be
     * passed to this method.
     *
     * @param ex Exception to handle.
     */
    protected void handleException(Throwable ex) {
        Status.printStackTrace("Error occurred", ex);
    }

    /**
     * Call to signal the thread to terminate the listening. This is not an immediate termination, it may take a few
     * moments for the thread to be actually done. To wait on it, make sure to call {@link Thread#join()} on this thread
     * after calling {@code terminate()}.
     *
     * @throws IOException If socket closure throws this exception.
     */
    public void terminate() throws IOException {
        terminated = true;
        serverSocket.close();
    }

    /**
     * Called just before the listener is done to free up the resources.
     */
    protected void free() {
        if (!terminated || !serverSocket.isClosed()) {
            try {
                terminate();
            } catch (IOException | RuntimeException e) {
                // Ignore exceptions during cleanup
                Status.println(e.getClass() + " occurred while freeing the listener: " + e.getMessage());
            }
        }
    }
}
