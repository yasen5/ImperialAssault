package game;

import game.Personnel.Directions;

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
    if (!figure.isNonSquareLargeFigure()) {
      return false;
    }
    Pos[] currentSpaces = figure.getOccupiedSpaces();
    Pos[] rotatedSpaces = occupiedSpacesAtSize(figure.getPos(), figure.getYSize(), figure.getXSize());
    int overlapCount = 0;
    for (Pos rotatedSpace : rotatedSpaces) {
      if (!isLegalBoardSpace(rotatedSpace)) {
        return false;
      }
      if (contains(currentSpaces, rotatedSpace)) {
        overlapCount++;
      }
      if (game != null && !game.isSpaceAvailable(rotatedSpace, figure)) {
        return false;
      }
    }
    return overlapCount * 2 >= rotatedSpaces.length;
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
