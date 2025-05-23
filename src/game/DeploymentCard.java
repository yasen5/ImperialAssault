package src.game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class DeploymentCard {
    private BufferedImage image;
    private boolean exhausted;
    private boolean visible = false;
    private static int heroXSize = 600, heroYSize = 472, imperialXSize = 380, imperialYSize = 600;
    private static int x = 1000, y = 10;

    private final boolean rebel;

    public DeploymentCard(String imgFilePath, boolean rebel) {
        try {
            image = ImageIO.read(new File(imgFilePath));
        } catch (IOException e) {
            System.out.println("Error reading deployment card image: " + e);
            System.exit(0);
        }
        exhausted = false;
        this.rebel = rebel;
    }

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

    public void setVisible(boolean value) {
        visible = value;
    }
}
