package src.game;

import java.awt.Graphics;

import src.Constants;
import src.Constants.WallLine;
import src.game.Personnel.Directions;

// Interactable that blocks attacks and movement until interacted with
public class Door<ValidInteractors extends Personnel> extends Interactable<ValidInteractors> {
    private boolean active = true;
    private static final int xSize = Constants.tileSize * 2, ySize = 10;

    public Door(Pos pos, Class<ValidInteractors> validInteractorClass) {
        super(pos, validInteractorClass, "Black-Rectangle-PNG.png", xSize, ySize,
                new WallLine[] { new WallLine(pos, false, false, false, false), 
                        new WallLine(pos.getNextPos(Directions.RIGHT), false, false, false, false) });
    }

    // Repaint to show that the door isn't there
    @Override
    public void safeInteract(ValidInteractors interactor) {
        active = false;
        Game.repaintScreen();
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
}
