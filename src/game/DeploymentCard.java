package src.game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import src.Constants;

// This class helps display the status of a hero and their special abilities
public class DeploymentCard {
    // Instance variables
    private BufferedImage image;
    private boolean exhausted;
    private boolean visible = false;
    public static final int heroXSize = 600, heroYSize = 472, imperialXSize = 380, imperialYSize = 600;
    public static final int x = 1000, y = 10;
    private FullDeployment parent;
    private final boolean rebel;

    // Constructor
    public DeploymentCard(String name, boolean rebel, FullDeployment parent) {
        String adjustedName = Constants.baseImgFilePath + name + "Deployment";
        // Try both .jpg and .png
        try {
            this.image = ImageIO.read(new File(adjustedName + ".jpg"));
        } catch (IOException ex) {
            try {
                this.image = ImageIO.read(new File(adjustedName + ".png"));
            } catch (IOException e) {
                System.out.println("Couldn't read either in jpg or png, tried " + adjustedName + ".jpg" + " and "
                        + adjustedName + ".png" + ex);
                System.exit(0);
            }
        }
        exhausted = false;
        this.rebel = rebel;
        this.parent = parent;
    }

    // Draw if currently visible, grey out if exhausted
    public void draw(Graphics g) {
        int xSize = rebel ? heroXSize : imperialXSize;
        int ySize = rebel ? heroYSize : imperialYSize;
        if (!visible) {
            return;
        }
        g.drawImage(image, x, y, x + xSize, y + ySize, 0, 0, image.getWidth(null), image.getHeight(null),
                null);
        if (exhausted) {
            g.setColor(new Color(87, 87, 87, 15));
            g.drawRect(x, y, xSize, ySize);
        }
    }

    public void setExhausted(boolean value) {
        exhausted = value;
    }

    // Toggle visibility
    public void setVisible(boolean value) {
        visible = value;
        parent.toggleDisplay();
    }
}
