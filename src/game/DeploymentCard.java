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
    private int x, y, xSize = 50, ySize = 50;

    public DeploymentCard(String imgFilePath, int x, int y) {
        this.x = x;
        this.y = y;
        try {
            image = ImageIO.read(new File(imgFilePath));
        } catch (IOException e) {
            System.out.println("Error reading deployment card image: " + e);
            System.exit(0);
        }
        exhausted = false;
    }

    public void draw(Graphics g) {
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
}
