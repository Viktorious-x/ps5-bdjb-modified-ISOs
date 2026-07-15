
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



package com.hdcookbook.grin.features;

import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.util.Debug;

import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Represents a group of features that are all activated at the same
 * time.  It's useful to group features
 * together so that they can be turned on and off as a unit within
 * an assembly.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class Group extends Feature implements Node {
   
    // The parts of this group as present in the original scene graph:
    private Feature[] parts;

    // The parts that are currently visible within this group.  This
    // will be identical to parts, unless it has been modified by
    // a call from and xlet to resetVisibleParts().
    //
    // See the documentation of resetVisibleParts()
    //
    private Feature[] visibleParts;

    private boolean activated = false;

    // 
    // Number of features checked so far for needsMoreSetup()
    //
    private int numSetupChecked;

    public Group(Show show) {
        super(show);
    }

    /**
     * {@inheritDoc}
     **/
    protected Feature createClone(HashMap clones) {
        if (!isSetup() || activated) {
            throw new IllegalStateException();
        }
        Group result = new Group(show);
        result.parts = new Feature[parts.length];
        result.numSetupChecked = numSetupChecked;
        for (int i = 0; i < parts.length; i++) {
            result.parts[i] = parts[i].makeNewClone(clones);
        }
            // result.activated remains false
        return result;
            // No initializeClone() of this feature is needed.
    }

    /**
     * {@inheritDoc}
     **/
    protected void initializeClone(Feature original, HashMap clones) {
        super.initializeClone(original, clones);
        Group other = (Group) original;
        if (other.visibleParts == other.parts) {
            visibleParts = parts;
        } else {
            visibleParts = new Feature[other.visibleParts.length];
            for (int i = 0; i < visibleParts.length; i++) {
                Feature f = other.visibleParts[i];
                visibleParts[i] = Feature.clonedReference(f, clones);
            }
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void addSubgraph(HashSet set) {
        if (set.contains(this)) {
            return;             // Avoid O(n^2) with assemblies 
        }
        super.addSubgraph(set);
        for (int i = 0; i < parts.length; i++) {
            parts[i].addSubgraph(set);
        }
    }

    /**
     * Get the parts that make up this group in the original scene graph.
     * A copy of the internal parts array is returned; the caller may not
     * modify this array.
     **/
    public Feature[] getParts() {
        return parts;
    }

    /**
     * Set the parts that make up this group.  This may only be
     * called when the object is initially being populated.
     **/
    protected void setParts(Feature[] parts) {
        this.parts = parts;
        this.visibleParts = parts;
    }

    /**
     * {@inheritDoc}
     **/
    public int getX() {
        int x = Integer.MAX_VALUE;
        for (int i = 0; i < visibleParts.length; i++) {
            int val = visibleParts[i].getX();
            if (val < x) {
                x = val;
            }
        }
        return x;
    }

    /**
     * {@inheritDoc}
     **/
    public int getY() {
        int y = Integer.MAX_VALUE;
        for (int i = 0; i < visibleParts.length; i++) {
            int val = visibleParts[i].getY();
            if (val < y) {
                y = val;
            }
        }
        return y;
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the segments.
     **/
    public void initialize() {
        // The show will initialize our sub-features, so we don't
        // need to do anything here.
    }

    /**
     * Free any resources held by this feature.  It is the opposite of
     * setup; each call to setup() shall be balanced by
     * a call to unsetup(), and they shall *not* be nested.  
     * <p>
     * It's possible an active segment may be destroyed.  For example,
     * the last segment a show is in when the show is destroyed will
     * probably be active (and it will probably be an empty segment
     * too!).
     **/
    public void destroy() {
        // The show will destroy our sub-features, so we don't
        // need to do anything here.
    }

    /**
     * Re-sets the parts that are visible in this group to a new set.  
     * This method may be used to make features created with cloneSubgraph()
     * visible.  Indeed, with the feeatures built into GRIN, this is the only
     * way to display a cloned feature.  Such features don't need to be set 
     * up in the way  that normal features are (via Feature.setup() which is
     * balanced by Feature.unsetup()), but they do need to be cloned from
     * features that <i>have</i> been set up.
     * <p>
     * This method may be called by
     * xlet code, but it <b>must only</b> be called within a command 
     * body or inside of Director.notifyNextFrame().
     * <p>
     * If called with a non-null argument, then this group must be in
     * the set up state.  If the argument is null and we're not set up,
     * then we must also not be activated.
     * <p>
     * If this group node is activated, then the new child features of this
     * node will be activated by calling this method, and the old children
     * will be deactivated.  This is done synchronously, within this method.
     * <p>
     * This method relies on resetVisiblePartsNoAssert(Feature[]) after 
     * performing appropriate parameter and state checks.
     *
     * @param visibleParts      An array of parts.  We take
     *                          ownership of the array.  A value of null
     *                          re-sets this group to its original state.
     *
     * @see com.hdcookbook.grin.Feature#cloneSubgraph(java.util.HashMap)
     * @see #resetVisiblePartsNoAssert(Feature[])
     **/
    public void resetVisibleParts(Feature[] visibleParts) {
        if (Debug.ASSERT && !isSetup()) {
            if (visibleParts != null || activated) {
                Debug.assertFail();
            }
        }
        if (Debug.ASSERT && visibleParts != null) {
            //
            // Check that each child's graph is disjoint from every other child's.
            // This property is necessary for the correctness of the scene graph,
            // which must be a directed acyclic graph where each node can only
            // be active through one path at any given time.
            //
            // Note that even with this assertion, a programmer could force a
            // scene graph that's invalid, e.g. by having a group that contains
            // two children, each a group, and then by populating each child group
            // with the same set of nodes as its sibling.  To catch that,
            // we could do a global validity check of the entire tree every time
            // resetVisibleParts() is called on any group, but that seems like
            // overkill; this assetion as it stands will hopefully catch all 
            // unintentional  programmer errors.
            //
            // No assertion is needed if we're re-setting the Group to its original
            // state, because that set was already checked for us by 
            // SEDoubleUseChecker.
            //
            HashSet union = null;
            for (int i = 0; i < visibleParts.length; i++) {
                HashSet child = new HashSet();
                visibleParts[i].addSubgraph(child);
                if (union == null) {
                    union = child;
                } else {
                    for (Iterator it = child.iterator(); it.hasNext(); ) {
                        Object f = it.next();
                        if (union.contains(f)) {
                            Debug.assertFail("Invalid cloned scene graph - "
                                             + " see comments in Group");
                        }
                        union.add(f);
                    }
                }
            }
        }

        resetVisiblePartsNoAssert(visibleParts);
    }
    
    /**
     * Re-sets the parts that are visible in this group to a new set
     * without performing any of the assertion checks.
     * 
     * @param visibleParts      An array of parts.  We take
     *                          ownership of the array.  A value of null
     *                          re-sets this group to its original state.
     *
     * @see #resetVisibleParts(Feature[])
     */
    public void resetVisiblePartsNoAssert(Feature[] visibleParts) {
        if (visibleParts == null) {
            visibleParts = parts;
        }
        if (activated) {
            for (int i = 0; i < visibleParts.length; i++) {
                visibleParts[i].activate();
            }
            for (int i = 0; i < this.visibleParts.length; i++) {
                this.visibleParts[i].deactivate();
            }
        }
        this.visibleParts = visibleParts;       
    }

    /**
     * {@inheritDoc}
     **/
    protected void setActivateMode(boolean mode) {
        // This is synchronized to only occur within model updates.
        activated = mode;
        if (mode) {
            for (int i = 0; i < visibleParts.length; i++) {
                visibleParts[i].activate();
            }
        } else {
            for (int i = 0; i < visibleParts.length; i++) {
                visibleParts[i].deactivate();
            }
        }
    }

    /**
     * {@inheritDoc}
     **/
    protected int setSetupMode(boolean mode) {
        //
        // Note that setup is  only done on the original scene graph;
        // cloned features are exempt from setup/unsetup.  This is
        // enforced by only doing setup on the original scene graph,
        // and by only setting cloned features into a scene graph by
        // virtue of a group.
        //
        if (mode) {
            numSetupChecked = 0;
            int num = 0;
            for (int i = 0; i < parts.length; i++) {
                num += parts[i].setup();
            }
            return num;
        } else {
            for (int i = 0; i < parts.length; i++) {
                parts[i].unsetup();
            }
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     **/
    public boolean needsMoreSetup() {
        //
        // See note about cloned features in setSetupMode()
        //
        while (numSetupChecked < parts.length) {
            if (parts[numSetupChecked].needsMoreSetup()) {
                return true;
            }
            numSetupChecked++;
                // Once a part doesn't need more setup, it will never go
                // back to needing setup until we call unsetup() then
                // setup().  numSetupChecked is re-set to 0 just before
                // callin setup() on our part, so this is safe.  Note
                // that the contract of Feature requires that setup()
                // be called before needsMoreSetup() is consulted.
                //
                // This optimization helps speed the calculation of
                // needsMoreSetup() in the case where a group or an
                // assembly is the child of multiple parts of an assembly.
                // With this optimization, a potential O(n^2) is turned
                // into O(n) (albeit typically with a small n).
        }
        return false;
    }

    /**
     * {@inheritDoc}
     **/
    public void markDisplayAreasChanged() {
        for (int i = 0; i < visibleParts.length; i++) {
            visibleParts[i].markDisplayAreasChanged();
                // Even if visibleParts changes in this frame, this will
                // be correct, because the only way a DrawRecord could fail
                // to be marked as changed is if it were active in both
                // the previoius and the next frame.
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void addDisplayAreas(RenderContext context) {
        for (int i = 0; i < visibleParts.length; i++) {
            visibleParts[i].addDisplayAreas(context);
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
        for (int i = 0; i < visibleParts.length; i++) {
            visibleParts[i].paintFrame(gr);
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void nextFrame() {
        for (int i = 0; i < visibleParts.length; i++) {
            visibleParts[i].nextFrame();
        }
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException 
    {
        in.readSuperClassData(this);
        setParts(in.readFeaturesArrayReference());
    }
}
