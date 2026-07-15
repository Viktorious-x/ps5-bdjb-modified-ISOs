package org.ps5jb.loader.jar.menu;

import org.ps5jb.loader.Config;
import org.ps5jb.loader.Status;
import org.ps5jb.loader.jar.JarLoader;
import org.ps5jb.loader.jar.menu.PayloadSender;
import java.io.*;
import java.net.DatagramSocket;

public class PipelineRunner {
    
    private static String getHistoryFilePath() {
        return "/OS/HDD/download0/xlet/cache/last_pipeline.txt";
    }

    private static void preInitializeNetworkSandbox() {
        try {
            DatagramSocket dummySocket = new DatagramSocket(0);
            dummySocket.close();
            Status.println("init pipe");
        } catch (Exception e) {
        }
    }

    public static void runPipeline(File pipelinePath, JarLoader jarLoader) {
        try {
            preInitializeNetworkSandbox();
            saveLastPipeline(pipelinePath.getAbsolutePath());
            parsePipeline(pipelinePath, jarLoader);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void saveLastPipeline(String absolutePath) {
        BufferedWriter bw = null;
        try {
            File historyFile = new File(getHistoryFilePath());
            File parentDir = historyFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            bw = new BufferedWriter(new FileWriter(historyFile));
            bw.write(absolutePath);
            bw.flush();
        } catch (Exception e) {
        } finally {
            if (bw != null) { try { bw.close(); } catch (Exception e) {} }
        }
    }

    public static String loadLastPipeline() {
        BufferedReader br = null;
        try {
            File historyFile = new File(getHistoryFilePath());
            if (historyFile.exists()) {
                br = new BufferedReader(new FileReader(historyFile));
                return br.readLine();
            }
        } catch (Exception e) {
        } finally {
            if (br != null) { try { br.close(); } catch (Exception e) {} }
        }
        return null;
    }

    private static void parsePipeline(File pipelinePath, JarLoader jarLoader) throws Exception {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(pipelinePath));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) continue;

                if (line.toLowerCase().startsWith("jar disc ")) {
                    final String discFileName = line.substring("jar disc ".length()).trim();
                    final File discFile = new File(Config.getLoaderPayloadPath(), discFileName);
                    if (discFile.exists()) {
                        jarLoader.loadJar(discFile, false);
                    } else {
                        Status.println("Pipeline error: Could not find disc file: " + discFileName);
                    }

                } else if (line.toLowerCase().startsWith("jar usb ")) {
                    final String usbFileName = line.substring("jar usb ".length()).trim();
                    File usbPayloadRoot = findUsbPayloads();
                    if (usbPayloadRoot == null) return;

                    final File usbFile = new File(usbPayloadRoot, usbFileName);
                    if (usbFile.exists()) {
                        jarLoader.loadJar(usbFile, false);
                    } else {
                        Status.println("Pipeline error: Could not find USB file: " + usbFileName);
                    }

                } else if (line.toLowerCase().startsWith("elf usb ")) {
                    final String usbFileName = line.substring("elf usb ".length()).trim();
                    File usbPayloadRoot = findUsbPayloads();
                    if (usbPayloadRoot == null) return;

                    final File usbFile = new File(usbPayloadRoot, usbFileName);
                    if (usbFile.exists()) {
                        PayloadSender.sendPayloadFromFile(usbFile);
                    } else {
                        Status.println("Pipeline error: Could not find USB file: " + usbFileName);
                    }

                } else if (line.toLowerCase().startsWith("sleep ")) {
                    final String sleepSeconds = line.substring("sleep ".length()).trim();
                    Status.println("Pipeline: Sleeping for " + sleepSeconds + " seconds...");
                    Thread.sleep(Integer.parseInt(sleepSeconds) * 1000);
                }
            }
        } finally {
            if (br != null) { br.close(); }
        }
    }

    private static File findUsbPayloads() {
        File usbPayloadRoot = null;
        for (int i = 0; i < 8; i++) {
            try {
                File f = new File("/mnt/usb" + i);
                String[] payloadFiles = f.list((dir, name) -> {
                    String n = name.toLowerCase();
                    return n.endsWith(".jar") || n.endsWith(".elf") || n.endsWith(".bin");
                });

                if (f.exists() && payloadFiles != null && payloadFiles.length > 0) {
                    Status.println("Found USB with payloads at: " + f.getAbsolutePath());
                    usbPayloadRoot = f;
                    break;
                }
            } catch (Exception ex) {
                Status.println("Error while searching USB" + i);
            }
        }
        if (usbPayloadRoot != null && usbPayloadRoot.isDirectory() && usbPayloadRoot.canRead()) {
            return usbPayloadRoot;
        }
        Status.println("Pipeline error: No USB payloads found.");
        return null;
    }
}
