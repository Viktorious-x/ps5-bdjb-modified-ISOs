package org.ps5jb.loader;

import java.awt.BorderLayout;
import java.io.IOException;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.ps5jb.loader.jar.JarLoader;
import org.ps5jb.loader.jar.RemoteJarLoader;
import org.ps5jb.loader.jar.menu.MenuLoader;
import org.bdj.sandbox.Exploit;
import org.bdj.sandbox.ExploitInternal;

public class LoaderXlet implements Xlet {
    private HScene scene;
    private JarLoader jarLoader;
    private Thread jarLoaderThread;

    public LoaderXlet() {
        super();
    }

    @Override
    public void initXlet(XletContext context) {
        Screen.getInstance().setSize(Config.getLoaderResolutionWidth(), Config.getLoaderResolutionHeight());
        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.add(Screen.getInstance(), BorderLayout.CENTER);
        scene.validate();

        try {
            Status.println("Trying DisableSecurityManagerAction...");
            DisableSecurityManagerAction.execute();
            Status.println("DisableSecurityManagerAction finished");
        } catch (Throwable e) {
            Status.printStackTrace("DisableSecurityManagerAction failed", e);
        }

        if (System.getSecurityManager() != null) {
            try {
                Status.println("Trying Exploit...");
                boolean result = Exploit.disableSecurityManager();
                Status.println("Exploit returned: " + result);
            } catch (Throwable e) {
                Status.printStackTrace("Exploit failed", e);
            }
        }

        if (System.getSecurityManager() != null) {
            try {
                Status.println("Trying ExploitInternal...");
                boolean result = ExploitInternal.disableSecurityManager();
                Status.println("ExploitInternal returned: " + result);
            } catch (Throwable e) {
                Status.printStackTrace("ExploitInternal failed", e);
            }
        }

        if (System.getSecurityManager() != null) {
            try {
                Status.println("Trying Exploit1342...");
                boolean result = org.bdj.sandbox.Exploit1342.disableSecurityManager();
                Status.println("Exploit1342 returned: " + result);
            } catch (Throwable e) {
                Status.printStackTrace("Exploit1342 failed", e);
            }
        }

        if (System.getSecurityManager() == null) {
            Status.println("Security Manager disabled");
        } else {
            Status.println("Security Manager still active: " + System.getSecurityManager());
        }
    }

    @Override
    public void startXlet() {
        Screen.getInstance().setVisible(true);
        scene.setVisible(true);

        try {
            if (System.getSecurityManager() == null) {
                final String jarLoaderThreadName;
                if (MenuLoader.listJarPayloads().length > 0) {
                    jarLoader = new MenuLoader();
                    jarLoaderThreadName = "MenuLoader";
                } else {
                    jarLoader = new RemoteJarLoader(Config.getLoaderPort());
                    jarLoaderThreadName = "JarLoader";
                }
                jarLoaderThread = new Thread(jarLoader, jarLoaderThreadName);
                jarLoaderThread.start();
            }
        } catch (Throwable e) {
            Status.printStackTrace("Loader startup failed", e);
        }
    }

    @Override
    public void pauseXlet() {
        Screen.getInstance().setVisible(false);
    }

    @Override
    public void destroyXlet(boolean unconditional) {
        try {
            if (jarLoader != null && jarLoaderThread != null) {
                jarLoader.terminate();
                jarLoaderThread.join(6000);
            }
        } catch (Throwable e) {
            Status.printStackTrace("Jar loader thread could not be terminated", e);
        }

        try {
            Status.close();
        } catch (Throwable e) {
            Screen.getInstance().printStackTrace(e);
        }

        scene.remove(Screen.getInstance());
        scene = null;
    }

    public static String getXletImplementationVersion() throws IOException {
        return ManifestUtils.getClassImplementationVersion(LoaderXlet.class, "xlet");
    }
}
