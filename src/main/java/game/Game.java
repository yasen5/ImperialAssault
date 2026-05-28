package game;

import java.awt.Graphics;
import util.MyArrayList;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import game.Die.GraphicDefenseDieResult;
import game.Die.GraphicOffenseDieResult;
import game.Personnel.Actions;
import util.MyHashSet;
import net.structs.MatchSnapshot;
import net.structs.GameSessionConfig;
import net.GameDecisionProvider;

public class Game {
  GameMapTile mapTile;
  final MyArrayList<DeploymentGroup<? extends Imperial>> imperialDeployments = new MyArrayList<>();
  final MyArrayList<Hero> heroes = new MyArrayList<>();
  final MyArrayList<MissionTerminal> missionTerminals = new MyArrayList<>();
  final MyArrayList<GraphicOffenseDieResult> offenseResults = new MyArrayList<>();
  final MyArrayList<GraphicDefenseDieResult> defenseResults = new MyArrayList<>();
  public Interactable<? extends Personnel>[] interactables;

  GameUi ui;
  GameDecisionProvider decisionProvider;
  private Consumer<MatchSnapshot> snapshotListener;
  CompletableFuture<Personnel> currentSelected = new CompletableFuture<>();
  MyArrayList<Personnel> availableTargets = new MyArrayList<>();
  final MyHashSet<Personnel> specialUsedThisActivation = new MyHashSet<>();
  final MyArrayList<Actions> actionsUsedThisActivation = new MyArrayList<>();
  int threatDial = 0;
  int threatLevel = 1;
  int roundDial = 1;
  int roundLimit = 0;
  boolean gameEnd;
  boolean rebelsWin = true;
  PlayerSeat actingSeat = PlayerSeat.REBEL_1;
  PlayerSeat currentTurnSeat = PlayerSeat.REBEL_1;
  private boolean advanceStatusPhaseRequested;
  private boolean statusPhaseInProgress;
  private boolean abortStatusPhasePrompts;
  private boolean restartRequested;
  private Runnable activePromptCancelAction = () -> {
  };
  long bannerId;
  String bannerText;
  long bannerExpiresAt;
  long lastAppliedBannerId;
  int nextSupplyEquipmentIndex;
  final GameSessionConfig sessionConfig;
  MissionDefinition missionDefinition;
  MyArrayList<PlayerSeat> rebelHeroSelectionOrder = new MyArrayList<>();
  boolean fortifiedResolved;
  boolean lockdownResolved;
  boolean missionDoorOpenedThisRound;
  private Door<?> atriumDoor;
  boolean debugWallLines;
  private final GameRenderer renderer = new GameRenderer(this);
  final GameSetup setup = new GameSetup(this);
  private final GameSnapshotMapper snapshots = new GameSnapshotMapper(this);
  private final GameActionController actions = new GameActionController(this);

  public Game(GameUi ui) {
    this(ui, new GameSessionConfig(1), null, true);
  }

  public Game(GameUi ui, GameSessionConfig sessionConfig, GameDecisionProvider decisionProvider,
      boolean authoritative) {
    this(ui, sessionConfig, MissionDefinition.forOption(net.structs.MissionOption.MISSION_ONE), decisionProvider,
        authoritative);
  }

  @SuppressWarnings("unchecked")
  public Game(GameUi ui, GameSessionConfig sessionConfig, MissionDefinition missionDefinition,
      GameDecisionProvider decisionProvider, boolean authoritative) {
    this.ui = ui;
    this.sessionConfig = sessionConfig;
    this.decisionProvider = decisionProvider;
    this.missionDefinition = missionDefinition == null
        ? MissionDefinition.forOption(net.structs.MissionOption.MISSION_ONE)
        : missionDefinition;
    Constants.useMissionDefinition(this.missionDefinition);
    this.interactables = createInteractables(this.missionDefinition);
    this.mapTile = new GameMapTile(LoaderUtils.getImage(this.missionDefinition.mapImageName()),
        this.missionDefinition.tileMatrix());
    if (authoritative) {
      setup();
    }
  }

  @SuppressWarnings("unchecked")
  Interactable<? extends Personnel>[] createInteractables(MissionDefinition missionDefinition) {
    atriumDoor = null;
    MyArrayList<Interactable<? extends Personnel>> missionInteractables = new MyArrayList<>();
    if (!missionDefinition.attackableTerminals()) {
      for (Pos pos : missionDefinition.terminalPositions()) {
        missionInteractables.add(new Terminal<Imperial>(pos, Imperial.class));
      }
    }
    for (MissionDefinition.DoorSpec doorSpec : missionDefinition.doors()) {
      Door<?> door = missionDefinition.attackableTerminals() ? new MissionDoor(doorSpec.pos(), doorSpec.vertical())
          : new Door<Personnel>(doorSpec.pos(), Personnel.class, doorSpec.vertical());
      if (atriumDoor == null) {
        atriumDoor = door;
      }
      missionInteractables.add(door);
    }
    for (Pos pos : missionDefinition.cratePositions()) {
      missionInteractables.add(new SupplyBox(pos));
    }
    Interactable<? extends Personnel>[] result = (Interactable<? extends Personnel>[]) new Interactable[missionInteractables
        .size()];
    for (int i = 0; i < missionInteractables.size(); i++) {
      result[i] = missionInteractables.get(i);
    }
    return result;
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

  public void setRebelHeroSelectionOrder(MyArrayList<PlayerSeat> rebelHeroSelectionOrder) {
    this.rebelHeroSelectionOrder = rebelHeroSelectionOrder == null ? new MyArrayList<>()
        : new MyArrayList<>(rebelHeroSelectionOrder);
  }

  public void setSnapshotListener(Consumer<MatchSnapshot> snapshotListener) {
    this.snapshotListener = snapshotListener;
  }

  public void setDebugWallLines(boolean debugWallLines) {
    this.debugWallLines = debugWallLines;
  }

  public void drawGame(Graphics g) {
    renderer.drawGame(g);
  }

  public int getMapDrawWidth() {
    return renderer.getMapDrawWidth();
  }

  public void playRound() {
    while (!gameEnd) {
      if (restartRequested) {
        restartFromBeginningInternal();
        continue;
      }
      playCycle();
    }
  }

  private void playCycle() {
    try {
      if (advanceStatusPhaseRequested) {
        finishRoundTransition();
        return;
      }
      PlayerSeat seat = chooseNextActivationSeat();
      if (seat == null) {
        resolveStatusPhase();
      } else if (seat == PlayerSeat.IMPERIAL) {
        startTurn(PlayerSeat.IMPERIAL);
        activateImperials(getImperialExhaustOptions());
        if (gameEnd) {
          return;
        }
        endTurn(PlayerSeat.IMPERIAL);
      } else {
        startTurn(seat);
        activateHero(seat, getHeroExhaustOptions(seat));
        if (gameEnd) {
          return;
        }
        endTurn(seat);
      }
      repaint();
      checkEndGame();
    } catch (CancellationException ex) {
      if (gameEnd) {
        Thread.interrupted();
        return;
      }
      if (restartRequested) {
        Thread.interrupted();
        restartFromBeginningInternal();
        return;
      }
      if (advanceStatusPhaseRequested) {
        Thread.interrupted();
        finishRoundTransition();
        return;
      }
      throw ex;
    }
  }

  private PlayerSeat chooseNextActivationSeat() {
    boolean rebelsReady = !getHeroExhaustOptions().isEmpty();
    boolean imperialsReady = !getImperialExhaustOptions().isEmpty();
    if (!rebelsReady && !imperialsReady) {
      return null;
    }
    if (currentTurnSeat == PlayerSeat.IMPERIAL) {
      return imperialsReady ? PlayerSeat.IMPERIAL : chooseNextRebelSeatByVote();
    }
    return rebelsReady ? chooseNextRebelSeatByVote() : PlayerSeat.IMPERIAL;
  }

  private PlayerSeat chooseNextRebelSeatByVote() {
    MyArrayList<RebelActivationChoice> choices = readyRebelActivationChoices();
    if (choices.isEmpty()) {
      return null;
    }
    if (choices.size() == 1) {
      return choices.get(0).seat();
    }
    int[] votes = new int[choices.size()];
    MyArrayList<PlayerSeat> voters = sessionConfig.rebelTurnOrder();
    if (voters.isEmpty()) {
      voters = readyRebelSeats();
    }
    for (PlayerSeat voter : voters) {
      int vote = promptMultipleChoice(voter, "Rebel Initiative",
          "Vote for the Rebel player who should activate next", choices.toArray());
      votes[vote]++;
    }
    int winningIndex = 0;
    for (int i = 1; i < votes.length; i++) {
      if (votes[i] > votes[winningIndex]) {
        winningIndex = i;
      }
    }
    return choices.get(winningIndex).seat();
  }

  private MyArrayList<RebelActivationChoice> readyRebelActivationChoices() {
    MyArrayList<RebelActivationChoice> choices = new MyArrayList<>();
    for (PlayerSeat seat : readyRebelSeats()) {
      choices.add(new RebelActivationChoice(seat, formatReadyRebelActivationChoice(seat)));
    }
    return choices;
  }

  private String formatReadyRebelActivationChoice(PlayerSeat seat) {
    MyArrayList<Hero> readyHeroes = getHeroExhaustOptions(seat);
    if (readyHeroes.isEmpty()) {
      return formatSeat(seat);
    }
    StringBuilder label = new StringBuilder();
    for (Hero hero : readyHeroes) {
      if (label.length() > 0) {
        label.append(" / ");
      }
      label.append(formatHeroDeploymentCardName(hero));
    }
    return label.toString();
  }

  private String formatHeroDeploymentCardName(Hero hero) {
    return switch (hero.getName()) {
      case "DialaPassil" -> "Diala Passil";
      case "FennSignis" -> "Fenn Signis";
      case "MakEshray" -> "Mak Eshka'rey";
      default -> hero.getDeploymentCard().getLabel();
    };
  }

  private MyArrayList<PlayerSeat> readyRebelSeats() {
    MyArrayList<PlayerSeat> seats = new MyArrayList<>();
    for (Hero hero : heroes) {
      PlayerSeat seat = hero.getOwnerSeat();
      if (!hero.getExhausted() && seat.isRebel() && !seats.contains(seat)) {
        seats.add(seat);
      }
    }
    return seats;
  }

  private record RebelActivationChoice(PlayerSeat seat, String label) {
    @Override
    public String toString() {
      return label;
    }
  }

  private void activateHero(PlayerSeat rebelSeat, MyArrayList<Hero> seatOptions) {
    actingSeat = rebelSeat;
    updateTurnStatus();
    Hero activeFigure = seatOptions
        .remove(promptMultipleChoice(rebelSeat, "Deployment Selection",
            "Choose deployment card to exhaust", seatOptions.toArray()));
    activeFigure.setActive(true);
    specialUsedThisActivation.clear();
    actionsUsedThisActivation.clear();
    activeFigure.setExhausted(true);
    activeFigure.onActivationStart();
    repaint();
    int leftoverMoves = 0;
    int numActions = 2;
    for (int i = 0; i < numActions; i++) {
      leftoverMoves += takeAction(activeFigure, true);
      checkEndGame();
      if (gameEnd) {
        return;
      }
    }
    handlePendingMoves(activeFigure, rebelSeat, leftoverMoves);
    activeFigure.onActivationEnd();
    activeFigure.setActive(false);
    specialUsedThisActivation.clear();
    actionsUsedThisActivation.clear();
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
      specialUsedThisActivation.clear();
      actionsUsedThisActivation.clear();
      imperial.onActivationStart();
      repaint();
      int leftoverMoves = 0;
      if (!imperial.stunned() && imperial.gainsMoveBeforeImperialAction()) {
        leftoverMoves += takeAction(imperial, Actions.MOVE);
      }
      for (int i = 0; i < imperial.getImperialActionCount(); i++) {
        leftoverMoves += takeAction(imperial, false);
      }
      checkEndGame();
      if (gameEnd) {
        return;
      }
      handlePendingMoves(imperial, PlayerSeat.IMPERIAL, leftoverMoves);
      imperial.onActivationEnd();
      imperial.setActive(false);
      specialUsedThisActivation.clear();
      actionsUsedThisActivation.clear();
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
      resolveMissionEndOfRoundEvents();
      roundDial++;
      replenishDeployments();
      if (missionDefinition.usesThreat()) {
        threatDial += threatLevel;
        triggerBanner("Threat dial increased to " + threatDial);
        resolveImperialOptionalDeployments();
        resolveImperialReinforcements();
      } else {
        triggerBanner("Status phase complete");
      }
    } finally {
      statusPhaseInProgress = false;
    }
  }

  public void increaseThreat() {
    if (gameEnd || !missionDefinition.usesThreat()) {
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

  public void skipToEndScreen() {
    if (gameEnd) {
      return;
    }
    endGameInternal(rebelsWin);
  }

  public void requestRestartFromBeginning() {
    boolean restartEndedGame = gameEnd;
    restartRequested = true;
    gameEnd = false;
    cancelActivePrompt();
    if (ui != null) {
      ui.resetTransientTurnState();
    }
    if (restartEndedGame) {
      playRound();
    }
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

  public void onMissionDoorOpened(Door<?> door) {
    if (!missionDefinition.attackableTerminals()) {
      return;
    }
    missionDoorOpenedThisRound = true;
    if (fortifiedResolved) {
      return;
    }
    fortifiedResolved = true;
    deployReservedMissionTwoGroups();
    triggerBanner("Fortified: reserved Imperial groups deployed");
  }

  private void resolveMissionEndOfRoundEvents() {
    if (!missionDefinition.attackableTerminals() || lockdownResolved || !missionDoorOpenedThisRound) {
      return;
    }
    lockdownResolved = true;
    missionDoorOpenedThisRound = false;
    Object[] options = new Object[] {
        "Each terminal has 7 Health instead of 4",
        "The Atrium door closes and is locked"
    };
    int choice = decisionProvider == null ? 0
        : promptMultipleChoice(PlayerSeat.IMPERIAL, "Lockdown",
            "Choose the Imperial lockdown effect", options);
    if (choice == 0) {
      for (MissionTerminal terminal : missionTerminals) {
        terminal.harden();
      }
      triggerBanner("Lockdown: terminals hardened");
    } else if (atriumDoor != null) {
      atriumDoor.close();
      triggerBanner("Lockdown: Atrium door closed");
    }
  }

  private void deployReservedMissionTwoGroups() {
    for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
      if (group.getDeployed() || group.getId() == null || !group.getId().contains("reserve")) {
        continue;
      }
      group.setDeployed(true);
      if (group.getId().contains("e-web")) {
        for (Imperial imperial : group.getMembers()) {
          imperial.setFocused(true);
        }
      }
      for (Imperial imperial : group.getMembers()) {
        imperial.setGame(this);
      }
    }
    repaint();
  }

  private void restartFromBeginningInternal() {
    restartRequested = false;
    advanceStatusPhaseRequested = false;
    abortStatusPhasePrompts = false;
    statusPhaseInProgress = false;
    cleanupTransientTurnState();
    resetStateForNewGame();
    triggerBanner("Game restarted");
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

  private void resolveImperialReinforcements() {
    while (true) {
      MyArrayList<DeploymentGroup<? extends Imperial>> reinforceableGroups = getReinforcementOptions();
      if (reinforceableGroups.isEmpty()) {
        return;
      }
      boolean wantsReinforce = promptYesNo(PlayerSeat.IMPERIAL, "Imperial Reinforcement",
          "Spend threat to reinforce a defeated imperial figure?");
      if (!wantsReinforce) {
        return;
      }
      DeploymentGroup<? extends Imperial> chosenGroup = reinforceableGroups
          .get(promptMultipleChoice(PlayerSeat.IMPERIAL, "Imperial Reinforcement",
              "Choose a group to reinforce", reinforceableGroups.toArray()));
      reinforceImperialGroup(chosenGroup);
    }
  }

  private MyArrayList<DeploymentGroup<? extends Imperial>> getReinforcementOptions() {
    MyArrayList<DeploymentGroup<? extends Imperial>> reinforceableGroups = new MyArrayList<>();
    for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
      if (group.canReinforce(threatDial)) {
        reinforceableGroups.add(group);
      }
    }
    return reinforceableGroups;
  }

  private void deployImperialGroup(DeploymentGroup<? extends Imperial> group) {
    if (group.getDeployed()) {
      return;
    }
    if (group.getDeploymentCost() > threatDial) {
      return;
    }
    threatDial -= group.getDeploymentCost();
    setup.initializeDeploymentOrientations(group, true);
    group.setDeployed(true);
    group.setExhausted(true);
    for (Imperial imperial : group.getMembers()) {
      imperial.setGame(this);
    }
    repaint();
    triggerBanner("Deployed " + group + " for " + group.getDeploymentCost() + " threat");
  }

  private void reinforceImperialGroup(DeploymentGroup<? extends Imperial> group) {
    int cost = group.getReinforcementCost();
    if (!group.canReinforce(threatDial)) {
      return;
    }
    Pos spawnPos = findOpenImperialDeploymentPosition();
    threatDial -= cost;
    int memberIndex = group.getMembers().size();
    group.reinforceMember(spawnPos);
    Imperial reinforced = group.getMembers().get(memberIndex);
    reinforced.setId(group.getId() + "-member-" + memberIndex);
    reinforced.setOwnerSeat(group.getOwnerSeat());
    reinforced.setGame(this);
    group.setExhausted(true);
    triggerBanner("Reinforced " + group + " for " + cost + " threat");
    repaint();
  }

  private Pos findOpenImperialDeploymentPosition() {
    for (int y = Constants.tileMatrix.length - 1; y >= 0; y--) {
      for (int x = Constants.tileMatrix[y].length - 1; x >= 0; x--) {
        Pos pos = new Pos(x, y);
        if (Constants.tileMatrix[y][x] == 1 && isSpaceAvailable(pos)) {
          return pos;
        }
      }
    }
    return new Pos(0, 0);
  }

  public void checkEndGame() {
    if (gameEnd) {
      return;
    }
    if (roundLimit > 0 && roundDial > roundLimit) {
      endGameInternal(false);
      return;
    }
    boolean anyHeroAlive = false;
    for (Hero hero : heroes) {
      if (!hero.isDefeated()) {
        anyHeroAlive = true;
        break;
      }
    }
    if (!anyHeroAlive) {
      endGameInternal(false);
      return;
    }
    if (missionDefinition.attackableTerminals() && allHeroesWounded()) {
      endGameInternal(false);
      return;
    }
    if (missionDefinition.attackableTerminals() && allMissionTerminalsDestroyed()) {
      endGameInternal(true);
      return;
    }
    boolean anyImperialAlive = false;
    for (DeploymentGroup<? extends Imperial> group : imperialDeployments) {
      if (!group.getDeployed()) {
        continue;
      }
      for (Imperial imperial : group.getMembers()) {
        if (!imperial.isDefeated()) {
          anyImperialAlive = true;
          break;
        }
      }
      if (anyImperialAlive) {
        break;
      }
    }
    if (!anyImperialAlive) {
      endGameInternal(true);
    }
  }

  private boolean allHeroesWounded() {
    if (heroes.isEmpty()) {
      return false;
    }
    for (Hero hero : heroes) {
      if (!hero.isWounded()) {
        return false;
      }
    }
    return true;
  }

  private boolean allMissionTerminalsDestroyed() {
    if (missionTerminals.isEmpty()) {
      return false;
    }
    for (MissionTerminal terminal : missionTerminals) {
      if (!terminal.isDefeated()) {
        return false;
      }
    }
    return true;
  }

  public void removeDeadFigures() {
    actions.removeDeadFigures();
  }

  public int takeAction(Personnel activeFigure, boolean rebel) {
    return actions.takeAction(activeFigure, rebel);
  }

  public int takeAction(Personnel activeFigure, Actions action) {
    return actions.takeAction(activeFigure, action);
  }

  MyArrayList<Actions> getAvailableActions(Personnel activeFigure, boolean rebel) {
    return actions.getAvailableActions(activeFigure, rebel);
  }

  public boolean isAdjacentToImperial(Pos pos) {
    return actions.isAdjacentToImperial(pos);
  }

  public void handleSpecial(Personnel activeFigure) {
    actions.handleSpecial(activeFigure);
  }

  public void handleInteraction(Personnel activeFigure) {
    actions.handleInteraction(activeFigure);
  }

  public Interactable<? extends Personnel> getAdjacentInteractable(Personnel activeFigure) {
    return actions.getAdjacentInteractable(activeFigure);
  }

  public boolean canInteract(Personnel activeFigure) {
    return actions.canInteract(activeFigure);
  }

  public MyArrayList<Personnel> availableDefenders(Personnel attacker, boolean rebelAttacker) {
    return actions.availableDefenders(attacker, rebelAttacker);
  }

  public MyArrayList<Hero> getHeroExhaustOptions() {
    return actions.getHeroExhaustOptions();
  }

  public MyArrayList<Hero> getHeroExhaustOptions(PlayerSeat seat) {
    return actions.getHeroExhaustOptions(seat);
  }

  public MyArrayList<DeploymentGroup<? extends Imperial>> getImperialExhaustOptions() {
    return actions.getImperialExhaustOptions();
  }

  public boolean isSpaceAvailable(Pos pos) {
    return actions.isSpaceAvailable(pos);
  }

  public boolean isSpaceAvailable(Pos pos, Personnel ignore) {
    return actions.isSpaceAvailable(pos, ignore);
  }

  private void handleMovesInternal(Personnel activeFigure, int numMoves) {
    actions.handleMovesInternal(activeFigure, numMoves);
  }

  private void handlePendingMoves(Personnel activeFigure, PlayerSeat seat, int leftoverMoves) {
    actions.handlePendingMoves(activeFigure, seat, leftoverMoves);
  }

  private void handleAttackInternal(Personnel activeFigure) {
    actions.handleAttackInternal(activeFigure);
  }

  public boolean trySetTarget(Personnel defender) {
    return actions.trySetTarget(defender);
  }

  public MyArrayList<Imperial> getImperials() {
    return actions.getImperials();
  }

  public void applyBlast(Personnel target, int blastValue) {
    actions.applyBlast(target, blastValue);
  }

  public void applyBlast(Personnel attacker, Personnel target, int blastValue) {
    actions.applyBlast(attacker, target, blastValue);
  }

  public MyArrayList<Hero> getHeroes() {
    return heroes;
  }

  public void reset() {
    resetStateForNewGame();
    playRound();
  }

  private void resetStateForNewGame() {
    setup.resetStateForNewGame();
  }

  public void setup() {
    setup.setup();
  }

  private boolean canOccupyFootprint(Pos[] spaces, Personnel ignore) {
    for (Pos space : spaces) {
      if (!isSpaceAvailable(space, ignore)) {
        return false;
      }
    }
    return true;
  }

  private int countOverlap(Pos[] first, Pos[] second) {
    int overlap = 0;
    for (Pos firstSpace : first) {
      for (Pos secondSpace : second) {
        if (firstSpace.equalTo(secondSpace)) {
          overlap++;
          break;
        }
      }
    }
    return overlap;
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
          if (imperial.occupiesSpace(pos)) {
            return imperial;
          }
        }
      }
    }
    for (Hero hero : heroes) {
      if (hero.occupiesSpace(pos)) {
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
    for (MissionTerminal terminal : missionTerminals) {
      if (id.equals(terminal.getId())) {
        return terminal;
      }
    }
    return null;
  }

  public DeploymentCard getDeploymentCard(Pos pos) {
    for (DeploymentGroup<? extends Imperial> deployment : imperialDeployments) {
      if (deployment.getDeployed()) {
        for (Imperial imperial : deployment.getMembers()) {
          if (imperial.occupiesSpace(pos)) {
            return deployment.getDeploymentCard();
          }
        }
      }
    }
    for (Hero hero : heroes) {
      if (hero.occupiesSpace(pos)) {
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
    gameEnd = true;
    cancelActivePrompt();
    if (ui != null) {
      ui.resetTransientTurnState();
      ui.endGame(rebelsWin);
      ui.deactiveateMovementButtons();
    }
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
    return snapshots.createSnapshot();
  }

  public void loadSnapshot(MatchSnapshot snapshot) {
    snapshots.loadSnapshot(snapshot);
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

  private PlayerSeat nextTurnSeatAfter(PlayerSeat seat) {
    return seat == PlayerSeat.IMPERIAL ? firstTurnSeat() : PlayerSeat.IMPERIAL;
  }

  PlayerSeat firstTurnSeat() {
    MyArrayList<PlayerSeat> rebelTurnOrder = sessionConfig.rebelTurnOrder();
    return rebelTurnOrder.isEmpty() ? PlayerSeat.IMPERIAL : rebelTurnOrder.get(0);
  }

  void triggerBanner(String text) {
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
      case REBEL_3 -> "Rebel 3";
      case REBEL_4 -> "Rebel 4";
    };
  }

  public boolean isGameEnd() {
    return gameEnd;
  }

  public boolean rebelsWin() {
    return rebelsWin;
  }

  public int getThreatDial() {
    return threatDial;
  }

  public int getThreatLevel() {
    return threatLevel;
  }

  public int getRoundDial() {
    return roundDial;
  }

  public int getRoundLimit() {
    return roundLimit;
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
