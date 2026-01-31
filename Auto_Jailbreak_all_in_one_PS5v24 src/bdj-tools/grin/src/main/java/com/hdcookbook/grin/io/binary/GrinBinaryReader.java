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

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.awt.Rectangle;
import java.util.Hashtable;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.GrinXHelper;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.commands.ActivatePartCommand;
import com.hdcookbook.grin.commands.ActivateSegmentCommand;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.commands.ResetFeatureCommand;
import com.hdcookbook.grin.commands.SetVisualRCStateCommand;
import com.hdcookbook.grin.GrinXHelper;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.Box;
import com.hdcookbook.grin.features.Clipped;
import com.hdcookbook.grin.features.Fade;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.GuaranteeFill;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.features.SetTarget;
import com.hdcookbook.grin.features.SrcOver;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.input.CommandRCHandler;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.VisualRCHandler;
import com.hdcookbook.grin.util.Debug;

/**
 * The main class to read in a Show object from a binary file format.
 **/

/*
 * This class works with a binary file writer, GrinBinaryWriter, which contains
 * write(...) version of the methods defined in this class.
 * If you change one file, make sure to update the other file as well.
 *
 * The syntax of a Show's binary file is as follows:
 * 
 *  --------------------------------
 *  xxxxx.grin {
 *      script_identifier                  integer
 *      version_number                     integer
 *      StringArray_info()
 *      IntArrays_info()
 *      Rectangles_info()
 *      Rectangle_Arrays_info()
 *      ExtensionClasses_info()
 *      Show_Setup_info()
 *      Nodes_declarations()
 *      Nodes_contents()
 *  }
 *
 *  StringArray_info {  // Saves all String values needed in this binary file.
 *                      // The array index integer is used in the file to
 *                       // refer to String values.
 *      array_length        integer
 *      for (i = 0; i < array_length; i++) {
 *          value                           String
 *      }
 *  }
 * 
 *  IntArrays_info {  // Saves all immutable int[] instances needed in this binary file.
 *                    // The array index integer is used in the file to
 *                    // refer to 2 dimensional int arrays that can be shared.
 *      array_length        integer
 *      for (i = 0; i < array_length; i++) {
 *          value                           int[]
 *      }
 *  }
 *  
 *  Show_setup_info() {
 *      show_segment_stack_depth           integer
 *      draw_targets                       String[]
 *      isdebuggable                       boolean
 *      ShowCommand_classname              String
 *  }
 *  
 *  Nodes_declaration() {
 *      foreach (Feature_array, RCHandler_array, Segment_array) 
 *         array_length                    integer
 *         for (i = 0; i < array_length; i++) {
 *             class_indicator     byte                 
 *         }
 *      }
 *  }
 *  
 *  Nodes_contents() {
 *      foreach (Feature_array, RCHandler_array, Segment_array) 
 *         array_length                    integer
 *         for (i = 0; i < array_length; i++) {
 *             node specific info              
 *         }
 *      }
 *  }
 *
 *  For the types that aren't defined here, please refer to the code :-)
 *
 *  --------------------------------      
 */

public class GrinBinaryReader {

    private Show show;
    private Object[] showInArray;
    
    private Feature[] featureList;
    private RCHandler[] rcHandlerList;
    private Segment[] segmentList;
    private Command[] commandList;
    Hashtable publicSegments = new Hashtable();
    Hashtable publicFeatures = new Hashtable();
    Hashtable publicRCHandlers = new Hashtable();
    
    private InputStream stream;
    private Class showCommandsClass = null;
    private String[] stringConstants = null;
    private int[][]  intArrayConstants = null;
    private Rectangle[] rectangleConstants;
    private Rectangle[][] rectangleArrayConstants;
    private Command[][] commandArrayConstants;

    private GrinXHelper showCommands = null;

    private int extensionStartIndex;    // -1 if no extensions recorded
    private Constructor[] extensionConstructors = null;
    
    /*
     * If true, the binary file contains some debugging information.
     */
    boolean debuggable = false;

    /*
     * If non-null, then use this classloader to find and instantiate
     * the extension classes.
     */
    private ClassLoader classLoader = null;
    
    /**
     * Constructs a GrinBinaryReader instance.
     *
     * @param stream    An InputStream to the grin binary format data.  It is recommended to be
     *                  an instance of BufferedInputStream for a performance improvement.
     */
    public GrinBinaryReader(InputStream stream) {
       
       if (Debug.ASSERT) {
           this.stream = new DebugInputStream(stream);
       } else {
          this.stream = stream;
       }
       
    }

    /**
     * Constructs a GrinBinaryReader instance.
     *
     * @param stream    An InputStream to the grin binary format data.  It is recommended to be
     *                  an instance of BufferedInputStream for a performance improvement.
     * @param loader    An ClassLoader to use for finding needed classes to construct
     *                  the Show object.  Extensions and Command subclasses will be searched
     *                  using this classloader, if given.
     */
    public GrinBinaryReader(InputStream stream, ClassLoader loader) {
       this(stream);
       this.classLoader = loader;
    }
    /**
     * Returns an instace of feature that corresponds to the index number
     * that this GrinBinaryReader keeps track of.
     * This method is expected to be used by the user defined ExtensionsReader class.
     * 
     * @param index     The index number for the feature.
     * @return          The feature corresponding to the index number.
     * 
     * @see GrinBinaryWriter#getFeatureIndex(Feature)
     */
     Feature getFeatureFromIndex(int index) throws IOException {
        if (index < 0 || index >= featureList.length) {
            throw new IOException("non-existing feature reference");
        }  else {
            return featureList[index];
        }
    }
 
    /**
     * Returns an instace of a segment that corresponds to the index number
     * that this GrinBinaryReader keeps track of.
     * This method is expected to be used by the user defined ExtensionsReader class.
     * 
     * @param index     The index number for the feature.
     * @return          The segment corresponding to the index number
     * 
     * @see GrinBinaryWriter#getSegmentIndex(Segment)
     */
     Segment getSegmentFromIndex(int index) throws IOException {
        if (index < 0 || index > segmentList.length) {
            throw new IOException("non-existing segment reference");
        }  else {
            return segmentList[index];
        } 
    }
    
    /**
     * Returns an instace of a RCHandler that corresponds to the index number
     * that this GrinBinaryReader keeps track of.
     * This method is expected to be used by the user defined ExtensionsReader class.
     * 
     * @param index     The index number for the feature.
     * @return          The RCHandler corresponding to the index number
     * 
     * @see GrinBinaryWriter#getRCHandlerIndex(RCHandler)
     */
    RCHandler getRCHandlerFromIndex(int index) throws IOException {
        if (index < 0 || index > rcHandlerList.length) {
            throw new IOException("non-existing rchandler reference");
        }  else {
            return rcHandlerList[index];
        }
        
    }
    
    /**
     * Returns an instace of a Command that corresponds to the index number
     * that this GrinBinaryReader keeps track of.
     * This method is expected to be used by the user defined ExtensionsReader class.
     * 
     * @param index     The index number for the command.
     * @return          The Command corresponding to the index number
     * 
     * @see GrinBinaryWriter#getCommandIndex(Command)
     */
    Command getCommandFromIndex(int index) throws IOException {
        if (index < 0 || index > commandList.length) {
            throw new IOException("non-existing command reference " + index);
        }  else {
            return commandList[index];
        }
        
    }

    int[] getIntArrayFromReference(int index) throws IOException {
        if (index < 0 || index > intArrayConstants.length) {
            throw new IOException("non-existing int array reference");
        }  else {
            return intArrayConstants[index];
        }
    }

    String getStringFromReference(int index) throws IOException {
        if (index < 0 || index > stringConstants.length) {
            throw new IOException("wrong string reference ");
        }  else {
            return stringConstants[index];
        }
    }

    Rectangle getRectangleFromReference(int index) throws IOException {
        if (index < 0 || index > rectangleConstants.length) {
            throw new IOException("bad rectangle reference");
        } else {
            return rectangleConstants[index];
        }
    }
    
    Rectangle[] getRectangleArrayFromReference(int index) throws IOException {
        if (index < 0 || index > rectangleArrayConstants.length) {
            throw new IOException("bad rectangle array reference");
        } else {
            return rectangleArrayConstants[index];
        }
    }

    Command[] getCommandArrayFromReference(int index) throws IOException {
        if (index < 0 || index > commandArrayConstants.length) {
            throw new IOException("bad command array reference");
        } else {
            return commandArrayConstants[index];
        }
    }
    
    private void checkValue(int x, int y, String message) throws IOException {
        if (x != y) {
            throw new IOException("Mismatch: " + message);
        }
    }
    
    private void checkScriptHeader(DataInputStream in) throws IOException {
       checkValue(in.readInt(), Constants.GRINSCRIPT_IDENTIFIER, "Script identifier");
       int version = in.readInt();
       checkValue(version, Constants.GRINSCRIPT_VERSION, 
           "Script version mismatch, expects " + Constants.GRINSCRIPT_VERSION 
           + ", found " + version);       
    }
    
    /**
     * Reconstructs the Show object passed in as argument.
     *
     * @param show      An empty Show object to reconstruct.
     * @throws IOException if binary data parsing fails.
     */
    
    public void readShow(Show show) throws IOException {

        this.show = show;
        this.showInArray = new Show[] { show } ;
        
        GrinDataInputStream in = new GrinDataInputStream(stream, this);       
        checkScriptHeader(in);

        stringConstants = readStringConstants(in);
        intArrayConstants = readIntArrayConstants(in);
        rectangleConstants = readRectangleConstants(in);
        rectangleArrayConstants = readRectangleArrayConstants(in);
        extensionConstructors = readExtensionConstructors(in);

        readShowCommandsClass(in);
        showCommands = instantiateShowCommandsCmd();
        
        commandList = new Command[in.readInt()];
        readDeclarations(in, commandList);
        commandArrayConstants = readCommandArrayConstants(in);
       
        int showSegmentStackDepth = in.readInt();
        show.setSegmentStackDepth(showSegmentStackDepth);
        String[] showDrawTargets = in.readStringArray();
        show.setDrawTargets(showDrawTargets);
        String[] stickyImages = in.readStringArray();
        Hashtable publicNamedCommands = readPublicNamedCommands(in);
        debuggable = in.readBoolean();
            
        // Read in the show file
        featureList = new Feature[in.readInt()];
        readDeclarations(in, featureList);
        rcHandlerList = new RCHandler[in.readInt()];
        readDeclarations(in, rcHandlerList);
        segmentList = new Segment[in.readInt()];
        readDeclarations(in, segmentList);  
        readContents(in, featureList);
        readContents(in, rcHandlerList);
        readContents(in, segmentList);
        readContents(in, commandList);

        Segment showTop = (Segment) in.readSegmentReference();
        Group showTopGroup     = (Group) in.readFeatureReference();
        
        String[] fontName = in.readStringArray();
        int[] fontStyleSize = in.readSharedIntArray();
        
        int xScale = in.readInt();
        int yScale = in.readInt();
        int xOffset = in.readInt();
        int yOffset = in.readInt();
        show.setScale(xScale, yScale, xOffset, yOffset);
        
        show.buildShow(segmentList, featureList, rcHandlerList, stickyImages,
                       showTop, showTopGroup,
                       publicSegments, publicFeatures, publicRCHandlers,
                       publicNamedCommands, fontName, fontStyleSize);
    }

    private Hashtable readPublicNamedCommands(GrinDataInputStream in) 
                throws IOException 
    {
        int num = in.readInt();
        Hashtable result = new Hashtable(num);
        for (int i = 0; i < num; i++) {
            String key = in.readString();
            Command value = getCommandFromIndex(in.readInt());
            result.put(key, value);
        }
        return result;
    }

    private void readDeclarations(GrinDataInputStream in, Object[] list)
            throws IOException 
    {
        Node node;
        for (int i = 0; i < list.length; i++) {
            int identifier = in.readInt();
            switch (identifier) {
                case Constants.ASSEMBLY_IDENTIFIER:
                    node = new Assembly(show);
                    break;
                case Constants.BOX_IDENTIFIER:
                    node = new Box(show);
                    break;
                case Constants.CLIPPED_IDENTIFIER:
                    node = new Clipped(show);
                    break;
                case Constants.FADE_IDENTIFIER:
                    node = new Fade(show);
                    break;
                case Constants.FIXEDIMAGE_IDENTIFIER:
                    node = new FixedImage(show);
                    break;
                case Constants.GROUP_IDENTIFIER:
                    node = new Group(show);
                    break;
                case Constants.IMAGESEQUENCE_IDENTIFIER:
                    node = new ImageSequence(show);
                    break;
                case Constants.TEXT_IDENTIFIER:
                    node = new Text(show);
                    break;
                case Constants.INTERPOLATED_MODEL_IDENTIFIER:
                    node = new InterpolatedModel(show);
                    break;
                case Constants.TRANSLATOR_IDENTIFIER:
                    node = new Translator(show);
                    break;
                case Constants.SRCOVER_IDENTIFIER:
                    node = new SrcOver(show);
                    break;
                case Constants.GUARANTEE_FILL_IDENTIFIER:
                    node = new GuaranteeFill(show);
                    break;
                case Constants.SET_TARGET_IDENTIFIER:
                    node = new SetTarget(show);
                    break;
                case Constants.COMMAND_RCHANDLER_IDENTIFIER:
                    node = new CommandRCHandler();
                    break;
                case Constants.VISUAL_RCHANDLER_IDENTIFIER:
                    node = new VisualRCHandler();
                    break;
                case Constants.SEGMENT_IDENTIFIER:
                    node = new Segment();
                    break;
                case Constants.ACTIVATEPART_CMD_IDENTIFIER:
                    node = new ActivatePartCommand(show);
                    break;
                case Constants.ACTIVATESEGMENT_CMD_IDENTIFIER:
                    node = new ActivateSegmentCommand(show);
                    break;
                case Constants.RESETFEATURE_CMD_IDENTIFIER:
                    node = new ResetFeatureCommand(show);
                    break;
                case Constants.GRINXHELPER_CMD_IDENTIFIER:
                    node = new GrinXHelper(show);
                    break;
                case Constants.SETVISUALRCSTATE_CMD_IDENTIFIER:
                    node = new SetVisualRCStateCommand(show);
                    break;
                case Constants.NULL: // happens for commands
                    node = null;
                default:  // extensions  
                    node = instantiateExtension(identifier);
                    break;
                }

            list[i] = node;
        }

    }

    private Node instantiateExtension(int typeIdentifier) throws IOException {
        if (extensionConstructors == null) {
            if (showCommands == null) {
                throw new IOException("Missing GrinXHelper subclass for "
                                      + "instantiating extensions");
            }
            return showCommands.getInstanceOf(show, typeIdentifier);
        } else {
            int i = typeIdentifier - extensionStartIndex;
            try {
                return (Node) extensionConstructors[i].newInstance(showInArray);
            } catch (Exception ex) {
                throw new IOException("Error instantiating extension:  " + ex);
            }
        }
    }
    
    private int[][] readIntArrayConstants(GrinDataInputStream in) 
            throws IOException 
    {
        checkValue(in.readByte(),
                Constants.INT_ARRAY_CONSTANTS_IDENTIFIER,
                "Integer array constants identifier");        
        int length = in.readInt();
        int[][] array = new int[length][];
        array[0] = null;
        for (int i = 1; i < length; i++) {
            array[i] = new int[in.readInt()];
            for (int j = 0; j < array[i].length; j++) {
                array[i][j] = in.readInt();
            }
        }
        return array;
    }

    private Rectangle[] readRectangleConstants(GrinDataInputStream in) 
            throws IOException 
    {
        checkValue(in.readByte(),
                Constants.RECTANGLE_CONSTANTS_IDENTIFIER,
                "Rectangle constants identifier");        
        int length = in.readInt();
        Rectangle[] array = new Rectangle[length];
        array[0] = null;
        for (int i = 1; i < length; i++) {
            Rectangle r = new Rectangle();
            r.x = in.readInt();
            r.y = in.readInt();
            r.width = in.readInt();
            r.height = in.readInt();
            array[i] = r;
        }
        return array;
    }

    private Rectangle[][] readRectangleArrayConstants(GrinDataInputStream in) 
            throws IOException 
    {
        checkValue(in.readByte(),
                Constants.RECTANGLE_ARRAY_CONSTANTS_IDENTIFIER,
                "Rectangle array constants identifier");        
        int length = in.readInt();
        Rectangle[][] array = new Rectangle[length][];
        array[0] = null;
        for (int i = 1; i < length; i++) {
            Rectangle[] row = new Rectangle[in.readInt()];
            array[i] = row;
            for (int j = 0; j < row.length; j++) {
                row[j] = getRectangleFromReference(in.readInt());
            }
        }
        return array;
    }

    private Command[][] readCommandArrayConstants(GrinDataInputStream in) 
            throws IOException 
    {
        checkValue(in.readByte(),
                Constants.COMMAND_ARRAY_CONSTANTS_IDENTIFIER,
                "Command array constants identifier");        
        int length = in.readInt();
        Command[][] array = new Command[length][];
        array[0] = null;
        for (int i = 1; i < length; i++) {
            Command[] row = new Command[in.readInt()];
            array[i] = row;
            for (int j = 0; j < row.length; j++) {
                row[j] = getCommandFromIndex(in.readInt());
            }
        }
        return array;
    }

    private Constructor[] readExtensionConstructors(GrinDataInputStream in) 
                throws IOException 
    {
        checkValue(in.readByte(),
                Constants.EXTENSION_CLASSES_IDENTIFIER,
                "Extension classes identifier");        
        extensionStartIndex = in.readInt();
        if (extensionStartIndex == -1) {
            return null;
        }
        int length = in.readInt();
        Constructor[] result = new Constructor[length];
        Class[] paramTypes = { Show.class };
        for (int i = 0; i <length; i++) {
            String name = in.readUTF();
            try {
            Class cl = null;
            if (classLoader == null) {
                        cl = Class.forName(name);
            } else {
                cl = Class.forName(name, true, classLoader);
            }
                    result[i] = cl.getDeclaredConstructor(paramTypes);
            } catch (ClassNotFoundException ex) {
                throw new IOException("Extension class " + name 
                                       + " is missing:  " + ex);
            } catch (NoSuchMethodException ex) {
                throw new IOException("Extension class " + name 
                                       + " missing constructor:  " + ex);
            }
        }
        return result;
    }

    
    private void readShowCommandsClass(GrinDataInputStream in)
           throws IOException 
    {
        String className = in.readString();
        if (className == null) {
            return;
        }
        try {
        if (classLoader == null) {
            showCommandsClass = Class.forName(className);
        } else {
            showCommandsClass = Class.forName(className, true, classLoader);
        }
        } catch (Exception ex) {
            throw new IOException(ex.toString());
        }
    }

    private void readContents(GrinDataInputStream in, Object[] list) 
       throws IOException 
    {
        for (int i = 0; i < list.length; i++) {  
            Node node = (Node) list[i];
            if (node != null) {
                int length = in.readInt();
                if (Debug.ASSERT) {
                    ((DebugInputStream) stream).pushExpectedLength(length);
                }
                node.readInstanceData(in, length);
                if (Debug.ASSERT) {
                    ((DebugInputStream) stream).popExpectedLength();
                }
            }
        }
    }

    private String[] readStringConstants(GrinDataInputStream in) 
        throws IOException {
        checkValue(in.readByte(), 
                Constants.STRING_CONSTANTS_IDENTIFIER,
                "String array identifier");
        
        String[] strings = new String[in.readInt()];
        // strings[0] is null
        for (int i = 1; i < strings.length; i++) {
            strings[i] = in.readUTF();
        }
        return strings;
    }

    Command[] getCommandArrayFromIndex(int index) throws IOException  {
        if (index < 0 || index > commandArrayConstants.length) {
            throw new IOException("non-existing command array reference");
        }  else {
            return commandArrayConstants[index];
        }
    }

    private GrinXHelper instantiateShowCommandsCmd() 
        throws IOException {    
        if (showCommandsClass == null) {
            return null;
        }
        GrinXHelper result;
        Class[] paramType = { Show.class };
        Object[] param = { show };
        try {
            result = (GrinXHelper) 
                showCommandsClass.getConstructor(paramType).newInstance(param);
        } catch (Throwable ex) {
            throw new IOException(ex.toString());
        }
        return result;
    }  

}
