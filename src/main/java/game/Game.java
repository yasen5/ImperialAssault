package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import game.Constants;
import game.Constants.WallLine;
import game.Screen;
import game.Screen.SelectingType;
import game.Die.GraphicDefenseDieResult;
import game.Die.GraphicOffenseDieResult;
import game.Personnel.Actions;
import game.Personnel.Directions;
import net.structs.DeploymentGroupSnapshot;
import net.structs.FigureSnapshot;
import net.structs.MatchSnapshot;
import net.structs.GameSessionConfig;
import net.GameDecisionProvider;

public class Game {
    private static final ThreadLocal<Game> currentGame = new ThreadLocal<>();

    private final MapTile mapTile;
    private final ArrayList<DeploymentGroup<? extends Imperial>> imperialDeployments = new ArrayList<>();
    private final ArrayList<Hero> heroes = new ArrayList<>();
    private final ArrayList<GraphicOffenseDieResult> offenseResults = new ArrayList<>();
    private final ArrayList<GraphicDefenseDieResult> defenseResults = new ArrayList<>();
    @SuppressWarnings("unchecked")
    public final Interactable<? extends Personnel>[] interactables = (Interactable<? extends Personnel>[]) new Interactable[] {
            new Terminal<Imperial>(new Pos(7, 0), Imperial.class),
            new Terminal<Imperial>(new Pos(0, 3), Imperial.class),
            new Door<Personnel>(new Pos(0, 6), Personnel.class),
            new Door<Personnel>(new Pos(6, 8), Personnel.class)
    };

    private Screen ui;
    private GameDecisionProvider decisionProvider;
    private Consumer<MatchSnapshot> snapshotListener;
    private CompletableFuture<Personnel> currentSelected = new CompletableFuture<>();
    private ArrayList<Personnel> availableTargets = new ArrayList<>();
    private boolean gameEnd;
    private boolean rebelsWin = true;
    private PlayerSeat actingSeat = PlayerSeat.REBEL_1;
    private long bannerId;
    private String bannerText;
    private long bannerExpiresAt;
    private long lastAppliedBannerId;
    private final GameSessionConfig sessionConfig;
    private final boolean authoritative;

    public static record MapTile(BufferedImage img, int[][] tileArray) {
    }

    public Game(Screen ui) {
        this(ui, new GameSessionConfig(1), null, true);
        this.decisionProvider = new LocalGameDecisionProvider(ui);
    }

    public Game(Screen ui, GameSessionConfig sessionConfig, GameDecisionProvider decisionProvider, boolean authoritative) {
        this.ui = ui;
        this.sessionConfig = sessionConfig;
        this.decisionProvider = decisionProvider;
        this.authoritative = authoritative;
        this.mapTile = new MapTile(LoaderUtils.getImage("TutorialTile"), Constants.tileMatrix);
        if (authoritative) {
            setup();
        }
    }

    public static Game createRemoteView(GameSessionConfig sessionConfig) {
        return new Game(null, sessionConfig, null, false);
    }

    public GameSessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public static Game current() {
        return currentGame.get();
    }

    public boolean hasDecisionProvider() {
        return decisionProvider != null;
    }

    public PlayerSeat getActingSeat() {
        return actingSeat;
    }

    public void setUi(Screen ui) {
        this.ui = ui;
        if (this.authoritative && this.decisionProvider == null) {
            this.decisionProvider = new LocalGameDecisionProvider(ui);
        }
    }

    public void setDecisionProvider(GameDecisionProvider decisionProvider) {
        this.decisionProvider = decisionProvider;
    }

    public void setSnapshotListener(Consumer<MatchSnapshot> snapshotListener) {
        this.snapshotListener = snapshotListener;
    }

    public void drawGame(Graphics g) {
        g.drawImage(mapTile.img(), 0, 0, Constants.tileSize * mapTile.tileArray()[0].length,
                Constants.tileSize * mapTile.tileArray().length,
                0, 0, mapTile.img().getWidth(null), mapTile.img().getHeight(null), null);
        for (Hero hero : heroes) {
            hero.draw(g);
        }
        for (DeploymentGroup<? extends Imperial> deployment : imperialDeployments) {
            deployment.draw(g);
        }
        drawDiceSection(g, offenseResults, true);
        drawDiceSection(g, defenseResults, false);
        for (Interactable<? extends Personnel> interactable : interactables) {
            interactable.draw(g);
        }
    }

    public int getMapDrawWidth() {
        return Constants.tileSize * mapTile.tileArray()[0].length;
    }

    private <T> void drawDiceSection(Graphics g, ArrayList<T> dice, boolean offense) {
        if (dice.isEmpty()) {
            return;
        }
        int startX = 960;
        int startY = offense ? 670 : 820;
        int rowSpacing = Die.ySize + 14;
        int columnSpacing = Die.xSize + 10;
        if (ui != null) {
            startX = ui.getSidebarDiceX();
            startY = offense ? ui.getDiceStartY() : ui.getDiceStartY() + 150;
        }
        int availableWidth = ui != null ? ui.getSidebarDetailWidth() : 300;
        int maxPerRow = Math.max(1, (availableWidth + 10) / columnSpacing);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setColor(new Color(0, 0, 0, 90));
        int rows = (dice.size() + maxPerRow - 1) / maxPerRow;
        int panelHeight = rows * rowSpacing + 22;
        g2.fillRoundRect(startX - 12, startY - 18, Math.min(availableWidth + 24, maxPerRow * columnSpacing + 14),
                panelHeight, 22, 22);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawRoundRect(startX - 12, startY - 18, Math.min(availableWidth + 24, maxPerRow * columnSpacing + 14),
                panelHeight, 22, 22);
        for (int i = 0; i < dice.size(); i++) {
            BufferedImage image = offense ? Die.offenseDieFaces.get(offenseResults.get(i))
                    : Die.defenseDieFaces.get(defenseResults.get(i));
            int column = i % maxPerRow;
            int row = i / maxPerRow;
            int x = startX + column * columnSpacing;
            int y = startY + row * rowSpacing;
            g2.drawImage(image, x, y, x + Die.xSize, y + Die.ySize, 0, 0, image.getWidth(null),
                    image.getHeight(null), null);
        }
        g2.dispose();
    }

    public void playRound() {
        currentGame.set(this);
        try {
            while (!gameEnd) {
                playCycle();
            }
        } finally {
            currentGame.remove();
        }
    }

    private void playCycle() {
        for (PlayerSeat rebelSeat : sessionConfig.rebelTurnOrder()) {
            ArrayList<Hero> seatOptions = getHeroExhaustOptions(rebelSeat);
            if (!seatOptions.isEmpty()) {
                announceTurnStart(rebelSeat);
                activateHero(rebelSeat, seatOptions);
                if (gameEnd) {
                    return;
                }
                announceTurnEnd(rebelSeat);
            }
        }
        ArrayList<DeploymentGroup<? extends Imperial>> imperialExhaustOptions = getImperialExhaustOptions();
        if (!imperialExhaustOptions.isEmpty()) {
            actingSeat = PlayerSeat.IMPERIAL;
            updateTurnStatus();
            announceTurnStart(PlayerSeat.IMPERIAL);
            activateImperials(imperialExhaustOptions);
            if (gameEnd) {
                return;
            }
            announceTurnEnd(PlayerSeat.IMPERIAL);
        }
        repaint();
        if (getHeroExhaustOptions().isEmpty() && imperialExhaustOptions.isEmpty()) {
            replenishDeployments();
        }
        checkEndGame();
    }

    private void activateHero(PlayerSeat rebelSeat, ArrayList<Hero> seatOptions) {
        actingSeat = rebelSeat;
        updateTurnStatus();
        Hero activeFigure = seatOptions
                .remove(promptMultipleChoice(rebelSeat, "Deployment Selection",
                        "Choose deployment card to exhaust", seatOptions.toArray()));
        activeFigure.setActive(true);
        activeFigure.setExhausted(true);
        repaint();
        int leftoverMoves = 0;
        int numActions = 2;
        if (activeFigure.stunned()) {
            numActions--;
            activeFigure.setStunned(false);
        }
        for (int i = 0; i < numActions; i++) {
            leftoverMoves += takeAction(activeFigure, true);
            checkEndGame();
            if (gameEnd) {
                return;
            }
        }
        handleMovesInternal(activeFigure, promptNumericChoice(rebelSeat, "# of moves you'll use", 0, leftoverMoves));
        activeFigure.setActive(false);
        repaint();
    }

    private void activateImperials(ArrayList<DeploymentGroup<? extends Imperial>> imperialExhaustOptions) {
        actingSeat = PlayerSeat.IMPERIAL;
        updateTurnStatus();
        DeploymentGroup<? extends Imperial> deploymentGroup = imperialExhaustOptions
                .remove(promptMultipleChoice(PlayerSeat.IMPERIAL, "Deployment Selection",
                        "Choose deployment card to exhaust", imperialExhaustOptions.toArray()));
        deploymentGroup.setExhausted(true);
        repaint();
        for (Imperial imperial : deploymentGroup.getMembers()) {
            imperial.setActive(true);
            repaint();
            int leftoverMoves = 0;
            if (!imperial.stunned()) {
                leftoverMoves += takeAction(imperial, Actions.MOVE);
            }
            leftoverMoves += takeAction(imperial, false);
            checkEndGame();
            if (gameEnd) {
                return;
            }
            handleMovesInternal(imperial,
                    promptNumericChoice(PlayerSeat.IMPERIAL, "# of moves you'll use", 0, leftoverMoves));
            imperial.setActive(false);
        }
        repaint();
    }

    public void replenishDeployments() {
        for (Hero hero : heroes) {
            hero.setExhausted(false);
        }
        for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
            group.setExhausted(false);
        }
        repaint();
    }

    public void checkEndGame() {
        if (heroes.isEmpty()) {
            endGameInternal(false);
        }
        boolean allDeploymentGroupsEmpty = true;
        for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
            if (!group.isEmpty()) {
                allDeploymentGroupsEmpty = false;
                break;
            }
        }
        if (allDeploymentGroupsEmpty) {
            endGameInternal(true);
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
        repaint();
    }

    public int takeAction(Personnel activeFigure, boolean rebel) {
        int leftoverMoves = 0;
        ArrayList<Actions> availableActions = new ArrayList<>();
        availableActions.addAll(activeFigure.getActions());
        availableTargets = availableDefenders(activeFigure, rebel);
        if (availableTargets.size() == 0) {
            availableActions.remove(Actions.ATTACK);
        }
        if (canInteract(activeFigure)) {
            availableActions.add(Actions.INTERACT);
        }
        Actions chosenAction = availableActions.get(promptMultipleChoice(activeFigure.getOwnerSeat(), "Action Selection",
                "Choose an action to take", availableActions.toArray()));
        leftoverMoves = takeAction(activeFigure, chosenAction);
        return leftoverMoves;
    }

    public int takeAction(Personnel activeFigure, Actions action) {
        int leftoverMoves = 0;
        switch (action) {
            case MOVE -> {
                leftoverMoves += activeFigure.getSpeed();
                int movesUsed = promptNumericChoice(activeFigure.getOwnerSeat(), "# of moves you'll use:", 0,
                        leftoverMoves);
                leftoverMoves -= movesUsed;
                handleMovesInternal(activeFigure, movesUsed);
            }
            case ATTACK -> {
                handleAttackInternal(activeFigure);
                removeDeadFigures();
            }
            case RECOVER -> activeFigure.dealDamage(-1 * ((Hero) activeFigure).getEndurance());
            case SPECIAL -> handleSpecial(activeFigure);
            case INTERACT -> handleInteraction(activeFigure);
        }
        return leftoverMoves;
    }

    public void handleSpecial(Personnel activeFigure) {
        if (activeFigure.specialRequiresSelection()) {
            performSpecialInternal(activeFigure);
        } else {
            activeFigure.performSpecial();
            repaint();
        }
    }

    public void handleInteraction(Personnel activeFigure) {
        getAdjacentInteractable(activeFigure).interact(activeFigure);
        repaint();
    }

    public Interactable<? extends Personnel> getAdjacentInteractable(Personnel activeFigure) {
        Pos activePos = activeFigure.getPos();
        for (Directions dir : Directions.values()) {
            Pos nextPos = activePos.getNextPos(dir);
            for (Interactable<? extends Personnel> interactable : interactables) {
                if (!interactable.canInteract()) {
                    continue;
                }
                if (interactable.blocking()) {
                    for (WallLine wallLine : interactable.getWallLines()) {
                        if (wallLine.intersects(activePos.getCenterPos(), nextPos.getCenterPos(), true)) {
                            return interactable;
                        }
                    }
                } else if (nextPos.equalTo(interactable.getPos())) {
                    return interactable;
                }
            }
        }
        return null;
    }

    public boolean canInteract(Personnel activeFigure) {
        return getAdjacentInteractable(activeFigure) != null;
    }

    public ArrayList<Personnel> availableDefenders(Personnel attacker, boolean rebelAttacker) {
        ArrayList<Personnel> defenders = new ArrayList<>();
        if (rebelAttacker) {
            for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
                for (Imperial member : group.getMembers()) {
                    if (attacker.canAttack(member)) {
                        defenders.add(member);
                    }
                }
            }
        } else {
            for (Hero hero : heroes) {
                if (attacker.canAttack(hero)) {
                    defenders.add(hero);
                }
            }
        }
        return defenders;
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

    public ArrayList<Hero> getHeroExhaustOptions(PlayerSeat seat) {
        ArrayList<Hero> readyDeployments = new ArrayList<>();
        for (Hero hero : heroes) {
            if (!hero.getExhausted() && hero.getOwnerSeat() == seat) {
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

    public boolean isSpaceAvailableInternal(Pos pos) {
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

    private void handleMovesInternal(Personnel activeFigure, int numMoves) {
        for (int j = 0; j < numMoves; j++) {
            handleMoveInternal(activeFigure);
        }
        if (ui != null) {
            ui.deactiveateMovementButtons();
        }
        repaint();
    }

    private void handleMoveInternal(Personnel activeFigure) {
        ArrayList<Directions> availableDirections = new ArrayList<>();
        for (Directions direction : Directions.values()) {
            if (activeFigure.canMove(direction)) {
                availableDirections.add(direction);
            }
        }
        Directions chosenDir = decisionProvider.chooseDirection(activeFigure.getOwnerSeat(), activeFigure,
                availableDirections);
        activeFigure.move(chosenDir);
        repaint();
    }

    private void handleAttackInternal(Personnel activeFigure) {
        currentSelected = new CompletableFuture<>();
        for (Personnel person : availableTargets) {
            person.setPossibleTarget(true);
        }
        repaint();
        Personnel chosenDefender = decisionProvider.chooseTarget(activeFigure.getOwnerSeat(), SelectingType.COMBAT,
                new ArrayList<>(availableTargets));
        for (Personnel person : availableTargets) {
            person.setPossibleTarget(false);
        }
        activeFigure.performAttack(chosenDefender);
        repaint();
    }

    public boolean trySetTarget(Personnel defender) {
        if (availableTargets.contains(defender)) {
            currentSelected.complete(defender);
            return true;
        }
        return false;
    }

    public ArrayList<Imperial> getImperials() {
        ArrayList<Imperial> imperials = new ArrayList<>();
        for (DeploymentGroup<? extends Imperial> depGroup : imperialDeployments) {
            imperials.addAll(depGroup.getMembers());
        }
        return imperials;
    }

    public ArrayList<Hero> getHeroes() {
        return heroes;
    }

    public void reset() {
        currentGame.set(this);
        try {
            gameEnd = false;
            rebelsWin = true;
            heroes.clear();
            imperialDeployments.clear();
            clearDiceInternal();
            for (Interactable<? extends Personnel> interactable : interactables) {
                interactable.applySnapshotState(true);
            }
            setup();
            playRound();
        } finally {
            currentGame.remove();
        }
    }

    public void setup() {
        heroes.clear();
        imperialDeployments.clear();
        Hero diala = new DialaPassil(new Pos(6, 5));
        Hero gaarkhan = new Gaarkhan(new Pos(1, 4));
        configureHero(diala, "hero-diala", PlayerSeat.REBEL_1);
        configureHero(gaarkhan, "hero-gaarkhan",
                sessionConfig.rebelPlayerCount() == 1 ? PlayerSeat.REBEL_1 : PlayerSeat.REBEL_2);
        heroes.add(diala);
        heroes.add(gaarkhan);

        DeploymentGroup<StormTrooper> troopers = new DeploymentGroup<>(
                new Pos[] { new Pos(4, 11), new Pos(4, 12), new Pos(5, 11) },
                StormTrooper::new, "StormTrooper");
        configureDeploymentGroup(troopers, "imperial-stormtroopers", PlayerSeat.IMPERIAL);
        DeploymentGroup<Officer> officers = new DeploymentGroup<>(
                new Pos[] { new Pos(7, 11) }, Officer::new, "ImperialOfficer");
        configureDeploymentGroup(officers, "imperial-officer", PlayerSeat.IMPERIAL);
        imperialDeployments.add(troopers);
        imperialDeployments.add(officers);
        repaint();
    }

    private void configureHero(Hero hero, String id, PlayerSeat seat) {
        hero.setId(id);
        hero.setOwnerSeat(seat);
    }

    private void configureDeploymentGroup(DeploymentGroup<? extends Imperial> group, String id, PlayerSeat seat) {
        group.setId(id);
        group.setOwnerSeat(seat);
        int index = 0;
        for (Imperial imperial : group.getMembers()) {
            imperial.setId(id + "-member-" + index);
            imperial.setOwnerSeat(seat);
            index++;
        }
    }

    public void addOffenseResultInternal(GraphicOffenseDieResult offenseResult) {
        offenseResults.add(offenseResult);
        repaint();
    }

    public void addDefenseResultInternal(GraphicDefenseDieResult defenseResult) {
        defenseResults.add(defenseResult);
        repaint();
    }

    public Personnel getPersonnelAtPosInternal(Pos pos) {
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

    public Personnel getPersonnelById(String id) {
        for (Hero hero : heroes) {
            if (id.equals(hero.getId())) {
                return hero;
            }
        }
        for (DeploymentGroup<? extends Imperial> deployment : imperialDeployments) {
            for (Imperial imperial : deployment.getMembers()) {
                if (id.equals(imperial.getId())) {
                    return imperial;
                }
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

    public void repaint() {
        if (ui != null) {
            ui.setTurnStatus(actingSeat);
            ui.repaint();
        }
        if (snapshotListener != null) {
            snapshotListener.accept(createSnapshot());
        }
    }

    public void clearDiceInternal() {
        offenseResults.clear();
        defenseResults.clear();
        repaint();
    }

    private void endGameInternal(boolean rebelsWin) {
        this.rebelsWin = rebelsWin;
        if (ui != null) {
            ui.endGame(rebelsWin);
            ui.deactiveateMovementButtons();
        }
        gameEnd = true;
        repaint();
    }

    public void removeOffenseDieInternal(int die) {
        offenseResults.remove(die);
        repaint();
    }

    public void removeDefenseDieInternal(int die) {
        defenseResults.remove(die);
        repaint();
    }

    private void performSpecialInternal(Personnel activeFigure) {
        availableTargets = activeFigure.getSpecialTargets();
        for (Personnel person : availableTargets) {
            person.setPossibleTarget(true);
        }
        repaint();
        Personnel chosenDefender = decisionProvider.chooseTarget(activeFigure.getOwnerSeat(), SelectingType.SPECIAL,
                new ArrayList<>(availableTargets));
        for (Personnel person : availableTargets) {
            person.setPossibleTarget(false);
        }
        activeFigure.performSpecial(chosenDefender);
        repaint();
    }

    public ArrayList<DeploymentGroup<? extends Imperial>> getDeploymentGroupsInternal() {
        return imperialDeployments;
    }

    public int promptMultipleChoice(PlayerSeat seat, String name, String explanation, Object[] options) {
        actingSeat = seat;
        return decisionProvider.chooseMultipleChoice(seat, name, explanation, options);
    }

    public boolean promptYesNo(PlayerSeat seat, String name, String explanation) {
        actingSeat = seat;
        return decisionProvider.chooseYesNo(seat, name, explanation);
    }

    public int promptNumericChoice(PlayerSeat seat, String name, int minValue, int maxValue) {
        actingSeat = seat;
        return decisionProvider.chooseNumericChoice(seat, name, minValue, maxValue);
    }

    public MatchSnapshot createSnapshot() {
        ArrayList<FigureSnapshot> heroSnapshots = new ArrayList<>();
        for (Hero hero : heroes) {
            heroSnapshots.add(new FigureSnapshot(hero.getId(), hero.getName(), hero.getPos().getX(), hero.getPos().getY(),
                    hero.getHealth(), hero.getStrain(), hero.stunned(), hero.focused, hero.isActive(),
                    hero.isPossibleTarget(), hero.getExhausted(), hero.getOwnerSeat()));
        }
        ArrayList<DeploymentGroupSnapshot> groupSnapshots = new ArrayList<>();
        for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
            ArrayList<FigureSnapshot> members = new ArrayList<>();
            for (Imperial imperial : group.getMembers()) {
                members.add(new FigureSnapshot(imperial.getId(), imperial.getName(), imperial.getPos().getX(),
                        imperial.getPos().getY(), imperial.getHealth(), imperial.getStrain(), imperial.stunned(),
                        imperial.focused, imperial.isActive(), imperial.isPossibleTarget(), false,
                        imperial.getOwnerSeat()));
            }
            groupSnapshots.add(new DeploymentGroupSnapshot(group.getId(), group.toString(), group.getExhausted(),
                    group.getOwnerSeat(), members));
        }
        ArrayList<Boolean> interactableStates = new ArrayList<>();
        for (Interactable<? extends Personnel> interactable : interactables) {
            interactableStates.add(interactable.snapshotState());
        }
        ArrayList<String> offense = new ArrayList<>();
        for (GraphicOffenseDieResult die : offenseResults) {
            offense.add(die.die().name() + ":" + die.face());
        }
        ArrayList<String> defense = new ArrayList<>();
        for (GraphicDefenseDieResult die : defenseResults) {
            defense.add(die.die().name() + ":" + die.face());
        }
        return new MatchSnapshot(sessionConfig, actingSeat, bannerId, bannerText, bannerExpiresAt, heroSnapshots,
                groupSnapshots, interactableStates, offense, defense, gameEnd, rebelsWin);
    }

    public void loadSnapshot(MatchSnapshot snapshot) {
        heroes.clear();
        imperialDeployments.clear();
        clearDiceInternal();
        for (FigureSnapshot heroSnapshot : snapshot.heroes()) {
            Hero hero = createHero(heroSnapshot);
            applyFigureSnapshot(hero, heroSnapshot);
            heroes.add(hero);
        }
        for (DeploymentGroupSnapshot groupSnapshot : snapshot.imperialGroups()) {
            DeploymentGroup<? extends Imperial> group = createGroup(groupSnapshot);
            group.setId(groupSnapshot.id());
            group.setOwnerSeat(groupSnapshot.ownerSeat());
            group.setExhausted(groupSnapshot.exhausted());
            for (int i = 0; i < group.getMembers().size() && i < groupSnapshot.members().size(); i++) {
                applyFigureSnapshot(group.getMembers().get(i), groupSnapshot.members().get(i));
            }
            imperialDeployments.add(group);
        }
        for (int i = 0; i < interactables.length && i < snapshot.interactableStates().size(); i++) {
            interactables[i].applySnapshotState(snapshot.interactableStates().get(i));
        }
        for (String die : snapshot.offenseResults()) {
            String[] parts = die.split(":");
            offenseResults.add(new GraphicOffenseDieResult(Integer.parseInt(parts[1]),
                    Die.OffenseDieType.valueOf(parts[0])));
        }
        for (String die : snapshot.defenseResults()) {
            String[] parts = die.split(":");
            defenseResults.add(new GraphicDefenseDieResult(Integer.parseInt(parts[1]),
                    Die.DefenseDieType.valueOf(parts[0])));
        }
        this.gameEnd = snapshot.gameEnd();
        this.rebelsWin = snapshot.rebelsWin();
        this.actingSeat = snapshot.actingSeat();
        if (snapshot.bannerId() > lastAppliedBannerId && ui != null) {
            lastAppliedBannerId = snapshot.bannerId();
            long remaining = snapshot.bannerExpiresAt() - System.currentTimeMillis();
            ui.showBannerFromSnapshot(snapshot.bannerText(), remaining);
        }
        if (ui != null) {
            ui.setTurnStatus(actingSeat);
        }
        repaint();
    }

    private void updateTurnStatus() {
        if (ui != null) {
            ui.setTurnStatus(actingSeat);
        }
    }

    private void announceTurnStart(PlayerSeat seat) {
        triggerBanner(formatSeat(seat) + " turn begins");
        if (ui != null) {
            ui.setTurnStatus(actingSeat);
        }
    }

    private void announceTurnEnd(PlayerSeat seat) {
        triggerBanner(formatSeat(seat) + " turn ends");
        if (ui != null) {
            ui.setTurnStatus(actingSeat);
        }
    }

    private void triggerBanner(String text) {
        bannerId++;
        bannerText = text;
        bannerExpiresAt = System.currentTimeMillis() + 1400L;
        if (ui != null) {
            ui.showBanner(text);
        }
        repaint();
    }

    private String formatSeat(PlayerSeat seat) {
        return switch (seat) {
            case IMPERIAL -> "Imperial";
            case REBEL_1 -> "Rebel 1";
            case REBEL_2 -> "Rebel 2";
        };
    }

    private Hero createHero(FigureSnapshot heroSnapshot) {
        return switch (heroSnapshot.name()) {
            case "DialaPassil" -> new DialaPassil(new Pos(heroSnapshot.x(), heroSnapshot.y()));
            case "Gaarkhan" -> new Gaarkhan(new Pos(heroSnapshot.x(), heroSnapshot.y()));
            default -> throw new IllegalArgumentException("Unknown hero " + heroSnapshot.name());
        };
    }

    private DeploymentGroup<? extends Imperial> createGroup(DeploymentGroupSnapshot groupSnapshot) {
        Pos[] poses = new Pos[groupSnapshot.members().size()];
        for (int i = 0; i < poses.length; i++) {
            FigureSnapshot member = groupSnapshot.members().get(i);
            poses[i] = new Pos(member.x(), member.y());
        }
        return switch (groupSnapshot.name()) {
            case "StormTrooper" -> new DeploymentGroup<StormTrooper>(poses, StormTrooper::new, "StormTrooper");
            case "ImperialOfficer" -> new DeploymentGroup<Officer>(poses, Officer::new, "ImperialOfficer");
            default -> throw new IllegalArgumentException("Unknown group " + groupSnapshot.name());
        };
    }

    private void applyFigureSnapshot(Personnel personnel, FigureSnapshot snapshot) {
        personnel.setId(snapshot.id());
        personnel.setOwnerSeat(snapshot.ownerSeat());
        personnel.setPos(new Pos(snapshot.x(), snapshot.y()));
        personnel.setHealth(snapshot.health());
        personnel.setStrain(snapshot.strain());
        personnel.setStunned(snapshot.stunned());
        personnel.setFocused(snapshot.focused());
        personnel.setActive(snapshot.active());
        personnel.setPossibleTarget(snapshot.possibleTarget());
        if (personnel instanceof Hero hero) {
            hero.setExhausted(snapshot.exhausted());
        }
    }

    public boolean isGameEnd() {
        return gameEnd;
    }

    public boolean rebelsWin() {
        return rebelsWin;
    }

    public static boolean isSpaceAvailable(Pos pos) {
        return current().isSpaceAvailableInternal(pos);
    }

    public static void addOffenseResult(GraphicOffenseDieResult offenseResult) {
        current().addOffenseResultInternal(offenseResult);
    }

    public static void addDefenseResult(GraphicDefenseDieResult defenseResult) {
        current().addDefenseResultInternal(defenseResult);
    }

    public static void handleMoves(Personnel activeFigure, int numMoves) {
        current().handleMovesInternal(activeFigure, numMoves);
    }

    public static Personnel getPersonnelAtPos(Pos pos) {
        return current().getPersonnelAtPosInternal(pos);
    }

    public static void handleAttack(Personnel activeFigure) {
        current().handleAttackInternal(activeFigure);
    }

    public static boolean setTarget(Personnel defender) {
        return current().trySetTarget(defender);
    }

    public static void repaintScreen() {
        current().repaint();
    }

    public static void clearDice() {
        current().clearDiceInternal();
    }

    public static void endGame(boolean rebelsWin) {
        current().endGameInternal(rebelsWin);
    }

    public static void removeOffenseDie(int die) {
        current().removeOffenseDieInternal(die);
    }

    public static void removeDefenseDie(int die) {
        current().removeDefenseDieInternal(die);
    }

    public static void performSpecial(Personnel activeFigure) {
        current().performSpecialInternal(activeFigure);
    }

    public static ArrayList<DeploymentGroup<? extends Imperial>> getDeploymentGroups() {
        return current().getDeploymentGroupsInternal();
    }

    public static Interactable<? extends Personnel>[] getInteractables() {
        return current().interactables;
    }

    public static void setAvailableTargets(ArrayList<Personnel> availableTargets) {
        current().availableTargets = availableTargets;
    }

    public static CompletableFuture<Personnel> getCurrentSelection() {
        return current().currentSelected;
    }

    public static void setCurrentSelection(CompletableFuture<Personnel> currentSelected) {
        current().currentSelected = currentSelected;
    }
}
