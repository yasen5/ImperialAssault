package game;

import java.util.Objects;
import java.util.Optional;

import game.Personnel.Directions;

public final class MovementChoice {
  private final Optional<Directions> direction;
  private final Optional<RotationMove> rotationMove;

  private MovementChoice(Optional<Directions> direction, Optional<RotationMove> rotationMove) {
    this.direction = direction;
    this.rotationMove = rotationMove;
  }

  public static MovementChoice direction(Directions direction) {
    return new MovementChoice(Optional.of(Objects.requireNonNull(direction)), Optional.empty());
  }

  public static MovementChoice rotate(RotationMove rotationMove) {
    return new MovementChoice(Optional.empty(), Optional.of(Objects.requireNonNull(rotationMove)));
  }

  public boolean rotateAction() {
    return rotationMove.isPresent();
  }

  public Optional<Directions> direction() {
    return direction;
  }

  public Optional<RotationMove> rotationMove() {
    return rotationMove;
  }

  @Override
  public String toString() {
    return rotationMove.map(RotationMove::token).orElseGet(() -> direction.orElseThrow().name());
  }
}
