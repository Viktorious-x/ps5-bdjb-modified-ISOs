/*  
 * Copyright (c) 2008, Sun Microsystems, Inc.
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

package net.java.bd.tools.index;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * BD-ROM Part 3-1 5.2.3 Indexes
 */
public class Title {
    
    IndexObject titleObject;
    TitleAccessType  type;
    
    public void setIndexObject(IndexObject object) {
        this.titleObject = object;
    }
    
    public IndexObject getIndexObject() {
        return this.titleObject;
    }
    
    public void setTitleAccessType(TitleAccessType type) {
        this.type = type;
    }
    
    public TitleAccessType getTitleAccessType() {
        return type;
    }
    
    public enum TitleAccessType {
        
        V_00,
        V_01,
        V_10,
        V_11;

        public byte getEncoding() {
            return (byte) ordinal();
        }      
    }    
    
    public void readObject(DataInputStream din) throws IOException {
        
        // 2 bit title-object-type
        // 2 bit title-access-type
        // 28 bit reserve
        // HDMV or BDJtitleObject
        
        byte b = din.readByte(); 
        din.skipBytes(3);
        
        int appType = ((b & 0x0C0) >> 6);        
        if (appType == 1) {
            titleObject = new HDMVIndexObject();           
        } else {
            titleObject = new BDJIndexObject();
        }
        
        int titleAccessType = ((b & 0x030) >> 4);
        Enum[] titleAccessTypes = TitleAccessType.values();
        for (int i = 0; i < titleAccessTypes.length; i++) {
            if (titleAccessTypes[i].ordinal() == titleAccessType) {
                setTitleAccessType((TitleAccessType) titleAccessTypes[i]);
                break;
            }
        }
        
        titleObject.readObject(din);        
    }
    
    public void writeObject(DataOutputStream dout) throws IOException {
        int b = (getTitleAccessType().ordinal() << 4) 
                | (titleObject.getObjectType().ordinal() << 6);      
        byte[] reserved = new byte[3];
         
        dout.writeByte(b);
        dout.write(reserved); 
        titleObject.writeObject(dout);
    }
}
