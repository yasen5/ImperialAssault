package game;

import game.Personnel.Directions;

public final class MovementRules {
    private MovementRules() {
    }

    public static boolean canMoveOneSpace(Personnel figure, Directions direction, Game game) {
        Pos from = figure.getPos();
        Pos to = from.getNextPos(direction);
        if (!from.canMove(direction, false, true, game)) {
            return false;
        }
        if (isDiagonal(direction) && crossesBlockedCorner(from, direction, game)) {
            return false;
        }
        Personnel occupant = game == null ? null : game.getPersonnelAtPos(to);
        return occupant == null || occupant == figure;
    }

    private static boolean isDiagonal(Directions direction) {
        return direction == Directions.UPLEFT || direction == Directions.UPRIGHT
                || direction == Directions.DOWNLEFT || direction == Directions.DOWNRIGHT;
    }

    private static boolean crossesBlockedCorner(Pos from, Directions diagonal, Game game) {
        Directions horizontal = switch (diagonal) {
            case UPLEFT, DOWNLEFT -> Directions.LEFT;
            case UPRIGHT, DOWNRIGHT -> Directions.RIGHT;
            default -> diagonal;
        };
        Directions vertical = switch (diagonal) {
            case UPLEFT, UPRIGHT -> Directions.UP;
            case DOWNLEFT, DOWNRIGHT -> Directions.DOWN;
            default -> diagonal;
        };
        return !from.canMove(horizontal, false, true, game) || !from.canMove(vertical, false, true, game);
    }
}
