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
        Personnel occupant = game == null ? null : game.getPersonnelAtPos(to);
        return occupant == null || occupant == figure;
    }
}
