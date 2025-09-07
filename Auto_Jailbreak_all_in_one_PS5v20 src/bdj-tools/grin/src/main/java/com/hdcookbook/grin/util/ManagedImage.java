
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

/**
 * An image that is managed by the GRIN utilities.  Managed images
 * have reference counts, and a count of how many clients have asked
 * it to be prepared.  This is used to flush images once they're
 * no longer needed.  A ManagedImage instance is obtained from
 * ImageManager.
 *
 * <h2>ManagedImage contract - get() and unget()</h2>
 *
 * ManagedImage instances are created and managed with a family
 * of <code>get()</code> and <code>unget()</code> methods on
 * <code>ImageManager</code>.  <code>ImageManager</code> does
 * reference counting based on the <code>get()</code> and
 * <code>unget()</code> calls so it can keep track of the
 * image instances being managed.  If you ask for the same image twice
 * (with one of the <code>get()</code> methods), you will get the same
 * instance of <code>ManagedImage</code>, and you will of course need
 * to balance the two calls to <code>get()</code> with two calls to
 * <code>unget()</code> when you're done.
 * <p>
 * <code>get()</code> and <code>unget()</code> are just for creating
 * <code>ManagedImage</code> instances -- they have nothing to do with
 * loading and unloading the actual image pixmap.
 * <p>
 * The GRIN features <code>fixed_image</code> and <code>image_sequence</code>
 * <code>get()</code> their images when the show is read, and 
 * <code>unget()</code> them when the show is destroyed.
 *
 * <h2>ManagedImage contract - prepare(), unprepare() and loading</h2>
 *
 * The second level of reference counting is for tracking whether the 
 * underlying image asset is not loaded, loading, or loaded.  Every client
 * of an image that wants the image to be loaded should call
 * <code>prepare()</code> on the image, and each prepare call must 
 * eventually be balanced by a call to <code>unprepare()</code>.
 * If a <code>ManagedImage</code> is "prepared" (that is, if 
 * <code>prepare()</code> has been called more times than 
 * <code>unprepare()</code>, that means that something wants the image
 * to be loaded, but it doesn't necessarily mean that the image is loaded.
 * Because image loading is a time-consuming information, it's
 * useful to do as the GRIN scene graph does, and use a seperate thread
 * (like the SetupManager thread) to do the actual image loading.  This is
 * why loading is separated from prepare/unprepare.
 * <p>
 * The following code shows one way of causing an image to actually load.
 * Queueing a task for another thread is somewhat expensive, so if an image
 * is already loaded, it's good to skip that step.  This can be done with
 * the following client code:
 * <pre>
 *
 *        ManagedImage mi = ...
 *        mi.prepare();
 *        if (!mi.isLoaded()) {
 *            Queue a task for another thread to call load() on this image
 *        }
 *
 * </pre>
 * When an image is in the prepared state, callling one of the load methods
 * (<code>load()</code> or <code>startLoading()</code>) is necessary to
 * make the actual image loading happen.
 * <p>
 * Eventually, when the client no longer wants the image to be loaded, 
 * it must call <code>unprepare()</code>.  In other words, each call to
 * <code>prepare()</code> must eventually be balanced by a call to
 * <code>unprepare()</code>.  When that final call to <code>unprepare()</code>
 * is received, the image is unloaded (by calling <code>Image.flush()</code>).
 * <p>
 * The GRIN features <code>fixed_image</code> and <code>image_sequence</code>
 * <code>prepare()</code> their images when the feature is in the active
 * segment's active or setup clause, and they </code>unprepare()</code> them 
 * when leaving the segment.  On a segment switch, <code>prepare()</code>
 * is called for the new segment before <code>unprepare()</code> is called
 * for the old segment, so images stay loaded when they should.
 *
 * <h2>Sticky Images</h2>
 *
 * An image can be marked as "sticky" by calling <code>makeSticky()</code>,
 * and unmarked with <code>unmakeSticky()</code>.  A sticky image won't
 * be unloaded when the count of prepares reaches zero.
 * 
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
abstract public class ManagedImage {

    ManagedImage() {
    }

    abstract public String getName();

    public String toString() {
        return super.toString() + " : " + getName();
    }

    /**
     * Get the width of this image.  This may return 0 if the image has
     * not yet been loaded.
     **/
    abstract public int getWidth();

    /**
     * Get the height of this image.  This may return 0 if the image has
     * not yet been loaded.
     **/
    abstract public int getHeight();

    /**
     * Add one to the reference count of this image.  This is unrelated
     * to image loading and unloading.  It's package-private, because it's
     * used by ImageManager only, for get().
     **/
    abstract void addReference();

    /**
     * Remove one from the reference count of this image.  This is unrelated
     * to image loading and unloading.  It's package-private, because it's
     * used by ImageManager only, for unget().
     **/
    abstract void removeReference();

    /**
     * Determine if this image is referenced, by consulting its reference
     * count.  It's package-private, because it's
     * used by ImageManager only.
     **/
    abstract boolean isReferenced();

    /**
     * Prepare this image for display, by registering interest in having
     * the image be loaded.  In order to actually load the image,
     * <code>load(Component)</code> must be called.
     * <p>
     * See ManagedImage's main class documentation under
     * "ManagedImage contract - image loading and unloading".
     *
     * @see #isLoaded()
     * @see #load(Component)
     * @see #unprepare()
     * @see ManagedImage
     **/
    abstract public void prepare();

    /**
     * Determine whether or not the image is currently loaded.  After a
     * call to prepare(), this method can be used to query whether or not
     * it's necessary to arrange for load(Component) to be called.
     * <p>
     * See ManagedImage's main class documentation under
     * "ManagedImage contract - image loading and unloading".
     * 
     * @see ManagedImage
     **/
    abstract public boolean isLoaded();

    /**
     * Tells whether or not the image had an error loading, e.g. because
     * the path didn't refer to a valid image.  This is always false if
     * isLoaded() is false.
     **/
    abstract public boolean hadErrorLoading();

    /**
     * Load this image for display in the given component, or any
     * other component for the same graphics device.  The image will
     * only be loaded if an interest in loading this ManagedImage has
     * been registered by calling <code>prepare()</code>.  If no interest
     * has been registered, or if this image is already loaded, then this
     * method will return immediately.  If another thread is loading this
     * same image, this method will wait until that image load is complete
     * before it returns.
     * <p>
     * See ManagedImage's main class documentation under
     * "ManagedImage contract - image loading and unloading".
     *
     * @param  comp     A component to use for loading the image.  Clients
     *                  using ManagedImage should never pass in null.
     *
     * @see #prepare()
     * @see #unprepare()
     * @see ManagedImage
     **/
    abstract public void load(Component comp);


    /**
     * Start loading an image.  This is just like 
     * <code>load(Component)</code>, except that it doesn't block until
     * the image is loaded.  If the image has a postive <code>prepare()</code> 
     * count, then sometime after <code>startLoading(Component)</code> is
     * called, <code>isLoaded()</code> will return true (unless, of course,
     * the caller loses interest in the image and calls 
     * <code>unprepare()</code>
     * <p>
     * This method is useful for loading an image asynchronously when the
     * threading model makes polling for image load a natural thing to do.
     * For example, if one wants to load an image while a show is running,
     * one good way to do that is to start the loading, then poll for
     * the completion of the loading in a once-per-frame "heartbeat"
     * method.
     * <p>
     * If the image is already loading or loaded, calling this method is
     * harmless, and has no effect.
     *
     * @see #prepare()
     * @see #unprepare()
     * @see #load(Component)
     * @see #isLoaded()
     * @see ManagedImage
     **/
    abstract public void startLoading(Component  comp);

    /** 
     * Undo a prepare.  We do reference counting; when the number of
     * active prepares hits zero, and the "sticky" count reaches zero,
     * we flush the image.
     * <p>
     * See ManagedImage's main class documentation under
     * "ManagedImage contract - image loading and unloading".
     * <p>
     * This should never be called with a lock held on this instance of
     * ManagedImage.  Of course, it's always a Bad Idea to hold a lock
     * on an external object like that!
     *
     * @see #prepare()
     * @see #load(Component)
     * @see #makeSticky()
     * @see #unmakeSticky()
     * @see ManagedImage
     **/
    abstract public void unprepare();

    /**
     * Make this image "sticky".  An image that is sticky will be loaded the
     * normal way when prepare()/load() are called, but it will not be unloaded
     * when the count of active prepares reaches zero due to a call to
     * unprepare().  The calls to makeSticky() are themselves reference-counted;
     * an image is sticky until the sticky count reaches zero due to a call
     * to unmakeSticky().
     * <p>
     * If an image is a tile within a mosaic, the entire mosaic will be held
     * in memory as long as the mosaic tile is loaded.
     * <p>
     * This is equivalent to <code>prepare()</code>.  It's given a different
     * name to emphasize the different role of a sticky image in a GRIN show.
     *
     * @see #unmakeSticky()
     * @see #unprepare()
     * @see #prepare()
     **/
    final public void makeSticky() {
        prepare();
    }

    /**
     * Undo the effects of one call to makeSticky().  This is described in
     * more detail under makeSticky().
     * <p>
     * This is equivalent to <code>unprepare()</code>.  It's given a different
     * name to emphasize the different role of a sticky image in a GRIN show.
     *
     * @see #makeSticky()
     * @see #unprepare()
     * @see #prepare()
     **/
    final public void unmakeSticky() {
        unprepare();
    }

    public boolean equals(Object other) {
        return this == other;
        // ImageManager canonicalizes ManagedImage instances
    }
    
    /**
     * Draw this image into the given graphics context
     **/
    abstract public void draw(Graphics2D gr, int x, int y, Component comp);

    /**
     * Draw this image into the given graphics context, scaled to fit within
     * the given bounds.
     **/
    abstract public void drawScaled(Graphics2D gr, Rectangle bounds,
                                    Component comp);
    /**
     * Draw the the given subsection of the image into a graphics context,
     * without scaling.  
     **/
    abstract public void drawClipped(Graphics2D gr, int x, int y,
                                    Rectangle subsection,
                                    Component comp);
    abstract void destroy();
}
