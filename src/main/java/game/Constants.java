package game;

import java.util.Optional;
import util.MyArrayList;

import game.Pathfinder;
import game.Pathfinder.FullPos;
import game.Personnel.Directions;
import game.Pos;

// Contains all the constants for use throughout the program
public class Constants {
  public static record WalledTile(Pos pos, Directions[] wallDirections) {
  }

  public static enum EndpointTouchPolicy {
    BLOCK,
    ALLOW,
    ALLOW_NORMAL_CORNER_CROSSING
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

    private boolean touchesEndpoint(FullPos p1, FullPos p2) {
      return pointOnMovementSegment(p1, startPoint(), p2) || pointOnMovementSegment(p1, endPoint(), p2);
    }

    private boolean pointOnMovementSegment(FullPos movementStart, FullPos point, FullPos movementEnd) {
      return Pathfinder.orientation(movementStart, point, movementEnd) == 0
          && Pathfinder.pointOnSegment(movementStart, point, movementEnd);
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

  public static boolean blocksMovement(WallLine[] lines, FullPos p1, FullPos p2, boolean includeSoft,
      EndpointTouchPolicy endpointPolicy) {
    for (WallLine wallLine : lines) {
      if (!wallLine.intersects(p1, p2, includeSoft)) {
        continue;
      }
      if (endpointPolicy == EndpointTouchPolicy.ALLOW && wallLine.touchesEndpoint(p1, p2)) {
        continue;
      }
      if (endpointPolicy == EndpointTouchPolicy.ALLOW_NORMAL_CORNER_CROSSING
          && endpointTouchCanBeCrossedNormally(lines, wallLine, p1, p2, includeSoft)) {
        continue;
      }
      return true;
    }
    return false;
  }

  private static boolean endpointTouchCanBeCrossedNormally(WallLine[] lines, WallLine touchedLine, FullPos p1,
      FullPos p2, boolean includeSoft) {
    Optional<FullPos> endpoint = touchedEndpoint(touchedLine, p1, p2);
    if (endpoint.isEmpty()) {
      return false;
    }

    MyArrayList<DirectionVector> incidentDirections = new MyArrayList<>();
    for (WallLine wallLine : lines) {
      if (!includeSoft && wallLine.softBarrier()) {
        continue;
      }
      incidentDirectionFromEndpoint(wallLine, endpoint.orElseThrow())
          .ifPresent(incidentDirections::add);
    }
    if (incidentDirections.size() != 2) {
      return false;
    }

    DirectionVector first = incidentDirections.get(0);
    DirectionVector second = incidentDirections.get(1);
    if (first.x() == second.x() || first.y() == second.y()) {
      return false;
    }

    int movementX = sign(p2.x() - p1.x());
    int movementY = sign(p2.y() - p1.y());
    int blockedX = first.x() + second.x();
    int blockedY = first.y() + second.y();
    return (movementX != blockedX || movementY != blockedY)
        && (movementX != -blockedX || movementY != -blockedY);
  }

  private static Optional<FullPos> touchedEndpoint(WallLine wallLine, FullPos p1, FullPos p2) {
    if (wallLine.pointOnMovementSegment(p1, wallLine.startPoint(), p2)) {
      return Optional.of(wallLine.startPoint());
    }
    if (wallLine.pointOnMovementSegment(p1, wallLine.endPoint(), p2)) {
      return Optional.of(wallLine.endPoint());
    }
    return Optional.empty();
  }

  private static Optional<DirectionVector> incidentDirectionFromEndpoint(WallLine wallLine, FullPos endpoint) {
    if (samePoint(endpoint, wallLine.startPoint())) {
      return Optional.of(directionBetween(wallLine.startPoint(), wallLine.endPoint()));
    }
    if (samePoint(endpoint, wallLine.endPoint())) {
      return Optional.of(directionBetween(wallLine.endPoint(), wallLine.startPoint()));
    }
    return Optional.empty();
  }

  private static DirectionVector directionBetween(FullPos from, FullPos to) {
    return new DirectionVector(sign(to.x() - from.x()), sign(to.y() - from.y()));
  }

  private static boolean samePoint(FullPos first, FullPos second) {
    return first.x() == second.x() && first.y() == second.y();
  }

  private static int sign(double value) {
    if (value > 0) {
      return 1;
    }
    if (value < 0) {
      return -1;
    }
    return 0;
  }

  private static record DirectionVector(int x, int y) {
  }

  public static void useMissionDefinition(MissionDefinition missionDefinition) {
    tileMatrix = missionDefinition.tileMatrix();
    wallLines = missionDefinition.wallLines();
  }
}
