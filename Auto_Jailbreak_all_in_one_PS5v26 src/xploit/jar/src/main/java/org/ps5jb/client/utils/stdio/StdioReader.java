package org.ps5jb.client.utils.stdio;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import org.ps5jb.client.utils.process.ProcessUtils;
import org.ps5jb.loader.SocketListener;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.ErrNo;
import org.ps5jb.sdk.include.sys.Socket;
import org.ps5jb.sdk.io.FileDescriptorFactory;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Can be used to launch a socket listener that upon a client connection sends back
 * all the stdout and stderr output from JVM to the client socket.
 *
 * Only one client at a time is supported.
 */
public class StdioReader extends SocketListener {
    protected LibKernel libKernel;
    protected Socket socket;
    protected ErrNo errNo;

    protected StdioRedirectionState redirectionState;

    protected boolean clientTerminated;
    protected long heartbeatInterval;

    /**
     * Constructor.
     *
     * @param port Port on which to listen for client connections.
     * @param heartbeatInterval When greater than 0, specifies the interval in number of seconds
     *   when a new-line heartbeat will be sent over socket to detect if client closed the connection.
     *   When 0, no heartbeat will be sent and the socket will remain connected until terminated
     *   programmatically.
     * @throws IOException If I/O exception occurs.
     */
    public StdioReader(int port, long heartbeatInterval) throws IOException {
        super("Stdio Reader", port);

        libKernel = new LibKernel();
        socket = new Socket(libKernel);
        errNo = new ErrNo(libKernel);

        this.heartbeatInterval = heartbeatInterval;
        this.redirectionState = new StdioRedirectionState(libKernel, errNo);
    }

    /**
     * Redirects stdio to the given socket.
     *
     * @param socket Socket to redirect to.
     * @throws SdkException If any exceptions occur during native calls required to perform redirection.
     */
    protected void redirectStdIo(java.net.Socket socket) throws SdkException {
        FileDescriptor socketFileDescriptor = FileDescriptorFactory.getSocketFileDescriptor(socket);
        this.redirectionState.redirectStdIo(socketFileDescriptor);
    }

    /**
     * Restore original stdio streams. Does nothing
     * if stdio is not redirected.
     */
    protected void restoreStdIo() {
        this.redirectionState.restoreStdIo();
    }

    /**
     * Upon connection, redirects the stdio to the connected client.
     * Then waits indefinitely, until heartbeat detects a broken connection,
     * or until a programmatic termination of connection is issued.
     * Before ending, restores the original stdio streams.
     *
     * @param clientSocket Socket created for the client connection.
     * @throws Exception Any exception thrown by the implementation.
     */
    @Override
    protected void acceptClient(java.net.Socket clientSocket) throws Exception {
        clientTerminated = false;
        OutputStream out = clientSocket.getOutputStream();
        try {
            redirectStdIo(clientSocket);

            String client = clientSocket.getRemoteSocketAddress().toString();
            Status.println("Redirected stdio to " + client);

            // There is no clean way to detect that client disconnected.
            // Do so by sending an empty-line heartbeat evey `heartbeatInterval` ms.
            try {
                long lastHeartbeat = 0;
                final long sleepTimeout = 100L;
                final long heartbeatMs = heartbeatInterval * 10000L;
                while (!terminated && !clientTerminated) {
                    Thread.sleep(sleepTimeout);

                    if (heartbeatMs > 0) {
                        lastHeartbeat += sleepTimeout;

                        if (lastHeartbeat >= heartbeatMs) {
                            out.write("\n".getBytes());
                            lastHeartbeat = 0;
                        }
                    }
                }
            } catch (SocketException e) {
                // Assume connection is closed on the client end
            }

            Status.println("Disconnected from " + client);
        } finally {
            restoreStdIo();
            out.close();
        }
    }

    @Override
    protected void free() {
        super.free();

        if (libKernel != null) {
            libKernel.closeLibrary();
            libKernel = null;
        }
    }

    @Override
    public void run() {
        try {
            // Set minimal priority because this thread just busy-waits
            ProcessUtils procUtils = new ProcessUtils(libKernel, KernelPointer.NULL, null);
            procUtils.setCurrentThreadPriority(new Integer(Thread.MIN_PRIORITY), new Short((short) 767));

            super.run();
        } catch (Throwable e) {
            handleException(e);
        } finally {
            free();
        }
    }

    /**
     * Stops the connection with the current client.
     * Useful to do so programmatically after a logical unit of work
     * because it is not possible to detect whether client disconnected or not
     * from the state of the socket.
     */
    public void terminateClient() {
        clientTerminated = true;
    }
}
