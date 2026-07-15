
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

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Color;
import java.util.HashMap;
import com.hdcookbook.grin.animator.RenderContext;


/**
 * Display text.  Like all features, the upper-left hand corner of
 * the visible text is given.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class Box extends Feature implements Node {
   
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected int outlineWidthX;        // in x dimension
    protected int outlineWidthY;        // in y dimension
    protected Color outlineColor;
    protected Color fillColor;
    protected InterpolatedModel scalingModel = null;
    protected Rectangle scaledBounds = null;

    private boolean isActivated;
    private DrawRecord drawRecord = new DrawRecord();
    private boolean boxPropertiesChanged = false;

    public Box(Show show) {
        super(show);
    }

    /**
     * {@inheritDoc}
     **/
    protected Feature createClone(HashMap clones) {
        if (!isSetup() || isActivated) {
            throw new IllegalStateException();
        }
        Box result = new Box(show);
        result.x = x;
        result.y = y;
        result.width = width;
        result.height = height;
        result.outlineWidthX = outlineWidthX;
        result.outlineWidthY = outlineWidthY;
        result.outlineColor = outlineColor;
        result.fillColor = fillColor;
        if (scaledBounds != null) {
            result.scaledBounds = new Rectangle(scaledBounds);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     **/
    protected void initializeClone(Feature original, HashMap clones) {
        super.initializeClone(original, clones);
        Box other = (Box) original;
        scalingModel = (InterpolatedModel)
                Feature.clonedReference(other.scalingModel, clones);
    }

    /**
     * {@inheritDoc}
     **/
    public int getX() {
        return x;
    }

    /**
     * {@inheritDoc}
     **/
    public int getY() {
        return y;
    }

    /**
     * Resizes the box.
     * <p>
     * This should not be directly called by clients of the GRIN
     * framework, unless it is done from the animation thread (within
     * a command body, or inside an implementation of Director.nextFrame()).
     * Calls are synchronized to only occur within
     * model updates, with the show lock held.
     **/
    public void resize(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        boxPropertiesChanged = true;
    }

    /**
     * Resizes the outline around the box.
     * <p>
     * This should not be directly called by clients of the GRIN
     * framework, unless it is done from the animation thread (within
     * a command body, or inside an implementation of Director.nextFrame()).
     * Calls are synchronized to only occur within
     * model updates, with the show lock held.
     **/
    public void resizeOutline(int outlineWidthX, int outlineWidthY) {
        this.outlineWidthX = outlineWidthX;
        this.outlineWidthY = outlineWidthY;
        boxPropertiesChanged = true;
    }

    /**
     * Changes the color of the box.
     * <p>
     * This should not be directly called by clients of the GRIN
     * framework, unless it is done from the animation thread (within
     * a command body, or inside an implementation of Director.nextFrame()).
     * Calls are synchronized to only occur within
     * model updates, with the show lock held.
     **/
    public void changeBoxColor(Color c) {
        this.fillColor = c;
        boxPropertiesChanged = true;
    }

    /**
     * Changes the color of the box outline.
     * <p>
     * This should not be directly called by clients of the GRIN
     * framework, unless it is done from the animation thread (within
     * a command body, or inside an implementation of Director.nextFrame()).
     * Calls are synchronized to only occur within
     * model updates, with the show lock held.
     **/
    public void changeBoxOutlineColor(Color c) {
        this.outlineColor = c;
        boxPropertiesChanged = true;
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the segments.
     **/
    public void initialize() {
    }

    /**
     * {@inheritDoc}
     **/
    public void destroy() {
    }


    /**
     * {@inheritDoc}
     **/
    protected void setActivateMode(boolean mode) {
        //
        // This is synchronized to only occur within model updates.
        //
        isActivated = mode;
    }

    /**
     * {@inheritDoc}
     **/
    protected int setSetupMode(boolean mode) {
        return 0;
    }

    /**
     * {@inheritDoc}
     **/
    public boolean needsMoreSetup() {
        return false;
    }

    /**
     * {@inheritDoc}
     **/
    public void nextFrame() {
    }

    /**
     * {@inheritDoc}
     **/
    public void markDisplayAreasChanged() {
        drawRecord.setChanged();
    }

    /**
     * {@inheritDoc}
     **/
    public void addDisplayAreas(RenderContext context) {
        if (scalingModel == null) {
            drawRecord.setArea(x, y, width, height);
        } else {
            boolean changed 
                = scalingModel.scaleBounds(x, y, width, height, scaledBounds);
                    // When newly activated, we might get a false positive
                    // on changed, but that's OK because our draw area is
                    // changed anyway.
            drawRecord.setArea(scaledBounds.x, scaledBounds.y, 
                               scaledBounds.width, scaledBounds.height);
            if (changed) {
                drawRecord.setChanged();
            }
        }
        if (boxPropertiesChanged) {
            drawRecord.setChanged();
            boxPropertiesChanged = false;
        }
        drawRecord.setSemiTransparent();
        context.addArea(drawRecord);
    }

    /**
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
        if (!isActivated) {
            return;
        }
        int x1;
        int y1;
        int w;
        int h;
        if (scalingModel == null) {
            x1 = x;
            y1 = y;
            w = width;
            h = height;
        } else {
            x1 = scaledBounds.x;
            y1 = scaledBounds.y;
            w = scaledBounds.width;
            if (w < 0) {
                w = -w;
                x1 -= w;
            }
            h = scaledBounds.height;
            if (h < 0) {
                h = -h;
                y1 -= h;
            }
            // We don't scale outlineWidth.  This would be complicated
            // to do, and it's not likely to be what's meant anyway.
        }
        int x2 = x1 + w - 1;
        int y2 = y1 + h - 1;
        if ((outlineWidthX > 0 || outlineWidthY > 0) && outlineColor != null) {
            gr.setColor(outlineColor);
            int tx = outlineWidthX;
            int t2x = 2*tx;
            int ty = outlineWidthY;
            int t2y = 2*ty;
            gr.fillArc(x1, y1, t2x, t2y, 90, 90);       // upper-left
            gr.fillArc(x1, y2-t2y, t2x, t2y, 180, 90);  // lower-left
            gr.fillArc(x2-t2x, y2-t2y, t2x, t2y, 270, 90); // lower-right
            gr.fillArc(x2-t2x, y1, t2x, t2y, 0, 90);    // upper-right
            // Issue #4 - subtract the right and bottom most pixels by one
            gr.fillRect(x1, y1+ty, tx, h-t2y-1);        // left
            gr.fillRect(x1+tx, y2-ty+1, w-t2x-1, ty);   // bottom
            gr.fillRect(x2-tx+1, y1+ty, tx, h-t2y-1);   // right
            gr.fillRect(x1+tx, y1, w-t2x-1, ty);        // top
            x1 += tx;
            y1 += ty;
            w -= t2x;
            h -= t2y;
        }
        if (fillColor != null) {
            gr.setColor(fillColor);
            gr.fillRect(x1, y1, w, h); 
        }
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
                
        in.readSuperClassData(this);
        
        this.x = in.readInt();
        this.y = in.readInt();
        this.width = in.readInt();
        this.height = in.readInt();
        this.outlineWidthX = in.readInt();
        this.outlineWidthY = in.readInt();
        this.outlineColor = in.readColor();
        this.fillColor = in.readColor();   
        if (in.readBoolean()) {
            this.scalingModel = (InterpolatedModel) in.readFeatureReference();
            this.scaledBounds = new Rectangle();
        }       
    }
}
