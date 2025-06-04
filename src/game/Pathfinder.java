package src.game;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import src.game.Personnel.Directions;

import src.Constants;
import src.Constants.WallLine;

public class Pathfinder {
    public static double positionTolerance = Constants.tileSize * 0.1;
    public static double raySpeed = Constants.tileSize * 0.5;
    public static double closeRaySpeed = Constants.tileSize * 0.05;
    public static double closeDistance = raySpeed;
    public static int maxIters = 50;

    public static record Point(Pos pos, Point parent) {
    }

    public static record FullPos(double x, double y) {}

    public static boolean canReachPoint(Pos start, Pos end, int maxMoves, boolean respectFigures) {
        return canReachPoint(start, end, maxMoves, respectFigures, true);
    }

    public static boolean canReachPoint(Pos start, Pos end, int maxMoves, boolean respectFigures, boolean respectSoftBarriers) {
        if (!start.isOnGrid()) {
            return false;
        }
        if (start.isEqualTo(end)) {
            return true;
        }
        int[][] grid = Constants.tileMatrix;
        boolean[][] visited = new boolean[grid.length][grid[0].length];
        Queue<Point> queue = new LinkedList<>();
        Point currentPoint;
        ArrayList<Directions> validDirections = new ArrayList<>();
        for (Directions dir : Directions.values()) {
            Pos point = start.getNextPos(dir);
            boolean xCloser = Math.abs(point.getX() - end.getX()) <= Math.abs(start.getX() - end.getX());
            boolean yCloser = Math.abs(point.getY() - end.getY()) <= Math.abs(start.getY() - end.getY());
            if (xCloser
                    && yCloser) {
                validDirections.add(dir);
            }
        }
        queue.add(new Point(start, null));
        do {
            currentPoint = queue.poll();
            visited[currentPoint.pos().getY()][currentPoint.pos().getX()] = true;

            if (currentPoint.pos().getX() == end.getX() && currentPoint.pos().getY() == end.getY()) {
                ArrayList<Point> bestPath = new ArrayList<>();
                while (currentPoint.parent() != null) {
                    bestPath.add(currentPoint);
                    currentPoint = currentPoint.parent();
                }
                boolean reachableUnderMax = bestPath.size() <= maxMoves;
                if (reachableUnderMax) {
                }
                return reachableUnderMax;
            } else {
                for (Directions dir : validDirections) {
                    Pos nextPos = currentPoint.pos().getNextPosUnsafe(dir, respectFigures, respectSoftBarriers);
                    if (nextPos != null && !visited[nextPos.getY()][nextPos.getX()]) {
                        queue.add(new Point(nextPos, currentPoint));
                    }
                }
            }
        } while (!queue.isEmpty());
        return false;
    }

    public static boolean straightlineToPos(Pos startingLocation, Pos endingLocation) {
        if (startingLocation.isEqualTo(endingLocation)) {
            return true;
        }
        int numIters = 0;
        double c_x = startingLocation.getFullX();
        double c_y = startingLocation.getFullY();
        int g_x = endingLocation.getFullX();
        int g_y = endingLocation.getFullY();
        double angle = Math.atan2(g_y - c_y, g_x - c_x);
        while (!inTolerance(c_x, g_x, positionTolerance) || !inTolerance(c_y, g_y, positionTolerance)) {
            numIters++;
            boolean closeToFinish = pointDistance(c_x, c_y, g_x, g_y) < closeDistance;
            c_x += (closeToFinish ? closeRaySpeed : raySpeed) * Math.cos(angle);
            c_y += (closeToFinish ? closeRaySpeed : raySpeed) * Math.sin(angle);
            if (Constants.tileMatrix[(int) (c_y / Constants.tileSize)][(int) (c_x / Constants.tileSize)] == 0) {
                return false;
            }
            if (numIters >= maxIters) {
                System.out.println("Failed because of " + numIters + " iterations");
                System.exit(0);
            }
        }
        for (Interactable<? extends Personnel> interactable : Game.interactables) {
                if (interactable.blocking()) {
                    for (WallLine line : interactable.getWallLines()) {
                        if (line.intersects(startingLocation.getFullPos(), endingLocation.getFullPos(), false)) {
                            return false;
                        }
                    }
                }
            }
        return true;
    }

    public static boolean inTolerance(double currValue, double goalValue, double tolerance) {
        return Math.abs(goalValue - currValue) < tolerance;
    }

    public static double pointDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static double orientation(FullPos p, FullPos q, FullPos r) {
        return (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
    }

    public static boolean pointOnSegment(FullPos p, FullPos q, FullPos r) {
        return q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) &&
                q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y);
    }

    public static boolean intersection(FullPos a, FullPos b, FullPos c, FullPos d) {
        // Find orientations
        double cSide = orientation(a, b, c);
        double dSide = orientation(a, b, d);
        double aSide = orientation(c, d, a);
        double bSide = orientation(c, d, b);

        // General case - opposite orientations
        if ((cSide > 0) != (dSide > 0) && (aSide > 0) != (bSide > 0)) {
            return true;
        }

        // Special collinear cases
        return (cSide == 0 && pointOnSegment(a, c, b) ||
        dSide == 0 && pointOnSegment(a, d, b) ||
        aSide == 0 && pointOnSegment(c, a, d) ||
        bSide == 0 && pointOnSegment(c, b, d));
    }
}
