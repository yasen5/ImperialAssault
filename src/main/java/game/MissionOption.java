package game;

import java.io.Serializable;

public enum MissionOption implements Serializable {
    MISSION_ONE("Mission 1"),
    MISSION_TWO("Mission 2");

    private final String displayName;

    MissionOption(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
