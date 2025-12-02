
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
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.SetupClient;

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;

/**
 * An image sequence does "cell" animation.  It consists of a number
 * of images that are displayed one after another.  All of the images
 * in a sequence are assumed to be the same size.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class ImageSequence extends Feature implements Node, SetupClient {

    protected Rectangle[] placements;   // Same length as fileNames[]
    protected String[] fileNames; 
    protected boolean repeat;
    protected InterpolatedModel scalingModel = null;
    protected Rectangle scaledBounds = null;
    protected int loopCount;    
        // # of times to repeat images before sending end commands
        // Integer.MAX_VALUE means "infinite"
    protected Command[] endCommands;

    protected ManagedImage[] images;
        // The images in this sequence.  A null image will show up as
        // blank, that is, any previous image will be erased.
    private boolean setupMode = false;
    private boolean imagesSetup = false;
    private Object setupMonitor = new Object();
    private boolean isActivated = false;

    protected ImageSequence model;      
        // We use model to count our frame and for end commands.  If
        // we're our own model, it's set to null.
    private int activeModelCount = 0;   
        // # of active sequences using us as a model, including ourselves
        // (if we're active).  This tells us how many
        // time nextFrame() will be called per frame
    private int nextFrameCalls = 0;
        // How many times we've been called without advancing currFrame;
    private int currFrame = 0;  // Frame of our animation
    private boolean atEnd;      // At end of animation
    private int loopsRemaining; // see loopCount

    private ManagedImage lastImage = null;
    private ManagedImage currImage = null;
    private Rectangle currPlacement = null;
    private DrawRecord drawRecord = new DrawRecord();

    public ImageSequence(Show show) {
        super(show);
    }

    /**
     * {@inheritDoc}
     **/
    protected Feature createClone(HashMap clones) {
        if (!isSetup() || isActivated) {
            throw new IllegalStateException();
        }
        ImageSequence result = new ImageSequence(show);
        result.placements = placements;
        result.fileNames = fileNames;
        result.repeat = repeat;
        if (scaledBounds != null) {
            result.scaledBounds = new Rectangle(scaledBounds);
        }
        result.loopCount = loopCount;
        result.images = images;
        for (int i = 0; i < images.length; i++) {
            ManagedImage mi = images[i]; 
            if (mi != null) {
                ImageManager.getImage(mi);
                    // This increments the reference count of this ManagedImage,
                    // which is necessary because when the clone is destroyed,
                    // it will decrement that reference count.
                mi.prepare();
                    // This is balanced by a call to unprepare() in destroy()
                if (Debug.ASSERT && mi != images[i]) {
                    Debug.assertFail();
                }
            }
        }
        result.setupMode = true;
        result.imagesSetup = imagesSetup;
        result.nextFrameCalls = nextFrameCalls;
        result.currFrame = currFrame;
        result.activeModelCount = activeModelCount;     // 0
        result.atEnd = atEnd;
        result.loopsRemaining = loopsRemaining;
        result.lastImage = lastImage;
        result.currImage = currImage;
        result.currPlacement = currPlacement;
        return result;
    }

    /**
     * {@inheritDoc}
     **/
    protected void initializeClone(Feature original, HashMap clones) {
        super.initializeClone(original, clones);
        ImageSequence other = (ImageSequence) original;
        scalingModel = (InterpolatedModel)
                Feature.clonedReference(other.scalingModel, clones);
        endCommands = Feature.cloneCommands(other.endCommands, clones);
        model = (ImageSequence) Feature.clonedReference(other.model, clones);
                // Remeber, we're not active now, so it's OK for us to
                // refer to a different ImageSequence as our model without
                // worrying about activeModelCount.
    }

    /**
     * {@inheritDoc}
     **/
    public int getX() {
        return placements[getStateHolder().currFrame].x;
    }

    /**
     * {@inheritDoc}
     **/
    public int getY() {
        return placements[getStateHolder().currFrame].y;
    }

    /**
     * Get the underlying images in this sequence.  Some of them might be
     * null.
     **/
    public ManagedImage[] getImages() {
        return images;
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the segments.
     * <p>
     * It's OK to call this method earlier if needed, e.g. in order to
     * determine image widths.
     **/
    public void initialize() {
        if (images != null) {
            return;     // Already initialized
        }
        images = new ManagedImage[fileNames.length]; 
        for (int i = 0; i < fileNames.length; i++) { 
            if (fileNames[i] == null) { 
                images[i] = null;
            } else {
                images[i] = ImageManager.getImage(fileNames[i]); 
            }
        }
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
        if (Debug.ASSERT && setupMode && !imagesSetup) {
            Debug.assertFail();
        }
        for (int i = 0; i < images.length; i++) {
            if (images[i] != null) {
                if (setupMode) {
                    // That is, if this is a cloned feature
                    images[i].unprepare();
                        // This balances the image.prepare() in createClone().
                }
                ImageManager.ungetImage(images[i]);
            }
        }
    }

    /**
     * {@inheritDoc}
     **/
    protected void setActivateMode(boolean mode) {
        isActivated = mode;
        if (model != null) {
            if (mode) {
                if (!model.isActivated && model.activeModelCount == 0) {
                    model.currFrame = 0;
                    model.atEnd = false;
                    model.loopsRemaining = model.loopCount;
                }
                model.activeModelCount++;
            } else {
                model.activeModelCount--;
            }
        } else {
            if (mode) {
                if (activeModelCount == 0) {
                    currFrame = 0;
                    atEnd = false;
                    loopsRemaining = loopCount;
                }
                activeModelCount++;
            } else {
                activeModelCount--;
            }
        }
        if (mode) {
            lastImage = null;
            currImage = images[getStateHolder().currFrame];
        }
    }

    /**
     * {@inheritDoc}
     **/
    protected int setSetupMode(boolean mode) {
        synchronized(setupMonitor) {
            setupMode = mode;
            if (setupMode) {
                boolean allLoaded = true;
                for (int i = 0; i < images.length; i++) {
                    ManagedImage mi = images[i];
                    if (mi != null) {
                        mi.prepare();
                        allLoaded = allLoaded && mi.isLoaded();
                    }
                }
                if (allLoaded) {
                    imagesSetup = true;
                    return 0;
                } else {
                    show.setupManager.scheduleSetup(this);
                    return 1;
                }
            } else {
                for (int i = 0; i < images.length; i++) {
                    ManagedImage mi = images[i];
                    if (mi != null) {
                        mi.unprepare();
                    }
                }
                imagesSetup = false;
            }
            return 0;
        }
    }


    /**
     * {@inheritDoc}
     **/
    public void doSomeSetup() {
        for (int i = 0; i < images.length; i++) {
            synchronized(setupMonitor) {
                if (!setupMode) {
                    return;
                }
            }
            ManagedImage mi = images[i];
            if (mi != null) {
                mi.load(show.component);
            }
        }
        synchronized(setupMonitor) {
            if (!setupMode) {
                return;
            }
            imagesSetup = true;
        }
        sendFeatureSetup();
    }

    /**
     * {@inheritDoc}
     **/
    public boolean needsMoreSetup() {
        synchronized (setupMonitor) {
            return setupMode && !imagesSetup;
        }
    }

    private ImageSequence getStateHolder() {
        if (model == null) {
            return this;
        } else {
            return model;
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void nextFrame() {
        if (Debug.LEVEL > 0 && !isActivated) {
            Debug.println("\n*** WARNING:  Advancing inactive sequence " 
                          + getName() + "\n");
        }
        if (model != null) {
            model.nextFrame();
        } else {
            nextFrameCalls++;
            if (nextFrameCalls >= activeModelCount) {
                nextFrameCalls = 0;     // We've got them all
                if (!atEnd) {
                    currFrame++;
                    if (currFrame == images.length) {
                        if (loopCount != Integer.MAX_VALUE) {
                            loopsRemaining--;
                        }
                        if (loopsRemaining > 0) {
                            currFrame = 0;
                        } else {
                            loopsRemaining = loopCount;
                            show.runCommands(endCommands);
                            if (repeat)  {
                                currFrame = 0;
                            } else {
                                atEnd = true;
                                currFrame--;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Set the current frame within the animation.  This should only be
     * called within the animation thread when model updates are safe,
     * e.g. during the execution of a show command.  Note that the current
     * frame value increments with every frame in nextFrame(), and commands
     * execute after that.  This means that you can control the frame # of
     * an image sequence by calling this method every frame, effectively
     * overwriting the value just calculated automatically in nextFrame().
     * <p>
     * Note that the logic for calling the end commands resides in nextFrame(),
     * so you should be careful if there are end commands - if you set the
     * current frame to the last image, then the end commands will execute
     * in the next animation frame.
     * 
     *
     *  @param f        The frame number to set, 0..(n-1)
     *
     *  @throws IllegalArgumentException if f is out of range
     **/
    public void setCurrentFrame(int f) {
        if (f < 0 || f >= images.length) {
            throw new IllegalArgumentException();
        }
        currFrame = f;
        atEnd = false;
    }

    /**
     * Get the current frame.
     *
     * @return the frame number, 0..(n-1)
     *
     * @see #setCurrentFrame(int)
     **/
    public int getCurrentFrame() {
        return currFrame;
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
        int frame = getStateHolder().currFrame;
        currImage = images[frame];
        currPlacement = placements[frame];
        if (currImage != null) {
            if (scalingModel == null) {
                drawRecord.setArea(currPlacement.x, currPlacement.y, 
                                   currPlacement.width, currPlacement.height);
            } else {
                boolean changed = 
                    scalingModel.scaleBounds(currPlacement.x, currPlacement.y, 
                                             currPlacement.width, 
                                             currPlacement.height, 
                                             scaledBounds);
                        // When newly activated, we might get a false positive
                        // on changed, but that's OK because our draw area is
                        // changed anyway.
                drawRecord.setArea(scaledBounds.x, scaledBounds.y, 
                                   scaledBounds.width, scaledBounds.height);
                if (changed) {
                    drawRecord.setChanged();
                }
            }
            if (currImage != lastImage) {
                drawRecord.setChanged();
            }
            context.addArea(drawRecord);

            // if currImage == null, then we simply don't add this drawRecord
            // to our RenderContext.  RenderContext remembers what was drawn
            // in the last frame, so if this drawRecord was drawn in the last
            // frame but wasn't drawn in this frame, it automatically knows
            // that it needs to be erased.

        }
        lastImage = currImage;
    }

    /**
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
        if (!isActivated) {
            return;
        }
        doPaint(gr);
    }

    private void doPaint(Graphics2D g) {
        if (currImage != null) {
            if (scalingModel == null) {
                currImage.drawScaled(g, currPlacement, show.component);
            } else {
                currImage.drawScaled(g, scaledBounds, show.component);
            }
        }
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException 
    {
        in.readSuperClassData(this);
        this.placements = in.readSharedRectangleArray();
        this.fileNames = in.readStringArray();
        this.repeat = in.readBoolean();
        if (in.readBoolean()) {
            this.model = (ImageSequence) in.readFeatureReference();
        }
        loopCount = in.readInt();
        this.endCommands = in.readCommands();       
        if (in.readBoolean()) {
            this.scalingModel = (InterpolatedModel) in.readFeatureReference();
            this.scaledBounds = new Rectangle();
        }
        if (Debug.ASSERT && placements.length != fileNames.length) {
            Debug.assertFail();
        }
    }
}
