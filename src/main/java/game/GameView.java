package game;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public interface GameView {
    void drawGame(Graphics g);

    int getMapDrawWidth();

    void playRound();

    boolean trySetTarget(Personnel defender);

    ArrayList<Hero> getHeroes();

    void reset();

    void increaseThreat();

    void advanceStatusPhase();

    void clearDice();

    Personnel getPersonnelById(String id);

    Personnel getPersonnelAtPos(Pos pos);

    DeploymentCard getDeploymentCard(Pos pos);

    CompletableFuture<Personnel> getCurrentSelection();

    void setCurrentSelection(CompletableFuture<Personnel> currentSelected);
}
