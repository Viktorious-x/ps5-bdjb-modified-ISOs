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

package com.hdcookbook.grin.input;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;
import java.awt.Rectangle;
import java.io.IOException;

/**
 * A VisualRCHandler models interaction with the remote control and the
 * mouse as a grid of cells.  Each cell can contain a state, but it
 * can instead contain an address of another cell.  When the user
 * navigates from one cell to another, he lands on the indicated
 * state, or if he lands on a cell that contains the address of another
 * cell, he goes to that cell.  A given state can only occupy one cell.
 * <p>
 * No wrapping behavior is specified:  If a user tries to navigate off
 * the edge of the grid, the state remains unchanged.  If that isn't
 * the desired UI behavior, then simply create a grid where the border
 * consists of cell addresses.
 * <p>
 * A visual handler may optionally be tied to an assembly.  When this
 * handler is activated, the handler is put into the state determined by
 * the assembly, by finding the state that corresponding to the 
 * assembly's currently active part.  Once a handler is active,
 * it assumes that it's the only one in control fo the assembly; it
 * doesn't check for the assembly changing state out from under it.
 * <p>
 * A handler can also optionally have commands associated with its
 * states.  These are invoked only when the handler changes state - a
 * command is <i>not</i> sent for the current state when the handler
 * is activated.  When commands are used to maintain the scene graph's
 * state, it's up to the application to ensure that the UI
 * is in a state that matches the handler's state before the handler
 * is activated.
 * <p>
 * The state of a handler is determined by the activated flag, and by
 * the named state.  It may be safely changed with a set_visual_rc command.
 * Once the handler is activated, it stays in that state until reset
 * by a set_visual_rc command, a call to setState(...), or the user
 * navigating with the arrow keys.  If the handler is activated and
 * the user presses the enter key again, the handler is "re-activated;"
 * the activation commands are executed again, and
 * the assembly's state is set to selected then immediately to activated,
 * in order to re-run any activation animation.
 * 
 *
 * @author Bill Foote (http://jovial.com)
 */

public class VisualRCHandler extends RCHandler implements Node {
 
    /**
     * A special value in a grid that means to activate the current
     * state
     **/
    public final static int GRID_ACTIVATE = 0xffff;

    protected static int MASK = RCKeyEvent.KEY_UP.getBitMask()
                                | RCKeyEvent.KEY_DOWN.getBitMask()
                                | RCKeyEvent.KEY_RIGHT.getBitMask()
                                | RCKeyEvent.KEY_LEFT.getBitMask()
                                | RCKeyEvent.KEY_ENTER.getBitMask();
    protected int[] upDown;    // For each state, the most significant 16 bits
                             // contains the state to go to on "up", and the
                             // least significant 16 bits the "down" value.
                             // If this has the special value GRID_ACTIVATE, 
                             // then there's no movement, but the feature is
                             // activated.

    protected int[] rightLeft; // For each state, the most significant 16 bits
                             // contains the state to go to on "right", and the
                             // least significant 16 bits the "left" value.
                             // If this has the special value GRID_ACTIVATE, 
                             // then there's no movement, but the feature is
                             // activated.

    protected int[][] upDownAlternates;     // alternate grid, 
    protected int[][] rightLeftAlternates;  // cf. visual_grid_alternate
    protected String[] gridAlternateNames;  // non-null, can be length 0

    protected String[] stateNames;   // The names corresponding to state numbers.
    protected Assembly assembly;     // can be null
    protected Feature[] selectFeatures; // By state #, array can be null, and
                                      // any element can be null.
    protected Command[][] selectCommands; // By state #, array can be null, and
                                      // any element can be null.
    protected Feature[] activateFeatures;  // by state #, etc.
    protected Command[][] activateCommands;  // by state #
    protected Rectangle[] mouseRects;  // hit zones on screen for the mouse
    protected int[] mouseRectStates;   // The state # corresponding to each rect
    protected int timeout;      // -1 means "no timeout"
    protected Command[] timeoutCommands;

    /**
     * Flag that, if true, makes the handler start out in the selected
     * state.  When the handler is activated, if the underlying assembly
     * is found to be in an activated state, it is coerced into the 
     * corresponding selected state.
     **/
    protected boolean startSelected = false;

    private boolean activated = false;
    private int currState = 0;
    private int currFrame;
    private boolean timedOut;
    
    public VisualRCHandler() {
        super();
    }
    
    /**
     * Give a developer-friendly string describing this handler.
     * Useful for development.
     **/
    public String toString() {
        return super.toString() + "(" + getName() + ")";
    }

    private boolean handlesActivation() {
        return activateFeatures != null || activateCommands != null;
    }

    /**
     * This is intended for applications that wish to query the UI
     * state.  The value of this will not be changed as long as a lock
     * is held on our show.
     **/
    public boolean getActivated() {
        return activated;
    }

    /**
     * This is intended for applications that wish to query the UI
     * state.  The value of this will not be changed as long as a lock
     * is held on our show.
     **/
    public int getState() {
        return currState;
    }

    /**
     * Get the name of a numbered state.
     **/
    public String getStateName(int stateNum) {
        return stateNames[stateNum];
    }

    /**
     * Lookup a state number by name.  Used for parsing, or by xlets.
     *
     * @return -1 if not found
     **/
    public int lookupState(String name) {
        for (int i = 0; i < stateNames.length; i++) {
            if (stateNames[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Look up an alternate grid by name.  Used for parsing, or by xlets.
     *
     * @return -1 if not found
     **/
    public int lookupGrid(String gridAlternateName) {
        for (int i = 0; i < gridAlternateNames.length; i++) {
            if (gridAlternateNames[i].equals(gridAlternateName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     **/
    public boolean handleKeyPressed(RCKeyEvent ke, Show caller) {
        if ((ke.getBitMask() & MASK) == 0) {
            return false;
        }
        synchronized(show) {
            int newState = 0;
            if (ke == ke.KEY_ENTER) {
                newState = GRID_ACTIVATE;
            } else if (ke == ke.KEY_UP) {
                newState = (upDown[currState] >> 16) & 0xffff;
            } else if (ke == ke.KEY_DOWN) {
                newState = upDown[currState] & 0xffff;
            } else if (ke == ke.KEY_RIGHT) {
                newState = (rightLeft[currState] >> 16) & 0xffff;
            } else if (ke == ke.KEY_LEFT) {
                newState = rightLeft[currState] & 0xffff;
            } else if (Debug.ASSERT) {
                Debug.assertFail();
            }
            if (newState == GRID_ACTIVATE) {
                if (!handlesActivation()) {
                    return false;
                }
                setState(-1, true, true);
                return true;
            } else {
                setState(newState, false, true);
                return true;
            }
        }
    }

    /**
     * {@inheritDoc}
     **/
    public boolean handleKeyReleased(RCKeyEvent ke, Show caller) {
        // ignored
        return false;
    }

    /**
     * {@inheritDoc}
     **/
    public boolean handleKeyTyped(RCKeyEvent ke, Show caller) {
        return false;
    }
    
    /**
     * {@inheritDoc}
     **/
    public boolean handleMouse(int x, int y, boolean activate) {
        if (mouseRects == null) {
            return false;
        }
        // Mouse events probably only occur on pretty high-end
        // players, and there probably aren't may rects, so a
        // simple linear search is fine.
        for (int i = 0; i < mouseRects.length; i++) {
            if (mouseRects[i].contains(x, y)) {
                setState(mouseRectStates[i], activate, true);
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     **/
    public Rectangle[] getMouseRects() {
	return mouseRects;
    }
    
    /**
     * Called from InvokeVisualCellCommand, and from internal methods.
     * This is synchronized on our show, to only occur during model
     * updates.  This method may also be called from a java_command or
     * from the director, within the animation thread.
     *
     * @param newState       New state, -1 means "current"
     * @param newActivated   New value for activated
     * @param runCommands    If true, run the commands normally associated
     *                       with entering this state due to a keypress.
     **/
    public void setState(int newState, boolean newActivated,
                         boolean runCommands) 
    {
        setState(newState, newActivated, runCommands, -1);
    }

    /**
     * Called from InvokeVisualCellCommand, and from internal methods.
     * This is synchronized on our show, to only occur during model
     * updates.  This method may also be called from a java_command or
     * from the director, within the animation thread.
     *
     * @param newState       New state, -1 means "current"
     * @param newActivated   New value for activated
     * @param runCommands    If true, run the commands normally associated
     *                       with entering this state due to a keypress.
     * @param gridAlternate  Alternate grid # to select, 0..max, or -1 to
     *                       leave grid unchanged.
     **/
    public void setState(int newState, boolean newActivated,
                         boolean runCommands, int gridAlternate) 
    {
        synchronized(show) {
            if (gridAlternate != -1)  {
                upDown = upDownAlternates[gridAlternate];
                rightLeft = rightLeftAlternates[gridAlternate];
            }
            if (newState == GRID_ACTIVATE) {
                newState = currState;
                newActivated = true;
            } else if (newState == -1) {
                newState = currState;
            }
            if (newState == currState && newActivated == activated) {
                if (activated) {
                    // If activated, re-run any animations by
                    // briefly setting the assembly to the selected
                    // state.
                    setState(newState, false, false);
                } else {
                    return;
                }
            }
            if (Debug.LEVEL > 1) {
                Debug.println("RC handler state becomes " 
                              + stateNames[newState]);
            }
            Feature[] fs = newActivated ? activateFeatures : selectFeatures;
            Command[][] cs = newActivated ? activateCommands : selectCommands;
            if (fs != null && fs[newState] != null) {
                assembly.setCurrentFeature(fs[newState]);
                if (Debug.LEVEL > 1) {
                    Debug.println("    Setting assembly to " + fs[newState]);
                }
            }
            if (runCommands && cs != null) {
                Command[] arr = cs[newState];
                if (arr != null) {
                    for (int i = 0; i < arr.length; i++) {
                        show.runCommand(arr[i]);
                    }
                }
            }
            currState = newState;
            activated = newActivated;
        } // end synchronized
    }

    /**
     * {@inheritDoc}
     **/
    public void activate(Segment s) {
        timedOut = timeout <= -1;
        currFrame = 0;
        if (assembly != null) {
                // If we have an assembly, make our state mirror
                // that of the assembly.
            Feature curr = assembly.getCurrentPart();
            int i = lookForFeature(curr, selectFeatures);
            if (i != -1) {
                currState = i;
                activated = false;
            } else  {
                i = lookForFeature(curr, activateFeatures);
                if (i != -1) {
                    currState = i;
                    if (startSelected) {
                        assembly.setCurrentFeature(selectFeatures[i]);
                        activated = false;
                    } else {
                        activated = true;
                    }
                } else if (Debug.LEVEL > 0) {
                    Debug.println("Handler " + getName()
                                  + " can't find current assembly state");
                }
            }
        }
    }

    private int lookForFeature(Feature f, Feature[] fs) {
        if (fs == null) {
            return -1;
        }
        for (int i = 0; i < fs.length; i++) {
            if (fs[i] == f) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     **/
    public void nextFrame() {
        currFrame++;
        if (!timedOut && currFrame > timeout) {
            timedOut = true;
            for (int i = 0; i < timeoutCommands.length; i++) {
                show.runCommand(timeoutCommands[i]);
            }
        }
    }
    
    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
        
        in.readSuperClassData(this);
        
        gridAlternateNames = in.readStringArray();
        upDownAlternates = new int[in.readInt()][];
        rightLeftAlternates = new int[upDownAlternates.length][];
        for (int i = 0; i < upDownAlternates.length; i++) {
            upDownAlternates[i] = in.readSharedIntArray();
            rightLeftAlternates[i] = in.readSharedIntArray();
        }
        upDown = upDownAlternates[0];
        rightLeft = rightLeftAlternates[0];
        stateNames = in.readStringArray();
        if (in.isNull()) {
            this.selectCommands = null;
        } else {
            this.selectCommands = new Command[in.readInt()][];
            for (int i = 0; i < selectCommands.length; i++) {
                this.selectCommands[i] = in.readCommands();
            }
        }
        if (in.isNull()) {
            activateCommands = null;
        } else {
            activateCommands = new Command[in.readInt()][];
            for (int i = 0; i < activateCommands.length; i++) {
                activateCommands[i] = in.readCommands();
            }
        }
        
        this.mouseRects = in.readRectangleArray();
        this.mouseRectStates = in.readIntArray();
        this.timeout = in.readInt();
        this.timeoutCommands = in.readCommands();
        
        if (in.readBoolean()) {
            this.assembly = (Assembly)in.readFeatureReference();
        }    
        this.selectFeatures = in.readFeaturesArrayReference();
        this.activateFeatures = in.readFeaturesArrayReference();      
        this.startSelected = in.readBoolean();
    }
}
