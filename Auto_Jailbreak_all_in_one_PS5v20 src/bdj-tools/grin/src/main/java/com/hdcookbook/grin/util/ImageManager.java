
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


import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;

/**
 * This class manages a set of images.  It loads and flushes them as needed.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class ImageManager {

    private static Hashtable images = new Hashtable();
    private static Hashtable imageMap = null;   
        // Map of mosaic tile name to MosaicTile
    private static Object lock = new Object();

    private ImageManager() {
    }

    /**
     * Get an image.  Each call to getImage should be balanced
     * by a call to ungetImage when you no longer need the image.
     * Image instances are shared, so this class does reference counting.
     * See the ManagedImage contract for more details.
     * <p>
     * ImageManager first looks within known mosaics, then looks for
     * an individual image file.
     *
     * @see #ungetImage(com.hdcookbook.grin.util.ManagedImage)
     * @see ManagedImage
     **/
    public static ManagedImage getImage(String name) {
        synchronized(lock) {
            ManagedImage im = (ManagedImage) images.get(name);
            if (im == null) {
                if (imageMap != null) {
                    MosaicTile t = (MosaicTile) imageMap.get(name);
                    if (t != null) {
                        im = new ManagedSubImage(name, t.mosaicName, 
                                                 t.placement);
                    } else if (Debug.LEVEL > 0) {
                        Debug.println(name + " not found in image map.");
                    }
                }
                if (im == null) {
                    im = new ManagedFullImage(name);
                }
                images.put(name, im);
            }
            im.addReference();
            return im;
        }
    }

    /**
     * Get an image from a URL.  Any image mosaic won't be consulted; the
     * image will be taken directly from whatever the URL refers to.
     * Each call to getImage should be balanced
     * by a call to ungetImage when you no longer need the image.
     * Image instances are shared, so this class does reference counting.
     * See the ManagedImage contract for more details.
     * <p>
     * ImageManager first looks within known mosaics, then looks for
     * an individual image file.
     *
     * @see #ungetImage(com.hdcookbook.grin.util.ManagedImage)
     * @see ManagedImage
     **/
    public static ManagedImage getImage(URL url) {
        String name = url.toExternalForm();
        synchronized(lock) {
            ManagedImage im = (ManagedImage) images.get(name);
            if (im == null) {
                im = new ManagedFullImage(name, url);
                images.put(name, im);
            }
            im.addReference();
            return im;
        }
    }

    /**
     * This is like <code>getImage(String)</code>, but for the case where
     * you already have the ManagedImage instance.  It just increments the
     * reference count, without needing to do a hash table lookup.
     * See the ManagedImage contract for more details.
     *
     * @throws  IllegalStateException if im.isReferenced() is false
     *
     * @see ManagedImage
     **/
    public static void getImage(ManagedImage im) {
        synchronized(lock) {
            if (!im.isReferenced()) {
                throw new IllegalStateException();
            }
            im.addReference();
        }
    }

    /**
     * Called when an image acquired with getImage is no longer needed.
     * See the ManagedImage contract for more details.
     *
     * @see #getImage(java.lang.String)
     * @see ManagedImage
     **/
    public static void ungetImage(ManagedImage im) {
        synchronized(lock) {
            im.removeReference();
            if (!im.isReferenced()) {
                images.remove(im.getName());
                im.destroy();
            }
        }
    }

    static void readImageMap(String fileName, Hashtable map) throws IOException 
    {
        // Reads the file written by 
        // com.hdcookbook.grin.build.mosaic.MosaicMaker.makeMosaics()
        // This maps the original image file name to the name of a
        // mosaic image, and the position within that mosaic.
        DataInputStream dis = null;
        try {
            URL u = AssetFinder.getURL(fileName);
            if (u == null) {
                throw new IOException("No image map " + fileName);
            }
            dis = new DataInputStream(new BufferedInputStream(u.openStream()));

            int n = dis.readInt();
            String[] mosaics = new String[n];
            for (int i = 0; i < n; i++) {
                mosaics[i] = dis.readUTF();
            }

            n = dis.readInt();
            for (int i = 0; i < n; i++) {
                String tileName = dis.readUTF();
                MosaicTile t = new MosaicTile();
                t.mosaicName = mosaics[dis.readInt()];
                t.placement = new Rectangle();
                t.placement.x = dis.readInt();
                t.placement.y = dis.readInt();
                t.placement.width = dis.readInt();
                t.placement.height = dis.readInt();
                map.put(tileName, t);
            }
            if (Debug.ASSERT && dis.read() != -1) {
                Debug.assertFail();
            }
            // dis.close is in the finally block
        } catch (IOException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
            }
            if (Debug.ASSERT) {
                Debug.assertFail();
            }
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    static void setImageMap(Hashtable map) {
        synchronized(lock) {
            imageMap = map;
        }
    }
}
