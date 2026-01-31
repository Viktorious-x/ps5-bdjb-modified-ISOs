
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
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;

import java.awt.Graphics2D;
import java.io.IOException;
import java.util.HashMap;

/**
 * A Translator wraps other features, and adds movement taken from a
 * translator model (represented at runtime by an InterpolatedModel)
 * to it.  The subfeature is translated by the given amount.  For backwards
 * compatibility reasons, there's also an "absolute" mode that attempts
 * to use the coordinates of the upper-left hand corner of the child, but
 * this mode is deprecated.
 *
 * @see InterpolatedModel
 *
 * @author Bill Foote (http://jovial.com)
 */
public class Translator extends Modifier implements Node {

    /**
     * The field number in our model for the X coordinate
     *
     * @see com.hdcookbook.grin.features.InterpolatedModel
     **/
    public final static int X_FIELD = 0;

    /**
     * The field number in our model for the Y coordinate
     *
     * @see com.hdcookbook.grin.features.InterpolatedModel
     **/
    public final static int Y_FIELD = 1;

    /** 
     * A special value to use to translate something offscreen.
     * Set the x or the y coordinate to this value if you want it
     * hidden.  Numerically, it's Integer.MIN_VALUE.
     **/
    public final static int OFFSCREEN = Integer.MIN_VALUE;

    protected InterpolatedModel model;

    protected int fx = 0;       // Feature's start position (if absolute model)
    protected int fy = 0;
    protected boolean modelIsRelative;  // false if absolute.

    private int dx;             // For this frame
    private int dy;

    private int lastDx;         // For last frame shown
    private int lastDy;

    private DrawRecord drawRecord = new DrawRecord();

        //
        // Here, we make an inner class of RenderContext.  We
        // pass this instance to our child; it modifies calls to the
        // parent RenderContext from our child.
        //
    private ChildContext childContext = new ChildContext();

    class ChildContext extends RenderContext {
        RenderContext   parent;

        public void addArea(DrawRecord r) {
            r.applyTranslation(dx, dy);
            if (dx != lastDx || dy != lastDy) {
                r.setChanged();
            }
            parent.addArea(r);
        }

        public void guaranteeAreaFilled(DrawRecord r) {
            r.applyTranslation(dx, dy);
            parent.guaranteeAreaFilled(r);
        }

        public int setTarget(int target) {
            return parent.setTarget(target);
        }
    };  // End of RenderContext anonymous inner class
    
    public Translator(Show show) {
        super(show);
    }

    /**
     * {@inheritDoc}
     **/
    protected Feature createClone(HashMap clones) {
        if (!isSetup() || activated) {
            throw new IllegalStateException();
        }
        Translator result = new Translator(show);
        result.part = part.makeNewClone(clones);
        result.fx = fx;
        result.fy = fy;
        result.modelIsRelative = modelIsRelative;
        result.dx = dx;
        result.dy = dy;
        result.lastDx = lastDx;
        result.lastDy = lastDy;
        return result;
    }

    /**
     * {@inheritDoc}
     **/
    protected void initializeClone(Feature original, HashMap clones) {
        super.initializeClone(original, clones);
        Translator other = (Translator) original;
        model = (InterpolatedModel)
                Feature.clonedReference(other.model, clones);
    }

    /**
     * Get the translation that moves us
     **/
    public InterpolatedModel getModel() {
        return model;
    }

    /**
     * {@inheritDoc}
     **/
    public void initialize() {
        super.initialize();
    }
    /**
     * {@inheritDoc}
     **/
    public void destroy() {
        super.destroy();
    }

    /**
     * {@inheritDoc}
     **/
    public void nextFrame() {
        if (Debug.ASSERT && !model.getIsActivated()) {
            Debug.assertFail();
        }
        super.nextFrame();

        // Note that at this point, we don't know if our model
        // has advanced to the next frame or not, so we can't depend
        // on its value
    }


    /**
     * {@inheritDoc}
     **/
    public void addDisplayAreas(RenderContext context) {
        dx = model.getField(X_FIELD);
        dy = model.getField(Y_FIELD);
        if (dx == OFFSCREEN || dy == OFFSCREEN) {
            return;
        }
        if (!modelIsRelative) {
            dx -= fx;
            dy -= fy;
        }
        childContext.parent = context;
        part.addDisplayAreas(childContext);
        lastDx = dx;
        lastDy = dy;
    }

    /**
     * {@inheritDoc}
     **/
    public int getX() {
        int x = model.getField(X_FIELD);
        if (!modelIsRelative) {
            x -= fx;
        }
        x += part.getX();
        return x;
    }

    /**
     * {@inheritDoc}
     **/
    public int getY() {
        int y = model.getField(Y_FIELD);
        if (!modelIsRelative) {
            y -= fy;
        }
        y += part.getY();
        return y;
    }


    /**
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
        if (dx == OFFSCREEN || dy == OFFSCREEN) {
            return;
        }
        gr.translate(dx, dy);
        part.paintFrame(gr);
        gr.translate(-dx, -dy);
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
                
        in.readSuperClassData(this);
        this.fx = in.readInt();
        this.fy = in.readInt();
        this.modelIsRelative = in.readBoolean();
        this.model = (InterpolatedModel) in.readFeatureReference();
    }
}
