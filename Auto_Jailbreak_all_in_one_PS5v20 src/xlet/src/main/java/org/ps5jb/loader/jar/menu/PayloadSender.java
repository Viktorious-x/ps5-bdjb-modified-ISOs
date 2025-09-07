package org.ps5jb.loader.jar.menu;

import org.ps5jb.loader.Status;

import java.io.*;
import java.net.Socket;

public class PayloadSender {

    private static String lastElfName = null;

    public static void sendPayloadFromFile(File elfFile) {
        Status.println("Trying to send " + elfFile.getAbsolutePath() + " to elfldr on port 9021...");

        FileInputStream fileInputStream = null;
        Socket elfldrSocket = null;
        try {
            fileInputStream = new FileInputStream(elfFile);
            elfldrSocket = new Socket("127.0.0.1", 9021);
            OutputStream outputStream = elfldrSocket.getOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();

            lastElfName = elfFile.getName(); // 👈 speichern des Namens

            Status.println(elfFile.getAbsolutePath() + " was sent to elfldr on port 9021.");
        } catch (FileNotFoundException e) {
            Status.printStackTrace("Detected invalid file to send", e);
        } catch (IOException e) {
            Status.printStackTrace("Failed to open elfldr socket", e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (elfldrSocket != null) {
                try {
                    elfldrSocket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    // Neue Methode zum Abrufen des letzten Dateinamens
    public static String getLastElfName() {
        return lastElfName;
    }
}
