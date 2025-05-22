package src.game;

public class Die {
    public static record OffenseDieResult(int damage, int surge, int accuracy) {
    };

    private static OffenseDieResult[] blueResults, greenResults, redResults, yellowResults;

    public static record DefenseDieResult(int shields, int surgeCancel, boolean dodge) {
    };

    private static DefenseDieResult[] blackResults, whiteResults;

    static {
        blueResults = new OffenseDieResult[] { new OffenseDieResult(1, 0, 5), new OffenseDieResult(2, 0, 3),
                new OffenseDieResult(1, 1, 4),
                new OffenseDieResult(1, 0, 2), new OffenseDieResult(1, 1, 3), new OffenseDieResult(2, 0, 2) };
        greenResults = new OffenseDieResult[] { new OffenseDieResult(2, 0, 2), new OffenseDieResult(2, 1, 2),
                new OffenseDieResult(1, 1, 3),
                new OffenseDieResult(1, 1, 2), new OffenseDieResult(1, 0, 4), new OffenseDieResult(1, 1, 1) };
        redResults = new OffenseDieResult[] { new OffenseDieResult(3, 0, 0), new OffenseDieResult(2, 0, 0),
                new OffenseDieResult(2, 0, 0),
                new OffenseDieResult(2, 1, 0), new OffenseDieResult(1, 1, 0), new OffenseDieResult(1, 0, 0) };
        yellowResults = new OffenseDieResult[] { new OffenseDieResult(1, 0, 0), new OffenseDieResult(1, 2, 0),
                new OffenseDieResult(2, 0, 1),
                new OffenseDieResult(1, 1, 1), new OffenseDieResult(0, 1, 2), new OffenseDieResult(1, 0, 2) };
        blackResults = new DefenseDieResult[] { new DefenseDieResult(3, 0, false), new DefenseDieResult(2, 0, false),
                new DefenseDieResult(2, 0, false), new DefenseDieResult(1, 0, false),
                new DefenseDieResult(1, 0, false), new DefenseDieResult(0, 1, false) };
        whiteResults = new DefenseDieResult[] { new DefenseDieResult(0, 0, true),
                new DefenseDieResult(0, 0, false),
                new DefenseDieResult(1, 0, false), new DefenseDieResult(0, 1, false),
                new DefenseDieResult(1, 1, false), new DefenseDieResult(1, 1, false) };
    }

    public static enum OffenseDieType {
        BLUE,
        GREEN,
        RED,
        YELLOW
    }

    public static enum DefenseDieType {
        BLACK,
        WHITE
    }

    public static OffenseDieResult rollOffense(OffenseDieType type) {
        int face = (int) (Math.random() * 6);
        switch (type) {
            case BLUE:
                return blueResults[face];
            case GREEN:
                return greenResults[face];
            case RED:
                return redResults[face];
            case YELLOW:
                return yellowResults[face];
        }
        return null;
    }

    public static DefenseDieResult rollDefense(DefenseDieType type) {
        int face = (int) (Math.random() * 6);
        switch (type) {
            case BLACK:
                return blackResults[face];
            case WHITE:
                return whiteResults[face];
        }
        return null;
    }
}
