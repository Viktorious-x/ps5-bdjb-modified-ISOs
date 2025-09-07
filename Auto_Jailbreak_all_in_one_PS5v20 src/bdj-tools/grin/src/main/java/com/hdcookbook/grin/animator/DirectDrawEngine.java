
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

package com.hdcookbook.grin.animator;

import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.Profile;
import java.awt.AlphaComposite;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Image;

/**
 * A double-buffered animation engine that uses direct draw.  
 * The engine maintains a BufferedImage that the animation clients
 * paint into for each animation frame.  At the end of each frame,
 * this buffer is blitted to the framebuffer using Component.getGraphics(),
 * and Toolkit.sync() is called.
 * <p>
 * On most players, direct draw is probably the fastest drawing
 * option.  However, direct draw doesn't take advantage of platform
 * double-buffering, that is, platforms where HScene.isDoubleBuffered()
 * returns true.  On such platforms, repaint draw will save memory, and
 * on some implementations of platform double buffering, repaint draw
 * may even prove to be faster.
 * <p>
 * Direct draw also has the drawback (no pun intended) that you can't
 * draw widgets in back of or in front of the area managed by the
 * engine, unless you make sure the right drawing happens yourself.
 * In other words, using direct draw in an area makes it difficult
 * to have widgets (like HButton) occupy overlapping screen real estate.
 **/
public class DirectDrawEngine extends ClockBasedEngine {


    private Container container;
    private Component ddComponent;
    private Image buffer;
    private Graphics2D bufferG;
    private Graphics2D componentG;
    private byte[] profileBlitToFB;     // Profiling model update
    private int engineNumber = 0;
    private static int nextEngineNumber = 0;

    /**
     * Create a new DirectDrawEngine.  It needs to be initialized with
     * the various initXXX methods (including the inherited ones).
     **/
    public DirectDrawEngine() {
        if (Debug.LEVEL > 0 || (Debug.PROFILE && Debug.PROFILE_ANIMATION)) {
            engineNumber = getNextEngineNumber();
        }
        if (Debug.PROFILE && Debug.PROFILE_ANIMATION) {
            profileBlitToFB = Profile.makeProfileTimer("blitToFB("+this+")");
        }
    }

    private synchronized static int getNextEngineNumber() {
        if (Debug.LEVEL > 0 || (Debug.PROFILE && Debug.PROFILE_ANIMATION)) {
            nextEngineNumber++;
        }
        return nextEngineNumber;
    }

    public String toString() {
        if (Debug.LEVEL > 0 || (Debug.PROFILE && Debug.PROFILE_ANIMATION)) {
            return "DD engine " + engineNumber;
        } else {
            return super.toString();
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void initContainer(Container container, Rectangle bounds) {
        this.container = container;
        ddComponent = new Component() {
            public void paint(Graphics g) {
                if (Debug.LEVEL > 0) {
                    Debug.println("repainting...");
                }
                try {
                    repaintFrame((Graphics2D) g);
                        // We could paint from the buffer, but this
                        // will be the same frame anyway.
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }

            }
        };
        ddComponent.setBounds(bounds);
        container.add(ddComponent);
        ddComponent.setVisible(true);
        buffer = AssetFinder.createCompatibleImageBuffer(
                                container, bounds.width, bounds.height);
        bufferG = AssetFinder.createGraphicsFromImageBuffer(buffer);
        bufferG.setComposite(AlphaComposite.Src);
        bufferG.setColor(transparent);
        bufferG.fillRect(0, 0, bounds.width, bounds.height);

        componentG = (Graphics2D) ddComponent.getGraphics();
        if (Debug.ASSERT && componentG == null) {
            Debug.assertFail();  // Maybe container is invisible?
        }
        componentG.setComposite(AlphaComposite.Src);
    }

    /** 
     * {@inheritDoc}
     **/
    public int getWidth() {
        return ddComponent.getWidth();
    }

    /** 
     * {@inheritDoc}
     **/
    public int getHeight() {
        return ddComponent.getHeight();
    }

    /**
     * {@inheritDoc}
     **/
    public Component getComponent() {
        return ddComponent;
    }


    /**
     * {@inheritDoc}
     **/
    protected void clearArea(int x, int y, int width, int height) {
        bufferG.setColor(transparent);
        bufferG.fillRect(x, y, width, height);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is always false for direct draw.  Because we maintian our
     * own double buffer, nothing external can damage its contents.
     **/
    protected boolean needsFullRedrawInAnimationLoop() {
        return false;
    }

    /**
     * {@inheritDoc}
     **/
    protected void callPaintTargets() throws InterruptedException {
        paintTargets(bufferG);
        bufferG.setComposite(AlphaComposite.Src);       // Add some robustness
    }

    /**
     * {@inheritDoc}
     **/
    protected void finishedFrame() {
        int tok;
        if (Debug.PROFILE && Debug.PROFILE_ANIMATION) {
            tok = Profile.startTimer(profileBlitToFB, Profile.TID_ANIMATION);
        }
        int n = renderContext.numDrawTargets;
        if (n > 0) {
            for (int i = 0; i < n; i++) {
                Rectangle a = renderContext.drawTargets[i];
                componentG.drawImage(buffer, a.x, a.y, 
                                             a.x+a.width, a.y+a.height,
                                             a.x, a.y,
                                             a.x+a.width, a.y+a.height,
                                             null);
            }
            Toolkit.getDefaultToolkit().sync();
        }
        if (Debug.PROFILE && Debug.PROFILE_ANIMATION) {
            Profile.stopTimer(tok);
        }
        Thread.currentThread().yield();
    }

    /**
     * {@inheritDoc}
     **/
    protected void terminatingEraseScreen() {
        componentG.setColor(transparent);
        componentG.fillRect(0, 0, getWidth(), getHeight());
        Toolkit.getDefaultToolkit().sync();
        container.remove(ddComponent);
        Image buf = buffer;
        buffer = null;
        bufferG = null;
        AssetFinder.destroyImageBuffer(buf);
    }
}
