
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

package com.hdcookbook.grin.commands;

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.input.VisualRCHandler;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;
import java.io.IOException;


/**
 * This command is used to set the state of a visual RC handler.  It's
 * useful when going into a segment that contains such a handler, to
 * put things into a known state.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class SetVisualRCStateCommand extends Command implements Node {
    
    protected boolean activated;
    protected int state;
    protected VisualRCHandler handler;
    protected boolean runCommands;
    protected int gridNumber;


    public SetVisualRCStateCommand(Show show) {
        super(show);
    }

    /**
     * Constructor for use by xlets that want to set a handler state
     **/
    public SetVisualRCStateCommand(Show show, boolean activated, int state, 
                                   VisualRCHandler handler,
                                   boolean runCommands)  
    {
        this(show, activated, state, handler, runCommands, -1);
    }

    /**
     * Constructor for use by xlets that want to set a handler state
     **/
    public SetVisualRCStateCommand(Show show, boolean activated, int state, 
                                   VisualRCHandler handler,
                                   boolean runCommands, int gridNumber)
    {
        this(show);
        this.activated = activated;
        this.state = state;
        this.handler = handler;
        this.runCommands = runCommands;
        this.gridNumber = gridNumber;
    }
    
    public boolean getActivated() {
        return activated;
    }
    
    public int getState() {
        return state;
    }
    
    public VisualRCHandler getVisualRCHandler() {
        return handler;
    }
    
    public boolean getRunCommands() {
        return runCommands;
    }
    
    public void execute(Show caller) {
        handler.setState(state, activated, runCommands, gridNumber);
    }

    public void execute() {
        if (Debug.ASSERT) {
            Debug.assertFail();
        }
    }
    
    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
                
        in.readSuperClassData(this);
        
        this.activated = in.readBoolean();
        this.state = in.readInt();
        this.handler = (VisualRCHandler) in.readRCHandlerReference();
        this.runCommands = in.readBoolean();
        this.gridNumber = in.readInt();
    }
    
}
