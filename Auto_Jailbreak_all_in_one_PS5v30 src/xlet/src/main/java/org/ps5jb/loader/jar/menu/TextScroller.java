package org.ps5jb.loader.jar.menu;

import java.awt.Graphics2D;

public class TextScroller {
    private String text;
    private int x;
    private final int y;
    private final int speed;
    private boolean active = true;
    private boolean glitchEnabled;

    public TextScroller(String text, int startX, int y, int speed, boolean glitchEnabled) {
        this.text = text;
        this.x = startX;
        this.y = y;
        this.speed = speed;
        this.glitchEnabled = glitchEnabled;
    }

    public void update() {
        if (active) {
            x -= speed;
            if (x <= 50) {
                x = 50;
                active = false;
            }
        }
    }

    public void draw(Graphics2D g) {
        String toDraw = glitchEnabled && Math.random() < 0.1 ? getGlitchedText(text) : text;
        g.drawString(toDraw, x, y);
    }

    private String getGlitchedText(String original) {
        StringBuilder glitched = new StringBuilder();
        for (int i = 0; i < original.length(); i++) {
            if (Math.random() < 0.1) {
                glitched.append((char) (33 + (int)(Math.random() * 30)));
            } else {
                glitched.append(original.charAt(i));
            }
        }
        return glitched.toString();
    }
}
