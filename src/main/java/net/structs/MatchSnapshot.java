package net;

import java.io.Serializable;
import java.util.List;

import game.GameSessionConfig;
import game.PlayerSeat;

public record MatchSnapshot(
        GameSessionConfig config,
        PlayerSeat actingSeat,
        long bannerId,
        String bannerText,
        long bannerExpiresAt,
        List<FigureSnapshot> heroes,
        List<DeploymentGroupSnapshot> imperialGroups,
        List<Boolean> interactableStates,
        List<String> offenseResults,
        List<String> defenseResults,
        boolean gameEnd,
        boolean rebelsWin) implements Serializable {
}
