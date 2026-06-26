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
//import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/*
 * BD-ROM Part 3-1 5.2.2 AppInfoBDMV
 */
public class AppInfoBDMV {
    
    byte[] contentProviderData = new byte[32];
    InitialOutputModePreferenceType initialOutputModePreference
        = InitialOutputModePreferenceType.Mode2D;
    boolean ssContentExistFlag = false;
    VideoFormat videoFormat = VideoFormat.IGNORED;
    FrameRate frameRate = FrameRate.IGNORED;
  
    public enum InitialOutputModePreferenceType {
        Mode2D,         // 0
        Mode3D;         // 1
    }

    public enum VideoFormat {
                // This is almost the same as the video format defined in
                // 5.4.4.3.2, but 5.2.2.3 specifies that a value of 0 is
                // ignored by the player in this context.
        IGNORED,
        VIDEO_480i,
        VIDEO_576i,
        VIDEO_480p,
        VIDEO_1080i,
        VIDEO_720p,
        VIDEO_1080p,
        VIDEO_576p;

        public int getEncoding() {
            return (byte) ordinal();
        }

        public static VideoFormat getFromEncoding(int encoding) {
            VideoFormat[] values = VideoFormat.values();
            for (int i = 0; i < values.length; i++) {
                if (values[i].ordinal() == encoding) {
                    return values[i];
                }
            }
            assert false;
            return IGNORED;
        }
    }     
   

    public enum FrameRate {
                // This is almost the same as the frame rate defined in
                // 5.4.4.3.2, but 5.2.2.3 specifies that a value of 0 is
                // ignored by the player in this context.
        IGNORED,
        Hz_24000_1001,
        Hz_24,
        Hz_25,
        Hz_30000_1001,
        RESERVED_5,
        Hz_50,
        Hz_60000_1001;
        
        public int getEncoding() {
            return ordinal();
        }        

        public static FrameRate getFromEncoding(int encoding) {
            FrameRate[] frameRates = FrameRate.values();
            for (int i = 0; i < frameRates.length; i++) {
                if (frameRates[i].ordinal() == encoding) {
                    return frameRates[i];
                }
            }
            assert false;
            return IGNORED;
        }
    }

    public InitialOutputModePreferenceType getInitialOutputModePreference() {
        return initialOutputModePreference;
    }

    public void setInitialOutputModePreference(
                        InitialOutputModePreferenceType v) 
    {
        initialOutputModePreference = v;
    }


    public boolean getSSContentExistFlag() {
        return ssContentExistFlag;
    }

    public void setSSContentExistFlag(boolean v) {
        this.ssContentExistFlag = v;
    }

    public VideoFormat getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(VideoFormat v) {
        videoFormat = v;
    }

    public FrameRate getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(FrameRate v) {
        frameRate = v;
    }

     // Note: Commenting out to stop data represenatation to be in the xml file.
     // According to the spec, this field is for the Content Provider's use and
     // the field has no effect on BD-ROM player's behavior.
     // When we encounter a use case for the field, it can be added back.
     // 
    // public void setContentProviderData(byte[] data) {
    //     if (data.length != 32) {
    //         throw new RuntimeException("AppInfo data is not 32 bytes " + data.length);
    //     }
    //     this.contentProviderData = data;
    // }

    // @XmlJavaTypeAdapter(HexStringBinaryAdapter.class)       
    // public byte[] getContentProviderData() {
    //     return contentProviderData;
    // }
    
    public void readObject(DataInputStream din) throws IOException {
       
        // See 3-1 section 5.2.2.2
        // 32 bit length (should be constant, 34)
        // 1 bit reserved
        // 1 bit initial_output_mode_preference
        // 1 bit SS content exist flag
        // 5 bits reserved
        // 4 bits video_format
        // 4 bits frame_rate
        // 16 bit reserved
        // 8*32 user data
        
        din.skipBytes(4); // reserved
        byte b = din.readByte();
        if ((b & 0x40) != 0) {
            initialOutputModePreference =
                InitialOutputModePreferenceType.Mode3D;
        } else {
            initialOutputModePreference =
                InitialOutputModePreferenceType.Mode2D;
        }
        ssContentExistFlag = (b & 0x20) != 0;
        b = din.readByte();
        videoFormat = VideoFormat.getFromEncoding(b >> 4);
        frameRate = FrameRate.getFromEncoding(b & 0xf);
        din.read(contentProviderData);
    }
    
    public void writeObject(DataOutputStream dout) throws IOException {
        dout.writeInt(34);
        int i = 0;
        if (initialOutputModePreference == InitialOutputModePreferenceType.Mode3D) {
            i |= 0x40;
        }
        if (ssContentExistFlag) {
            i |= 0x20;
        }
        dout.writeByte(i);
        i = videoFormat.getEncoding() << 4;
        i |= frameRate.getEncoding();
        dout.writeByte(i);
        dout.write(contentProviderData);
    }

}
