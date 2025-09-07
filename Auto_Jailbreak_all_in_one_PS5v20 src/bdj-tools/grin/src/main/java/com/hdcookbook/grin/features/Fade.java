
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
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.io.IOException;
import java.util.HashMap;

/**
 * Modifies a child feature by applying an alpha value when drawing in
 * it.  This lets you animate a fade-in and fade-out effect.  It works
 * by specifying alpha values at a few keyframes, and doing linear
 * interpolation between those keyframes.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class Fade extends Modifier implements Node {

    private AlphaComposite[] alphas;
    private AlphaComposite opaqueAlpha = null;
 
    /*
     * keyframes and keyAlphas are read-only, hence they are
     * reconstructed using GrinDataInputStream.getSharedIntArray().
     */
    protected int[] keyframes;
    protected int[] keyAlphas;
    protected boolean srcOver;
    protected int repeatFrame;  // Integer.MAX_VALUE for "stick at end"
    private int alphaIndex;
    protected int loopCount;    
        // # of times to repeat images before sending end commands
        // Integer.MAX_VALUE means "infinite"
    private int loopsRemaining; // see loopCount
    protected Command[] endCommands;
    private AlphaComposite currAlpha;
    private AlphaComposite lastAlpha;

        //
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
            if (srcOver) {
                r.setSemiTransparent();
            }
            if (currAlpha != lastAlpha) {
                r.setChanged();
            }
            parent.addArea(r);
        }

        public void guaranteeAreaFilled(DrawRecord r) {
            if (!srcOver || currAlpha == opaqueAlpha || currAlpha == null) {
                parent.guaranteeAreaFilled(r);
            }
        }

        public int setTarget(int target) {
            return parent.setTarget(target);
        }

    };  // End of RenderContext anonymous inner class

    public Fade(Show show) {
        super(show);
    }  

    /**
     * {@inheritDoc}
     **/
    protected Feature createClone(HashMap clones) {
        if (activated || alphas == null) {
            throw new IllegalStateException();
        }
        Fade result = new Fade(show);
        result.part = part.makeNewClone(clones);
        result.alphas = alphas;
        result.opaqueAlpha = opaqueAlpha;
        result.srcOver = srcOver;
        result.repeatFrame = repeatFrame;
        result.alphaIndex = alphaIndex;
        result.loopCount = loopCount;
        result.loopsRemaining = loopsRemaining;
        result.currAlpha = currAlpha;
        result.lastAlpha = lastAlpha;
        return result;
    }

    /**
     * {@inheritDoc}
     **/
    protected void initializeClone(Feature original, HashMap clones) {
        super.initializeClone(original, clones);
        Fade other = (Fade) original;
        endCommands = Feature.cloneCommands(other.endCommands, clones);
    }

    
    /**
     * {@inheritDoc}
     **/
    public void initialize() {
        if (keyframes.length == 1) {
            AlphaComposite ac = show.initializer.getAlpha(srcOver,keyAlphas[0]);
            alphas = new AlphaComposite[] { ac };
        } else {
            alphas = new AlphaComposite[keyframes[keyframes.length-1]+1];
            int i = 0;          // keyframes[i] <= f < keyframes[i+1]
            for (int f = 0; f < alphas.length; f++) {
                // Restore invariant on i
                while ((i+1) < keyframes.length && f >= keyframes[i+1]) {
                    i++;
                }
                int alpha;
                if (f == keyframes[i]) {
                    alpha = keyAlphas[i];
                } else {
                    int dist = keyframes[i+1] - keyframes[i];
                    int distNext = keyframes[i+1] - f;
                    int distLast = f - keyframes[i];
                    if (Debug.ASSERT && (distNext < 0 || distLast < 0)) {
                        Debug.assertFail();
                    }
                    alpha = (keyAlphas[i+1]*distLast + keyAlphas[i]*distNext + dist/2) / dist;
                }
                alphas[f] = show.initializer.getAlpha(srcOver, alpha);
                if (opaqueAlpha == null && alpha == 255) {
                    opaqueAlpha = alphas[f];
                }
            }
        }
    }

    /**
     * Sets the value of this fade's AlpahComposite to value.  This method
     * may be called by xlet code, so long as it's called within a 
     * command body or inside of Director.notifyNextFrame().  It is 
     * an error to call this method if more than one keyframe is defined
     * for this fade, and trying to do so may result in an assertion 
     * failure.  In other words, if you want to programmatically control a 
     * value, don't also try to control it by defining multiple keyframes.
     **/
    public final void setAlpha(AlphaComposite ac) {
        if (Debug.ASSERT && alphas.length != 1) {
            Debug.assertFail();         // This is a value that is interpolated
        }
        alphas[0] = ac;
        currAlpha = ac;
        srcOver = ac.getRule() != AlphaComposite.SRC;
    }

    /**
     * {@inheritDoc}
     **/
    protected void setActivateMode(boolean mode) {
        super.setActivateMode(mode);
        if (mode) {
            alphaIndex = 0;
            lastAlpha = null;
            currAlpha = alphas[alphaIndex];
            loopsRemaining = loopCount;
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void nextFrame() {
        super.nextFrame();
        if (alphaIndex == Integer.MAX_VALUE) {
            return;
        }
        alphaIndex++;
        if (alphaIndex == alphas.length) {
            if (loopCount != Integer.MAX_VALUE) {
                loopsRemaining--;
            }
            if (loopsRemaining > 0) {
                if (repeatFrame == Integer.MAX_VALUE) {
                    alphaIndex = 0;
                } else {
                    alphaIndex = repeatFrame;
                }
            } else {
                loopsRemaining = loopCount;
                show.runCommands(endCommands);
                alphaIndex = repeatFrame;
            }
        }
        if (alphaIndex < alphas.length) {
            currAlpha = alphas[alphaIndex];
        }
    }


    /**
     * {@inheritDoc}
     **/
    public void addDisplayAreas(RenderContext context) {
        childContext.parent = context;
        super.addDisplayAreas(childContext);
        lastAlpha = currAlpha;
    }

    /**
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
        if (currAlpha != null) {
            Composite old = gr.getComposite();
            gr.setComposite(currAlpha);
            part.paintFrame(gr);
            gr.setComposite(old);
        } else {
            part.paintFrame(gr);
        }
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException 
    {
        in.readSuperClassData(this);
        this.srcOver = in.readBoolean();
        this.keyframes = in.readSharedIntArray();
        this.keyAlphas = in.readSharedIntArray();
        this.repeatFrame = in.readInt();
        loopCount = in.readInt();
        this.endCommands = in.readCommands();     
    }
}
