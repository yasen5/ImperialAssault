package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

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
        // Try both .jpg and .png
        image = LoaderUtils.getImage(name + "Deployment");
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
        int drawX = getDrawX();
        int drawY = getDrawY();
        Screen screen = Constants.screen;
        if (screen != null) {
            int availableWidth = Math.max(240, screen.getSidebarCardWidth());
            int cappedWidth = Math.min(xSize, availableWidth);
            float scale = cappedWidth / (float) xSize;
            xSize = cappedWidth;
            ySize = Math.round(ySize * scale);
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRoundRect(drawX - 6, drawY - 6, xSize + 12, ySize + 12, 24, 24);
        g2.drawImage(image, drawX, drawY, drawX + xSize, drawY + ySize, 0, 0, image.getWidth(null),
                image.getHeight(null), null);
        if (exhausted) {
            g2.setColor(new Color(255, 255, 255, 70));
            g2.drawRoundRect(drawX, drawY, xSize, ySize, 22, 22);
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(drawX, drawY, xSize, ySize, 22, 22);
        }
        g2.dispose();
    }

    public void setExhausted(boolean value) {
        exhausted = value;
    }

    // Toggle visibility
    public void setVisible(boolean value) {
        visible = value;
        parent.toggleDisplay();
    }

    public int getDrawX() {
        Screen screen = Constants.screen;
        if (screen == null) {
            return x;
        }
        return screen.getSidebarCardX();
    }

    public int getDrawY() {
        Screen screen = Constants.screen;
        if (screen == null) {
            return y;
        }
        return screen.getSidebarCardY();
    }

    public Rectangle getBounds() {
        int xSize = rebel ? heroXSize : imperialXSize;
        int ySize = rebel ? heroYSize : imperialYSize;
        Screen screen = Constants.screen;
        if (screen != null) {
            int availableWidth = Math.max(240, screen.getSidebarCardWidth());
            int cappedWidth = Math.min(xSize, availableWidth);
            float scale = cappedWidth / (float) xSize;
            xSize = cappedWidth;
            ySize = Math.round(ySize * scale);
        }
        return new Rectangle(getDrawX(), getDrawY(), xSize, ySize);
    }

    public String getLabel() {
        return parent.toString();
    }
}
