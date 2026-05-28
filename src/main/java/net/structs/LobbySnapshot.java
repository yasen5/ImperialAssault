package net.structs;

import java.io.Serializable;
import util.MyHashMap;

import util.MyArrayList;


import game.PlayerSeat;

public record LobbySnapshot(
    GameSessionConfig config,
    MyArrayList<PlayerSeat> occupiedSeats,
    MyHashMap<PlayerSeat, MissionOption> missionSelections,
    boolean allSeatsFilled,
    boolean allMissionsSelected,
    boolean allMissionsMatch,
    MissionOption selectedMission) implements Serializable {
}
