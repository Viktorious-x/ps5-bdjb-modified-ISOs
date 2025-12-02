
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

package net.java.bd.tools.bdjo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;

import com.hdcookbook.grin.util.BitStreamIO;
        
/**
 * A class to read BDJO object specified as JavaFX object literal
 * or as a XML document or as a binary BDJO file.
 *
 * @author A. Sundararajan
 */
public final class BDJOReader {
    private static boolean TRACE = false;
        // Set to true to debug binary reading by tracing all of the
        // reads from the .bdjo file
    // don't create me!
    private BDJOReader() {}
    
    private static volatile ScriptEngine javaFxEngine;
    private static void initJavaFxEngine() {
        if (javaFxEngine == null) {
            synchronized (BDJOReader.class) {
                ScriptEngineManager m = new ScriptEngineManager();
                javaFxEngine = m.getEngineByName("FX");
                if (javaFxEngine == null) {
                    throw new RuntimeException("cannot load JavaFX engine, check your CLASSPATH");
                }
            }
        }
    }
    
    public static synchronized BDJO readFX(Reader reader) 
                throws ScriptException {
        initJavaFxEngine();
        return (BDJO) javaFxEngine.eval(reader);
    }
    
    public static synchronized BDJO readFX(String code) 
                throws ScriptException {
        initJavaFxEngine();
        return (BDJO) javaFxEngine.eval(code);
    }
    
    public static BDJO readXML(Reader reader) throws JAXBException {
        String className = BDJO.class.getName();
        String pkgName = className.substring(0, className.lastIndexOf('.'));
        JAXBContext jc = JAXBContext.newInstance(pkgName);
        Unmarshaller u = jc.createUnmarshaller();
        u.setEventHandler(new DefaultValidationEventHandler());
        return (BDJO) u.unmarshal(reader);
    }
    
    public static BDJO readXML(String str) throws JAXBException {
        return readXML(new StringReader(str));
    }
    
    private static String iso646String(byte[] buf) {
        try {
            return new String(buf, "ISO646-US");
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }
    
    private static String readISO646String(String name, DataInputStream dis, 
                                           BitStreamIO bio, int len)
            throws IOException 
    {
        byte[] buf = new byte[len];
        bio.assertByteAligned(1);
        dis.read(buf);
        String result = iso646String(buf);
        if (TRACE) {
            System.out.println(name + ":  \"" + result + "\"");
        }
        return result;
    }
    
    private static String toUTF8String(byte[] buf) {
        try {
            return new String(buf, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }
    
    private static String readUTF8String(String name, DataInputStream dis, 
                                         BitStreamIO bio, int len)
            throws IOException 
    {
        byte[] buf = new byte[len];
        bio.assertByteAligned(1);
        dis.read(buf);
        String result = toUTF8String(buf);
        if (TRACE) {
            System.out.println(name + ":  \"" + result + "\"");
        }
        return result;
    }

    private static byte readByte(String name, DataInputStream dis, 
                                 BitStreamIO bio) 
            throws IOException 
    {
        bio.assertByteAligned(1);
        byte result = dis.readByte();
        if (TRACE) {
            System.out.println(name + ":  " + ((int) result) + " ");
        }
        return result;
    }

    private static int readUnsignedByte(String name, DataInputStream dis, 
                                        BitStreamIO bio) 
            throws IOException 
    {
        bio.assertByteAligned(1);
        int result = dis.readUnsignedByte();
        if (TRACE) {
            System.out.println(name + ":  " + result + " ");
        }
        return result;
    }

    private static short readShort(String name, DataInputStream dis, 
                                   BitStreamIO bio) 
            throws IOException 
    {
        bio.assertByteAligned(1);
        short  result = dis.readShort();
        if (TRACE) {
            System.out.println(name + ":  " + result + " ");
        }
        return result;
    }

    private static int readUnsignedShort(String name, DataInputStream dis, 
                                         BitStreamIO bio) 
            throws IOException 
    {
        bio.assertByteAligned(1);
        int result = dis.readUnsignedShort();
        if (TRACE) {
            System.out.println(name + ":  " + result + " ");
        }
        return result;
    }

    private static int readInt(String name, DataInputStream dis, 
                                 BitStreamIO bio) 
            throws IOException 
    {
        bio.assertByteAligned(1);
        int result = dis.readInt();
        if (TRACE) {
            System.out.println(name + ":  " + result + " ");
        }
        return result;
    }

    private static int readBits(String name, DataInputStream dis, 
                          BitStreamIO bio, int bits) 
            throws IOException 
    {
        int result = bio.readBits(dis, bits);
        if (TRACE) {
            System.out.println(name + ":  \"" + result + "\"");
        }
        return result;
    }

    private static long readBitsLong(String name, DataInputStream dis, 
                                     BitStreamIO bio, int bits) 
            throws IOException 
    {
        long result = bio.readBitsLong(dis, bits);
        if (TRACE) {
            System.out.println(name + ":  \"" + result + "\"");
        }
        return result;
    }

    // section 10.2.2.2 TerminalInfo - Syntax
    private static TerminalInfo readTerminalInfo(DataInputStream dis, 
                                                 BitStreamIO bio)
            throws IOException 
    {
        TerminalInfo ti = new TerminalInfo();
        if (TRACE) {
            System.out.println("Terminal info:");
        }
        // skip "length" field which is 4 bytes
        readInt("    length (ignored)", dis, bio);
        // followed by length, we have default_font_file_name
        ti.setDefaultFontFile(readISO646String("    default font", dis, bio,5));
        // read initial_HAVi_configuration_id, menu_call_mask and 
        // title_search_mask - which is 4 bits + 1 bit + 1 bit
        int id = readBits("    HAVi config", dis, bio, 4);
        Enum[] values = HaviDeviceConfig.values();
        for (int i = 0; i < values.length; i++) {
            HaviDeviceConfig hdc = (HaviDeviceConfig)values[i];
            if (hdc.getId() == id) {
                ti.setInitialHaviConfig(hdc);
                break;
            }
        }
        boolean b = readBits("    menu call mask", dis, bio, 1) != 0;
        ti.setMenuCallMask(b);
        b = readBits("    title search mask", dis, bio, 1) != 0;
        ti.setTitleSearchMask(b);
        b = readBits("    mouse supported", dis, bio, 1) != 0;
        ti.setMouseSupported(b);
        b = readBits("    mouse interest", dis, bio, 1) != 0;
        ti.setMouseInterest(b);
        ti.setInitialOutputMode(readBits("    initial output mode", dis,bio,2));
        ti.setInitialFrameRate(readBits("    initial frame rate", dis, bio, 4));
        readBitsLong("    padding", dis, bio, 26);
        
        return ti;
    }
    
    // section 10.2.3.2 AppCacheInfo - Syntax
    private static AppCacheInfo readAppCacheInfo(DataInputStream dis,
                                                 BitStreamIO bio)
            throws IOException 
    {
        if (TRACE) {
            System.out.println("App cache info:");
        }
        AppCacheInfo aci = new AppCacheInfo();
        // ignore "length" field which is 4 bytes
        readInt("    length (ignored)", dis, bio);
        // get "number_of_entries" field
        final int numEntries 
            = readUnsignedByte("    number of entries", dis, bio);
        // followed by that we have "reserved_for_word_align" field
        // which is one byte
        readByte("    padding", dis, bio);
        
        AppCacheEntry[] entries = new AppCacheEntry[numEntries];
        /* each app cache entry is of size is 12 bytes */
        int entrySize = 12;
        for (int e = 0; e < entries.length; e++) {
            AppCacheEntry ace = new AppCacheEntry();
            // entry_type field
            ace.setType(readByte("        entry_type", dis, bio));
            // ref_to_name field
            ace.setName(readISO646String("        ref_to_name", dis, bio, 5));
            // language_code field
            ace.setLanguage(
                readISO646String("        language_code", dis, bio, 3));
            entries[e] = ace;
            // skip "reserved_for_future_use" field
            readBits("        padding", dis, bio, 8*3);
        }
        aci.setEntries(entries);
        return aci;
    }
    
    // section 10.2.4.2 TableOfAccessiblePlayLists - Syntax
    private static TableOfAccessiblePlayLists 
        readTableOfAccessiblePlayLists(DataInputStream dis, BitStreamIO bio) 
            throws IOException 
    {
        if (TRACE) {
            System.out.println("Table of accessible playlists:");
        }
        // ignore "length" field which is 4 bytes
        readInt("    length (ignored)", dis, bio);
        // read "number_of_acc_Playlists", "access_to_all_flag" and
        // "autostart_first_PlayList_flag" fields.
        // -- which are 11 bits + 1 bit + 1 bit respectively
        final int numPlayLists = readBits("    num playlists", dis, bio, 11);
        TableOfAccessiblePlayLists tapl = new TableOfAccessiblePlayLists();
        boolean b = readBits("    access to all flag", dis, bio, 1) != 0;
        tapl.setAccessToAllFlag(b);
        b = readBits("    autostart first playlist", dis, bio, 1) != 0;
        tapl.setAutostartFirstPlayListFlag(b);
        readBits("    padding", dis, bio, 19);

        String[] playLists = new String[numPlayLists];
        for (int p = 0; p < playLists.length; p++) {
            // read PlayList_file_name field
            playLists[p] = readISO646String("        playlist file name", 
                                            dis, bio, 5);
            // skip reserved_for_word_align field
            readByte("        padding", dis, bio);
        }
        tapl.setPlayListFileNames(playLists);
        return tapl;
    }
    
    private static ApplicationManagementTable 
        readApplicationManagementTable(DataInputStream dis, BitStreamIO bio) 
            throws IOException 
    {
        if (TRACE) {
            System.out.println("Application management table:");
        }
        // ignore "length" field which is 4 bytes
        readInt("    length (ignored)", dis, bio);
        // get "number_of_applications" field
        int numApps = readUnsignedByte("    number of applications", dis, bio);
        // skip reserved_for_word_align field
        readByte("    padding", dis, bio);
        
        AppInfo[] apps = new AppInfo[numApps];
        for (int a = 0; a < numApps; a++) {
            if (TRACE) {
                System.out.println("    " + a + ":");
            }
            AppInfo ai = new AppInfo();
            // get application_control_code field
            ai.setControlCode(
                readByte("        application control code", dis, bio));
            
            // application_type is 4 bits, followed by
            // 4 bit alignment field
            ai.setType((byte)readBits("        application type", dis, bio, 4));
            readBits("        padding", dis, bio, 4);
            
            ai.setOrganizationId(readInt("        org id", dis, bio));
            ai.setApplicationId(readShort("        app id", dis, bio));
            
            // read application descriptor
            // ignore the following fields
            // - descriptor_tag (1 byte)
            // - reserved_word_align (1 byte)
            // - descriptor_length (4 bytes)
            // - reserved_for_future_use (4 bytes)
            readByte("        descriptor_tag (ignored)", dis, bio);
            readByte("        padding", dis, bio);
            readInt("        descriptor_length (ignored)", dis, bio);
            readInt("        padding", dis, bio);
                    
            // Application_profiles_count is 4 bit field
            final int appProfileCount
                = readBits("        application profiles count", dis, bio, 4);
            readBits("        padding", dis, bio, 12);
            
            AppProfile[] profiles = new AppProfile[appProfileCount];
            for (int p = 0; p < appProfileCount; p++) {
                if (TRACE) {
                    System.out.println("        " + p + ":");
                }
                AppProfile ap = new AppProfile();
                ap.setProfile(readShort("            profile", dis, bio));
                ap.setMajorVersion((short)
                    readUnsignedByte("            major version", dis, bio));
                ap.setMinorVersion((short)
                    readUnsignedByte("            minor version", dis, bio));
                ap.setMicroVersion((short)
                    readUnsignedByte("            micro version", dis, bio));
                // skip "reserved_for_word_align" field
                readUnsignedByte("            padding", dis, bio);
                profiles[p] = ap;
            }
            ApplicationDescriptor appDesc = new ApplicationDescriptor();
            appDesc.setProfiles(profiles);
            // get "application_priority" field
            appDesc.setPriority((short)
                readUnsignedByte("        application priority", dis, bio));
            
            // get "application_binding", "Visibility" fields
            // each 2 bits in size and also the "reserved_for_word_align"
            // field (4 bits)
            int bind = readBits("        binding", dis, bio, 2);
            Enum[] bindings = Binding.values();
            for (int i = 0; i < bindings.length; i++) {
                if (bindings[i].ordinal() == bind) {
                    appDesc.setBinding((Binding)bindings[i]);
                    break;
                }
            }
            
            int visibility = readBits("        visibility", dis, bio, 2);
            Enum[] visibilities = Visibility.values();
             for (int i = 0; i < visibilities.length; i++) {
                if (visibilities[i].ordinal() == visibility) {
                    appDesc.setVisibility((Visibility)visibilities[i]);
                    break;
                }
            }
            readBits("        padding", dis, bio, 4);
            
            // read "number_of_application_name_bytes" field
            int totalNameBytes 
                = readUnsignedShort("        application name bytes", dis, bio);
            if (totalNameBytes > 0) {
                int nameBytesRead = 0;
                List<AppName> appNames = new ArrayList<AppName>();
                while (nameBytesRead < totalNameBytes) {
                    AppName an = new AppName();
                    an.setLanguage(readISO646String(
                        "            language", dis, bio, 3));
                    nameBytesRead += 3;
                    int nameLen = readUnsignedByte("            name length",
                                                   dis, bio);
                    nameBytesRead++;
                    an.setName(readUTF8String("            name", dis, bio,
                                              nameLen));
                    nameBytesRead += nameLen;
                    appNames.add(an);
                }
                AppName[] appNamesArr 
                    = appNames.toArray(new AppName[appNames.size()]);
                appDesc.setNames(appNamesArr);
            }
                
            // The field next to the alignment for-loop has to start at 16-bit 
            // word bounday. The current field (in this case "names" is at 
            // 16-bit boundary. So, if the "length" value is odd, then we have 
            // 2 bytes length field + odd number of bytes for the name(s) => we 
            // need to skip one more byte to make it even again.
            if ((totalNameBytes & 0x1) != 0) {
                readByte("        padding", dis, bio);
            }
            
            int iconLength = readUnsignedByte("        icon length", dis, bio);
            appDesc.setIconLocator(readISO646String("        icon locator",
                                                    dis, bio, iconLength));
            
            // length is 1 byte field. If length value is even, then we
            // have 1 byte for length + even bytes for string => we need
            // skip one more byte to make it even again. 
            if ((iconLength & 0x1) == 0) {
                readByte("        padding", dis, bio);
            }
            
            // read "application_icon_flags" field
            appDesc.setIconFlags(readShort("        application icon flags",
                                           dis, bio));
           
            int baseDirLength = readUnsignedByte("        base dir length",
                                                 dis, bio);
            appDesc.setBaseDirectory(readISO646String("        base directory",
                                                      dis, bio, baseDirLength));
            // there is another for-loop here for word align!!
            if ((baseDirLength & 0x1) == 0) {
                dis.skipBytes(1);
            }
            int classPathLength = dis.readUnsignedByte();
            appDesc.setClasspathExtension(
                readISO646String("        classpath extenstion", 
                                 dis, bio, classPathLength));
            // there is another for-loop here for word align!!
            if ((classPathLength & 0x1) == 0) {
                readByte("        padding", dis, bio);
            }
            int initClassLength 
                = readUnsignedByte("        initial class length", dis, bio);
            appDesc.setInitialClassName(
                readUTF8String("        initial class", dis, bio, 
                               initClassLength));
            // there is another for-loop here for word align!!
            if ((initClassLength & 0x1) == 0) {
                readByte("        padding", dis, bio);
            }

            int totalParamBytes = readUnsignedByte("        param bytes",
                                                   dis, bio);
            if (totalParamBytes > 0) {
                int paramBytesRead = 0;
                List<String> params = new ArrayList<String>();
                while (paramBytesRead < totalParamBytes) {
                    int paramLen = readUnsignedByte("            len",dis,bio);
                    paramBytesRead++;
                    params.add(readUTF8String("            param", 
                                              dis, bio, paramLen));
                    paramBytesRead += paramLen;
               }
                String[] paramsArr = params.toArray(new String[params.size()]);
                appDesc.setParameters(paramsArr);
            }
            
            // there is another for-loop here for word align!! 
            if ((totalParamBytes & 0x1) == 0) {
                readByte("        padding", dis, bio);
            }
            
            ai.setApplicationDescriptor(appDesc);
            apps[a] = ai;
        }
        
        ApplicationManagementTable amt = new ApplicationManagementTable();
        amt.setApplications(apps);
        return amt;
    }
    
    public static BDJO readBDJO(InputStream in) throws IOException {
        DataInputStream dis;
        if (in instanceof DataInputStream) {
            dis = (DataInputStream) in;
        } else {
            dis = new DataInputStream(in);
        }
        BitStreamIO bio = new BitStreamIO();
        
        String magic = readISO646String("magic number", dis, bio, 4);
        if (!magic.equals("BDJO")) {
            throw new IOException("BDJO magic is missing, not a bdjo file?");
        }
        String version = readISO646String("version", dis, bio, 4);
        BDJO bdjo = new BDJO();
        bdjo.setVersion(Version.valueOf("V_" + version));

        readInt("terminal info start address (ignored)", dis, bio);
        readInt("app cache info start address (ignored)", dis, bio);
        readInt("table of accessible playlists start address (ignored)", 
                dis, bio);
        readInt("application management table start address (ignored)",
                dis, bio);
        readInt("key interest table start address (ignored)", dis, bio);
        readInt("file access info start address (ignored)", dis, bio);
        
        // reserved_for_future_use 128 bits
        readBitsLong("padding", dis, bio, 64);
        readBitsLong("padding", dis, bio, 64);
        
        bdjo.setTerminalInfo(readTerminalInfo(dis, bio));
        bdjo.setAppCacheInfo(readAppCacheInfo(dis, bio));
        bdjo.setTableOfAccessiblePlayLists(
                readTableOfAccessiblePlayLists(dis, bio));
        bdjo.setApplicationManagementTable(
                readApplicationManagementTable(dis, bio));
        bdjo.setKeyInterestTable(readInt("key interest table", dis, bio));
        int dirPathsLength = readUnsignedShort("file access info length",
                                               dis, bio);
        bdjo.setFileAccessInfo(readISO646String("file access info", dis, 
                                                bio, dirPathsLength));
        
        return bdjo;
    }
    
    public static void main(String[] args) throws Exception {
        BDJO bdjo;
        if (args[0].endsWith(".fx")) {
            BufferedReader reader = new BufferedReader(new FileReader(args[0]));
            bdjo = readFX(reader);
        } else if (args[0].endsWith(".bdjo")) {
            bdjo = readBDJO(new BufferedInputStream(new FileInputStream(args[0])));
        } else {
            BufferedReader reader = new BufferedReader(new FileReader(args[0]));
            bdjo = readXML(reader);
        }
        BDJOWriter.writeFX(bdjo, new PrintWriter(System.out));
    }
}
