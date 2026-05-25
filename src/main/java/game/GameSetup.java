package game;

import java.util.function.Function;

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
      default -> throw new IllegalArgumentException("Unknown deployment group: " + spec.groupName());
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
    MyArrayList<HeroSetupOption> availableHeroes = MyArrayList.of(
        new HeroSetupOption("Diala Passil", "hero-diala", DialaPassil::new),
        new HeroSetupOption("Gaarkhan", "hero-gaarkhan", Gaarkhan::new),
        new HeroSetupOption("Fenn Signis", "hero-fenn", FennSignis::new),
        new HeroSetupOption("Mak Eshka'rey", "hero-mak", MakEshray::new));
    MissionDefinition.HeroPlacement heroPlacement = game.missionDefinition.heroPlacement();
    MyArrayList<PlayerSeat> owners = heroSelectionOwners();
    for (int i = 0; i < owners.size(); i++) {
      HeroSetupOption option = chooseHeroSetupOption(owners.get(i), availableHeroes);
      Hero hero = option.constructor().apply(heroPlacement.position(i));
      configureHero(hero, option.id(), owners.get(i));
      game.heroes.add(hero);
      availableHeroes.remove(option);
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

  private HeroSetupOption chooseHeroSetupOption(PlayerSeat owner, MyArrayList<HeroSetupOption> availableHeroes) {
    if (game.decisionProvider == null || game.sessionConfig.rebelPlayerCount() == 0 || availableHeroes.size() == 1) {
      return availableHeroes.get(0);
    }
    int choice = game.promptMultipleChoice(owner, "Hero Selection", "Choose your hero", availableHeroes.toArray());
    return availableHeroes.get(choice);
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

  private record HeroSetupOption(String label, String id, Function<Pos, Hero> constructor) {
    @Override
    public String toString() {
      return label;
    }
  }
}
