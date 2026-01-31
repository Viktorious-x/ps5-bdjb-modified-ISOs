
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

package com.hdcookbook.grin.animator;

import com.hdcookbook.grin.util.Debug;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Interface to be implemented by an xlet or other app that uses the
 * animation framework.  This interface provides state change hooks,
 * including initialization.
 *
 * @see AnimationEngine
 **/

public interface AnimationContext {

    /**
     * Run the first part of initialization.  By the time this
     * is done, all of the initXXX methods of the animation
     * framework that need to be called should be.  Notably,
     * the animation framework should be provided with a component.
     * This method is called as the first step in the animation thread.
     * <p>
     * If this initialization is time-consuming, it should poll
     * AnimationEngine.destroyRequested() from time to time, and bail
     * out if needed.  A good way to do this is by calling checkDestroy().
     *
     * @see com.hdcookbook.grin.animator.AnimationEngine#destroyRequested()
     * @see com.hdcookbook.grin.animator.AnimationEngine#checkDestroy()
     **/
    public void animationInitialize() throws InterruptedException;

    /**
     * Run the last part of initialization.  This is called by the
     * animation thread after all of the animation clients are
     * initialized.  This is the last initialization step before
     * running the actual animation loop.  It's a good place
     * to set the state of the UI, e.g. by calling a GRIN
     * Show object's activateSegment() command.  It might also be
     * a reasonable place to call System.gc(), since initialization
     * normally creates a lot of garbage, and the normal running of
     * an xlet hopefully doesn't.
     * <p>
     * If this initialization is time-consuming, it should poll
     * AnimationEngine.destroyRequested() from time to time, and bail
     * out if needed.  A good way to do this is by calling checkDestroy().
     *
     * @see com.hdcookbook.grin.animator.AnimationEngine#destroyRequested()
     * @see com.hdcookbook.grin.Show#activateSegment(com.hdcookbook.grin.Segment)
     **/
    public void animationFinishInitialization() throws InterruptedException;


}
