package game;

import java.awt.Graphics;

import game.Constants;
import game.Constants.WallLine;
import game.Personnel.Directions;

// Interactable that blocks attacks and movement until interacted with
public class Door<ValidInteractors extends Personnel> extends Interactable<ValidInteractors> {
    private boolean active = true;
    private static final int longSize = Constants.tileSize * 2, shortSize = 10;

    public Door(Pos pos, Class<ValidInteractors> validInteractorClass) {
        this(pos, validInteractorClass, false);
    }

    public Door(Pos topLeftHinge, Class<ValidInteractors> validInteractorClass, boolean vertical) {
        super(topLeftHinge, validInteractorClass, "Black-Rectangle-PNG",
                vertical ? shortSize : longSize,
                vertical ? longSize : shortSize,
                wallLinesFromTopLeftHinge(topLeftHinge, vertical));
    }

    private static WallLine[] wallLinesFromTopLeftHinge(Pos topLeftHinge, boolean vertical) {
        return vertical
                ? new WallLine[] { new WallLine(topLeftHinge, true, false, false, false),
                        new WallLine(topLeftHinge.getNextPos(Directions.DOWN), true, false, false, false) }
                : new WallLine[] { new WallLine(topLeftHinge, false, false, false, false),
                        new WallLine(topLeftHinge.getNextPos(Directions.RIGHT), false, false, false, false) };
    }

    // Repaint to show that the door isn't there
    @Override
    public void safeInteract(ValidInteractors interactor) {
        active = false;
        if (game != null) {
            game.repaint();
        }
    }

    @Override
    public void draw(Graphics g) {
        if (active) {
            super.draw(g);
        }
    }

    @Override
    public boolean canInteract() {
        return active;
    }

    @Override
    public void applySnapshotState(boolean active) {
        this.active = active;
    }

    public void close() {
        active = true;
        if (game != null) {
            game.repaint();
        }
    }
}
