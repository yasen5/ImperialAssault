package game;

import util.MyArrayList;

import game.Pathfinder;
import game.Pathfinder.FullPos;
import game.Personnel.Directions;
import game.Pos;

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
      return Pathfinder.intersection(p1, p2, startPoint(), endPoint());
    }

    public FullPos startPoint() {
      int adjustedStartX = pos.getFullX();
      int adjustedStartY = pos.getFullY();
      if (vertical) {
        if (shortenFirstTip) {
          adjustedStartY += 5;
        }
      } else {
        if (shortenFirstTip) {
          adjustedStartX += 5;
        }
      }
      return new FullPos(adjustedStartX, adjustedStartY);
    }

    public FullPos endPoint() {
      int adjustedEndX = pos.getFullX();
      int adjustedEndY = pos.getFullY();
      if (vertical) {
        adjustedEndY += Constants.tileSize;
        if (shortenSecondTip) {
          adjustedEndY -= 5;
        }
      } else {
        adjustedEndX += Constants.tileSize;
        if (shortenSecondTip) {
          adjustedEndY -= 5;
        }
      }
      return new FullPos(adjustedEndX, adjustedEndY);
    }

    public MyArrayList<Pos> getHardEnds() {
      MyArrayList<Pos> hardEnds = new MyArrayList<>();
      if (!shortenFirstTip) {
        hardEnds.add(pos);
      }
      if (!shortenSecondTip) {
        hardEnds.add(pos.getNextPos(vertical ? Directions.DOWN : Directions.RIGHT));
      }
      return hardEnds;
    }
  }

  public static final int tileSize = 70;
  public static final int[][] TUTORIAL_TILE_MATRIX = new int[][] {
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

  public static final int[][] MISSION_TWO_TILE_MATRIX = new int[][] {
      new int[] { 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
      new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0 },
      new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0 },
      new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0 },
      new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0 },
      new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
      new int[] { 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
      new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0 }
  };

  public static final WallLine[] TUTORIAL_WALL_LINES = new WallLine[] {
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

  public static final WallLine[] MISSION_TWO_WALL_LINES = new WallLine[] {
      new WallLine(new Pos(2, 3), false, true, false, false),
      new WallLine(new Pos(4, 1), true, false, false, false),
      new WallLine(new Pos(3, 2), true, false, false, false),
      new WallLine(new Pos(3, 2), false, false, false, false),
      new WallLine(new Pos(3, 3), true, false, false, false),
      new WallLine(new Pos(3, 4), false, false, false, false),
      new WallLine(new Pos(4, 4), true, false, false, false),
      new WallLine(new Pos(4, 5), false, false, false, false),
      new WallLine(new Pos(5, 5), false, false, false, false),
      new WallLine(new Pos(6, 5), false, false, false, false),
      new WallLine(new Pos(7, 5), false, false, false, false),
      new WallLine(new Pos(8, 4), true, false, false, false),
  };

  public static int[][] tileMatrix = TUTORIAL_TILE_MATRIX;
  public static WallLine[] wallLines = TUTORIAL_WALL_LINES;

  public static void useMissionDefinition(MissionDefinition missionDefinition) {
    tileMatrix = missionDefinition.tileMatrix();
    wallLines = missionDefinition.wallLines();
  }
}
