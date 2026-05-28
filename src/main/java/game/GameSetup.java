package game;

import net.structs.GameSessionConfig;
import util.MyArrayList;

final class GameSetup {
  private final Game game;

  GameSetup(Game game) {
    this.game = game;
  }

  void resetStateForNewGame() {
    game.gameEnd = false;
    game.rebelsWin = true;
    game.fortifiedResolved = false;
    game.lockdownResolved = false;
    game.missionDoorOpenedThisRound = false;
    game.offenseResults.clear();
    game.defenseResults.clear();
    for (Interactable<? extends Personnel> interactable : game.interactables) {
      interactable.applySnapshotState(true);
    }
    setup();
  }

  void setup() {
    game.threatDial = 0;
    game.threatLevel = game.missionDefinition.threatLevel();
    game.roundLimit = game.missionDefinition.roundLimit();
    game.roundDial = 1;
    game.nextSupplyEquipmentIndex = 0;
    game.currentTurnSeat = game.firstTurnSeat();
    game.actingSeat = game.currentTurnSeat;
    game.heroes.clear();
    game.imperialDeployments.clear();
    game.missionTerminals.clear();
    addSelectedHeroes();
    int heroCount = game.heroes.size();
    setupMissionDeployments(heroCount);
    if (game.missionDefinition.attackableTerminals()) {
      setupMissionTerminals();
    }
    bindGameReferences();
    game.repaint();
  }

  void bindGameReferences() {
    for (Hero hero : game.heroes) {
      hero.setGame(game);
    }
    for (DeploymentGroup<? extends Imperial> group : game.imperialDeployments) {
      if (group.getDeployed()) {
        for (Imperial imperial : group.getMembers()) {
          imperial.setGame(game);
        }
      }
    }
    for (Interactable<? extends Personnel> interactable : game.interactables) {
      interactable.setGame(game);
    }
    for (MissionTerminal terminal : game.missionTerminals) {
      terminal.setGame(game);
    }
  }

  private void setupMissionDeployments(int heroCount) {
    for (MissionDefinition.DeploymentSpec spec : game.missionDefinition.deployments()) {
      if (heroCount < spec.minimumHeroCount()) {
        continue;
      }
      DeploymentGroup<? extends Imperial> group = createDeploymentGroup(spec);
      group.setDeploymentCost(spec.deploymentCost());
      group.setDeployed(spec.deployed());
      if (spec.horizontal() != null) {
        initializeDeploymentOrientations(group, spec.horizontal());
      }
      configureDeploymentGroup(group, spec.id(), PlayerSeat.IMPERIAL);
      game.imperialDeployments.add(group);
    }
  }

  DeploymentGroup<? extends Imperial> createDeploymentGroup(MissionDefinition.DeploymentSpec spec) {
    return switch (spec.groupName()) {
      case "StormTrooper" -> new DeploymentGroup<StormTrooper>(spec.positions(), StormTrooper::new, "StormTrooper");
      case "ImperialOfficer" -> new DeploymentGroup<Officer>(spec.positions(), Officer::new, "ImperialOfficer");
      case "ProbeDroid" -> new DeploymentGroup<ProbeDroid>(spec.positions(), ProbeDroid::new, "ProbeDroid");
      case "EWebEngineer" -> new DeploymentGroup<EWebEngineer>(spec.positions(), EWebEngineer::new, "EWebEngineer");
      default -> new DeploymentGroup<StormTrooper>(spec.positions(), StormTrooper::new, "StormTrooper");
    };
  }

  private void setupMissionTerminals() {
    int index = 0;
    for (Pos pos : game.missionDefinition.terminalPositions()) {
      MissionTerminal terminal = new MissionTerminal(pos);
      terminal.setId("mission-terminal-" + index);
      terminal.setGame(game);
      game.missionTerminals.add(terminal);
      index++;
    }
  }

  private void addSelectedHeroes() {
    MyArrayList<Integer> availableHeroIndexes = MyArrayList.of(0, 1, 2, 3);
    MissionDefinition.HeroPlacement heroPlacement = game.missionDefinition.heroPlacement();
    MyArrayList<PlayerSeat> owners = heroSelectionOwners();
    for (int i = 0; i < owners.size(); i++) {
      int heroIndex = chooseHeroSetupOption(owners.get(i), availableHeroIndexes);
      Hero hero = createHero(heroIndex, heroPlacement.position(i));
      configureHero(hero, heroId(heroIndex), owners.get(i));
      game.heroes.add(hero);
      availableHeroIndexes.remove(Integer.valueOf(heroIndex));
    }
  }

  private MyArrayList<PlayerSeat> heroSelectionOwners() {
    GameSessionConfig sessionConfig = game.sessionConfig;
    int heroCount = Math.max(2, sessionConfig.rebelPlayerCount());
    MyArrayList<PlayerSeat> owners = new MyArrayList<>();
    MyArrayList<PlayerSeat> selectionOrder = game.rebelHeroSelectionOrder.isEmpty()
        ? sessionConfig.rebelTurnOrder()
        : new MyArrayList<>(game.rebelHeroSelectionOrder);
    if (selectionOrder.isEmpty()) {
      selectionOrder.add(PlayerSeat.REBEL_1);
    }
    for (PlayerSeat seat : selectionOrder) {
      if (owners.size() < heroCount && seat.isRebel()) {
        owners.add(seat);
      }
    }
    while (owners.size() < heroCount) {
      owners.add(selectionOrder.get(owners.size() % selectionOrder.size()));
    }
    return owners;
  }

  private int chooseHeroSetupOption(PlayerSeat owner, MyArrayList<Integer> availableHeroIndexes) {
    if (game.decisionProvider == null || game.sessionConfig.rebelPlayerCount() == 0 || availableHeroIndexes.size() == 1) {
      return availableHeroIndexes.get(0);
    }
    Object[] labels = new Object[availableHeroIndexes.size()];
    for (int i = 0; i < availableHeroIndexes.size(); i++) {
      labels[i] = heroLabel(availableHeroIndexes.get(i));
    }
    int choice = game.promptMultipleChoice(owner, "Hero Selection", "Choose your hero", labels);
    return availableHeroIndexes.get(choice);
  }

  private Hero createHero(int heroIndex, Pos pos) {
    return switch (heroIndex) {
      case 0 -> new DialaPassil(pos);
      case 1 -> new Gaarkhan(pos);
      case 2 -> new FennSignis(pos);
      case 3 -> new MakEshray(pos);
      default -> new DialaPassil(pos);
    };
  }

  private String heroId(int heroIndex) {
    return switch (heroIndex) {
      case 0 -> "hero-diala";
      case 1 -> "hero-gaarkhan";
      case 2 -> "hero-fenn";
      case 3 -> "hero-mak";
      default -> "hero-diala";
    };
  }

  private String heroLabel(int heroIndex) {
    return switch (heroIndex) {
      case 0 -> "Diala Passil";
      case 1 -> "Gaarkhan";
      case 2 -> "Fenn Signis";
      case 3 -> "Mak Eshka'rey";
      default -> "Diala Passil";
    };
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

  void initializeDeploymentOrientations(DeploymentGroup<? extends Imperial> group, boolean horizontal) {
    for (Imperial imperial : group.getMembers()) {
      if (!imperial.isNonSquareLargeFigure()) {
        continue;
      }
      imperial.setHorizontalOrientation(horizontal);
      imperial.setPos(imperial.getPos());
    }
  }
}
