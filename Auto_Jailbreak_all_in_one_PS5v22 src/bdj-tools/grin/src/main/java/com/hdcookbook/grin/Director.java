
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

package com.hdcookbook.grin;

import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.util.Debug;

/**
 * This class is a supertype that xlets can subclass to interact with a show.
 * The java_command commands can access the directory and do a downcast as
 * a way of getting into the xlet.  Director also defines various protected
 * methods to notify the xlet of Show state changes, and to allow the xlet to
 * insert itself into the animation loop.  Shows that are created without a
 * director will be given a default director that is a direct instance of
 * this class.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/

public class Director {

    private Show show;

    /**
     * Create a new Director.
     **/
    public Director()  {
    }
    
    //
    // Called from Show constructor
    //
    void setShow(Show show) {
        this.show = show;
    }

    /**
     * Get the show we're managing.
     **/
    public Show getShow() {
        return show;
    }

    /**
     * Get the named public feature from the show we're managing.  If not
     * found and if we're in debug mode, trigger an assertion failure.
     * This method does
     * a search through the show's features using a hashtable lookup, 
     * so it shouldn't be called
     * frequently; it's best used during initialization.
     **/
    public Feature getFeature(String name) {
        Feature f = getShow().getFeature(name);
        if (Debug.ASSERT && f == null) {
            Debug.assertFail("Feature \"" + name + "\" not found.");
        }
        return f;
    }

    /**
     * Get the named public command (or command list) from the show we're
     * managing.  If not found and if we're in debug mode, trigger an assertion
     * failure.  This method does a search through the show's named commands
     * using a hashtable lookup, so it shouldn't be called too frequently;
     * it's best used during initialization.
     * <p>
     * The returned Command
     * object can be used in a call to runCommand(Command) on the show
     * from whene it came.  If you send a named command to a different
     * show, the results are undefined.
     **/
    public Command getNamedCommand(String name) {
        Command c = getShow().getNamedCommand(name);
        if (Debug.ASSERT && c == null) {
            Debug.assertFail("Named command \"" + name + "\" not found.");
        }
        return c;
    }

    /**
     * Look up the named part in the given assembly.  If not found and if
     * we're in debug mode, trigger an assertion failure.  This method does
     * a search through the assembly's parts, so it shouldn't be called
     * frequently; it's best used during initialization.
     **/
    public Feature getPart(Assembly assembly, String partName) {
        Feature[] parts = assembly.getParts();
        String[] partNames = assembly.getPartNames();
        for (int i = 0; i < parts.length; i++) {
            if (partName.equals(partNames[i])) {
                return parts[i];
            }
        }
        if (Debug.ASSERT) {
            Debug.assertFail();
        }
        return null;
    }

    /**
     * Look up the given named public segment in the show we're managing.
     * If not found and if we're in debug mode, trigger an assertion failure.
     * This method does
     * a search through the show's features using a hashtable lookup, 
     * so it shouldn't be called
     * frequently; it's best used during initialization.
     **/
    public Segment getSegment(String name) {
        Segment s = getShow().getSegment(name);
        if (Debug.ASSERT && s == null) {
            Debug.assertFail("Segment \"" + name + "\" not found.");
        }
        return s;
    }


    /**
     * Is this director interested in KEY_TYPED EVENTS?  This method may be
     * called during Xlet startup, shortly after the Director instance is 
     * created, and potentially subsequent to that, e.g. in the system 
     * thread where event delivery occurs.  If this method returns true, 
     * KEY_TYPED events will be queued, and and delivered to the director 
     * in the animation thread via notifyKeyTyped().  The semantics of 
     * event delivery are undefined if this method does not consistently
     * return the same value. A typical and recommend override in a director 
     * would be to return true from this method.
     * <p>
     * Note that {com.hdcookbook.grinxlet.GrinXlet} will only deliver
     * key typed events if the director in effect at xlet startup returns
     * true from this method.  If it is false, events will not be delivered,
     * even if 
     * {com.hdcookbook.grinxlet.GrinXlet#pushKeyInterest(com.hdcookbook.grin.Show)}
     * is subsequently called with a director that returns true.
     * <p>
     * It is unknown if any Blu-ray players or other GEM devices implement
     * the optional support for key typed events.  However, they may in the
     * future, and other environments where GRIN might be applied
     * (such as Android) do have the equivalent of KEY_TYPED events.
     **/
    public boolean wantsKeyTyped() {
        return false;
    }

    /**
     * Notify the director that a key typed event has been received.
     * This will only be called if wantsKeyTyped() returns true, and if the
     * other conditions described in that method are true.  Key typed events
     * are delivered in the animation thread, just like commands.  Director
     * instances interested in key typed events should override this method.
     * <p>
     * This Director-based handling of key typed events is unrelated to
     * any mechanism based on an RC handler and subclassing of
     * {com.hdcookbook.grin.input.RCKeyEvent}.  That mechanism was never
     * implemented in the core GRIN library in the HD Cookbook project, though
     * minimal support was provided so that it could be implemented through
     * subclassing.
     *
     * @see java.awt.event.KeyEvent#getKeyChar()
     * @see #wantsKeyTyped()
     *
     * @param   keyChar  The key that was typed
     **/
    public void notifyKeyTyped(char keyChar) {
    }

    /**
     * Notify the director that the mouse has moved.  This method will be
     * called in the animation thread for all shows in the "key" interest
     * table when a mouse move event is received.
     * <p>
     * Mouse event notification to the director occurs independent of, and
     * in addition to, mouse notification via the RC handler mechanism.
     * If a mouse event is "consumed" via the RC handler mechanism, it will
     * still be delivered to all directors in the "key" interest table.
     * <p>
     * Unlike the case with {Director#wantsKeyTyped()}, no method is consulted
     * at xlet startup.  That's because delivery of mouse events is controlled
     * by two flags in the BDJO file.
     *
     * @return true    if the mouse is now in an active area, such that the
     *		       cursor should indicate that pressing would be meaningful.
     *
     * @see #wantsKeyTyped()
     **/
    public boolean notifyMouseMoved(int x, int y) {
	return false;
    }

    /**
     * Notify the director that the mouse has been pressed.  This method will be
     * called in the animation thread for all shows in the "key" interest
     * table when a mouse move event is received.
     * <p>
     * Mouse event notification to the director occurs independent of, and
     * in addition to, mouse notification via the RC handler mechanism.
     * If a mouse event is "consumed" via the RC handler mechanism, it will
     * still be delivered to all directors in the "key" interest table.
     * <p>
     * Unlike the case with {Director#wantsKeyTyped()}, no method is consulted
     * at xlet startup.  That's because delivery of mouse events is controlled
     * by two flags in the BDJO file.
     *
     * @see #wantsKeyTyped()
     **/
    public void notifyMousePressed(int x, int y) {
    }



    /**
     * Notify the director that the model is moving to the next frame.
     * Subclasses that override this method must call super.notifyNextFrame()
     * at least once.  The implementation of this method in Director causes
     * the show to execute all pending commands, which can result in model
     * state changes, such as selecting new segments, changing the selected
     * part in an assembly, etc.  Usually, you'll probably want to call
     * super.notifyNextFrame() first thing, but it may be useful in some
     * circumstances to do some computation before, e.g. something that might
     * result in posting a command to the show.
     * <p>
     * This method is called after the scene graph's model has moved to the 
     * upcoming frame's state.  Xlets that override this method may wish to
     * update user-programmable node values in the body of this method.  This
     * method and the execute() body of a command are the only safe times
     * for user code to update show nodes.
     * <p>
     * This method is called with the show lock held, that is, within a
     * synchronized block in this director's show.  It's essential to hold
     * the show lock when updating the scene graph, e.g. so that changes
     * from remote control keypresses do not happen at the same time.
     * Xlets that override notifyNextFrame() should be careful that any
     * xlet state they access is done in a thread-safe way.
     * <p>
     * If you want to run some Java code to update the scene graph for every
     * frame when the show is in certain states, it might be easier to make
     * a timer that goes off every frame, invoking a java_command.
     **/
    public void notifyNextFrame() {
        show.runPendingCommands();
    }

    /**
     * Notify the director that a new segment has been activated. 
     * <p>
     * This method is called with the show lock held, that is, within a
     * synchronized block in this director's show.  It's essential to hold
     * the show lock when updating the scene graph, e.g. so that changes
     * from remote control keypresses do not happen at the same time.
     * Xlets that override this method should be careful that any
     * xlet state they access is done in a thread-safe way.
     * <p>
     * The default implementation of this method does nothing,
     * so there is no need to call super.notifySegmentActivated().
     *
     * @param newSegment        The new segment that was activated
     * @param oldSegment        The old segment that was previously active
     **/
    public void 
    notifySegmentActivated(Segment newSegment, Segment oldSegment) {
    }


    /**
     * Notify the director that a new part has been selected within an assembly.
     * <p>
     * This method is called with the show lock held, that is, within a
     * synchronized block on this director's show.  It's essential to hold
     * the show lock when updating the scene graph, e.g. so that changes
     * from remote control keypresses do not happen at the same time.
     * Xlets that override this method should be careful that any
     * xlet state they access is done in a thread-safe way.
     * <p>
     * The default implementation of this method does nothing,
     * so there is no need to call super.notifyAssemblyPartSelected().
     * <p>
     * Note that during the callback, the current part of the assembly is
     * undefined, and might not reflect the new value.
     *
     * @param assembly          The assembly within which a new part was
     *                          selected
     * @param newPart           The new part that's now selected
     * @param oldPart           The old part that used to be selected.
     * @param assemblyIsActive  True if assembly is currently active, that
     *                          is, being displayed.
     **/
    public void notifyAssemblyPartSelected(Assembly assembly, 
                                           Feature newPart, Feature oldPart,
                                           boolean assemblyIsActive) 
    {
    }

    /**
     * Notifies the director that the underlying show has been destroyed.
     * This is called from the animation thread with the show lock held.
     * If the director has acquired any resources, it should release them
     * here.
     **/
    public void notifyDestroyed() {
    }

}
