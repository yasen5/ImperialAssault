package src.game;

import javax.swing.JButton;

public abstract class Imperial extends Personnel {
    private ImperialType type;


    public static enum ImperialType {
        TROOPER,
        DROID,
        OFFICER
    }

    public Imperial(String name, int startingHealth, int speed, Pos pos,
            ImperialType type) {
        super(name, startingHealth, speed, pos);
        this.type = type;
    }
}
