package net;

import java.util.ArrayList;

import game.Personnel.Directions;
import game.PlayerSeat;
import game.Personnel;
import game.SelectionType;

public interface GameDecisionProvider {
    int chooseMultipleChoice(PlayerSeat seat, String name, String explanation, Object[] options);

    boolean chooseYesNo(PlayerSeat seat, String name, String explanation);

    int chooseNumericChoice(PlayerSeat seat, String name, int minValue, int maxValue);

    Directions chooseDirection(PlayerSeat seat, Personnel activeFigure, ArrayList<Directions> allowedDirections);

    Personnel chooseTarget(PlayerSeat seat, SelectionType selectionType, ArrayList<Personnel> availableTargets);
}
