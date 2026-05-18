package game;

import net.structs.MissionOption;

public record MissionDefinition(
        MissionOption option,
        String displayName,
        int threatLevel,
        int roundLimit,
        Pos[] terminalPositions,
        Pos[] doorPositions,
        Pos[] cratePositions) {
    public static MissionDefinition forOption(MissionOption option) {
        return switch (option) {
            case MISSION_ONE -> new MissionDefinition(option, "Aftermath", 2, 6,
                    new Pos[] { new Pos(7, 0), new Pos(0, 3) },
                    new Pos[] { new Pos(0, 6), new Pos(6, 8) },
                    new Pos[] { new Pos(3, 3) });
            case MISSION_TWO -> new MissionDefinition(option, "A New Threat", 3, 7,
                    new Pos[] { new Pos(7, 0) },
                    new Pos[] { new Pos(0, 6), new Pos(6, 8) },
                    new Pos[] { new Pos(3, 3), new Pos(8, 9) });
        };
    }
}
