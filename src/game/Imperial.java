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
            ImperialType type, DefenseDieType[] defenseDice, OffenseDieType[] offenseDice) {
        super(name, startingHealth, speed, pos, defenseDice);
        this.type = type;
        this.offenseDice = offenseDice;
    }

    @Override
    public OffenseRoll[] getOffense() {
        OffenseRoll[] results = new OffenseRoll[offenseDice.length];
        for (int i = 0; i < offenseDice.length; i++) {
            results[0] = offenseDice[i].roll();
        }
        return results;
    }
}
