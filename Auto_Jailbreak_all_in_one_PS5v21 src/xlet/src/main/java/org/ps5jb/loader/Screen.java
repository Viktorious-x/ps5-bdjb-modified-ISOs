package org.ps5jb.loader;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Encapsulates the capabilities of the screen.
 */
public class Screen extends Container {
    private static final long serialVersionUID = 0x4141414141414141L;

    private final Font FONT = new Font(null, Font.PLAIN, 20);
    private final ArrayList messages = new ArrayList();
    private static final Screen instance = new Screen();

    // AtomicBoolean entfernt – unter Java 1.3/1.4 nicht verfügbar
    private boolean isPainting = false;
    private boolean isDirty    = false;

    private Screen() {
        super();
    }

    public static Screen getInstance() {
        return instance;
    }

    public static void println(String msg) {
        println(msg, true, false);
    }

    public static void println(String msg, boolean repaint, boolean replaceLast) {
        getInstance().print(msg, repaint, replaceLast);
    }

    public void print(String msg, boolean repaint, boolean replaceLast) {
        synchronized (this) {
            if (replaceLast && messages.size() > 0) {
                messages.remove(messages.size() - 1);
            }
            messages.add(msg);
            // feste 48-Begrenzung entfernt
            isDirty = true;
        }

        if (repaint) {
            repaint();
        }
    }

    public void printStackTrace(Throwable e) {
        StringTokenizer st;
        StringBuffer   sb;

        try {
            StringWriter sw = new StringWriter();
            try {
                PrintWriter pw = new PrintWriter(sw);
                try {
                    e.printStackTrace(pw);
                } finally {
                    pw.close();
                }
                String stackTrace = sw.toString();
                st = new StringTokenizer(stackTrace, "\n", false);
                sb = new StringBuffer(stackTrace.length());
            } finally {
                sw.close();
            }

            synchronized (this) {
                while (st.hasMoreTokens()) {
                    String line = st.nextToken();
                    sb.setLength(0);
                    for (int i = 0; i < line.length(); ++i) {
                        char c = line.charAt(i);
                        // keine Character.valueOf-Calls hier!
                        if (c == '\t') {
                            sb.append("   ");
                        } else {
                            sb.append(c);
                        }
                    }
                    print(sb.toString(), !st.hasMoreTokens(), false);
                }
            }
        } catch (IOException ioEx) {
            printThrowable(e);
            throw new RuntimeException("Another exception occurred while printing stacktrace. "
                    + ioEx.getClass().getName() + ": " + ioEx.getMessage());
        }
    }

    public void printThrowable(Throwable e) {
        print(e.getClass().getName() + ": " + e.getMessage(), true, false);
    }

    @Override
    public void paint(Graphics g) {
        List messagesCopy;
        synchronized (this) {
            if (isPainting || !isDirty) return;
            isPainting = true;
            isDirty    = false;
            messagesCopy = new ArrayList(messages);
        }

        g.setFont(FONT);
        g.setColor(Color.white);
        g.clearRect(0, 0, getWidth(), getHeight());

        // dynamische Zeilen-Anzeige:
        int topMargin    = 20;
        int bottomMargin = 20;
        int leftMargin   = 10;

        int lineHeight   = g.getFontMetrics().getHeight();
        int usableHeight = getHeight() - topMargin - bottomMargin;
        int maxLines     = usableHeight / lineHeight;

        int startIndex = Math.max(0, messagesCopy.size() - maxLines);

        int x = leftMargin;
        int y = topMargin + g.getFontMetrics().getAscent();

        for (int i = startIndex; i < messagesCopy.size(); i++) {
            String msg = (String) messagesCopy.get(i);
            g.drawString(msg, x, y);
            y += lineHeight;
        }

        isPainting = false;
    }
}
