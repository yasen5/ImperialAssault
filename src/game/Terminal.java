package src.game;

public class Terminal<ValidInteractors extends Personnel> extends Interactable<ValidInteractors> {
    public Terminal(Pos pos, Class<ValidInteractors> validInteractorClass) {
        super(pos, validInteractorClass, "RedTerminalToken", null);
    }

    // Interactable that ends the game if interacted with (in the imperials favor)
    @Override
    public void safeInteract(ValidInteractors interactor) {
        Game.endGame(false);
    }
}
