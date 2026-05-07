package runners;

import game.Pathfinder;
import game.Pos;

public class Test {
    public static void main(String[] args) {

        Pos p1, p2;
        for (int i = 0; i < 1; i++) {
            // do {
            //     p1 = new Pos((int) (Math.random() * Constants.tileMatrix[0].length),
            //         (int) (Math.random() * Constants.tileMatrix.length));
            // } while (!p1.isOnGrid());
            // do {
            //     p2 = new Pos((int) (Math.random() * Constants.tileMatrix[0].length),
            //             (int) (Math.random() * Constants.tileMatrix.length));
            // } while (!p2.isOnGrid());
            p1 = new Pos(1, 6);
            p2 = new Pos(2, 5);
            System.out.println(
                    "Can reach point " + p1 + " from point " + p2 + " : " + Pathfinder.straightlineToPos(p1, p2));
        }
    }
}
