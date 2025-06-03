package src.game;

import src.game.Die.DefenseDieType;
import src.game.Die.OffenseDieType;
import src.game.Die.OffenseRoll;

public abstract class Imperial extends Personnel {
    private ImperialType type;
    private OffenseDieType[] offenseDice;

    public static enum ImperialType {
        TROOPER,
        DROID,
        OFFICER
    }

    public Imperial(String name, int startingHealth, int speed, Pos pos,
            ImperialType type, DefenseDieType[] defenseDice, OffenseDieType[] offenseDice, boolean hasSpecial) {
        super(name, startingHealth, speed, pos, defenseDice, hasSpecial);
        this.type = type;
        this.offenseDice = offenseDice;
    }

    @Override
    public OffenseRoll[] getOffense() {
        OffenseRoll[] results = new OffenseRoll[offenseDice.length + (focused ? 1 : 0)];
        for (int i = 0; i < offenseDice.length; i++) {
            results[i] = offenseDice[i].roll();
        }
        if (focused) {
            results[results.length - 1] = OffenseDieType.GREEN.roll();
            focused = false;
        }
        return results;
    }
}
