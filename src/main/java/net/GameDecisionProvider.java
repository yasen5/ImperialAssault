package game;

import java.util.ArrayList;

import game.Screen.SelectingType;
import game.Personnel.Directions;

public interface GameDecisionProvider {
    int chooseMultipleChoice(PlayerSeat seat, String name, String explanation, Object[] options);

    boolean chooseYesNo(PlayerSeat seat, String name, String explanation);

    int chooseNumericChoice(PlayerSeat seat, String name, int minValue, int maxValue);

    Directions chooseDirection(PlayerSeat seat, Personnel activeFigure, ArrayList<Directions> allowedDirections);

    Personnel chooseTarget(PlayerSeat seat, SelectingType selectionType, ArrayList<Personnel> availableTargets);
}
