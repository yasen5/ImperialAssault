package game;

import java.awt.image.BufferedImage;
import util.MyHashMap;


import game.Constants;

// This class has a lot of lines, but it basically just contains the set up possible results for different dice and the sounds/images that pop up when you roll a die
public class Die {
    public static final int xSize = Constants.tileSize * 2, ySize = Constants.tileSize * 2;

    public static record OffenseDieResult(int damage, int surge, int accuracy) {
    }

    public static record DefenseDieResult(int shields, int surgeCancel, boolean dodge) {
    }

    public static record OffenseRoll(int face, OffenseDieResult result) {
    }

    public static record DefenseRoll(int face, DefenseDieResult result) {
    }

    public static record GraphicOffenseDieResult(int face, OffenseDieType die) {
    }

    public static record GraphicDefenseDieResult(int face, DefenseDieType die) {
    }

    // Initialize dice results
    public enum OffenseDieType {
        BLUE(new OffenseDieResult[] {
                new OffenseDieResult(0, 1, 2), new OffenseDieResult(1, 0, 2),
                new OffenseDieResult(1, 1, 3), new OffenseDieResult(2, 0, 3),
                new OffenseDieResult(2, 0, 4), new OffenseDieResult(1, 0, 5)
        }),
        GREEN(new OffenseDieResult[] {
                new OffenseDieResult(2, 0, 2), new OffenseDieResult(1, 1, 1),
                new OffenseDieResult(0, 1, 1), new OffenseDieResult(1, 1, 2),
                new OffenseDieResult(2, 0, 2), new OffenseDieResult(2, 0, 3)
        }),
        RED(new OffenseDieResult[] {
                new OffenseDieResult(1, 0, 0), new OffenseDieResult(2, 1, 0),
                new OffenseDieResult(2, 0, 0), new OffenseDieResult(2, 0, 0),
                new OffenseDieResult(3, 0, 0), new OffenseDieResult(3, 0, 0)
        }),
        YELLOW(new OffenseDieResult[] {
                new OffenseDieResult(1, 2, 0), new OffenseDieResult(0, 1, 0),
                new OffenseDieResult(0, 1, 2), new OffenseDieResult(1, 0, 2),
                new OffenseDieResult(1, 1, 1), new OffenseDieResult(2, 0, 1)
        });

        private final OffenseDieResult[] results;

        OffenseDieType(OffenseDieResult[] results) {
            this.results = results;
        }

        // Roll, play the sound, show the die in the game
        public OffenseRoll roll() {
            return roll(null);
        }

        public OffenseRoll roll(Game game) {
            int face = (int) (Math.random() * 6);
            if (game != null) {
                game.addOffenseResult(new GraphicOffenseDieResult(face, this));
            }
            LoaderUtils.playSound("DieRoll");
            return new OffenseRoll(face, results[face]);
        }

        public OffenseDieResult getResult(int face) {
            return results[face];
        }
    }

    // Same exact thing as the offense
    public enum DefenseDieType {
        BLACK(new DefenseDieResult[] {
                new DefenseDieResult(0, 1, false), new DefenseDieResult(1, 0, false),
                new DefenseDieResult(1, 0, false), new DefenseDieResult(2, 0, false),
                new DefenseDieResult(2, 0, false), new DefenseDieResult(3, 0, false)
        }),
        WHITE(new DefenseDieResult[] {
                new DefenseDieResult(0, 0, false), new DefenseDieResult(0, 1, false),
                new DefenseDieResult(1, 0, false), new DefenseDieResult(1, 1, false),
                new DefenseDieResult(1, 1, false), new DefenseDieResult(0, 0, true)
        });

        private final DefenseDieResult[] results;

        DefenseDieType(DefenseDieResult[] results) {
            this.results = results;
        }

        public DefenseRoll roll() {
            return roll(null);
        }

        public DefenseRoll roll(Game game) {
            int face = (int) (Math.random() * 6);
            if (game != null) {
                game.addDefenseResult(new GraphicDefenseDieResult(face, this));
            }
            LoaderUtils.playSound("DieRoll");
            return new DefenseRoll(face, results[face]);
        }

        public DefenseDieResult getResult(int face) {
            return results[face];
        }
    }

    public static MyHashMap<GraphicOffenseDieResult, BufferedImage> offenseDieFaces;
    public static MyHashMap<GraphicDefenseDieResult, BufferedImage> defenseDieFaces;

    static {
        offenseDieFaces = new MyHashMap<GraphicOffenseDieResult, BufferedImage>();
        defenseDieFaces = new MyHashMap<GraphicDefenseDieResult, BufferedImage>();
        for (OffenseDieType die : OffenseDieType.values()) {
            for (int i = 0; i < 6; i++) {
                offenseDieFaces.put(new GraphicOffenseDieResult(i, die),
                        LoaderUtils.getImage("dice/" + die.name().toLowerCase() + (i + 1)));
            }
        }
        for (DefenseDieType die : DefenseDieType.values()) {
            for (int i = 0; i < 6; i++) {
                defenseDieFaces.put(new GraphicDefenseDieResult(i, die),
                        LoaderUtils.getImage("dice/" + die.name().toLowerCase() + (i + 1)));
            }
        }
    }
}
