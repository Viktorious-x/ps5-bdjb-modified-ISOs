
/*  
 * Copyright (c) 2007, Sun Microsystems, Inc.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A SetupManager manages a low-priority thread that's used to set up
 * GRIN features, by doing things like loading images.  This way, image
 * loading and other setup work can proceed in the background, while
 * other features (that are already set up) animate or are otherwise
 * active.
 * <p>
 * At present, all setup work is serialized into one thread, but a
 * future extension might allow for concurrent execution of setup
 * activity (e.g. for a dual-core system or suchlike).
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class SetupManager implements Runnable {

    private SetupClient[] settingUp;
    int first;       // index into setting up.  Wraps.
    int lastPlusOne; // index into setting up.  Wraps.
    private static Object monitor = new Object();
    private static SetupManager worker = null;
    private ArrayList managers;
        // ArrayList<SetupManager>, contains all running managers managed
        // by a given thread.  This is null for most SetupManager instances,
        // but populated for the worker singleton.
        //
        // Making this an instance variable rather than a static only
        // costs four bytes, and is a simple way to avoid a race condition
        // if the number of managers briefly becomes 0, then becomes
        // > 0 again.

    private static byte[] profileSetup; // Profiling setup calls

    static {
        if (Debug.PROFILE && Debug.PROFILE_SETUP) {
            profileSetup = Profile.makeProfileTimer("doSomeSetup()");
        }
    }

    /**
     * Create a SetupManager for setting up a maximum of numFeatures
     * clients.
     **/
    public SetupManager(int numFeatures) {
        managers = null;
        settingUp = new SetupClient[numFeatures + 1];
        first = 0;
        lastPlusOne = 0;
    }

    //
    // Make a "blank" SetupManager to act as a Runnable.  It would be more
    // elegant to have a SetupManagerHelper class or something to do this,
    // but we do it this way to minimize the number of classes
    // that need to be classloaded in an xlet.  Remeber, measurement has
    // shown that classloading time is a significant part of xlet startup
    // in BD.
    //
    private SetupManager() {
        managers = new ArrayList(4);
            // A capacity of 4 is small enough so the memory doesn't matter,
            // and big enough so it will almost never grow.  Managers has
            // as many entries as there are active shows, which will usually
            // be 1 or 2, and should never be large.
    }

    /**
     * Start providing service from this SetupManager.  This must be
     * balanced by a call to stop().
     *
     * @see #stop()
     **/
    public void start() {
        synchronized(monitor) {
            if (worker == null) {
                worker = new SetupManager();
                Thread t = new Thread(worker, "SetupManager");
                t.setDaemon(true);
                t.setPriority(3);
                t.start();
            }
            if (Debug.ASSERT && worker.managers.indexOf(this) != -1) {
                // Called start() twice on the same SetupManager
                Debug.assertFail();
            }
            worker.managers.add(this);
            monitor.notifyAll();
        }
    }

    /**
     * Stop providing service from this SetupManager.  Once stopped, a
     * manager cannot be re-started.
     *
     * @see #start()
     **/
    public void stop() {
        synchronized(monitor) {
            int i = worker.managers.indexOf(this);
            if (Debug.ASSERT && i < 0) {
                Debug.assertFail();
            }
            if (i >= 0) {
                worker.managers.remove(i);
            }
            if (worker.managers.size() == 0) {
                monitor.notifyAll();
                // run() will set worker null, unless more work
                // arrives before it gets scheduled.
            }
        }
    }

    public void scheduleSetup(SetupClient f) {
        synchronized(monitor) {
            int nextLastPlusOne = next(lastPlusOne);
            if (first == nextLastPlusOne) {
                //
                // This is a little complicated to explain...  settingUp
                // is intentionally one bigger than the maximum number of
                // clients we can possibly have.  However, there's an
                // outside possibility that a feature might call scheduleSetup()
                // a second time, before the first setup is done -- this might
                // happen if the feature goes out of setting up state, and
                // back into it, before it gets a chance to set up.
                //
                // Normally, this is harmless - there would just be two
                // entries on settingUp, and the thread would get to them
                // in order.  The contract of SetupClient says to just
                // return if no setup is required, so the second time it's
                // called would be a NOP.  However, it's just
                // possible that the duplicate entries on settingUp might
                // cause us to fill up the array.
                //
                // In this rare case, we need to purge the array of
                // duplicates.  Since it's rare, we go ahead and
                // create heap traffic by using a HashSet.
                HashSet set = new HashSet();
                for (int i = first; i != lastPlusOne; i = next(i)) {
                    set.add(settingUp[i]);
                }
                set.add(f);
                if (Debug.ASSERT && set.size() >= settingUp.length) {
                    // If this happens, settingUp is too small, but it
                    // can't be, because it's one bigger than the total
                    // number of features.
                    Debug.assertFail();
                }
                first = 0;
                lastPlusOne = 0;
                for (Iterator it = set.iterator(); it.hasNext(); ) {
                    settingUp[lastPlusOne] = (SetupClient) it.next();
                    lastPlusOne++;
                }
                for (int i = lastPlusOne; i < settingUp.length; i++) {
                    settingUp[i] = null;
                }
            } else {
                // Otherwise, we've got room in settingUp, so we just
                // append
                settingUp[lastPlusOne] = f;
                lastPlusOne = nextLastPlusOne;
            }
            monitor.notifyAll();
        }
    }

    private int next(int i) {
        i++;
        if (i >= settingUp.length) {
            return 0;
        } else {
            return i;
        }
    }

    private boolean hasWork() {
        return first != lastPlusOne;
    }
    
    private void doWork() {
        SetupClient work;
        synchronized(monitor) {
            if (!hasWork()) {
                return;
            }
            work = settingUp[first];
        }
        if (work.needsMoreSetup()) {
            int tok;
            if (Debug.PROFILE && Debug.PROFILE_SETUP) {
                tok = Profile.startTimer(profileSetup, Profile.TID_SETUP);
            }
            work.doSomeSetup();
            if (Debug.PROFILE && Debug.PROFILE_SETUP) {
                Profile.stopTimer(tok);
            }
            // The check of needsMoreSetup() above isn't strictly necessary,
            // but it is possible that it's false (due to the settingUp
            // array being purged of duplicates).  That's admittedly rare,
            // but calling doSomeSetup() unnecessarily reduces the value
            // of an optimization in Show.  It's also counter-intuitive
            // that doSomeSetup() could be called even if needsMoreSetup()
            // returns false, so doing the test makes it so that can't
            // happen (unless, of course, another thread changes the
            // state of the feature in the intervening time...  but
            // that _is_ something a developer should expect to need
            // to cope with).
        }
        synchronized(monitor) {
            if (hasWork() && !settingUp[first].needsMoreSetup()) {
                // There's a slight chance that settingUp changed out from
                // under us, but even if it did, this logic is safe.  At
                // worst, the client we just set up is somewhere else in
                // settingUp, and will therefore be quickly eliminated when
                // we get to it in the future.
                settingUp[first] = null;
                first = next(first);
            }
        }
    }

    /**
     * This isn't really public; it's only called by our worker thread.
     **/
    public void run() {
        if (Debug.LEVEL > 0) {
            Debug.println("Setup thread starts.");
        }
        SetupManager found = null;
        for (;;) {
            synchronized(monitor) {
                if (managers.size() == 0) {
                    if (Debug.ASSERT && worker != this) {
                        Debug.assertFail();
                    }
                    worker = null;
                    break;      // exits thread
                }
                if (found != null && !found.hasWork()) {
                    found = null;
                }
                for (int i = 0; found == null && i < managers.size(); i++)  {
                    SetupManager m = (SetupManager) managers.get(i);
                    if (m.hasWork()) {
                        found = m;
                    }
                }
                if (found == null) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException ex) {
                        break;          // bail out of thread
                    }
                }
            }
            if (found != null) {
                found.doWork();
            }
        }
        if (Debug.LEVEL > 0) {
            Debug.println("Setup thread exits.");
        }
    }
}


