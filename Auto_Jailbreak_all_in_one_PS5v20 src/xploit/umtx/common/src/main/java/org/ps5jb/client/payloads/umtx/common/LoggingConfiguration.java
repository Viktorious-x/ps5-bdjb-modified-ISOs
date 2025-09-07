package org.ps5jb.client.payloads.umtx.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import org.dvb.event.EventManager;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.havi.ui.HContainer;
import org.havi.ui.HOrientable;
import org.havi.ui.HRangeValue;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSinglelineEntry;
import org.havi.ui.HState;
import org.havi.ui.HStaticText;
import org.havi.ui.event.HAdjustmentEvent;
import org.havi.ui.event.HAdjustmentListener;
import org.havi.ui.event.HKeyListener;
import org.havi.ui.event.HRcEvent;
import org.havi.ui.event.HTextEvent;
import org.havi.ui.event.HTextListener;
import org.ps5jb.loader.Config;
import org.ps5jb.loader.Status;

/**
 * Component which renders UI to configure logging parameters
 */
public class LoggingConfiguration extends HContainer
        implements UserEventListener, HTextListener, HAdjustmentListener, HKeyListener {

    private static final String EMPTY_IP = "0.0.0.0";

    protected String loggerHost;
    protected int loggerPort;
    protected DebugStatus.Level debugLevel;

    protected boolean aborted;
    protected boolean canceled;
    protected boolean accepted;

    protected LoggingConfiguration() {
        loggerHost = Config.getLoggerHost();
        if ("".equals(loggerHost) || loggerHost == null) {
            loggerHost = EMPTY_IP;
        }
        loggerPort = Config.getLoggerPort();
        debugLevel = DebugStatus.level;
        canceled = false;
        accepted = false;
        aborted = false;
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public boolean isFocusTraversable() {
        return true;
    }

    @Override
    public void paint(Graphics graphics) {
        if (isShowing()) {
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
        }
        super.paint(graphics);
    }

    @Override
    public void userEventReceived(UserEvent userEvent) {
        if (userEvent.getType() == HRcEvent.KEY_RELEASED) {
            switch (userEvent.getCode()) {
                case HRcEvent.VK_COLORED_KEY_0:
                    aborted = true;
                    break;
                case HRcEvent.VK_COLORED_KEY_1:
                    accepted = true;
                    break;
                case HRcEvent.VK_COLORED_KEY_3:
                    canceled = true;
                    break;
                case KeyEvent.VK_0:
                    debugLevel = DebugStatus.Level.ERROR;
                    setStaticText("level", debugLevel.toString());
                    break;
                case KeyEvent.VK_1:
                    debugLevel = DebugStatus.Level.INFO;
                    setStaticText("level", debugLevel.toString());
                    break;
                case KeyEvent.VK_2:
                    debugLevel = DebugStatus.Level.NOTICE;
                    setStaticText("level", debugLevel.toString());
                    break;
                case KeyEvent.VK_3:
                    debugLevel = DebugStatus.Level.DEBUG;
                    setStaticText("level", debugLevel.toString());
                    break;
                case KeyEvent.VK_4:
                    debugLevel = DebugStatus.Level.TRACE;
                    setStaticText("level", debugLevel.toString());
                    break;
                case KeyEvent.VK_DOWN:
                    if (!(userEvent.getSource() instanceof Component)) {
                        getComponent("host").requestFocus();
                    }
                    break;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
        // Do nothing
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getSource() instanceof Component) {
            Component source = (Component) keyEvent.getSource();
            if ("host".equals(source.getName())) {
                HSinglelineEntry textCtrl = (HSinglelineEntry) source;
                if (keyEvent.getKeyCode() == KeyEvent.VK_UP) {
                    incrementInt(textCtrl, 1, 0, 255);
                } else if (keyEvent.getKeyCode() == KeyEvent.VK_DOWN) {
                    incrementInt(textCtrl, -1, 0, 255);
                } else if (keyEvent.getKeyCode() == 461) {
                    // Square button deletes previous char
                    int caretPos = textCtrl.getCaretCharPosition();
                    String ip = textCtrl.getTextContent(HState.NORMAL_STATE);
                    if ((caretPos > 0 && ip.charAt(caretPos - 1) == '.') ||
                            (caretPos > 1 && ip.charAt(caretPos - 2) == '.') ||
                            (caretPos == 1)) {
                        // Do nothing
                    } else {
                        textCtrl.deletePreviousChar();
                    }
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        // Do nothing
    }

    protected void incrementInt(HSinglelineEntry control, int delta, int min, int max) {
        String ip = control.getTextContent(HState.NORMAL_STATE);
        int caretPos = control.getCaretCharPosition();
        int prevDot = 0;
        int nextDot = ip.indexOf('.');
        while (nextDot != -1 && nextDot < caretPos) {
            prevDot = nextDot;
            nextDot = ip.indexOf('.', nextDot + 1);
        }
        if (nextDot == -1) {
            nextDot = ip.length();
        }

        String ipComponent = ip.substring(prevDot == 0 ? 0 : prevDot + 1, nextDot);
        int componentVal;
        try {
            componentVal = Integer.parseInt(ipComponent) + delta;
        } catch (NumberFormatException e) {
            componentVal = min;
        }

        if (componentVal >= min && componentVal <= max) {
            String newIp = ip.substring(0, prevDot == 0 ? 0 : prevDot + 1) + componentVal + ip.substring(nextDot);
            control.setTextContent(newIp, HState.NORMAL_STATE);
            control.setCaretCharPosition(caretPos);
        }
    }

    protected Component getComponent(String name) {
        Component result = null;
        for (int i = 0; i < getComponentCount(); ++i) {
            Component comp = getComponent(i);
            if (name.equals(comp.getName())) {
                result = comp;
                break;
            }
        }
        return result;
    }

    @Override
    public void textChanged(HTextEvent textEvent) {
        if (textEvent.getID() == HTextEvent.TEXT_CHANGE || textEvent.getID() == HTextEvent.TEXT_END_CHANGE) {
            Component component = (Component) textEvent.getSource();
            if (component.getName().equals("host")) {
                HSinglelineEntry source = (HSinglelineEntry) component;
                loggerHost = source.getTextContent(HState.NORMAL_STATE);
            }
        }
    }

    @Override
    public void valueChanged(HAdjustmentEvent changeEvent) {
        Component component = (Component) changeEvent.getSource();
        if (component.getName().equals("port")) {
            HRangeValue rangeValue = (HRangeValue) component;
            loggerPort = rangeValue.getValue();
            setStaticText("portval", Integer.toString(loggerPort));
        }
    }

    protected void setStaticText(String componentName, String value) {
        Component comp = getComponent(componentName);
        HStaticText textComp = (HStaticText) comp;
        Font font = textComp.getFont();
        if (font == null) {
            font = getFont();
        }
        FontMetrics fm = getFontMetrics(font);
        textComp.setTextContent(value, HState.NORMAL_STATE);
        textComp.setSize(fm.stringWidth(value), textComp.getHeight());
    }

    @Override
    public void caretMoved(HTextEvent textEvent) {
        // Unused
    }

    /**
     * Renders the component and blocks execution until RED button is pressed in the menu.
     *
     * @return True if user decided to proceed with the rest of execution. False to abort.
     */
    public boolean render() {
        EventManager.getInstance().addUserEventListener(this, new OverallRepository());
        try {
            HScene scene = HSceneFactory.getInstance().getDefaultHScene();
            scene.add(this, BorderLayout.CENTER, 0);
            try {
                scene.validate();

                while (!this.canceled && !this.accepted && !this.aborted) {
                    scene.repaint();
                    Thread.yield();
                }
            } finally {
                this.setVisible(false);
                scene.remove(this);
            }

            // Apply changes
            if (accepted) {
                applySelection();
            } else if (canceled) {
                DebugStatus.info("Logging configuration changes canceled");
            }
        } finally {
            EventManager.getInstance().removeUserEventListener(this);
        }

        return !aborted;
    }

    /**
     * Once rendering is done, apply selected changes.
     */
    protected void applySelection() {
        DebugStatus.level = debugLevel;
        String host = loggerHost == null ? null : loggerHost.trim();
        if ("".equals(host) || EMPTY_IP.equals(host)) {
            host = null;
        }
        Status.resetLogger(host, loggerPort, Config.getLoggerTimeout());

        DebugStatus.info("The logging level is changed to: " + DebugStatus.level);
        if (host == null) {
            DebugStatus.info("The remote logging is disabled");
        } else {
            DebugStatus.info("The remote logging server is changed to: " + host + ":" + loggerPort +
                    ". Capture messages with `socat udp-recv:" + loggerPort + " stdout`");
        }
    }

    /**
     * Constructs an instance of logging configuration component
     * which can be added to the scene for rendering.
     *
     * @return New logging configuration component instance.
     */
    public static LoggingConfiguration createComponent() {
        LoggingConfiguration loggingComponent = new LoggingConfiguration();
        loggingComponent.setSize(Config.getLoaderResolutionWidth(), Config.getLoaderResolutionHeight());
        loggingComponent.setFont(new Font(null, Font.PLAIN, 18));
        loggingComponent.setBackground(Color.lightGray);
        loggingComponent.setForeground(Color.black);
        loggingComponent.setVisible(true);

        final Font font = loggingComponent.getFont();
        final FontMetrics fm = loggingComponent.getFontMetrics(font);
        final Font valueFont = new Font(null, Font.BOLD, 18);
        final FontMetrics vfm = loggingComponent.getFontMetrics(valueFont);
        final Font hintFont = new Font(null, Font.ITALIC, 14);
        final FontMetrics hfm = loggingComponent.getFontMetrics(hintFont);

        final int horizonalLabelSpace = 5;
        final int horizonalControlSpace = 20;
        final int verticalLabelSpace = 1;
        final int verticalControlSpace = 20;
        final int labelHeight = fm.getHeight();
        final int controlHeight = 50;

        String text1 = "Logging level (screen and remote):";
        HStaticText text1ctrl = new HStaticText(text1, 80, 80, fm.stringWidth(text1), labelHeight);
        text1ctrl.setForeground(Color.black);
        text1ctrl.setBordersEnabled(false);
        loggingComponent.add(text1ctrl);

        String text1val = loggingComponent.debugLevel.toString();
        HStaticText text1valCtrl = new HStaticText(text1val, text1ctrl.getX() + text1ctrl.getWidth() + horizonalLabelSpace, text1ctrl.getY(), vfm.stringWidth(text1val), labelHeight);
        text1valCtrl.setForeground(Color.red);
        text1valCtrl.setBordersEnabled(false);
        text1valCtrl.setFont(valueFont);
        text1valCtrl.setName("level");
        loggingComponent.add(text1valCtrl);

        String text2 = "Press Triangle, then 0 [ERROR] or 1 [INFO] or 2 [NOTICE] or 3 [DEBUG] or 4 [TRACE]";
        HStaticText text2ctrl = new HStaticText(text2, text1ctrl.getX(), text1ctrl.getY() + text1ctrl.getHeight() + verticalLabelSpace, hfm.stringWidth(text2), labelHeight);
        text2ctrl.setForeground(Color.darkGray);
        text2ctrl.setBordersEnabled(false);
        text2ctrl.setFont(hintFont);
        loggingComponent.add(text2ctrl);

        String hostLabel = "Logging host:";
        HStaticText hostLabelCtrl = new HStaticText(hostLabel, text1ctrl.getX(), text2ctrl.getY() + text2ctrl.getHeight() + verticalControlSpace, fm.stringWidth(hostLabel), labelHeight);
        hostLabelCtrl.setForeground(Color.black);
        hostLabelCtrl.setBordersEnabled(false);
        loggingComponent.add(hostLabelCtrl);

        HSinglelineEntry hostEntry = new HSinglelineEntry(loggingComponent.loggerHost, text1ctrl.getX(), hostLabelCtrl.getY() + hostLabelCtrl.getHeight() + verticalLabelSpace, 200, controlHeight, 15, valueFont, Color.lightGray);
        hostEntry.addHTextListener(loggingComponent);
        hostEntry.addHKeyListener(loggingComponent);
        hostEntry.setForeground(Color.black);
        hostEntry.setName("host");
        loggingComponent.add(hostEntry);

        String portLabel = "Logging port:";
        HStaticText portLabelCtrl = new HStaticText(portLabel, hostEntry.getX() + hostEntry.getWidth() + horizonalControlSpace, hostLabelCtrl.getY(), fm.stringWidth(portLabel), labelHeight);
        portLabelCtrl.setForeground(Color.black);
        portLabelCtrl.setBordersEnabled(false);
        loggingComponent.add(portLabelCtrl);

        String portVal = Integer.toString(loggingComponent.loggerPort);
        HStaticText portValCtrl = new HStaticText(portVal, portLabelCtrl.getX() + portLabelCtrl.getWidth() + horizonalLabelSpace, portLabelCtrl.getY(), vfm.stringWidth(portVal), labelHeight);
        portValCtrl.setForeground(Color.red);
        portValCtrl.setBordersEnabled(false);
        portValCtrl.setFont(valueFont);
        portValCtrl.setName("portval");
        loggingComponent.add(portValCtrl);

        HRangeValue portEntry = new HRangeValue(HOrientable.ORIENT_LEFT_TO_RIGHT, 0, 65535, loggingComponent.loggerPort, portLabelCtrl.getX(), portLabelCtrl.getY() + portLabelCtrl.getHeight() + verticalLabelSpace, 200, controlHeight);
        portEntry.addAdjustmentListener(loggingComponent);
        portEntry.setName("port");
        portEntry.setBlockIncrement(20);
        loggingComponent.add(portEntry);

        String text3 = "To disable remote logging, specify 0.0.0.0. Use Up/Down to change the IP components";
        HStaticText text3ctrl = new HStaticText(text3, text1ctrl.getX(), hostEntry.getY() + hostEntry.getHeight() + verticalLabelSpace, hfm.stringWidth(text3), labelHeight);
        text3ctrl.setForeground(Color.darkGray);
        text3ctrl.setBordersEnabled(false);
        text3ctrl.setFont(hintFont);
        loggingComponent.add(text3ctrl);

        String conclusionLabel = "Green Square - apply and proceed. Yellow Square - revert and proceed. Red Square - abort";
        HStaticText conclusionCtrl = new HStaticText(conclusionLabel, text1ctrl.getX(), text3ctrl.getY() + text3ctrl.getHeight() + verticalControlSpace, fm.stringWidth(conclusionLabel), labelHeight);
        conclusionCtrl.setForeground(Color.black);
        conclusionCtrl.setBordersEnabled(false);
        loggingComponent.add(conclusionCtrl);

        // Set how to switch focus between inputs
        hostEntry.setMove(KeyEvent.VK_RIGHT, portEntry);
        portEntry.setMove(KeyEvent.VK_LEFT, hostEntry);

        return loggingComponent;
    }
}
