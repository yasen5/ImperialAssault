package src.game;

import src.Constants;
import src.Constants.WallLine;
import src.game.Pathfinder.FullPos;
import src.game.Personnel.Directions;

public class Pos {
    private int x, y;

    public Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int value) {
        x = value;
    }

    public void setY(int value) {
        y = value;
    }

    public void incrementX(int amt) {
        x += amt;
    }

    public void incrementY(int amt) {
        y += amt;
    }

    public boolean equalTo(Pos other) {
        return (this.x == other.getX() && this.y == other.getY());
    }

    // Determines whether you can move in that direction
    // Respect figures: check if you are moving through figures
    // RespectSoftBarriers: check if you are moving through dotted red lines
    public boolean canMove(Directions dir, boolean respectFigures, boolean respectSoftBarriers) {
        int newX = getX();
        int newY = getY();
        switch (dir) {
            case UP -> {
                newY -= 1;
            }
            case DOWN -> {
                newY += 1;
            }
            case LEFT -> {
                newX -= 1;
            }
            case RIGHT -> {
                newX += 1;
            }
            case UPLEFT -> {
                newX -= 1;
                newY -= 1;
            }
            case UPRIGHT -> {
                newX += 1;
                newY -= 1;
            }
            case DOWNLEFT -> {
                newX -= 1;
                newY += 1;
            }
            case DOWNRIGHT -> {
                newX += 1;
                newY += 1;
            }
        }
        boolean inBounds = newX >= 0 && newY >= 0 && newX < Constants.tileMatrix[0].length
                && newY < Constants.tileMatrix.length;
        if (!inBounds) {
            return false;
        }
        FullPos thisCenterPos = new FullPos((int) (getFullX() + Constants.tileSize * 0.5),
                getFullY() + Constants.tileSize * 0.5);
        FullPos nextCenterPos = new FullPos(
                (newX + 0.5) * Constants.tileSize,
                (newY + 0.5) * Constants.tileSize);
        if (respectSoftBarriers) {
            for (WallLine wallLine : Constants.wallLines) {
                if (wallLine.intersects(thisCenterPos,
                        nextCenterPos,
                        true)) {
                    return false;
                }
            }
        }
        for (Interactable<? extends Personnel> interactable : Game.interactables) {
            if (interactable.blocking()) {
                for (WallLine wallLine : interactable.getWallLines()) {
                    if (wallLine.intersects(thisCenterPos,
                            nextCenterPos,
                            true)) {
                        return false;
                    }
                }
            }
        }
        return (Constants.tileMatrix[newY][newX] == 1
                && (!respectFigures || Game.isSpaceAvailable(new Pos(newX, newY))));
    }

    // Move in a direction
    public void move(Directions dir) {
        switch (dir) {
            case UP -> y -= 1;
            case DOWN -> y += 1;
            case LEFT -> x -= 1;
            case RIGHT -> x += 1;
            case UPLEFT -> {
                x -= 1;
                y -= 1;
            }
            case UPRIGHT -> {
                x += 1;
                y -= 1;
            }
            case DOWNLEFT -> {
                x -= 1;
                y += 1;
            }
            case DOWNRIGHT -> {
                x += 1;
                y += 1;
            }
        }
    }

    // Unsafe because it might return null if you can't move there
    public Pos getNextPosUnsafe(Directions dir, boolean respectFigures, boolean respectSoftBarriers) {
        if (!canMove(dir, respectFigures, respectSoftBarriers)) {
            return null;
        }
        return getNextPos(dir);
    }

    // Safe pos, but watch out because it doesn't monitor the validity of the next point
    public Pos getNextPos(Directions dir) {
        Pos nextPoint = new Pos(x, y);
        nextPoint.move(dir);
        return nextPoint;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    public boolean isOnGrid() {
        return x >= 0 && y >= 0 && x < Constants.tileMatrix[0].length
                && y < Constants.tileMatrix.length;
    }

    public boolean isEqualTo(Pos other) {
        return this.getX() == other.getX() && this.getY() == other.getY();
    }

    public int getFullX() {
        return x * Constants.tileSize;
    }

    public int getFullY() {
        return y * Constants.tileSize;
    }

    public FullPos getFullPos() {
        return new FullPos(getFullX(), getFullY());
    }

    // Check if the point got closer in any way
    public static boolean closerToTarget(Pos original, Pos newPos, Pos target) {
        return Math.abs(target.getX() - newPos.getX()) < Math.abs(target.getX() - original.getX())
                || Math.abs(target.getY() - newPos.getY()) < Math.abs(target.getY() - original.getY());
    }

    public static double getDistance(Pos p1, Pos p2) {
        return Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + Math.pow(p2.getY() - p1.getY(), 2));
    }
}
