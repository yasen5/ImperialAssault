package game;

import java.awt.Graphics;

public class Terminal<ValidInteractors extends Personnel> extends Interactable<ValidInteractors> {
    private boolean active = true;

    public Terminal(Pos pos, Class<ValidInteractors> validInteractorClass) {
        super(pos, validInteractorClass, "RedTerminalToken", null);
    }

    @Override
    public void safeInteract(ValidInteractors interactor) {
        active = false;
        if (game != null) {
            if (interactor instanceof Imperial) {
                game.endGame(false);
            } else {
                game.repaintScreen();
            }
        }
    }

    @Override
    public boolean canInteract() {
        return active;
    }

    @Override
    public void draw(Graphics g) {
        if (active) {
            super.draw(g);
        }
    }

    @Override
    public void applySnapshotState(boolean active) {
        this.active = active;
    }
}
