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
            Personnel occupant = game == null ? null : game.getPersonnelAtPos(destinationSpace);
            if (occupant != null && occupant != figure) {
                return false;
            }
        }
        return true;
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

    private static boolean isDiagonal(Directions direction) {
        return direction == Directions.UPLEFT || direction == Directions.UPRIGHT || direction == Directions.DOWNLEFT
                || direction == Directions.DOWNRIGHT;
    }
}
