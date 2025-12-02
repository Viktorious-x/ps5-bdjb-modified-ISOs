
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
import java.awt.AlphaComposite;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Image;

/**
 * An animation engine that uses repaint draw.  Repaint draw
 * proceeds by calling Component.repaint(), and then waiting
 * for the platform to call our Component.paint(Graphics) method.
 * On most players, it's probably at least a little bit slower
 * than direct draw, due to the thread context switching.  Repaint
 * draw can be significantly slower than direct draw for certain
 * kinds of drawing, because a frame's damage rect is limited to
 * one big rectangle (even when several small ones might do), and
 * because of extra overhead erasing areas of the
 * double buffer that are later drawn to in source mode.
 * <p>
 * Repaint draw has the advantage that it can take advantage of
 * platform-supplied double buffering.  That is, the engine will
 * only needs to create a BufferedImage for double-buffering on
 * platforms where HScene.isDoubleBuffered() returns false.  On
 * platforms where it returns true, repaint draw may be faster.
 * Repaint draw may also make it easier to coexist with widgets
 * (like HButton), particularly if they overlap with the area
 * managed by the animation engine.
 **/

public class RepaintDrawEngine extends ClockBasedEngine {


    private Container container;
    private Component rdComponent;
    private Rectangle bounds;
    private Image buffer = null;
    private Graphics2D bufferG = null;
    private Object repaintMonitor = new Object();
    private boolean repaintPending = false;
    private Rectangle clipBounds = new Rectangle();
    private Rectangle damageArea = new Rectangle();

    /**
     * Create a new RepaintDrawEngine.  It needs to be initialized with
     * the various initXXX methods (including the inherited ones).
     **/
    public RepaintDrawEngine() {
    }

    /**
     * {@inheritDoc}
     **/
    public void initContainer(Container container, Rectangle bounds) {
        this.container = container;
        this.bounds = bounds;
        rdComponent = new Component() {
            public void paint(Graphics g) {
                try {
                    rdRepaintFrame((Graphics2D) g);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }

            }
        };
        rdComponent.setBounds(bounds);
        container.add(rdComponent);
        rdComponent.setVisible(true);
        if (container.isDoubleBuffered()) {
            // buffer and bufferG will be null
        } else {
            buffer = AssetFinder.createCompatibleImageBuffer(
                                    container, bounds.width, bounds.height);
            bufferG = AssetFinder.createGraphicsFromImageBuffer(buffer);
            bufferG.setComposite(AlphaComposite.Src);
            bufferG.setColor(transparent);
            bufferG.fillRect(0, 0, bounds.width, bounds.height);
        }
        repaintBounds = new Rectangle();
    }

    //
    // Only called from Component inner class in initContainer
    void rdRepaintFrame(Graphics2D g) throws InterruptedException {
        g.setComposite(AlphaComposite.Src);
        synchronized(repaintMonitor) {
            if (repaintPending) {
                if (buffer == null) {
                    paintFrame(g);
                } else {
                    clipBounds.setBounds(bounds);
                    g.getClipBounds(clipBounds);
                    Rectangle a = clipBounds;
                    g.drawImage(buffer, a.x, a.y, a.x+a.width, a.y+a.height,
                                        a.x, a.y, a.x+a.width, a.y+a.height,
                                         null);
                }
                repaintPending = false;
                repaintMonitor.notify();
                return;
            } 
        } 

        // No repaint generated by us was pending, so this is a real
        // repaint (e.g. an expose event)
        //
        if (Debug.LEVEL > 0) {
            Debug.println("repainting...");
        }
        repaintFrame(g);
    }

    /** 
     * {@inheritDoc}
     **/
    public int getWidth() {
        return bounds.width;
    }

    /** 
     * {@inheritDoc}
     **/
    public int getHeight() {
        return bounds.height;
    }

    /**
     * {@inheritDoc}
     **/
    public Component getComponent() {
        return rdComponent;
    }

    /**
     * Sets this engine so that the given region of the image buffer is 
     * forced to be repainted at next frame.   This can be used
     * if some section of the framebuffer was damaged somehow.
     *
     * If there are multiple calls to this method before the next frame
     * drawing comes around, then the regions are combined together.
     **/
    public synchronized void addRepaintArea(int x, int y, int width, int height) {
       if (width <= 0 || height <= 0) {
           return;
       }
       if (repaintBounds.isEmpty()) {
           repaintBounds.setBounds(x, y, width, height);
       } else {
           // See DrawRecord.addToRect(..) for the explanation.
           repaintBounds.add(x, y);
           repaintBounds.add(x+width, y+height);
       }
    }

    /**
     * {@inheritDoc}
     **/
    protected void clearArea(int x, int y, int width, int height) {
        if (buffer == null) {
            // Do nothing, because repaint draw erases for us.
        } else {
            bufferG.setColor(transparent);
            bufferG.fillRect(x, y, width, height);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is always false for repaint draw, because the AWT subsystem
     * tells us the extent of our drawing, and it erases behind that area
     * for us.
     **/
    protected boolean needsFullRedrawInAnimationLoop() {
        return false;
    }

    /**
     * {@inheritDoc}
     **/
    protected void callPaintTargets() throws InterruptedException {
        if (renderContext.numDrawTargets == 0) {
                // Remember that the draw targets will always have
                // the erase targets contained within them
            return;     // Nothing changed, so do nothing
        }
        damageArea.setBounds(renderContext.drawTargets[0]);
        for (int i = 1; i < renderContext.numDrawTargets; i++) {
            damageArea.add(renderContext.drawTargets[i]);
        }
        //
        // With repaint draw, we have to collapse it all into one big
        // damage area, because there's no AWT repaint() call taking
        // multiple damage rectangles.  It's not safe to call repaint()
        // a bunch of times in succession, because that might result
        // in multiple paints, which would be slow and would result
        // in user-visible partial screens
        //
        Rectangle a = damageArea;
        synchronized (repaintMonitor) {
            if (buffer == null) {
                // rdRepaintFrame() will call paintFrame(Graphics2D) for us
            } else {
                bufferG.setClip(damageArea);
                paintTargets(bufferG);
                bufferG.setClip(null);
            }
            repaintPending = true;
            rdComponent.repaint(a.x, a.y, a.width, a.height);
            try {
                repaintMonitor.wait(40);
                //
                // Wait up to 40ms for the paint() call to come around.
                // That's short enough that we'll time out reasonably
                // if we never get the paint() (i.e. because the xlet is
                // being destroyed), but long enough so that we should
                // always get notified before timing out.  
                //
                // If it does time out, it's not a disaster, though
                // it is possible that this frame won't get painted,
                // and the next one will get painted twice.
                //
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * {@inheritDoc}
     **/
    protected void finishedFrame() {
        // Nothing needed - AWT flushes the drawing for us
    }

    /**
     * {@inheritDoc}
     **/
    protected void terminatingEraseScreen() {
        rdComponent.repaint();
        container.remove(rdComponent);
        Image buf = buffer;
        if (buf != null) {
            synchronized(repaintMonitor) {
                buffer = null;
                bufferG = null;
            }
            AssetFinder.destroyImageBuffer(buf);
        }
    }
}
