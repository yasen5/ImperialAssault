package net.structs;

import java.io.Serializable;
import java.util.Map;
import java.util.List;

import game.MissionOption;
import game.PlayerSeat;

public record LobbySnapshot(
        GameSessionConfig config,
        List<PlayerSeat> occupiedSeats,
        Map<PlayerSeat, MissionOption> missionSelections,
        boolean allSeatsFilled,
        boolean allMissionsSelected,
        boolean allMissionsMatch,
        MissionOption selectedMission) implements Serializable {
}
