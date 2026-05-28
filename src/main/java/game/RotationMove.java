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
      return new RotationMove(new Pos(0, 0), 1, 1);
    }
    String[] parts = value.split(":");
    if (parts.length != 5) {
      return new RotationMove(new Pos(0, 0), 1, 1);
    }
    return new RotationMove(new Pos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])),
        Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
  }
}
