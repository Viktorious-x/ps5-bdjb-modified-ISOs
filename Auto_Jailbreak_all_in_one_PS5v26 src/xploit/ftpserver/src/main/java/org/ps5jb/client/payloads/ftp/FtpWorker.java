/*
MIT License

Copyright (c) 2017 Moritz Stückler

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package org.ps5jb.client.payloads.ftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.ps5jb.client.utils.init.SdkInit;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.proc.Process;
import java.util.StringTokenizer;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.include.sys.stat.FileStatusMode;

/**
 * Class for an FTP server worker thread.
 *
 * @author Moritz Stueckler
 *
 */
public class FtpWorker extends Thread {
    static final String DEFAULT_USERNAME = "ps5jb";
    static final String DEFAULT_PASSWORD = "";

    /** SceAuthId set during file download to grant access to restricted files */
    static final long DOWNLOAD_SCE_AUTH_ID = 0x4801000000000013L;

    /**
     * Enable debugging output to console
     */
    private boolean debugMode = true;

    private static SimpleDateFormat fileMonthFormat = new SimpleDateFormat("MMM", Locale.ENGLISH);
    private static SimpleDateFormat fileDayHourFormat = new SimpleDateFormat("dd HH:mm", Locale.ENGLISH);
    private static SimpleDateFormat fileDayYearFormat = new SimpleDateFormat("dd  yyyy", Locale.ENGLISH);
    private static MessageFormat lsDirFormat = new MessageFormat("{0}{1}{2} {3} {4} {5} {6}");

    /**
     * Indicating the last set transfer type
     */
    private static final class transferType {
        private static final String ASCII = "ASCII";
        private static final String BINARY = "BINARY";
    }

    /**
     * Indicates the authentification status of a user
     */
    private static final class userStatus {
        private static final String NOTLOGGEDIN = "NOTLOGGEDIN";
        private static final String ENTEREDUSERNAME = "ENTEREDUSERNAME";
        private static final String LOGGEDIN = "LOGGEDIN";
    }

    // Path information
    private String root;
    private String currDirectory;

    // control connection
    private Socket controlSocket;
    private PrintWriter controlOutWriter;
    private BufferedReader controlIn;

    // data Connection
    private ServerSocket dataSocket;
    private Socket dataConnection;
    private PrintWriter dataOutWriter;

    private int dataPort;
    private String transferMode = transferType.ASCII;

    // user properly logged in?
    private String currentUserStatus = userStatus.NOTLOGGEDIN;
    private String validUser = DEFAULT_USERNAME;
    private String validPassword = DEFAULT_PASSWORD;
    private FtpServer server;
    private boolean useNativeCalls;

    private boolean quitCommandLoop = false;

    /**
     * Create new worker with given client socket.
     *
     * @param server The server instance.
     * @param client The socket for the current client.
     * @param dataPort The port for the data connection.
     * @param name Name of the worker thread.
     * @param useNativeCalls Whether the platform can use native calls for I/O.
     * @throws IOException If any I/O errors occur.
     */
    public FtpWorker(FtpServer server, Socket client, int dataPort, String name, boolean useNativeCalls) throws IOException {
        super(name);

        this.server = server;
        this.controlSocket = client;
        this.dataPort = dataPort;
        this.useNativeCalls = useNativeCalls;

        File rootFile = createFile(System.getProperty("user.dir"));
        rootFile = rootFile.getCanonicalFile();
        while (rootFile.getParentFile() != null) {
            rootFile = rootFile.getParentFile();
        }
        this.root = rootFile.getAbsolutePath();

        File curDir = createFile("/app0");
        if (!isFileExists(curDir)) {
            this.currDirectory = this.root;
        } else {
            this.currDirectory = curDir.getAbsolutePath();
        }
    }

    protected File createFile(String path) {
        File result;
        if (this.useNativeCalls) {
            result = new org.ps5jb.sdk.io.File(path);
        } else {
            result = new File(path);
        }
        return result;
    }

    protected File createFile(File parent, String path) {
        File result;
        if (this.useNativeCalls) {
            result = new org.ps5jb.sdk.io.File(parent, path);
        } else {
            result = new File(parent, path);
        }
        return result;
    }

    protected FileInputStream createFileInputStream(File file) throws FileNotFoundException {
        FileInputStream result;
        if (this.useNativeCalls) {
            result = new org.ps5jb.sdk.io.FileInputStream(file);
        } else {
            result = new FileInputStream(file);
        }
        return result;
    }

    public void terminate() {
        quitCommandLoop = true;

        try {
            controlSocket.close();
        } catch (IOException e) {
            // Don't print the full stacktrace, but output exception message as a one liner.
            Status.println("Warning, the socket could not be closed. This can usually be safely ignored. Cause: " + e.getMessage());
        }
    }

    /**
     * Worker entry point.
     */
    @Override
    public void run() {
        debugOutput("Current working directory " + this.currDirectory);
        try {
            // Input from client
            controlIn = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            try {
                // Output to client, automatically flushed after each print
                controlOutWriter = new PrintWriter(controlSocket.getOutputStream(), true);
                try {
                    // Greeting
                    sendMsgToClient("220 Welcome to the PS5JB FTP-Server");

                    // Get new command from client
                    while (!quitCommandLoop) {
                        try {
                            String command = controlIn.readLine();
                            if (command != null) {
                                executeCommand(command);
                            } else {
                                quitCommandLoop = true;
                            }
                        } catch (SocketException e) {
                            // This usually happens when server forcefully exits so don't print full stacktrace.
                            Status.println(e.getMessage());
                            quitCommandLoop = true;
                        }
                    }
                } finally {
                    controlOutWriter.close();
                }
            } finally {
                controlIn.close();
            }
        } catch (Throwable e) {
            Status.printStackTrace(e.getMessage(), e);
        } finally {
            // Clean up
            try {
                controlSocket.close();
            } catch (IOException e) {
                Status.printStackTrace("Could not close the socket", e);
            }
            closeDataConnection();
            debugOutput("Worker stopped");
        }
    }

    /**
     * Converts path to absolute if it's relative to {@link #currDirectory}.
     *
     * @param path Path to convert.
     * @return File with absolute path.
     */
    private File toAbsoluteFile(String path) {
        File pathFile = createFile(path == null ? currDirectory : path);
        if (!pathFile.isAbsolute() && path != null) {
            pathFile = createFile(createFile(currDirectory), path);
        }
        return pathFile.getAbsoluteFile();
    }

    private boolean isFileValid(File file, boolean checkExists) {
        boolean valid;

        if (checkExists) {
            try {
                file = file.getCanonicalFile();
                valid = isFileExists(file) && file.getAbsolutePath().startsWith(root);
            } catch (IOException e) {
                valid = false;
            }
        } else {
            valid = file.getAbsolutePath().startsWith(root);
        }
        return valid;
    }

    private boolean isFileReadable(File file) {
        boolean readable = false;

        try {
            file = file.getCanonicalFile();
            if (!file.isFile()) {
                if (file instanceof org.ps5jb.sdk.io.File) {
                    org.ps5jb.sdk.io.File sdkFile = (org.ps5jb.sdk.io.File) file;
                    readable = sdkFile.isDevice();
                }
            } else {
                readable = true;
            }
        } catch (IOException e) {
            // leave as not readable
        }

        return readable;
    }

    private boolean isFileExists(File file) {
        // On PS5, root is inaccessible from Java, but it does exist
        return file.getParentFile() == null || file.exists();
    }

    private boolean isDirectory(File file) {
        // On PS5, root is inaccessible from Java, but it is a directory
        return file.getParentFile() == null || file.isDirectory();
    }

    /**
     * Main command dispatcher method. Separates the command from the arguments and
     * dispatches it to single handler functions.
     *
     * @param c The raw input from the socket consisting of command and arguments.
     * @throws IOException When I/O error occurs.
     */
    private void executeCommand(String c) throws IOException {
        // split command and arguments
        int index = c.indexOf(' ');
        String command = ((index == -1) ? c.toUpperCase() : (c.substring(0, index)).toUpperCase());
        String args = ((index == -1) ? null : c.substring(index + 1));

        debugOutput("Command: " + command + " Args: " + args);

        // dispatcher mechanism for different commands
        switch (command) {
            case "USER":
                handleUser(args);
                break;

            case "PASS":
                handlePass(args);
                break;

            case "CWD":
                handleCwd(args);
                break;

            case "LIST":
                handleNlst(args);
                break;

            case "NLST":
                handleNlst(args);
                break;

            case "PWD":
            case "XPWD":
                handlePwd();
                break;

            case "QUIT":
                handleQuit();
                break;

            case "PASV":
                handlePasv();
                break;

            case "EPSV":
                handleEpsv();
                break;

            case "SYST":
                handleSyst();
                break;

            case "FEAT":
                handleFeat();
                break;

            case "PORT":
                handlePort(args);
                break;

            case "EPRT":
                handleEPort(args);
                break;

            case "RETR":
                handleRetr(args);
                break;

            case "MKD":
            case "XMKD":
                handleMkd(args);
                break;

            case "RMD":
            case "XRMD":
                handleRmd(args);
                break;

            case "TYPE":
                handleType(args);
                break;

            case "STOR":
                handleStor(args);
                break;

            case "TERM":
                server.terminate();
                break;

            default:
                sendMsgToClient("501 Unknown command");
                break;

        }

    }

    /**
     * Sends a message to the connected client over the control connection. Flushing
     * is automatically performed by the stream.
     *
     * @param msg The message that will be sent
     */
    private void sendMsgToClient(String msg) {
        controlOutWriter.println(msg);
    }

    /**
     * Send a message to the connected client over the data connection.
     *
     * @param msg Message to be sent
     */
    private void sendDataMsgToClient(String msg) {
        if (dataConnection == null) {
            sendMsgToClient("425 No data connection was established");
            debugOutput("Cannot send message, because no data connection is established");
        } else {
            dataOutWriter.print(msg + '\r' + '\n');
        }

    }

    /**
     * Open a new data connection socket and wait for new incoming connection from
     * client. Used for passive mode.
     *
     * @param port Port on which to listen for new incoming connection
     */
    private void openDataConnectionPassive(int port) {
        closeDataConnection();
        try {
            dataSocket = new ServerSocket(port);
            dataConnection = dataSocket.accept();
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            debugOutput("Data connection - Passive Mode - established");

        } catch (IOException e) {
            Status.printStackTrace("Could not create data connection.", e);
        }

    }

    /**
     * Connect to client socket for data connection. Used for active mode.
     *
     * @param ipAddress Client IP address to connect to
     * @param port      Client port to connect to
     */
    private void openDataConnectionActive(String ipAddress, int port) {
        closeDataConnection();
        try {
            dataConnection = new Socket(ipAddress, port);
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            debugOutput("Data connection - Active Mode - established");
        } catch (IOException e) {
            Status.printStackTrace("Could not connect to client data socket", e);
        }

    }

    /**
     * Close previously established data connection sockets and streams
     */
    private void closeDataConnection() {
        try {
            if (dataOutWriter != null) {
                dataOutWriter.close();
                dataOutWriter = null;
            }
            if (dataConnection != null) {
                dataConnection.close();
                dataConnection = null;
            }
            if (dataSocket != null) {
                dataSocket.close();
                dataSocket = null;
            }

            debugOutput("Data connection was closed");
        } catch (IOException e) {
            Status.printStackTrace("Could not close data connection", e);
        }
    }

    /**
     * Handler for USER command. User identifies the client.
     *
     * @param username Username entered by the user
     */
    private void handleUser(String username) {
        if (username.toLowerCase().equals(validUser)) {
            sendMsgToClient("331 User name okay, need password");
            currentUserStatus = userStatus.ENTEREDUSERNAME;
        } else if (currentUserStatus == userStatus.LOGGEDIN) {
            sendMsgToClient("530 User already logged in");
        } else {
            sendMsgToClient("530 Not logged in");
        }
    }

    /**
     * Handler for PASS command. PASS receives the user password and checks if it's
     * valid.
     *
     * @param password Password entered by the user
     */

    private void handlePass(String password) {
        // User has entered a valid username and password is correct
        if (currentUserStatus == userStatus.ENTEREDUSERNAME && password.equals(validPassword)) {
            currentUserStatus = userStatus.LOGGEDIN;
            sendMsgToClient("230-Welcome to HKUST");
            sendMsgToClient("230 User logged in successfully");
        }

        // User is already logged in
        else if (currentUserStatus == userStatus.LOGGEDIN) {
            sendMsgToClient("530 User already logged in");
        }

        // Wrong password
        else {
            sendMsgToClient("530 Not logged in");
        }
    }

    /**
     * Handler for CWD (change working directory) command.
     *
     * @param args New working directory
     */
    private void handleCwd(String args) {
        File f;

        if ("..".equals(args)) {
            f = toAbsoluteFile(null);
            if (f.getParentFile() != null) {
                f = f.getParentFile();
            }
        } else if (".".equals(args)) {
            f = toAbsoluteFile(null);
        } else {
            f = toAbsoluteFile(args);
        }

        // check if file exists, is directory and is not above root directory
        if (isFileValid(f, true) && isDirectory(f)) {
            currDirectory = f.getAbsolutePath();
            sendMsgToClient("250 The current directory has been changed to " + currDirectory);
        } else {
            sendMsgToClient("550 Requested action not taken. File unavailable: " + currDirectory);
        }
    }

    /**
     * Handler for NLST (Named List) command. Lists the directory content in a short
     * format (names only)
     *
     * @param args The directory to be listed
     */
    private void handleNlst(String args) {
        if (dataConnection == null) {
            sendMsgToClient("425 No data connection was established");
        } else {
            String[] dirContent = nlstHelper(args);

            if (dirContent == null) {
                sendMsgToClient("550 File does not exist.");
            } else {
                sendMsgToClient("125 Opening ASCII mode data connection for file list.");

                for (int i = 0; i < dirContent.length; i++) {
                    sendDataMsgToClient(dirContent[i]);
                }

                sendMsgToClient("226 Transfer complete.");
                closeDataConnection();

            }
        }
    }

    /**
     * A helper for the NLST command. The directory name is obtained by appending
     * "args" to the current directory
     *
     * @param args The directory to list
     * @return an array containing names of files in a directory. If the given name
     *         is that of a file, then return an array containing only one element
     *         (this name). If the file or directory does not exist, return null.
     */
    private String[] nlstHelper(String args) {
        String[] result = null;

        // Now get a File object, and see if the name we got exists and is a
        // directory.
        File f = toAbsoluteFile(args);
        if (isFileValid(f, true)) {
            if (isDirectory(f)) {
                List files = new ArrayList();
                String[] children = f.list();
                if (children != null) {
                    for (String child : children) {
                        files.add(createFile(f, child));
                    }
                }

                if (files.size() > 0) {
                    result = new String[files.size()];
                    for (int i = 0; i < files.size(); ++i) {
                        result[i] = ls((File) files.get(i));
                    }
                }
            } else if (f.isFile()) {
                result = new String[1];
                result[0] = ls(f);
            }
        }

        return result;
    }

    /**
     * Format the file in a format suitable for LIST command.
     *
     * @param f File to format
     * @return A string in the form:
     *   <pre>-rw-r--r-- 1 owner group           213 Aug 26 16:31 README</pre>
     */
    private String ls(File f) {
        String fileSize = Long.toString(f.length());
        StringBuffer padding = new StringBuffer();
        for (int i = 0; i < Math.min(1, 14 - fileSize.length()); ++i) {
            padding.append(" ");
        }

        Date lastModified = new Date(f.lastModified());
        String month = fileMonthFormat.format(lastModified).substring(0, 3);
        String restOfDate;
        if ((System.currentTimeMillis() - lastModified.getTime()) > (182 * 60 * 60 * 1000)) {
            restOfDate = fileDayYearFormat.format(lastModified);
        } else {
            restOfDate = fileDayHourFormat.format(lastModified);
        }

        String owner = "owner";
        String group = "group";
        String mode = isDirectory(f) ? "rwxr-xr-x" : "rw-r--r--";
        FileStatusMode[] modeIter = new FileStatusMode[] {
                FileStatusMode.S_IRUSR,
                FileStatusMode.S_IWUSR,
                FileStatusMode.S_IXUSR,
                FileStatusMode.S_IRGRP,
                FileStatusMode.S_IWGRP,
                FileStatusMode.S_IXGRP,
                FileStatusMode.S_IROTH,
                FileStatusMode.S_IWOTH,
                FileStatusMode.S_IXOTH
        };
        if (f instanceof org.ps5jb.sdk.io.File) {
            org.ps5jb.sdk.io.File sdkFile = (org.ps5jb.sdk.io.File) f;
            owner = sdkFile.getOwner() == 0 ? "root " : Integer.toString(sdkFile.getOwner());
            if (owner.length() > 5) {
                owner = owner.substring(0, 5);
            } else {
                while (owner.length() != 5) {
                    owner = "0" + owner;
                }
            }

            group = sdkFile.getGroup() == 0 ? "root " : Integer.toString(sdkFile.getGroup());
            if (group.length() > 5) {
                group = group.substring(0, 5);
            } else {
                while (group.length() != 5) {
                    group = "0" + group;
                }
            }

            StringBuffer modeBuffer = new StringBuffer();
            Set sdkFileModes = sdkFile.getModes();
            for (int i = 0; i < modeIter.length; ++i) {
                if (sdkFileModes.contains(modeIter[i])) {
                    if (i % 3 == 0) {
                        modeBuffer.append("r");
                    } else if (i % 3 == 1) {
                        modeBuffer.append("w");
                    } else {
                        modeBuffer.append("x");
                    }
                } else {
                    modeBuffer.append("-");
                }
            }
            mode = modeBuffer.toString();
        }

        return lsDirFormat.format(new Object[] {
                isDirectory(f) ? "d" : "-",
                mode + " 1 " + owner + " " + group,
                padding,
                fileSize,
                month,
                restOfDate,
                f.getName()
        });
    }

    /**
     * Handler for the PORT command. The client issues a PORT command to the server
     * in active mode, so the server can open a data connection to the client
     * through the given address and port number.
     *
     * @param args The first four segments (separated by comma) are the IP address.
     *             The last two segments encode the port number (port = seg1*256 +
     *             seg2)
     */
    private void handlePort(String args) {
        // Extract IP address and port number from arguments
        StringTokenizer tokenizer = new StringTokenizer(args, ",");
        String[] stringSplit = splitString(args, ",");
        String hostName = stringSplit[0] + "." + stringSplit[1] + "." + stringSplit[2] + "." + stringSplit[3];

        int p = Integer.parseInt(stringSplit[4]) * 256 + Integer.parseInt(stringSplit[5]);

        // Initiate data connection to client
        openDataConnectionActive(hostName, p);
        sendMsgToClient("200 Command OK");
    }

    /**
     * Handler for the EPORT command. The client issues an EPORT command to the
     * server in active mode, so the server can open a data connection to the client
     * through the given address and port number.
     *
     * @param args This string is separated by vertical bars and encodes the IP
     *             version, the IP address and the port number
     */
    private void handleEPort(String args) {
        final String IPV4 = "1";
        final String IPV6 = "2";

        // Example arg: |2|::1|58770| or |1|132.235.1.2|6275|
        String[] splitArgs = splitString(args, "\\|");
        String ipVersion = splitArgs[1];
        String ipAddress = splitArgs[2];

        if (!IPV4.equals(ipVersion) || !IPV6.equals(ipVersion)) {
            throw new IllegalArgumentException("Unsupported IP version");
        }

        int port = Integer.parseInt(splitArgs[3]);

        // Initiate data connection to client
        openDataConnectionActive(ipAddress, port);
        sendMsgToClient("200 Command OK");

    }

    /**
     * Handler for PWD (Print working directory) command. Returns the path of the
     * current directory back to the client.
     */
    private void handlePwd() {
        sendMsgToClient("257 \"" + currDirectory + "\"");
    }

    /**
     * Handler for PASV command which initiates the passive mode. In passive mode
     * the client initiates the data connection to the server. In active mode the
     * server initiates the data connection to the client.
     */
    private void handlePasv() {
        String myIp = server.getNetAddress();
        String myIpSplit[] = splitString(myIp, "\\.");

        int p1 = dataPort / 256;
        int p2 = dataPort % 256;

        sendMsgToClient("227 Entering Passive Mode (" + myIpSplit[0] + "," + myIpSplit[1] + "," + myIpSplit[2] + ","
                + myIpSplit[3] + "," + p1 + "," + p2 + ")");

        openDataConnectionPassive(dataPort);

    }

    /**
     * Handler for EPSV command which initiates extended passive mode. Similar to
     * PASV but for newer clients (IPv6 support is possible but not implemented
     * here).
     */
    private void handleEpsv() {
        sendMsgToClient("229 Entering Extended Passive Mode (|||" + dataPort + "|)");
        openDataConnectionPassive(dataPort);
    }

    /**
     * Handler for the QUIT command.
     */
    private void handleQuit() {
        sendMsgToClient("221 Closing connection");
        quitCommandLoop = true;
    }

    private void handleSyst() {
        sendMsgToClient("215 PS5JB FTP Server");
    }

    /**
     * Handler for the FEAT (features) command. Feat transmits the
     * abilities/features of the server to the client. Needed for some ftp clients.
     * This is just a dummy message to satisfy clients, no real feature information
     * included.
     */
    private void handleFeat() {
        sendMsgToClient("211-Extensions supported:");
        sendMsgToClient("211 END");
    }

    /**
     * Handler for the MKD (make directory) command. Creates a new directory on the
     * server.
     *
     * @param args Directory name
     */
    private void handleMkd(String args) {
        // Allow only alphanumeric characters
        if (args != null) {
            File dir = toAbsoluteFile(args);

            if (!dir.mkdir()) {
                sendMsgToClient("550 Failed to create new directory");
                debugOutput("Failed to create new directory");
            } else {
                sendMsgToClient("250 Directory successfully created");
            }
        } else {
            sendMsgToClient("550 Invalid directory name");
        }

    }

    /**
     * Handler for RMD (remove directory) command. Removes a directory.
     *
     * @param dir directory to be deleted.
     */
    private void handleRmd(String dir) {
        File f = toAbsoluteFile(dir);

        if (isFileValid(f, true) && isDirectory(f) && f.getParentFile() != null) {
            if (f.delete()) {
                sendMsgToClient("250 Directory was successfully removed");
            } else {
                sendMsgToClient("550 Failed to delete the directory");
            }
        } else {
            sendMsgToClient("550 Invalid directory name");
        }

    }

    /**
     * Handler for the TYPE command. The type command sets the transfer mode to
     * either binary or ascii mode
     *
     * @param mode Transfer mode: "a" for Ascii. "i" for image/binary.
     */
    private void handleType(String mode) {
        if (mode.toUpperCase().equals("A")) {
            transferMode = transferType.ASCII;
            sendMsgToClient("200 OK");
        } else if (mode.toUpperCase().equals("I")) {
            transferMode = transferType.BINARY;
            sendMsgToClient("200 OK");
        } else
            sendMsgToClient("504 Not OK");
        ;

    }

    /**
     * Handler for the RETR (retrieve) command. Retrieve transfers a file from the
     * ftp server to the client.
     *
     * @param file The file to transfer to the user
     */
    private void handleRetr(String file) {
        File f = toAbsoluteFile(file);

        if (!isFileValid(f, true)) {
            sendMsgToClient("550 Invalid file name");
        } else if (!isFileReadable(f)) {
            sendMsgToClient("550 File not readable");
        }
        else {
            // Some files need special auth id
            Process proc = null;
            long oldSceAuthId = 0;
            if (useNativeCalls) {
                SdkInit sdk = SdkInit.instance();
                if (sdk.curProcAddress != 0) {
                    proc = new Process(KernelPointer.valueOf(sdk.curProcAddress));
                    oldSceAuthId = proc.getUserCredentials().getSceAuthId();
                    proc.getUserCredentials().setSceAuthId(DOWNLOAD_SCE_AUTH_ID);
                }
            }
            try {
                // Binary mode
                if (transferMode == transferType.BINARY) {
                    sendMsgToClient("150 Opening binary mode data connection for requested file " + f.getName());

                    BufferedInputStream fin;
                    try {
                        fin = new BufferedInputStream(createFileInputStream(f));
                    } catch (Exception e) {
                        Status.printStackTrace("Could not create input stream", e);
                        sendMsgToClient("550 Could not open requested file");
                        return;
                    }

                    BufferedOutputStream fout;
                    try {
                        // create streams
                        fout = new BufferedOutputStream(dataConnection.getOutputStream());
                    } catch (Exception e) {
                        Status.printStackTrace("Could not create output stream", e);
                        sendMsgToClient("550 Error writing to data connection");
                        return;
                    }

                    debugOutput("Starting file transmission of " + f.getName());

                    // write file with buffer
                    byte[] buf = new byte[1024];
                    int l = 0;
                    try {
                        while ((l = fin.read(buf, 0, 1024)) != -1) {
                            fout.write(buf, 0, l);
                        }
                    } catch (IOException e) {
                        Status.printStackTrace("Could not read from or write to file streams", e);
                    }

                    // close streams
                    try {
                        fin.close();
                        fout.close();
                    } catch (IOException e) {
                        Status.printStackTrace("Could not close file streams", e);
                    }

                    debugOutput("Completed file transmission of " + f.getName());
                    sendMsgToClient("226 File transfer successful. Closing data connection.");
                }

                // ASCII mode
                else {
                    sendMsgToClient("150 Opening ASCII mode data connection for requested file " + f.getName());

                    BufferedReader rin = null;
                    PrintWriter rout = null;

                    try {
                        rin = new BufferedReader(new FileReader(f));
                        rout = new PrintWriter(dataConnection.getOutputStream(), true);

                    } catch (IOException e) {
                        debugOutput("Could not create file streams");
                    }

                    String s;

                    try {
                        while ((s = rin.readLine()) != null) {
                            rout.println(s);
                        }
                    } catch (IOException e) {
                        Status.printStackTrace("Could not read from or write to file streams", e);
                    }

                    try {
                        rout.close();
                        rin.close();
                    } catch (IOException e) {
                        Status.printStackTrace("Could not close file streams", e);
                    }
                    sendMsgToClient("226 File transfer successful. Closing data connection.");
                }
            } finally {
                if (proc != null) {
                    proc.getUserCredentials().setSceAuthId(oldSceAuthId);
                }
            }
        }
        closeDataConnection();

    }

    /**
     * Handler for STOR (Store) command. Store receives a file from the client and
     * saves it to the ftp server.
     *
     * @param file The file that the user wants to store on the server
     */
    private void handleStor(String file) {
        if (file == null) {
            sendMsgToClient("501 No filename given");
        } else {
            File f = toAbsoluteFile(file);

            if (isFileExists(f)) {
                sendMsgToClient("550 File already exists");
            } else if (!isFileValid(f, false)) {
                sendMsgToClient("550 Invalid file name");
            }
            else {
                // Binary mode
                if (transferMode == transferType.BINARY) {
                    BufferedOutputStream fout = null;
                    BufferedInputStream fin = null;

                    sendMsgToClient("150 Opening binary mode data connection for requested file " + f.getName());

                    try {
                        // create streams
                        fout = new BufferedOutputStream(new FileOutputStream(f));
                        fin = new BufferedInputStream(dataConnection.getInputStream());
                    } catch (Exception e) {
                        debugOutput("Could not create file streams");
                    }

                    debugOutput("Start receiving file " + f.getName());

                    // write file with buffer
                    byte[] buf = new byte[1024];
                    int l = 0;
                    try {
                        while ((l = fin.read(buf, 0, 1024)) != -1) {
                            fout.write(buf, 0, l);
                        }
                    } catch (IOException e) {
                        Status.printStackTrace("Could not read from or write to file streams", e);
                    }

                    // close streams
                    try {
                        fin.close();
                        fout.close();
                    } catch (IOException e) {
                        Status.printStackTrace("Could not close file streams", e);
                    }

                    debugOutput("Completed receiving file " + f.getName());

                    sendMsgToClient("226 File transfer successful. Closing data connection.");

                }

                // ASCII mode
                else {
                    sendMsgToClient("150 Opening ASCII mode data connection for requested file " + f.getName());

                    BufferedReader rin = null;
                    PrintWriter rout = null;

                    try {
                        rin = new BufferedReader(new InputStreamReader(dataConnection.getInputStream()));
                        rout = new PrintWriter(new FileOutputStream(f), true);

                    } catch (IOException e) {
                        debugOutput("Could not create file streams");
                    }

                    String s;

                    try {
                        while ((s = rin.readLine()) != null) {
                            rout.println(s);
                        }
                    } catch (IOException e) {
                        Status.printStackTrace("Could not read from or write to file streams", e);
                    }

                    try {
                        rout.close();
                        rin.close();
                    } catch (IOException e) {
                        Status.printStackTrace("Could not close file streams", e);
                    }
                    sendMsgToClient("226 File transfer successful. Closing data connection.");
                }

            }
            closeDataConnection();
        }

    }

    /**
     * Debug output.
     *
     * @param msg Debug message.
     */
    private void debugOutput(String msg) {
        if (debugMode) {
            Status.println(msg);
        }
    }

    /**
     * Splits the string by a delimiter.
     *
     * @param str String to split.
     * @param delim Delimiter to split on.
     * @return Array of sub-strings.
     */
    private String[] splitString(String str, String delim) {
        StringTokenizer tokenizer = new StringTokenizer(str, delim);
        List result = new ArrayList();
        while (tokenizer.hasMoreTokens()) {
            String nextArg = tokenizer.nextToken();
            result.add(nextArg);
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

}
