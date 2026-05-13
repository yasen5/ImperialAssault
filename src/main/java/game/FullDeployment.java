package game;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

// Interface for things that have deployment cards and stats
interface FullDeployment {
  int DETAIL_HEADER_HEIGHT_DIVISOR = 8;
  int DETAIL_ROW_HEIGHT_DIVISOR = 4;
  int DETAIL_ROW_MIN_HEIGHT_DIVISOR = 5;
  int DETAIL_PADDING_DIVISOR = 16;
  int DETAIL_SMALL_PADDING_DIVISOR = 32;
  int DETAIL_CORNER_DIVISOR = 12;
  int DETAIL_PILL_WIDTH_DIVISOR = 3;
  int DETAIL_PILL_HEIGHT_DIVISOR = 14;
  int METRIC_BAR_HEIGHT_DIVISOR = 40;
  int TEXT_ROW_STEP_DIVISOR = 18;

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
    Rectangle detailBounds = getDeploymentCard().getDetailBounds();
    if (detailBounds.width <= 0) {
      return;
    }

    PersonnelStatus[] statuses = getStatuses();
    String[] labels = getStatusLabels();
    int panelX = detailBounds.x;
    int panelY = detailBounds.y;
    int panelWidth = detailBounds.width;
    int padding = getDetailPadding(panelWidth);
    int smallPadding = getDetailSmallPadding(panelWidth);
    int headerHeight = Math.max(padding * 2, panelWidth / DETAIL_HEADER_HEIGHT_DIVISOR);
    int minimumRowHeight = Math.max(padding * 2, panelWidth / DETAIL_ROW_MIN_HEIGHT_DIVISOR);
    int rowHeight = statuses.length <= 1 ? Math.max(minimumRowHeight, panelWidth / DETAIL_ROW_HEIGHT_DIVISOR)
        : Math.max(minimumRowHeight, detailBounds.height / Math.max(1, statuses.length));
    int panelHeight = headerHeight + rowHeight * statuses.length + padding;
    int cornerArc = Math.max(1, panelWidth / DETAIL_CORNER_DIVISOR);

    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g2.setColor(new Color(10, 12, 16, 230));
    g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, cornerArc, cornerArc);
    g2.setColor(new Color(255, 255, 255, 28));
    g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, cornerArc, cornerArc);

    g2.setFont(g2.getFont().deriveFont(Font.BOLD, Math.max(1f, panelWidth / 16f)));
    g2.setColor(Color.WHITE);
    g2.drawString(getDisplayName(), panelX + padding, panelY + padding * 2);
    if (isExhausted()) {
      int pillWidth = panelWidth / DETAIL_PILL_WIDTH_DIVISOR;
      int pillHeight = Math.max(1, panelWidth / DETAIL_PILL_HEIGHT_DIVISOR);
      drawPill(g2, panelX + panelWidth - pillWidth - padding, panelY + smallPadding, pillWidth, pillHeight,
          warningColor, "Exhausted");
    }

    int currentY = panelY + headerHeight;
    for (int i = 0; i < statuses.length; i++) {
      PersonnelStatus status = statuses[i];
      String label = i < labels.length ? labels[i] : ("Figure " + (i + 1));
      int rowX = panelX + smallPadding;
      int rowWidth = panelWidth - smallPadding * 2;
      int availableWidth = rowWidth - padding * 2;
      int rowArc = Math.max(1, rowWidth / DETAIL_CORNER_DIVISOR);

      g2.setColor(new Color(255, 255, 255, 18));
      g2.fillRoundRect(rowX, currentY, rowWidth, rowHeight - smallPadding, rowArc, rowArc);
      g2.setColor(new Color(255, 255, 255, 24));
      g2.drawRoundRect(rowX, currentY, rowWidth, rowHeight - smallPadding, rowArc, rowArc);

      g2.setFont(g2.getFont().deriveFont(Font.BOLD, Math.max(1f, panelWidth / 20f)));
      g2.setColor(Color.WHITE);
      g2.drawString(label, rowX + padding, currentY + padding * 2);

      int healthMax = getMaxHealth();
      String healthText = healthMax > 0 ? status.health() + "/" + healthMax : String.valueOf(status.health());
      int textStep = Math.max(1, panelWidth / TEXT_ROW_STEP_DIVISOR);
      drawMetric(g2, rowX + padding, currentY + textStep * 2, availableWidth, "Health", healthText, status.health(),
          Math.max(1, healthMax), healthColor);

      Integer maxStrain = getMaxStrain();
      int strainBarMax = maxStrain == null ? Math.max(1, Math.max(status.strain(), 1)) : Math.max(1, maxStrain);
      String strainText = maxStrain == null ? String.valueOf(status.strain())
          : status.strain() + "/"
              + maxStrain;
      drawMetric(g2, rowX + padding, currentY + textStep * 4, availableWidth, "Strain", strainText, status.strain(),
          strainBarMax, strainColor);

      int chipsY = currentY + rowHeight - padding * 2;
      int chipHeight = Math.max(1, panelWidth / DETAIL_PILL_HEIGHT_DIVISOR);
      int chipWidth = panelWidth / 5;
      drawPill(g2, rowX + padding, chipsY, chipWidth, chipHeight,
          status.stunned() ? warningColor : new Color(70, 76, 88), status.stunned() ? "Stunned" : "Clear");
      drawPill(g2, rowX + padding + chipWidth + smallPadding, chipsY, chipWidth, chipHeight,
          status.focused() ? accentColor : new Color(70, 76, 88), status.focused() ? "Focused" : "Not Focused");

      String evaluation = evaluateStatus(status, healthMax);
      int textWidth = g2.getFontMetrics().stringWidth(evaluation);
      g2.setColor(mutedTextColor);
      g2.drawString(evaluation, rowX + rowWidth - textWidth - padding, chipsY + chipHeight - smallPadding);

      currentY += rowHeight;
    }

    g2.dispose();
  }

  private static void drawMetric(Graphics2D g2, int x, int y, int width, String label, String value, int current,
      int max, Color fillColor) {
    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
    g2.setColor(new Color(255, 255, 255, 190));
    g2.drawString(label + ": " + value, x, y);

    int barY = y + Math.max(1, width / 48);
    int barWidth = Math.max(1, width - Math.max(1, width / 24));
    int barHeight = Math.max(1, width / METRIC_BAR_HEIGHT_DIVISOR);
    g2.setColor(new Color(255, 255, 255, 24));
    g2.fillRoundRect(x, barY, barWidth, barHeight, barHeight, barHeight);
    float ratio = Math.max(0f, Math.min(1f, current / (float) max));
    g2.setColor(fillColor);
    g2.fillRoundRect(x, barY, Math.round(barWidth * ratio), barHeight, barHeight, barHeight);
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

  private static int getDetailPadding(int panelWidth) {
    return Math.max(1, panelWidth / DETAIL_PADDING_DIVISOR);
  }

  private static int getDetailSmallPadding(int panelWidth) {
    return Math.max(1, panelWidth / DETAIL_SMALL_PADDING_DIVISOR);
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
