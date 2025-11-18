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
package com.hdcookbook.grin;

import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;
import java.io.IOException;
import java.awt.Point;

/**
 * This class has three distinct functions.  It's mostly a helper
 * class that acts as a superclass for code that gets generated
 * in a GRIN show file, for the java_command structures and for
 * instantiating GRIN extensions.  This class is also used directly
 * for some built-in GRIN commands.  Doing this kind of functional
 * overloading in one class definition is admittedly not very OO,
 * but experimental data shows that classloading is a moderately
 * expensive operation, so we do this overloading to optimize
 * xlet start-up time.
 * <p>
 * To illustrate the different functions of this class, assume a
 * show that defines a class called MenuShowCommands as the helper
 * classname in the show file.  The functions of GrinXHelper are:
 * <ul>
 *     <li>There's one instance of MenuShowCommands that's not a 
 *         command at all - it's really a factory object that's used 
 *         to call the instantiateXXX() method
 *
 *    <li>For each java_command in the show, there's an instance 
 *        of MenuShowCommands that's set up with the correct 
 *        commandNumber.  MenuShowCommands overrides execute(), 
 *        so the switch statement in the override determines
 *        the meaning of commandNumber, which is automatically generated
 *        by the show compiler.
 *
 *    <li>For each built-in GRIN command that uses GrinXHelper (that is, 
 *        each sync_display or segment_done command) becomes a direct 
 *        instance of GrinXHelper.  In this case, execute() isn't 
 *        overriden, so we get the built-in switch statement.
 * </ul>
 * <p>
 * For the built-in GRIN commands, the direct instances of GrinXHelper
 * get instantiated "automatically" by the GRIN binary reader.  The
 * class GrinXHelper is registered as a built-in class, so that part 
 * "just works" -- it's represented by the integer constant 
 * GRINXHELPER_CMD_IDENTIFIER.  For the MenuShowCommands instances, 
 * they're represented by the (interned) string that hold the 
 * fully-qualified classname of MenuShowCommands, which the binary 
 * reader feeds (once) into Class.forName() so that it can call
 * newInstance().
 * <p>
 * To see how the java_command commands get compiled into the show's
 * subclass of GrinXHelper, see
 * <code>com.hdcookbook.grin.SEShowCommands</code>
 *
 * <h2>Accessing Blu-ray Player Registers</h2>
 *
 * One thing you might want to do in a java_command is access a player register.
 * That's easy to do, and there's even support in GrinView to emulate the
 * values of player registers, so you can do some testing of the control
 * logic of your show on a PC.  In a show file, you can do this by including
 * a static data member of your command subclass, as defined in the
 * java_generated_class section of your show file:
 * 
 * <pre>
 *     XLET_ONLY [[
 *     private final static org.bluray.system.RegisterAccess
 *         registers = org.bluray.system.RegisterAccess.getInstance();
 *     ]]
 *     GRINVIEW_ONLY [[
 *     private final static com.hdcookbook.grin.test.bigjdk.BDRegisterEmulator
 *         registers = com.hdcookbook.grin.test.bigjdk.BDRegisterEmulator.getInstance();
 *     ]]
 * </pre>
 * 
 * With that, your commands can have statements like
 * "int angleNumber = registers.getPSR(3)" or
 * "registers.setGPR(10, 42)".
 * <p>
 * A better pattern might be to divide your director into an abstract superclass,
 * with two implementations of the subclass, one for GrinView and one for a real xlet.
 * That way, the common director can have an abstract method like "getPSR(int)" that you
 * implement for GrinView in terms of an array, or maybe in terms of BDRegisterEmulator.
 * This same abstract director pattern can be used for other player functionality, too.
 * This is a more powerful and general technique than GRINVIEW_ONLY and XLET_ONLY.
 *
 *  @author     Bill Foote (http://jovial.com)
 **/
public class GrinXHelper extends Command implements Node {

    protected int commandNumber;
    protected Command[] subCommands;
    /**
     * The commandNumber for a sync_display command
     **/
    protected final static int SYNC_DISPLAY = 0;
    /**
     * The commandNumber for a segment_done command
     **/
    protected final static int SEGMENT_DONE = 1;
    /**
     * The commandNumber for the GRIN-internal feature setup command
     **/
    final static int FEATURE_SETUP = 2;
    /**
     * The commandNumber for the GRIN internal list-of-commands command
     **/
    protected final static int COMMAND_LIST = 3;
    /**
     * The commandNumber for handling MOUSE_MOVE event
     **/
    public final static int MOUSE_MOVE = 4;
    /**
     * The commandNumber for handling MOUSE_PRESS event
     **/
    public final static int MOUSE_PRESS = 5;
    /**
     * The commandNumber for handling MOUSE_MOVE_DIRECTOR_ONLY event
     **/
    public final static int MOUSE_MOVE_DIRECTOR_ONLY = 6;
    /**
     * The commandNumber for handling MOUSE_PRESS_DIRECTOR_ONLY event
     **/
    public final static int MOUSE_PRESS_DIRECTOR_ONLY = 7;
    /**
     * The commandNumber for handling KEY_TYPED events that are delivered
     * to the director.
     *
     * @see Director#wantsKeyTyped()
     **/
    public final static int HANDLE_KEY_TYPED_FOR_DIRECTOR = 8;

    /*
     * A data member that can be used to hold additional info for
     * executing a command.  For MOUSE_MOVE and MOUSE_PRESS commands,
     * this data is expected to be java.awt.Point that holds x,y of the
     * mouse event.  For HANDLE_KEY_TYPED_FOR_DIRECTOR, it is a Character
     * representing the key that was typed.
     */
    private Object data = null;

    public GrinXHelper(Show show) {
        super(show);
    }

    /**
     * Sets the command number for this class when used as a command.
     * This should only be called as part of initializing this object.
     */
    public void setCommandNumber(int commandNumber) {
        this.commandNumber = commandNumber;
    }

    /**
     * Sets the additional data for this class when used as a command.
     * This should only be called as part of initializing this object.
     */
    public void setCommandObject(Object object) {
        this.data = object;
    }

    /**
     * Sets the sub-commands array for this class when used as a
     * java_command.  This should only be called as part of initializing
     * the object.
     */
    public void setSubCommands(Command[] subCommands) {
        this.subCommands = subCommands;
    }

    /**
     * Run a sub-command.  This supports the GRIN_COMMAND_[[ ]] commands
     * within a command body.  This may only be called within the thread
     * that is calling execute() on the parent command.
     * 
     * @param num  The numeric ID of the command to execute.  This is 
     *             automatically generated by the compiler.
     */
    protected void runSubCommand(int num, Show caller) {
        subCommands[num].execute(caller);
    }

    /**
     * {@inheritDoc}
     **/
    public void readInstanceData(GrinDataInputStream in, int length)
            throws IOException {
        in.readSuperClassData(this);
        this.commandNumber = in.readInt();
        this.subCommands = in.readCommands();
    }

    /**
     * Execute the command.  This method must be overridden in the show's
     * subclass of GrinXHelper, and that override must not call this
     * method.  The implementation of this method in GrinXHelper executes
     * some built-in GRIN commands.
     **/
    public void execute(Show caller) {
        switch (commandNumber) {
            case SYNC_DISPLAY: {
                show.deferNextCommands();
                break;
            }
            case SEGMENT_DONE: {
                // This command only makes sense inside a show, so
                // we are being called within Show.nextFrame(),
                // with the show lock held.  That means we don't have to
                // worry about a race condition with the show moving to
                // a different segment before this gets executed.
                show.doSegmentDone();
                break;
            }
            case FEATURE_SETUP: {
                Segment s = show.getCurrentSegment();
                if (s != null) {
                    s.runFeatureSetup();
                }
                break;
            }
            case COMMAND_LIST: {
                for (int i = 0; i < subCommands.length; i++) {
                    subCommands[i].execute(caller);
                }
                break;
            }
            case MOUSE_MOVE: {
                Point p = (Point) data;
                show.internalHandleMouseMoved(p.x, p.y);
		show.directorHandleMouseMoved(p.x, p.y);
                break;
            }
            case MOUSE_PRESS: {
                Point p = (Point) data;
                show.internalHandleMousePressed(p.x, p.y);
		show.directorHandleMousePressed(p.x, p.y);
                break;
            }
            case MOUSE_MOVE_DIRECTOR_ONLY: {
                Point p = (Point) data;
		show.directorHandleMouseMoved(p.x, p.y);
                break;
            }
            case MOUSE_PRESS_DIRECTOR_ONLY: {
                Point p = (Point) data;
		show.directorHandleMousePressed(p.x, p.y);
                break;
            }
            case HANDLE_KEY_TYPED_FOR_DIRECTOR: {
                Character key = (Character) data;
                show.internalHandleKeyTypedToDirector(key.charValue());
                break;
            }
            default: {
                if (Debug.ASSERT) {
                    Debug.assertFail();
                }
            }
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
     * Instantiate an extension class.  This method must be overridden
     * by the show's sublcass of GrinXHelper.
     **/
    public Node getInstanceOf(Show show, int id) throws IOException {
        throw new IOException();
    }

    /**
     * {@inheritDoc}
     **/
    public String toString() {
        return super.toString() + "(" + commandNumber + ")";
    }
}
