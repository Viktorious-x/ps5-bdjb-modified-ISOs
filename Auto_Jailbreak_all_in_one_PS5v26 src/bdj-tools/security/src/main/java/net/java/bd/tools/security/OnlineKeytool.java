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

package net.java.bd.tools.security;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.math.BigInteger;

import java.net.URL;

import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPublicKeySpec;

import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.x509.X509V3CertificateGenerator;

//import sun.misc.HexDumpEncoder;

/**
 * OnlineKeyTool is used for importing the private and
 * public keys from BDA provided files into a Java Keystore.
 */
public class OnlineKeytool {

    String keyFile;
    String crtFile;
    X509Certificate cert;
    PrivateKey privateKey;

    // Used by the command line tool
    static String keystoreFile = SecurityUtil.DEF_KEYSTORE_FILE;
    static String keystorePassword = SecurityUtil.DEF_KEYSTORE_PASSWORD;
    static String alias = SecurityUtil.DEF_ONLINE_CERT_ALIAS;
    static String keyPass = SecurityUtil.DEF_ONLINEKEY_PASSWORD;
    static KeyStore ks;
    public boolean debug = false;

    /**
     * Creates an OnlineKeytool instance that reads private and
     * public keys from the given files
     * <p>
     * @param kfile Private key file.
     * @param cfile online.crt file
     * @param debug
     */
    OnlineKeytool(String kfile, String cfile, boolean debug) {
        keyFile = kfile;
        crtFile = cfile;
        this.debug = debug;
    }

    /** Adds a new KeyEntry to the given keystore
     *  The private key and the certificate are created using the binary files.
     * <p>
     * @param store
     * @param alias
     * @param keypass
     * @return
     * @throws java.lang.Exception
     */
    public KeyStore importKeys(KeyStore store, String alias, String keypass)
            throws Exception {
        createPrivateKey();
        createSelfSignedCert();
        store.setKeyEntry(alias, this.privateKey, keypass.toCharArray(),
                new X509Certificate[]{this.cert});
        return store;
    }

    /**
     *  Creates a private key from the key CRT parameters
     * <p>
     * @throws java.lang.Exception
     */
    public void createPrivateKey() throws Exception {
        FileInputStream fin = new FileInputStream(keyFile);
        OnlinePrivateKeyReader kr = new OnlinePrivateKeyReader(fin);
        BigInteger m = kr.readModulus().abs();
        BigInteger pbe = kr.readPublicExponent();
        BigInteger pre = kr.readPrivateExponent();
        BigInteger p = kr.readPrimeP();
        BigInteger q = kr.readPrimeQ();
        BigInteger pe = kr.readPrimeExponentP();
        BigInteger qe = kr.readPrimeExponentQ();
        BigInteger coeff = kr.readCrtCoefficient();
        fin.close();
        /*if (debug) {
            HexDumpEncoder hexDump = new HexDumpEncoder();
            System.out.println("PrimeP:" + hexDump.encodeBuffer(p.toByteArray()));
            System.out.println();
            System.out.println("PrimeQ:" + hexDump.encodeBuffer(q.toByteArray()));
            System.out.println();
            System.out.println("Modulus:" + hexDump.encodeBuffer(m.toByteArray()));
            System.out.println();
            System.out.println("Private e:" + hexDump.encodeBuffer(pre.toByteArray()));
            System.out.println();
            System.out.println("Exponent e:" + hexDump.encodeBuffer(pe.toByteArray()));
            System.out.println();
            System.out.println("Exponent Q:" + hexDump.encodeBuffer(qe.toByteArray()));
            System.out.println();
            System.out.println("CRT coeff :" + hexDump.encodeBuffer(coeff.toByteArray()));
            System.out.println();
        }*/
        RSAPrivateCrtKeySpec privateSpec = new RSAPrivateCrtKeySpec(
                m, pbe, pre, p, q, pe, qe, coeff);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        this.privateKey = factory.generatePrivate(privateSpec);
    }

    /**
     * Creates a self signed certificate using the public key
     * from the "online.crt" file. See BD-ROM specification part 3.2,
     * version 2.2 Annex DD.
     * The certificate fields other than the key pair do not matter.
     * The certificate is just a place holder in the keystore.
     * The private key is created from the BDA binary file.
     * <p>
     * @throws java.lang.Exception
     */
    public void createSelfSignedCert() throws Exception {
        DataInputStream dis = new DataInputStream(
                new FileInputStream(crtFile));

        String typeIndicator = SecurityUtil.readISO646String(dis, 4);
        if (!typeIndicator.equals("OCRT")) {
            throw new RuntimeException("Invalid TypeIndicator: " + typeIndicator +
                    " in " + crtFile);
        }
        String versionNo = SecurityUtil.readISO646String(dis, 4);
        if (!versionNo.equals("0200")) {
            throw new RuntimeException("Invalid Version No:" + versionNo +
                    " in " + crtFile);
        }
        // reserved bytes
        dis.skipBytes(32);
        int certVerNo = dis.readInt();
        if (certVerNo != 0) {
            throw new RuntimeException("Invalid Certificate Version No:" +
                    certVerNo);
        }
        int contentOwnerId = dis.readInt();
        if (debug) {
            System.out.println("Content owner ID:" + contentOwnerId);
        }
        // reserved bytes
        dis.skipBytes(32);
        String contentOwnerName = SecurityUtil.readISO646String(dis, 256);
        if (debug) {
            System.out.println("Content owner Name:" + contentOwnerName);
        }
        String dn = "cn=" + contentOwnerName; // used in cert

        // read the public key modulus 'n'
        byte[] buf = new byte[256];
        int read = dis.read(buf);
        if (read == -1) {
            throw new IOException("Reached end of file before finished reading the file:" +
                    crtFile);
        }
        BigInteger modulus = new BigInteger(1, buf);
        /*HexDumpEncoder hexDump = null;
        if (debug) {
            System.out.println("public modulus:");
            hexDump = new HexDumpEncoder();
            System.out.println(hexDump.encodeBuffer(modulus.toByteArray()));
        }*/
        BigInteger exponent = BigInteger.valueOf(65537);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = factory.generatePublic(spec);

        // create a self signed cert for importing into the keystore.
        X509V3CertificateGenerator cg = new X509V3CertificateGenerator();
        cg.reset();
        Calendar calendar = Calendar.getInstance();
        calendar.set(0000, 1, 1);
        Date validFrom = calendar.getTime();
        calendar.clear();
        calendar.set(9999, 1, 1);
        Date validTo = calendar.getTime();
        cg.setNotBefore(validFrom);
        cg.setNotAfter(validTo);
        cg.setPublicKey(publicKey);
        cg.setSignatureAlgorithm("SHA1WITHRSA");
        cg.setSubjectDN(new X509Name(dn));

        cg.setIssuerDN(new X509Name(dn));
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
        BigInteger serNo = new BigInteger(32, prng);
        cg.setSerialNumber(serNo);
        this.cert = cg.generate(this.privateKey);

        // read the BDA signature; not currently used
        //int read = dis.read(buf);
        //if (read == -1) {
        //    throw new IOException("Reached end of file before finished reading the file:" +
        //            crtFile);
        //}
        dis.close();
    }

    static void initKeyStore() throws Exception {

        char[] password = keystorePassword.toCharArray();
        ks = KeyStore.getInstance(KeyStore.getDefaultType());
        File kfile = new File(keystoreFile);
        if (!kfile.exists()) {
            ks.load(null, password);
            FileOutputStream fout = new FileOutputStream(kfile);
            ks.store(fout, password);
            fout.close();
        }
        URL url = new URL("file:" + kfile.getCanonicalPath());
        InputStream is = url.openStream();
        ks.load(is, password);
        is.close();
    }

    public static void main(String args[]) throws Exception {
        String keyFile = null;
        String crtFile = null;
        boolean keyFileExpected = true;
        boolean debug = false;

        for (int i = 0; i < args.length; i++) {
            String opt = args[i];
            if (opt.equals("-keystore")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                keystoreFile = args[i];
            } else if (opt.equals("-storepass")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                keystorePassword = args[i];
            } else if (opt.equals("-alias")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                alias = args[i];
            } else if (opt.equals("-keypass")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                keyPass = args[i];
            } else if (opt.equals("-help")) {
                printUsageAndExit("");
            } else if (opt.equals("-debug")) {
                debug = true;
            } else {
                if (keyFileExpected) {
                    keyFile = args[i];
                    keyFileExpected = false;
                } else {
                    crtFile = args[i];
                }
            }
        }
        checkFile(keyFile);
        checkFile(crtFile);
        initKeyStore();
        OnlineKeytool tool = new OnlineKeytool(keyFile, crtFile, debug);
        ks = tool.importKeys(ks, alias, keyPass);

        // make the updated keystore persistent
        FileOutputStream fos = new FileOutputStream(keystoreFile);
        ks.store(fos, keystorePassword.toCharArray());
        fos.close();
    }

    static void checkFile(String file) {
        if ((file == null) || (!(new File(file)).exists())) {
            printUsageAndExit("File Not Found:" + file);
        }
    }

    static private void tinyHelp() {
        System.err.println("Try OnlineKeytool -help");

        // do not drown user with the help lines.
        System.exit(1);
    }

    static private void errorNeedArgument(String flag) {
        System.err.println("Command option " + flag + " needs an argument.");
        tinyHelp();
    }

    private static void printUsageAndExit(String reason) {
        if (!reason.isEmpty()) {
            System.err.println("\nFailed: " + reason);
        }
        System.err.println("\n/**");
        System.err.println(" This is a tool for importing online credentials" +
                " into a Keystore");
        System.err.println(" This tool imports the private and public online keys obtained");
        System.err.println(" from BDA into the keystore.");
        System.err.println("**/\n");
        System.err.println("usage: OnlineKeytool [options] <private key file> <online.crt file>\n");
        System.err.println("Valid Options:");
        System.err.println(" -keystore filename  \t:Keystore where the keys get stored");
        System.err.println("                     \t In the absense of this option, a default store:\"keystore.store\"");
        System.out.println("                     \t is used from the current working directory.");
        System.err.println(" -storepass password \t:Keystore password");
        System.err.println(" -alias alias        \t:Alias for the online keys");
        System.err.println(" -keypass password   \t:Password for online keys");
        System.err.println(" -help               \t:Prints this message");
        System.err.println("\nExample: java net.java.bd.tools.security.OnlineKeytool owner.bin online.crt\n");
        System.exit(1);
    }
}
