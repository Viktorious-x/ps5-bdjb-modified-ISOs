
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

import java.awt.Toolkit;
import java.awt.Image;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.util.Hashtable;


/**
 * This class serves two functions.  First, it has a bunch of
 * static methods that are used by GRIN and can be used by applications
 * to find various resources.  Secondly, an xlet (or other application)
 * can create an instance of AssetFinder and bind it to us with the method
 * setHelper.  If this is done, AssetFinder will try to locate assets
 * by first querying the helper.  This way, an Xlet can make decisions
 * about where to look for stuff.  For example, a signed BD-Live xlet
 * could set up a search path on the BUDA.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class AssetFinder  {
    
    private static AssetFinder helper = null;
    private static Class theClass = AssetFinder.class;
    private static String[] appJarPath;
    private static File[] filePath;

    /**
     * See setHelper()
     **/
    protected AssetFinder() {
    }

    /**
     * An xlet can make an instance of a subclass of AssetFinder, and
     * connect it to us by calling this method.  Whenever the AssetFinder
     * is looking for something, it will first check with the helper,
     * by calling one of the helperXXX methods.
     **/
    public static void setHelper(AssetFinder helperArg) {
        helper = helperArg;
    }

    /**
     * @param  appJarPathArg  A list of paths within the classpath
     *                        of the app, for use by Class.getResource
     * @param  filePathArg    A list of paths in the filesystem,
     *                        e.g. from mounting a DSMCC carousel.
     **/
    public static void setSearchPath(String[] appJarPathArg, 
                                     File[] filePathArg) 
    {
        if (appJarPathArg == null) {
            appJarPath = null;
        } else {
            appJarPath = new String[appJarPathArg.length];
            for (int i = 0; i < appJarPathArg.length; i++) {
                if (appJarPathArg[i].endsWith("/")) {
                    appJarPath[i] = appJarPathArg[i];
                } else {
                    appJarPath[i] = appJarPathArg[i] + "/";
                }
            }
        }
        filePath = filePathArg;
    }

    /**
     * Get the filePathArg as set by setSearchPath()
     **/
    public static String[] getSearchPathJar() {
        return appJarPath;
    }

    /**
     * Get the appJarPathArg as set by setSearchPath()
     **/
    public static File[] getSearchPathFile() {
        return filePath;
    }
    
    /**
     * Sets the image map.  This is used for mosaic
     * images:  The image map translates a logical image name
     * into a tuple (mosaic image, rect within mosaic).  This
     * must be set after the search path, since the search path
     * is used to load the image map.
     * <p>
     * If an image map was previously in place, it will be replaced
     * with the new map as an atomic operation when the image map file
     * has been completely read.
     * 
     * @param   mapFile The name of an image map file produced by MosaicMaker 
     * 
     * @see #setImageMap(String[])
     **/
    public static void setImageMap(String mapFile) {
        Hashtable map = new Hashtable();
        try {
            ImageManager.readImageMap(mapFile, map);
            ImageManager.setImageMap(map);
        } catch (IOException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
            }
            if (Debug.ASSERT) {
                Debug.assertFail();
            }
        }
    }

    /**
     * Sets the image map to the concatination of the maps in the given
     * files.  This is used for mosaic
     * images:  The image map translates a logical image name
     * into a tuple (mosaic image, rect within mosaic).  This
     * must be set after the search path, since the search path
     * is used to load the image map.
     * <p>
     * If an image map was previously in place, it will be replaced
     * with the new map as an atomic operation when the image map file
     * has been completely read.
     * <p>
     * It is up to the callee to make sure that there are no name
     * collisions, either in the name of the source images, or in the
     * names of the mosaics.
     * 
     * @param   mapFiles The name of image map files produced by MosaicMaker 
     *
     * @see #setImageMap(String)
     **/
    public static void setImageMap(String[] mapFiles) {
        Hashtable map = new Hashtable();
        try {
            for (int i = 0; i < mapFiles.length; i++) {
                ImageManager.readImageMap(mapFiles[i], map);
            }
            ImageManager.setImageMap(map);
        } catch (IOException ex) {
            Debug.printStackTrace(ex);
            Debug.assertFail();
        }
    }

    /**
     * Get a URL to an asset.  If the asset doesn't exist, emits debug
     * messages and returns null.
     *
     * @param path      A string, relative to the search path for assets
     *                  TODO: Maybe need to search locators, too
     **/
    public static URL getURL(String path) {
        URL u = tryURL(path);
        if (Debug.ASSERT && u == null) {
            if (appJarPath != null) {
                for (int i = 0; i < appJarPath.length; i++) {
                    Debug.println("   Tried " + appJarPath[i] + path);
                }
            }
            if (filePath != null) {
                for (int i = 0; i < filePath.length; i++) {
                    Debug.println("   Tried " + new File(filePath[i], path));
                }
            }
            Debug.println();
            Debug.println("****  Resource " + path + " does not exist!  ****");
            Debug.println();
        }
        return u;
    }

    /**
     * Try to get an asset that might not be there.  If it's not,
     * return null.
     **/
    public static URL tryURL(String path) {
        if (helper != null) {
            URL u = helper.tryURLHelper(path);
            if (u != null) {
                return u;
            }
        }
        if (Debug.ASSERT && appJarPath == null && filePath == null) {
            Debug.assertFail("Search path not set.");
        }
        if (appJarPath != null) {
            for (int i = 0; i < appJarPath.length; i++) {
                URL u = theClass.getResource(appJarPath[i] + path);
                if (u != null) {
                    return u;
                }
            }
        }
        if (filePath != null) {
            for (int i = 0; i < filePath.length; i++) {
                File f = new File(filePath[i], path);
                if (f.exists()) {
                    try {
                        return f.toURL();
                            // When compiled against desktop JDK, this will
                            // generate a warning about the method being
                            // deprecated.  Ignore that; the suggested
                            // replacement is "f.toURI().toURL()", which
                            // doesn't exist in PBP.
                    } catch (Exception ex) {
                        // This should never happen
                        if (Debug.LEVEL > 0) {
                            Debug.printStackTrace(ex);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Helper subclass can override this in order to search for
     * generic assets, like images.
     **/
    protected URL tryURLHelper(String path) {
        return null;
    }


    /**
     * Efficiently get a Color that's used within a Show.  This method
     * might someday share one Color instance for multiple calls
     * with the same rgba values.
     **/
    public static Color getColor(int r, int g, int b, int a) {
        Color c = new Color(r, g, b, a);
        return c;
        // We could consider canonicalizing Color instances for efficiency.  Not
        // sure if this should use weak refs, or just a static 
        // AssetFinder.clear() method.
    }

    /**
     * Get a Font that's used within a Show.  This method
     * might someday share one Font instance and any needed font
     * factories for multiple calls with the same specifications.
     **/
    public static Font getFont(String fontName, int style, int size) {
        if (helper != null) {
            Font f = helper.getFontHelper(fontName, style, size);
            if (f != null) {
                return f;
            }
            if (Debug.LEVEL > 0 && !("SansSerif".equals(fontName))) {
                        // SansSerif is the one font guaranteed to be present
                        // on all players.  It's useful for debugging, but since
                        // there's no guarantee of appearance, it probably shouldn't
                        // be used in production.
                Debug.println("*** Helper didn't find font " + fontName);
            }
        }
        return new Font(fontName, style, size);
    }

    /**
     * Helper subclass can override this in order to search for
     * generic assets, like images.
     **/
    protected Font getFontHelper(String fontName, int style, int size) {
        return null;
    }

    /**
     * Get an image buffer that's suitable for use double-buffering
     * drawing to the componet c.  This is functionally equivalent to
     * a java.awt.BufferedImage obtained from:
     * <pre>
     *
     *    c.getGraphicsConfiguration().createCompatibleImage(width, height)
     *
     * </pre>
     * In GEM-based systems, it is recommended that this be a
     * <code>DVBBufferedImage</code>, for improved native memory management.  See
     * <a href="http://wiki.java.net/bin/view/Mobileandembedded/BDJImageMemoryManagement">http://wiki.java.net/bin/view/Mobileandembedded/BDJImageMemoryManagement</a>
     * for a discussion of why.
     *
     * @return an off-screen image buffer
     *
     * @see #createCompatibleImageBufferHelper(java.awt.Component, int, int)
     * @see #createGraphicsFromImageBuffer(java.awt.Image)
     * @see #destroyImageBuffer(java.awt.Image)
     **/
    public static Image createCompatibleImageBuffer(Component c, 
                                                    int width, int height) 
    {
        if (helper != null) {
            Image im = helper.createCompatibleImageBufferHelper(c,width,height);
            if (im != null) {
                return im;
            }
        }
        return c.getGraphicsConfiguration().createCompatibleImage(width,height);
    }

    /**
     * Subclasses can override this to return a different kind of buffered
     * image, e.g. <code>DVBBufferedImage</code>
     *
     * @see #createCompatibleImageBufferHelper(java.awt.Component, int, int)
     **/
    protected Image createCompatibleImageBufferHelper(Component c,
                                                     int width, int height)
    {
        return null;
    }

    /**
     * Create a Graphic2D image for drawing into the given image buffer.
     * This image buffer must have been obtained from
     * <code>createCompatibleImageBuffer()</code>.
     *
     * @see #createCompatibleImageBuffer(java.awt.Component, int, int)
     **/
    public static Graphics2D createGraphicsFromImageBuffer(Image buffer) {
        if (helper != null) {
            Graphics2D g = helper.createGraphicsFromImageBufferHelper(buffer);
            if (g != null) {
                return g;
            }
        }
        return ((BufferedImage) buffer).createGraphics();
    }

    /**
     * Subclasses can override this to work with a different kind of buffered
     * image, e.g. <code>DVBBufferedImage</code>
     *
     * @see #createGraphicsFromImageBuffer(java.awt.Image)
     **/
    protected Graphics2D createGraphicsFromImageBufferHelper(Image buffer) {
        return null;
    }

    /**
     * Destroy an image buffer, freeing native resources that it uses
     * (such as native memory used to hold a pixmap).  In Desktop java,
     * this does nothing, because this is left to the garbage collector.
     * In GEM-based systems, it should be equivalent to
     * DVBBufferedImage.dispose(). See
     * <a href="http://wiki.java.net/bin/view/Mobileandembedded/BDJImageMemoryManagement">http://wiki.java.net/bin/view/Mobileandembedded/BDJImageMemoryManagement</a>
     * for a discussion of images and memory management.
     *
     * @see #createCompatibleImageBuffer(java.awt.Component, int, int)
     **/
    public static void destroyImageBuffer(Image buffer) {
        if (helper != null) {
            helper.destroyImageBufferHelper(buffer);
        }
    }

    /**
     * Subclasses can override this to work with a different kind of buffered
     * image, e.g. <code>DVBBufferedImage</code>
     *
     * @see #destroyImageBuffer(java.awt.Image)
     **/
    protected void destroyImageBufferHelper(Image buffer) {
    }
    
    /**
     * Load an image from the given path.
     *
     * @param path should be an absolute path within asset finder's path.
     **/
    public static Image loadImage(String path) {
        if (helper != null) {
            Image im = helper.loadImageHelper(path);
            if (im != null) {
                return im;
            }
        }

        Toolkit tk = Toolkit.getDefaultToolkit();
        URL url = getURL(path);
        return tk.createImage(url);
    }

    /**
     * Helper subclass can override this in order to search for
     * images in some special way.  If the helper doesn't find one, the
     * default AssetFinder implementation will call tryURL(), which 
     * the helper can also override.  If you override tryURL() such
     * that images can be located, there's no reason to override
     * this method too.
     **/
    protected Image loadImageHelper(String path) {
        return null;
    }

    /**
     * Get the key code for the given color key.  The HD Cookbook has
     * an algorithm that an xlet might choose to use, if it wants
     * to assign the color keys in a way that works on most players.
     * See the HD cookbook page 19-4, "Those Crazy Color Keys" for an
     * algorithm.
     *
     * @param c         A color that is == to one of the standard
     *                  constants Color.red, Color.green, Color.yellow
     *                  or Color.blue.
     *
     * @return  A HAVi key code in the range 403..406 inclusive
     **/
    public static int getColorKeyCode(Color c) {
        int code = -1;
        if (helper != null) {
            code = helper.getColorKeyCodeHelper(c);
        }
        if (code == -1) {
            if (c == Color.red) {
                return 403;     // VK_COLORED_KEY_0
            } else if (c == Color.green) {
                return 404;     // VK_COLORED_KEY_1
            } else if (c == Color.yellow) {
                return 405;     // VK_COLORED_KEY_2
            } else if (c == Color.blue) {
                return 406;     // VK_COLORED_KEY_3
            } 
        } 
        if (Debug.ASSERT) {
            if (code < 403 || code > 406) {
                Debug.assertFail();
            }
        }
        return code;
    }

    /**
     * Get the key code for the given color key.  Xlets may override
     * this to do an intelligent assignment of the color keys.
     * See the HD cookbook page 19-4, "Those Crazy Color Keys" for an
     * algorithm.
     * <p>
     * An implementation of the cookbook's color key algorithm is
     * also available, in the GrinXlet directory project.  See, for example,
     * xlets/GrinXlet/src/deploy/com/hdcookbook/grinxlet/GrinXlet.java
     *
     * @param c         A color that is == to one of the standard
     *                  constants Color.red, Color.green, Color.yellow
     *                  or Color.blue.
     *
     * @return  A HAVi key code in the range 403..406 inclusive
     **/
    protected int getColorKeyCodeHelper(Color c) {
        return -1;
    }

    /**
     * Called when the disc playback should abort.  This should only
     * be called when there's a fatal error, like an assertion failure.
     * The expected behavior is immediate termination - like
     * System.exit(1) on big JDK, or ejecting the disc on a player.
     **/
    public static void abort() {
        if (helper != null) {
            helper.abortHelper();
        }
        throw new RuntimeException("ABORT");
    }

    /**
     * Called when the disc playback should abort.  This should only
     * be called when there's a fatal error, like an assertion failure.
     * The expected behavior is immediate termination - like
     * System.exit(1) on big JDK, or ejecting the disc on a player.
     **/
    protected void abortHelper() {
    }

    /**
     * Called by ManagedFullImage when an image finishes loading.  
     * Note that the calling thread might be holdling locks, so the 
     * implementation of this method should never acquire non-local locks.
     *
     * @see #notifyLoadedHelper(ManagedFullImage)
     * @see #notifyUnloaded(ManagedFullImage, int, int)
     **/
    public static void notifyLoaded(ManagedFullImage mi) {
        if (Debug.LEVEL > 1) {
            Debug.println("Loaded image " + mi.getName());
        }
        if (helper != null) {
            helper.notifyLoadedHelper(mi);
        }
    }

    /**
     * Called when a ManagedFullImage has finished loading.  By default
     * this does nothing, but it can be overridden, e.g. for resource
     * usage tracking.  This will be called in one of the platform's
     * image fetcher threads.
     *
     * @see #notifyLoaded(ManagedFullImage)
     * @see #notifyUnloadedHelper(ManagedFullImage, int, int)
     **/
    protected void notifyLoadedHelper(ManagedFullImage mi) {
        // do nothing
    }

    /**
     * Called by ManagedFullImage when a loaded image has been
     * unloaded (flushed).  This will be called from
     * ManagedImage.unprepare(), which normally happens in the
     * animation thread.
     * Note that the calling thread might be holdling locks, so the 
     * implementation of this method should never acquire non-local locks.
     *
     * @see #notifyUnloadedHelper(ManagedFullImage, int, int)
     * @see #notifyLoaded(ManagedFullImage)
     **/
    public static void notifyUnloaded(ManagedFullImage mi, 
                                      int width, int height)
    {
        if (Debug.LEVEL > 1) {
            Debug.println("Unloaded image " + mi.getName());
        }
        if (helper != null) {
            helper.notifyUnloadedHelper(mi, width, height);
        }
    }

    /**
     * Called when a ManagedFullImage has been unloaded (flushed).  By 
     * default this does nothing, but it can be overridden, e.g. for resource
     * usage tracking.
     * Note that the calling thread might be holdling locks, so the 
     * implementation of this method should never acquire non-local locks.
     *
     * @see #notifyLoaded(ManagedFullImage)
     * @see #notifyLoadedHelper(ManagedFullImage)
     **/
    protected void notifyUnloadedHelper(ManagedFullImage mi, 
                                        int width, int height) 
    {
        // do nothing
    }
}
