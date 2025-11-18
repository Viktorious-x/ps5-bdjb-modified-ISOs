
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
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.animator.AnimationEngine;
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.util.Debug;

import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;
import java.io.IOException;

/**
 * Guarantees that its children will completely fill a given rectangle
 * on the screen with source-mode drawing.  In other words, this node and
 * its children guarantee that they will completely paint every pixel within
 * a given rectangle.
 * <p>
 * This node has a feature to paint transparent pixels (in the current
 * drawing mode) in rectangular areas.  This allows an author to guarantee
 * filling a large rectangular area composed of small rectangular items
 * (like images drawn in Src mode) that have small gaps between them -- in
 * this case, the GuaranteeFill node can fill in those gaps.  This node
 * paints those fill areas before it paints its children.
 * <p>
 * If this node is a child of a node that sets SrcOver drawing mode,
 * then the guarantee will not apply.  In other words, it's OK as
 * far as correctness is concerned to put a structure including a
 * GuaranteeFill node under a SrcOver node or a Fade node, but you
 * won't see any increase in redraw efficiency due to the GuaranteeFill
 * node in this case.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class GuaranteeFill extends Modifier implements Node {

        // Here, we make an inner class of RenderContext.  We
        // pass this instance to our child; it modifies calls to the
        // parent RenderContext from our child.
        //

    protected Rectangle guaranteed;     // Guaranteed area
    protected Rectangle[] fills;                // The areas we need to fill
    private DrawRecord drawRecord = new DrawRecord();

    
    public GuaranteeFill(Show show) {
        super(show);
    }

    /**
     * {@inheritDoc}
     **/
    protected Feature createClone(HashMap clones) {
        if (!isSetup() || activated) {
            throw new IllegalStateException();
        }
        GuaranteeFill result = new GuaranteeFill(show);
        result.part = part.makeNewClone(clones);
        result.guaranteed = guaranteed;
        result.fills = fills;
        return result;
        // No initializeClone() of this feature is needed.
    }

    /**
     * {@inheritDoc}
     **/
    protected void setChildChanged() {
        // When we're deactivated, our child doesn't need to be marked as
        // modified, because we don't change the drawing at all.
    }

    /**
     * {@inheritDoc}
     **/
    public void addDisplayAreas(RenderContext context) {
        drawRecord.setArea(guaranteed.x, guaranteed.y, 
                           guaranteed.width, guaranteed.height);
        context.guaranteeAreaFilled(drawRecord);
        super.addDisplayAreas(context);
    }

    /**
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
        if (fills != null) {
            gr.setColor(AnimationEngine.transparent);
            for (int i = 0; i < fills.length; i++) {
                Rectangle a = fills[i];
                gr.fillRect(a.x, a.y, a.width, a.height);
            }
        }
        part.paintFrame(gr);
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
                
        in.readSuperClassData(this);
        this.guaranteed = in.readRectangle();
        this.fills = in.readRectangleArray();
    }
}
