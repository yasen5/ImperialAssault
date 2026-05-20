package game;

import game.Constants.WallLine;
import game.Pathfinder.FullPos;
import game.Personnel.Directions;
import util.MyArrayList;

public final class MovementRules {
  private MovementRules() {
  }

  public static boolean canMoveOneSpace(Personnel figure, Directions direction, Game game) {
    if (figure.isLargeFigure() && isDiagonal(direction)) {
      return false;
    }
    for (Pos occupiedSpace : figure.getOccupiedSpaces()) {
      if (!occupiedSpace.canMove(direction, false, true, game)) {
        return false;
      }
    }
    Pos nextAnchor = figure.getPos().getNextPos(direction);
    for (Pos destinationSpace : occupiedSpacesAt(figure, nextAnchor)) {
      if (game != null && !game.isSpaceAvailable(destinationSpace, figure)) {
        return false;
      }
    }
    return true;
  }

  public static boolean canRotate(Personnel figure, Game game) {
    return !getLegalRotations(figure, game).isEmpty();
  }

  public static MyArrayList<RotationMove> getLegalRotations(Personnel figure, Game game) {
    MyArrayList<RotationMove> legalRotations = new MyArrayList<>();
    if (!figure.isNonSquareLargeFigure()) {
      return legalRotations;
    }
    Pos[] currentSpaces = figure.getOccupiedSpaces();
    int rotatedXSize = figure.getYSize();
    int rotatedYSize = figure.getXSize();
    for (int y = 0; y < Constants.tileMatrix.length; y++) {
      for (int x = 0; x < Constants.tileMatrix[y].length; x++) {
        Pos anchor = new Pos(x, y);
        Pos[] rotatedSpaces = occupiedSpacesAtSize(anchor, rotatedXSize, rotatedYSize);
        if (isLegalRotation(currentSpaces, rotatedSpaces, figure, game)) {
          legalRotations.add(new RotationMove(anchor, rotatedXSize, rotatedYSize));
        }
      }
    }
    return legalRotations;
  }

  private static boolean isLegalRotation(Pos[] currentSpaces, Pos[] rotatedSpaces, Personnel figure, Game game) {
    if (!isLegalFootprint(rotatedSpaces)) {
      return false;
    }
    int overlapCount = 0;
    for (Pos rotatedSpace : rotatedSpaces) {
      if (contains(currentSpaces, rotatedSpace)) {
        overlapCount++;
      }
      if (game != null && !game.isSpaceAvailable(rotatedSpace, figure)) {
        return false;
      }
    }
    return overlapCount * 2 >= rotatedSpaces.length;
  }

  public static boolean isLegalFootprint(Pos[] spaces) {
    for (Pos space : spaces) {
      if (!isLegalBoardSpace(space)) {
        return false;
      }
    }
    for (int i = 0; i < spaces.length; i++) {
      for (int j = i + 1; j < spaces.length; j++) {
        if (areAdjacent(spaces[i], spaces[j]) && barrierBetween(spaces[i], spaces[j])) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean barrierBetween(Pos first, Pos second) {
    FullPos firstCenter = first.getCenterPos();
    FullPos secondCenter = second.getCenterPos();
    for (WallLine wallLine : Constants.wallLines) {
      if (wallLine.intersects(firstCenter, secondCenter, true)) {
        return true;
      }
    }
    return false;
  }

  private static boolean areAdjacent(Pos first, Pos second) {
    int deltaX = Math.abs(first.getX() - second.getX());
    int deltaY = Math.abs(first.getY() - second.getY());
    return deltaX + deltaY == 1;
  }

  private static Pos[] occupiedSpacesAt(Personnel figure, Pos anchor) {
    Pos currentAnchor = figure.getPos();
    Pos[] currentSpaces = figure.getOccupiedSpaces();
    Pos[] destinationSpaces = new Pos[currentSpaces.length];
    int deltaX = anchor.getX() - currentAnchor.getX();
    int deltaY = anchor.getY() - currentAnchor.getY();
    for (int i = 0; i < currentSpaces.length; i++) {
      destinationSpaces[i] = new Pos(currentSpaces[i].getX() + deltaX, currentSpaces[i].getY() + deltaY);
    }
    return destinationSpaces;
  }

  public static Pos[] occupiedSpacesAtSize(Pos anchor, int xSize, int ySize) {
    Pos[] occupiedSpaces = new Pos[xSize * ySize];
    int index = 0;
    for (int y = 0; y < ySize; y++) {
      for (int x = 0; x < xSize; x++) {
        occupiedSpaces[index++] = new Pos(anchor.getX() + x, anchor.getY() + y);
      }
    }
    return occupiedSpaces;
  }

  public static boolean isLegalBoardSpace(Pos space) {
    return space.isOnGrid() && Constants.tileMatrix[space.getY()][space.getX()] == 1;
  }

  private static boolean contains(Pos[] spaces, Pos target) {
    for (Pos space : spaces) {
      if (space.equalTo(target)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isDiagonal(Directions direction) {
    return direction == Directions.UPLEFT || direction == Directions.UPRIGHT || direction == Directions.DOWNLEFT
        || direction == Directions.DOWNRIGHT;
  }
}
