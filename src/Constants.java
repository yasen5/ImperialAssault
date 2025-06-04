package src;

import javax.swing.JFrame;

import src.game.Pathfinder;
import src.game.Pathfinder.FullPos;
import src.game.Personnel.Directions;
import src.game.Pos;

// Contains all the constants for use throughout the program
public class Constants {
    public static record WalledTile(Pos pos, Directions[] wallDirections) {
    }

    // Can be put anywhere, be vertical horizontal, have the tips count (block line
    // of sight)
    public static record WallLine(Pos pos, boolean vertical, boolean shortenFirstTip, boolean shortenSecondTip,
            boolean softBarrier) {
        public boolean intersects(FullPos p1, FullPos p2, boolean includeSoft) {
            if (!includeSoft && softBarrier) {
                return false;
            }
            int adjustedStartX = pos.getFullX();
            int adjustedStartY = pos.getFullY();
            int adjustedEndX = pos.getFullX();
            int adjustedEndY = pos.getFullY();
            if (vertical) {
                adjustedEndY += Constants.tileSize;
                if (shortenFirstTip) {
                    adjustedStartY += 5;
                }
                if (shortenSecondTip) {
                    adjustedEndY -= 5;
                }
            } else {
                adjustedEndX += Constants.tileSize;
                if (shortenFirstTip) {
                    adjustedStartX += 5;
                }
                if (shortenSecondTip) {
                    adjustedEndY -= 5;
                }
            }
            return Pathfinder.intersection(p1, p2, new FullPos(adjustedStartX, adjustedStartY),
                    new FullPos(adjustedEndX, adjustedEndY));
        }
    }

    public static final int tileSize = 70;
    public static final String gamePath = "/Users/yasen/Documents/Quarter4Project/src/game/";
    public static final String baseImgFilePath = gamePath + "images/";
    public static final String baseSoundFilePath = gamePath + "sounds/";
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

    public static final WallLine[] wallLines = new WallLine[] {
            new WallLine(new Pos(2, 5), true, true, false, false),
            new WallLine(new Pos(6, 5), true, true, false, false),
            new WallLine(new Pos(6, 9), true, false, true, true),
            new WallLine(new Pos(5, 11), false, true, true, true),
            new WallLine(new Pos(6, 11), true, false, false, true),
            new WallLine(new Pos(6, 12), false, false, false, true),
            new WallLine(new Pos(7, 12), false, false, false, true),
            new WallLine(new Pos(8, 12), false, false, true, true),
            new WallLine(new Pos(4, 4), true, true, true, true),
    };

    public static JFrame frame;
}
