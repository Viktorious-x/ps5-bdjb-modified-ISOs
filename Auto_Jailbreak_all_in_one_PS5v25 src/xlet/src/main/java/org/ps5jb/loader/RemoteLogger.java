package org.ps5jb.loader;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.StringTokenizer;

/**
 * Log over UDP, big thanks to psxdev.
 */
public class RemoteLogger {
    /** Remote port where the log messages are sent. */
    protected int loggerPort;
    /** Remote IP address or hostname where the log messages are sent. */
    protected String loggerServer;
    /** UDP socket created for message sending. */
    protected DatagramSocket loggerSocket;

    /**
     * Constructor for RemoteLogger.
     *
     * @param server Hostname or IP of the remote machine receiving the logs.
     * @param port Port on which the remote machine receives the logs.
     * @param timeout Socket timeout for the remote logger (in milliseconds).
     */
    public RemoteLogger(String server, int port, int timeout) {
        if (server != null && server.length() > 0 && port > 0) {
            try {
                loggerSocket = new DatagramSocket();
                loggerSocket.setSoTimeout(timeout);
                loggerPort = port;
                loggerServer = server;
            } catch (Throwable e) {
                Screen.println("Network logger could not be initialized");
                Screen.getInstance().printStackTrace(e);
            }
        }
    }

    /**
     * Terminates the remote logger by closing the socket.
     */
    public synchronized void close() {
        if (loggerSocket != null) {
            loggerSocket.close();
            loggerSocket = null;
        }
    }

    /**
     * Sends the binary data over the logging socket.
     *
     * @param buffer Buffer to send.
     * @param len Number of bytes in the buffer to send (typically, {@code buffer.length}).
     */
    public void sendBytes(byte[] buffer, int len) {
        Throwable ex = null;

        synchronized (this) {
            if (loggerSocket != null) {
                try {
                    // Send in small chunks otherwise there may be an exception that packet is too large.
                    // This is a UDP protocol constraint
                    int i = 0;
                    while (i < len) {
                        int curLen = Math.min(len - i, 1024);
                        DatagramPacket sendPacket = new DatagramPacket(buffer, i, curLen, InetAddress.getByName(loggerServer), loggerPort);
                        loggerSocket.send(sendPacket);
                        i += curLen;
                    }
                } catch (Throwable e) {
                    // Do not attempt network logging after failure (assume host down)
                    close();

                    ex = e;
                }
            }
        }

        if (ex != null) {
            Screen.println("Network data could not be sent", false, false);
            Screen.getInstance().printStackTrace(ex);
        }
    }

    /**
     * Sends a string over the logging socket. The string is sent in UTF-8 encoding.
     *
     * @param message String to send.
     */
    private void sendString(String message) {
        Throwable ex = null;

        synchronized (this) {
            if (loggerSocket != null) {
                try {
                    byte[] buf = message.getBytes("UTF-8");
                    sendBytes(buf, buf.length);
                } catch (Throwable e) {
                    ex = e;
                }
            }
        }

        if (ex != null) {
            Screen.println("Network message could not be sent", false, false);
            Screen.getInstance().printStackTrace(ex);
        }
    }

    /**
     * Sends an error message as well as the stack trace of an exception over the logging socket.
     *
     * @param msg Message to send.
     * @param e Exception whose stack trace to send. If {@code null}, stack trace will not be generated.
     */
    public void error(String msg, Throwable e) {
        StringBuffer sb = new StringBuffer();
        sb.append("[ERROR] ");
        sb.append(msg);
        if (e != null) {
            StringWriter sw = new StringWriter();
            try {
                PrintWriter pw = new PrintWriter(sw);
                try {
                    e.printStackTrace(pw);
                } finally {
                    pw.close();
                }
                sb.append("\n");
                sb.append(sw);
            } finally {
                try {
                    sw.close();
                } catch (Throwable ioEx) {
                    // Do nothing
                }
            }
        }
        sb.append("\n");
        sendString(sb.toString());
    }

    /**
     * Sends an info message over the logging socket.
     *
     * @param msg Message to send.
     */
    public void info(String msg) {
        String message = "[INFO] " + msg + "\n";
        sendString(message);
    }

    /**
     * Sends a debug message over the logging socket. Debugging message will contain the information about
     * the method that generated the log.
     *
     * @param msg Message to send.
     */
    public void debug(String msg) {
        try {
            // Raise an exception to print its stacktrace and extract the line that contains the caller of debug()
            try {
                throw new RuntimeException("Fake exception to get stack trace");
            } catch (RuntimeException e) {
                StringWriter sw = new StringWriter();
                try {
                    PrintWriter pw = new PrintWriter(sw);
                    try {
                        e.printStackTrace(pw);
                    } finally {
                        pw.close();
                    }

                    String currentMethod = null;
                    StringTokenizer st = new StringTokenizer(sw.toString(), "\n");
                    for (int i = 0; i < 3 && st.hasMoreTokens(); ++i) {
                        String line = st.nextToken();
                        if (i == 2) {
                            currentMethod = "[" + line.trim() + "] ";
                        }
                    }

                    String message = "[DEBUG] " + currentMethod + msg + "\n";
                    sendString(message);
                } finally {
                    sw.close();
                }
            }
        } catch (Throwable e) {
            Screen.println("Sending debug message failed", false, false);
            Screen.getInstance().printStackTrace(e);
        }
    }
}
