
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

package com.hdcookbook.grin.input;

import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.awt.Color;
import java.awt.event.KeyEvent;


/**
 * This class is used to manage constants related to the remote
 * control keys.  Instances of RCKeyEvent can be queued as GRIN
 * commands; when they are executed, the show will process the
 * keystroke.  The built-in RCKeyEvent class is used for key pressed
 * and key released events, taken from a strictly limited set of keys.
 * <p>
 * If desired, a GRIN extension can subclass RCKeyEvent to add support for
 * key typed events.  GRIN doesn't really support key typed events, but
 * it does provide support for queuing them and dispatching them in the
 * animation thread (as is done with key pressed and released events).
 *
 * @author Bill Foote (http://jovial.com)
 */
public class RCKeyEvent extends Command {

    //
    // Constants for key pressed events.  Key released events are built
    // lazily from the corresponding key pressed events.  Key typed events
    // are not handled here; they are the responsibility of a GRIN
    // extension, done as a subclass of RCKeyEvent.
    //
    public static RCKeyEvent KEY_0;
    public static RCKeyEvent KEY_1;
    public static RCKeyEvent KEY_2;
    public static RCKeyEvent KEY_3;
    public static RCKeyEvent KEY_4;
    public static RCKeyEvent KEY_5;
    public static RCKeyEvent KEY_6;
    public static RCKeyEvent KEY_7;
    public static RCKeyEvent KEY_8;
    public static RCKeyEvent KEY_9;
    public static RCKeyEvent KEY_RIGHT;
    public static RCKeyEvent KEY_LEFT;
    public static RCKeyEvent KEY_UP;
    public static RCKeyEvent KEY_DOWN;
    public static RCKeyEvent KEY_ENTER;
    public static RCKeyEvent KEY_POPUP_MENU;
    public static RCKeyEvent KEY_PLAY;
    public static RCKeyEvent KEY_STOP;
    public static RCKeyEvent KEY_STILL_OFF;
    public static RCKeyEvent KEY_TRACK_NEXT;
    public static RCKeyEvent KEY_TRACK_PREV;
    public static RCKeyEvent KEY_FAST_FWD;
    public static RCKeyEvent KEY_REWIND;
    public static RCKeyEvent KEY_PAUSE;
    public static RCKeyEvent KEY_SECONDARY_VIDEO_ENABLE_DISABLE;
    public static RCKeyEvent KEY_SECONDARY_AUDIO_ENABLE_DISABLE;
    public static RCKeyEvent KEY_PG_TEXTST_ENABLE_DISABLE;
    public static RCKeyEvent KEY_INFO;

    //  NOTE:  If you add a new key, check the note at the end of
    //         generatePerfectHashOfEventCodes().

    /**
     * The red color key.
     *
     * @see com.hdcookbook.grin.util.AssetFinder#getColorKeyCode(java.awt.Color)
     **/
    public static RCKeyEvent KEY_RED;

    /**
     * The green color key.
     *
     * @see com.hdcookbook.grin.util.AssetFinder#getColorKeyCode(java.awt.Color)
     **/
    public static RCKeyEvent KEY_GREEN;

    /**
     * The yellow color key.
     *
     * @see com.hdcookbook.grin.util.AssetFinder#getColorKeyCode(java.awt.Color)
     **/
    public static RCKeyEvent KEY_YELLOW;

    /**
     * The blue color key.
     *
     * @see com.hdcookbook.grin.util.AssetFinder#getColorKeyCode(java.awt.Color)
     **/
    public static RCKeyEvent KEY_BLUE;

    private static Hashtable keyByName;
    private static RCKeyEvent[] keyByEventCode; // see getKeyByEventCode()

    static {
        keyByName = new Hashtable();
        KEY_0 = new RCKeyEvent("0", KeyEvent.VK_0, 0x00000001);
        KEY_1 = new RCKeyEvent("1", KeyEvent.VK_1, 0x00000002);
        KEY_2 = new RCKeyEvent("2", KeyEvent.VK_2, 0x00000004);
        KEY_3 = new RCKeyEvent("3", KeyEvent.VK_3, 0x00000008);
        KEY_4 = new RCKeyEvent("4", KeyEvent.VK_4, 0x00000010);
        KEY_5 = new RCKeyEvent("5", KeyEvent.VK_5, 0x00000020);
        KEY_6 = new RCKeyEvent("6", KeyEvent.VK_6, 0x00000040);
        KEY_7 = new RCKeyEvent("7", KeyEvent.VK_7, 0x00000080);
        KEY_8 = new RCKeyEvent("8", KeyEvent.VK_8, 0x00000100);
        KEY_9 = new RCKeyEvent("9", KeyEvent.VK_9, 0x00000200);
        KEY_RIGHT = new RCKeyEvent("right", KeyEvent.VK_RIGHT, 0x00000400);
        KEY_LEFT = new RCKeyEvent("left", KeyEvent.VK_LEFT,    0x00000800);
        KEY_UP = new RCKeyEvent("up", KeyEvent.VK_UP,          0x00001000);
        KEY_DOWN = new RCKeyEvent("down", KeyEvent.VK_DOWN,    0x00002000);
        KEY_ENTER = new RCKeyEvent("enter", KeyEvent.VK_ENTER, 0x00004000);

        // For the color keys, I just lifted the constants out of the
        // HAVi stubs.  This avoids a compilation dependency on HAVi.
        KEY_RED = new RCKeyEvent("red", 
                        AssetFinder.getColorKeyCode(Color.red),   0x00008000);
        KEY_GREEN = new RCKeyEvent("green", 
                        AssetFinder.getColorKeyCode(Color.green), 0x00010000);
        KEY_YELLOW = new RCKeyEvent("yellow", 
                        AssetFinder.getColorKeyCode(Color.yellow),0x00020000);
        KEY_BLUE = new RCKeyEvent("blue", 
                        AssetFinder.getColorKeyCode(Color.blue),  0x00040000);

        // For the popup key and other BD-specific keys, I just use the 
        // integer value, rather than introduce
        // a compile-time AI dependency.  The values are documented in
        // the DAVIC and Blu-ray specifications -- look for the VK_
        // key constants.
        KEY_POPUP_MENU = new RCKeyEvent("popup_menu", 461, 0x00080000);
        KEY_PLAY       = new RCKeyEvent("play",       415, 0x00100000);
        KEY_STOP       = new RCKeyEvent("stop",       413, 0x00200000);
        KEY_STILL_OFF  = new RCKeyEvent("still_off",  462, 0x00400000);
        KEY_TRACK_NEXT = new RCKeyEvent("track_next", 425, 0x00800000);
        KEY_TRACK_PREV = new RCKeyEvent("track_prev", 424, 0x01000000);
        KEY_FAST_FWD   = new RCKeyEvent("fast_fwd",   417, 0x02000000);
        KEY_REWIND     = new RCKeyEvent("rewind",     412, 0x04000000);
        KEY_PAUSE      = new RCKeyEvent("pause",       19, 0x08000000);
        KEY_SECONDARY_VIDEO_ENABLE_DISABLE 
                       = new RCKeyEvent("secondary_video_enable_disable",
                                                      464, 0x10000000);
        KEY_SECONDARY_AUDIO_ENABLE_DISABLE 
                       = new RCKeyEvent("secondary_audio_enable_disable",
                                                      463, 0x20000000);
        KEY_PG_TEXTST_ENABLE_DISABLE
                       = new RCKeyEvent("pg_textst_enable_disable",
                                                      465, 0x40000000);
  
        // The info key support is mandatory in OCAP (tru2way) devices.
        // The constant, 457, is defined in org.havi.ui.event.HRcEvent.
        KEY_INFO
                       = new RCKeyEvent("info",
                                                      457, 0x80000000);


        RCKeyEvent[] keys = new RCKeyEvent[] {
            KEY_0, KEY_1, KEY_2, KEY_3, KEY_4, 
            KEY_5, KEY_6, KEY_7, KEY_8, KEY_9,
            KEY_RIGHT, KEY_LEFT, KEY_UP, KEY_DOWN,
            KEY_ENTER, KEY_RED, KEY_GREEN, KEY_YELLOW, KEY_BLUE,
            KEY_POPUP_MENU, KEY_PLAY, KEY_STOP, KEY_STILL_OFF,
            KEY_TRACK_NEXT, KEY_TRACK_PREV, KEY_FAST_FWD, KEY_REWIND,
            KEY_PAUSE, KEY_SECONDARY_VIDEO_ENABLE_DISABLE,
            KEY_SECONDARY_AUDIO_ENABLE_DISABLE,
            KEY_PG_TEXTST_ENABLE_DISABLE,
            KEY_INFO
        };
        //
        // IMPORTANT NOTE:  If a key is added to this table, then
        // you need to go into generatePerfectHashOfEventCodes()
        // and make sure it still produces a reasonable result.  I recommend
        // starting the algorithm off at what you know to be the right
        // answer, to save startup time.
        //
        keyByEventCode = generatePerfectHashOfEventCodes(keys);
    }
    
    private String name;    // human-readable name, used in script file.
    private int keyCode;    // java.awt.event.KeyEvent.getKeyCode()
    private int mask;       // Mask value that we assign

    private RCKeyEvent keyReleased;
        // The key released event for this key.  If this instance represents
        // a key pressed event, this will be null, or a value that's != this.
        // For a subclass defined to represent key typed events, this will
        // be null, and won't be accessed.
    
    private RCKeyEvent(String name, int keyCode, int mask) {
        super(null);    // The show data member of Command will be null
        this.name = name;
        this.keyCode = keyCode;
        this.mask = mask;
        keyByName.put(name, this);
    }


    /**
     * Initialize a new key typed event.  This constructor is not used by
     * the internal GRIN support for key pressed and key released handlers;
     * it is intended for a subclass of RCKeyEvent that extends GRIN to
     * support key typed events.  It is up to the extension whether or not it
     * chooses to pre-allocate a number of constants, as is done in GRIN, or
     * to allocate a new one with each arriving event.
     * <p>
     * Not all GEM devices support key typed events, nor do they have keyboards.
     * If you extend GRIN to add support for key typed events, it's up to you
     * how to generate the needed key typed events, e.g. by perhaps adding a 
     * virtual keyboard.
     * <p>
     * NOTE:  If you subclass RCKeyEvent, be sure to override execute(Show)
     *
     * @param   keyCode The VK code of the key.  This can be zero.  A subclass
     *                  will likely add a char data member to contain the 
     *                  character that is typed.
     *
     * @param   mask    The bitmask of this key event.  A bit in the bitmask
     *                  can be used to identify groups of related keys.  The
     *                  bitmask is used to identify whether or not a segment
     *                  is interested in receiving key typed events from the
     *                  corresponding key group.
     *
     * @see #execute(Show)
     * @see Segment#keyTypedInterest
     **/
    protected RCKeyEvent(int keyCode, int mask) {
        super(null);    // The show data member of Command will be null
        this.keyCode = keyCode;
        this.mask = mask;
    }

        // constructor for key released instances.  See getKeyReleased().
    private RCKeyEvent() {
        super(null);
    }

    /**
     * Get a developer-friendly name of this key event.
     * Useful for debugging.
     **/
    public String getName() {
        return name;
    }

    /**
     * @return the VK_ key code corresponding to this key
     **/
    public int getKeyCode() {
        return keyCode;
    }

    /**
     * A bitmask value is assigned to each remote control key.  Since
     * there are 27 keys we consider and 32 bits in an int, there's 
     * room in an int.  Using a bitmask lets us check a key against
     * a set of expected keys very efficiently.
     * 
     * @return The mask for this key.
     **/
     public int getBitMask() {
        return mask;
    }

    /**
     * Return true if this is represents key press, and false if it represents
     * a key release.
     **/
    public boolean isKeyPress() {
        return keyReleased != this;     // see keyReleased comment above
    }

    /**
     * Give the RCKeyEvent for this VK code that represents a key release.
     **/
    public synchronized RCKeyEvent getKeyReleased() {
        if (this.keyReleased == null) {
            this.keyReleased = new RCKeyEvent();        // doesn't add to table
            keyReleased.name = this.name;
            keyReleased.keyCode = this.keyCode;
            keyReleased.mask = this.mask;
            keyReleased.keyReleased = this.keyReleased;
        }
        return keyReleased;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This version of execute(Show) is only for use by the built-in GRIN
     * support for key pressed and key released events.  If you subclass
     * RCKeyEvent to support key typed events, be sure to override this
     * method so that it calls Show.internalHandleKeyTypedEvent().
     **/
    public void execute(Show caller) {
        if (isKeyPress()) {
            caller.internalHandleKeyPressed(this, caller);
        } else {
            caller.internalHandleKeyReleased(this, caller);
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void execute() {
        if (Debug.ASSERT) {
            Debug.assertFail();
        }
    }

    /**
     * Look up the RCKeyEvent corresponding to a VK_ key code.
     * This method is very fast and creates no garbage.  It
     * uses a perfect hash function, an idea I lifted from
     * http://www.onjava.com/pub/a/onjava/2001/01/25/hash_functions.html
     * <p>
     * This returns the RCKeyEvent corresponding to a key press.  
     *
     * @return The RCKeyEvent, or null if there's no corresponding event
     *
     * @see #getKeyReleased()
     **/
    public static RCKeyEvent getKeyByEventCode(int key) {
        RCKeyEvent result = keyByEventCode[key % keyByEventCode.length];
        if (result != null && result.keyCode == key) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * Look up the RCKeyEvent by its logical name.
     *
     * @return the RCKeyEvent, or null if there's no corresponding event.
     **/
    public static RCKeyEvent getKeyByName(String name) {
        return (RCKeyEvent) keyByName.get(name);
    }

    /**
     * Returns a vector of events turned-on by the given mask.
     */
    public static Vector getEventsFromMask(int mask) {
        Collection values = keyByName.values();
        Vector result = new Vector();
        for (Iterator itr = values.iterator(); itr.hasNext();) {
            RCKeyEvent keyEvent = (RCKeyEvent)itr.next();
            if ((mask & keyEvent.getBitMask()) != 0) {
                result.add(keyEvent);
            }
        }
        return result;  
    }

    private static RCKeyEvent[] 
            generatePerfectHashOfEventCodes(RCKeyEvent[] keys) 
    {
        //
        // This is a time-consuming consistency check, so I disable it
        // even when assertions are enabled.  It only needs to be re-run
        // when a key is added to the table of keys.
        //
        if (false && Debug.ASSERT) {
            for (int i = 0; i < keys.length-1; i++) {
                for (int j = i+1; j < keys.length; j++) {
                    if (keys[i].getKeyCode() == keys[j].getKeyCode()
                        || keys[i].getBitMask() == keys[j].getBitMask())
                    {
                        Debug.assertFail(keys[i].getName() + " key is same as "
                                         + keys[j].getName());
                    }
                }
            }
        }
        int remainder = 78;     // At 31 keys, algorithm terminates there.
        for (;;) {
            boolean ok = true;
            RCKeyEvent[] result = new RCKeyEvent[remainder];
            for (int i = 0; i < keys.length; i++) {
                int x = keys[i].keyCode % remainder;
                if (result[x] != null) {
                    ok = false;
                    break;
                } else {
                    result[x] = keys[i];
                }
            }
            if (ok) {
                return result;
            }
            remainder++;
                // This might look a little crazy, but it terminates
                // after eleven iterations with the 20 keys we have
                // defined (as of 12/19/06).  Given what I saw in
                // http://www.onjava.com/pub/a/onjava/2001/01/25/hash_functions.html ,
                // this is a safe thing to do.  Still, if a key is added,
                // it wouldn't hurt to check that this terminates quickly.
            if (Debug.ASSERT && remainder > 120) {
                Debug.assertFail("Find a better algorithm!");
            }
        }
    }
}
