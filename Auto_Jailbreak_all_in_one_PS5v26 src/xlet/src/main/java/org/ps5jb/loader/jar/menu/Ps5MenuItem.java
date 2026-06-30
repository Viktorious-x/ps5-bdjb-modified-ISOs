package org.ps5jb.loader.jar.menu;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Ps5MenuItem {

    private final String label;
    private Image icon;

    public Ps5MenuItem(String label, String imagePath) throws IOException {
        this.label = label;

        if (imagePath != null) {
            InputStream iconStream = this.getClass().getResourceAsStream(imagePath);
            if (iconStream == null) {
                throw new FileNotFoundException(imagePath);
            }

            byte[] iconBytes;
            try {
                int iconSize = iconStream.available();
                int iconWriteStart = 0;
                int iconRead = 0;
                iconBytes = new byte[iconSize];
                while ((iconWriteStart < iconSize) && (iconRead != -1)) {
                    iconRead = iconStream.read(iconBytes, iconWriteStart, iconSize);
                    if (iconRead > 0) {
                        iconWriteStart += iconRead;
                    }
                }
            } finally {
                iconStream.close();
            }
            this.icon = Toolkit.getDefaultToolkit().createImage(iconBytes);
        }
    }

    public String getLabel() {
        return label;
    }

    public Image getIcon() {
        return icon;
    }
}
