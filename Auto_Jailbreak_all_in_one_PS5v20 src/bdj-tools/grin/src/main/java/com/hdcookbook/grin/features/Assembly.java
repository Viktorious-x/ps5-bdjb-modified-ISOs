
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
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;

import java.io.IOException;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.HashSet;

/**
 * An assembly is a feature composed of other features.  It's a bit
 * like a switch statement:  Only one child of an assembly can be
 * active at a time.  This can be used to compose a menu that can
 * be in one of several visual states.  Often the parts of an assembly
 * are groups.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class Assembly extends Feature implements Node {

    protected String[] partNames;
    protected Feature[] parts;
    protected Feature currentFeature = null;
    protected boolean activated = false;

    // 
    // Number of features checked so far for needsMoreSetup()
    //
    private int numSetupChecked;

    public Assembly(Show show) {
        super(show);
    }

    public void setParts(String[] partNames, Feature[] parts) { 
        this.partNames = partNames;
        this.parts = parts;
        currentFeature = parts[0];
    }

    /**
     * {@inheritDoc}
     **/
    protected Feature createClone(HashMap clones) {
        if (!isSetup() || activated) {
            throw new IllegalStateException();
        }
        Assembly result = new Assembly(show);
        int found = -1;
        result.partNames = partNames;
        result.parts = new Feature[parts.length];
        for (int i = 0; i < parts.length; i++) {
            if (currentFeature == parts[i]) {
                found = i;
            }
            result.parts[i] = parts[i].makeNewClone(clones);
        }
        if (found != -1) {
            result.currentFeature = result.parts[found];
        }
        result.numSetupChecked = numSetupChecked;
            // result.activated remains false
        return result;
            // No initializeClone() of this feature is needed.
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
     * {@inheritDoc}
     **/
    public int getX() {
        return currentFeature.getX();
    }

    /**
     * {@inheritDoc}
     **/
    public int getY() {
        return currentFeature.getY();
    }

    /**
     * Get the names of our parts.
     **/
    public String[] getPartNames() {
        return partNames;
    }

    /**
     * Get our parts, that is, our child features.
     **/
    public Feature[] getParts() {
        return parts;
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

    //
    // This is synchronized to only occur within model updates.
    //
    protected void setActivateMode(boolean mode) {
        activated = mode;
        if (mode) {
            currentFeature.activate();
        } else {
            currentFeature.deactivate();
        }
    }

    /**
     * This should not be directly called by clients of the GRIN
     * framework, unless it is done from the animation thread (within
     * a command body, or inside an implementation of Director.nextFrame()).  
     * Calls are synchronized to only occur within
     * model updates, with the show lock held.  Normally, clients of
     * the GRIN framewwork will want to set the current feature with: <pre>
     *     show.runCommand(new ActivatePartCommand(assembly, part))
     * </pre>
     * <p>
     * This really should have been called setCurrentPart() for symmetry
     * with getCurrentPart().  Sorry about that!
     *
     * @see #getCurrentPart()
     **/
    public void setCurrentFeature(Feature feature) {
        synchronized(show) {    // It's already held, but this is harmless
            if (currentFeature == feature) {
                return;
            }
            if (activated) {
                feature.activate();
                currentFeature.deactivate();
                show.getDirector().notifyAssemblyPartSelected
                                    (this, feature,  currentFeature, activated);
            }
            currentFeature = feature;
        }
    }

    /**
     * {@inheritDoc}
     **/
    protected int setSetupMode(boolean mode) {
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
     * Find the part of the given name in this assembly, or
     * null if it can't be found.
     **/
    public Feature findPart(String name) {
        for (int i = 0; i < parts.length; i++) {
            if (partNames[i].equals(name)) {
                return parts[i];
            }
        }
        return null;
    }

    /** 
     * Get the currently active part within this assembly.
     *
     * @see #setCurrentFeature(com.hdcookbook.grin.Feature)
     **/
    public Feature getCurrentPart() {
        return currentFeature;
    }

    /**
     * {@inheritDoc}
     **/
    public void markDisplayAreasChanged() {
        currentFeature.markDisplayAreasChanged();
            // At this point, we're not sure if currentFeature refers to
            // the previous frame or the next frame, but either way will
            // generate correct results, because a feature could only fail
            // to be marked as modified if it was active in both.
    }


    /**
     * {@inheritDoc}
     **/
    public void addDisplayAreas(RenderContext context) {
        currentFeature.addDisplayAreas(context);
    }

    /**
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
        currentFeature.paintFrame(gr);
    }

    /**
     * {@inheritDoc}
     **/
    public void nextFrame() {
        currentFeature.nextFrame();
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {     
                
        in.readSuperClassData(this);
        setParts(in.readStringArray(), in.readFeaturesArrayReference());
    }
}
