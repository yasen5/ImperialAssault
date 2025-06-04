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
import src.Screen.SelectingType;
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
    private static ArrayList<Personnel> availableTargets = new ArrayList<>();
    private static CompletableFuture<Personnel> currentSelected = new CompletableFuture<>();
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
        ArrayList<Hero> heroExhaustOptions = getHeroExhaustOptions();
        ArrayList<DeploymentGroup<? extends Imperial>> imperialExhaustOptions = getImperialExhaustOptions();

        if (!heroExhaustOptions.isEmpty()) {
            Hero activeFigure;
            activeFigure = heroExhaustOptions.remove(InputUtils.getMultipleChoice("Deployment Selection",
                    "Choose deployment card to exhaust", heroExhaustOptions.toArray()));
            activeFigure.setActive(true);
            activeFigure.setExhausted(true);
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
            handleMoves(activeFigure, InputUtils.getNumericChoice(
                    "# of moves you'll use", 0, leftoverMoves));
            activeFigure.setActive(false);
        }
        if (!imperialExhaustOptions.isEmpty()) {
            DeploymentGroup<? extends Imperial> deploymentGroup = imperialExhaustOptions
                    .remove(InputUtils.getMultipleChoice("Deployment Selection",
                            "Choose deployment card to exhaust", imperialExhaustOptions.toArray()));
            deploymentGroup.setExhausted(true);
            for (Imperial imperial : deploymentGroup.getMembers()) {
                imperial.setActive(true);
                ui.repaint();
                int leftoverMoves = 0;
                if (!imperial.stunned()) {
                    leftoverMoves += takeAction(imperial, Actions.MOVE);
                }
                leftoverMoves += takeAction(imperial, false);
                handleMoves(imperial, InputUtils.getNumericChoice(
                        "# of moves you'll use", 0, leftoverMoves));
                imperial.setActive(false);
            }
        }
        ui.repaint();
        if (heroExhaustOptions.isEmpty() && imperialExhaustOptions.isEmpty()) {
            replenishDeployments();
        }
        checkEndGame();
        if (!gameEnd) {
            playRound();
        }
    }

    public void replenishDeployments() {
        for (Hero hero : heroes) {
            hero.setExhausted(false);
        }
        for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
            group.setExhausted(false);
        }
    }

    public void checkEndGame() {
        if (heroes.isEmpty()) {
            endGame(false);
        }
        boolean allDeploymentGroupsEmpty = true;
        for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
            if (!group.isEmpty()) {
                allDeploymentGroupsEmpty = false;
                break;
            }
        }
        if (allDeploymentGroupsEmpty) {
            endGame(true);
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
        availableTargets = availableDefenders(activeFigure, rebel);
        if (availableTargets.size() == 0) {
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
                        "# of moves you'll use:", 0, leftoverMoves);
                leftoverMoves -= movesUsed;
                handleMoves(activeFigure, movesUsed);
            }
            case ATTACK -> {
                handleAttack(activeFigure);
                removeDeadFigures();
            }
            case RECOVER -> activeFigure.dealDamage(-1 * ((Hero) (activeFigure)).getEndurance());
            case SPECIAL -> handleSpecial(activeFigure);
            case INTERACT -> handleInteraction(activeFigure);
        }
        return leftoverMoves;
    }

    public void handleSpecial(Personnel activeFigure) {
        if (activeFigure.specialRequiresSelection()) {
            performSpecial(activeFigure);
        } else {
            activeFigure.performSpecial();
        }
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

    public ArrayList<Hero> getHeroExhaustOptions() {
        ArrayList<Hero> readyDeployments = new ArrayList<>();
        for (Hero hero : heroes) {
            if (!hero.getExhausted()) {
                readyDeployments.add(hero);
            }
        }
        return readyDeployments;
    }

    public ArrayList<DeploymentGroup<? extends Imperial>> getImperialExhaustOptions() {
        ArrayList<DeploymentGroup<? extends Imperial>> readyDeployments = new ArrayList<>();
        for (DeploymentGroup<? extends Imperial> deploymentGroup : imperialDeployments) {
            if (!deploymentGroup.getExhausted()) {
                readyDeployments.add(deploymentGroup);
            }
        }
        return readyDeployments;
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
        currentSelected = new CompletableFuture<>();
        ui.setSelectionType(SelectingType.COMBAT);
        for (Personnel person : availableTargets) {
            person.setPossibleTarget(true);
        }
        ui.repaint();
        Personnel chosenDefender = currentSelected.join();
        for (Personnel person : availableTargets) {
            person.setPossibleTarget(false);
        }
        activeFigure.performAttack(chosenDefender);
    }

    public static boolean setTarget(Personnel defender) {
        if (availableTargets.contains(defender)) {
            currentSelected.complete(defender);
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
        heroes.add(new DialaPassil(new Pos(6, 5)));
        heroes.add(new Gaarkhan(new Pos(1, 4)));
        imperialDeployments
                .add(new DeploymentGroup<StormTrooper>(new Pos[] { new Pos(4, 11), new Pos(4, 12), new Pos(5, 11) },
                        StormTrooper::new, "StormTrooper"));
        imperialDeployments
                .add(new DeploymentGroup<Officer>(new Pos[] { new Pos(7, 11) }, Officer::new, "ImperialOfficer"));
    }

    public static void addOffenseResult(GraphicOffenseDieResult offenseResults) {
        Game.offenseResults.add(offenseResults);
    }

    public static void addDefenseResult(GraphicDefenseDieResult defenseResults) {
        Game.defenseResults.add(defenseResults);
    }

    public static Personnel getPersonnelAtPos(Pos pos) {
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

    public static void removeOffenseDie(int die) {
        offenseResults.remove(die);
    }

    public static void removeDefenseDie(int die) {
        defenseResults.remove(die);
    }

    public static void performSpecial(Personnel activeFigure) {
        currentSelected = new CompletableFuture<>();
        ui.setSelectionType(SelectingType.SPECIAL);
        availableTargets = activeFigure.getSpecialTargets();
        for (Personnel person : availableTargets) {
            person.setPossibleTarget(true);
        }
        ui.repaint();
        Personnel chosenDefender = currentSelected.join();
        for (Personnel person : availableTargets) {
            person.setPossibleTarget(false);
        }
        activeFigure.performSpecial(chosenDefender);
    }

    public static ArrayList<DeploymentGroup<? extends Imperial>> getDeploymentGroups() {
        return imperialDeployments;
    }
}
