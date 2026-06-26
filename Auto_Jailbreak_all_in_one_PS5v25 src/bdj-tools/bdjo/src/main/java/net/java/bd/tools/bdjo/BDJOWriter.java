
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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.helpers.DefaultValidationEventHandler;

import com.hdcookbook.grin.util.BitStreamIO;

/**
 * A class to write BDJO object as JavaFX object literal
 * or as a XML document.
 *
 * @author A. Sundararajan
 */
public final class BDJOWriter {
    // don't create me!
    private BDJOWriter() {}
    
    private static final String FOUR_SPACES = "    ";
    
    private static String escape(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"') {
                sb.append('"');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private static void writeValue(int intend, Object value, PrintWriter pw) {
        Class c = value.getClass();
        if (c.isPrimitive()) {
            if (c == Character.TYPE) {
                pw.print('\'');
                pw.print(value);
                pw.print('\'');
            } else {
                pw.print(value);
            }
        } else {
            if (value instanceof Enum) {
                Enum e = (Enum)value;
                pw.print(e.name());
                pw.print(":<<");
                pw.print(c.getName());
                pw.print(">>");
            } else if (value instanceof Number) {
                pw.print(value.toString());
            } else if (value instanceof String) {
                pw.print('"');
                pw.print(escape((String)value));
                pw.print('"');
            } else if (value instanceof Character) {
                pw.print('\'');
                pw.print(((Character)value).charValue());
                pw.print('\'');
            } else if (value instanceof Boolean) {
                pw.print(((Boolean)value).booleanValue());
            } else {
                writeObjectLiteral(intend + 1, value, pw);
            }
        }
    }
    
    private static void writeObjectLiteral(int intend, Object obj, PrintWriter pw) {
        try {
            Class clazz = obj.getClass();
            BeanInfo bi = Introspector.getBeanInfo(clazz);
            PropertyDescriptor[] props = bi.getPropertyDescriptors();
            boolean isArray = clazz.isArray();
            if (isArray) {
                pw.println('[');
                int len = Array.getLength(obj);
                Class ec = clazz.getComponentType();
                for (int i = 0; i < len; i++) {
                    Object val = Array.get(obj, i);
                    for (int j = 0; j < intend; j++) {
                        pw.print(FOUR_SPACES);
                    }
                    if (val == null) {
                        pw.print("null");
                    } else {
                        writeValue(intend, val, pw);
                    }
                    pw.println(", ");
                }
                for (int i = 0; i < intend - 1; i++) {
                    pw.print(FOUR_SPACES);
                }
                pw.print(']');
            } else {
                pw.println('{');
                for (PropertyDescriptor pd : props) {
                    Class c = pd.getPropertyType();
                    Method readMethod = pd.getReadMethod();
                    Object value = readMethod.invoke(obj, (Object[])null);
                    String name = pd.getName();
                    if (name.equals("class")) {
                        continue;
                    }
                    if (value == null) {
                        continue;
                    }
                    for (int i = 0; i < intend; i++) {
                        pw.print(FOUR_SPACES);
                    }
                    pw.print(name);
                    pw.print(" : ");
                    XmlJavaTypeAdapter typeAdapter = 
                            readMethod.getAnnotation(XmlJavaTypeAdapter.class);
                    if (typeAdapter != null) {
                        Class type = typeAdapter.value();                 
                        int val = 0;
                        if (type == HexStringByteAdapter.class) {
                            val = 0x0FF & ((Byte)value).intValue();
                            pw.print("0x" + Integer.toHexString(val));
                        } else if (type == HexStringShortAdapter.class) {
                            val = 0x0FFFF & ((Short)value).intValue();
                            pw.print("0x" + Integer.toHexString(val));
                        } else if (type == HexStringIntegerAdapter.class) {
                            val = ((Integer)value).intValue();
                            pw.print("0x" + Integer.toHexString(val));
                        } else {
                            writeValue(intend, value, pw);
                        }
                    } else {
                        writeValue(intend, value, pw);
                    }
                    pw.println(",");
                }
                for (int i = 0; i < intend - 1; i++) {
                    pw.print(FOUR_SPACES);
                }
                pw.print('}');
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception exp) {
            throw new RuntimeException(exp);
        }
    }
    
    public static void writeFX(BDJO bdjo, Writer writer) throws IOException {
        PrintWriter pw = (writer instanceof PrintWriter)?
            (PrintWriter)writer : new PrintWriter(writer);
        String className = BDJO.class.getName();
        String pkgName = className.substring(0, className.lastIndexOf('.'));
        pw.println("// Generated on " + new Date());
        pw.println();
        pw.print("import ");
        pw.print(pkgName);
        pw.println(".*;");
        pw.println();
        pw.print("BDJO ");
        writeObjectLiteral(1, bdjo, pw);
        pw.flush();
    }
    
    public static void writeXML(BDJO bdjo, Writer writer) throws JAXBException {
        String className = BDJO.class.getName();
        String pkgName = className.substring(0, className.lastIndexOf('.'));
        JAXBContext jc = JAXBContext.newInstance(pkgName);
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.setEventHandler(new DefaultValidationEventHandler());
        m.marshal(bdjo, writer);
    }
    
    public static String writeXML(BDJO bdjo) throws JAXBException {
        StringWriter sw = new StringWriter();
        writeXML(bdjo, sw);
        return sw.toString();
    }
    
    private static byte[] getISO646Bytes(String str) {
        try {
            return str.getBytes("ISO646-US");
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }
    
    private static byte[] getUTF8Bytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }
    
    private static int getUTF8Length(String str) {
        // is there a better way?
        return getUTF8Bytes(str).length;
    }
    
    // section 10.2.2.2 TerminalInfo - Syntax
    private static byte[] writeTerminalInfoBuf(TerminalInfo ti) 
            throws IOException 
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        BitStreamIO bio = new BitStreamIO();
        // write "length" field
        final int length = 5 /* default font file name */ +
                     1 /* havi config, menu call mask, title search mask */ +
                     4 /* reserved for future use field */;
        
        dos.writeInt(length);
        // write default_font_file_name
        String fontFile = ti.getDefaultFontFile();
        if (fontFile == null) {
            fontFile = "*****";
        }
        dos.write(getISO646Bytes(fontFile));
        // write initial_HAVi_configuration_id, menu_call_mask and
        // Title_search_mask fields (4 bits + 1 bit + 1 bit)
        HaviDeviceConfig config = ti.getInitialHaviConfig();
        int id = config.getId();
        bio.writeBits(dos, 4, config.getId());
        bio.writeBits(dos, 1, (ti.isMenuCallMask() ? 1 : 0));
        bio.writeBits(dos, 1, (ti.isTitleSearchMask() ? 1 : 0));
        bio.writeBits(dos, 1, (ti.isMouseSupported() ? 1 : 0));
        bio.writeBits(dos, 1, (ti.isMouseInterest() ? 1 : 0));
        bio.writeBits(dos, 2, ti.getInitialOutputMode());
        bio.writeBits(dos, 4, ti.getInitialFrameRate());
        bio.writeBitsLong(dos, 26, 0);  // padding
        
        bio.assertByteAligned(1);
        dos.flush();
        return bos.toByteArray();
    }
    
    // section 10.2.3.2 AppCacheInfo - Syntax
    private static byte[] writeAppCacheInfoBuf(AppCacheInfo aci)
            throws IOException 
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        AppCacheEntry[] entries = aci.getEntries();
        if (entries == null) {
            entries = new AppCacheEntry[0];
        }
        final int entrySize = 1 /* entry_type */ +
                              5 /* ref_to_name */ +
                              3 /* language_code */ +
                              3 /* reserved_for_future_use */;
        
        final int length = 1 /* number_of_entries */ +
                           1 /* reserved_for_word_align */ +
                           (entries.length * entrySize);
        
        dos.writeInt(length);
        dos.writeByte(entries.length);
        // reserved_for_word_align */
        dos.writeByte(0);
        for (AppCacheEntry ace : entries) {            
            dos.writeByte(ace.getType());
            String name = ace.getName();
            if (name == null) {
                name = "";
            }
            String lang = ace.getLanguage();
            if (lang == null) {
                lang = BDJO.LANGUAGE_CODE_ENGLISH;
            }
            dos.write(getISO646Bytes(name));
            dos.write(getISO646Bytes(lang));
            // reserved_for_future_use field
            dos.writeByte(0);
            dos.writeByte(0);
            dos.writeByte(0);
        }
        
        dos.flush();
        return bos.toByteArray();
    }
    
    // section 10.2.4.2 TableOfAccessiblePlayLists - Syntax
    private static byte[] writeTableOfAccessiblePlayListsBuf(
            TableOfAccessiblePlayLists tapl) throws IOException 
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        BitStreamIO bio = new BitStreamIO();
        String[] playLists = tapl.getPlayListFileNames();
        if (playLists == null) {
            playLists = new String[0];
        }
        
        final int playListSize = 5 /* PlayList_file_name */ +
                1 /* reserved_for_word_align */;
        
        final int length = 2 /* number_of_acc_PlayLists + 2 1-bit flags */ +
                2 /* reserved_for_future_use 19 bits - 3 bits in last word */ +
                (playLists.length * playListSize);
        
        dos.writeInt(length);
        // 11 bit number_of_acc_PlayLists + 1 bit access_to_all_flag +
        // 1 bit autostart_first_PlayList_flag
        bio.writeBits(dos, 11, playLists.length);
        bio.writeBits(dos, 1, (tapl.isAccessToAllFlag() ? 1 : 0));
        bio.writeBits(dos, 1, (tapl.isAutostartFirstPlayListFlag() ? 1 : 0));
        bio.writeBits(dos, 3, 0);       // padding
        bio.assertByteAligned(1);

        // reserved_for_future_use bits
        dos.writeShort(0);
        for (String pl : playLists) {
            if (pl == null) {
                pl = "";
            }
            dos.write(getISO646Bytes(pl));
            // reserved_for_word_align
            dos.writeByte(0);
        }
        
        dos.flush();
        return bos.toByteArray();
    }
    
    private static byte[] 
        writeApplicationDescriptorBuf(ApplicationDescriptor appDesc)
            throws IOException 
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        BitStreamIO bio = new BitStreamIO();
        
        AppProfile[] profiles = appDesc.getProfiles();
        if (profiles == null) {
            profiles = new AppProfile[0];
        }
       
        final int profileSize = 2 /* application_profile */ +
                1 /* version.major */ +
                1 /* version.minor */ +
                1 /* version.micro */ +
                1 /* reserved_for_word_align */;
        
        int length = 4 /* reserved for future use */ +
                2 /* profile count + align */ +
                (profiles.length * profileSize) +
                1 /* application_priority */ +
                1 /* application_binding + visibility + align */ +
                2 /* number_of_application_name_bytes */;
        
        AppName[] names = appDesc.getNames();
        if (names == null) {
            names = new AppName[0];
        }
        
        int totalNameLen = 0;
        for (AppName an : names) {
            totalNameLen += 3; /* application_language_code */;
            totalNameLen += 1; /* application_name_length */;
            String name = an.getName();
            if (name == null) {
                name = "";
            }
            int nameLen = getUTF8Length(name);
            totalNameLen += nameLen;
        }
        length += totalNameLen;
        if ((totalNameLen & 0x1) != 0) {
            length++; // for reserved_for_word_align
        }
        
        length++; // application_icon_locator_length
        String iconLoc = appDesc.getIconLocator();
        if (iconLoc == null) {
            iconLoc = "";
        }
        int iconLocLength = iconLoc.length();
        length += iconLocLength;
        if ((iconLocLength & 0x1) == 0) {
            length++; // for reserved_for_word_align
        }
        
        length += 2; // application_icon_flags
        
        length++; // base_directory_length
        String baseDir = appDesc.getBaseDirectory();
        if (baseDir == null) {
            baseDir = "";
        }
        int baseDirLength = baseDir.length();
        length += baseDirLength;
        if ((baseDirLength & 0x1) == 0) {
            length++; // for reserved_for_word_align
        }
        
        length++; // classpath_extension_length
        String classPath = appDesc.getClasspathExtension();
        if (classPath == null) {
            classPath = "";
        }
        int classPathLength = classPath.length();
        length += classPathLength;
        if ((classPathLength & 0x1) == 0) {
            length++; // for reserved_for_word_align
        }
        
        length++; // initial_class_name_length
        String initClassName = appDesc.getInitialClassName();
        if (initClassName == null) {
            initClassName = "";
        }
        int classNameLength = getUTF8Length(initClassName);
        length += classNameLength;
        if ((classNameLength & 0x1) == 0) {
            length++; // for reserved_for_word_align
        }
        
        length++; // number_of_overall_parameter_bytes
        String[] params = appDesc.getParameters();
        if (params == null) {
            params = new String[0];
        }
        int totalParamLen = 0;
        for (String p : params) {
            int len = getUTF8Length(p);
            totalParamLen++; // number_of_parameter_bytes
            totalParamLen += len;
        }
        length += totalParamLen;
        if ((totalParamLen & 0x1) == 0) {
            length++; // for reserved_for_word_align
        }
        
        
        // start writing application descriptor
        dos.writeByte(0); // descriptor_tag - has to be 0
        dos.writeByte(0); // reserved_for_word_align 
        dos.writeInt(length); // descriptor_length
        dos.writeInt(0); // reserved_for_future_use
        bio.writeBits(dos, 4, profiles.length);
        bio.writeBits(dos, 12, 0);      // padding
        bio.assertByteAligned(1);
        for (AppProfile ap : profiles) {
            dos.writeShort(ap.getProfile());
            dos.writeByte(ap.getMajorVersion());
            dos.writeByte(ap.getMinorVersion());
            dos.writeByte(ap.getMicroVersion());
            dos.writeByte(0); // reserved_for_word_align
        }
        dos.writeByte(appDesc.getPriority());
        
        if (appDesc.getBinding() == null) {
            throw new IOException("Missing binding for application description whose initial class name is " + initClassName);
        }
        if (appDesc.getVisibility() == null) {
            throw new IOException("Missing visibility for application description whose initial class name is " + initClassName);
        }
        bio.writeBits(dos, 2, appDesc.getBinding().ordinal());
        bio.writeBits(dos, 2, appDesc.getVisibility().ordinal());
        bio.writeBits(dos, 4, 0);       // padding
        bio.assertByteAligned(1);
        dos.writeShort(totalNameLen);
        
        for (AppName an : names) {
            String lang = an.getLanguage();
            if (lang == null) {
                lang = BDJO.LANGUAGE_CODE_ENGLISH;
            }
            dos.write(getISO646Bytes(lang));
            String name = an.getName();
           if (name == null) {
                name = "";
            }
            byte[] utf8Bytes = getUTF8Bytes(name);
            dos.writeByte(utf8Bytes.length);
            dos.write(utf8Bytes);
            utf8Bytes = null;
        }
        
        if ((totalNameLen & 0x1) != 0) {
            dos.writeByte(0); // reserved_word_for_align
        }
        
        // application_icon_locator_length
        dos.writeByte(iconLocLength);
        dos.write(getISO646Bytes(iconLoc));
        if ((iconLocLength & 0x1) == 0) {
            dos.writeByte(0); // reserved_for_word_align
        }
        
        // application_icon_flags
        dos.writeShort(appDesc.getIconFlags());
        
        // base_directory_length
        dos.writeByte(baseDirLength);
        dos.write(getISO646Bytes(baseDir));
        if ((baseDirLength & 0x1) == 0) {
            dos.writeByte(0); // reserved_for_word_align
        }
        
        // classpath_extension_length
        dos.writeByte(classPathLength);
        dos.write(getISO646Bytes(classPath));
        if ((classPathLength & 0x1) == 0) {
            dos.writeByte(0); // reserved_for_word_align
        }
        
        // initial_class_name_length
        dos.writeByte(classNameLength);
        dos.write(getUTF8Bytes(initClassName));
        if ((classNameLength & 0x1) == 0) {
            dos.writeByte(0); // reserved_for_word_align
        }
        
        // number_of_overall_parameter_bytes
        dos.writeByte(totalParamLen);        
        for (String p : params) {
            if (p == null) {
                p = "";
            }
            byte[] utf8Bytes = getUTF8Bytes(p);
            dos.writeByte(utf8Bytes.length);
            dos.write(utf8Bytes);
            utf8Bytes = null;
        }
        
        if ((totalParamLen & 0x1) == 0) {
            dos.writeByte(0); // reserved_for_word_align
        }
        
        dos.flush();
        return bos.toByteArray();
    }
    
    // section 10.2.5.2 ApplicationManagementTable - Syntax
    private static byte[] 
        writeApplicationManagementTableBuf(ApplicationManagementTable amt) 
            throws IOException 
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        AppInfo[] apps = amt.getApplications();
        if (apps == null) {
            apps = new AppInfo[0];
        }
        
        int length = 1 /* number_of_applications */ +
                1 /* reserved_word_for_align */;
       
        List<byte[]> appDescBufs = new ArrayList<byte[]>(apps.length);
        for (AppInfo ai : apps) {
            ApplicationDescriptor appDesc = ai.getApplicationDescriptor();
            length += 1 /* application_control_code */ +
                1 /* application_type + reserved_align_word */ +
                4 /* application_id */ +
                2 /* organization_id */;
            appDescBufs.add(writeApplicationDescriptorBuf(appDesc));
        }
        
        for (byte[] buf : appDescBufs) {
            length += buf.length;
        }
        
        // start writing ApplicationManagementTable
        dos.writeInt(length);
        dos.writeByte(apps.length);
        // reserved_for_word_align
        dos.writeByte(0);
        int idx = 0;
        for (AppInfo ai : apps) {
            dos.writeByte(ai.getControlCode());
            dos.writeByte(ai.getType() << 4);
            dos.writeInt(ai.getOrganizationId());
            dos.writeShort(ai.getApplicationId());
            dos.write(appDescBufs.get(idx));
            idx++;
        }
        
        dos.flush();
        return bos.toByteArray();
    }
    
    public static void writeBDJO(BDJO bdjo, OutputStream out) throws IOException {
        TerminalInfo ti = bdjo.getTerminalInfo();
        if (ti == null) {
            ti = new TerminalInfo();
        }
        byte[] terminalInfoBuf = writeTerminalInfoBuf(ti);
        
        AppCacheInfo aci = bdjo.getAppCacheInfo();
        if (aci == null) {
            aci = new AppCacheInfo();
        }
        byte[] appCacheInfoBuf = writeAppCacheInfoBuf(aci);
        
        TableOfAccessiblePlayLists tapl = bdjo.getTableOfAccessiblePlayLists();
        if (tapl == null) {
            tapl = new TableOfAccessiblePlayLists();
        }
        byte[] tableOfAccessiblePlayListsBuf =
                writeTableOfAccessiblePlayListsBuf(tapl);
        
        ApplicationManagementTable amt = bdjo.getApplicationManagementTable();
        if (amt == null) {
            amt = new ApplicationManagementTable();
        }
        byte[] applicationManagementTableBuf =
                writeApplicationManagementTableBuf(amt);
                
        String fai = bdjo.getFileAccessInfo();
        if (fai == null) {
            fai = "";
        }
        byte[] fileAccessInfoBuf = getISO646Bytes(fai);
        
        // start writing the file
        DataOutputStream dos;
        if (out instanceof DataOutputStream) {
            dos = (DataOutputStream)out;
        } else {
            dos = new DataOutputStream(out);
        }
        dos.write(getISO646Bytes("BDJO"));
        dos.write(getISO646Bytes(bdjo.getVersion().getValue()));        // four bytes
        
        final int terminalInfoStart = 4 /* type_indicator */ +
                4 /* version_number */ +
                4 /* TerminalInfo_start_address */ +
                4 /* AppCacheInfo_start_address */ +
                4 /* TableOfAccessiblePlayLists_start_address */ +
                4 /* ApplicationManagementTable_start_address */ +
                4 /* KeyInterestTable_start_address */ +
                4 /* FileAccessInfo_start_address */ +
                16 /* 128 bit reserved_for_future_use */;
        
        dos.writeInt(terminalInfoStart);
        final int appCacheInfoStart = terminalInfoStart + terminalInfoBuf.length;
        dos.writeInt(appCacheInfoStart);
        final int tableOfAccessiblePlayListsStart = appCacheInfoStart +
                appCacheInfoBuf.length;
        dos.writeInt(tableOfAccessiblePlayListsStart);
        
        final int appManagementTableStart = tableOfAccessiblePlayListsStart +
                tableOfAccessiblePlayListsBuf.length;
        dos.writeInt(appManagementTableStart);
        
        final int keyInterestTableStart = appManagementTableStart +
                applicationManagementTableBuf.length;
        dos.writeInt(keyInterestTableStart);
        
        final int fileAccessInfoStart = keyInterestTableStart +
                4 /* keyInterestTable is 4 bytes */;
        dos.writeInt(fileAccessInfoStart);
        
        // 128-bit reserved_for_future_use
        dos.writeLong(0L);
        dos.writeLong(0L);
        
        dos.write(terminalInfoBuf);
        dos.write(appCacheInfoBuf); 
        dos.write(tableOfAccessiblePlayListsBuf);
        dos.write(applicationManagementTableBuf);
        dos.writeInt(bdjo.getKeyInterestTable());
        dos.writeShort(fileAccessInfoBuf.length);
        dos.write(fileAccessInfoBuf);
        // reserved_for_word_align byte
        if ((fileAccessInfoBuf.length & 0x1) != 0) {
            dos.writeByte(0);
        }
        dos.flush();
    }
    
    public static void main(String[] args) throws Exception {
        BDJO b = new BDJO();
        b.setTerminalInfo(new TerminalInfo());
        ApplicationManagementTable amt = new ApplicationManagementTable();
        amt.setApplications(new AppInfo[0]);
        b.setApplicationManagementTable(amt);
        writeFX(b, new PrintWriter(System.out));
    }
}
