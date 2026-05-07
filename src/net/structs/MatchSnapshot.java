package src.net;

import java.io.Serializable;
import java.util.List;

import src.game.GameSessionConfig;

public record MatchSnapshot(
        GameSessionConfig config,
        List<FigureSnapshot> heroes,
        List<DeploymentGroupSnapshot> imperialGroups,
        List<Boolean> interactableStates,
        List<String> offenseResults,
        List<String> defenseResults,
        boolean gameEnd,
        boolean rebelsWin) implements Serializable {
}
