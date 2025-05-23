package src.game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import javax.imageio.ImageIO;

import src.Constants;
import src.Screen;
import src.game.Die.GraphicDefenseDieResult;
import src.game.Die.GraphicOffenseDieResult;
import src.game.Hero.Actions;
import src.game.Personnel.Directions;

public class Game {
    private static Turn turn;
    private static MapTile mapTile;
    private static ArrayList<DeploymentGroup<? extends Imperial>> imperialDeployments = new ArrayList<>();
    private static ArrayList<Hero> heroes = new ArrayList<>();
    private static Screen ui;
    private static ArrayList<GraphicOffenseDieResult> offenseResults = new ArrayList<>();
    private static ArrayList<GraphicDefenseDieResult> defenseResults = new ArrayList<>();

    private static enum Turn {
        REBELS,
        IMPERIALS
    }

    public static record MapTile(BufferedImage img, int[][] tileArray) {

    }

    public Game(Screen ui) {
        this.ui = ui;
        heroes.add(new DialaPassil(new Pos(6, 5)));
        imperialDeployments.add(new DeploymentGroup<StormTrooper>(new Pos[] {new Pos(4, 11), new Pos(4, 12), new Pos(5, 11)}, new Pos(500, 500), StormTrooper::new, "StormTrooper"));
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
        for (Hero hero : heroes) {
            hero.draw(g);
        }
        for (DeploymentGroup<? extends Imperial> deployment : imperialDeployments) {
            deployment.draw(g);
        }
        g.setColor(new Color(255, 255, 255));
        g.drawString("Offense Results:", 960, 1080/2);
        g.drawString("Defense Results:", 960, 810);
        for (int i = 0; i < offenseResults.size(); i++) {
            BufferedImage image = Die.offenseDieFaces.get(offenseResults.get(i));
            int startX = 960 + i * (Constants.tileSize + 10);
            int startY = 1080/2 + 20;
            g.drawImage(image, startX,startY
					,
					startX + Constants.tileSize,
					startY + Constants.tileSize, 0, 0, image.getWidth(null), image.getHeight(null),
					null);
        }
        for (int i = 0; i < defenseResults.size(); i++) {
            BufferedImage image = Die.defenseDieFaces.get(defenseResults.get(i));
            int startX = 960 + i * (Constants.tileSize + 10);
            int startY = 820;
            g.drawImage(image, startX,
                    startY,
                    startX + Constants.tileSize,
                    startY + Constants.tileSize, 0, 0, image.getWidth(null), image.getHeight(null),
                    null);
        }
    }

    public void handleKeyboardInput(int keyCode) {
        switch (keyCode) {
            case 81 -> heroes.get(0).move(Directions.UPLEFT);
            case 87 -> heroes.get(0).move(Directions.UP);
            case 69 -> heroes.get(0).move(Directions.UPRIGHT);
            case 65 -> heroes.get(0).move(Directions.LEFT);
            case 68 -> heroes.get(0).move(Directions.RIGHT);
            case 90 -> heroes.get(0).move(Directions.DOWNLEFT);
            case 88 -> heroes.get(0).move(Directions.DOWN);
            case 67 -> heroes.get(0).move(Directions.DOWNRIGHT);
        }
    }

    public void playRound() {
        if (turn == Turn.REBELS) {
            boolean rebelsExhausted = true;
            for (Hero hero : heroes) {
                if (!hero.getExhausted()) {
                    rebelsExhausted = false;
                }
            }
            if (rebelsExhausted) {
                turn = Turn.IMPERIALS;
            } else {
                int selectedIndex = InputUtils.getMultipleChoice("Deployment Selection", "Choose deployment card to exhaust", getExhaustOptions());
                Hero activeFigure = heroes.get(selectedIndex);
                int totalAvailableMoves = activeFigure.getSpeed();
                int movesUsedFirst = InputUtils.getNumericChoice(
                        "Choose the number of moves you will use first", 0, totalAvailableMoves);
                totalAvailableMoves -= movesUsedFirst;
                for (int i = 0; i < movesUsedFirst; i++) {
                    handleMove(activeFigure);
                }
                ui.deactiveateMovementButtons();
                // Player chooses from a list of available actions: attack, recover, special
                Actions[] availableActions = activeFigure.getActions();
                selectedIndex = InputUtils.getMultipleChoice("Action Selection", "Choose an action to take", availableActions);
                if (availableActions[selectedIndex] == Actions.ATTACK) {
                    handleAttack(activeFigure);
                }
                // action
                // Player uses the rest of their moves (if any)
            }
        }
        if (turn == Turn.IMPERIALS) {
            // Same as for rebels but for imperials
        }
        // playRound();
        ui.repaint();
    }

    public String[] getExhaustOptions() {
        ArrayList<String> readyDeployments = new ArrayList<String>();
        if (turn == Turn.REBELS) {
            for (Hero hero : heroes) {
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
        for (Hero hero : heroes) {
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

    public void handleMove(Hero activeFigure) {
        CompletableFuture<Directions> dir = new CompletableFuture<>();
        ui.setMovementButtonOutput(dir);
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
                    ui.deactivateMovementButton(direction);
                }
            }
        });
        Directions chosenDir = dir.join();
        activeFigure.move(chosenDir);
        ui.repaint();
    }

    public void handleAttack(Personnel activeFigure) {
        CompletableFuture<Personnel> other = new CompletableFuture<>();
        ui.setSelectionButtonOutput(other, activeFigure instanceof Hero);
        Personnel chosenDefender = other.join();
        activeFigure.performAttack(chosenDefender);
    }

    public ArrayList<Imperial> getImperials() {
        ArrayList<Imperial> imperials = new ArrayList<>();
        for (DeploymentGroup<? extends Imperial> depGroup : imperialDeployments) {
            for (Imperial imperial : depGroup.getMembers()) {
                imperials.add(imperial);
            }
        }
        return imperials;
    }

    public ArrayList<Hero> getHeroes() {
        return heroes;
    }

    public void reset() {
        while (!heroes.isEmpty()) {
            heroes.remove(0);
        }
        while (!imperialDeployments.isEmpty()) {
            imperialDeployments.remove(0);
        }
        heroes.add(new DialaPassil(new Pos(6, 5)));
        imperialDeployments.add(new DeploymentGroup<StormTrooper>(
                new Pos[] { new Pos(4, 11), new Pos(4, 12), new Pos(5, 11) }, new Pos(500, 500), StormTrooper::new, "StormTrooper"));
        turn = Turn.REBELS;
    }

    public static void addOffenseResult(GraphicOffenseDieResult offenseResults) {
        Game.offenseResults.add(offenseResults);
    }
    
    public static void addDefenseResult(GraphicDefenseDieResult defenseResults) {
        Game.defenseResults.add(defenseResults);
    }

    public Personnel getPersonnelAtPos(Pos pos) {
        for (DeploymentGroup<? extends Imperial> deployment : imperialDeployments) {
            for (Imperial imperial : deployment.getMembers()) {
                if (imperial.getPos().equalTo(pos)) {
                    return imperial;
                }
            }
        }
        for (Hero hero : heroes) {
            if (hero.getPos().equalTo(pos)) {
                return hero;
            }
        }
        return null;
    }

    public DeploymentCard getDeploymentCard(Pos pos) {
        for (DeploymentGroup<? extends Imperial> deployment : imperialDeployments) {
            for (Imperial imperial : deployment.getMembers()) {
                if (imperial.getPos().equalTo(pos)) {
                    return deployment.getDeploymentCard();
                }
            }
        }
        for (Hero hero : heroes) {
            if (hero.getPos().equalTo(pos)) {
                return hero.getDeploymentCard();
            }
        }
        return null;
    }

    public static void repaintScreen() {
        ui.repaint();
    }
}
