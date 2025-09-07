
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

import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;

import java.awt.Rectangle;
import java.io.IOException;

/**
 * This remote control handler will trigger a set of commands
 * whenever one of a set of keys is pressed.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class CommandRCHandler extends RCHandler implements Node {
   
    protected int mask;
    protected Command[] commands;
    protected boolean wantKeypress;     // otherwise wants key release
    
    public CommandRCHandler() {
        super();
    }

    /**
     * {@inheritDoc}
     **/
    public boolean handleKeyPressed(RCKeyEvent ke, Show caller) {
        if (wantKeypress && (ke.getBitMask() & mask) != 0) {
            for (int i = 0; i < commands.length; i++) {
                commands[i].execute(caller);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     **/
    public boolean handleKeyReleased(RCKeyEvent ke, Show caller) {
        if (!wantKeypress && (ke.getBitMask() & mask) != 0) {
            for (int i = 0; i < commands.length; i++) {
                commands[i].execute(caller);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     **/
    public boolean handleKeyTyped(RCKeyEvent ke, Show caller) {
        return false;
    }
    
    /**
     * {@inheritDoc}
     **/
    public boolean handleMouse(int x, int y, boolean activate) {
        return false;
    }

    /**
     * {@inheritDoc}
     **/
    public Rectangle[] getMouseRects() {
	return null;
    }

    /**
     * {@inheritDoc}
     **/
    public void activate(Segment s) {
    }

    /**
     * {@inheritDoc}
     **/
    public void nextFrame() {
    }
    
    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
                
        in.readSuperClassData(this);
        this.mask = in.readInt();     
        this.wantKeypress = in.readBoolean();
        this.commands = in.readCommands();
    }
}
