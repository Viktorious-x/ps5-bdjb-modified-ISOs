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

package com.hdcookbook.grin.io.binary;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.util.AssetFinder;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * GrinDataInputStream is a convenience DataInputStream subclass 
 * that can handle certain Objects and Object arrays, including null.
 * This class is used by the GrinBinaryReader to read in information about the
 * show nodes.
 * <p>
 * See also GrinDataOutputStream in the JavaSE tools.
 * 
 * @see GrinBinaryReader
 */
public class GrinDataInputStream extends DataInputStream {


   /**
    * An instance of the GrinBinaryReader that this input stream
    * is working with.
    */
   private GrinBinaryReader binaryReader; 
   
   GrinDataInputStream(InputStream in, GrinBinaryReader reader) {
       super(in);
       this.binaryReader = reader;
   }
   
   /**
    * Reads in and constructs a Color instance.
    * @return A Color instance reconstructed from the input stream.
    * @throws java.io.IOException if IO error occurs.
    */
   public Color readColor() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
      
       int rgba = readInt();
       return new Color(rgba, true);
   }
   
   /**
    * Reads in and constructs a Rectangle instance.
    * @return A Rectangle instance reconstructed from the input stream.
    * @throws java.io.IOException if IO error occurs.
    */
   public Rectangle readRectangle() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       double x = readDouble();
       double y = readDouble();
       double w = readDouble();
       double h = readDouble();
       return new Rectangle((int)x,(int)y,(int)w,(int)h);
   }
   
   /**
    * Reads in and constructs an array of Rectangle.
    * @return An array of Rectangles reconstructed from the input stream.
    * @throws java.io.IOException if IO error occurs.
    */
   public Rectangle[] readRectangleArray() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       
       Rectangle[] array = new Rectangle[readInt()];
       for (int i = 0; i < array.length; i++) {
           array[i] = readRectangle();
       }
       return array;       
   }
   
   
   /**
    * Reads in and constructs an array of integer.
    * @return An array of integers reconstructed from the input stream.
    * @throws java.io.IOException if IO error occurs.
    */
   public int[] readIntArray() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       
       int[] array = new int[readInt()];
       for (int i = 0; i < array.length; i++) {
           array[i] = readInt();
       }
       return array;
   }
   
   /**
    * Reads in an reference to an integer array.
    * 
    * @return An array of integers reconstructed from the input stream.
    * @throws java.io.IOException if IO error occurs.
    */
    public int[] readSharedIntArray() throws IOException {
        int index = readInt();
        return binaryReader.getIntArrayFromReference(index);
    }     

    public Rectangle[] readSharedRectangleArray() throws IOException {
        int index = readInt();
        return binaryReader.getRectangleArrayFromReference(index);
    }

    public Rectangle readSharedRectangle() throws IOException {
        int index = readInt();
        return binaryReader.getRectangleFromReference(index);
    }

   /**
    * Reads in and constructs a String instance.
    * @return A String instance reconstructed from the input stream.
    * @throws java.io.IOException if IO error occurs.
    */
   public String readString() throws IOException {
       int index = readInt();           // index 0 is null
       return binaryReader.getStringFromReference(index);
   }

   /**
    * Reads in and constructs an array of Strings.
    * @return An array of Strings reconstructed from the input stream.
    * @throws java.io.IOException if IO error occurs.
    */
   public String[] readStringArray() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       
       String[] array = new String[readInt()];
       for (int i = 0; i < array.length; i++) {
           array[i] = readString();
       }
       return array;
   }
   
   /**
    * Reads in a reference of a feature and returns an instance of the 
    * feature.
    * 
    * @return   A feature that is referenced from, or null if no such
    *           feature exists in the GrinBinaryReader that this input stream
    *           is working with.
    */
   public Feature readFeatureReference() throws IOException {
       if (isNull()) {
           return null;
       }
       
       int index = readInt();      
       return binaryReader.getFeatureFromIndex(index);
   }
   
   /**
    * Reads in a reference of a segment and returns an instance of the 
    * segment.
    * 
    * @return   a Segument that is referenced from, or null if no such
    *           segment exists in the GrinBinaryReader that this input stream
    *           is working with.
    */
   public Segment readSegmentReference() throws IOException {
       if (isNull()) {
           return null;
       }
       
       int index = readInt(); 
       return binaryReader.getSegmentFromIndex(index);
   } 
   
   /**
    * Reads in a reference of an RCHandler and returns an instance of the 
    * RCHandler.
    * 
    * @return   a RCHandler that is referenced from, or null if no such
    *           RCHandler exists in the GrinBinaryReader that this input stream
    *           is working with.
    */
   public RCHandler readRCHandlerReference() throws IOException {
       if (isNull()) {
           return null;
       }
       
       int index = readInt(); 
       return binaryReader.getRCHandlerFromIndex(index);
   }   
   
    /**
    * Reads in refereces of Features and returns an array of  
    * Features corresponding to the references.
    * 
    * @return   an array of Features that is referenced from.
    */
   public Feature[] readFeaturesArrayReference() 
            throws IOException {
        
        if (readByte() == Constants.NULL) {
            return null;
        }    
        
        Feature[] f = new Feature[readInt()];       
        for (int i = 0; i < f.length; i++) {
            f[i] = readFeatureReference();
        }   
        
        return f;
    }   
   
   /**
    * Reads in refereces of RCHandler and returns an array of the 
    * RCHandler.
    * 
    * @return   an array of RCHandler that is referenced from.
    */
   public RCHandler[] readRCHandlersArrayReference() 
           throws IOException {
       
       int length = readInt();
       RCHandler[] handlers = new RCHandler[length];
       for (int i = 0; i < handlers.length; i++) {
           handlers[i] = readRCHandlerReference();
       }      
        
       return handlers;
   }   
   
   /**
    * Reads in information about Commands and reconstructs
    * an array of Commands from it.
    * 
    * @return an array of Commands reconstructed from this stream.
    * @throws java.io.IOException
    */
    public Command[] readCommands() throws IOException {
        int index = readInt();
        return binaryReader.getCommandArrayFromReference(index);
    }
   
   /**
    * Checks whether the Object reading is is null or not.
    * 
    * When writing custom object, one can do:
    * <pre>
    * public void readInstanceData(GrinDataInputStream in) {
    *    .....
    *    MyObject myObject = null;
    *    if (!in.isNull()) {
    *       // proceed with populating data in "myObject"
    *    }
    *    ....
    * }
    * </pre>
    * <p>
    * See also GrinDataOutputStream's writeNull and writeNonNull methods
    * in the JavaSE tools.
    * @return boolean whether the object is null.
    * @throws java.io.IOException
    */
   public boolean isNull() throws IOException {
       return (readByte() == Constants.NULL);
   }
   
   /**
    * Reads in information common to all Feature types.  This method
    * reads in following information.
    * <ul>
    *     <li>Whether the node is public or private
    *     <li>The name of a Feature
    *     <li>The sub-feature "part" of a Modifier if this Feature is a Modifier
    * </ul> 
    * 
    * <p>
    * See also GrinDataOutputStream's writeSuperClassData(Feature) method
    * in the JavaSE tools.
    * 
    * @param feature the feature type of populate base data with.
    * @throws java.io.IOException
    * 
    */
    public void readSuperClassData(Feature feature) 
            throws IOException 
    {
        boolean isPublic = readBoolean();
        if (isPublic || binaryReader.debuggable) {
            feature.setName(readString());
        }      
        if (feature instanceof Modifier) {
            ((Modifier)feature).setup(readFeatureReference());
        }
        
        if (isPublic) {
            binaryReader.publicFeatures.put(feature.getName(), feature);
        }
    }
    
   /**
    * Reads in information common to all RChandler types.  This method
    * reads in following information.
    * <ul>
    *     <li>Whether the node is public or private
    *     <li>The name of a RCHandler
    * </ul> 
    * 
    * <p>
    * See also GrinDataOutputStream's writeSuperClassData(RCHandler)
    * method in the JavaSE tools.
    * 
    * @param  handler RCHandler instance to populate data.
    * @throws java.io.IOException
    * 
    */    
    public void readSuperClassData(RCHandler handler) 
            throws IOException {
        boolean isPublic = readBoolean();
        if (isPublic || binaryReader.debuggable) {
            handler.setName(readString());
        }
        
        if (isPublic) {
            binaryReader.publicRCHandlers.put(handler.getName(), handler);
        }        
    }   
    
   /**
    * Reads in information common to all Segment types.  This method
    * reads in following information.
    * <ul>
    *     <li>Whether the node is public or private
    *     <li>The name of a Segment
    * </ul> 
    * 
    * <p>
    * See also GrinDataOutputStream's writeSuperClassData(Segment) method
    * in the JavaSE tools.
    * 
    * @param  segment RCHandler instance to populate data.
    * @throws java.io.IOException
    */     
    public void readSuperClassData(Segment segment) 
            throws IOException {
        boolean isPublic = readBoolean();
        if (isPublic || binaryReader.debuggable) {
            segment.setName(readString());
        }
        
        if (isPublic) {
            binaryReader.publicSegments.put(segment.getName(), segment);
        }        
    }  
  
   /**
    * Reads in information common to all Command types.  
    * 
    * There is no shared data for Command class currently.
    * <p>
    * See also GrinDataOutputStream's writeSuperClassData(Command) method
    * in the JavaSE tools.
    * 
    * @param  command Command instance to populate data.
    * @throws java.io.IOException
    */ 
    public void readSuperClassData(Command command) {
        // nothing to do for the command.
    }    
}
