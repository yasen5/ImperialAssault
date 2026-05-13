package visual;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;

public class LayoutHandler {
  public static final int DEFAULT_SCREEN_WIDTH = 1920;
  public static final int DEFAULT_SCREEN_HEIGHT = 1080;

  private static final int SIDEBAR_GAP_DIVISOR = 48;
  private static final int CARD_WIDTH_DIVISOR = 2;
  private static final int MAX_CARD_WIDTH_NUMERATOR = 4;
  private static final int MAX_CARD_WIDTH_DENOMINATOR = 5;
  private static final int MIN_DETAIL_WIDTH_DIVISOR = 4;
  private static final int PROMPT_HEIGHT_DIVISOR = 4;
  private static final int THREAT_BUTTON_WIDTH_DIVISOR = 8;
  private static final int THREAT_BUTTON_HEIGHT_DIVISOR = 24;
  private static final int BUTTON_HEIGHT_DIVISOR = 25;
  private static final int BUTTON_MIN_WIDTH_DIVISOR = 7;
  private static final int BUTTON_MAX_WIDTH_DIVISOR = 5;
  private static final int DICE_START_Y_DIVISOR = 16;
  private static final int LOBBY_PANEL_WIDTH_DIVISOR = 2;
  private static final int LOBBY_PANEL_HEIGHT_DIVISOR = 5;
  private static final int SERVER_STATUS_MIN_WIDTH_DIVISOR = 6;
  private static final int HUD_WIDTH_DIVISOR = 7;
  private static final int HUD_HEIGHT_DIVISOR = 20;
  private static final int BANNER_WIDTH_DIVISOR = 3;
  private static final int START_SCREEN_WIDTH_MULTIPLIER = 2;
  private static final int START_SCREEN_HEIGHT_MULTIPLIER = 1;
  private static final int MOVEMENT_BUTTON_SIZE_DIVISOR = 48;

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  }
  private record VisualComponent(Priority priority, Component component, int width, int height) {
  }

  private ArrayList<VisualComponent> visualComponents = new ArrayList<>();
  private int x;
  private int y;
  private int width;
  private int screenWidth = DEFAULT_SCREEN_WIDTH;
  private int screenHeight = DEFAULT_SCREEN_HEIGHT;
  private int mapWidth = 0;
  private boolean promptVisible;
  private boolean reserveThreatButtonSpace;

  public void addVisualComponent(Component comp, Priority priority, int width, int height) {
    if (comp == null || visualComponents.contains(comp)) {
      return;
    }
    visualComponents.add(new VisualComponent(priority, comp, width, height));
    sortByPriority();
    arrangeLayout();
  }

  public void setLayoutArea(int x, int y, int width) {
    this.x = x;
    this.y = y;
    this.width = Math.max(0, width);
    arrangeLayout();
  }

  public void setSidebarState(int screenWidth, int screenHeight, int mapWidth, boolean promptVisible,
      boolean reserveThreatButtonSpace) {
    this.screenWidth = Math.max(0, screenWidth);
    this.screenHeight = Math.max(0, screenHeight);
    this.mapWidth = Math.max(0, mapWidth);
    this.promptVisible = promptVisible;
    this.reserveThreatButtonSpace = reserveThreatButtonSpace;
    setLayoutArea(getSidebarLeft(), getSidebarContentTop(), getSidebarWidth());
  }

  public void arrangeLayout() {
    sortByPriority();
    int nextY = y;
    for (VisualComponent comp : visualComponents) {
      if (!comp.component.isVisible()) {
        continue;
      }
      int componentWidth = Math.min(comp.width, width);
      comp.component.setBounds(x, nextY, componentWidth, comp.height);
      nextY += comp.height + getSidebarGap();
    }
  }

  private void sortByPriority() {
    visualComponents.sort(Comparator.comparingInt(comp -> {
      return comp.priority.ordinal();
    }));
  }

  public Rectangle getDeploymentCardBounds(int baseWidth, int baseHeight) {
    int cardWidth = Math.max(0, Math.min(baseWidth, getSidebarCardWidth()));
    if (cardWidth <= 0 || baseWidth <= 0) {
      return new Rectangle(getSidebarLeft(), getSidebarContentTop(), 0, 0);
    }
    float scale = cardWidth / (float) baseWidth;
    int cardHeight = Math.round(baseHeight * scale);
    return new Rectangle(getSidebarLeft(), getSidebarContentTop(), cardWidth, cardHeight);
  }

  public Rectangle getSidebarDetailBounds(Rectangle cardBounds) {
    int panelY = cardBounds == null ? getSidebarContentTop() : cardBounds.y;
    int panelBottom = Math.max(panelY, getDiceStartY() - getSidebarGap());
    return new Rectangle(getSidebarDetailX(), panelY, getSidebarDetailWidth(), panelBottom - panelY);
  }

  public Rectangle getPromptPanelBounds() {
    return new Rectangle(getSidebarLeft(), getPromptPanelTop(), getSidebarWidth(), getPromptPanelHeight());
  }

  public Rectangle getThreatButtonBounds() {
    return new Rectangle(getSidebarLeft() + getSidebarGap(), getThreatButtonTop(), getThreatButtonWidth(),
        getThreatButtonHeight());
  }

  public Rectangle getMissionOneButtonBounds() {
    int buttonWidth = getLobbyButtonWidth();
    return new Rectangle(getSidebarLeft(), getLobbyButtonTop(), buttonWidth, getButtonHeight());
  }

  public Rectangle getMissionTwoButtonBounds() {
    int buttonWidth = getLobbyButtonWidth();
    return new Rectangle(getSidebarLeft() + buttonWidth + getSidebarGap(), getLobbyButtonTop(), buttonWidth,
        getButtonHeight());
  }

  public Rectangle getLobbyPanelBounds() {
    int panelWidth = Math.min(getSidebarWidth(), Math.max(getMapScaleWidth(LOBBY_PANEL_WIDTH_DIVISOR),
        screenWidth - (getSidebarGap() * 2)));
    int panelHeight = Math.max(getMapScaleWidth(LOBBY_PANEL_HEIGHT_DIVISOR), getButtonHeight() * 5);
    int panelX = Math.max(getSidebarGap(), (screenWidth - panelWidth) / 2);
    int panelY = Math.max(getSidebarGap(), (screenHeight - panelHeight) / 6);
    return new Rectangle(panelX, panelY, panelWidth, panelHeight);
  }

  public Rectangle getServerStatusBounds(int textWidth) {
    int paddingX = getInlinePadding();
    int width = Math.max(getMapScaleWidth(SERVER_STATUS_MIN_WIDTH_DIVISOR), textWidth + paddingX * 2);
    int height = getButtonHeight();
    return new Rectangle(screenWidth - width - getSidebarGap(), getSidebarGap(), width, height);
  }

  public Rectangle getTurnHudBounds() {
    return new Rectangle(getSidebarGap(), getSidebarGap(), getMapScaleWidth(HUD_WIDTH_DIVISOR),
        getMapScaleWidth(HUD_HEIGHT_DIVISOR));
  }

  public Rectangle getBannerBounds() {
    int width = getMapScaleWidth(BANNER_WIDTH_DIVISOR);
    return new Rectangle((screenWidth - width) / 2, getSidebarGap(), width, getMapScaleWidth(HUD_HEIGHT_DIVISOR));
  }

  public Rectangle getGameOverTextBounds() {
    return new Rectangle(getSidebarGap(), getSidebarGap(), screenWidth - getSidebarGap() * 2, getButtonHeight());
  }

  public Rectangle getBoardBackdropBounds() {
    int panelWidth = Math.max(getSidebarGap(), getSidebarWidth());
    return new Rectangle(getSidebarLeft(), 0, panelWidth, screenHeight);
  }

  public Rectangle getStartScreenBounds() {
    return new Rectangle(0, 0, screenWidth, screenHeight);
  }

  public int getPreferredScreenWidth() {
    return Math.max(DEFAULT_SCREEN_WIDTH, mapWidth * START_SCREEN_WIDTH_MULTIPLIER);
  }

  public int getPreferredScreenHeight() {
    return Math.max(DEFAULT_SCREEN_HEIGHT, mapWidth * START_SCREEN_HEIGHT_MULTIPLIER);
  }

  public int getInlinePadding() {
    return Math.max(1, getSidebarGap());
  }

  public int getSmallPadding() {
    return Math.max(1, getSidebarGap() / 2);
  }

  public int getMovementButtonSize() {
    return getMapScaleWidth(MOVEMENT_BUTTON_SIZE_DIVISOR);
  }

  public int getSidebarLeft() {
    return mapWidth;
  }

  public int getSidebarWidth() {
    return Math.max(0, screenWidth - getSidebarLeft());
  }

  public int getSidebarCardWidth() {
    return Math.max(0,
        Math.min(getMapScaleWidth(MAX_CARD_WIDTH_NUMERATOR, MAX_CARD_WIDTH_DENOMINATOR),
            getSidebarWidth() / CARD_WIDTH_DIVISOR));
  }

  public int getSidebarDetailX() {
    return getSidebarLeft() + getSidebarCardWidth() + getSidebarGap();
  }

  public int getSidebarDetailWidth() {
    return Math.max(getMapScaleWidth(MIN_DETAIL_WIDTH_DIVISOR), getSidebarWidth() - getSidebarCardWidth()
        - getSidebarGap());
  }

  public int getSidebarDiceX() {
    return getSidebarDetailX();
  }

  public int getDiceStartY() {
    return Math.max(getSidebarContentTop(), screenHeight - getMapScaleWidth(DICE_START_Y_DIVISOR) * 6);
  }

  public int getPromptPanelTop() {
    return getSidebarGap();
  }

  public int getSidebarContentTop() {
    int top = promptVisible ? getPromptPanelTop() + getPromptPanelHeight() + getSidebarGap() : getSidebarGap();
    if (reserveThreatButtonSpace) {
      return top + getThreatButtonHeight() + getSidebarGap();
    }
    return top;
  }

  private int getThreatButtonTop() {
    return promptVisible ? getPromptPanelTop() + getPromptPanelHeight() + getSidebarGap() : getSidebarGap();
  }

  private int getPromptPanelHeight() {
    return getMapScaleWidth(PROMPT_HEIGHT_DIVISOR);
  }

  private int getThreatButtonWidth() {
    return getMapScaleWidth(THREAT_BUTTON_WIDTH_DIVISOR);
  }

  private int getThreatButtonHeight() {
    return getMapScaleWidth(THREAT_BUTTON_HEIGHT_DIVISOR);
  }

  private int getButtonHeight() {
    return getMapScaleWidth(BUTTON_HEIGHT_DIVISOR);
  }

  private int getLobbyButtonTop() {
    return screenHeight - getButtonHeight() - getSidebarGap();
  }

  private int getLobbyButtonWidth() {
    return Math.min(getMapScaleWidth(BUTTON_MAX_WIDTH_DIVISOR),
        Math.max(getMapScaleWidth(BUTTON_MIN_WIDTH_DIVISOR), (getSidebarWidth() - getSidebarGap()) / 2));
  }

  private int getSidebarGap() {
    return getMapScaleWidth(SIDEBAR_GAP_DIVISOR);
  }

  private int getMapScaleWidth(int divisor) {
    int basis = mapWidth > 0 ? mapWidth : screenWidth;
    return Math.max(1, basis / divisor);
  }

  private int getMapScaleWidth(int numerator, int denominator) {
    int basis = mapWidth > 0 ? mapWidth : screenWidth;
    return Math.max(1, basis * numerator / denominator);
  }
}
