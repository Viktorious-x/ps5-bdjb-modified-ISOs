
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
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.util.Debug;

import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;

/**
 * Represents a clipped version of another feature.  When painting, a
 * clipping rectangle is set.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class Clipped extends Modifier implements Node {

    protected Rectangle clipRegion;
    private Rectangle lastClipRegion = new Rectangle();
        // Last clip region during paint
    private Rectangle tmpI = null;
        //
        // Here, we make an inner class of RenderContext.  We
        // pass this instance to our child; it modifies calls to the
        // parent RenderContext from our child.
        //
    private ChildContext childContext = new ChildContext();
    
    class ChildContext extends RenderContext {
        RenderContext   parent;

        public void addArea(DrawRecord r) {
            r.addClip(clipRegion.x, clipRegion.y, 
                      clipRegion.width, clipRegion.height);
            parent.addArea(r);
        }

        public void guaranteeAreaFilled(DrawRecord r) {
            r.addClip(clipRegion.x, clipRegion.y, 
                      clipRegion.width, clipRegion.height);
            parent.guaranteeAreaFilled(r);
        }

        public int setTarget(int target) {
            return parent.setTarget(target);
        }

    };  // End of RenderContext anonymous inner class


    
    public Clipped(Show show) {
        super(show);
    }

    /**
     * {@inheritDoc}
     **/
    protected Feature createClone(HashMap clones) {
        if (!isSetup()) {
            throw new IllegalStateException();
        }
        Clipped result = new Clipped(show);
        if (clipRegion != null) {
            result.clipRegion = new Rectangle(clipRegion);
        }
        result.part = part.makeNewClone(clones);
        return result;
    }

    /**
     * Change the region being clipped.  This should only be called from
     * the animation thread, when it is safe to update the model (e.g.
     * from a show command).
     *
     * @param newRegion The new clip region.  
     **/
    public void changeClipRegion(Rectangle newRegion) {
        clipRegion.x = newRegion.x;
        clipRegion.y = newRegion.y;
        clipRegion.width = newRegion.width;
        clipRegion.height = newRegion.height;
        // The drawing framework takes care of the new clip region
        // automatically - if we're drawing more or less of our child
        // as the result of the clip, it notices and does the right
        // thing.
    }

    /**
     * {@inheritDoc}
     **/
    public void addDisplayAreas(RenderContext context) {
        childContext.parent = context;
        super.addDisplayAreas(childContext);
    }


    /**
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
        // This is synchronized by Show.paintFrame, so we don't
        // have to worry about concurrent calls.
        lastClipRegion.x = Integer.MIN_VALUE;
        gr.getClipBounds(lastClipRegion);
        if (lastClipRegion.x == Integer.MIN_VALUE) {
            gr.setClip(clipRegion);
            part.paintFrame(gr);
            gr.setClip(null);
        } else {
            if (tmpI == null) {
                tmpI = new Rectangle();         // Holds intersection
            }
            tmpI.setBounds(lastClipRegion);
            if (tmpI.x < clipRegion.x) {
                tmpI.width -= clipRegion.x - tmpI.x;
                tmpI.x = clipRegion.x;
            }
            if (tmpI.y < clipRegion.y) {
                tmpI.height -= clipRegion.y - tmpI.y;
                tmpI.y = clipRegion.y;
            }
            if (tmpI.x + tmpI.width > clipRegion.x + clipRegion.width) {
                tmpI.width = clipRegion.x + clipRegion.width - tmpI.x;
            }
            if (tmpI.y + tmpI.height > clipRegion.y + clipRegion.height) {
                tmpI.height = clipRegion.y + clipRegion.height - tmpI.y;
            }
            if (tmpI.width > 0 && tmpI.height > 0) {
                gr.setClip(tmpI);
                part.paintFrame(gr);
                gr.setClip(lastClipRegion);
            }
        }
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
                
        in.readSuperClassData(this);
        this.clipRegion = in.readRectangle();   
    }
}
