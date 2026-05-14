package net.structs;

import java.io.Serializable;
import util.MyArrayList;


import game.PlayerSeat;

public record MatchSnapshot(
        GameSessionConfig config,
        PlayerSeat actingSeat,
        PlayerSeat currentTurnSeat,
        int threatDial,
        long bannerId,
        String bannerText,
        long bannerExpiresAt,
        MyArrayList<FigureSnapshot> heroes,
        MyArrayList<DeploymentGroupSnapshot> imperialGroups,
        MyArrayList<Boolean> interactableStates,
        int nextSupplyEquipmentIndex,
        MyArrayList<String> offenseResults,
        MyArrayList<String> defenseResults,
        boolean gameEnd,
        boolean rebelsWin) implements Serializable {
}
