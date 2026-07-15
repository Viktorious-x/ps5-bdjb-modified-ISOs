package org.ps5jb.loader.jar.menu;

import org.dvb.event.EventManager;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.havi.ui.HContainer;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.event.HRcEvent;
import org.ps5jb.loader.Config;
import org.ps5jb.loader.Status;
import org.ps5jb.loader.jar.JarLoader;
import org.ps5jb.loader.jar.RemoteJarLoader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.IOException;

public class MenuLoader extends HContainer implements Runnable, UserEventListener, JarLoader {
    private static String[] discPayloadList;
    private static String[] usbPayloadList;
    private static String[] pipelineList;
    private static File usbPayloadRoot;

    private boolean active = true;
    private boolean terminated = false;
    private boolean waiting = false;
    private int terminateRemoteJarLoaderPressCount;

    private Ps5MenuLoader ps5MenuLoader;

    private File discPayloadPath = null;
    private File pipelinePath = null;
    private JarLoader remoteJarLoaderJob = null;
    Thread remoteJarLoaderThread = null;

    private static int currentCategoryMode = 0;
    private static double firmwareValue = 0.00;
    private static String firmwareString = "0.00";
    private static boolean initialFwSyncDone = false;
    private static boolean firmwareDetectionAttempted = false;
    
    private boolean kernelActiveState = false;
    private int portProbeCounter = 0;
    private boolean executeLastPipelineFlag = false;

    public MenuLoader() throws IOException {
        detectFirmware();
        loadIconModeCache();
        
        ps5MenuLoader = initMenuLoader();
        if (firmwareString != null && !firmwareString.equals("0.00")) {
            ps5MenuLoader.updateFirmwareDisplay(firmwareString);
        }
        
        ps5MenuLoader.setSelected(1);
        ps5MenuLoader.setSubMenuActive(true);
        ps5MenuLoader.setSelectedSub(1);
    }

    public void detectFirmware() {
        if (firmwareDetectionAttempted) {
            return;
        }
        firmwareDetectionAttempted = true;

        try {
            Status.println("Searching for saved firmware data in cache");
            File cacheFile = new File("/OS/HDD/download0/xlet/cache/fw_cache.txt");
            
            if (!cacheFile.exists()) {
                Status.println("No firmware data was found in cache, looking for fwdetect.jar");
                File dir = Config.getLoaderPayloadPath();
                File targetDetectorJar = new File(dir, "22.fwdetect.jar");
                
                if (targetDetectorJar.exists()) {
                    try {
                        Status.println("Running fwdetect.jar");
                        this.loadJar(targetDetectorJar, false);
                        
                        Status.println("Waiting for firmware data file to generate.");
                        int retry = 0;
                        while (!cacheFile.exists() && retry < 30) {
                            Thread.sleep(100);
                            retry++;
                        }
                    } catch (Throwable e) {
                        Status.println("Failed to run fwdetect.jar: " + e.getMessage());
                    }
                } else {
                    Status.println("fwdetect.jar not found at: " + targetDetectorJar.getAbsolutePath());
                }
            }

            if (cacheFile.exists()) {
                Status.println("Firmware data found in cache, loading...");
                BufferedReader br = new BufferedReader(new FileReader(cacheFile));
                String cachedLine = br.readLine();
                br.close();
                
                if (cachedLine != null && cachedLine.trim().length() > 0) {
                    firmwareString = cachedLine.trim();
                    firmwareValue = Double.parseDouble(firmwareString);
                    
                    Status.println("Firmware data verified: " + firmwareString);
                    
                    if (ps5MenuLoader != null) {
                        ps5MenuLoader.updateFirmwareDisplay(firmwareString);
                    }
                    
                    if (!initialFwSyncDone) {
                        currentCategoryMode = (firmwareValue <= 7.61) ? 1 : 0;
                        initialFwSyncDone = true;
                    }
                    return;
                }
            } else {
                Status.println("Firmware detection timed out.");
            }
        } catch (Throwable t) {
            Status.println("Error reading firmware data file: " + t.getMessage());
        }

        firmwareValue = 0.00;
        firmwareString = "0.00";
        currentCategoryMode = 0;
    }
    public static void clearFirmwareCacheManually() {
        try {
            File cacheDir = new File("/OS/HDD/download0/xlet/cache");
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        files[i].delete();
                    }
                }
            }
            Status.println("Cache deleted");
        } catch (Throwable t) {
            Status.println("Error while deleting cache: " + t.getMessage());
        }
        firmwareValue = 0.00;
        firmwareString = "0.00";
        firmwareDetectionAttempted = false;
        initialFwSyncDone = false;
        Ps5MenuLoader.setActiveIconSetMode(0);
    }

    private void loadIconModeCache() {
        try {
            File iconCache = new File("/OS/HDD/download0/xlet/cache/icon_cache.txt");
            if (iconCache.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(iconCache));
                String line = br.readLine();
                br.close();
                if (line != null && line.trim().equals("1")) {
                    Ps5MenuLoader.setActiveIconSetMode(1);
                    return;
                }
            }
        } catch (Throwable t) {}
        Ps5MenuLoader.setActiveIconSetMode(0);
    }

    private void saveIconModeCache(int mode) {
        try {
            File cacheDir = new File("/OS/HDD/download0/xlet/cache");
            if (!cacheDir.exists()) { cacheDir.mkdirs(); }
            File iconCache = new File(cacheDir, "icon_cache.txt");
            FileOutputStream fos = new FileOutputStream(iconCache);
            fos.write(String.valueOf(mode).getBytes());
            fos.close();
        } catch (Throwable t) {}
    }

    public double getFirmwareValue() { return firmwareValue; }
    public String getFirmwareString() { return firmwareString; }
    public boolean isKernelActiveState() { return kernelActiveState; }
    public int getCategoryMode() { return currentCategoryMode; }

    @Override
    public void run() {
        EventManager em = EventManager.getInstance();
        UserEventRepository evRep = new OverallRepository();

        Status.println("MenuLoader starting...");
        for (String payload : listJarPayloads()) {
            Status.println("[Payload] " + payload);
        }

        em.addUserEventListener(this, evRep);
        try {
            while (!terminated) {
                if (!active) {
                    if (!waiting) {
                        if (discPayloadPath != null) {
                            em.removeUserEventListener(this);
                            try {
                                loadJar(discPayloadPath, false);
                            } catch (InterruptedException e) {
                                // Ignore
                            } catch (Throwable ex) {
                                Status.printStackTrace("Could not load JAR from disc", ex);
                            } finally {
                                em.addUserEventListener(this, evRep);
                            }
                            discPayloadPath = null;
                        } else if (remoteJarLoaderThread != null) {
                            try {
                                remoteJarLoaderThread.join();
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                            remoteJarLoaderThread = null;
                            remoteJarLoaderJob = null;
                        } else if (executeLastPipelineFlag) {
                            try {
                                String lastPipe = PipelineRunner.loadLastPipeline();
                                if (lastPipe != null && lastPipe.indexOf("Pipeline") == -1 && lastPipe.indexOf("[") == -1 && lastPipe.indexOf("pipe") != -1) {
                                    PipelineRunner.runPipeline(new File(lastPipe), this);
                                } else {
                                    Status.println("No previous pipeline data found in cache.");
                                }
                            } catch (Throwable t) {
                                Status.printStackTrace("Failed to launch last selected pipeline", t);
                            }
                            executeLastPipelineFlag = false;
                        } else if (pipelinePath != null) {
                            String pathCheck = pipelinePath.getName().toLowerCase();
                            if (pathCheck.indexOf("pipeline") == -1 && pathCheck.indexOf("[") == -1) {
                                PipelineRunner.runPipeline(pipelinePath, this);
                            }
                            pipelinePath = null;
                        }

                        reloadMenuLoader();

                        Status.println("Press X to return to the menu");
                        waiting = true;
                    } else {
                        Thread.yield();
                    }
                } else {
                    initRenderLoop();
                }
            }
        } catch (RuntimeException | Error | IOException ex) {
            Status.printStackTrace("Unhandled exception", ex);
            terminated = true;
        } finally {
            em.removeUserEventListener(this);
        }
    }
    /**
     * Returns a list of JAR files that are present on disc.
     *
     * @return Array of loadable JAR files or an empty list if there are none.
     */
    public static String[] listJarPayloads() {
        if (discPayloadList == null) {
            final File dir = Config.getLoaderPayloadPath();
            if (dir.isDirectory() && dir.canRead()) {
                discPayloadList = dir.list((dir1, name) -> name.toLowerCase().endsWith(".jar"));
            }

            if (discPayloadList == null) {
                discPayloadList = new String[0];
            }
        }
        return discPayloadList;
    }

    /**
     * Returns a list of ELF/BIN files that are present on usb.
     *
     * @return Array of sendable ELF files or an empty list if there are none.
     */
    public static String[] listElfPayloads() {
        for (int i = 0; i < 8; i++) {
            try {
                File f = new File("/mnt/usb" + i);
                if (f.exists() &&
                    f.list((dir1, name) -> 
                        name.toLowerCase().endsWith(".elf") ||
                        name.toLowerCase().endsWith(".bin") ||
                        name.toLowerCase().endsWith(".jar")
                    ).length > 0) {
                    Status.println("Found usb with payload(s) on " + f.getAbsolutePath());
                    usbPayloadRoot = f;
                    break;
                }
            } catch (Exception ex) {
                Status.println("Error searching for usb" + i);
            }
        }

        if (usbPayloadRoot != null && usbPayloadRoot.isDirectory() && usbPayloadRoot.canRead()) {
            usbPayloadList = usbPayloadRoot.list((dir1, name) -> 
                name.toLowerCase().endsWith(".elf") ||
                name.toLowerCase().endsWith(".bin") ||
                name.toLowerCase().endsWith(".jar"));
        } else {
            Status.println("No usb with payloads found");
            usbPayloadList = new String[0];
        }

        return usbPayloadList;
    }

    /**
     * Returns a list of Pipeline files that are present on /.
     *
     * @return Array of loadable JAR files or an empty list if there are none.
     */
    public static String[] listPipelines() {
        if (pipelineList == null) {
            final File dir = Config.getLoaderPayloadPath();
            if (dir.isDirectory() && dir.canRead()) {
                pipelineList = dir.list((dir1, name) -> name.toLowerCase().endsWith(".pipe"));
            }

            if (pipelineList == null) {
                pipelineList = new String[0];
            }
        }
        return pipelineList;
    }

    public String[] getFilteredPipelines(int mode) {
        String[] allPipes = listPipelines();
        int matchCount = 0;
        String filterString = (mode == 0) ? "poops" : "umtx";
        
        for (int i = 0; i < allPipes.length; i++) {
            if (allPipes[i].toLowerCase().indexOf(filterString) != -1) {
                matchCount++;
            }
        }

        String[] filtered = new String[matchCount + 1];
        filtered[0] = "[Run Last Selected Pipeline]";
        
        int index = 1;
        for (int i = 0; i < allPipes.length; i++) {
            if (allPipes[i].toLowerCase().indexOf(filterString) != -1) {
                filtered[index++] = allPipes[i];
            }
        }

        return filtered;
    }
    private void initUsbElfSender(Ps5MenuLoader ps5MenuLoader) throws IOException {
        final String[] elfPayloads = listElfPayloads();
        final Ps5MenuItem[] usbSubItems = new Ps5MenuItem[elfPayloads.length];
        for (int i = 0; i < elfPayloads.length; i++) {
            final String payload = elfPayloads[i];
            usbSubItems[i] = new Ps5MenuItem(payload, null);
        }
        ps5MenuLoader.setSubmenuItems(3, usbSubItems);
    }

    private Ps5MenuLoader initMenuLoader() throws IOException {
        int iconSetIndex = Ps5MenuLoader.getActiveIconSetMode();
        String suffix = (iconSetIndex == 1) ? "_2.png" : ".png";

        Ps5MenuLoader ps5MenuLoaderInstance = new Ps5MenuLoader(new Ps5MenuItem[]{
            new Ps5MenuItem("Pipeline runner", "pipeline_icon" + suffix),
            new Ps5MenuItem("Disc JAR loader", "disc_icon" + suffix),
            new Ps5MenuItem("USB ELF/BIN/JAR loader", "usb_icon" + suffix),
            new Ps5MenuItem("Remote JAR loader", "wifi_icon" + suffix)
        });

        final String[] jarPayloads = listJarPayloads();
        final Ps5MenuItem[] diskSubItems = new Ps5MenuItem[jarPayloads.length];
        for (int i = 0; i < jarPayloads.length; i++) {
            final String payload = jarPayloads[i];
            diskSubItems[i] = new Ps5MenuItem(payload, null);
        }
        ps5MenuLoaderInstance.setSubmenuItems(2, diskSubItems);

        initUsbElfSender(ps5MenuLoaderInstance);

        final String[] pipelines = getFilteredPipelines(currentCategoryMode);
        final Ps5MenuItem[] pipelinesSubItems = new Ps5MenuItem[pipelines.length];
        for (int i = 0; i < pipelines.length; i++) {
            final String payload = pipelines[i];
            pipelinesSubItems[i] = new Ps5MenuItem(payload, null);
        }
        ps5MenuLoaderInstance.setSubmenuItems(1, pipelinesSubItems);

        return ps5MenuLoaderInstance;
    }

    private void reloadMenuLoader() throws IOException {
        Ps5MenuLoader oldMenuLoader = ps5MenuLoader;

        discPayloadList = null;
        usbPayloadList = null;
        usbPayloadRoot = null;
        pipelineList = null;
        
        ps5MenuLoader = initMenuLoader();
        ps5MenuLoader.setSelected(1);
        ps5MenuLoader.setSubMenuActive(true);
        ps5MenuLoader.setSelectedSub(1);
        ps5MenuLoader.setSelected(oldMenuLoader.getSelected());
        ps5MenuLoader.setSelectedSub(oldMenuLoader.getSelectedSub());
        ps5MenuLoader.setSubMenuActive(oldMenuLoader.isSubMenuActive());
        
        ps5MenuLoader.cacheLastPipelineName();
        
        if (firmwareString != null && !firmwareString.equals("0.00")) {
            ps5MenuLoader.updateFirmwareDisplay(firmwareString);
        }
    }

    private void initRenderLoop() {
        setSize(Config.getLoaderResolutionWidth(), Config.getLoaderResolutionHeight());
        setBackground(Color.darkGray);
        setForeground(Color.lightGray);
        setVisible(true);

        HScene scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.add(this, BorderLayout.CENTER, 0);

        try {
            scene.validate();
            while (active) {
                scene.repaint();
                Thread.yield();
            }
        } finally {
            this.setVisible(false);
            scene.remove(this);
        }
    }
    @Override
    public void userEventReceived(UserEvent userEvent) {
        if (userEvent.getType() == HRcEvent.KEY_RELEASED) {
            boolean isTerminateRemoteJarLoaderSeq = false;
            if (!active) {
                isTerminateRemoteJarLoaderSeq =
                        ((userEvent.getCode() == HRcEvent.VK_3) && (terminateRemoteJarLoaderPressCount == 0)) ||
                                ((userEvent.getCode() == HRcEvent.VK_2) && (terminateRemoteJarLoaderPressCount == 1)) ||
                                ((userEvent.getCode() == HRcEvent.VK_1) && (terminateRemoteJarLoaderPressCount == 2));
                if (!isTerminateRemoteJarLoaderSeq) {
                    if (!waiting || (userEvent.getCode() != HRcEvent.VK_ENTER)) {
                        return;
                    }
                }
            }

            if (!isTerminateRemoteJarLoaderSeq && (terminateRemoteJarLoaderPressCount > 0) && remoteJarLoaderThread != null) {
                terminateRemoteJarLoaderPressCount = 0;
            }

            int code = userEvent.getCode();
            
            if (active && (code == HRcEvent.VK_9 || code == 57)) {
                clearFirmwareCacheManually();
                detectFirmware();
                return;
            }

            if (active && (code == HRcEvent.VK_7 || code == 55)) {
                int nextMode = (Ps5MenuLoader.getActiveIconSetMode() == 0) ? 1 : 0;
                if (nextMode == 1) {
                    boolean filesExist = (getClass().getResource("disc_icon_2.png") != null) &&
                                         (getClass().getResource("pipeline_icon_2.png") != null) &&
                                         (getClass().getResource("usb_icon_2.png") != null) &&
                                         (getClass().getResource("wifi_icon_2.png") != null);
                    if (filesExist) {
                        Ps5MenuLoader.setActiveIconSetMode(1);
                        saveIconModeCache(1);
                    } else {
                        Status.println("The 2nd category of icons is incomplete or missing.");
                        Ps5MenuLoader.setActiveIconSetMode(0);
                        saveIconModeCache(0);
                    }
                } else {
                    Ps5MenuLoader.setActiveIconSetMode(0);
                    saveIconModeCache(0);
                }
                try {
                    reloadMenuLoader();
                } catch (IOException e) {
                    Status.println("Failed to refresh icons.");
                }
                return;
            }

            if (active && (code == HRcEvent.VK_1 || code == 49 || code == HRcEvent.VK_8 || code == 56 || code == 424 || code == 425 || code == 412 || code == 417)) {
                boolean executeToggle = false;
                if (code == HRcEvent.VK_1 || code == 49) {
                    if (firmwareValue <= 7.61 && firmwareValue > 0.00) {
                        executeToggle = true;
                    }
                } else {
                    executeToggle = true;
                }
                if (executeToggle) {
                    currentCategoryMode = (currentCategoryMode == 0) ? 1 : 0;
                    try {
                        reloadMenuLoader();
                        ps5MenuLoader.setSelectedSub(1);
                    } catch (IOException e) {
                        Status.printStackTrace("Category refresh error", e);
                    }
                }
                return;
            }
            switch (userEvent.getCode()) {
                case HRcEvent.VK_3:
                case HRcEvent.VK_2:
                case HRcEvent.VK_1:
                    if (isTerminateRemoteJarLoaderSeq) {
                        if (terminateRemoteJarLoaderPressCount == 2) {
                            if (remoteJarLoaderJob != null) {
                                try {
                                    remoteJarLoaderJob.terminate();
                                    terminateRemoteJarLoaderPressCount = 0;
                                } catch (Throwable ex) {
                                    Status.printStackTrace("Remote JAR loader could not be terminated.", ex);
                                }
                            }
                        } else {
                            ++terminateRemoteJarLoaderPressCount;
                        }
                    }
                    break;
                case HRcEvent.VK_RIGHT:
                    if (ps5MenuLoader.getSelected() < ps5MenuLoader.getMenuItems().length) {
                        ps5MenuLoader.setSelected(ps5MenuLoader.getSelected() + 1);
                        ps5MenuLoader.setSelectedSub(1);
                    }
                    switch(ps5MenuLoader.getSelected()) {
                        case 1:
                        case 2:
                            ps5MenuLoader.setSubMenuActive(true);
                            break;
                        case 3:
                            ps5MenuLoader.setSubMenuActive(true);
                            try {
                                initUsbElfSender(ps5MenuLoader);
                            } catch (IOException e) {
                                Status.printStackTrace("Error initUsbElfSender()", e);
                            }
                            break;
                        case 4:
                            ps5MenuLoader.setSubMenuActive(false);
                    }
                    break;

                case HRcEvent.VK_LEFT:
                    if (ps5MenuLoader.getSelected() > 1) {
                        ps5MenuLoader.setSelected(ps5MenuLoader.getSelected() - 1);
                        ps5MenuLoader.setSelectedSub(1);
                    }
                    switch(ps5MenuLoader.getSelected()) {
                        case 1:
                        case 2:
                            ps5MenuLoader.setSubMenuActive(true);
                            break;
                        case 3:
                            ps5MenuLoader.setSubMenuActive(true);
                            try {
                                initUsbElfSender(ps5MenuLoader);
                            } catch (IOException e) {
                                Status.printStackTrace("Error initUsbElfSender()", e);
                            }
                            break;
                        case 4:
                            ps5MenuLoader.setSubMenuActive(false);
                    }
                    break;

                case HRcEvent.VK_DOWN:
                    if (ps5MenuLoader.isSubMenuActive() && ((Ps5MenuItem[]) ps5MenuLoader.getSubmenuItems(ps5MenuLoader.getSelected())).length > ps5MenuLoader.getSelectedSub()) {
                        ps5MenuLoader.setSelectedSub(ps5MenuLoader.getSelectedSub() + 1);
                    }
                    break;

                case HRcEvent.VK_UP:
                    if (ps5MenuLoader.isSubMenuActive() && ps5MenuLoader.getSelectedSub() > 1) {
                        ps5MenuLoader.setSelectedSub(ps5MenuLoader.getSelectedSub() - 1);
                    }
                    break;
                case HRcEvent.VK_ENTER: // X button
                    if (waiting) {
                        active = true;
                        waiting = false;
                    } else if (ps5MenuLoader.getSelected() == 1) {
                        String[] activePipes = getFilteredPipelines(currentCategoryMode);
                        if (activePipes.length > 0) {
                             int selectedIdx = ps5MenuLoader.getSelectedSub() - 1;
                             if (selectedIdx == 0) {
                                 executeLastPipelineFlag = true;
                                 active = false;
                             } else {
                                 Ps5MenuItem selectedItem = ((Ps5MenuItem[]) ps5MenuLoader.getSubmenuItems(ps5MenuLoader.getSelected()))[selectedIdx];
                                 pipelinePath = new File(Config.getLoaderPayloadPath(), selectedItem.getLabel());
                                 PipelineRunner.saveLastPipeline(pipelinePath.getAbsolutePath());
                                 active = false;
                             }
                        }
                    } else if (ps5MenuLoader.getSelected() == 2) {
                        Ps5MenuItem selectedItem = ((Ps5MenuItem[]) ps5MenuLoader.getSubmenuItems(ps5MenuLoader.getSelected()))[ps5MenuLoader.getSelectedSub() - 1];
                        discPayloadPath = new File(Config.getLoaderPayloadPath(), selectedItem.getLabel());
                        active = false;
                    } else if (ps5MenuLoader.getSelected() == 3) {
                        if (usbPayloadList.length > 0) {
                            Ps5MenuItem selectedItem = ((Ps5MenuItem[]) ps5MenuLoader.getSubmenuItems(ps5MenuLoader.getSelected()))[ps5MenuLoader.getSelectedSub() - 1];
                            File payloadToLoad = new File(usbPayloadRoot, selectedItem.getLabel());

                            if (payloadToLoad.getName().toLowerCase().endsWith(".jar")) {
                                try {
                                    loadJar(payloadToLoad, false);
                                } catch (Throwable ex) {
                                    Status.printStackTrace("Could not load JAR from USB", ex);
                                }
                            } else {
                                PayloadSender.sendPayloadFromFile(payloadToLoad);
                            }
                            active = false;
                        }
                    } else if (ps5MenuLoader.getSelected() == 4 && remoteJarLoaderThread == null) {
                         try {
                             remoteJarLoaderJob = new RemoteJarLoader(Config.getLoaderPort());
                             remoteJarLoaderThread = new Thread(remoteJarLoaderJob, "RemoteJarLoader");
                             Status.println("Starting remote JAR loader. To return to the loader menu, press 3-2-1");
                             remoteJarLoaderThread.start();
                         } catch (Throwable ex) {
                             Status.printStackTrace("Remote JAR loader could not be initialized. Press X to continue", ex);
                             waiting = true;
                         }
                         active = false;
                     }
                     break;
            }
        }
    }

    @Override
    public void paint(Graphics graphics) {
        if (graphics == null) return;
        if (firmwareString.equals("0.00") && !firmwareDetectionAttempted) {
            detectFirmware();
            if (!firmwareString.equals("0.00")) {
                try {
                    ps5MenuLoader.updateFirmwareDisplay(firmwareString);
                } catch (Throwable t) {}
            }
        }
        
        portProbeCounter++;
        if (portProbeCounter >= 30) {
            portProbeCounter = 0;
            java.net.Socket s = null;
            try {
                s = new java.net.Socket();
                s.connect(new java.net.InetSocketAddress("127.0.0.1", 9021), 100);
                
                boolean hasUmtxState = false;
                try {
                    Class rwClass = Class.forName("org.ps5jb.loader.KernelReadWrite");
                    java.lang.reflect.Method checkMethod = rwClass.getMethod("hasAccessorState", new Class[]{});
                    Boolean res = (Boolean) checkMethod.invoke(null, new Object[]{});
                    hasUmtxState = res.booleanValue();
                } catch (Throwable t) {}

                if (!hasUmtxState) {
                    this.kernelActiveState = true;
                }
            } catch (Exception e) {
                this.kernelActiveState = false;
            } finally {
                if (s != null) { try { s.close(); } catch (Exception ex) {} }
            }
        }

        if (active) {
            ps5MenuLoader.renderMenu(graphics, this);
        }
        super.paint(graphics);
    }

    @Override
    public void terminate() throws IOException {
        this.active = false;
        this.terminated = true;
    }
}
