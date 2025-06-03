package src.game;

import java.awt.Graphics;

import src.Constants;

public class Door<ValidInteractors extends Personnel> extends Interactable<ValidInteractors> {
    private boolean active = true;
    private static final int xSize = Constants.tileSize * 2, ySize = 10;

    public Door(Pos pos, Class<ValidInteractors> validInteractorClass) {
        super(pos, validInteractorClass, "Black-Rectangle-PNG.png", xSize, ySize);
    }

    @Override
    public void safeInteract(ValidInteractors interactor) {
        active = false;
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
