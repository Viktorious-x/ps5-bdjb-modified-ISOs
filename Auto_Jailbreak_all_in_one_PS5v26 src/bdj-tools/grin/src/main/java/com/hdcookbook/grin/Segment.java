
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

import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.commands.ActivateSegmentCommand;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.RCKeyEvent;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;

import java.awt.Graphics2D;
import java.io.IOException;
import java.awt.Rectangle;

/**
 * A segment within a show.  A show is composed of segments, and at all
 * times exactly one segment is active.  When a segment is active, its
 * features are showing, and its remote control handlers receive events.
 * When a new feature is activated, any features that are active in both
 * segments are not re-initialized, so that animations will just continue,
 * for example.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class Segment implements Node {

    private final static Rectangle[] EMPTY_RECTANGLE_ARRAY = new Rectangle[0];

    protected String name;
    protected Show show;
    protected Feature[] activeFeatures;
    private boolean[] featureWasActivated;
    protected Feature[] settingUpFeatures;
    protected Command[] onEntryCommands;
    protected boolean nextOnSetupDone;
    protected Command[] nextCommands;
    protected RCHandler[] rcHandlers;
    /** 
     * The bitmask is formed by or-ing the set of remote control key presses
     * this segment has handlers for.
     **/
    protected int rcPressedInterest;
    /** 
     * The bitmask is formed by or-ing the set of remote control key releases
     * this segment has handlers for.
     **/
    protected int rcReleasedInterest;
    /**
     * This bitmask is formed by or-ing the set of key typed events
     * this segment has handlers for.
     **/
    protected int keyTypedInterest;

    protected Rectangle[] mouseRects = null;   
	// hit zones on screen for the mouse.  Populated on demand.

    private boolean active = false;
    private boolean segmentSetupComplete;

    private ActivateSegmentCommand cmdToActivate;
    private ActivateSegmentCommand cmdToActivatePush;

    private int outstandingSetups;

    private int setupCheckedInSetup;    
    private int setupCheckedInActive;
        // # of features in setup clause and activate clause that have
        // been checked so far for setup

    public Segment() {
    }
    
    public String toString() {
        if (Debug.LEVEL > 0) {
            if (name == null) {
                return "segment @" + Integer.toHexString(hashCode());
            } else {
                return "segment " + name;
            }
        } else {
            return super.toString();
        }
    }

    /**
     * Get the name of this segment.  Public segments all have names.
     * Private segments may have names -- as of this writing, the binary
     * writer always wrote out all segment names, because this doesn't
     * cost much and it's so useful for debugging.  However, it would be
     * valid to have a null name.
     **/
    public String getName() {
        return name;
    }
    
    /**
     * Set this segment's name.  This should only be called when a Segment
     * is first created, and should probably never be called by GRIN client
     * code.
     * 
     * @param name  The segment's name (possibly null)
     */
    public void setName(String name) {
        this.name = name;
    }

    public Show getShow() {
        return show;
    }

    // package-private
    void setShow(Show show) {
        this.show = show;
    }

    /**
     * Initialize up this segment.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the segments.
     **/
    public void initialize() {
        featureWasActivated = new boolean[activeFeatures.length];
        for (int i = 0; i < featureWasActivated.length; i++) {
            featureWasActivated[i] = false;
        }
    }

    //
    // For use by Show.activateSegment().  We create it lazily, but
    // if we ever create a command we keep it, to avoid creating
    // garbage.  The same command is pretty likely to be used multiple
    // times.
    //
    // This is synchronized, but Segment is an internal object that is
    // only locked for brief periods of time, without making external
    // calls, so this is safe.  It's intentionally not synchronized on
    // show.
    //
    synchronized Command getCommandToActivate(boolean push) {
        if (push) {
            if (cmdToActivatePush == null) {
                cmdToActivatePush 
                        = new ActivateSegmentCommand(show, true, false);
                cmdToActivatePush.setup(this);
            }
            return cmdToActivatePush;
        } else {
            if (cmdToActivate == null) {
                cmdToActivate = new ActivateSegmentCommand(show);
                cmdToActivate.setup(this);
            }
            return cmdToActivate;
        }
    }


    /* package-private
     * 
     * Activate this segment, that is, cause it to start presenting.
     * This will not take long; all real work is deferred
     * to worker threads.
     * <p>
     * This call is synchronized by the Show.
     *
     * @param   lastSegment     The last segment we're coming from.
     **/
    void activate(Segment lastSegment) {
        if (Debug.LEVEL > 1) {
            Debug.println("Going from segment " + lastSegment + " to " + this);
        }
        if (lastSegment == this) {
            return;
        }
        active = true;
        segmentSetupComplete = false;
        outstandingSetups = 0;
        setupCheckedInSetup = 0;
        setupCheckedInActive = 0;
        /* 
         * Reset showTopGroup's parts to a default 0-length array.
         * When all of this Segment's active features finish their
         * set up in runFeatureSetup(), showTopGroup's parts are updated
         * to reflect this Segment's active feature array.
         **/
        show.showTopGroup.resetVisiblePartsNoAssert(null);
        for (int i = 0; i < activeFeatures.length; i++) {
            int needed = activeFeatures[i].setup();
            outstandingSetups += needed;
            if (Debug.LEVEL > 0 
                && (needed > 0 || activeFeatures[i].needsMoreSetup())) 
            {
                Debug.println();
                Debug.println("WARNING:  Feature " + activeFeatures[i]
                              + " in segment " + name 
                              + " wasn't set up on time.");
                Debug.println();
            }
            if (!activeFeatures[i].needsMoreSetup()) {
                activeFeatures[i].activate();
                featureWasActivated[i] = true;
            }
        }
        for (int i = 0; i < settingUpFeatures.length; i++) {
            outstandingSetups += settingUpFeatures[i].setup();
                // Our count of outstanding setups might be low, if some
                // features had already started setting up in a previous
                // segment, but it will never be high.  If it's low, the
                // result will be some wasted CPU time, but correct behavior.
        }
        if (lastSegment != null) {
            lastSegment.deactivate();
        }
        if (rcHandlers != null) {
            for (int i = 0; i < rcHandlers.length; i++) {
                rcHandlers[i].activate(this);
            }
        }
        if (onEntryCommands != null) {
            for (int i = 0; i < onEntryCommands.length; i++) {
                show.runCommand(onEntryCommands[i]);
            }
        }
        outstandingSetups++;    // The one we set up on the next line...
        runFeatureSetup();
    }

    //
    // Called when another segment is activated, and called on the active
    // segment when the show is destroyed.
    //
    void deactivate() {
        active = false;
        for (int i = 0; i < activeFeatures.length; i++) {
            if (featureWasActivated[i]) {
                activeFeatures[i].deactivate();
                featureWasActivated[i] = false;
            }
            activeFeatures[i].unsetup();
        }
        for (int i = 0; i < settingUpFeatures.length; i++) {
            settingUpFeatures[i].unsetup();
        }
        
        show.showTopGroup.resetVisiblePartsNoAssert(null);
    }

    //
    // When a feature is setup, we get this call.  We have to be a
    // little conservative; it's possible that a feature from a
    // previous, stale segment could finish its setup after we
    // become the current segment, so this call really means "one
    // of our features probably finished setup, but we'd better
    // check to be sure."
    //
    // This is externally synchronized by show, and must be.
    //
    void runFeatureSetup() {
        // Check to see if all features in active clause are set up
        if (setupCheckedInActive < activeFeatures.length) {
            while (setupCheckedInActive < activeFeatures.length) {
                if (!featureWasActivated[setupCheckedInActive] 
                    && activeFeatures[setupCheckedInActive].needsMoreSetup()) 
                {
                    return;
                }
                setupCheckedInActive++;
            }
            for (int i = 0; i < activeFeatures.length; i++) {
                if (!featureWasActivated[i]) {
                    activeFeatures[i].activate();
                    featureWasActivated[i] = true;
                }
            }

            // Set the showTopGroup to this Segment's 
            // activeFeature list, unless this segment is the ShowTop segment, 
            // in which its activeFeature node tree already contains 
            // showTopGroup.
            //
            // Note that if this segment has no active features, it's OK
            // that this statement is never executed, because the 
            // showTopGroup's default is to have no active features.
            if (this != show.showTop) {
               show.showTopGroup.resetVisiblePartsNoAssert(activeFeatures);
            }
        }

        outstandingSetups--;    
                // This can actually go negative -- see the comment where
                // it's incremented to see why.
        if (!active || outstandingSetups > 0 || segmentSetupComplete) {
            return;
        }

        // Check if the setup clause is really finished.
        while (setupCheckedInSetup < settingUpFeatures.length) {
            if (settingUpFeatures[setupCheckedInSetup].needsMoreSetup()) {
                return;
            }
            setupCheckedInSetup++;
        }

        segmentSetupComplete = true;
        
        // Now check to see if we should send the next command(s).
        // At this point in the code, all of our features are set up,
        // so we send the next command if we have no active features,
        // or if nextOnSetupDone is true.  nextOnSetupDone will be true
        // if our "next" clause was called "setup_done".
        //
        // A "next" clause gets the "setup_done" behavior because originally,
        // there were only "next" clauses, the idea being that a segment
        // with no active features could only reasonably be used for
        // setup.  Later, "setup_done" was added, e.g. to allow a "loading"
        // animation, but the old behavior was kept for backwards
        // compatibility.

        if (nextOnSetupDone || activeFeatures.length == 0) {
            doSegmentDone();
        }
    }

    void doSegmentDone() {
        // The "segment done" command is sent from a feature within
        // the model update loop; if the next command moves us to
        // a new segment, that will prevent us from getting a second
        // one due to finishing setup.  If it *doesn't* move us to a 
        // new segment, then maybe the show author means to send the 
        // next command more than once.
        if (nextCommands != null) {
            for (int i = 0; i < nextCommands.length; i++) {
                show.runCommand(nextCommands[i]);
            }
        }
    }

    //
    // Called from Show with the Show lock held
    //
    void paintFrame(Graphics2D gr) {
        for (int i = 0; i < activeFeatures.length; i++) {
           activeFeatures[i].paintFrame(gr);
        }
    }

    //
    // Called from Show with the Show lock held.  This adds all the
    // areas that will be drawn this frame.
    //
    void addDisplayAreas(RenderContext context) {
        for (int i = 0; i < activeFeatures.length; i++) {
           activeFeatures[i].addDisplayAreas(context);
        }
    }

    //
    // Called from Show with the Show lock held
    //
    void nextFrameForActiveFeatures() {
        for (int i = 0; i < activeFeatures.length; i++) {
           activeFeatures[i].nextFrame();
        }
    }
    //
    // Called from Show with the Show lock held
    //    
    void nextFrameForRCHandlers() {
        if (rcHandlers != null) {
            for (int i = 0; i < rcHandlers.length; i++) {
                rcHandlers[i].nextFrame();
            }
        }         
    }

    //
    // Called from Show with the Show lock held
    //
    boolean handleKeyPressed(RCKeyEvent re, Show caller) {
        if (rcHandlers == null) {
            return false;
        }
        for (int i = 0; i < rcHandlers.length; i++) {
            if (rcHandlers[i].handleKeyPressed(re, caller)) {
                return true;
            }
        }
        return false;
    }

    //
    // Called from Show with the Show lock held
    //
    boolean handleKeyReleased(RCKeyEvent re, Show caller) {
        if (rcHandlers == null) {
            return false;
        }
        for (int i = 0; i < rcHandlers.length; i++) {
            if (rcHandlers[i].handleKeyReleased(re, caller)) {
                return true;
            }
        }
        return false;
    }
    
    //
    // Called from Show with the Show lock held
    //
    boolean handleKeyTyped(RCKeyEvent re, Show caller) {
        if (rcHandlers == null) {
            return false;
        }
        for (int i = 0; i < rcHandlers.length; i++) {
            if (rcHandlers[i].handleKeyTyped(re, caller)) {
                return true;
            }
        }
        return false;
    }
    
    // Called from show with show lock held
    boolean handleMouse(int x, int y, boolean activate) {
        if (rcHandlers == null) {
            return false;
        }
        boolean handled = false;
        for (int i = 0; i < rcHandlers.length; i++) {
            if (rcHandlers[i].handleMouse(x, y, activate)) {
                handled = true;
            }
        }
        return handled;
    }
    
    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
                
        in.readSuperClassData(this);
        this.activeFeatures = in.readFeaturesArrayReference();
        this.settingUpFeatures = in.readFeaturesArrayReference();
        this.rcHandlers = in.readRCHandlersArrayReference();
        this.onEntryCommands = in.readCommands();
        this.nextOnSetupDone = in.readBoolean();
        this.nextCommands = in.readCommands();
        this.rcPressedInterest = in.readInt();
        this.rcReleasedInterest = in.readInt();
        this.keyTypedInterest = in.readInt();
    }

    /**
     * Get the list of active features of this segment.  Normally client
     * code shouldn't call this, but it is needed for building debugging
     * tools, like grinview.
     **/
    public Feature[] getActiveFeatures() {
        return activeFeatures;
    }

    /**
     * Get the list of features in the setup clause of this segment.
     * Normally client code shouldn't call this, but it is needed for
     * building debugging tools, like grinview.
     **/
    public Feature[] getSetupFeatures() {
        return settingUpFeatures;
    }

    /**
     * Get the list of commands that are called when this segment is activated.
     * Normally client code shouldn't call this, but it is needed for
     * building debugging tools, like grinview.
     **/
    public Command[] getOnEntryCommands() {
        return onEntryCommands;
    }

    /**
     * Do we trigger the commands in our next clause when all of the
     * features in our setup clause have finished loading? 
     *
     * @return the answer to that question.
     **/
    public boolean getNextOnSetupDone() {
        return nextOnSetupDone;
    }

    /**
     * Give the commands in our next clause.  This can be triggered
     * by setup being done, or by a segment_done command.
     * Normally client code shouldn't call this, but it is needed for
     * building debugging tools, like grinview.
     *
     * @see #getNextOnSetupDone()
     * @see com.hdcookbook.grin.Show#segmentDone()
     **/
    public Command[] getNextCommands() {
        return nextCommands;
    }

    /**
     * Give the set of remote control handlers for this segment.
     * Normally client code shouldn't call this, but it is needed for
     * building debugging tools, like grinview.
     **/
    public RCHandler[] getRCHandlers() {
        return rcHandlers;
    }

    //
    // Called by Show.  Returns the list of mouse interest areas.  If
    // there are none, returns a zero-length array.
    //
    Rectangle[] getMouseRects() {
	if (mouseRects == null) {
	    // Calculate the list of hit zones for the mouse.
	    //
	    // In GRIN, we normally try very hard to avoid creating heap
	    // traffic, so data structures like this are normally stored
	    // in the .grin file.  However, we only expect mouse events to
	    // be generated on fast players, where this isn't nearly as much
	    // of a concern.
	    int numRects = 0;
	    for (int i = 0; i < rcHandlers.length; i++) {
		Rectangle[] r = rcHandlers[i].getMouseRects();
		if (r != null) {
		    if (mouseRects == null) {
			mouseRects = r;		// it's immutable
		    }
		    numRects += r.length;
		}
	    }
	    if (mouseRects == null) {
		mouseRects = EMPTY_RECTANGLE_ARRAY;
	    } else if (numRects > mouseRects.length) {
		mouseRects = new Rectangle[numRects];
		int i = 0;
		for (int j = 0; j < rcHandlers.length; j++) {
		    Rectangle[] r = rcHandlers[j].getMouseRects();
		    if (r != null) {
			for (int k = 0; k < r.length; k++) {
			    mouseRects[i++] = r[k];
			}
		    }
		}
	    }
	}
	return mouseRects;
    }
}
