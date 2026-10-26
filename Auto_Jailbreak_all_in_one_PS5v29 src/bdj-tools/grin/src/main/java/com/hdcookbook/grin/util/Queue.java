
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

import java.util.NoSuchElementException;

/**
 * This class maintains a queue that avoids generating heap traffic.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class Queue {

   
    private Object[] buffer;
    private int addPos = 0;
    private int removePos = 0;
    private Queue overflow = null;

    /**
     * Create a queue.  It will fill up to the given capacity
     * without creating any new objects.  If the queue gets more
     * entries than this, it will start allocating objects.
     **/
    public Queue(int capacity) {
        buffer = new Object[capacity];
    }

    public synchronized void add(Object el) {
        int n = (addPos + 1) % buffer.length;
        if (n != removePos) {
            buffer[addPos] = el;
            addPos = n;
        } else {        // overflow
            if (overflow == null) {
                overflow = new Queue(buffer.length);
            }
            overflow.add(el);
        }
    }

    public synchronized boolean isEmpty() {
        return addPos == removePos;
    }

    /**
     * @throws NoSuchElementException if isEmpty() is true
     **/
    public synchronized Object remove() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        Object result = buffer[removePos];
        buffer[removePos] = null;
        removePos = (removePos + 1) % buffer.length;
        if (overflow != null) {
            buffer[addPos] = overflow.remove();
            addPos = (addPos + 1) % buffer.length;
            if (overflow.isEmpty()) {
                overflow = null;
            }
        }
        return result;
    }
    
}
