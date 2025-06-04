package src.game;

import java.awt.Color;
import java.awt.Graphics;

// Interface for things that have deployment cards and stats
interface FullDeployment {
    public static final int statusSpacing = 100;
    public static final int startingX = DeploymentCard.x - 200;
    public static final Color healthColor = new Color(0, 255, 0), stunColor = new Color(113, 1, 130),
            focusedColor = new Color(1, 66, 20);

    public static record PersonnelStatus(int health, int strain, boolean stunned, boolean focused) {
    }

    public DeploymentCard getDeploymentCard();

    public default void setDeploymentVisibility(boolean value) {
        getDeploymentCard().setVisible(value);
    }

    public void toggleDisplay();

    // Draw all the collective statuses
    public default void drawStats(Graphics g) {
        PersonnelStatus[] statuses = getStatuses();
        for (int i = 0; i < statuses.length; i++) {
            g.setColor(healthColor);
            g.drawString("Health: " + statuses[i].health(), startingX + statusSpacing * i,
                    DeploymentCard.heroYSize + 25);
            g.setColor(stunColor);
            g.drawString("Stunned: " + statuses[i].stunned(), startingX + statusSpacing * i,
                    DeploymentCard.heroYSize + 75);
            g.setColor(focusedColor);
            g.drawString("Focused: " + statuses[i].focused(), startingX + statusSpacing * i,
                    DeploymentCard.heroYSize + 125);
        }
    }

    public PersonnelStatus[] getStatuses();
}
