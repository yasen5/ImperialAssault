package src;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import src.game.Game.MapTile;

public class Constants {

    public static final int tileSize = 70;
    public static final String baseImgFilePath = "/Users/yasen/Documents/Quarter4Project/src/game/images/";
    public static final boolean debug = true;
    public static final int[][] tileMatrix = new int[][] {
            new int[] { 0, 0, 0, 1, 1, 0, 1, 1, 0, 0 },
            new int[] { 0, 0, 0, 1, 1, 0, 1, 1, 0, 0 },
            new int[] { 0, 0, 0, 1, 1, 0, 1, 1, 0, 0 },
            new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 0, 0 },
            new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 0, 0 },
            new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 0, 0 },
            new int[] { 1, 1, 0, 0, 0, 0, 1, 1, 0, 0 },
            new int[] { 1, 1, 1, 0, 0, 0, 1, 1, 0, 0 },
            new int[] { 1, 1, 1, 1, 0, 0, 1, 1, 0, 0 },
            new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            new int[] { 0, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            new int[] { 0, 0, 0, 0, 1, 1, 1, 1, 1, 1 },
            new int[] { 0, 0, 0, 0, 1, 1, 1, 1, 1, 1 }
    };
}
