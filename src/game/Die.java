package src.game;

public class Die {
    public static record OffenseDieResult(int damage, int surge, int accuracy) {
    }

    public static record DefenseDieResult(int shields, int surgeCancel, boolean dodge) {
    }

    public static record OffenseRoll(int face, OffenseDieResult result) {
    }

    public static record DefenseRoll(int face, DefenseDieResult result) {
    }

    public enum OffenseDieType {
        BLUE(new OffenseDieResult[] {
                new OffenseDieResult(1, 0, 5), new OffenseDieResult(2, 0, 3),
                new OffenseDieResult(1, 1, 4), new OffenseDieResult(1, 0, 2),
                new OffenseDieResult(1, 1, 3), new OffenseDieResult(2, 0, 2)
        }),
        GREEN(new OffenseDieResult[] {
                new OffenseDieResult(2, 0, 2), new OffenseDieResult(2, 1, 2),
                new OffenseDieResult(1, 1, 3), new OffenseDieResult(1, 1, 2),
                new OffenseDieResult(1, 0, 4), new OffenseDieResult(1, 1, 1)
        }),
        RED(new OffenseDieResult[] {
                new OffenseDieResult(3, 0, 0), new OffenseDieResult(2, 0, 0),
                new OffenseDieResult(2, 0, 0), new OffenseDieResult(2, 1, 0),
                new OffenseDieResult(1, 1, 0), new OffenseDieResult(1, 0, 0)
        }),
        YELLOW(new OffenseDieResult[] {
                new OffenseDieResult(1, 0, 0), new OffenseDieResult(1, 2, 0),
                new OffenseDieResult(2, 0, 1), new OffenseDieResult(1, 1, 1),
                new OffenseDieResult(0, 1, 2), new OffenseDieResult(1, 0, 2)
        });

        private final OffenseDieResult[] results;

        OffenseDieType(OffenseDieResult[] results) {
            this.results = results;
        }

        public OffenseRoll roll() {
            int face = (int) (Math.random() * 6);
            return new OffenseRoll(face, results[face]);
        }

        public OffenseDieResult getResult(int face) {
            return results[face];
        }
    }

    public enum DefenseDieType {
        BLACK(new DefenseDieResult[] {
                new DefenseDieResult(3, 0, false), new DefenseDieResult(2, 0, false),
                new DefenseDieResult(2, 0, false), new DefenseDieResult(1, 0, false),
                new DefenseDieResult(1, 0, false), new DefenseDieResult(0, 1, false)
        }),
        WHITE(new DefenseDieResult[] {
                new DefenseDieResult(0, 0, true), new DefenseDieResult(0, 0, false),
                new DefenseDieResult(1, 0, false), new DefenseDieResult(0, 1, false),
                new DefenseDieResult(1, 1, false), new DefenseDieResult(1, 1, false)
        });

        private final DefenseDieResult[] results;

        DefenseDieType(DefenseDieResult[] results) {
            this.results = results;
        }

        public DefenseRoll roll() {
            int face = (int) (Math.random() * 6);
            return new DefenseRoll(face, results[face]);
        }

        public DefenseDieResult getResult(int face) {
            return results[face];
        }
    }
}