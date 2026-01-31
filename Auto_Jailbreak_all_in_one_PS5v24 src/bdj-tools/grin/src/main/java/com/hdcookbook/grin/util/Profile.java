
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
package com.hdcookbook.grin.util;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.UnsupportedEncodingException;

/**
 * Profiling support.  This is enabled or disabled by setting the static
 * variable Debug.PROFILE to true or false.  When profiling is enabled,
 * parts of the GRIN framework will give you profiling data, and you might
 * wish to profile parts of your xlet's code as well.  As of this writing,
 * the direct draw animation manager collects profiling data for key parts
 * of the animation loop.
 * <p>
 * Each profile call is associated with a byte "thread ID."  This is meant to
 * provide a lightweight mechanism to seperate out execution in different
 * threads.  The programmer must pass in a byte constant for this; we don't
 * try to lookup the current thread with any automated means, because doing
 * so would be time-consuming.  We expect the developer to know what happens
 * on what thread, and we expect that a typical xlet will have a very small
 * number of well-defined threads that are of interest for profiling.  Thread
 * IDs from 0xf8 through 0xff are reserved for the cookbook tools, and are
 * declared as constants in this class; beyond that, it's up to the developer
 * to manage the namespace of thread IDs.
 * <p>
 * Usage:
 * <pre>
 *     private static byte[] PROFILE_TIMER_1;
 *     static {
 *          if (Debug.PROFILE) {
 *              PROFILE_TIMER_1 = Profile.makeProfileTimer("My animation");
 *          }
 *      }
 *      <...>
 *      public void myMethod() {
 *          int token;
 *          if (Profile.PROFILE) {
 *              Profile.initProfiler(2008, "127.0.0.1");
 *              token = Profile.startTimer(PROFILE_TIMER_1, 
 *                                         Profile.TID_ANIMATION);
 *          }
 *          doTheThingIWantMeasured();
 *          if (Profile.PROFILE) {
 *              Profile.stopTimer(token);
 *              Profile.doneProfiling();
 *          }
 *      }
 * </pre>
 *
 * @author Jaya
 */
public class Profile {

    public final static byte TIMER_START = 0;
    public final static byte TIMER_STOP = 1;
    public final static byte MESSAGE = 2;
    private static DatagramSocket socket = null;
    private static DatagramPacket packet;
    private static byte[] stopBuf = new byte[5];
    private static int token = 0;

    /**
     * Constant for the thread ID of the GRIN animation thread.
     * @see #startTimer(byte[], byte)
     */
    public final static byte TID_ANIMATION = (byte) 0xff;

    /**
     * Constant for the thread ID of the GRIN setup thread.
     * @see #startTimer(byte[], byte)
     */
    public final static byte TID_SETUP = (byte) 0xfe;

    private Profile() {
    }

    /**
     * Initializes this class with the network address of the
     * remote computer where profiling is done.  This is a NOP if
     * Debug.PROFILE is false.
     *
     * @param port The UDP port on which the remote computer is waiting for
     *             data
     * @param host The hostname or the IP address of the remote computer
     */
    public static void initProfiler(int port, String host) {
        if (!Debug.PROFILE) {
            return;
        }
        if (Debug.LEVEL > 0) {
            Debug.println("***  Profiling data being sent to host " + host
                          + " port " + port);
        }
        InetAddress addr = null;
        try {
            // get the inet address from the string
            addr = InetAddress.getByName(host);
            socket = new DatagramSocket();
            packet = new DatagramPacket(stopBuf, stopBuf.length,
                                        addr, port);
        } catch (IOException e) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(e);
                socket = null;
            }
        }
    }

    /**
     * Initialize the starting point of counting for tokens.  This usually
     * isn't needed, but it makes it possible for one xlet's tokens to
     * start from where a previous xlet's ended.  This lets you use the
     * profiler to do things like time xlet startup time.
     **/
    public static void initTokenStart(int tokenStart) {
        if (!Debug.PROFILE) {
            return;
        }
        if (Debug.ASSERT && tokenStart < token) {
            Debug.assertFail("Illegal token start value " + tokenStart
                             + " < " + token);
        }
        token = tokenStart;
    }

    /**
     * Allocates buffer and returns UTF-8 bytes for the string representing
     * profile information. This method is meant to be called by the application
     * during class loading:
     * Usage:
     * <p>
     * <pre>
     *     private static byte[] PROFILE_TIMER_1;
     *     static {
     *          if (Debug.PROFILE) {
     *              PROFILE_TIMER_1 = Profile.makeProfileTimer("my animation");
     *          }
     *     }
     * </pre>
     * @param description of the task that is being profiled.
     * @return A UTF-8 encoded byte array representing the description.
     */
    public static byte[] makeProfileTimer(String description) {
        if (!Debug.PROFILE) {
            return null;
        }
        byte[] utf8Buf = null;
        try {
            utf8Buf = description.getBytes("UTF-8");
         } catch (UnsupportedEncodingException e) {
            Debug.printStackTrace(e);
         }
         byte[] retBuf = new byte[(utf8Buf.length + 6)];
         System.arraycopy(utf8Buf, 0,
                         retBuf, 6, utf8Buf.length);
         utf8Buf = null;
         return retBuf;
    }

    /**
     * Allocates buffer and returns UTF-8 bytes for a debug message packet.
     * The first byte of the result is added by the profiling framework,
     * and should not be modified.  Any other bytes can be, e.g. to insert
     * numeric result data without generating heap objects.
     * Usage:
     * <p>
     * <pre>
     *     private static byte[] PROFILE_MESSAGE_1;
     *     static {
     *          if (Debug.PROFILE) {
     *              PROFILE_MESSAGE_1 
     *                  = Profile.makeMessage("count now X");
     *          }
     *     }
     *
     *     <...>
     *     
     *        if (Debug.PROFILE) {
     *             PROFILE_MESSAGE_1[PROFILE_MESSAGE_1.length - 1]
     *                 = (byte) count;
     *             Profile.sendMessage(PROFILE_MESSAGE_1);
     *        }
     * </pre>
     * @param message the message
     * @return A UTF-8 encoded byte array for the message
     */
    public static byte[] makeMessage(String message) {
        if (!Debug.PROFILE) {
            return null;
        }
        byte[] utf8Buf = null;
        try {
            utf8Buf = message.getBytes("UTF-8");
         } catch (UnsupportedEncodingException e) {
            Debug.printStackTrace(e);
         }
         byte[] retBuf = new byte[(utf8Buf.length + 1)];
         System.arraycopy(utf8Buf, 0,
                         retBuf, 1, utf8Buf.length);
         utf8Buf = null;
         return retBuf;
    }

    /**
     * Indicates profiling is over, releases the network resources.
     */
    public static synchronized void doneProfiling() {
        if (!Debug.PROFILE) {
            return;
        }
        if (socket != null) {
            socket.close();
        }
        socket = null;
    }

    /**
     * Signals starting the timer on the remote computer.
     *
     * @param startBuf  Buffer holding the description of the 
     *                  block of code that is time.  This byte array
     *                  should be obtained from makeProfileTimer().
     *
     * @param threadID  Identifier of the "thread" this execution occurs on.
     *                  See the class comments about "thread," and why it's a
     *                  byte.
     *                  
     * @return Returns the token for the task that is timed
     *
     * @see #makeProfileTimer(String)
     * @see Profile
     */
    public static synchronized int startTimer(byte[] startBuf, byte threadID) {
        if (!Debug.PROFILE) {
            return 0;
        }
        token++;
        DatagramSocket sock = socket;
        if (sock != null) {
            startBuf[0] = (byte) TIMER_START;
            startBuf[1] = (byte) ((token >> 24) & 0xff);
            startBuf[2] = (byte) ((token >> 16) & 0xff);
            startBuf[3] = (byte) ((token >> 8) & 0xff);
            startBuf[4] = (byte) (token & 0xff);
            startBuf[5] = threadID;
            try {
                packet.setData(startBuf);
                sock.send(packet);
            } catch (IOException e) {
                Debug.printStackTrace(e);
            }
        }
        return token;
    }

    /**
     * Signals stopping the timer on the remote computer.
     *
     * @param tk Token for the task that is done.
     */
    public synchronized static void stopTimer(int tk) {
        if (!Debug.PROFILE) {
            return;
        }
        DatagramSocket sock = socket;
        if (sock != null) {
            stopBuf[0] = (byte) TIMER_STOP;
            stopBuf[1] = (byte) ((tk >> 24) & 0xff);
            stopBuf[2] = (byte) ((tk >> 16) & 0xff);
            stopBuf[3] = (byte) ((tk >> 8) & 0xff);
            stopBuf[4] = (byte) (tk & 0xff);
            try{
                packet.setData(stopBuf);
                sock.send(packet);
            } catch (IOException e) {
                Debug.printStackTrace(e);
            }
        }
    }

    /**
     * Send a message packet
     *
     * @param buf       Buffer holding the message.
     *                  This byte array
     *                  should be obtained from makeMessage().
     *
     * @see #makeMessage(String)
     * @see Profile
     */
    public static synchronized void sendMessage(byte[] buf) {
        if (!Debug.PROFILE) {
            return;
        }
        DatagramSocket sock = socket;
        if (sock != null) {
            buf[0] = (byte) MESSAGE;
            try {
                packet.setData(buf);
                sock.send(packet);
            } catch (IOException e) {
                Debug.printStackTrace(e);
            }
        }
    }
}
