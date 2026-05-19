package game;

import game.Personnel.Directions;

public record MovementChoice(Directions direction, boolean rotateAction) {
  public static MovementChoice direction(Directions direction) {
    return new MovementChoice(direction, false);
  }

  public static MovementChoice rotate() {
    return new MovementChoice(null, true);
  }

  @Override
  public String toString() {
    return rotateAction ? "ROTATE" : direction.name();
  }
}
