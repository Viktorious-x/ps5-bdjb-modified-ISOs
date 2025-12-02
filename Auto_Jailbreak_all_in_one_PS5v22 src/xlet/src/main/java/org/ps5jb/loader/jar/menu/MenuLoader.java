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
import java.io.IOException;
import java.util.ResourceBundle;


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
    private Thread remoteJarLoaderThread = null;

    public MenuLoader() throws IOException {
        ps5MenuLoader = initMenuLoader();
        ps5MenuLoader.setSelected(1);
        ps5MenuLoader.setSubMenuActive(true);
        ps5MenuLoader.setSelectedSub(1);
    }

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
                                // JAR execution didn't work, notify and wait to return to the menu
                                Status.printStackTrace("Could not load JAR from disc", ex);
                            } finally {
                                em.addUserEventListener(this, evRep);
                            }
                            discPayloadPath = null;
                        } else if (remoteJarLoaderThread != null) {
                            try {
                                // Wait on remote JAR loader to finish
                                remoteJarLoaderThread.join();
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                            remoteJarLoaderThread = null;
                            remoteJarLoaderJob = null;
                        } else if (pipelinePath != null) {
                            PipelineRunner.runPipeline(pipelinePath, this);
                            pipelinePath = null;
                        }

                        // Reload the menu in case paths to payloads changed after JAR execution
                        reloadMenuLoader();

                        // Wait for user input before returning
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
    // search for usb0 - usb7
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

  private Ps5MenuLoader initMenuLoader() throws IOException {
    Ps5MenuLoader ps5MenuLoader = new Ps5MenuLoader(new Ps5MenuItem[]{
    new Ps5MenuItem("Pipeline runner", "pipeline_icon.png"),
    new Ps5MenuItem("Disc JAR loader", "disc_icon.png"),
    new Ps5MenuItem("USB ELF/BIN/JAR loader", "usb_icon.png"),
    new Ps5MenuItem("Remote JAR loader", "wifi_icon.png")
});

    // init disk jar loader sub items
    final String[] jarPayloads = listJarPayloads();
    final Ps5MenuItem[] diskSubItems = new Ps5MenuItem[jarPayloads.length];
    for (int i = 0; i < jarPayloads.length; i++) {
        final String payload = jarPayloads[i];
        diskSubItems[i] = new Ps5MenuItem(payload, null);
    }
    ps5MenuLoader.setSubmenuItems(2, diskSubItems);

    initUsbElfSender(ps5MenuLoader);

    final String[] pipelines = listPipelines();
    final Ps5MenuItem[] pipelinesSubItems = new Ps5MenuItem[pipelines.length];
    for (int i = 0; i < pipelines.length; i++) {
        final String payload = pipelines[i];
        pipelinesSubItems[i] = new Ps5MenuItem(payload, null);
    }
    ps5MenuLoader.setSubmenuItems(1, pipelinesSubItems);

    return ps5MenuLoader;
}



    private void reloadMenuLoader() throws IOException {
        Ps5MenuLoader oldMenuLoader = ps5MenuLoader;

        discPayloadList = null;
        usbPayloadList = null;
        usbPayloadRoot = null;
        ps5MenuLoader = initMenuLoader();
        ps5MenuLoader.setSelected(1);
        ps5MenuLoader.setSubMenuActive(true);
        ps5MenuLoader.setSelectedSub(1);
        ps5MenuLoader.setSelected(oldMenuLoader.getSelected());
        ps5MenuLoader.setSelectedSub(oldMenuLoader.getSelectedSub());
        ps5MenuLoader.setSubMenuActive(oldMenuLoader.isSubMenuActive());
    }

    private void initUsbElfSender(Ps5MenuLoader ps5MenuLoader) throws IOException {
        // init usb elf sender sub items
        final String[] elfPayloads = listElfPayloads();
        final Ps5MenuItem[] usbSubItems = new Ps5MenuItem[elfPayloads.length];
        for (int i = 0; i < elfPayloads.length; i++) {
            final String payload = elfPayloads[i];
            usbSubItems[i] = new Ps5MenuItem(payload, null);
        }
        ps5MenuLoader.setSubmenuItems(3, usbSubItems);
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
            // Exit early if running a payload and not waiting for specific user input
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

            // Reset sequence to exit Remote JAR loader
            if (!isTerminateRemoteJarLoaderSeq && (terminateRemoteJarLoaderPressCount > 0) && remoteJarLoaderThread != null) {
                terminateRemoteJarLoaderPressCount = 0;
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
                    }
                    switch(ps5MenuLoader.getSelected()) {
                        case 1:
                            ps5MenuLoader.setSubMenuActive(true);
                            break;
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
                    }
                    switch(ps5MenuLoader.getSelected()) {
                        case 1:
                            ps5MenuLoader.setSubMenuActive(true);
                            break;
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
                    if (ps5MenuLoader.isSubMenuActive() && ps5MenuLoader.getSelectedSub() < ps5MenuLoader.getSubmenuItems(ps5MenuLoader.getSelected()).length) {
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
                        if (pipelineList.length > 0) {
                             Ps5MenuItem selectedItem = ps5MenuLoader.getSubmenuItems(ps5MenuLoader.getSelected())[ps5MenuLoader.getSelectedSub() - 1];
                             pipelinePath = new File(Config.getLoaderPayloadPath(), selectedItem.getLabel());
                             active = false;
                        }
                    } else if (ps5MenuLoader.getSelected() == 2) {
                        Ps5MenuItem selectedItem = ps5MenuLoader.getSubmenuItems(ps5MenuLoader.getSelected())[ps5MenuLoader.getSelectedSub() - 1];
                        discPayloadPath = new File(Config.getLoaderPayloadPath(), selectedItem.getLabel());
                        active = false;
						}
                    else if (ps5MenuLoader.getSelected() == 3) {
    if (usbPayloadList.length > 0) {
        Ps5MenuItem selectedItem = ps5MenuLoader.getSubmenuItems(ps5MenuLoader.getSelected())[ps5MenuLoader.getSelectedSub() - 1];
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
 
                             // Notify the user that this is a one time switch and that BD-J restart is required to return to the menu
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
        if (active) {
            ps5MenuLoader.renderMenu(graphics);
        }

        super.paint(graphics);
    }

    @Override
    public void terminate() throws IOException {
        this.active = false;
        this.terminated = true;
    }
}
