package net.structs;

import java.io.Serializable;

public enum MissionOption implements Serializable {
    MISSION_ONE("Aftermath"),
    MISSION_TWO("A New Threat");

    private final String displayName;

    MissionOption(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
