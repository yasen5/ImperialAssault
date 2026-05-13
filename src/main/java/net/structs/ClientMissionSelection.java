package net.structs;

import java.io.Serializable;

public record ClientMissionSelection(MissionOption mission) implements Serializable {
}
