package src.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import src.Constants;

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
            int face = (int) (Math.random() * 6);
            Game.addOffenseResult(new GraphicOffenseDieResult(face, this));
            playSound("/Users/yasen/Documents/Quarter4Project/src/game/sounds/DieRoll.wav");
            return new OffenseRoll(face, results[face]);
        }

        public OffenseDieResult getResult(int face) {
            System.out.println("Dice rolled");
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
            int face = (int) (Math.random() * 6);
            Game.addDefenseResult(new GraphicDefenseDieResult(face, this));
            playSound("/Users/yasen/Documents/Quarter4Project/src/game/sounds/DieRoll.wav");
            return new DefenseRoll(face, results[face]);
        }

        public DefenseDieResult getResult(int face) {
            return results[face];
        }
    }

    public static HashMap<GraphicOffenseDieResult, BufferedImage> offenseDieFaces;
    public static HashMap<GraphicDefenseDieResult, BufferedImage> defenseDieFaces;

    // MANY MANY LINES to get the right images, in the future will use a factory
    static {
        offenseDieFaces = new HashMap<GraphicOffenseDieResult, BufferedImage>();
        defenseDieFaces = new HashMap<GraphicDefenseDieResult, BufferedImage>();
        // BLUE
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(0, OffenseDieType.BLUE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/blue1.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load blue 1 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(1, OffenseDieType.BLUE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/blue2.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load blue 2 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(2, OffenseDieType.BLUE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/blue3.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load blue 3 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(3, OffenseDieType.BLUE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/blue4.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load blue 4 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(4, OffenseDieType.BLUE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/blue5.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load blue 5 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(5, OffenseDieType.BLUE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/blue6.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load blue 6 image");
        }

        // GREEN
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(0, OffenseDieType.GREEN),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/green1.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load green 1 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(1, OffenseDieType.GREEN),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/green2.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load green 2 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(2, OffenseDieType.GREEN),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/green3.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load green 3 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(3, OffenseDieType.GREEN),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/green4.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load green 4 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(4, OffenseDieType.GREEN),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/green5.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load green 5 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(5, OffenseDieType.GREEN),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/green6.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load green 6 image");
        }

        // RED
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(0, OffenseDieType.RED),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/red1.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load red 1 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(1, OffenseDieType.RED),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/red2.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load red 2 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(2, OffenseDieType.RED),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/red3.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load red 3 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(3, OffenseDieType.RED),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/red4.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load red 4 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(4, OffenseDieType.RED),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/red5.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load red 5 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(5, OffenseDieType.RED),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/red6.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load red 6 image");
        }

        // YELLOW
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(0, OffenseDieType.YELLOW),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/yellow1.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load yellow 1 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(1, OffenseDieType.YELLOW),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/yellow2.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load yellow 2 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(2, OffenseDieType.YELLOW),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/yellow3.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load yellow 3 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(3, OffenseDieType.YELLOW),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/yellow4.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load yellow 4 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(4, OffenseDieType.YELLOW),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/yellow5.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load yellow 5 image");
        }
        try {
            offenseDieFaces.put(new GraphicOffenseDieResult(5, OffenseDieType.YELLOW),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/yellow6.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load yellow 6 image");
        }

        // BLACK
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(0, DefenseDieType.BLACK),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/black1.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load black 1 image");
        }
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(1, DefenseDieType.BLACK),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/black2.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load black 2 image");
        }
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(2, DefenseDieType.BLACK),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/black3.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load black 3 image");
        }
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(3, DefenseDieType.BLACK),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/black4.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load black 4 image");
        }
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(4, DefenseDieType.BLACK),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/black5.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load black 5 image");
        }
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(5, DefenseDieType.BLACK),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/black6.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load black 6 image");
        }

        // WHITE
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(0, DefenseDieType.WHITE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/white1.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load white 1 image");
        }
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(1, DefenseDieType.WHITE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/white2.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load white 2 image");
        }
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(2, DefenseDieType.WHITE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/white3.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load white 3 image");
        }
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(3, DefenseDieType.WHITE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/white4.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load white 4 image");
        }
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(4, DefenseDieType.WHITE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/white5.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load white 5 image");
        }
        try {
            defenseDieFaces.put(new GraphicDefenseDieResult(5, DefenseDieType.WHITE),
                    ImageIO.read(new File(Constants.baseImgFilePath + "dice/white6.png")));
        } catch (IOException e) {
            System.out.println("Couldn't load white 6 image");
        }
    }

    public static void playSound(String name) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(new File(name).getAbsoluteFile()));
            clip.start();
        } catch (Exception exc) {
            exc.printStackTrace(System.out);
        }
    }
}