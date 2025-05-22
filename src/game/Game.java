package src.game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import javax.swing.SwingUtilities;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import src.Constants;
import src.Screen;
import src.game.Personnel.Directions;

public class Game {
    private static Turn turn;
    private static MapTile mapTile;
    private static ArrayList<DeploymentGroup<? extends Imperial>> imperialDeployments = new ArrayList<>();
    private static ArrayList<Hero> heroDeployments = new ArrayList<>();
    private static Screen ui;

    private static enum Turn {
        REBELS,
        IMPERIALS
    }

    public static record MapTile(BufferedImage img, int[][] tileArray) {

    }

    public Game(Screen ui) {
        this.ui = ui;
        heroDeployments.add(new DialaPassil(new Pos(6, 5), 50, 50));
        imperialDeployments.add(new DeploymentGroup<StormTrooper>(new Pos[] {new Pos(4, 11), new Pos(4, 12), new Pos(5, 11)}, new Pos(500, 500), StormTrooper::new));
        turn = Turn.REBELS;
        BufferedImage mapImg = null;
        try {
            mapImg = ImageIO.read(new File(Constants.baseImgFilePath + "TutorialTile.png"));
        } catch (IOException e) {
            System.out.println("No map image L bozo");
            System.exit(0);
        }
        mapTile = new MapTile(mapImg, Constants.tileMatrix);
    }

    public void drawGame(Graphics g) {
        g.drawImage(mapTile.img(), 0, 0, Constants.tileSize * mapTile.tileArray()[0].length, Constants.tileSize * mapTile
                .tileArray().length, 0, 0, mapTile.img().getWidth(null), mapTile.img().getHeight(null),
                null);
        if (Constants.debug) {
            for (int i = 0; i < mapTile.tileArray().length; i++) {
                for (int j = 0; j < mapTile.tileArray()[0].length; j++) {
                    g.setColor(mapTile.tileArray()[i][j] == 0 ? new Color(255, 0, 0) : new Color(255, 255, 255));
                    g.fillRect((int) (Constants.tileSize * (j + 0.5)), (int) (Constants.tileSize * (i + 0.5)), 3, 3);
                }
            }
        }
        for (Hero hero : heroDeployments) {
            hero.draw(g);
        }
        for (DeploymentGroup<? extends Imperial> deployment : imperialDeployments) {
            deployment.draw(g);
        }
    }

    public void handleKeyboardInput(int keyCode) {
        switch (keyCode) {
            case 81 -> heroDeployments.get(0).move(Directions.UPLEFT);
            case 87 -> heroDeployments.get(0).move(Directions.UP);
            case 69 -> heroDeployments.get(0).move(Directions.UPRIGHT);
            case 65 -> heroDeployments.get(0).move(Directions.LEFT);
            case 68 -> heroDeployments.get(0).move(Directions.RIGHT);
            case 90 -> heroDeployments.get(0).move(Directions.DOWNLEFT);
            case 88 -> heroDeployments.get(0).move(Directions.DOWN);
            case 67 -> heroDeployments.get(0).move(Directions.DOWNRIGHT);
        }
    }

    public void playRound() {
        if (turn == Turn.REBELS) {
            boolean rebelsExhausted = true;
            for (Hero hero : heroDeployments) {
                if (!hero.getExhausted()) {
                    rebelsExhausted = false;
                }
            }
            if (rebelsExhausted) {
                turn = Turn.IMPERIALS;
            } else {
                int selectedIndex = getDeploymentChoice();
                Hero activeFigure = heroDeployments.get(selectedIndex);
                int totalAvailableMoves = activeFigure.getSpeed();
                int movesUsedFirst = displayMovementOptions(totalAvailableMoves);
                totalAvailableMoves -= movesUsedFirst;
                for (int i = 0; i < movesUsedFirst; i++) {
                    handleMove(activeFigure);
                }
                ui.deactiveateButtons();
                // Player chooses from a list of available actions: attack, recover, special
                // action
                // Player uses the rest of their moves (if any)
            }
        }
        if (turn == Turn.IMPERIALS) {
            // Same as for rebels but for imperials
        }
        // playRound();
    }

    public int displayMovementOptions(int maxMoves) {
        int numMovesUsed;
        do {
            numMovesUsed = Integer
                    .valueOf(JOptionPane.showInputDialog("Choose the number of moves you will use first"));
        } while (numMovesUsed < 0 || numMovesUsed > maxMoves);
        return numMovesUsed;
    }

    public String[] getExhaustOptions() {
        ArrayList<String> readyDeployments = new ArrayList<String>();
        if (turn == Turn.REBELS) {
            for (Hero hero : heroDeployments) {
                if (!hero.getExhausted()) {
                    readyDeployments.add(hero.getName());
                }
            }
        } else {
            for (DeploymentGroup<? extends Imperial> deploymentCard : imperialDeployments) {
                if (deploymentCard.getExhausted()) {
                    readyDeployments.add(deploymentCard.getName());
                }
            }
        }
        return readyDeployments.toArray(new String[readyDeployments.size()]);
    }

    public static boolean isSpaceAvailable(Pos pos) {
        for (Hero hero : heroDeployments) {
            if (hero.getPos().equalTo(pos)) {
                return false;
            }
        }
        for (DeploymentGroup<? extends Imperial> depGroup : imperialDeployments) {
            for (Imperial imperial : depGroup.getMembers()) {
                if (imperial.getPos().equalTo(pos)) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getDeploymentChoice() {
        int selectedIndex;
        do {
            selectedIndex = JOptionPane.showOptionDialog(
                    null,
                    "Deployment Selection",
                    "Choose deployment card to exhaust",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    getExhaustOptions(),
                    null);
        } while (selectedIndex < 0 || selectedIndex >= heroDeployments.size());
        return selectedIndex;
    }

    public void handleMove(Hero activeFigure) {
        CompletableFuture<Directions> dir = new CompletableFuture<>();
        ui.setButtonOutput(dir);
        double[] angleRads = { Math.PI / 4.0 };
        SwingUtilities.invokeLater(() -> {
            for (Directions direction : Directions.values()) {
                angleRads[0] += Math.PI / 4;
                // Show the available buttons and disable/hide incorrect ones
                if (activeFigure.canMove(direction)) {
                    ui.moveAndActivateButton(direction,
                            activeFigure.getPos().getX(), activeFigure.getPos().getY(),
                            angleRads[0]);
                } else {
                    ui.deactivateButton(direction);
                }
            }
        });
        Directions chosenDir = dir.join();
        activeFigure.move(chosenDir);
        ui.repaint();
    }
}
