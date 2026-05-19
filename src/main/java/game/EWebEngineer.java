package game;

import game.Die.DefenseDieType;
import game.Die.OffenseDieType;
import game.Personnel.Actions;
import util.MyArrayList;

public class EWebEngineer extends Imperial {
    public EWebEngineer(Pos pos) {
        super("EWebEngineer", 5, 2, pos,
                ImperialType.TROOPER, new DefenseDieType[] { DefenseDieType.BLACK },
                new OffenseDieType[] { OffenseDieType.BLUE, OffenseDieType.RED, OffenseDieType.YELLOW },
                false, false);
        setFigureSize(1, 2);
    }

    @Override
    public Equipment.SurgeOptions[] getSurgeOptions() {
        return new Equipment.SurgeOptions[] { Equipment.SurgeOptions.RECOVER2, Equipment.SurgeOptions.DAMAGE1,
                Equipment.SurgeOptions.ACCURACY3 };
    }

    @Override
    public boolean gainsMoveBeforeImperialAction() {
        return false;
    }

    @Override
    public int getImperialActionCount() {
        return 2;
    }

    @Override
    public boolean canTakeAction(Actions action, MyArrayList<Actions> actionsUsedThisActivation) {
        if (action == Actions.ATTACK) {
            return !actionsUsedThisActivation.contains(Actions.MOVE);
        }
        if (action == Actions.MOVE) {
            return !actionsUsedThisActivation.contains(Actions.ATTACK);
        }
        return true;
    }
}
