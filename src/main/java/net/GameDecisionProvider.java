package net;

import util.MyArrayList;


import game.Personnel.Directions;
import game.PlayerSeat;
import game.Personnel;
import game.MovementChoice;
import game.SelectionType;

public interface GameDecisionProvider {
    int chooseMultipleChoice(PlayerSeat seat, String name, String explanation, Object[] options);

    boolean chooseYesNo(PlayerSeat seat, String name, String explanation);

    int chooseNumericChoice(PlayerSeat seat, String name, int minValue, int maxValue);

    Directions chooseDirection(PlayerSeat seat, Personnel activeFigure, MyArrayList<Directions> allowedDirections);

    default MovementChoice chooseMovement(PlayerSeat seat, Personnel activeFigure, MyArrayList<Directions> allowedDirections,
            boolean canRotate) {
        if (canRotate && allowedDirections.isEmpty()) {
            return MovementChoice.rotate();
        }
        return MovementChoice.direction(chooseDirection(seat, activeFigure, allowedDirections));
    }

    Personnel chooseTarget(PlayerSeat seat, SelectionType selectionType, MyArrayList<Personnel> availableTargets);
}
