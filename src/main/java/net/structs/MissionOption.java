package net.structs;

import java.io.Serializable;

public enum MissionOption implements Serializable {
    MISSION_ONE("Tutorial Mission"),
    MISSION_TWO("Real Mission");

    private final String displayName;

    MissionOption(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
