
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
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.SetupClient;

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;

/**
 * Represents a fixed image.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class FixedImage extends Feature implements Node, SetupClient {

    protected Rectangle placement;
    private boolean placementCopied = false;
    protected String fileName;
    protected InterpolatedModel scalingModel = null;
    protected Rectangle scaledBounds = null;

    protected ManagedImage image;
    private boolean setupMode = false;
    private Object setupMonitor = new Object();
    private boolean imageSetup = false;
    private boolean isActivated = false;
    private boolean imageChanged = false;
    private DrawRecord drawRecord = new DrawRecord();
    
    public FixedImage(Show show) {
        super(show);
    }

    /**
     * {@inheritDoc}
     **/
    protected Feature createClone(HashMap clones) {
        if (!setupMode || !imageSetup || isActivated) {
            throw new IllegalStateException();
        }
        FixedImage result = new FixedImage(show);
        result.placement = placement;
        result.fileName = fileName;     // null if image replaced
        if (scaledBounds != null) {
            result.scaledBounds = new Rectangle(scaledBounds);
        }
        result.image = image;
        ImageManager.getImage(image);
                // This increments the reference count of this ManagedImage,
                // which is necessary because when the clone is destroyed,
                // it will decrement that reference count.
        result.image.prepare();
                // Balanced by an unprepare in destroy()
        result.imageSetup = true;
        result.setupMode = true;
        return result;
    }

    /**
     * {@inheritDoc}
     **/
    protected void initializeClone(Feature original, HashMap clones) {
        super.initializeClone(original, clones);
        FixedImage other = (FixedImage) original;
        scalingModel = (InterpolatedModel)
                Feature.clonedReference(other.scalingModel, clones);
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the segments.
     **/
    public void initialize() {
        image = ImageManager.getImage(fileName);
    }

    /**
     * {@inheritDoc}
     **/
    public int getX() {
        return placement.x;
    }

    /**
     * {@inheritDoc}
     **/
    public int getY() {
        return placement.y;
    }

    /**
     * Get the placement of this image, that is, the x, y, position, the width
     * and the height.  You can modify these values, so long as you do so
     * in a thread-safe manner, e.g. with the Show lock held.  Usually, this
     * means modifying them from a Show command.
     * <p>
     * If you change the width or height, you <b>must</b> call
     * setImageSizeChanged().
     *
     * @see #setImageSizeChanged()
     **/
    public synchronized Rectangle getMutablePlacement() {
        if (!placementCopied) {
            placement = new Rectangle(placement);       // No longer shared
            placementCopied = true;
        }
        return placement;
    }

    /**
     * Notify us that our image size has changed.  It can be changed
     * by setting the width or height of our placement.  If this is done,
     * setImageSizeChanged() must be called, to guarantee that the now-modified
     * image will get drawn to the screen.
     *
     * @see #getMutablePlacement()
     **/
    public void setImageSizeChanged() {
        imageChanged = true;
    }

    
    /**
     * Get the underlying image that we display.  Neither the
     * reference count nor the prepare count are adjusted.
     **/
    public ManagedImage getImage() {
        return image;
    }

    /**
     * Set the image being displayed by this FixedImage.  The old image
     * will be de-referenced (and, if needed, unprepared) by FixedImage,
     * and the new image will be referenced (and, if needed, prepared)
     * by FixedImage.  If this FixedImage feature is set up, then the new
     * image must have been loaded.  If it isn't, an IllegalStateException
     * will be thrown.
     * <p>
     * Note that if you have a ManagedImage instance that you want to forget
     * about after you give it to FixedImage, if you've prepared it you'll 
     * have to call ManagedImage.unprepare() after giving FixedImage the
     * instance (because FixedImage itself calls prepare()).  Similarly,
     * you'll need to call ImageManager.ungetImage() after giving
     * FixedImage the instance.  In other words, when give FixedImage the 
     * instance, it increments the reference counts, so you have to 
     * decrement the  reference counts you added when you first referenced 
     * the ManagedImage.
     * <p>
     * This method must only be called when it is safe to do so, according to 
     * the threading model, and with the Show lock held.  Usually, this means 
     * calling it from a Show command.
     * <p>
     * Code using this method to swap out an image with one that's to be
     * displayed at a different size might look something like this:
     * <pre>
     * FixedImage fi = ...  the place you want to put the image
     * ManagedImage mi = ... the image you want to put there
     * fi.replaceImage(mi);
     * Rectangle r = fi.getMutablePlacement();
     * r.x, r.y = the upper-left hand corner where you want it to be
     * r.width, r.height = the values you want (which can be taken
     *                  from mi.getWidth() and mi.getHeight()
     * fi.setImageSizeChanged();
     *</pre>
     * Code that gets a new image from the ImageManager and swaps it into a 
     * new image to FixedImage instance that <i>isn't</i>
     * set up is fairly straightforward:
     * </pre>
     * {
     *     FixedImage fi = ... the feature
     *     URL url = ... the place the image comes from
     *     ManagedImage mi = ImageManager.getImage(url);
     *     fi.replaceImage(mi);
     *     ImageManager.ungetImage(mi);     // mi goes out of scope
     * }
     * 
     * @throws  IllegalStateException if this feature is set up, and 
     *                  newImage has not been loaded.  Also thrown if
     *                  newImage.isReferenced() is false.
     **/
    public void replaceImage(ManagedImage newImage) {
        if (newImage == image) {
            return;
        }
        if (setupMode) {
            if (!newImage.isLoaded()) {
                throw new IllegalStateException();
            }
            ImageManager.getImage(newImage);
            newImage.prepare();
            image.unprepare();
            ImageManager.ungetImage(image);
        } else {
            ImageManager.getImage(newImage);
            ImageManager.ungetImage(image);
        }
        image = newImage;
        fileName = null;
        imageChanged = true;
    }


    /**
     * Free any resources held by this feature.  It is the opposite of
     * initialize().
     * <p>
     * It's possible an active segment may be destroyed.  For example,
     * the last segment a show is in when the show is destroyed will
     * probably be active (and it will probably be an empty segment
     * too!).
     **/
    public void destroy() {
        if (setupMode) {
            // That is, if this is a cloned feature
            if (Debug.ASSERT && !imageSetup) {
                Debug.assertFail();
            }
            image.unprepare();
                // This balances the image.prepare() in createClone().
        }
        ImageManager.ungetImage(image);
    }


    /**
     * {@inheritDoc}
     **/
    protected void setActivateMode(boolean mode) {
        isActivated = mode;
    }

    /**
     * {@inheritDoc}
     **/
    protected int setSetupMode(boolean mode) {
        synchronized(setupMonitor) {
            setupMode = mode;
            if (setupMode) {
                image.prepare();
                if (image.isLoaded()) {
                    imageSetup = true;
                    return 0;
                } else {
                    show.setupManager.scheduleSetup(this);
                    return 1;
                }
            } else {
                image.unprepare();
                imageSetup = false;
                return 0;
            }
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void doSomeSetup() {
        synchronized(setupMonitor) {
            if (!setupMode) {
                return;
            }
        }
        image.load(show.component);
        synchronized(setupMonitor) {
            if (!setupMode) {
                return;
            }
            imageSetup = true;
        }
        sendFeatureSetup();
    }

    /**
     * {@inheritDoc}
     **/
    public boolean needsMoreSetup() {
        synchronized (setupMonitor) {
            return setupMode && (!imageSetup);
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
        if (!isActivated) {
            return;
        }
        if (scalingModel == null) {
            image.drawScaled(gr, placement, show.component);
        } else {
            image.drawScaled(gr, scaledBounds, show.component);
        }
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
            drawRecord.setArea(placement.x, placement.y, 
                               placement.width, placement.height);
        } else {
            boolean changed 
                = scalingModel.scaleBounds(placement.x, placement.y, 
                                           placement.width, placement.height, 
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
        if (imageChanged) {
            drawRecord.setChanged();
            imageChanged = false;
        }
        context.addArea(drawRecord);
    }

    /**
     * {@inheritDoc}
     **/
    public void nextFrame() {
        // do nothing
    }
    
    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
                
        in.readSuperClassData(this);
        this.placement = in.readSharedRectangle();
        this.fileName = in.readString();    
        if (in.readBoolean()) {
            this.scalingModel = (InterpolatedModel) in.readFeatureReference();                    
            this.scaledBounds = new Rectangle();
        } 
    }
}
