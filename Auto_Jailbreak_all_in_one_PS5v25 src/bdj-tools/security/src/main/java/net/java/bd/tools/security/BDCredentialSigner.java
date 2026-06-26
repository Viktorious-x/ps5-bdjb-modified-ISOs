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

import java.util.Enumeration;
import java.util.jar.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import sun.tools.jar.Main; // this may not work on Apple's jdk!
import java.io.IOException;
import java.io.File;

public class BDCredentialSigner {
    
    static boolean debug = false;
    static boolean isBudaCredential = false;
    static String jarFileName;
    static String permReqFileName;
    static SecurityUtil.Builder sBuilder;
    static CredentialUtil.Builder cBuilder;
    static boolean generateCred = true;
    static boolean updateCerts = true;
    
    public static void main(String args[]) throws  Exception {   
        parseArgs(args);
        CredentialUtil cUtil = cBuilder.build();
        if (permReqFileName == null) {
            printUsageAndExit("Please specify the permission request file " +
                    " to be updated with signed credentials");
        }
        if (generateCred) {
            // 1.
            if (debug) { 
                System.out.println("[Debug] extracting permission request file from the jar ..");
            }
            
            if (jarFileName != null) {
                extractFile(jarFileName, permReqFileName);
            }

            // 2. Generate credentials
            if (debug) {
                System.out.println("[Debug] Generating the credentials...");
            }
            cUtil.genCredentials();
        }
        if (!updateCerts) {
            return;
        }
        // 3. Replace the permission request file with the one that is signed
        if (debug) {
            System.out.println("[Debug] Updating the jar file with permission" +
                    "request file that has credentials..");
        }
        if (jarFileName == null) {
            printUsageAndExit("Please specify the jarfile tobe updated and signed");
        } else {
            System.err.println("jarfilename:" + jarFileName);
        }
        updateJar(jarFileName, permReqFileName);
        // 4. sign the updated jar file
        if (debug) {
            System.out.println("[Debug] Signing the updated jar file...");
        }
        SecurityUtil sUtil = sBuilder.build();
        sUtil.signJars();
        
        // 5. update the certs of the signer jar file
        if (debug) {
            System.out.println("[Debug] Updating the signed jar file with grantor's certs....");
        }
        cUtil.updateCerts();
        
        if (debug) {
            CredentialVerifier.verify(jarFileName, permReqFileName, 
                                        "app.discroot.crt", isBudaCredential);
        }
    }
    
    private static void extractFile(String jarFileName, String fileToExtract) {
        String jarOpt = "-xf";
        if (debug) {
            jarOpt = "-xvf";
        }
        String[] jarArgs = {jarOpt, jarFileName, fileToExtract};
        Main jar = new Main(System.out, System.err, "jar");
        if (!jar.run(jarArgs)) {
            System.out.println("Unable to extract file:" + fileToExtract);
            System.exit(1);
        } 
    }
    
    private static void updateJar(String jarFileName, String newPermReqFile) {
        String jarOpt = "-uf";
        if (debug) {
            jarOpt = "-uvf";
        }
        String[] jarArgs = {jarOpt, jarFileName, newPermReqFile};
        Main jar = new Main(System.out, System.err, "jar");
        if (!jar.run(jarArgs)) {
            System.out.println("Unable to update the jar file:" + jarFileName);
            System.exit(1);
        } 
    }
    
     public static void parseArgs(String args[]) {
        if (args.length < 2) {
            printUsageAndExit("");
        }
        boolean nextOptJar = false;
        cBuilder = new CredentialUtil.Builder();
        sBuilder = new SecurityUtil.Builder();
        for(int i = 0; i < args.length; i++) {
            String opt = args[i];
            if (opt.equals("-debug")) {
                debug = true;
                cBuilder = cBuilder.debug();
                sBuilder = sBuilder.debug();
            } else if (opt.equals("-gencred")) {
                updateCerts = false;
            } else if (opt.equals("-updatecerts")) {
                generateCred = false;
            } else if (opt.equals("-buda")) {
                isBudaCredential = true;
                cBuilder = cBuilder.budaCredential();
            } else if (opt.equals("-gastore")) {
                if (++i == args.length) errorNeedArgument(opt);
                cBuilder = cBuilder.grantorKeyStore(args[i]);
            } else if (opt.equals("-gastorepass")) {
                if (++i == args.length) errorNeedArgument(opt);
                cBuilder = cBuilder.grantorStorePass(args[i]);
            } else if (opt.equals("-gaalias")) {
                if (++i == args.length) errorNeedArgument(opt);
                cBuilder = cBuilder.grantorAlias(args[i]);
            } else if (opt.equals("-gakeypass")) {
                if (++i == args.length) errorNeedArgument(opt);
                cBuilder = cBuilder.grantorPassword(args[i]);
           } else if (opt.equals("-gecert")) {
                if (++i == args.length) errorNeedArgument(opt);
                cBuilder = cBuilder.granteeRootCert(args[i]);
            } else if (opt.equals("-store")) {
                 if (++i == args.length) errorNeedArgument(opt);
                 cBuilder = cBuilder.granteeKeyStore(args[i]);
                 sBuilder = sBuilder.keystoreFile(args[i]);
            } else if (opt.equals("-storepass")) {
                 if (++i == args.length) errorNeedArgument(opt);
                 cBuilder = cBuilder.granteeStorePass(args[i]);
                 sBuilder = sBuilder.storepass(args[i]);
            } else if (opt.equals("-gerootalias")) {
               if (++i == args.length) errorNeedArgument(opt);
               cBuilder = cBuilder.granteeRootAlias(args[i]);
            } else if (opt.equals("-alias")) {
                if (++i == args.length) errorNeedArgument(opt);
               sBuilder = sBuilder.contentSignerAlias(args[i]);
            } else if (opt.equals("-keypass")) {
               if (++i == args.length) errorNeedArgument(opt);
               sBuilder = sBuilder.contentSignerPassword(args[i]);
            } else if (opt.equals("-gacerts")) {
               if (++i == args.length) errorNeedArgument(opt);
               cBuilder = cBuilder.grantorCertFile(args[i]);
            }  else if (opt.equals("-help")) {
                printUsageAndExit("");
            } else {
                if (!nextOptJar) {
                    cBuilder = cBuilder.permReqFile(opt);
                    nextOptJar = true;
                    permReqFileName = opt;
                } else {
                    cBuilder = cBuilder.jarFile(opt);
                    List<String> list = new ArrayList<String>();
                    list.add(args[i]);
                    sBuilder = sBuilder.jarfiles(list);
                    jarFileName = opt;
                }
            }
        }
    }
    
      private static void printUsageAndExit(String reason) {
        if (!reason.isEmpty()) {
            System.err.println();
            System.err.println("FAILED: " + reason);
        }
        System.err.println("\n-----------------------------------------------------------------------");
        System.err.println("This tool is for generating credentials, updating and signing the jar file\n" +
                           "with the new permission request file and grantor's certificates as per\n" +
                           "the BD-J specificiation.");
        System.err.println("For credential generation the input permission request file should have all\n" +
                           "fields of persistent credentials but the <signature> and the <certchainfileid>\n" +
                           "fields/elements. The grantor keystore is used for signing the credentials.\n" +
                           "The grantee keystore is used for signing the final jar after it's updated with\n" +
                           "credentials");  
        System.err.println("-----------------------------------------------------------------------\n");
        
        System.err.println("usage: BDCredentialSigner [options] permission-request-file jarfile\n");
        System.err.println("Valid Options:");
        System.err.println(" -gencred             \t:Generate credentials;Output:PRF with credentials");
        System.err.println(" -buda                \t:Generate BUDA credentials (default: Persistence storage credentials)");
        System.err.println(" -gastore filename    \t:Grantor's keystore;default used:\"grantor.store\"");
        System.err.println("                      \t from the current working directory");
        System.err.println(" -gastorepass password\t:Grantor's keystore password");
        System.err.println(" -gaalias alias       \t:Grantor's alias");
        System.err.println(" -gakeypass password  \t:Grantor's key password;used for signing credentials");
        System.err.println(" -gecert              \t:File containing disc root certificate of the grantee");
        System.err.println(" -updatecerts         \t:Update certificate chain and sign the jar");
        System.err.println(" -store filename      \t:Grantee's keystore;default used:\"keystore.store\"");
        System.err.println("                      \t from the current working directory");
        System.err.println(" -storepass password\t:Grantee's keystore password");
        System.err.println(" -alias alias         \t:Alias for the signing key");
        System.err.println(" -keypass password    \t:Password for accessing the signing key");
        System.err.println(" -gacerts             \t:File containing grantor certificate chain");
        System.err.println(" -debug               \t:Prints debug messages");
        System.err.println(" -help                \t:Prints this message");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("net.java.bd.tools.security.BDCredentialSigner -gencred -gecert grantee.discroot.crt bluray.MyXlet.perm");
        System.err.println();
        System.err.println("net.java.bd.tools.security.BDCredentialSigner -updatecert -gacerts grantorchain.crt bluray.MyXlet.perm 00000.jar");
        System.err.println();
        System.err.println("net.java.bd.tools.security.BDCredentialSigner -gencert -buda discroot.crt bluray.MyXlet.perm");
        System.err.println("For testing:");
        System.err.println("net.java.bd.tools.security.BDCredentialSigner bluray.MyXlet.perm 00000.jar");
        System.err.println();
        System.exit(1);
     }    
        
     private static void tinyHelp() {
        System.err.println("Try BDCredentialSigner -help");
                
        // do not drown user with the help lines.
        System.exit(1);
    }

    static private void errorNeedArgument(String flag) {
        System.err.println("Command option " + flag + " needs an argument.");
        tinyHelp();
    }
}
