package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

// This class helps display the status of a hero and their special abilities
public class DeploymentCard {
  private static final int FRAME_PADDING_DIVISOR = 64;
  private static final int CORNER_ARC_DIVISOR = 16;

  // Instance variables
  private BufferedImage image;
  private boolean exhausted;
  private boolean visible = false;
  private FullDeployment parent;
  private Rectangle imageBounds = new Rectangle();
  private Rectangle detailBounds = new Rectangle();

  // Constructor
  public DeploymentCard(String name, boolean rebel, FullDeployment parent) {
    // Try both .jpg and .png
    image = LoaderUtils.getImage(name + "Deployment");
    exhausted = false;
    this.parent = parent;
  }

  // Draw if currently visible, grey out if exhausted
  public void draw(Graphics g) {
    if (!visible || imageBounds.width <= 0 || imageBounds.height <= 0) {
      return;
    }
    Graphics2D g2 = (Graphics2D) g.create();
    int framePadding = Math.max(1, imageBounds.width / FRAME_PADDING_DIVISOR);
    int cornerArc = Math.max(1, imageBounds.width / CORNER_ARC_DIVISOR);
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.setColor(new Color(0, 0, 0, 130));
    g2.fillRoundRect(imageBounds.x - framePadding, imageBounds.y - framePadding,
        imageBounds.width + framePadding * 2, imageBounds.height + framePadding * 2, cornerArc, cornerArc);
    g2.drawImage(image, imageBounds.x, imageBounds.y, imageBounds.x + imageBounds.width,
        imageBounds.y + imageBounds.height, 0, 0, image.getWidth(null),
        image.getHeight(null), null);
    if (exhausted) {
      g2.setColor(new Color(255, 255, 255, 70));
      g2.drawRoundRect(imageBounds.x, imageBounds.y, imageBounds.width, imageBounds.height, cornerArc, cornerArc);
      g2.setColor(new Color(0, 0, 0, 90));
      g2.fillRoundRect(imageBounds.x, imageBounds.y, imageBounds.width, imageBounds.height, cornerArc, cornerArc);
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

  public void setLayoutBounds(Rectangle imageBounds, Rectangle detailBounds) {
    this.imageBounds = imageBounds == null ? new Rectangle() : new Rectangle(imageBounds);
    this.detailBounds = detailBounds == null ? new Rectangle() : new Rectangle(detailBounds);
  }

  public int getBaseImageWidth() {
    return image.getWidth(null);
  }

  public int getBaseImageHeight() {
    return image.getHeight(null);
  }

  public Rectangle getBounds() {
    return new Rectangle(imageBounds);
  }

  public Rectangle getDetailBounds() {
    return new Rectangle(detailBounds);
  }

  public String getLabel() {
    return parent.toString();
  }
}
