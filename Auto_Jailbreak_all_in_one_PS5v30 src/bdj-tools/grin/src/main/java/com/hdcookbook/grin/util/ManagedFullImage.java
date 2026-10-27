
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

package com.hdcookbook.grin.util;

import java.awt.Image;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.net.URL;

/**
 * A managed image that's loaded from its own image file (and not
 * as a part of a mosaic).
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class ManagedFullImage extends ManagedImage implements ImageObserver {

    private String name;
    private URL url = null;     // Stays null unless special constructor used
    private int numReferences = 0;
    private int numPrepares = 0;
    Image image = null;         // Accessed by ManagedSubImage
    private boolean loaded = false;
        // If image != null && !loaded, then we're loading.
    private int width = 0;
    private int height = 0;
        // If there's an error loading, width and height are left at 0
    private boolean flushing = false;

    /////////////////////////////////
    //    STATE MODEL              //
    /////////////////////////////////
    //
    // The state model of ManagedFullImage deserves some explanation.
    // First a word about what it does, and doesn't do.
    //
    // The state model of ManagedFullImage has a certain complexity
    // that largely comes from working with Java's rather complex and
    // sometimes inconsistently implemented image loading model.
    // ManagedFullImage deals with that.  What the state model does
    // _not_ attempt to do is manage a multi-threaded client, where
    // (for example) prepare(), unprepare() and load() might be called
    // at the same time as a different thread is calling paint().  Rather,
    // ManagedFullImage is designed to work with a framework like
    // com.hdcookbook.grin.animator.AnimationEngine, where paint is only
    // called when the framework knows the ManagedFullImage is in the
    // loaded state.  Image loading can be managed asynchronously from 
    // the animation thread (using startLoading()), or synchronously from 
    // the setup thread (using load()).  
    //
    // The image loading state of a ManagedFullImage is determined by the 
    // values of the variables numPrepares, image, loaded, and flushing.
    // Synchronization is done by synchronizing on "this."  Of course, we
    // trust that no external code synchronizes on our instance!  When the
    // lock is not held (outside of a synchronized block, or during a wait()),
    // ManagedFullImage has five valid states, as follows.
    //
    //  UNLOADED:
    //      numPrepares = 0
    //      image = null
    //      loaded = false
    //      flushing = false
    //
    //  READY TO LOAD:
    //      numPrepares > 0
    //      image = null
    //      loaded = false
    //      flushing = false
    //
    //  LOADING:
    //      numPrepares > 0
    //      image != null
    //      loaded = false
    //      flushing = false
    //
    //  LOADED:
    //      numPrepares > 0
    //      image != null
    //      loaded = true
    //      flushing = false
    //
    //  FLUSHING:
    //      numPrepares = ? (usually 0)
    //      image = ? (usually null, but maybe a different instance of Image)
    //      loaded = ? (usually false)
    //      flushing = true
    //
    // The FLUSHING state is a solution to what should probably be considered
    // a bug in PBP.  Under certain circumstances, a call to Image.flush()
    // blocks, while a callback _in_ _a_ _different_ _thread_ (presumably
    // an image fetcher) to imageUpdate() occurs.  Of course, our 
    // implementation of imageUpdate() needs to take out a lock, so this
    // means that the ManagedFullImage lock /cannot/ be held while
    // Image.flush() is called.  This behavior was observed on players
    // from two major manufacturers, and seems to be triggered by calling
    // flush() immediately after Component.prepareImage().  This behavior
    // was not observed on desktop JDK (1.6); in other words, this subtle
    // bug got fixed :-)
    //
    // LOADED includes the case where there was an error loading an image,
    // e.g. because of a network error, or because the .png or .jpg file
    // isn't there or is invalid.

    ManagedFullImage(String name) {
        this.name = name;
        // We are now in the UNLOADED state
    }

    ManagedFullImage(String name, URL url) {
        this.name = name;
        this.url = url;
        // We are now in the UNLOADED state
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }

    synchronized void addReference() {
        numReferences++;
        // This does not change our loading/unloading state
    }

    synchronized void removeReference() {
        numReferences--;
        // This does not change our loading/unloading state, but
        // ImageManager notices when the reference count drops to
        // 0, and calls destroy() when this happens.
    }

    synchronized boolean isReferenced() {
        return numReferences > 0;
    }

    /**
     * {@inheritDoc}
     **/
    public synchronized void prepare() {
            // See ManagedImage's main class documentation under
            //  "ManagedImage contract - image loading and unloading".
        numPrepares++;
        // This might move us from UNLOADED to READY TO LOAD
    }

    /**
     * {@inheritDoc}
     **/
    public synchronized boolean isLoaded() {
            //  See ManagedImage's main class documentation under
            //  "ManagedImage contract - image loading and unloading".
        return loaded;
    }

    /**
     * {@inheritDoc}
     **/
    public synchronized boolean hadErrorLoading() {
        return loaded && height == 0 && width == 0;
    }

    /**
     * {@inheritDoc}
     **/
    public void load(Component comp) {
            // See ManagedImage's main class documentation under
            //  "ManagedImage contract - image loading and unloading".
        synchronized(this) {
            while (true) {
                if (loaded || numPrepares <= 0) {
                        // If we're in the UNLOADED or LOADED state, bail.
                        //
                        // If load is done in a different thread than
                        // unprepare, it's possible for our client to lose
                        // interest in us before we even start preparing.
                        // For example, in GRIN, the show could possibly
                        // move to a different segment before the setup
                        // thread starts preparing an image from the previous
                        // segment.
                    return;
                }
                if (image == null) {
                    // If we're in READY TO LOAD, or possibly FLUSHING
                    startLoading(comp); // Sets image to non-null
                } else {
                    // We are in the LOADING state, so we wait.
                    try {
                        wait(); 
                        // Wait until the image fetcher loads image, or
                        // until we lose interest.
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    // Now, go back around the loop
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     **/
    public synchronized void startLoading(Component  comp) {
        while (flushing) {
            // If we're FLUSHING, we need to wait until we're not.  When
            // we get out of FLUSHING, we could be in any other state, but
            // normally we'll be in UNLOADED, or perhaps READY TO LOAD.
            try {
                wait(); 
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (image != null || numPrepares <= 0) {
            // if we're UNLOADED, LOADING or LOADED
            return;
        }
        // Now we know we're in READY TO LOAD
        if (url == null) {
            image = AssetFinder.loadImage(name);
        } else {
            image = Toolkit.getDefaultToolkit().createImage(url);
        }
        notifyAll();
        // Now our state is LOADING

        //
        // The JDK seems to put the image fetching thread priority
        // really high, which is the opposite of what we want.  By
        // yielding, we increase the odds that higher-priority animation
        // will be given a chance before our lower-priority setup thread
        // grabs the CPU with the image fetching thread.  On all 
        // implementations, yielding in this manner should be at worst 
        // harmless.
        //
        Thread.currentThread().yield();
        comp.prepareImage(image, this);
    }

    //
    // Implementation of the ImageObserver method.  This gets called by the
    // system on the image loading thread.
    //
    public boolean 
    imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
    {
        synchronized(this) {
            if (img != image) {
                    // They've lost interest in us.  So sad.
                    //
                    // Usually, we'd get here because image is null, because
                    // unprepare() got called.  It's possible that after image
                    // becomes null, it might become non-null with a fresh
                    // instance of Image; in this case, the old image in img
                    // is still something we don't care about, so we flush it
                    // and tell the system to stop updating us.
                    //
                    // We used to call flush() here, but that's not necessary.
                    // The only time the image instance variable is set null,
                    // we call flush().  "image" is only set non-null when
                    // it's already null.  Thus, every time we get here,
                    // img.flush() will have been called.
                return false;
            } else if ((infoflags & (ERROR | ABORT)) != 0) {
                // ERROR and ABORT shouldn't really happen, but if it does
                // the best we can do is accept the fact, and externally
                // treat the image as though it were loaded.
                if (Debug.LEVEL > 1) {
                    Debug.println("Error loading image " + this);
                }
                loaded = true;
                this.width = 0;
                this.height = 0;
                notifyAll();
                // Fall through to notifyLoaded
            } else if ((infoflags & ALLBITS) != 0) {
                loaded = true;
                this.width = width;
                this.height = height;
                notifyAll();
                // Fall through to notifyLoaded
            } else {
                return true;
            }
        }

        // At this point, the image just finished loading completely, and
        // we are outside of the synchronized block.  Our thread might
        // be holding locks, however.
        AssetFinder.notifyLoaded(this);

        return false;
    }

    /** 
     * {@inheritDoc}
     **/
    public void unprepare() {
            // See ManagedImage's main class documentation under
            //  "ManagedImage contract - image loading and unloading".
        int w = 0;
        int h = 0;
        boolean notify = false;
        Image flush = null;
        synchronized(this) {
            numPrepares--;
            if (numPrepares > 0) {
                // If we're in READY TO LOAD, LOADING, LOADED, or
                // perhaps FLUSHING
                return;
            } else {
                // Now we want to be in UNLOADED, but we're going to have
                // to go through FLUSHING first.
                if (image != null) {
                    w = width;
                    h = height;
                    width = 0;
                    height = 0;
                    flush = image;
                    flushing = true;
                    image = null;
                }
                notify = loaded;
                loaded = false;
                notifyAll();
            }
        }
        if (flush != null) {
                // We needed to pull the flush() call outside of the 
                // synchronized block.  See the discussion about the PBP
                // bug under the state model at the beginning of this
                // class.
            flush.flush();
            synchronized(this) {
                flushing = false;
                    // Since we let go of the lock, we might be in some other
                    // state now.  But we're probably in UNLOADED.
                notifyAll();
            }
        }
        //
        // At this point, we just unloaded a loaded image, and we're safely
        // outside the synchronized block.  This is even true if we started
        // re-loading it.  Note that our caller might be holding
        // locks, so the implementation of notifyUnloaded can't take out
        // non-local locks.
        //
        if (notify) {
            AssetFinder.notifyUnloaded(this, w, h);
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void draw(Graphics2D gr, int x, int y, Component comp) {
        if (width > 0) {
            gr.drawImage(image, x, y, comp);
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void drawScaled(Graphics2D gr, Rectangle bounds, Component comp) {
        if (width > 0) {
            gr.drawImage(image, bounds.x, bounds.y, 
                            bounds.x+bounds.width, bounds.y+bounds.height,
                            0, 0, width, height, comp);
        }
    }
    
    /**
     * {@inheritDoc}
     **/
    public void drawClipped(Graphics2D gr, int x, int y, 
                            Rectangle subsection, Component comp) 
    {
        if (width > 0) {
            gr.drawImage(image, x, y, x+ subsection.width, y+subsection.height,
                            subsection.x, subsection.y, 
                            subsection.x+subsection.width, 
                            subsection.y+subsection.height, 
                            comp);
        }
    }

    void destroy() {
        if (Debug.LEVEL > 0 && loaded) {
            Debug.println("Warning:  Destroying loaded image " + this + ".");
            Debug.println("          unprepare() should always be called before ungetImage().");
                // A bit of explanation here:  destroy() is called from
                // ImageManger.ungetImage() when the ref count drops to 0.
                // This is supposed to mean that the application no longer
                // references the ManagedImage, so it should be impossible
                // for the application to call unprepare().
                //
                // An xlet should always unprepare() its images before
                // calling ImageManager.ungetImage().
        }
        Image im = image;
        if (im != null) {
            im.flush(); 
            // This should never happen, since the  unprepare() called for
            // above will have set image null.  However, a buggy application
            // might fail to do this; calling flush() here adds some
            // robustness.
        }
    }
}
