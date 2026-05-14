package game;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import util.MyArrayList;

// Interface for things that have deployment cards and stats
interface FullDeployment {
  int DETAIL_HEADER_HEIGHT_DIVISOR = 8;
  int DETAIL_ROW_HEIGHT_DIVISOR = 4;
  int DETAIL_ROW_MIN_HEIGHT_DIVISOR = 5;
  int DETAIL_PADDING_DIVISOR = 16;
  int DETAIL_SMALL_PADDING_DIVISOR = 32;
  int DETAIL_CORNER_DIVISOR = 12;
  int DETAIL_PILL_WIDTH_DIVISOR = 3;
  int DETAIL_PILL_HEIGHT_DIVISOR = 28;
  int METRIC_BAR_HEIGHT_DIVISOR = 40;
  int CHIP_WIDTH_DIVISOR = 5;
  int TITLE_FONT_DIVISOR = 20;
  int ROW_FONT_DIVISOR = 24;
  int METRIC_FONT_DIVISOR = 32;

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

  default MyArrayList<Equipment.Item> getEquipment() {
    return new MyArrayList<>();
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
    MyArrayList<Equipment.Item> equipment = getEquipment();
    int panelX = detailBounds.x;
    int panelY = detailBounds.y;
    int panelWidth = detailBounds.width;
    int padding = getDetailPadding(panelWidth);
    int smallPadding = getDetailSmallPadding(panelWidth);
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    float titleFontSize = Math.max(1f, panelWidth / (float) TITLE_FONT_DIVISOR);
    float rowFontSize = Math.max(1f, panelWidth / (float) ROW_FONT_DIVISOR);
    float metricFontSize = Math.max(1f, panelWidth / (float) METRIC_FONT_DIVISOR);
    g2.setFont(g2.getFont().deriveFont(Font.BOLD, titleFontSize));
    int titleLineHeight = g2.getFontMetrics().getHeight();
    g2.setFont(g2.getFont().deriveFont(Font.BOLD, rowFontSize));
    int rowLineHeight = g2.getFontMetrics().getHeight();
    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, metricFontSize));
    int metricLineHeight = g2.getFontMetrics().getHeight();
    int metricBarHeight = Math.max(1, panelWidth / METRIC_BAR_HEIGHT_DIVISOR);
    int chipHeight = Math.max(1, panelWidth / DETAIL_PILL_HEIGHT_DIVISOR);
    int headerHeight = Math.max(padding * 2 + titleLineHeight, panelWidth / DETAIL_HEADER_HEIGHT_DIVISOR);
    int equipmentSectionHeight = equipment.isEmpty() ? 0
        : getEquipmentSectionHeight(panelWidth - smallPadding * 2, equipment, padding, smallPadding, metricLineHeight);
    int minimumRowHeight = padding * 3 + rowLineHeight + metricLineHeight * 2 + metricBarHeight * 2 + chipHeight
        + smallPadding * 4;
    int availableRowsHeight = Math.max(minimumRowHeight,
        detailBounds.height - headerHeight - equipmentSectionHeight - padding);
    int rowHeight = statuses.length <= 1
        ? Math.min(availableRowsHeight, Math.max(minimumRowHeight, panelWidth / DETAIL_ROW_HEIGHT_DIVISOR))
        : Math.max(minimumRowHeight, availableRowsHeight / Math.max(1, statuses.length));
    int panelHeight = headerHeight + rowHeight * statuses.length + equipmentSectionHeight + padding;
    int cornerArc = Math.max(1, panelWidth / DETAIL_CORNER_DIVISOR);

    g2.setColor(new Color(10, 12, 16, 230));
    g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, cornerArc, cornerArc);
    g2.setColor(new Color(255, 255, 255, 28));
    g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, cornerArc, cornerArc);

    g2.setFont(g2.getFont().deriveFont(Font.BOLD, titleFontSize));
    g2.setColor(Color.WHITE);
    g2.drawString(getDisplayName(), panelX + padding, panelY + padding + g2.getFontMetrics().getAscent());
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

      g2.setFont(g2.getFont().deriveFont(Font.BOLD, rowFontSize));
      g2.setColor(Color.WHITE);
      int contentY = currentY + padding;
      g2.drawString(label, rowX + padding, contentY + g2.getFontMetrics().getAscent());
      contentY += g2.getFontMetrics().getHeight() + smallPadding;

      int healthMax = getMaxHealth();
      String healthText = healthMax > 0 ? status.health() + "/" + healthMax : String.valueOf(status.health());
      contentY = drawMetric(g2, rowX + padding, contentY, availableWidth, metricFontSize, "Health", healthText,
          status.health(),
          Math.max(1, healthMax), healthColor);
      contentY += smallPadding;

      Integer maxStrain = getMaxStrain();
      int strainBarMax = maxStrain == null ? Math.max(1, Math.max(status.strain(), 1)) : Math.max(1, maxStrain);
      String strainText = maxStrain == null ? String.valueOf(status.strain())
          : status.strain() + "/"
              + maxStrain;
      contentY = drawMetric(g2, rowX + padding, contentY, availableWidth, metricFontSize, "Strain",
          strainText, status.strain(),
          strainBarMax, strainColor);
      contentY += smallPadding;

      int chipsY = Math.max(contentY, currentY + rowHeight - chipHeight - padding);
      int chipWidth = panelWidth / CHIP_WIDTH_DIVISOR;
      drawPill(g2, rowX + padding, chipsY, chipWidth, chipHeight,
          status.stunned() ? warningColor : new Color(70, 76, 88), status.stunned() ? "Stunned" : "Clear");
      drawPill(g2, rowX + padding + chipWidth + smallPadding, chipsY, chipWidth, chipHeight,
          status.focused() ? accentColor : new Color(70, 76, 88), status.focused() ? "Focused" : "Not Focused");

      String evaluation = evaluateStatus(status, healthMax);
      int textWidth = g2.getFontMetrics().stringWidth(evaluation);
      g2.setColor(mutedTextColor);
      g2.drawString(evaluation, rowX + rowWidth - textWidth - padding,
          chipsY + ((chipHeight - g2.getFontMetrics().getHeight()) / 2) + g2.getFontMetrics().getAscent());

      currentY += rowHeight;
    }

    if (!equipment.isEmpty()) {
      drawEquipmentSection(g2, panelX + smallPadding, currentY, panelWidth - smallPadding * 2, equipment,
          metricFontSize, padding, smallPadding);
    }

    g2.dispose();
  }

  private static void drawEquipmentSection(Graphics2D g2, int x, int y, int width,
      MyArrayList<Equipment.Item> equipment, float fontSize, int padding, int smallPadding) {
    int visibleItems = Math.min(4, equipment.size());
    int columns = getEquipmentColumns(width, padding, smallPadding);
    int itemWidth = getEquipmentItemWidth(width, columns, padding, smallPadding);
    int itemHeight = getEquipmentItemHeight(itemWidth);
    int rows = (visibleItems + columns - 1) / columns;
    g2.setFont(g2.getFont().deriveFont(Font.BOLD, fontSize));
    int height = getEquipmentSectionHeight(width, equipment, padding, smallPadding, g2.getFontMetrics().getHeight());
    int cornerArc = Math.max(1, width / DETAIL_CORNER_DIVISOR);
    g2.setColor(new Color(255, 255, 255, 18));
    g2.fillRoundRect(x, y, width, height, cornerArc, cornerArc);
    g2.setColor(new Color(255, 255, 255, 24));
    g2.drawRoundRect(x, y, width, height, cornerArc, cornerArc);

    g2.setColor(Color.WHITE);
    FontMetrics metrics = g2.getFontMetrics();
    int textY = y + padding + metrics.getAscent();
    g2.drawString("Equipment", x + padding, textY);
    int gridY = textY + smallPadding;
    for (int i = 0; i < visibleItems; i++) {
      Equipment.Item item = equipment.get(i);
      BufferedImage image = LoaderUtils.getImage(item.imageName());
      int column = i % columns;
      int row = i / columns;
      int itemX = x + padding + column * (itemWidth + smallPadding);
      int itemY = gridY + row * (itemHeight + smallPadding);
      drawEquipmentImage(g2, image, itemX, itemY, itemWidth, itemHeight);
    }
    if (equipment.size() > visibleItems) {
      g2.setFont(g2.getFont().deriveFont(Font.PLAIN, fontSize));
      g2.setColor(mutedTextColor);
      g2.drawString("+" + (equipment.size() - visibleItems) + " more", x + padding,
          gridY + rows * (itemHeight + smallPadding));
    }
  }

  private static void drawEquipmentImage(Graphics2D g2, BufferedImage image, int x, int y, int width, int height) {
    int imageWidth = image.getWidth(null);
    int imageHeight = image.getHeight(null);
    float scale = Math.min(width / (float) imageWidth, height / (float) imageHeight);
    int drawWidth = Math.max(1, Math.round(imageWidth * scale));
    int drawHeight = Math.max(1, Math.round(imageHeight * scale));
    int drawX = x + (width - drawWidth) / 2;
    int drawY = y + (height - drawHeight) / 2;
    g2.drawImage(image, drawX, drawY, drawX + drawWidth, drawY + drawHeight, 0, 0, imageWidth, imageHeight, null);
  }

  private static int getEquipmentSectionHeight(int width, MyArrayList<Equipment.Item> equipment, int padding,
      int smallPadding, int headerLineHeight) {
    int visibleItems = Math.min(4, equipment.size());
    int columns = getEquipmentColumns(width, padding, smallPadding);
    int rows = (visibleItems + columns - 1) / columns;
    int itemWidth = getEquipmentItemWidth(width, columns, padding, smallPadding);
    int itemHeight = getEquipmentItemHeight(itemWidth);
    int overflowHeight = equipment.size() > visibleItems ? headerLineHeight + smallPadding : 0;
    return padding * 2 + headerLineHeight + smallPadding + rows * itemHeight
        + Math.max(0, rows - 1) * smallPadding + overflowHeight;
  }

  private static int getEquipmentColumns(int width, int padding, int smallPadding) {
    return width - padding * 2 >= smallPadding * 2 + 120 ? 2 : 1;
  }

  private static int getEquipmentItemWidth(int width, int columns, int padding, int smallPadding) {
    return Math.max(1, (width - padding * 2 - smallPadding * (columns - 1)) / columns);
  }

  private static int getEquipmentItemHeight(int itemWidth) {
    return Math.max(1, Math.round(itemWidth * 1.4f));
  }

  private static int drawMetric(Graphics2D g2, int x, int y, int width, float fontSize, String label, String value,
      int current,
      int max, Color fillColor) {
    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, fontSize));
    g2.setColor(new Color(255, 255, 255, 190));
    FontMetrics metrics = g2.getFontMetrics();
    g2.drawString(label + ": " + value, x, y + metrics.getAscent());

    int barY = y + metrics.getHeight() + Math.max(1, width / 48);
    int barWidth = Math.max(1, width - Math.max(1, width / 24));
    int barHeight = Math.max(1, width / METRIC_BAR_HEIGHT_DIVISOR);
    g2.setColor(new Color(255, 255, 255, 24));
    g2.fillRoundRect(x, barY, barWidth, barHeight, barHeight, barHeight);
    float ratio = Math.max(0f, Math.min(1f, current / (float) max));
    g2.setColor(fillColor);
    g2.fillRoundRect(x, barY, Math.round(barWidth * ratio), barHeight, barHeight, barHeight);
    return barY + barHeight;
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

  private static String fitText(FontMetrics metrics, String text, int maxWidth) {
    if (metrics.stringWidth(text) <= maxWidth) {
      return text;
    }
    String suffix = "...";
    int suffixWidth = metrics.stringWidth(suffix);
    String result = "";
    for (int i = 0; i < text.length(); i++) {
      String next = result + text.charAt(i);
      if (metrics.stringWidth(next) + suffixWidth > maxWidth) {
        break;
      }
      result = next;
    }
    return result + suffix;
  }

  PersonnelStatus[] getStatuses();
}
