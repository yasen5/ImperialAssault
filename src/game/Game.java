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

import src.Constants;
import src.Screen;
import src.game.Die.GraphicDefenseDieResult;
import src.game.Die.GraphicOffenseDieResult;
import src.game.Personnel.Actions;
import src.game.Personnel.Directions;

public class Game {
    private final MapTile mapTile;
    private static ArrayList<DeploymentGroup<? extends Imperial>> imperialDeployments = new ArrayList<>();
    private static ArrayList<Hero> heroes = new ArrayList<>();
    private static Screen ui;
    private static ArrayList<GraphicOffenseDieResult> offenseResults = new ArrayList<>();
    private static ArrayList<GraphicDefenseDieResult> defenseResults = new ArrayList<>();
    private static ArrayList<Personnel> availableDefenders = new ArrayList<>();
    private static CompletableFuture<Personnel> currentDefender = new CompletableFuture<>();
    @SuppressWarnings("unchecked")
    public static final Interactable<? extends Personnel>[] interactables = (Interactable<? extends Personnel>[]) new Interactable[] {
            new Terminal<Imperial>(new Pos(7, 0), Imperial.class),
            new Terminal<Imperial>(new Pos(0, 3), Imperial.class),
            new Door<Personnel>(new Pos(0, 6), Personnel.class),
            new Door<Personnel>(new Pos(6, 7), Personnel.class)
    };
    private static boolean gameEnd;

    public static record MapTile(BufferedImage img, int[][] tileArray) {
    }

    public Game(Screen ui) {
        this.ui = ui;
        setup();
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
        g.drawImage(mapTile.img(), 0, 0, Constants.tileSize * mapTile.tileArray()[0].length,
                Constants.tileSize * mapTile
                        .tileArray().length,
                0, 0, mapTile.img().getWidth(null), mapTile.img().getHeight(null),
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
        for (int i = 0; i < offenseResults.size(); i++) {
            BufferedImage image = Die.offenseDieFaces.get(offenseResults.get(i));
            int startX = 960 + i * (Die.xSize + 10);
            int startY = 650 + 20;
            g.drawImage(image, startX, startY,
                    startX + Die.xSize,
                    startY + Die.ySize, 0, 0, image.getWidth(null), image.getHeight(null),
                    null);
        }
        for (int i = 0; i < defenseResults.size(); i++) {
            BufferedImage image = Die.defenseDieFaces.get(defenseResults.get(i));
            int startX = 960 + i * (Die.xSize + 10);
            int startY = 820;
            g.drawImage(image, startX,
                    startY,
                    startX + Die.xSize,
                    startY + Die.ySize, 0, 0, image.getWidth(null), image.getHeight(null),
                    null);
        }
        for (Interactable<? extends Personnel> interactable : interactables) {
            interactable.draw(g);
        }
    }

    public void playRound() {
        String[] heroExhaustOptions = getExhaustOptions(true);
        String[] imperialExhaustOptions = getExhaustOptions(false);
        Personnel activeFigure;
        if (heroExhaustOptions.length > 0) {
            activeFigure = heroes.get(InputUtils.getMultipleChoice("Deployment Selection",
                    "Choose deployment card to exhaust", heroExhaustOptions));
            activeFigure.setActive(true);
            ui.repaint();
            int leftoverMoves = 0;
            int numActions = 2;
            if (activeFigure.stunned()) {
                numActions--;
                activeFigure.setStunned(false);
            }
            for (int i = 0; i < numActions; i++) {
                leftoverMoves += takeAction(activeFigure, true);
            }
            // Player uses the rest of their moves (if any)
            handleMoves(activeFigure, leftoverMoves);
            activeFigure.setActive(false);
        }
        removeDeadFigures();
        if (imperialExhaustOptions.length > 0) {
            DeploymentGroup<? extends Imperial> deploymentGroup = imperialDeployments
                    .get(InputUtils.getMultipleChoice("Deployment Selection",
                            "Choose deployment card to exhaust", imperialExhaustOptions));
            for (Imperial imperial : deploymentGroup.getMembers()) {
                imperial.setActive(true);
                ui.repaint();
                int leftoverMoves = 0;
                if (!imperial.stunned()) {
                    leftoverMoves += takeAction(imperial, Actions.MOVE);
                }
                leftoverMoves += takeAction(imperial, false);
                handleMoves(imperial, leftoverMoves);
                imperial.setActive(false);
            }
        }
        removeDeadFigures();
        ui.repaint();
        if (!gameEnd) {
            playRound();
        }
    }

    public void removeDeadFigures() {
        for (int i = 0; i < heroes.size(); i++) {
            if (heroes.get(i).getDead()) {
                heroes.remove(i);
                i--;
            }
        }
        for (int i = 0; i < imperialDeployments.size(); i++) {
            imperialDeployments.get(i).removeDeadFigures();
        }
    }

    // Returns the number of moves gained
    public int takeAction(Personnel activeFigure, boolean rebel) {
        int leftoverMoves = 0;
        ArrayList<Actions> availableActions = new ArrayList<Actions>();
        for (Actions action : activeFigure.getActions()) {
            availableActions.add(action);
        }
        availableDefenders = availableDefenders(activeFigure, rebel);
        if (availableDefenders.size() == 0) {
            availableActions.remove(Actions.ATTACK);
        }
        if (canInteract(activeFigure)) {
            availableActions.add(Actions.INTERACT);
        }
        Actions chosenAction = availableActions.get(InputUtils.getMultipleChoice("Action Selection",
                "Choose an action to take",
                availableActions.toArray()));
        leftoverMoves = takeAction(activeFigure, chosenAction);
        return leftoverMoves;
    }

    public int takeAction(Personnel activeFigure, Actions action) {
        int leftoverMoves = 0;
        switch (action) {
            case MOVE -> {
                leftoverMoves += activeFigure.getSpeed();
                int movesUsed = InputUtils.getNumericChoice(
                        "Choose the number of moves you will use first", 0, leftoverMoves);
                leftoverMoves -= movesUsed;
                handleMoves(activeFigure, movesUsed);
            }
            case ATTACK -> handleAttack(activeFigure);
            case RECOVER -> activeFigure.dealDamage(-1 * ((Hero) (activeFigure)).getEndurance());
            case SPECIAL -> activeFigure.performSpecial();
            case INTERACT -> handleInteraction(activeFigure);
        }
        return leftoverMoves;
    }

    public void handleInteraction(Personnel activeFigure) {
        Pos activePos = activeFigure.getPos();
        for (Directions dir : Directions.values()) {
            Pos nextPos = activePos.getNextPos(dir);
            for (Interactable<? extends Personnel> interactable : interactables) {
                if (nextPos.isEqualTo(interactable.getPos())) {
                    interactable.interact(activeFigure);
                    return;
                }
            }
        }
    }

    public boolean canInteract(Personnel activeFigure) {
        Pos activePos = activeFigure.getPos();
        for (Directions dir : Directions.values()) {
            Pos nextPos = activePos.getNextPos(dir);
            for (Interactable<? extends Personnel> interactable : interactables) {
                if (nextPos.isEqualTo(interactable.getPos()) && interactable.canInteract()) {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<Personnel> availableDefenders(Personnel attacker, boolean rebelAttacker) {
        ArrayList<Personnel> availableDefenders = new ArrayList<>();
        if (rebelAttacker) {
            for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
                for (Imperial member : group.getMembers()) {
                    if (attacker.canAttack(member)) {
                        availableDefenders.add(member);
                    }
                }
            }
        } else {
            for (Hero hero : heroes) {
                if (attacker.canAttack(hero)) {
                    availableDefenders.add(hero);
                }
            }
        }
        return availableDefenders;
    }

    public String[] getExhaustOptions(boolean rebels) {
        ArrayList<String> readyDeployments = new ArrayList<String>();
        if (rebels) {
            for (Hero hero : heroes) {
                if (!hero.getExhausted()) {
                    readyDeployments.add(hero.getName());
                }
            }
        } else {
            for (DeploymentGroup<? extends Imperial> deploymentCard : imperialDeployments) {
                if (!deploymentCard.getExhausted()) {
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

    public static void handleMoves(Personnel activeFigure, int numMoves) {
        for (int j = 0; j < numMoves; j++) {
            handleMove(activeFigure);
        }
        ui.deactiveateMovementButtons();
    }

    public static void handleMove(Personnel activeFigure) {
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

    public static void handleAttack(Personnel activeFigure) {
        currentDefender = new CompletableFuture<>();
        ui.setSelectingCombat(true);
        for (Personnel person : availableDefenders) {
            person.setPossibleTarget(true);
        }
        ui.repaint();
        Personnel chosenDefender = currentDefender.join();
        for (Personnel person : availableDefenders) {
            person.setPossibleTarget(false);
        }
        activeFigure.performAttack(chosenDefender);
    }

    public static boolean setDefender(Personnel defender) {
        if (availableDefenders.contains(defender)) {
            currentDefender.complete(defender);
            return true;
        } else {
            return false;
        }
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
        setup();
        playRound();
    }

    public void setup() {
        heroes.add(new DialaPassil(new Pos(4, 9)));
        heroes.add(new Gaarkhan(new Pos(1, 4)));
        imperialDeployments
                .add(new DeploymentGroup<StormTrooper>(new Pos[] { new Pos(4, 11), new Pos(4, 12), new Pos(5, 11) },
                        StormTrooper::new, "StormTrooper"));
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

    public static void clearDice() {
        while (!offenseResults.isEmpty()) {
            offenseResults.remove(0);
        }
        while (!defenseResults.isEmpty()) {
            defenseResults.remove(0);
        }
    }

    public static void endGame(boolean rebelsWin) {
        ui.endGame(rebelsWin);
        ui.deactiveateMovementButtons();
        gameEnd = true;
    }
}
