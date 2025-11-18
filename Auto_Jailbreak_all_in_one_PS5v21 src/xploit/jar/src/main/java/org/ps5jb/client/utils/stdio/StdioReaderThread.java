package org.ps5jb.client.utils.stdio;

import java.io.IOException;

/**
 * Convenience subclass of thread which gives access to the underlying
 * StdioReader runnable instance.
 */
public class StdioReaderThread extends Thread {
    private StdioReader stdioReader;

    /**
     * Constructor.
     *
     * @param name Thread name
     * @param port Socket listener port
     * @param heartbeatInterval When greater than 0, specifies the interval in number of seconds
     *   when a new-line heartbeat will be sent over socket to detect if client closed the connection.
     *   When 0, no heartbeat will be sent and the socket will remain connected until terminated
     *   programmatically.
     * @throws IOException If I/O exception occurs.
     */
    public StdioReaderThread(String name, int port, long heartbeatInterval) throws IOException {
        super(name);
        stdioReader = new StdioReader(port, heartbeatInterval);
    }

    /**
     * Get the instance of StdioReader.
     *
     * @return StdioReader instance.
     */
    public StdioReader getStdioReader() {
        return stdioReader;
    }

    @Override
    public void run() {
        stdioReader.run();
    }
}
