package game;

public class MissionDoor extends Door<Hero> {
    public MissionDoor(Pos pos, boolean vertical) {
        super(pos, Hero.class, vertical);
    }

    @Override
    public void safeInteract(Hero interactor) {
        super.safeInteract(interactor);
        if (game != null) {
            game.onMissionDoorOpened(this);
        }
    }
}
