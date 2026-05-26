package game;

import java.util.HashSet;
import java.util.Set;

import game.Constants.WallLine;
import game.Personnel.Actions;
import game.Personnel.Directions;
import util.MyArrayList;

final class GameActionController {
  private final Game game;

  GameActionController(Game game) {
    this.game = game;
  }

  void removeDeadFigures() {
    for (int i = 0; i < game.heroes.size(); i++) {
      if (game.heroes.get(i).getDead()) {
        game.heroes.remove(i);
        i--;
      }
    }
    for (int i = 0; i < game.imperialDeployments.size(); i++) {
      game.imperialDeployments.get(i).removeDeadFigures();
    }
    game.repaint();
  }

  int takeAction(Personnel activeFigure, boolean rebel) {
    MyArrayList<Actions> availableActions = getAvailableActions(activeFigure, rebel);
    if (availableActions.isEmpty()) {
      return 0;
    }
    Actions chosenAction = availableActions.get(game.promptMultipleChoice(activeFigure.getOwnerSeat(),
        "Action Selection", actionSelectionPrompt(availableActions), availableActions.toArray()));
    if (chosenAction == Actions.USE_EQUIPMENT) {
      takeAction(activeFigure, chosenAction);
      return takeAction(activeFigure, rebel);
    }
    return takeAction(activeFigure, chosenAction);
  }

  int takeAction(Personnel activeFigure, Actions action) {
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
        game.checkEndGame();
      }
      case RECOVER -> {
        Hero hero = (Hero) activeFigure;
        hero.recover();
        offerAfterRecoverEquipment(hero);
      }
      case DISCARD_CONDITION -> activeFigure.setStunned(false);
      case USE_EQUIPMENT -> {
        if (activeFigure instanceof Hero hero) {
          handleEquipmentUse(hero, Equipment.UseTiming.DURING_ACTIVATION);
        }
      }
      case SPECIAL -> {
        game.specialUsedThisActivation.add(activeFigure);
        handleSpecial(activeFigure);
      }
      case INTERACT -> {
        handleInteraction(activeFigure);
        game.checkEndGame();
      }
    }
    if (game.gameEnd) {
      return leftoverMoves;
    }
    applyAfterActionConditions(activeFigure, action);
    game.actionsUsedThisActivation.add(action);
    game.checkEndGame();
    return leftoverMoves;
  }

  MyArrayList<Actions> getAvailableActions(Personnel activeFigure, boolean rebel) {
    MyArrayList<Actions> availableActions = new MyArrayList<>();
    availableActions.addAll(activeFigure.getActions());
    game.availableTargets = availableDefenders(activeFigure, rebel);
    if (activeFigure.stunned()) {
      availableActions.remove(Actions.ATTACK);
      availableActions.remove(Actions.SPECIAL);
      availableActions.add(Actions.DISCARD_CONDITION);
    }
    if (game.availableTargets.size() == 0) {
      availableActions.remove(Actions.ATTACK);
      if (activeFigure.specialNeedsAttackTarget()) {
        availableActions.remove(Actions.SPECIAL);
      }
    }
    if (canInteract(activeFigure)) {
      availableActions.add(Actions.INTERACT);
    }
    if (game.specialUsedThisActivation.contains(activeFigure)) {
      availableActions.remove(Actions.SPECIAL);
    }
    if (activeFigure instanceof Hero hero && hero.hasUsableEquipment(Equipment.UseTiming.DURING_ACTIVATION)) {
      availableActions.add(Actions.USE_EQUIPMENT);
    }
    for (int i = 0; i < availableActions.size(); i++) {
      if (!activeFigure.canTakeAction(availableActions.get(i), game.actionsUsedThisActivation)) {
        availableActions.remove(i);
        i--;
      }
    }
    return availableActions;
  }

  void handleSpecial(Personnel activeFigure) {
    if (activeFigure.specialRequiresSelection()) {
      performSpecialInternal(activeFigure);
    } else {
      activeFigure.performSpecial();
      game.repaint();
    }
  }

  void handleInteraction(Personnel activeFigure) {
    getAdjacentInteractable(activeFigure).interact(activeFigure);
    game.repaint();
  }

  Interactable<? extends Personnel> getAdjacentInteractable(Personnel activeFigure) {
    Pos activePos = activeFigure.getPos();
    for (Directions dir : Directions.values()) {
      Pos nextPos = activePos.getNextPos(dir);
      for (Interactable<? extends Personnel> interactable : game.interactables) {
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

  boolean canInteract(Personnel activeFigure) {
    return getAdjacentInteractable(activeFigure) != null;
  }

  MyArrayList<Personnel> availableDefenders(Personnel attacker, boolean rebelAttacker) {
    MyArrayList<Personnel> defenders = new MyArrayList<>();
    if (rebelAttacker) {
      for (DeploymentGroup<? extends Imperial> group : game.imperialDeployments) {
        if (group.getDeployed()) {
          for (Imperial member : group.getMembers()) {
            if (attacker.canAttack(member)) {
              defenders.add(member);
            }
          }
        }
      }
      for (MissionTerminal terminal : game.missionTerminals) {
        if (!terminal.isDefeated() && attacker.canAttack(terminal)) {
          defenders.add(terminal);
        }
      }
    } else {
      for (Hero hero : game.heroes) {
        if (attacker.canAttack(hero)) {
          defenders.add(hero);
        }
      }
    }
    return defenders;
  }

  MyArrayList<Hero> getHeroExhaustOptions() {
    MyArrayList<Hero> readyDeployments = new MyArrayList<>();
    for (Hero hero : game.heroes) {
      if (!hero.getExhausted()) {
        readyDeployments.add(hero);
      }
    }
    return readyDeployments;
  }

  MyArrayList<Hero> getHeroExhaustOptions(PlayerSeat seat) {
    MyArrayList<Hero> readyDeployments = new MyArrayList<>();
    for (Hero hero : game.heroes) {
      if (!hero.getExhausted() && hero.getOwnerSeat() == seat) {
        readyDeployments.add(hero);
      }
    }
    return readyDeployments;
  }

  MyArrayList<DeploymentGroup<? extends Imperial>> getImperialExhaustOptions() {
    MyArrayList<DeploymentGroup<? extends Imperial>> readyDeployments = new MyArrayList<>();
    for (DeploymentGroup<? extends Imperial> deploymentGroup : game.imperialDeployments) {
      if (deploymentGroup.getDeployed() && !deploymentGroup.getExhausted() && deploymentGroup.hasReadyMembers()) {
        readyDeployments.add(deploymentGroup);
      }
    }
    return readyDeployments;
  }

  boolean isSpaceAvailable(Pos pos) {
    for (Hero hero : game.heroes) {
      if (hero.occupiesSpace(pos)) {
        return false;
      }
    }
    for (DeploymentGroup<? extends Imperial> depGroup : game.imperialDeployments) {
      if (!depGroup.getDeployed()) {
        continue;
      }
      for (Imperial imperial : depGroup.getMembers()) {
        if (imperial.occupiesSpace(pos)) {
          return false;
        }
      }
    }
    return true;
  }

  boolean isSpaceAvailable(Pos pos, Personnel ignore) {
    for (Hero hero : game.heroes) {
      if ((ignore == null || !ignore.equals(hero)) && hero.occupiesSpace(pos)) {
        return false;
      }
    }
    for (DeploymentGroup<? extends Imperial> depGroup : game.imperialDeployments) {
      if (!depGroup.getDeployed()) {
        continue;
      }
      for (Imperial imperial : depGroup.getMembers()) {
        if ((ignore == null || !ignore.equals(imperial)) && imperial.occupiesSpace(pos)) {
          return false;
        }
      }
    }
    return true;
  }

  void handleMovesInternal(Personnel activeFigure, int numMoves) {
    for (int j = 0; j < numMoves; j++) {
      if (!handleMoveInternal(activeFigure)) {
        break;
      }
    }
    if (game.ui != null) {
      game.ui.deactiveateMovementButtons();
    }
    game.repaint();
  }

  void handlePendingMoves(Personnel activeFigure, PlayerSeat seat, int leftoverMoves) {
    handleMovesInternal(activeFigure, promptPendingMoves(seat, leftoverMoves));
  }

  void handleAttackInternal(Personnel activeFigure) {
    game.currentSelected = new java.util.concurrent.CompletableFuture<>();
    game.availableTargets = availableDefenders(activeFigure, activeFigure.getOwnerSeat().isRebel());
    if (game.availableTargets.isEmpty()) {
      game.triggerBanner(activeFigure.getName() + " has no available target");
      return;
    }
    for (Personnel person : game.availableTargets) {
      person.setPossibleTarget(true);
    }
    game.repaint();
    Personnel chosenDefender = game.decisionProvider.chooseTarget(activeFigure.getOwnerSeat(), SelectionType.COMBAT,
        new MyArrayList<>(game.availableTargets));
    for (Personnel person : game.availableTargets) {
      person.setPossibleTarget(false);
    }
    activeFigure.performAttack(chosenDefender);
    game.repaint();
  }

  boolean trySetTarget(Personnel defender) {
    if (game.availableTargets.contains(defender)) {
      game.currentSelected.complete(defender);
      return true;
    }
    return false;
  }

  MyArrayList<Imperial> getImperials() {
    MyArrayList<Imperial> imperials = new MyArrayList<>();
    for (DeploymentGroup<? extends Imperial> depGroup : game.imperialDeployments) {
      if (depGroup.getDeployed()) {
        imperials.addAll(depGroup.getMembers());
      }
    }
    return imperials;
  }

  void applyBlast(Personnel target, int blastValue) {
    applyBlast(null, target, blastValue);
  }

  void applyBlast(Personnel attacker, Personnel target, int blastValue) {
    if (target == null || blastValue <= 0) {
      return;
    }
    Set<Personnel> affected = new HashSet<>();
    for (Personnel targetSpaceOwner : allPersonnel()) {
      if (targetSpaceOwner == target) {
        continue;
      }
      if (attacker != null && !areEnemies(attacker, targetSpaceOwner)) {
        continue;
      }
      for (Pos targetSpace : target.getOccupiedSpaces()) {
        if (isAdjacentToAnyOccupiedSpace(targetSpaceOwner, targetSpace)) {
          affected.add(targetSpaceOwner);
          break;
        }
      }
    }
    for (Personnel personnel : affected) {
      personnel.dealDamage(blastValue);
    }
  }

  boolean isAdjacentToImperial(Pos pos) {
    for (Imperial imperial : getImperials()) {
      if (isAdjacent(pos, imperial.getPos())) {
        return true;
      }
    }
    return false;
  }

  private String actionSelectionPrompt(MyArrayList<Actions> availableActions) {
    StringBuilder prompt = new StringBuilder("Choose an action to take");
    boolean hasDescriptions = false;
    for (Actions action : availableActions) {
      if (game.missionDefinition.actionDescription(action) != null) {
        hasDescriptions = true;
        break;
      }
    }
    if (!hasDescriptions) {
      return prompt.toString();
    }
    prompt.append("\n\n");
    for (Actions action : availableActions) {
      String description = game.missionDefinition.actionDescription(action);
      if (description != null) {
        prompt.append(action).append(": ").append(description).append("\n\n");
      }
    }
    return prompt.toString().trim();
  }

  private void applyAfterActionConditions(Personnel activeFigure, Actions action) {
    if (activeFigure instanceof Hero hero && hero.bleeding() && action != Actions.RECOVER
        && action != Actions.DISCARD_CONDITION) {
      hero.ApplyStrain(1);
    }
  }

  private void offerAfterRecoverEquipment(Hero hero) {
    if (!hero.hasUsableEquipment(Equipment.UseTiming.AFTER_RECOVER)) {
      return;
    }
    if (game.promptYesNo(hero.getOwnerSeat(), "Equipment", "Use equipment after resting?")) {
      handleEquipmentUse(hero, Equipment.UseTiming.AFTER_RECOVER);
    }
  }

  private void handleEquipmentUse(Hero owner, Equipment.UseTiming timing) {
    MyArrayList<Equipment.Item> usableEquipment = owner.getUsableEquipment(timing);
    if (usableEquipment.isEmpty()) {
      return;
    }
    Equipment.Item item = usableEquipment.get(usableEquipment.size() == 1 ? 0
        : game.promptMultipleChoice(owner.getOwnerSeat(), "Equipment", "Choose equipment to use",
            usableEquipment.toArray()));
    MyArrayList<Hero> targets = getFriendlyAdjacentHeroes(owner);
    if (targets.isEmpty()) {
      return;
    }
    Hero target = targets.get(targets.size() == 1 ? 0
        : game.promptMultipleChoice(owner.getOwnerSeat(), item.name(), "Choose a target", targets.toArray()));
    if (item.recoverAmount() > 0) {
      target.dealDamage(-item.recoverAmount());
    }
    if (item.grantsFocus()) {
      target.setFocused(true);
    }
    if (item.consumable()) {
      owner.removeEquipment(item);
    }
    game.triggerBanner(owner.getDisplayName() + " used " + item.name());
    game.repaint();
  }

  private MyArrayList<Hero> getFriendlyAdjacentHeroes(Hero owner) {
    MyArrayList<Hero> targets = new MyArrayList<>();
    for (Hero hero : game.heroes) {
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

  private int promptPendingMoves(PlayerSeat seat, int leftoverMoves) {
    if (leftoverMoves == 0) {
      return 0;
    }
    return game.promptNumericChoice(seat, "# of moves you'll use", 0, leftoverMoves);
  }

  private boolean handleMoveInternal(Personnel activeFigure) {
    MyArrayList<Directions> availableDirections = new MyArrayList<>();
    for (Directions direction : Directions.values()) {
      if (activeFigure.canMove(direction)) {
        availableDirections.add(direction);
      }
    }
    MyArrayList<RotationMove> legalRotations = MovementRules.getLegalRotations(activeFigure, game);
    if (availableDirections.isEmpty() && legalRotations.isEmpty()) {
      game.triggerBanner(activeFigure.getName() + " cannot move farther");
      return false;
    }
    MovementChoice choice = game.decisionProvider.chooseMovement(activeFigure.getOwnerSeat(), activeFigure,
        availableDirections, legalRotations);
    if (choice.rotateAction()) {
      RotationMove rotationMove = choice.rotationMove() == null ? legalRotations.get(0) : choice.rotationMove();
      if (!containsRotation(legalRotations, rotationMove)) {
        game.triggerBanner(activeFigure.getName() + " cannot rotate there");
        return false;
      }
      activeFigure.rotateTo(rotationMove);
    } else {
      activeFigure.move(choice.direction());
    }
    game.repaint();
    return true;
  }

  private boolean containsRotation(MyArrayList<RotationMove> legalRotations, RotationMove target) {
    for (RotationMove rotation : legalRotations) {
      if (rotation.anchor().equalTo(target.anchor()) && rotation.xSize() == target.xSize()
          && rotation.ySize() == target.ySize()) {
        return true;
      }
    }
    return false;
  }

  private void performSpecialInternal(Personnel activeFigure) {
    game.availableTargets = activeFigure.getSpecialTargets();
    for (Personnel person : game.availableTargets) {
      person.setPossibleTarget(true);
    }
    game.repaint();
    Personnel chosenDefender = game.decisionProvider.chooseTarget(activeFigure.getOwnerSeat(), SelectionType.SPECIAL,
        new MyArrayList<>(game.availableTargets));
    for (Personnel person : game.availableTargets) {
      person.setPossibleTarget(false);
    }
    activeFigure.performSpecial(chosenDefender);
    game.repaint();
  }

  private boolean areEnemies(Personnel first, Personnel second) {
    return first.getOwnerSeat().isRebel() != second.getOwnerSeat().isRebel();
  }

  private MyArrayList<Personnel> allPersonnel() {
    MyArrayList<Personnel> personnel = new MyArrayList<>();
    personnel.addAll(game.heroes);
    personnel.addAll(getImperials());
    personnel.addAll(game.missionTerminals);
    return personnel;
  }

  private boolean isAdjacentToAnyOccupiedSpace(Personnel personnel, Pos targetSpace) {
    for (Pos occupiedSpace : personnel.getOccupiedSpaces()) {
      int xDistance = Math.abs(occupiedSpace.getX() - targetSpace.getX());
      int yDistance = Math.abs(occupiedSpace.getY() - targetSpace.getY());
      if (xDistance <= 1 && yDistance <= 1 && (xDistance + yDistance) > 0) {
        return true;
      }
    }
    return false;
  }
}
