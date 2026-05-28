package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import util.MyArrayList;

import java.util.Arrays;
import java.util.Objects;

import game.Constants;
import game.Constants.EndpointTouchPolicy;
import game.Constants.WallLine;
import game.Die.*;
import game.FullDeployment.PersonnelStatus;
import game.SelectionType;
import visual.UiContext;

public abstract class Personnel {
  // Instance variables
  private int startingHealth, health, speed;
  private int xSize = 1, ySize = 1;
  private Pos pos;
  private Pos[] corners;
  protected boolean stunned = false, focused = false;
  private boolean bleeding = false;
  private boolean defeated = false;
  private BufferedImage image;
  private String name;
  protected DefenseDieType[] defenseDice;
  private static int imageSideSpace = 7;
  private boolean possibleTarget = false, active = false;
  protected MyArrayList<Actions> actions;
  protected int strain = 0;
  private boolean specialRequiresSelection;
  private String id;
  private PlayerSeat ownerSeat = PlayerSeat.IMPERIAL;
  protected Game game;

  public static enum Directions {
    UP,
    UPLEFT,
    LEFT,
    DOWNLEFT,
    DOWN,
    DOWNRIGHT,
    RIGHT,
    UPRIGHT
  }

  public static enum Actions {
    MOVE,
    ATTACK,
    RECOVER,
    USE_EQUIPMENT,
    DISCARD_CONDITION,
    SPECIAL,
    INTERACT
  }

  public Personnel(String name, int startingHealth, int speed, Pos pos, DefenseDieType[] defenseDice,
      boolean hasSpecial, boolean specialRequiresSelection) {
    this.name = name;
    this.startingHealth = startingHealth;
    this.health = startingHealth;
    this.speed = speed;
    this.defenseDice = defenseDice;
    this.specialRequiresSelection = specialRequiresSelection;
    image = LoaderUtils.getImage(name);
    this.pos = pos;
    updateCorners();
    this.actions = new MyArrayList<>(Arrays.asList(Actions.MOVE, Actions.ATTACK));
    if (hasSpecial) {
      actions.add(Actions.SPECIAL);
    }
  }

  // Perform an attack by getting the defender's results, subtracting them from
  // your results, and then doing surge effects, then figuring out if you had
  // enough range
  public void performAttack(Personnel other) {
    AttackResolver.resolve(this, other, requireGame());
  }

  // Roll all the defense dice
  public DefenseRoll[] getDefense() {
    DefenseRoll[] results = new DefenseRoll[defenseDice.length];
    for (int i = 0; i < defenseDice.length; i++) {
      results[i] = defenseDice[i].roll(requireGame());
    }
    return results;
  }

  public abstract OffenseRoll[] getOffense();

  public abstract Equipment.SurgeOptions[] getSurgeOptions();

  // Deal damage, can't go above starting health
  public void dealDamage(int damage) {
    if (defeated) {
      return;
    }
    LoaderUtils.playSound("short-blaster");
    health -= damage;
    if (health > startingHealth) {
      health = startingHealth;
    }
    if (health <= 0) {
      defeated = true;
    }
  }

  public boolean getDead() {
    return defeated || health <= 0;
  }

  public void move(Directions dir) {
    pos.move(dir);
    updateCorners();
  }

  public boolean canRotate() {
    return MovementRules.canRotate(this, game);
  }

  public void rotate() {
    int previousXSize = xSize;
    xSize = ySize;
    ySize = previousXSize;
    updateCorners();
  }

  public void rotateTo(RotationMove rotationMove) {
    pos = rotationMove.anchor();
    xSize = rotationMove.xSize();
    ySize = rotationMove.ySize();
    updateCorners();
  }

  public void setStunned(boolean value) {
    stunned = value;
  }

  public void addCondition(Condition condition) {
    switch (condition) {
      case STUNNED -> stunned = true;
      case FOCUSED -> focused = true;
      case BLEEDING -> bleeding = true;
    }
  }

  public void removeCondition(Condition condition) {
    switch (condition) {
      case STUNNED -> stunned = false;
      case FOCUSED -> focused = false;
      case BLEEDING -> bleeding = false;
    }
  }

  public boolean hasCondition(Condition condition) {
    return switch (condition) {
      case STUNNED -> stunned;
      case FOCUSED -> focused;
      case BLEEDING -> bleeding;
    };
  }

  public MyArrayList<Condition> getConditions() {
    MyArrayList<Condition> conditions = new MyArrayList<>();
    if (stunned) {
      conditions.add(Condition.STUNNED);
    }
    if (focused) {
      conditions.add(Condition.FOCUSED);
    }
    if (bleeding) {
      conditions.add(Condition.BLEEDING);
    }
    return conditions;
  }

  public MyArrayList<String> getConditionNames() {
    MyArrayList<String> names = new MyArrayList<>();
    for (Condition condition : getConditions()) {
      names.add(condition.name());
    }
    return names;
  }

  public void applyConditionNames(MyArrayList<String> conditions) {
    stunned = false;
    focused = false;
    bleeding = false;
    if (conditions == null) {
      return;
    }
    for (String condition : conditions) {
      if (condition != null) {
        addCondition(Condition.valueOf(condition));
      }
    }
  }

  public DefenseRoll[] getDefense(Personnel other) {
    return other.getDefense();
  }

  // Draw the image, do various shades on top based on whether it is active, being
  // targeted, etc.
  public void draw(Graphics g) {
    g.drawImage(image, pos.getFullX() + imageSideSpace,
        pos.getFullY() + imageSideSpace,
        Constants.tileSize * (pos.getX() + xSize) - imageSideSpace,
        Constants.tileSize * (pos.getY() + ySize) - imageSideSpace, 0, 0, image.getWidth(null),
        image.getHeight(null),
        null);
    if ((UiContext.getSelectionType() == SelectionType.COMBAT || UiContext.getSelectionType() == SelectionType.SPECIAL)
        && !possibleTarget) {
      g.setColor(new Color(0, 0, 0, 70));
      g.fillRect(pos.getFullX() + imageSideSpace,
          pos.getFullY() + imageSideSpace, Constants.tileSize * xSize - 2 * imageSideSpace,
          Constants.tileSize * ySize - 2 * imageSideSpace);
    }
    if (active) {
      g.setColor(new Color(0, 255, 0, 70));
      g.fillRect(pos.getFullX() + imageSideSpace,
          pos.getFullY() + imageSideSpace, Constants.tileSize * xSize - 2 * imageSideSpace,
          Constants.tileSize * ySize - 2 * imageSideSpace);
    }
  }

  public String getName() {
    return name;
  }

  public int getSpeed() {
    return speed;
  }

  public boolean canMove(Directions dir) {
    return MovementRules.canMoveOneSpace(this, dir, requireGame());
  }

  public Pos getPos() {
    return pos;
  }

  // Optional functions that define special action behavior
  public void performSpecial() {
    if (specialRequiresSelection) {
      return;
    }
  }

  public void performSpecial(Personnel selected) {
    if (!specialRequiresSelection) {
      return;
    }
  }

  public void onActivationStart() {
  }

  public void onActivationEnd() {
  }

  public void applyAttackAbilities(Personnel defender, TotalAttackResult totalResults) {
  }

  public int getRange() {
    return Integer.MAX_VALUE;
  }

  public int getBlastValue() {
    return 0;
  }

  public boolean specialNeedsAttackTarget() {
    return false;
  }

  // Checks if the defender is in line of sight and if melee, if they are < range
  // spaces away
  public boolean canAttack(Personnel other) {
    int range = getRange();
    for (Pos attackSpace : getOccupiedSpaces()) {
      for (Pos targetSpace : other.getOccupiedSpaces()) {
        if (range != Integer.MAX_VALUE
            && !Pathfinder.canReachPoint(attackSpace, targetSpace, range, false, requireGame())) {
          continue;
        }
        if (hasLineOfSightToSpace(attackSpace, targetSpace) && hasLineOfSightTo(other)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean hasLineOfSightTo(Personnel other) {
    if (other instanceof MakEshray mak && mak.isCovertAgainst(this)) {
      return false;
    }
    for (Pos attackSpace : getOccupiedSpaces()) {
      for (Pos targetSpace : other.getOccupiedSpaces()) {
        if (hasLineOfSightToSpace(attackSpace, targetSpace)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasLineOfSightToSpace(Pos attackSpace, Pos targetSpace) {
    if (isPointBlankWithClearCenterLine(attackSpace, targetSpace)) {
      return true;
    }

    for (Pos corner : getCornersForSpace(attackSpace)) {
      Pos[] cornersUsed = new Pos[2];
      int sightCount = 0;
      for (Pos enemyCorner : getCornersForSpace(targetSpace)) {
        if (isHardWallEnd(enemyCorner)) {
          continue;
        }
        if (Pathfinder.straightlineToPos(corner, enemyCorner, requireGame())) {
          cornersUsed[sightCount >= 2 ? 0 : sightCount] = enemyCorner;
          sightCount++;
          if (sightCount >= 2) {
            if (!Pos.onOneLine(attackSpace, cornersUsed[0], cornersUsed[1])) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private boolean isPointBlankWithClearCenterLine(Pos attackSpace, Pos targetSpace) {
    if (Math.abs(attackSpace.getX() - targetSpace.getX()) > 1
        || Math.abs(attackSpace.getY() - targetSpace.getY()) > 1) {
      return false;
    }
    if (attackSpace.isEqualTo(targetSpace)) {
      return false;
    }
    if (Constants.blocksMovement(Constants.wallLines, attackSpace.getCenterPos(), targetSpace.getCenterPos(), false,
        EndpointTouchPolicy.ALLOW)) {
      return false;
    }
    for (Interactable<? extends Personnel> interactable : requireGame().getInteractables()) {
      if (interactable.blocking()) {
        if (Constants.blocksMovement(interactable.getWallLines(), attackSpace.getCenterPos(),
            targetSpace.getCenterPos(), false, EndpointTouchPolicy.ALLOW)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean isHardWallEnd(Pos corner) {
    for (WallLine wallLine : Constants.wallLines) {
      if (!wallLine.softBarrier()) {
        for (Pos hardEnd : wallLine.getHardEnds()) {
          if (hardEnd.equalTo(corner)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public Pos[] getCorners() {
    return corners;
  }

  public Pos[] getOccupiedSpaces() {
    Pos[] occupiedSpaces = new Pos[xSize * ySize];
    int index = 0;
    for (int y = 0; y < ySize; y++) {
      for (int x = 0; x < xSize; x++) {
        occupiedSpaces[index++] = new Pos(pos.getX() + x, pos.getY() + y);
      }
    }
    return occupiedSpaces;
  }

  public boolean occupiesSpace(Pos space) {
    for (Pos occupiedSpace : getOccupiedSpaces()) {
      if (occupiedSpace.equalTo(space)) {
        return true;
      }
    }
    return false;
  }

  public boolean isLargeFigure() {
    return xSize * ySize > 1;
  }

  public boolean isNonSquareLargeFigure() {
    return isLargeFigure() && xSize != ySize;
  }

  public int getXSize() {
    return xSize;
  }

  public int getYSize() {
    return ySize;
  }

  public void setHorizontalOrientation(boolean horizontal) {
    if (!isNonSquareLargeFigure()) {
      return;
    }
    boolean currentlyHorizontal = xSize > ySize;
    if (currentlyHorizontal != horizontal) {
      rotate();
    }
  }

  protected void setFigureSize(int xSize, int ySize) {
    if (xSize < 1 || ySize < 1) {
      return;
    }
    this.xSize = xSize;
    this.ySize = ySize;
    updateCorners();
  }

  private void updateCorners() {
    this.corners = new Pos[] { pos, new Pos(pos.getX() + xSize, pos.getY()),
        new Pos(pos.getX(), pos.getY() + ySize), new Pos(pos.getX() + xSize, pos.getY() + ySize) };
  }

  private Pos[] getCornersForSpace(Pos space) {
    return new Pos[] { space, space.getNextPos(Directions.RIGHT), space.getNextPos(Directions.DOWN),
        space.getNextPos(Directions.DOWNRIGHT) };
  }

  // Get the closest corner to the other Personnel
  public Pos getClosestCorner(Personnel other) {
    double minDistance = Integer.MAX_VALUE;
    Pos otherPos = other.getPos();
    Pos closestCorner = null;
    for (Pos corner : corners) {
      double distance = Pos.getDistance(otherPos, corner);
      if (distance < minDistance) {
        minDistance = distance;
        closestCorner = corner;
      }
    }
    return closestCorner;
  }

  public void setPossibleTarget(boolean value) {
    possibleTarget = value;
  }

  public void setActive(boolean value) {
    active = value;
  }

  public MyArrayList<Actions> getActions() {
    return actions;
  }

  public boolean gainsMoveBeforeImperialAction() {
    return true;
  }

  public int getImperialActionCount() {
    return 1;
  }

  public boolean canTakeAction(Actions action, MyArrayList<Actions> actionsUsedThisActivation) {
    return true;
  }

  public PersonnelStatus getStatus() {
    return new PersonnelStatus(health, strain, stunned, focused, bleeding, false, defeated);
  }

  public boolean stunned() {
    return stunned;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Personnel other)) {
      return false;
    }
    return id != null && other.id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id == null ? System.identityHashCode(this) : Objects.hash(id);
  }

  public void setFocused(boolean focused) {
    this.focused = focused;
  }

  public void setBleeding(boolean bleeding) {
    this.bleeding = bleeding;
  }

  public boolean bleeding() {
    return bleeding;
  }

  public MyArrayList<Personnel> getSpecialTargets() {
    return null;
  }

  public boolean specialRequiresSelection() {
    return specialRequiresSelection;
  }

  public void setStrain(int strain) {
    this.strain = strain;
  }

  public void setHealth(int health) {
    this.health = health;
    this.defeated = health <= 0;
  }

  public void setPos(Pos pos) {
    this.pos = pos;
    updateCorners();
  }

  public boolean isPossibleTarget() {
    return possibleTarget;
  }

  public boolean isActive() {
    return active;
  }

  public int getHealth() {
    return health;
  }

  public int getStartingHealth() {
    return startingHealth;
  }

  public int getStrain() {
    return strain;
  }

  public void setDefeated(boolean defeated) {
    this.defeated = defeated;
  }

  public boolean isDefeated() {
    return defeated;
  }

  public PlayerSeat getOwnerSeat() {
    return ownerSeat;
  }

  public void setOwnerSeat(PlayerSeat ownerSeat) {
    this.ownerSeat = ownerSeat;
  }

  public void setGame(Game game) {
    this.game = Objects.requireNonNull(game, "game");
  }

  protected Game requireGame() {
    return Objects.requireNonNull(game, "game");
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
