package game;

public interface GameUi {
    void repaint();

    void setTurnStatus(PlayerSeat seat);

    void deactiveateMovementButtons();

    void endGame(boolean rebelsWin);

    void showBanner(String text);

    void showBannerFromSnapshot(String text, long remainingMs);

    int getSidebarDiceX();

    int getDiceStartY();

    int getSidebarDetailWidth();
}
