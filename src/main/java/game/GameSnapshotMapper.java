package game;

import game.Die.GraphicDefenseDieResult;
import game.Die.GraphicOffenseDieResult;
import net.structs.DeploymentGroupSnapshot;
import net.structs.FigureSnapshot;
import net.structs.MatchSnapshot;
import util.MyArrayList;

final class GameSnapshotMapper {
  private final Game game;

  GameSnapshotMapper(Game game) {
    this.game = game;
  }

  MatchSnapshot createSnapshot() {
    MyArrayList<FigureSnapshot> heroSnapshots = new MyArrayList<>();
    for (Hero hero : game.heroes) {
      heroSnapshots.add(new FigureSnapshot(hero.getId(), hero.getName(), hero.getPos().getX(), hero.getPos().getY(),
          hero.getXSize(), hero.getYSize(),
          hero.getHealth(), hero.getStrain(), hero.stunned(), hero.focused, hero.isActive(),
          hero.isPossibleTarget(), hero.getExhausted(), hero.getOwnerSeat(), hero.getEquipmentIds(),
          hero.getConditionNames(), hero.isWounded(), hero.isDefeated()));
    }
    MyArrayList<DeploymentGroupSnapshot> groupSnapshots = new MyArrayList<>();
    for (DeploymentGroup<? extends Imperial> group : game.imperialDeployments) {
      MyArrayList<FigureSnapshot> members = new MyArrayList<>();
      for (Imperial imperial : group.getMembers()) {
        members.add(new FigureSnapshot(imperial.getId(), imperial.getName(), imperial.getPos().getX(),
            imperial.getPos().getY(), imperial.getXSize(), imperial.getYSize(),
            imperial.getHealth(), imperial.getStrain(), imperial.stunned(),
            imperial.focused, imperial.isActive(), imperial.isPossibleTarget(), false,
            imperial.getOwnerSeat(), new MyArrayList<>(), imperial.getConditionNames(), false, imperial.isDefeated()));
      }
      groupSnapshots.add(new DeploymentGroupSnapshot(group.getId(), group.toString(), group.getExhausted(),
          group.getDeployed(), group.getDeploymentCost(), group.getReinforcementCost(), group.getMaxMemberCount(),
          group.getOwnerSeat(), members));
    }
    MyArrayList<FigureSnapshot> terminalSnapshots = new MyArrayList<>();
    for (MissionTerminal terminal : game.missionTerminals) {
      terminalSnapshots.add(new FigureSnapshot(terminal.getId(), terminal.getName(), terminal.getPos().getX(),
          terminal.getPos().getY(), terminal.getXSize(), terminal.getYSize(),
          terminal.getHealth(), terminal.getStrain(), terminal.stunned(), false,
          terminal.isActive(), terminal.isPossibleTarget(), false, terminal.getOwnerSeat(), new MyArrayList<>(),
          terminal.getConditionNames(), false, terminal.isDefeated()));
    }
    MyArrayList<Boolean> interactableStates = new MyArrayList<>();
    for (Interactable<? extends Personnel> interactable : game.interactables) {
      interactableStates.add(interactable.snapshotState());
    }
    MyArrayList<String> offense = new MyArrayList<>();
    for (GraphicOffenseDieResult die : game.offenseResults) {
      offense.add(die.die().name() + ":" + die.face());
    }
    MyArrayList<String> defense = new MyArrayList<>();
    for (GraphicDefenseDieResult die : game.defenseResults) {
      defense.add(die.die().name() + ":" + die.face());
    }
    return new MatchSnapshot(game.missionDefinition.option(), game.sessionConfig, game.actingSeat, game.currentTurnSeat,
        game.threatDial, game.threatLevel, game.roundDial, game.roundLimit, game.bannerId, game.bannerText,
        game.bannerExpiresAt, heroSnapshots, groupSnapshots, terminalSnapshots, interactableStates,
        game.nextSupplyEquipmentIndex, offense, defense, game.gameEnd, game.rebelsWin);
  }

  void loadSnapshot(MatchSnapshot snapshot) {
    applySnapshotMission(snapshot);
    game.heroes.clear();
    game.imperialDeployments.clear();
    game.missionTerminals.clear();
    game.clearDiceInternal();
    for (FigureSnapshot heroSnapshot : snapshot.heroes()) {
      Hero hero = createHero(heroSnapshot);
      applyFigureSnapshot(hero, heroSnapshot);
      game.heroes.add(hero);
    }
    for (DeploymentGroupSnapshot groupSnapshot : snapshot.imperialGroups()) {
      DeploymentGroup<? extends Imperial> group = createGroup(groupSnapshot);
      group.setId(groupSnapshot.id());
      group.setOwnerSeat(groupSnapshot.ownerSeat());
      group.setExhausted(groupSnapshot.exhausted());
      group.setDeployed(groupSnapshot.deployed());
      group.setDeploymentCost(groupSnapshot.deploymentCost());
      group.setReinforcementCost(groupSnapshot.reinforcementCost());
      group.setMaxMemberCount(groupSnapshot.maxMemberCount());
      for (int i = 0; i < group.getMembers().size() && i < groupSnapshot.members().size(); i++) {
        applyFigureSnapshot(group.getMembers().get(i), groupSnapshot.members().get(i));
      }
      game.imperialDeployments.add(group);
    }
    if (snapshot.missionTerminals() != null) {
      for (FigureSnapshot terminalSnapshot : snapshot.missionTerminals()) {
        MissionTerminal terminal = new MissionTerminal(new Pos(terminalSnapshot.x(), terminalSnapshot.y()));
        applyFigureSnapshot(terminal, terminalSnapshot);
        game.missionTerminals.add(terminal);
      }
    }
    for (int i = 0; i < game.interactables.length && i < snapshot.interactableStates().size(); i++) {
      game.interactables[i].applySnapshotState(snapshot.interactableStates().get(i));
    }
    for (String die : snapshot.offenseResults()) {
      String[] parts = die.split(":");
      game.offenseResults.add(new GraphicOffenseDieResult(Integer.parseInt(parts[1]),
          Die.OffenseDieType.valueOf(parts[0])));
    }
    for (String die : snapshot.defenseResults()) {
      String[] parts = die.split(":");
      game.defenseResults.add(new GraphicDefenseDieResult(Integer.parseInt(parts[1]),
          Die.DefenseDieType.valueOf(parts[0])));
    }
    game.gameEnd = snapshot.gameEnd();
    game.rebelsWin = snapshot.rebelsWin();
    game.actingSeat = snapshot.actingSeat();
    game.currentTurnSeat = snapshot.currentTurnSeat() == null ? snapshot.actingSeat() : snapshot.currentTurnSeat();
    game.threatDial = snapshot.threatDial();
    game.threatLevel = snapshot.threatLevel();
    game.roundDial = snapshot.roundDial();
    game.roundLimit = snapshot.roundLimit();
    game.nextSupplyEquipmentIndex = snapshot.nextSupplyEquipmentIndex();
    if (snapshot.bannerId() > game.lastAppliedBannerId && game.ui != null) {
      game.lastAppliedBannerId = snapshot.bannerId();
      long remaining = snapshot.bannerExpiresAt() - System.currentTimeMillis();
      game.ui.showBannerFromSnapshot(snapshot.bannerText(), remaining);
    }
    if (game.ui != null) {
      game.ui.setTurnStatus(game.actingSeat);
      if (game.gameEnd) {
        game.ui.endGame(game.rebelsWin);
      }
    }
    game.setup.bindGameReferences();
    game.repaint();
  }

  private void applySnapshotMission(MatchSnapshot snapshot) {
    net.structs.MissionOption snapshotMission = snapshot.mission() == null
        ? net.structs.MissionOption.MISSION_ONE
        : snapshot.mission();
    if (game.missionDefinition.option() == snapshotMission) {
      Constants.useMissionDefinition(game.missionDefinition);
      return;
    }
    game.missionDefinition = MissionDefinition.forOption(snapshotMission);
    Constants.useMissionDefinition(game.missionDefinition);
    game.interactables = game.createInteractables(game.missionDefinition);
    game.mapTile = new GameMapTile(LoaderUtils.getImage(game.missionDefinition.mapImageName()),
        game.missionDefinition.tileMatrix());
  }

  private Hero createHero(FigureSnapshot heroSnapshot) {
    return switch (heroSnapshot.name()) {
      case "DialaPassil" -> new DialaPassil(new Pos(heroSnapshot.x(), heroSnapshot.y()));
      case "Gaarkhan" -> new Gaarkhan(new Pos(heroSnapshot.x(), heroSnapshot.y()));
      case "FennSignis" -> new FennSignis(new Pos(heroSnapshot.x(), heroSnapshot.y()));
      case "MakEshray" -> new MakEshray(new Pos(heroSnapshot.x(), heroSnapshot.y()));
      default -> new DialaPassil(new Pos(heroSnapshot.x(), heroSnapshot.y()));
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
      case "ProbeDroid" -> new DeploymentGroup<ProbeDroid>(poses, ProbeDroid::new, "ProbeDroid");
      case "EWebEngineer" -> new DeploymentGroup<EWebEngineer>(poses, EWebEngineer::new, "EWebEngineer");
      default -> new DeploymentGroup<StormTrooper>(poses, StormTrooper::new, "StormTrooper");
    };
  }

  private void applyFigureSnapshot(Personnel personnel, FigureSnapshot snapshot) {
    personnel.setId(snapshot.id());
    personnel.setOwnerSeat(snapshot.ownerSeat());
    personnel.setPos(new Pos(snapshot.x(), snapshot.y()));
    if (snapshot.xSize() > 0 && snapshot.ySize() > 0 && personnel.isNonSquareLargeFigure()) {
      personnel.setHorizontalOrientation(snapshot.xSize() > snapshot.ySize());
    }
    personnel.setHealth(snapshot.health());
    personnel.setStrain(snapshot.strain());
    personnel.setStunned(snapshot.stunned());
    personnel.setFocused(snapshot.focused());
    personnel.applyConditionNames(snapshot.conditions());
    personnel.setActive(snapshot.active());
    personnel.setPossibleTarget(snapshot.possibleTarget());
    personnel.setDefeated(snapshot.defeated());
    if (personnel instanceof Hero hero) {
      hero.setExhausted(snapshot.exhausted());
      hero.setWounded(snapshot.wounded());
      hero.applyEquipmentIds(snapshot.equipmentIds());
    }
  }
}
