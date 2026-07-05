
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
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.HashMap;

/**
 * Causes its child to be painted in SRC_OVER mode, that is, with
 * graphic-to-graphics alpha blending.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class SrcOver extends Modifier implements Node {

        // Here, we make an inner class of RenderContext.  We
        // pass this instance to our child; it modifies calls to the
        // parent RenderContext from our child.
        //
    private ChildContext childContext = new ChildContext();
    
    class ChildContext extends RenderContext {
        RenderContext   parent;
        private int x;
        private int y;
        private int width;
        private int height;

        public void addArea(DrawRecord r) {
            r.setSemiTransparent();
            parent.addArea(r);
        }

        public void guaranteeAreaFilled(DrawRecord r) {
            // Nothing - our semi-transparent children can't guarantee
            // that anything gets filled.
        }

        public int setTarget(int target) {
            return parent.setTarget(target);
        }

    };  // End of RenderContext anonymous inner class

    
    public SrcOver(Show show) {
        super(show);
    }

    /**
     * {@inheritDoc}
     **/
    protected Feature createClone(HashMap clones) {
        if (!isSetup() || activated) {
            throw new IllegalStateException();
        }
        SrcOver result = new SrcOver(show);
        result.part = part.makeNewClone(clones);
        return result;
            // initializeClone() not needed
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
        Composite old = gr.getComposite();
        gr.setComposite(AlphaComposite.SrcOver);
        part.paintFrame(gr);
        gr.setComposite(old);
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {  
                
        in.readSuperClassData(this);
        // nothing to do...
    }
}
