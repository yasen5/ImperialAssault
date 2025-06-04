package src.game;

import src.game.Die.DefenseDieType;
import src.game.Die.OffenseDieType;
import src.game.Die.OffenseRoll;

public abstract class Imperial extends Personnel {
    // Instance variables
    private ImperialType type;
    protected OffenseDieType[] offenseDice;

    // Types relevant for some abilities
    public static enum ImperialType {
        TROOPER,
        DROID,
        OFFICER
    }

    // Constructor
    public Imperial(String name, int startingHealth, int speed, Pos pos,
            ImperialType type, DefenseDieType[] defenseDice, OffenseDieType[] offenseDice, boolean hasSpecial, boolean specialRequiresSelection) {
        super(name, startingHealth, speed, pos, defenseDice, hasSpecial, specialRequiresSelection);
        this.type = type;
        this.offenseDice = offenseDice;
    }

    // Same as in Hero.java's implementation, with more time could be set up to be in the superclass for less duplicate code
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

    public ImperialType getType() {
        return type;
    }
}
