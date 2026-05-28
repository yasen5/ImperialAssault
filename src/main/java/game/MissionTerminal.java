package game;

import java.awt.Graphics;

import game.Die.DefenseDieResult;
import game.Die.DefenseDieType;
import game.Die.DefenseRoll;
import game.Die.OffenseRoll;

public class MissionTerminal extends Personnel {
    private static final int DEFAULT_HEALTH = 4;

    public MissionTerminal(Pos pos) {
        super("RedTerminalToken", DEFAULT_HEALTH, 0, pos, new DefenseDieType[] { DefenseDieType.BLACK }, false, false);
        setOwnerSeat(PlayerSeat.IMPERIAL);
    }

    @Override
    public OffenseRoll[] getOffense() {
        return new OffenseRoll[0];
    }

    @Override
    public Equipment.SurgeOptions[] getSurgeOptions() {
        return new Equipment.SurgeOptions[0];
    }

    @Override
    public DefenseRoll[] getDefense() {
        DefenseRoll[] baseDefense = super.getDefense();
        if (!game.isAdjacentToImperial(getPos())) {
            return baseDefense;
        }
        DefenseRoll[] boostedDefense = new DefenseRoll[baseDefense.length + 1];
        for (int i = 0; i < baseDefense.length; i++) {
            boostedDefense[i] = baseDefense[i];
        }
        boostedDefense[boostedDefense.length - 1] = new DefenseRoll(-1, new DefenseDieResult(1, 0, false));
        return boostedDefense;
    }

    @Override
    public void draw(Graphics g) {
        if (!isDefeated()) {
            super.draw(g);
        }
    }

    public void harden() {
        if (getHealth() <= 0) {
            return;
        }
        setHealth(7);
    }
}
