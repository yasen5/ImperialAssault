package game;

import java.awt.Graphics;

public class SupplyBox extends Interactable<Hero> {
    private boolean active = true;

    public SupplyBox(Pos pos) {
        super(pos, Hero.class, "supply-box", null);
    }

    @Override
    public void safeInteract(Hero interactor) {
        // Future equipment reward logic should be triggered here when supply cards exist.
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
}
