package org.ps5jb.client.payloads;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.dvb.event.EventManager;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.havi.ui.HContainer;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.event.HRcEvent;
import org.ps5jb.loader.Config;
import org.ps5jb.loader.Status;

/**
 * Implementation of a Mini-Tennis game on PS5. Originally from:
 * <a href="http://www.edu4java.com/en/game/game0-en.html">Edu4Java</a>.
 */
public class MiniTennisGame extends HContainer implements Runnable, UserEventListener {
    private static final int RACQUET_WIDTH = 150;
    private static final int RACQUET_HEIGHT = 10;
    private static final int RAQUET_SPEED = 10;

    private static final int BALL_DIAMETER = 30;
    private static int BALL_SPEED = 15;

    /** Ball x position */
    private int x = 0;
    /** Ball y position */
    private int y = 0;
    /** Ball horizontal shift per frame */
    int xa = BALL_DIAMETER / BALL_SPEED;
    /** Ball vertical shift per frame */
    int ya = BALL_DIAMETER / BALL_SPEED;

    /** Raquet x position */
    int rx = 0;
    /** Raquet horizontal shift per frame */
    int rxa = 0;
    /** Raquet y position */
    int ry = 0;

    /** Indicates whether to terminate the game */
    private boolean terminated = false;

    /** Indicates that the ball did not hit the raquet and game is over */
    private boolean isGameOver = false;

    /** Start time of the current game round. */
    private long startTime;

    /** All the rendering happens into this image off-screen. Then the main rendering loop just shows this image. */
    private BufferedImage offscreenBuffer;
    /** Graphics object associated with the off-screen buffer. */
    private Graphics2D offscreenGraphics;
    /** Scale of the off-screen buffer. The final image is scaled to 1 to simulate anti-aliasing. */
    private int offscreenScale = 3;

    /** Game entry point. */
    @Override
    public void run() {
        EventManager.getInstance().addUserEventListener(this, new OverallRepository());
        try {
            setSize(Config.getLoaderResolutionWidth(), Config.getLoaderResolutionHeight());
            setBackground(Color.darkGray);
            setForeground(Color.lightGray);
            setVisible(true);

            HScene scene = HSceneFactory.getInstance().getDefaultHScene();
            scene.add(this, BorderLayout.CENTER, 0);
            try {
                scene.validate();

                Graphics2D g2d = (Graphics2D) getGraphics();
                offscreenBuffer = g2d.getDeviceConfiguration().createCompatibleImage(offscreenScale * getWidth(), offscreenScale * getHeight());
                offscreenGraphics = offscreenBuffer.createGraphics();
                try {
                    startTime = System.currentTimeMillis();
                    while (!terminated) {
                        moveBall();
                        moveRacquet();
                        repaint();

                        try {
                            Thread.sleep(isGameOver ? 5000L : 10);
                        } catch (InterruptedException e) {
                            Status.printStackTrace(e.getMessage(), e);
                            terminated = true;
                        }

                        if (isGameOver) {
                            x = 0;
                            y = 0;
                            xa = BALL_DIAMETER / BALL_SPEED;
                            ya = BALL_DIAMETER / BALL_SPEED;
                            rx = 0;
                            rxa = 0;
                            ry = 0;
                            isGameOver = false;
                            startTime = System.currentTimeMillis();
                            BALL_SPEED = 15;
                        } else {
                            BALL_SPEED = Math.max(5, 15 - (int) ((System.currentTimeMillis() - startTime) / 1000 / 10));
                        }
                    }
                } finally {
                    setVisible(false);
                    scene.remove(this);
                }
            } finally {
                if (offscreenGraphics != null) {
                    offscreenGraphics.dispose();
                }
            }
        } finally {
            EventManager.getInstance().removeUserEventListener(this);
        }

        Status.println("Mini-Tennis Terminated");
    }

    /**
     * Paint a scaled image of this container using the given Graphics object.
     *
     * @param g Graphics to use for paining.
     * @param scale Scale at which to paint.
     */
    private synchronized void paintTo(Graphics g, int scale) {
        g.setColor(getBackground());
        g.fillRect(0, 0, scale * getWidth(), scale * getHeight());

        g.setColor(getForeground());
        g.fillOval(scale * x, scale * y, scale * BALL_DIAMETER, scale * BALL_DIAMETER);
        g.fillRect(scale * rx, scale * ry, scale * RACQUET_WIDTH, scale * RACQUET_HEIGHT);

        if (isGameOver) {
            g.setColor(Color.red);
            g.setFont(new Font(null, Font.BOLD, scale * 25));

            String text = "Game over, restarting in 5 seconds unless RED button is pressed...";
            int height = g.getFontMetrics().getHeight();
            int width = g.getFontMetrics().stringWidth(text);
            g.drawString(text, (scale * getWidth() - width) / 2, (scale * getHeight() - height) / 2);
        }
    }

    /**
     * Paint this component with the given Graphics object.
     *
     * @param g Graphics to paint with.
     */
    @Override
    public synchronized void paint(Graphics g) {
        paintTo(offscreenGraphics, offscreenScale);
        g.drawImage(offscreenBuffer, 0, 0, getWidth(), getHeight(), 0, 0, offscreenBuffer.getWidth(), offscreenBuffer.getHeight(), null);
    }

    /**
     * Move th ball position.
     */
    private void moveBall() {
        if (x + xa < 0)
            xa = BALL_DIAMETER / BALL_SPEED;
        if (x + xa > getWidth() - BALL_DIAMETER)
            xa = -(BALL_DIAMETER / BALL_SPEED);
        if (y + ya < 0)
            ya = BALL_DIAMETER / BALL_SPEED;
        if (y + ya > getHeight() - BALL_DIAMETER)
            isGameOver = true;
        if (isCollision()) {
            ya = -(BALL_DIAMETER / BALL_SPEED);
            y = ry - BALL_DIAMETER;
        }
        x = Math.min(x + xa, getWidth() - BALL_DIAMETER);
        y = Math.min(y + ya, getHeight() - BALL_DIAMETER);
    }

    /**
     * Move the raquet position based on whether user key is pressed or no.
     */
    public void moveRacquet() {
        ry = getHeight() - 50;

        if (rx + rxa > 0 && rx + rxa < getWidth() - RACQUET_WIDTH)
            rx = rx + rxa;
    }

    /**
     * Check if the ball collided with the racquet.
     *
     * @return True of ball has collided with the racquet.
     */
    private boolean isCollision() {
        Rectangle rBounds = new Rectangle(rx, ry, RACQUET_WIDTH, RACQUET_HEIGHT);
        Rectangle bBounds = new Rectangle(x, y, BALL_DIAMETER, BALL_DIAMETER);

        return rBounds.intersects(bBounds);
    }

    /**
     * Handler for key press events.
     *
     * @param userEvent Event associated with user pressing/de-pressing the controller buttons.
     */
    @Override
    public void userEventReceived(UserEvent userEvent) {
        if (userEvent.getFamily() == UserEvent.UEF_KEY_EVENT) {
            if (userEvent.getType() == HRcEvent.KEY_PRESSED) {
                switch (userEvent.getCode()) {
                    case HRcEvent.VK_LEFT:
                        rxa = -(RACQUET_WIDTH / RAQUET_SPEED);
                        break;
                    case HRcEvent.VK_RIGHT:
                        rxa = (RACQUET_WIDTH / RAQUET_SPEED);
                        break;
                }
            } else if (userEvent.getType() == HRcEvent.KEY_RELEASED) {
                switch (userEvent.getCode()) {
                    case HRcEvent.VK_LEFT:
                    case HRcEvent.VK_RIGHT:
                        rxa = 0;
                        break;
                    case HRcEvent.VK_COLORED_KEY_0:
                        terminated = true;
                        break;
                }
            }
        }
    }
}
