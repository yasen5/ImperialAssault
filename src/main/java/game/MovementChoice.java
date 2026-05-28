package game;

import game.Personnel.Directions;

public record MovementChoice(Directions direction, RotationMove rotationMove) {
  public static MovementChoice direction(Directions direction) {
    return new MovementChoice(direction, null);
  }

  public static MovementChoice rotate(RotationMove rotationMove) {
    return new MovementChoice(null, rotationMove);
  }

  public static MovementChoice rotate() {
    return new MovementChoice(null, null);
  }

  public boolean rotateAction() {
    return direction == null;
  }

  @Override
  public String toString() {
    return rotateAction() ? "ROTATE" : direction.name();
  }
}
