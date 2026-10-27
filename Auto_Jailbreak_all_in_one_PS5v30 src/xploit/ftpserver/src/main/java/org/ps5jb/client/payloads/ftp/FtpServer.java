/*
MIT License

Copyright (c) 2017 Moritz Stückler

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package org.ps5jb.client.payloads.ftp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dvb.event.EventManager;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.havi.ui.event.HRcEvent;
import org.ps5jb.client.utils.init.SdkInit;
import org.ps5jb.loader.ManifestUtils;
import org.ps5jb.loader.SocketListener;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.OpenModuleAction;

/**
 * A very simple FTP Server class. On receiving a new connection it creates a
 * new worker thread.
 *
 * @author Moritz Stueckler
 */
public class FtpServer extends SocketListener implements UserEventListener {
    private List workers;

    private boolean useNativeCalls;

    private int exitConfirmCount;

    public FtpServer() throws IOException {
        super("FTP Server v" + ManifestUtils.getClassImplementationVersion(FtpServer.class, "ftpserver"), 9225);
        workers = new ArrayList();

        // Subscribe to all events
        EventManager eventManager = EventManager.getInstance();
        if (eventManager != null) {
            eventManager.addUserEventListener(this, new OverallRepository());
        }

        Status.println("Welcome to " + this.listenerName +
                ". You can login anonymously using the username '" + FtpWorker.DEFAULT_USERNAME + "' and " +
                (FtpWorker.DEFAULT_PASSWORD.equals("") ? "no password." : "password '" + FtpWorker.DEFAULT_PASSWORD + "'.") +
                " Exit the " + this.listenerName + " by issuing a custom 'TERM' command or by pressing the RED button 3 times.");
    }

    @Override
    public void acceptClient(Socket clientSocket) throws Exception {
        // Don't have a timeout on connection listening for FTP Commands.
        clientSocket.setSoTimeout(0);

        // Port for incoming dataConnection (for passive mode) is the controlPort +
        // number of created threads + 1
        int dataPort = serverSocket.getLocalPort() + workers.size() + 1;

        // Create new worker thread for new connection
        FtpWorker w = new FtpWorker(this, clientSocket, dataPort, "FTPWorker " + (workers.size() + 1), useNativeCalls);

        Status.println("New connection received from " + clientSocket.getInetAddress().getHostAddress());

        // Start the worker thread
        workers.add(w);
        w.start();
    }

    /**
     * PS5 BDJ runtime includes a mechanism to proxy all instances of key I/O classes
     * such as {@link java.io.File} to restrict access to sensitive information.
     * This method disables the proxying.
     *
     * @author astrelsky
     */
    protected void disableIOProxyFactory() {
        final String BDJ_FACTORY_CLASS_NAME = "com.oracle.orbis.io.BDJFactory";

        try {
            OpenModuleAction.execute(BDJ_FACTORY_CLASS_NAME);
            useNativeCalls = true;
            Status.println("Using native calls for file I/O");
        } catch (PrivilegedActionException e) {
            Status.println("Error while opening PS5-specific com.oracle.orbis.io package. " +
                    "Assuming this package does not exist in the current execution environment. " +
                    "Error: " + e.getException().getClass() + "; " +
                    "Message: " + e.getException().getMessage());
            Status.println("Using Java file I/O");
            return;
        }

        try {
            Class bdjFactoryClass = Class.forName(BDJ_FACTORY_CLASS_NAME);
            Field bdjFactoryInstance = bdjFactoryClass.getDeclaredField("instance");
            bdjFactoryInstance.setAccessible(true);
            bdjFactoryInstance.set(null, null);
        } catch (Throwable e) {
            handleException(e);
        }
    }

    @Override
    public void run() {
        try {
            // Disable I/O proxies
            disableIOProxyFactory();

            // When using native calls, activate the SDK
            if (useNativeCalls) {
                SdkInit.init(false, false);
            }

            // Execute the socket listener
            super.run();

            // Unsubscribe from events
            EventManager eventManager = EventManager.getInstance();
            if (eventManager != null) {
                eventManager.removeUserEventListener(this);
            }

            // Close all the workers
            Iterator workerIter = workers.iterator();
            while (workerIter.hasNext()) {
                FtpWorker worker = (FtpWorker) workerIter.next();
                worker.terminate();
            }

            workerIter = workers.iterator();
            while (workerIter.hasNext()) {
                FtpWorker worker = (FtpWorker) workerIter.next();
                try {
                    worker.join(5000);
                } catch (InterruptedException e) {
                    Status.printStackTrace(e.getMessage(), e);
                }
            }
        } finally {
            free();
        }
    }

    @Override
    protected void disposeClient(Socket clientSocket) {
        // Do nothing, socket is closed by the worker thread.
    }

    @Override
    public void userEventReceived(UserEvent userEvent) {
        if (userEvent.getFamily() == UserEvent.UEF_KEY_EVENT) {
            if (userEvent.getType() == HRcEvent.KEY_RELEASED) {
                switch (userEvent.getCode()) {
                    case HRcEvent.VK_COLORED_KEY_0:
                        if (exitConfirmCount == 2) {
                            try {
                                terminate();
                            } catch (IOException e) {
                                Status.printStackTrace(e.getMessage(), e);
                                exitConfirmCount = 0;
                            }
                        } else {
                            if (exitConfirmCount == 1) {
                                Status.println("Are you sure you want to terminate the " + this.listenerName + "? Press the same key one last time to confirm or any other key to cancel.");
                            }
                            ++exitConfirmCount;
                        }
                        break;
                    default:
                        if (exitConfirmCount > 1) {
                            Status.println("Termination request cancelled.");
                            exitConfirmCount = 0;
                        }
                }
            }
        }
    }
}
