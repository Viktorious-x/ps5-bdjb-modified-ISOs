package com.hdcookbook.grin.util;

/*  
 * Copyright (c) 2009, Sun Microsystems, Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of Sun Microsystems nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 *  Note:  In order to comply with the binary form redistribution 
 *         requirement in the above license, the licensee may include 
 *         a URL reference to a copy of the required copyright notice, 
 *         the list of conditions and the disclaimer in a human readable 
 *         file with the binary form of the code that is subject to the
 *         above license.  For example, such file could be put on a 
 *         Blu-ray disc containing the binary form of the code or could 
 *         be put in a JAR file that is broadcast via a digital television 
 *         broadcast medium.  In any event, you must include in any end 
 *         user licenses governing any code that includes the code subject 
 *         to the above license (in source and/or binary form) a disclaimer 
 *         that is at least as protective of Sun as the disclaimers in the 
 *         above license.
 * 
 *         A copy of the required copyright notice, the list of conditions and
 *         the disclaimer will be maintained at 
 *         https://hdcookbook.dev.java.net/misc/license.html .
 *         Thus, licensees may comply with the binary form redistribution
 *         requirement with a text file that contains the following text:
 * 
 *             A copy of the license(s) governing this code is located
 *             at https://hdcookbook.dev.java.net/misc/license.html
 */

/**
 * This class runs a thread that is used for communicating with a server
 * via a socket.  It accepts requests for network activity on the enqueue()
 * method, which queues the request for execution on the networking thread.
 * It then performs the request on the networking thread.  If used with
 * GRIN, it is expected that the response data will be packaged as a
 * GRIN command, for later execution back in the animation thread.
 **/
public class NetworkManager {

    static Object LOCK = new Object();          // Also needed by XletDirector
    private static int numActivations = 0;
    private static boolean destroyed = false;
    private static Queue queue = new Queue(10);
    private static Thread thread;

    public static void start() {
        synchronized(LOCK) {
            if (destroyed) {
                   throw new IllegalStateException();
            }
            numActivations++;
            if (numActivations > 1) {
                return;
            }
        }
        Runnable r = new Runnable() {
            public void run() {
                try {
                    processQueue();
                } catch (InterruptedException ex) {
                    if (Debug.LEVEL > 0) {
                        Debug.println("Network manager interrupted.");
                    }
                }
                if (Debug.LEVEL > 0) {
                    Debug.println("Network manager thread exits.");
                }
            }
        };
        thread = new Thread(r, "Network Manager");
        // Set the priority as the same as SetupManager, and one less than
        // AnimationEngine.
        thread.setPriority(3); 
        thread.setDaemon(true);
        thread.start();
    }
    
    public static void shutdown() {
        synchronized(LOCK) {
            numActivations--;
            if (numActivations < 1) {
                destroyed = true;
                LOCK.notifyAll();
            }
            if (thread != null) {
                thread.interrupt();
                thread = null;
                    // This thread.interrupt() is important.  Our network
                    // taskg (the Runnable instances that get enqueued)
                    // should poll Thread.interrupted() regularly, and shut
                    // down if it is true.  This will more quickly terminate
                    // the NetworkManager thread on xlet shutdown.
            }
        }
    }

    /**
     * Queue the runnable to run in the networking thread.  When the
     * Runnable executes, it should regulary check Thread.isInterrupted,
     * and return if it finds the thread has been interrupted.
     *
     * @see java.lang.Thread#isInterrupted()
     **/
    public static void enqueue(Runnable r) {
        synchronized(LOCK) {
            queue.add(r);
            LOCK.notifyAll();
        }
    }

    private static void processQueue() throws InterruptedException {
        for (;;) {
            Runnable runnable;
            for (;;) {
                synchronized(LOCK) {
                    if (destroyed) {
                        return;
                    }
                    if (!queue.isEmpty()) {
                        runnable = (Runnable) queue.remove();
                        break;
                    }
                    LOCK.wait();
                }
            }
            try {
        // In runnable.run(), most likely the code will be doing some
        // network access, and if it's http, then it'll take time.
        // This seems to slow down the animation engine trying to do
        // the screen update.  Hence, yielding before invoking the
        // runnable - just like we do in ManagedFullImage class.
        // By yielding we increase the odds that higher-priority animation
        // will be given a chance before  the NetworkManager thread 
        // possibly blocks.
                Thread.currentThread().yield();
                runnable.run();
            } catch (Throwable t) {
                if (t instanceof InterruptedException) {
                    throw (InterruptedException) t;
                }
                if (Debug.LEVEL > 0) {
                    Debug.printStackTrace(t);
                }
            }
        }
    }
}
