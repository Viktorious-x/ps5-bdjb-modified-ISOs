
/*  
 * Copyright (c) 2009, Sun Microsystems, Inc.
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

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * This contains utility methods to read and write JSON-formatted objects.
 * See http://json.org for the extremely simple syntax.  In addition to 
 * that syntax, the reader discards whitespace, and comments in the "//",
 * "/*" and "#" form.
 * <p>
 * JSON has a clear mapping to Java types that we use in this class:
 * <pre>
 *      JSON TYPE  ->  Java Type
 *
 *          Object -> HashMap
 *           Array -> Object[]
 *          String -> String
 *          Number -> java.lang.Number (subclass determined by value)
 *      true/false -> Boolean
 *            null -> null
 * </pre>
 * For numbers, the reader will produce Integer, Long or Double; the
 * writer will accept Integer, Long, Float or Double.
 *
 * @author Bill Foote (http://jovial.com)
 */

public class JsonIO {

    //
    // No public constructor
    //
    private JsonIO() {
    }

    //
    // The maximum long value's most significant digit
    //
    private static long LONG_MAX_MSD = 9000000000000000000l;

    /**
     * Write a JSON object to out.  The argument must correspond to the
     * JSON type as described in the class documentation, one of
     * HashMap, Object[], String, Integer, Long, Float, Double, Boolean or null.
     * See the class documentation  for other details of the
     * syntax.
     *
     * @param   out     The stream to write to.  A buffered writer is
     *                  recommended; a UTF-8 character encoding is common
     *                  for JSON streams. 
     *
     * @throws  IOException if there is an underlying IO exception, or if value
     *                      contains an invalid type.
     *
     * @see JsonIO
     **/
    public static void writeJSON(Writer out, Object value) throws IOException {
        if (value == null) {
            out.write("null");
        } else if (value instanceof Number) {
            out.write(value.toString());
        } else if (value instanceof Boolean) {
            if (Boolean.TRUE.equals(value)) {
                out.write("true");
            } else {
                out.write("false");
            }
        } else if (value instanceof String) {
            String s = (String) value;
            out.write('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '"') {
                    out.write('\\');
                    out.write('"');
                } else if (c == '\\') {
                    out.write("\\\\");
                } else if (c == '\b') {
                    out.write("\\b");
                } else if (c == '\f') {
                    out.write("\\f");
                } else if (c == '\n') {
                    out.write("\\n");
                } else if (c == '\r') {
                    out.write("\\r");
                } else if (c == '\t') {
                    out.write("\\t");
                } else if (c < 32 || c > 126) {
                    out.write("\\u");
                    String hex = Integer.toHexString(c);
                    for (int j = hex.length(); j < 4; j++) {
                        out.write('0');
                    }
                    out.write(hex);
                } else {
                    out.write(c);
                }
            }
            out.write('"');
        } else if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            out.write('[');
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) {
                    out.write(',');
                }
                writeJSON(out, arr[i]);
            }
            out.write(']');
        } else if (value instanceof Map) {
            Map map = (Map) value;
            out.write('{');
            boolean first = true;
            for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
                if (first) {
                    first = false;
                } else {
                    out.write(',');
                }
                Map.Entry ent = (Map.Entry) it.next();
                writeJSON(out, ent.getKey());
                out.write(':');
                writeJSON(out, ent.getValue());
            }
            out.write('}');
        } else {
            throw new IOException("Invalid type " + value.getClass() + " for " 
                                  + value);
        }
    }


    /**
     * Read a JSON object from rdr.  The result will correspond to the
     * JSON type as described in the class documentation, one of
     * HashMap, Object[], String, Integer, Long, Double, Boolean or null.
     * rdr will be positioned one character after the last character of the
     * JSON value.  See the class documentation  for other details of the
     * syntax.
     *
     * @param   rdr     The stream to read from.  A buffered reader is
     *                  recommended; a UTF-8 character encoding is common
     *                  for JSON streams.  The reader must be one where
     *                  markSupported() returns true, e.g. a BufferedReader.
     *
     * @throws  IOException if there is an underlying IO exception, a
     *                      syntax error, or if rdr.markSupported is false.
     *
     * @see JsonIO
     **/
    public static Object readJSON(Reader rdr) throws IOException {
        if (!rdr.markSupported()) {
            throw new IOException("Reader.markSupported must be true");
        }
        for (;;) {
            int c = rdr.read();
            if (c == -1) {
                throw new IOException("Unexpected EOF");
            }
            char ch = (char) c;
            if (skipWhitespace(ch, rdr)) {
                continue;
            }  else if (ch == '"' || ch == '\'') {
                return readString(rdr, ch);
            } else if (ch == '{') {
                return readHashMap(rdr);
            } else if (ch == '[') {
                return readArray(rdr);
            }
            ch = Character.toLowerCase(ch);
            if (ch == 't') {
                readConstant(rdr, "rue");
                return Boolean.TRUE;
            } else if (ch == 'f') {
                readConstant(rdr, "alse");
                return Boolean.FALSE;
            } else if (ch == 'n') {
                readConstant(rdr, "ull");
                return null;
            } else {
                return readNumber(rdr, ch);
            }
        }
    }

    //
    // Skip whitespace, including comments.  Return true iff ch used.
    //
    private static boolean skipWhitespace(int ch, Reader rdr) throws IOException {
        if (Character.isWhitespace((char) ch)) {
            return true;
        } else if (ch == '/') {
            skipSlashComment(rdr);
            return true;
        } else if (ch == '#') {
            skipToEOLN(rdr);
            return true;
        }
        return false;
    }

    private static void skipSlashComment(Reader rdr) throws IOException {
        int c = rdr.read();
        if (c == '/') {
            skipToEOLN(rdr);
        } else if (c == '*') {
            boolean starSeen = false;
            for (;;) {
                c = rdr.read();
                if (c == -1) {
                    throw new IOException("Unexpected EOF");
                } else if (starSeen && c == '/') {
                    return;
                }
                starSeen = c == '*';
            }
        } else {
            throw new IOException("Syntax error");
        }
    }

    private static void skipToEOLN(Reader rdr) throws IOException {
        for (;;) {
            int c = rdr.read();
            if (c == -1) {
                throw new IOException("Unexpected EOF");
            }
            if (c == '\n' || c == '\r') {
                return;
            }
        }
    }

    private static String readString(Reader rdr, char delimiter) 
            throws IOException 
    {
        StringBuffer buf = new StringBuffer();
        for (;;) {
            int c = rdr.read();
            if (c == -1) {
                throw new IOException("Unexpected EOF");
            } else if (c == '\\') {
                c = rdr.read();
                switch (c) {
                    case -1:
                        throw new IOException("Unexpected EOF");
                    case 'b':
                        buf.append('\b');
                        break;
                    case 't':
                        buf.append('\t');
                        break;
                    case 'n':
                        buf.append('\n');
                        break;
                    case 'f':
                        buf.append('\f');
                        break;
                    case 'r':
                        buf.append('\r');
                        break;
                    case 'u':
                        buf.append(parseHex(rdr, 4));
                        break;
                    case 'x':
                        buf.append(parseHex(rdr, 2));
                        break;
                    default:
                        buf.append((char) c);
                        break;
                }
            } else if (c == delimiter) {
                return buf.toString();
            } else {
                buf.append((char) c);
            }
        }
    }

    private static char parseHex(Reader rdr, int digits) throws IOException {
        int val = 0;
        for (int i = 0; i < digits; i++) {
            val *= 16;
            int ch = rdr.read();
            if (ch >= '0' && ch <= '9') {
                val += (ch - '0');
            } else if (ch >= 'A' && ch <= 'F') {
                val += (ch - 'A' + 10);
            } else if (ch >= 'a' && ch <= 'f') {
                val += (ch - 'a' + 10);
            } else {
                throwUnexpected(ch);
            }
        }
        return (char) val;
    }

    private static HashMap readHashMap(Reader rdr) throws IOException {
        HashMap result = new HashMap();
        for (;;) {
            int ch = rdr.read();
            if (skipWhitespace(ch, rdr)) {
                continue;
            } else if (ch == '}') {
                return result;
            } else if (ch == ',') {
                continue;
            } else if (ch == '"' || ch == '\'') {
                String key = readString(rdr, (char) ch);
                for (;;) {
                    ch = rdr.read();
                    if (ch == ':') {
                        break;
                    } else if (skipWhitespace(ch, rdr)) {
                        continue;
                    } else {
                        throwUnexpected(ch);
                    }
                }
                Object value = readJSON(rdr);
                result.put(key, value);
            } else {
                throwUnexpected(ch);
            }
        }
    }

    private static Object[] readArray(Reader rdr) throws IOException {
        for (;;) {
            rdr.mark(1);
            int ch = rdr.read();
            if (ch == -1) {
                throwUnexpected(ch);
            } else if (ch == ']') {
                return new Object[0];
            } else if (skipWhitespace(ch, rdr)) {
                continue;
            } else {
                rdr.reset();
                break;
            }
        }
        ArrayList result = new ArrayList();
        for (;;) {
            result.add(readJSON(rdr));
            for (;;) {
                int ch = rdr.read();
                if (ch == ',') {
                    break;
                } else if (ch == ']') {
                    return result.toArray(new Object[result.size()]);
                } else if (skipWhitespace(ch, rdr)) {
                    continue;
                } else { 
                    throwUnexpected(ch);
                }
            }
        }
    }

    private static void readConstant(Reader rdr, String wanted) 
            throws IOException 
    {
        for (int i = 0; i < wanted.length(); i++) {
            int ch = rdr.read();
            if (ch != (int) wanted.charAt(i)) {
                throwUnexpected(ch);
            }
        }
    }

    private static Number readNumber(Reader rdr, char initial) throws IOException {
        boolean negative = false;
        boolean digitSeen = false;
        int value = 0;          // Kept as a negative value
            // Value is kept as a negative number throughtout.  That's because
            // abs(Integer.MIN_VALUE) > abs(Integer.MIN_VALUE).
        int ch = initial;
        if (initial == '-') {
            negative = true;
            ch = rdr.read();
        }
        for (;;) {
            if (ch >= '0' && ch <= '9') {
                digitSeen = true;
                if (value <= (Integer.MIN_VALUE / 10)) {
                    // It might or mignt not overflow if it's ==
                    return readLong(rdr, negative, value, ch);
                }
                value *= 10;
                value -= (ch - '0');    // value is negative
                rdr.mark(1);
                ch = rdr.read();
            } else if (ch == '.') {
                return readDouble(rdr, negative, value, true);
            } else if (ch == 'e' || ch == 'E') {
                double v = negative ? ((double) value) : (-((double) value));
                return readScientific(rdr, v);
            } else if (digitSeen) {
                rdr.reset();
                if (negative) {
                    return new Integer(value);
                } else {
                    return new Integer(-value);
                }
            } else {
                throwUnexpected(ch);
            }
        }
    }

    //
    // Read a number that might be a long.  It might be an integer that's
    // close to Integer.MAX_VALUE or Integer.MIN_VALUE too; in this case an
    // Integer is returned.
    //
    // value is negative
    //
    private static Number readLong(Reader rdr, boolean negative, 
                                   long value, int ch) 
        throws IOException 
    {
        long limit = negative ? Long.MIN_VALUE : -Long.MAX_VALUE;
        limit += LONG_MAX_MSD;  // Knock the most significant digit off
        value *= 10;
        value -= (ch - '0');    // Remember, value is negative
        for (;;) {
            rdr.mark(1);
            ch = rdr.read();
            if (ch >= '0' && ch <= '9') {
                if (value < (Long.MIN_VALUE / 10)) {
                    rdr.reset();
                    return readDouble(rdr, negative, value, false);
                }
                value *= 10;
                int digit = ch - '0';
                if ((value + LONG_MAX_MSD) - digit < limit) {
                    double v = value;
                    v -= digit;
                    return readDouble(rdr, negative, v, false);
                }
                value -= digit;
            } else if (ch == '.') {
                return readDouble(rdr, negative, value, true);
            } else if (ch == 'e' || ch == 'E') {
                double v = negative ? ((double) value) : (-((double) value));
                readScientific(rdr, v);
            } else {
                rdr.reset();
                if (negative) {
                    if (value >= Integer.MIN_VALUE) {
                        return new Integer((int) value);
                    } else {
                        return new Long(value);
                    }
                } else {
                    if (value >= -Integer.MAX_VALUE) {
                        return new Integer((int) -value);
                    } else {
                        return new Long(-value);
                    }
                }
            }
        }
    }

    //
    // Read a double
    //
    // value is negative
    //
    private static Number readDouble(Reader rdr, boolean negative, double value, 
                                     boolean decimalSeen)
        throws IOException
    {
        while (!decimalSeen) {
            rdr.mark(1);
            int ch = rdr.read();
            if (ch >= '0' && ch <= '9') {
                value *= 10;
                value -= ch - '0';      // value is negative
            } else if (ch == '.') {
                decimalSeen = true;
            } else if (ch == 'e' || ch == 'E') {
                if (!negative) {
                    value = -value;
                }
                return readScientific(rdr, value);
            } else {
                rdr.reset();
                if (negative) {
                    return new Double(value);
                } else {
                    return new Double(-value);
                }
            }
        }
        double multiplier = 1.0;
        for (;;) {
            rdr.mark(1);
            int ch = rdr.read();
            if (ch >= '0' && ch <= '9') {
                multiplier /= 10;
                value -= multiplier * (ch - '0');       // value is negative
            } else if (ch == 'e' || ch == 'E') {
                if (!negative) {
                    value = -value;
                }
                return readScientific(rdr, value);
            } else {
                rdr.reset();
                if (negative) {
                    return new Double(value);
                } else {
                    return new Double(-value);
                }
            }
        }
    }

    //
    // Read a double after an 'e' or 'E' is seen
    //
    // value is the correct sign for the number being read
    //
    private static Number readScientific(Reader rdr, double value) 
            throws IOException 
    {
        boolean expNegative = false;
        int ch = rdr.read();
        if (ch == '+') {
            ch = rdr.read();
        } else if (ch == '-') {
            expNegative = true;
            ch = rdr.read();
        }
        if (ch < '0' || ch > '9') {
            throwUnexpected(ch);
        }
        int exp = ch - '0';     
            // We don't worry about exponent overflow, since the biggest
            // exponent for a positive number is only 309.
        for (;;) {
            rdr.mark(1);
            ch = rdr.read();
            if (ch >= '0' && ch <= '9') {
                exp *= 10;
                exp += ch - '0';
            } else {
                rdr.reset();
                if (expNegative) {
                    return new Double(value / Math.pow(10.0, exp));
                } else {
                    return new Double(value * Math.pow(10.0, exp));
                }
            }
        }
    }

    private static void throwUnexpected(int ch) throws IOException {
        String str;
        if (ch == -1) {
            str = "EOF";
        } else {
            str = "" + ((char) ch);
        }
        throw new IOException("Syntax error in JSON object:  " + str 
                              + " unexpected.");
    }
}
