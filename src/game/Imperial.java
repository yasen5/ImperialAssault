package src.game;

public abstract class Imperial extends Personnel {
    private boolean massive;
    private ImperialType type;

    public static enum ImperialType {
        TROOPER,
        DROID,
        OFFICER
    }

    public Imperial(String name, int startingHealth, int speed, Pos pos, boolean massive,
            ImperialType type) {
        super(name, startingHealth, speed, pos);
        this.massive = massive;
        this.type = type;
    }
}
