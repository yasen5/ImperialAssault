package src.game;

public class Terminal<ValidInteractors extends Personnel> extends Interactable<ValidInteractors> {
    public Terminal(Pos pos, Class<ValidInteractors> validInteractorClass) {
        super(pos, validInteractorClass, "RedTerminalToken.png");
    }

    @Override
    public void safeInteract(ValidInteractors interactor) {
        Game.endGame(false);
    }
}
