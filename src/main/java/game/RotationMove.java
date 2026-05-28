package game;

public record RotationMove(Pos anchor, int xSize, int ySize) {
  private static final String PREFIX = "ROTATE:";

  public String token() {
    return PREFIX + anchor.getX() + ":" + anchor.getY() + ":" + xSize + ":" + ySize;
  }

  public static boolean isToken(String value) {
    return value != null && value.startsWith(PREFIX);
  }

  public static RotationMove fromToken(String value) {
    if (!isToken(value)) {
      throw new IllegalArgumentException("Not a rotation token: " + value);
    }
    String[] parts = value.split(":");
    if (parts.length != 5) {
      throw new IllegalArgumentException("Invalid rotation token: " + value);
    }
    return new RotationMove(new Pos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])),
        Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
  }
}
