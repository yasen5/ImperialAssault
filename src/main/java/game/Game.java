package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import util.MyArrayList;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import game.Constants.WallLine;
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
  private final MapTile mapTile;
  private final MyArrayList<DeploymentGroup<? extends Imperial>> imperialDeployments = new MyArrayList<>();
  private final MyArrayList<Hero> heroes = new MyArrayList<>();
  private final MyArrayList<GraphicOffenseDieResult> offenseResults = new MyArrayList<>();
  private final MyArrayList<GraphicDefenseDieResult> defenseResults = new MyArrayList<>();
  @SuppressWarnings("unchecked")
  public final Interactable<? extends Personnel>[] interactables = (Interactable<? extends Personnel>[]) new Interactable[] {
      new Terminal<Imperial>(new Pos(7, 0), Imperial.class),
      new Terminal<Imperial>(new Pos(0, 3), Imperial.class),
      new Door<Personnel>(new Pos(0, 6), Personnel.class),
      new Door<Personnel>(new Pos(6, 8), Personnel.class),
      new SupplyBox(new Pos(3, 3))
  };

  private GameUi ui;
  private GameDecisionProvider decisionProvider;
  private Consumer<MatchSnapshot> snapshotListener;
  private CompletableFuture<Personnel> currentSelected = new CompletableFuture<>();
  private MyArrayList<Personnel> availableTargets = new MyArrayList<>();
  private int threatDial = 0;
  private boolean gameEnd;
  private boolean rebelsWin = true;
  private PlayerSeat actingSeat = PlayerSeat.REBEL_1;
  private PlayerSeat currentTurnSeat = PlayerSeat.REBEL_1;
  private volatile boolean advanceStatusPhaseRequested;
  private volatile boolean statusPhaseInProgress;
  private volatile boolean abortStatusPhasePrompts;
  private volatile Runnable activePromptCancelAction = () -> {
  };
  private long bannerId;
  private String bannerText;
  private long bannerExpiresAt;
  private long lastAppliedBannerId;
  private int nextSupplyEquipmentIndex;
  private final GameSessionConfig sessionConfig;

  public static record MapTile(BufferedImage img, int[][] tileArray) {
  }

  public Game(GameUi ui) {
    this(ui, new GameSessionConfig(1), null, true);
  }

  public Game(GameUi ui, GameSessionConfig sessionConfig, GameDecisionProvider decisionProvider,
      boolean authoritative) {
    this.ui = ui;
    this.sessionConfig = sessionConfig;
    this.decisionProvider = decisionProvider;
    this.mapTile = new MapTile(LoaderUtils.getImage("TutorialTile"), Constants.tileMatrix);
    if (authoritative) {
      setup();
    }
  }

  public boolean hasDecisionProvider() {
    return decisionProvider != null;
  }

  public PlayerSeat getActingSeat() {
    return actingSeat;
  }

  public void setUi(GameUi ui) {
    this.ui = ui;
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
    drawThreatHud(g);
    drawDiceSection(g, offenseResults, true);
    drawDiceSection(g, defenseResults, false);
    for (Interactable<? extends Personnel> interactable : interactables) {
      interactable.draw(g);
    }
  }

  public int getMapDrawWidth() {
    return Constants.tileSize * mapTile.tileArray()[0].length;
  }

  private <T> void drawDiceSection(Graphics g, MyArrayList<T> dice, boolean offense) {
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
    while (!gameEnd) {
      playCycle();
    }
  }

  private void playCycle() {
    try {
      if (advanceStatusPhaseRequested) {
        finishRoundTransition();
        return;
      }
      MyArrayList<PlayerSeat> turnOrder = currentRoundTurnOrder();
      int startIndex = getTurnIndex(currentTurnSeat, turnOrder);
      for (int i = startIndex; i < turnOrder.size(); i++) {
        PlayerSeat seat = turnOrder.get(i);
        if (seat == PlayerSeat.IMPERIAL) {
          MyArrayList<DeploymentGroup<? extends Imperial>> imperialExhaustOptions = getImperialExhaustOptions();
          if (!imperialExhaustOptions.isEmpty()) {
            startTurn(PlayerSeat.IMPERIAL);
            activateImperials(imperialExhaustOptions);
            if (gameEnd) {
              return;
            }
            endTurn(PlayerSeat.IMPERIAL);
          }
        } else {
          MyArrayList<Hero> seatOptions = getHeroExhaustOptions(seat);
          if (!seatOptions.isEmpty()) {
            startTurn(seat);
            activateHero(seat, seatOptions);
            if (gameEnd) {
              return;
            }
            endTurn(seat);
          }
        }
      }
      repaint();
      if (getHeroExhaustOptions().isEmpty() && getImperialExhaustOptions().isEmpty()) {
        resolveStatusPhase();
      }
      checkEndGame();
    } catch (CancellationException ex) {
      if (advanceStatusPhaseRequested) {
        Thread.interrupted();
        finishRoundTransition();
        return;
      }
      throw ex;
    }
  }

  private void activateHero(PlayerSeat rebelSeat, MyArrayList<Hero> seatOptions) {
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
    handlePendingMoves(activeFigure, rebelSeat, leftoverMoves);
    activeFigure.setActive(false);
    repaint();
  }

  private void activateImperials(MyArrayList<DeploymentGroup<? extends Imperial>> imperialExhaustOptions) {
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
      handlePendingMoves(imperial, PlayerSeat.IMPERIAL, leftoverMoves);
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

  private void resolveStatusPhase() {
    statusPhaseInProgress = true;
    advanceStatusPhaseRequested = false;
    abortStatusPhasePrompts = false;
    try {
      currentTurnSeat = firstTurnSeat();
      threatDial++;
      triggerBanner("Threat dial increased to " + threatDial);
      replenishDeployments();
      resolveImperialOptionalDeployments();
    } finally {
      statusPhaseInProgress = false;
    }
  }

  public void increaseThreat() {
    if (gameEnd) {
      return;
    }
    threatDial++;
    triggerBanner("Threat dial increased to " + threatDial);
  }

  public void advanceStatusPhase() {
    if (gameEnd) {
      return;
    }
    requestAdvanceStatusPhase();
  }

  public void finishCurrentRound() {
    if (gameEnd) {
      return;
    }
    requestAdvanceStatusPhase();
    repaint();
  }

  public void requestAdvanceStatusPhase() {
    if (gameEnd) {
      return;
    }
    if (!statusPhaseInProgress) {
      advanceStatusPhaseRequested = true;
    } else {
      abortStatusPhasePrompts = true;
    }
    cancelActivePrompt();
    if (ui != null) {
      ui.resetTransientTurnState();
    }
  }

  public void setActivePromptCancelAction(Runnable activePromptCancelAction) {
    this.activePromptCancelAction = activePromptCancelAction == null ? () -> {
    } : activePromptCancelAction;
  }

  public void clearActivePromptCancelAction() {
    this.activePromptCancelAction = () -> {
    };
  }

  public void cancelActivePrompt() {
    Runnable cancelAction = activePromptCancelAction;
    if (cancelAction != null) {
      cancelAction.run();
    }
  }

  private void finishRoundTransition() {
    advanceStatusPhaseRequested = false;
    cleanupTransientTurnState();
    resolveStatusPhase();
    checkEndGame();
  }

  private void cleanupTransientTurnState() {
    currentSelected.cancel(true);
    currentSelected = new CompletableFuture<>();
    for (Personnel target : availableTargets) {
      target.setPossibleTarget(false);
    }
    availableTargets.clear();
    for (Hero hero : heroes) {
      hero.setActive(false);
      hero.setPossibleTarget(false);
    }
    for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
      for (Imperial imperial : group.getMembers()) {
        imperial.setActive(false);
        imperial.setPossibleTarget(false);
      }
    }
    if (ui != null) {
      ui.resetTransientTurnState();
    }
    repaint();
  }

  private void resolveImperialOptionalDeployments() {
    while (true) {
      if (!statusPhaseInProgress || abortStatusPhasePrompts) {
        return;
      }
      MyArrayList<DeploymentGroup<? extends Imperial>> deployableGroups = getOptionalDeploymentOptions();
      if (deployableGroups.isEmpty()) {
        return;
      }
      boolean wantsDeploy;
      try {
        wantsDeploy = promptYesNo(PlayerSeat.IMPERIAL, "Imperial Deployment",
            "Spend threat to deploy an additional imperial group?");
      } catch (CancellationException ex) {
        if (statusPhaseInProgress && abortStatusPhasePrompts) {
          return;
        }
        throw ex;
      }
      if (!wantsDeploy) {
        return;
      }
      DeploymentGroup<? extends Imperial> chosenGroup;
      try {
        chosenGroup = deployableGroups
            .get(promptMultipleChoice(PlayerSeat.IMPERIAL, "Imperial Deployment",
                "Choose a group to deploy", deployableGroups.toArray()));
      } catch (CancellationException ex) {
        if (statusPhaseInProgress && abortStatusPhasePrompts) {
          return;
        }
        throw ex;
      }
      deployImperialGroup(chosenGroup);
    }
  }

  private MyArrayList<DeploymentGroup<? extends Imperial>> getOptionalDeploymentOptions() {
    MyArrayList<DeploymentGroup<? extends Imperial>> deployableGroups = new MyArrayList<>();
    for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
      if (!group.getDeployed() && group.getDeploymentCost() <= threatDial) {
        deployableGroups.add(group);
      }
    }
    return deployableGroups;
  }

  private void deployImperialGroup(DeploymentGroup<? extends Imperial> group) {
    if (group.getDeployed()) {
      throw new IllegalStateException("Group is already deployed: " + group);
    }
    if (group.getDeploymentCost() > threatDial) {
      throw new IllegalStateException("Not enough threat to deploy " + group);
    }
    threatDial -= group.getDeploymentCost();
    group.setDeployed(true);
    group.setExhausted(true);
    for (Imperial imperial : group.getMembers()) {
      imperial.setGame(this);
    }
    repaint();
    triggerBanner("Deployed " + group + " for " + group.getDeploymentCost() + " threat");
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
    MyArrayList<Actions> availableActions = new MyArrayList<>();
    availableActions.addAll(activeFigure.getActions());
    availableTargets = availableDefenders(activeFigure, rebel);
    if (availableTargets.size() == 0) {
      availableActions.remove(Actions.ATTACK);
    }
    if (canInteract(activeFigure)) {
      availableActions.add(Actions.INTERACT);
    }
    if (activeFigure instanceof Hero hero && hero.hasUsableEquipment(Equipment.UseTiming.DURING_ACTIVATION)) {
      availableActions.add(Actions.USE_EQUIPMENT);
    }
    Actions chosenAction = availableActions.get(promptMultipleChoice(activeFigure.getOwnerSeat(), "Action Selection",
        "Choose an action to take", availableActions.toArray()));
    if (chosenAction == Actions.USE_EQUIPMENT) {
      takeAction(activeFigure, chosenAction);
      return takeAction(activeFigure, rebel);
    }
    leftoverMoves = takeAction(activeFigure, chosenAction);
    return leftoverMoves;
  }

  public int takeAction(Personnel activeFigure, Actions action) {
    int leftoverMoves = 0;
    switch (action) {
      case MOVE -> {
        leftoverMoves += activeFigure.getSpeed();
        int movesUsed = promptPendingMoves(activeFigure.getOwnerSeat(), leftoverMoves);
        leftoverMoves -= movesUsed;
        handleMovesInternal(activeFigure, movesUsed);
      }
      case ATTACK -> {
        handleAttackInternal(activeFigure);
        removeDeadFigures();
      }
      case RECOVER -> {
        Hero hero = (Hero) activeFigure;
        hero.dealDamage(-1 * hero.getEndurance());
        offerAfterRecoverEquipment(hero);
      }
      case USE_EQUIPMENT -> {
        if (activeFigure instanceof Hero hero) {
          handleEquipmentUse(hero, Equipment.UseTiming.DURING_ACTIVATION);
        }
      }
      case SPECIAL -> handleSpecial(activeFigure);
      case INTERACT -> handleInteraction(activeFigure);
    }
    return leftoverMoves;
  }

  private void offerAfterRecoverEquipment(Hero hero) {
    if (!hero.hasUsableEquipment(Equipment.UseTiming.AFTER_RECOVER)) {
      return;
    }
    if (promptYesNo(hero.getOwnerSeat(), "Equipment", "Use equipment after resting?")) {
      handleEquipmentUse(hero, Equipment.UseTiming.AFTER_RECOVER);
    }
  }

  private void handleEquipmentUse(Hero owner, Equipment.UseTiming timing) {
    MyArrayList<Equipment.Item> usableEquipment = owner.getUsableEquipment(timing);
    if (usableEquipment.isEmpty()) {
      return;
    }
    Equipment.Item item = usableEquipment.get(usableEquipment.size() == 1 ? 0
        : promptMultipleChoice(owner.getOwnerSeat(), "Equipment", "Choose equipment to use", usableEquipment.toArray()));
    MyArrayList<Hero> targets = getFriendlyAdjacentHeroes(owner);
    if (targets.isEmpty()) {
      return;
    }
    Hero target = targets.get(targets.size() == 1 ? 0
        : promptMultipleChoice(owner.getOwnerSeat(), item.name(), "Choose a target", targets.toArray()));
    if (item.recoverAmount() > 0) {
      target.dealDamage(-item.recoverAmount());
    }
    if (item.grantsFocus()) {
      target.setFocused(true);
    }
    if (item.consumable()) {
      owner.removeEquipment(item);
    }
    triggerBanner(owner.getDisplayName() + " used " + item.name());
    repaint();
  }

  private MyArrayList<Hero> getFriendlyAdjacentHeroes(Hero owner) {
    MyArrayList<Hero> targets = new MyArrayList<>();
    for (Hero hero : heroes) {
      if (hero == owner || isAdjacent(owner.getPos(), hero.getPos())) {
        targets.add(hero);
      }
    }
    return targets;
  }

  private boolean isAdjacent(Pos first, Pos second) {
    int xDistance = Math.abs(first.getX() - second.getX());
    int yDistance = Math.abs(first.getY() - second.getY());
    return xDistance <= 1 && yDistance <= 1 && (xDistance + yDistance) > 0;
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
        if (!interactable.canInteract(activeFigure)) {
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

  public MyArrayList<Personnel> availableDefenders(Personnel attacker, boolean rebelAttacker) {
    MyArrayList<Personnel> defenders = new MyArrayList<>();
    if (rebelAttacker) {
      for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
        if (group.getDeployed()) {
          for (Imperial member : group.getMembers()) {
            if (attacker.canAttack(member)) {
              defenders.add(member);
            }
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

  public MyArrayList<Hero> getHeroExhaustOptions() {
    MyArrayList<Hero> readyDeployments = new MyArrayList<>();
    for (Hero hero : heroes) {
      if (!hero.getExhausted()) {
        readyDeployments.add(hero);
      }
    }
    return readyDeployments;
  }

  public MyArrayList<Hero> getHeroExhaustOptions(PlayerSeat seat) {
    MyArrayList<Hero> readyDeployments = new MyArrayList<>();
    for (Hero hero : heroes) {
      if (!hero.getExhausted() && hero.getOwnerSeat() == seat) {
        readyDeployments.add(hero);
      }
    }
    return readyDeployments;
  }

  public MyArrayList<DeploymentGroup<? extends Imperial>> getImperialExhaustOptions() {
    MyArrayList<DeploymentGroup<? extends Imperial>> readyDeployments = new MyArrayList<>();
    for (DeploymentGroup<? extends Imperial> deploymentGroup : imperialDeployments) {
      if (deploymentGroup.getDeployed() && !deploymentGroup.getExhausted()) {
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
      if (!depGroup.getDeployed()) {
        continue;
      }
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

  private void handlePendingMoves(Personnel activeFigure, PlayerSeat seat, int leftoverMoves) {
    handleMovesInternal(activeFigure, promptPendingMoves(seat, leftoverMoves));
  }

  private int promptPendingMoves(PlayerSeat seat, int leftoverMoves) {
    if (leftoverMoves == 0) {
      return 0;
    }
    return promptNumericChoice(seat, "# of moves you'll use", 0, leftoverMoves);
  }

  private void handleMoveInternal(Personnel activeFigure) {
    MyArrayList<Directions> availableDirections = new MyArrayList<>();
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
    Personnel chosenDefender = decisionProvider.chooseTarget(activeFigure.getOwnerSeat(), SelectionType.COMBAT,
        new MyArrayList<>(availableTargets));
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

  public MyArrayList<Imperial> getImperials() {
    MyArrayList<Imperial> imperials = new MyArrayList<>();
    for (DeploymentGroup<? extends Imperial> depGroup : imperialDeployments) {
      if (depGroup.getDeployed()) {
        imperials.addAll(depGroup.getMembers());
      }
    }
    return imperials;
  }

  public MyArrayList<Hero> getHeroes() {
    return heroes;
  }

  public void reset() {
    gameEnd = false;
    rebelsWin = true;
    threatDial = 0;
    nextSupplyEquipmentIndex = 0;
    currentTurnSeat = firstTurnSeat();
    actingSeat = currentTurnSeat;
    heroes.clear();
    imperialDeployments.clear();
    clearDiceInternal();
    for (Interactable<? extends Personnel> interactable : interactables) {
      interactable.applySnapshotState(true);
    }
    setup();
    playRound();
  }

  public void setup() {
    threatDial = 0;
    nextSupplyEquipmentIndex = 0;
    currentTurnSeat = firstTurnSeat();
    actingSeat = currentTurnSeat;
    heroes.clear();
    imperialDeployments.clear();
    Hero diala = new DialaPassil(new Pos(6, 5));
    Hero gaarkhan = new Gaarkhan(new Pos(1, 4));
    configureHero(diala, "hero-diala", PlayerSeat.REBEL_1);
    configureHero(gaarkhan, "hero-gaarkhan",
        sessionConfig.rebelPlayerCount() == 2 ? PlayerSeat.REBEL_2 : PlayerSeat.REBEL_1);
    heroes.add(diala);
    heroes.add(gaarkhan);

    DeploymentGroup<StormTrooper> troopers = new DeploymentGroup<>(
        new Pos[] { new Pos(4, 11), new Pos(4, 12), new Pos(5, 11) },
        StormTrooper::new, "StormTrooper");
    troopers.setDeploymentCost(6);
    troopers.setDeployed(true);
    configureDeploymentGroup(troopers, "imperial-stormtroopers", PlayerSeat.IMPERIAL);
    DeploymentGroup<Officer> officers = new DeploymentGroup<>(
        new Pos[] { new Pos(7, 11) }, Officer::new, "ImperialOfficer");
    officers.setDeploymentCost(4);
    officers.setDeployed(true);
    configureDeploymentGroup(officers, "imperial-officer", PlayerSeat.IMPERIAL);
    imperialDeployments.add(troopers);
    imperialDeployments.add(officers);
    DeploymentGroup<StormTrooper> reserveTroopers = new DeploymentGroup<>(
        new Pos[] { new Pos(8, 9), new Pos(9, 9), new Pos(8, 10) },
        StormTrooper::new, "StormTrooper");
    reserveTroopers.setDeploymentCost(6);
    reserveTroopers.setDeployed(false);
    configureDeploymentGroup(reserveTroopers, "imperial-stormtroopers-reserve", PlayerSeat.IMPERIAL);
    imperialDeployments.add(reserveTroopers);
    bindGameReferences();
    repaint();
  }

  private void bindGameReferences() {
    for (Hero hero : heroes) {
      hero.setGame(this);
    }
    for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
      if (group.getDeployed()) {
        for (Imperial imperial : group.getMembers()) {
          imperial.setGame(this);
        }
      }
    }
    for (Interactable<? extends Personnel> interactable : interactables) {
      interactable.setGame(this);
    }
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

  public void awardSupplyEquipment(Hero hero) {
    Equipment.Item item = Equipment.getSupplyItem(nextSupplyEquipmentIndex);
    if (item == null) {
      return;
    }
    nextSupplyEquipmentIndex++;
    hero.addEquipment(item);
    triggerBanner(hero.getDisplayName() + " found " + item.name());
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
      if (deployment.getDeployed()) {
        for (Imperial imperial : deployment.getMembers()) {
          if (imperial.getPos().equalTo(pos)) {
            return imperial;
          }
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
      if (deployment.getDeployed()) {
        for (Imperial imperial : deployment.getMembers()) {
          if (id.equals(imperial.getId())) {
            return imperial;
          }
        }
      }
    }
    return null;
  }

  public DeploymentCard getDeploymentCard(Pos pos) {
    for (DeploymentGroup<? extends Imperial> deployment : imperialDeployments) {
      if (deployment.getDeployed()) {
        for (Imperial imperial : deployment.getMembers()) {
          if (imperial.getPos().equalTo(pos)) {
            return deployment.getDeploymentCard();
          }
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
    Personnel chosenDefender = decisionProvider.chooseTarget(activeFigure.getOwnerSeat(), SelectionType.SPECIAL,
        new MyArrayList<>(availableTargets));
    for (Personnel person : availableTargets) {
      person.setPossibleTarget(false);
    }
    activeFigure.performSpecial(chosenDefender);
    repaint();
  }

  public MyArrayList<DeploymentGroup<? extends Imperial>> getDeploymentGroupsInternal() {
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
    MyArrayList<FigureSnapshot> heroSnapshots = new MyArrayList<>();
    for (Hero hero : heroes) {
      heroSnapshots.add(new FigureSnapshot(hero.getId(), hero.getName(), hero.getPos().getX(), hero.getPos().getY(),
          hero.getHealth(), hero.getStrain(), hero.stunned(), hero.focused, hero.isActive(),
          hero.isPossibleTarget(), hero.getExhausted(), hero.getOwnerSeat(), hero.getEquipmentIds()));
    }
    MyArrayList<DeploymentGroupSnapshot> groupSnapshots = new MyArrayList<>();
    for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
      MyArrayList<FigureSnapshot> members = new MyArrayList<>();
      for (Imperial imperial : group.getMembers()) {
        members.add(new FigureSnapshot(imperial.getId(), imperial.getName(), imperial.getPos().getX(),
            imperial.getPos().getY(), imperial.getHealth(), imperial.getStrain(), imperial.stunned(),
            imperial.focused, imperial.isActive(), imperial.isPossibleTarget(), false,
            imperial.getOwnerSeat(), new MyArrayList<>()));
      }
      groupSnapshots.add(new DeploymentGroupSnapshot(group.getId(), group.toString(), group.getExhausted(),
          group.getDeployed(), group.getDeploymentCost(), group.getOwnerSeat(), members));
    }
    MyArrayList<Boolean> interactableStates = new MyArrayList<>();
    for (Interactable<? extends Personnel> interactable : interactables) {
      interactableStates.add(interactable.snapshotState());
    }
    MyArrayList<String> offense = new MyArrayList<>();
    for (GraphicOffenseDieResult die : offenseResults) {
      offense.add(die.die().name() + ":" + die.face());
    }
    MyArrayList<String> defense = new MyArrayList<>();
    for (GraphicDefenseDieResult die : defenseResults) {
      defense.add(die.die().name() + ":" + die.face());
    }
    return new MatchSnapshot(sessionConfig, actingSeat, currentTurnSeat, threatDial, bannerId, bannerText,
        bannerExpiresAt,
        heroSnapshots,
        groupSnapshots, interactableStates, nextSupplyEquipmentIndex, offense, defense, gameEnd, rebelsWin);
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
      group.setDeployed(groupSnapshot.deployed());
      group.setDeploymentCost(groupSnapshot.deploymentCost());
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
    this.currentTurnSeat = snapshot.currentTurnSeat() == null ? snapshot.actingSeat() : snapshot.currentTurnSeat();
    this.threatDial = snapshot.threatDial();
    this.nextSupplyEquipmentIndex = snapshot.nextSupplyEquipmentIndex();
    if (snapshot.bannerId() > lastAppliedBannerId && ui != null) {
      lastAppliedBannerId = snapshot.bannerId();
      long remaining = snapshot.bannerExpiresAt() - System.currentTimeMillis();
      ui.showBannerFromSnapshot(snapshot.bannerText(), remaining);
    }
    if (ui != null) {
      ui.setTurnStatus(actingSeat);
    }
    bindGameReferences();
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

  private void startTurn(PlayerSeat seat) {
    currentTurnSeat = seat;
    actingSeat = seat;
    updateTurnStatus();
    announceTurnStart(seat);
  }

  private void endTurn(PlayerSeat seat) {
    currentTurnSeat = nextTurnSeatAfter(seat);
    announceTurnEnd(seat);
  }

  private MyArrayList<PlayerSeat> currentRoundTurnOrder() {
    MyArrayList<PlayerSeat> turnOrder = sessionConfig.rebelTurnOrder();
    turnOrder.add(PlayerSeat.IMPERIAL);
    return turnOrder;
  }

  private int getTurnIndex(PlayerSeat seat, MyArrayList<PlayerSeat> turnOrder) {
    for (int i = 0; i < turnOrder.size(); i++) {
      if (turnOrder.get(i) == seat) {
        return i;
      }
    }
    return 0;
  }

  private PlayerSeat nextTurnSeatAfter(PlayerSeat seat) {
    MyArrayList<PlayerSeat> turnOrder = currentRoundTurnOrder();
    int index = getTurnIndex(seat, turnOrder);
    if (index + 1 < turnOrder.size()) {
      return turnOrder.get(index + 1);
    }
    return firstTurnSeat();
  }

  private PlayerSeat firstTurnSeat() {
    MyArrayList<PlayerSeat> rebelTurnOrder = sessionConfig.rebelTurnOrder();
    return rebelTurnOrder.isEmpty() ? PlayerSeat.IMPERIAL : rebelTurnOrder.get(0);
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

  private void drawThreatHud(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setColor(new Color(0, 0, 0, 180));
    g2.fillRoundRect(20, 82, 300, 76, 18, 18);
    g2.setColor(Color.WHITE);
    g2.drawRoundRect(20, 82, 300, 76, 18, 18);
    g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 20f));
    g2.drawString("Threat Dial: " + threatDial, 38, 113);
    g2.setFont(g2.getFont().deriveFont(java.awt.Font.PLAIN, 14f));
    g2.drawString("Use the + Threat button", 38, 139);
    g2.dispose();
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
      hero.applyEquipmentIds(snapshot.equipmentIds());
    }
  }

  public boolean isGameEnd() {
    return gameEnd;
  }

  public boolean rebelsWin() {
    return rebelsWin;
  }

  public boolean isSpaceAvailable(Pos pos) {
    return isSpaceAvailableInternal(pos);
  }

  public void addOffenseResult(GraphicOffenseDieResult offenseResult) {
    addOffenseResultInternal(offenseResult);
  }

  public void addDefenseResult(GraphicDefenseDieResult defenseResult) {
    addDefenseResultInternal(defenseResult);
  }

  public void handleMoves(Personnel activeFigure, int numMoves) {
    handleMovesInternal(activeFigure, numMoves);
  }

  public Personnel getPersonnelAtPos(Pos pos) {
    return getPersonnelAtPosInternal(pos);
  }

  public void handleAttack(Personnel activeFigure) {
    handleAttackInternal(activeFigure);
  }

  public boolean setTarget(Personnel defender) {
    return trySetTarget(defender);
  }

  public void repaintScreen() {
    repaint();
  }

  public void clearDice() {
    clearDiceInternal();
    triggerBanner("Dice cleared");
  }

  public void endGame(boolean rebelsWin) {
    endGameInternal(rebelsWin);
  }

  public void removeOffenseDie(int die) {
    removeOffenseDieInternal(die);
  }

  public void removeDefenseDie(int die) {
    removeDefenseDieInternal(die);
  }

  public void performSpecial(Personnel activeFigure) {
    performSpecialInternal(activeFigure);
  }

  public MyArrayList<DeploymentGroup<? extends Imperial>> getDeploymentGroups() {
    return getDeploymentGroupsInternal();
  }

  public Interactable<? extends Personnel>[] getInteractables() {
    return interactables;
  }

  public void setAvailableTargets(MyArrayList<Personnel> availableTargets) {
    this.availableTargets = availableTargets;
  }

  public CompletableFuture<Personnel> getCurrentSelection() {
    return currentSelected;
  }

  public void setCurrentSelection(CompletableFuture<Personnel> currentSelected) {
    this.currentSelected = currentSelected;
  }
}
