
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

import java.awt.Rectangle;

/**
 * Superclass for remote control keypress handlers.  Some also handle
 * mouse events.
 *
 * @author Bill Foote (http://jovial.com)
 */
public abstract class RCHandler {

    protected Show show;
    protected String name;

    public RCHandler() {
    }
    
    public void setShow(Show show) {
        this.show = show;
    }

    public String toString() {
        String nm = getClass().getName();
        int i = nm.lastIndexOf('.');
        if (i >= 0) {
            nm = nm.substring(i+1, nm.length());
        }
        return nm;
    }

    /**
     * Returns the name of this RCHandler, if known.  All public handlers
     * have names; private handlers might not.
     */
     public String getName() {
         return name;
     }
     
     public void setName(String name) {
         this.name = name;
     }
    
    /** 
     * Handle a remote control key press.  This is called on the 
     * animation thread, at a
     * time when it's safe to modify the scene graph.  The show lock will
     * be held, and the show won't be between a call to addDisplayAreas()
     * and paintFrame().
     *
     * @return true if the keypress is used
     **/
    abstract public boolean handleKeyPressed(RCKeyEvent ke, Show caller);
    
    /** 
     * Handle a remote control key release.  This is not supported on
     * all devices.  This is called on the animation thread, at a
     * time when it's safe to modify the scene graph.  The show lock will
     * be held, and the show won't be between a call to addDisplayAreas()
     * and paintFrame().
     *
     * @return true if the keypress is used
     **/
    abstract public boolean handleKeyReleased(RCKeyEvent ke, Show caller);

    /** 
     * Handle a key typed event.  Key typed events can be part of a GRIN
     * extension; see the protected constructor of RCKeyEvent for details.
     * This is called on the animatino thread when it's safe to modify the 
     * scene graph.  The show lock will
     * be held, and the show won't be between a call to addDisplayAreas()
     * and paintFrame().
     *
     * @return true if the keypress is used
     **/
    abstract public boolean handleKeyTyped(RCKeyEvent ke, Show caller);
   
    /** 
     * @return true if something is done with the mouse
     **/
    abstract public boolean handleMouse(int x, int y, boolean activate);

    /**
     * @return  The list of mouse interest areas, or null if there are none.
     */
    abstract public Rectangle[] getMouseRects();

    /**
     * Called for handlers in s when s is activated
     **/
    abstract public void activate(Segment s);

    /**
     * Called by the show to let us know as the model progresses through
     * time.  This can be useful for things like timeouts.
     **/
    abstract public void nextFrame();
    
}
