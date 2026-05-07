package net;

import java.io.Serializable;

import game.MissionOption;

public record ClientMissionSelection(MissionOption mission) implements Serializable {
}
