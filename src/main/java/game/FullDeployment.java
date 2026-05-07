package game;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

// Interface for things that have deployment cards and stats
interface FullDeployment {
    Color accentColor = new Color(88, 177, 255);
    Color healthColor = new Color(86, 219, 124);
    Color strainColor = new Color(251, 193, 58);
    Color warningColor = new Color(255, 108, 108);
    Color mutedTextColor = new Color(225, 227, 232);

    public static record PersonnelStatus(int health, int strain, boolean stunned, boolean focused) {
    }

    DeploymentCard getDeploymentCard();

    String getDisplayName();

    default String[] getStatusLabels() {
        return new String[] { getDisplayName() };
    }

    default int getMaxHealth() {
        return -1;
    }

    default Integer getMaxStrain() {
        return null;
    }

    default boolean isExhausted() {
        return false;
    }

    public default void setDeploymentVisibility(boolean value) {
        getDeploymentCard().setVisible(value);
    }

    public void toggleDisplay();

    // Draw all the collective statuses
    public default void drawStats(Graphics g) {
        Screen screen = UiContext.getScreen();
        if (screen == null) {
            return;
        }

        PersonnelStatus[] statuses = getStatuses();
        String[] labels = getStatusLabels();
        int cardX = getDeploymentCard().getDrawX();
        int cardY = getDeploymentCard().getDrawY();
        int panelX = screen.getSidebarDetailX();
        int panelY = cardY;
        int panelWidth = screen.getSidebarDetailWidth();
        int rowHeight = statuses.length <= 1 ? 150 : Math.max(112, 720 / Math.max(1, statuses.length));
        int panelHeight = 60 + rowHeight * statuses.length + 16;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(10, 12, 16, 230));
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 26, 26);
        g2.setColor(new Color(255, 255, 255, 28));
        g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 26, 26);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
        g2.setColor(Color.WHITE);
        g2.drawString(getDisplayName(), panelX + 18, panelY + 30);
        if (isExhausted()) {
            drawPill(g2, panelX + panelWidth - 132, panelY + 12, 112, 26, warningColor, "Exhausted");
        }

        int currentY = panelY + 52;
        for (int i = 0; i < statuses.length; i++) {
            PersonnelStatus status = statuses[i];
            String label = i < labels.length ? labels[i] : ("Figure " + (i + 1));
            int rowX = panelX + 12;
            int rowWidth = panelWidth - 24;
            int availableWidth = rowWidth - 24;

            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillRoundRect(rowX, currentY, rowWidth, rowHeight - 10, 20, 20);
            g2.setColor(new Color(255, 255, 255, 24));
            g2.drawRoundRect(rowX, currentY, rowWidth, rowHeight - 10, 20, 20);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
            g2.setColor(Color.WHITE);
            g2.drawString(label, rowX + 14, currentY + 24);

            int healthMax = getMaxHealth();
            String healthText = healthMax > 0 ? status.health() + "/" + healthMax : String.valueOf(status.health());
            drawMetric(g2, rowX + 14, currentY + 38, availableWidth, "Health", healthText, status.health(),
                    Math.max(1, healthMax), healthColor);

            Integer maxStrain = getMaxStrain();
            int strainBarMax = maxStrain == null ? Math.max(1, Math.max(status.strain(), 1)) : Math.max(1, maxStrain);
            String strainText = maxStrain == null ? String.valueOf(status.strain()) : status.strain() + "/"
                    + maxStrain;
            drawMetric(g2, rowX + 14, currentY + 78, availableWidth, "Strain", strainText, status.strain(),
                    strainBarMax, strainColor);

            int chipsY = currentY + rowHeight - 32;
            drawPill(g2, rowX + 14, chipsY, status.stunned() ? 108 : 96, 22,
                    status.stunned() ? warningColor : new Color(70, 76, 88), status.stunned() ? "Stunned" : "Clear");
            drawPill(g2, rowX + 128, chipsY, status.focused() ? 100 : 92, 22,
                    status.focused() ? accentColor : new Color(70, 76, 88), status.focused() ? "Focused" : "Not Focused");

            String evaluation = evaluateStatus(status, healthMax);
            int textWidth = g2.getFontMetrics().stringWidth(evaluation);
            g2.setColor(mutedTextColor);
            g2.drawString(evaluation, rowX + rowWidth - textWidth - 14, chipsY + 16);

            currentY += rowHeight;
        }

        g2.dispose();
    }

    private static void drawMetric(Graphics2D g2, int x, int y, int width, String label, String value, int current,
            int max, Color fillColor) {
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
        g2.setColor(new Color(255, 255, 255, 190));
        g2.drawString(label + ": " + value, x, y);

        int barY = y + 8;
        int barWidth = Math.max(120, width - 18);
        g2.setColor(new Color(255, 255, 255, 24));
        g2.fillRoundRect(x, barY, barWidth, 12, 12, 12);
        float ratio = Math.max(0f, Math.min(1f, current / (float) max));
        g2.setColor(fillColor);
        g2.fillRoundRect(x, barY, Math.round(barWidth * ratio), 12, 12, 12);
    }

    private static void drawPill(Graphics2D g2, int x, int y, int width, int height, Color fill, String text) {
        g2.setColor(fill);
        g2.fillRoundRect(x, y, width, height, height, height);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y, width, height, height, height);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
        FontMetrics metrics = g2.getFontMetrics();
        int textX = x + (width - metrics.stringWidth(text)) / 2;
        int textY = y + ((height - metrics.getHeight()) / 2) + metrics.getAscent() - 1;
        g2.drawString(text, textX, textY);
    }

    private static String evaluateStatus(PersonnelStatus status, int maxHealth) {
        String healthState;
        if (maxHealth > 0) {
            float ratio = status.health() / (float) maxHealth;
            if (ratio <= 0.25f) {
                healthState = "Critical";
            } else if (ratio <= 0.65f) {
                healthState = "Wounded";
            } else {
                healthState = "Healthy";
            }
        } else {
            healthState = "Operational";
        }

        if (status.stunned()) {
            healthState = healthState + ", stunned";
        }
        if (status.focused()) {
            healthState = healthState + ", focused";
        }
        return healthState;
    }

    PersonnelStatus[] getStatuses();
}
