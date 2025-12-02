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
import javax.xml.bind.annotation.XmlType;

/**
 * BD-ROM Part 3-1 5.2.3 Indexes
 */
@XmlType ( name="BDJIndexObject" )
public class BDJIndexObject extends IndexObject {
    
    BDJPlaybackType type;
    String bdjoName;
    IndexObjectType objectType = IndexObjectType.V_10;
    
    public void setBDJOName(String name) {
        this.bdjoName = name;
    }

    public String getBDJOName() {
        return bdjoName;
    }
    
    public void setPlaybackType(BDJPlaybackType type) {
        this.type = type;
    }
    
    public BDJPlaybackType getPlaybackType() {
        return type;
    }
    
    public enum BDJPlaybackType {
        
        BDJPlayback_RESERVED,
        BDJPlayback_RESERVED2,
        BDJPlayback_MOVIE,
        BDJPlayback_INTERACTIVE;

        public byte getEncoding() {
            return (byte) ordinal();
        }      
    }
    
    @Override
    public void readObject(DataInputStream din) throws IOException {

        // 2 bits playback type
        // 14 bits alignment
        // 8*5 bdjo_file_name
        // 8 bit word align
        
        byte b;
        String name;
        
        b = din.readByte();  // first 2 bits here for playback type
        din.skipBytes(1);    // word_align
        name = StringIOHelper.readISO646String(din, 5);      // 5 bytes for name
        din.skipBytes(1);    // word_align
        
        int playback = ((b & 0x0C0) >> 6);
        Enum[] playbackType = BDJPlaybackType.values();
        for (int i = 0; i < playbackType.length; i++) {
            if (playbackType[i].ordinal() == playback) {
                setPlaybackType((BDJPlaybackType) playbackType[i]);
                break;
            }
        }
        setBDJOName(name);
    }
    
    @Override  
    public void writeObject(DataOutputStream dout) throws IOException {
         int b = (getPlaybackType().ordinal() << 6);
         byte[] reserved = new byte[1];
         
         dout.writeByte(b);
         dout.write(reserved); 
         dout.write(StringIOHelper.getISO646Bytes(getBDJOName()));
         dout.write(reserved);
    }

    @Override
    public IndexObjectType getObjectType() {
        return objectType;
    }     
}
