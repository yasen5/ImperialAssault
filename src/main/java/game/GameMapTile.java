package game;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

record GameMapTile(BufferedImage img, int[][] tileArray, Rectangle sourceBounds) {
  GameMapTile(BufferedImage img, int[][] tileArray) {
    this(img, tileArray, getOpaqueBounds(img));
  }

  private static Rectangle getOpaqueBounds(BufferedImage img) {
    int minX = img.getWidth();
    int minY = img.getHeight();
    int maxX = -1;
    int maxY = -1;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        if (((img.getRGB(x, y) >>> 24) & 0xff) == 0) {
          continue;
        }
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
      }
    }
    if (maxX < minX || maxY < minY) {
      return new Rectangle(0, 0, img.getWidth(), img.getHeight());
    }
    return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
  }
}
