package visual;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import util.MyArrayList;
import util.MyHashMap;
import util.MyHashSet;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.KeyEventDispatcher;

import game.*;
import game.GameUi;
import net.structs.MissionOption;
import game.Personnel.Directions;
import net.structs.LobbySnapshot;
import net.structs.RemotePrompt;
import net.structs.RemotePrompt.PromptType;

public class Screen extends JPanel implements ActionListener, MouseListener, KeyListener, GameUi {
  private static final Runnable NO_ACTION = () -> {
  };
  private static final Consumer<MissionOption> NO_MISSION_SELECTION = mission -> {
  };

  private final boolean remoteMode;
  private final boolean readOnly;
  private final Game game;
  private boolean gameStarted = false;
  private BufferedImage startScreenimage;
  private int buttonSize;
  private Optional<CompletableFuture<String>> movementButtonOutput = Optional.empty();
  private final JButton rotateMovementButton = new JButton("Rotate");
  private final MyHashMap<JButton, String> rotationButtonTokens = new MyHashMap<>();
  private static boolean gameEnd = false;
  private Thread mainGameLoop;
  private static SelectionType currentSelectionType = SelectionType.EXPLANATION;
  private Optional<DeploymentCard> selectedDeploymentCard = Optional.empty();
  private boolean rebelsWin = true;
  private Optional<RemoteBoardPrompt> activeRemoteBoardPrompt = Optional.empty();
  private Optional<CompletableFuture<String>> activePromptResponse = Optional.empty();
  private long activePromptId = -1L;
  private Optional<PromptKind> activePromptKind = Optional.empty();
  private final MyHashSet<Long> pendingPromptCancels = new MyHashSet<>();
  private final JPanel promptPanel = new JPanel(new BorderLayout(8, 8)) {
    @Override
    protected void paintComponent(Graphics g) {
      java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
      g2.setColor(new Color(8, 8, 10, 245));
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
      g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(255, 255, 255, 24), 0, getHeight(),
          new Color(255, 255, 255, 0)));
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
      g2.setColor(new Color(120, 120, 120, 180));
      g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
      g2.dispose();
      super.paintComponent(g);
    }
  };
  private final JLabel promptTitleLabel = new JLabel();
  private final JTextArea promptMessageArea = new JTextArea();
  private final JPanel promptActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
  private final JTextField numericPromptField = new JTextField();
  private final JButton numericSubmitButton = new JButton("Submit");
  private final JButton increaseThreatButton = new JButton("+ Threat");
  private final JButton nextRoundButton = new JButton("Next Round");
  private final JButton restartGameButton = new JButton("Restart");
  private final JButton missionOneButton = new JButton(MissionOption.MISSION_ONE.displayName());
  private final JButton missionTwoButton = new JButton(MissionOption.MISSION_TWO.displayName());
  private final LayoutHandler layoutHandler = new LayoutHandler();
  private final Object deploymentInfoArea = new Object();
  private int numericPromptMinValue;
  private int numericPromptMaxValue;
  private final BannerState bannerState = new BannerState();
  private Timer bannerTimer;
  private volatile String statusText = "Turn: --";
  private Optional<LobbySnapshot> lobbySnapshot = Optional.empty();
  private Optional<MissionOption> localMissionSelection = Optional.empty();
  private Consumer<MissionOption> missionSelectionAction = NO_MISSION_SELECTION;
  private Runnable increaseThreatAction = NO_ACTION;
  private Runnable nextRoundAction = NO_ACTION;
  private Runnable finishGameAction = NO_ACTION;
  private Runnable restartGameAction = NO_ACTION;
  private Optional<game.PlayerSeat> localSeat = Optional.empty();
  private Optional<String> serverStatusText = Optional.empty();
  private Optional<Rectangle> completeGameGuideLinkBounds = Optional.empty();
  private final KeyEventDispatcher shortcutDispatcher = this::dispatchShortcutKeyEvent;

  private static final String[] START_SCREEN_INSTRUCTIONS = new String[] {
      "Welcome to the Tutorial Mission",
      "This mission introduces the main controls and campaign rules. If you are new to Imperial Assault, read pages 4-8 of the Complete Game Guide.",
      "On your turn, follow the prompts to activate a figure or group. The active figure is highlighted in green. Prompts will ask you to move, attack, interact, rest, or choose another available action.",
      "When you move, choose how many movement points to spend, then use the arrow buttons to step around the map. When you attack, valid targets are highlighted and unavailable targets are grayed out.",
      "Click a figure when no prompt is blocking the board, or while selecting a combat target, to inspect health, strain, conditions, and abilities. Crates, doors, terminals, wounded heroes, threat, and the status phase use campaign mission rules.",
      "To vote for the mission, click one of the mission names at the bottom."
  };
  private static final String COMPLETE_GAME_GUIDE_URL =
      "https://images-cdn.fantasyflightgames.com/filer_public/89/06/8906c720-5ed5-4b22-aa1b-b58b4528956c/swi01_learn_to_play_v17.pdf";

  public static BiMap<Directions, JButton> movementButtons = new BiMap<>();

  private static enum PromptKind {
    MULTIPLE_CHOICE,
    YES_NO,
    NUMERIC
  }

  private static record RemoteBoardPrompt(RemotePrompt prompt, CompletableFuture<String> selection) {
    boolean matches(long promptId) {
      return prompt.promptId() == promptId;
    }

    boolean is(PromptType type) {
      return prompt.type() == type;
    }

    void complete(String value) {
      selection.complete(value);
    }

    void cancelIfOpen() {
      if (!selection.isDone()) {
        selection.cancel(true);
      }
    }
  }

  public Screen(Game game, boolean remoteMode) {
    this(game, remoteMode, false);
  }

  public Screen(Game game, boolean remoteMode, boolean readOnly) {
    this.game = game;
    this.remoteMode = remoteMode;
    this.readOnly = readOnly;
    UiContext.setScreen(this);
    setFocusable(true);
    setLayout(null);
    promptPanel.setOpaque(false);
    updateLayoutState();
    initializeButtons();
    initializePromptPanel();
    installKeyBindings();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(shortcutDispatcher);
    if (!readOnly) {
      addMouseListener(this);
      addKeyListener(this);
    }
    setBackground(new Color(0, 0, 0));
    startScreenimage = LoaderUtils.getImage("IACoverArt");
    if (!game.getHeroes().isEmpty()) {
      selectedDeploymentCard = Optional.of(game.getHeroes().get(0).getDeploymentCard());
      selectedDeploymentCard.ifPresent(card -> card.setVisible(true));
    }
    if (remoteMode && !readOnly) {
      initializeLobbyControls();
    }
  }

  private void installKeyBindings() {
    registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0), "increaseThreat", () -> {
      performIncreaseThreat();
    });
    registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), "nextRound", () -> {
      performAdvanceStatusPhase();
    });
    registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "clearDice", () -> {
      if (!remoteMode) {
        game.clearDice();
      }
    });
    registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "endGame", () -> gameEnd = true);
    registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "resetGame", () -> {
      if (readOnly) {
        performRestartGame();
      } else if (!remoteMode) {
        reset();
      }
    });
  }

  private void registerKeyBinding(KeyStroke keyStroke, String actionName, Runnable action) {
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
    getActionMap().put(actionName, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        action.run();
        if (!readOnly) {
          repaint();
        }
      }
    });
  }

  private boolean dispatchShortcutKeyEvent(KeyEvent event) {
    if (event.getID() != KeyEvent.KEY_RELEASED) {
      return false;
    }
    return handleShortcutKeyCode(event.getKeyCode());
  }

  private boolean handleShortcutKeyCode(int keyCode) {
    if (readOnly) {
      if (keyCode == KeyEvent.VK_T) {
        performIncreaseThreat();
      } else if (keyCode == KeyEvent.VK_N) {
        performAdvanceStatusPhase();
      } else if (keyCode == KeyEvent.VK_U) {
        performFinishGame();
      } else if (keyCode == KeyEvent.VK_R) {
        performRestartGame();
      } else if (keyCode == KeyEvent.VK_ESCAPE) {
        gameEnd = true;
      } else {
        return false;
      }
      return true;
    }
    if (keyCode == KeyEvent.VK_ESCAPE) {
      gameEnd = true;
    } else if (!remoteMode && keyCode == KeyEvent.VK_T) {
      performIncreaseThreat();
    } else if (!remoteMode && keyCode == KeyEvent.VK_N) {
      performAdvanceStatusPhase();
    } else if (keyCode == KeyEvent.VK_U) {
      performFinishGame();
    } else if (!remoteMode && keyCode == KeyEvent.VK_C) {
      game.clearDice();
    } else if (!remoteMode && keyCode == KeyEvent.VK_R) {
      reset();
    } else {
      return false;
    }
    repaint();
    return true;
  }

  private void initializeLobbyControls() {
    missionOneButton.setVisible(false);
    missionOneButton.addActionListener(e -> submitMissionSelection(MissionOption.MISSION_ONE));
    add(missionOneButton);

    missionTwoButton.setVisible(false);
    missionTwoButton.addActionListener(e -> submitMissionSelection(MissionOption.MISSION_TWO));
    add(missionTwoButton);
  }

  private void initializePromptPanel() {
    promptPanel.setBackground(new Color(18, 18, 18));
    promptPanel.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 2));
    promptPanel.setVisible(false);

    promptTitleLabel.setForeground(Color.WHITE);
    promptTitleLabel.setFont(promptTitleLabel.getFont().deriveFont(Font.BOLD, 18f));

    promptMessageArea.setEditable(false);
    promptMessageArea.setLineWrap(true);
    promptMessageArea.setWrapStyleWord(true);
    promptMessageArea.setOpaque(false);
    promptMessageArea.setForeground(Color.WHITE);
    promptMessageArea.setFont(promptMessageArea.getFont().deriveFont(15f));

    JScrollPane promptMessageScrollPane = new JScrollPane(promptMessageArea);
    promptMessageScrollPane.setOpaque(false);
    promptMessageScrollPane.getViewport().setOpaque(false);
    promptMessageScrollPane.setBorder(BorderFactory.createEmptyBorder());
    promptMessageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    promptMessageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

    promptActionsPanel.setOpaque(false);

    JPanel numericPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    numericPanel.setOpaque(false);
    numericPromptField.setColumns(8);
    numericPanel.add(numericPromptField);
    numericSubmitButton.addActionListener(e -> submitNumericPrompt());
    numericPromptField.addActionListener(e -> submitNumericPrompt());
    numericPanel.add(numericSubmitButton);

    JPanel promptControlsPanel = new JPanel(new BorderLayout(0, 4));
    promptControlsPanel.setOpaque(false);
    promptControlsPanel.add(promptActionsPanel, BorderLayout.CENTER);
    promptControlsPanel.add(numericPanel, BorderLayout.SOUTH);

    promptPanel.add(promptTitleLabel, BorderLayout.NORTH);
    promptPanel.add(promptMessageScrollPane, BorderLayout.CENTER);
    promptPanel.add(promptControlsPanel, BorderLayout.SOUTH);
    add(promptPanel);
  }

  private void initializeButtons() {
    movementButtons.put(Directions.UP,
        new JButton(new ImageIcon(LoaderUtils.getImageIcon("ArrowButtonUp").getImage()
            .getScaledInstance(buttonSize, buttonSize, Image.SCALE_SMOOTH))));
    movementButtons.put(Directions.UPLEFT,
        new JButton(new ImageIcon(LoaderUtils.getImageIcon("ArrowButtonUpLeft").getImage()
            .getScaledInstance(buttonSize, buttonSize, Image.SCALE_SMOOTH))));
    movementButtons.put(Directions.LEFT,
        new JButton(new ImageIcon(LoaderUtils.getImageIcon("ArrowButtonLeft").getImage()
            .getScaledInstance(buttonSize, buttonSize, Image.SCALE_SMOOTH))));
    movementButtons.put(Directions.DOWNLEFT,
        new JButton(new ImageIcon(LoaderUtils.getImageIcon("ArrowButtonDownLeft").getImage()
            .getScaledInstance(buttonSize, buttonSize, Image.SCALE_SMOOTH))));
    movementButtons.put(Directions.DOWN,
        new JButton(new ImageIcon(LoaderUtils.getImageIcon("ArrowButtonDown").getImage()
            .getScaledInstance(buttonSize, buttonSize, Image.SCALE_SMOOTH))));
    movementButtons.put(Directions.DOWNRIGHT,
        new JButton(new ImageIcon(LoaderUtils.getImageIcon("ArrowButtonDownRight").getImage()
            .getScaledInstance(buttonSize, buttonSize, Image.SCALE_SMOOTH))));
    movementButtons.put(Directions.RIGHT,
        new JButton(new ImageIcon(LoaderUtils.getImageIcon("ArrowButtonRight").getImage()
            .getScaledInstance(buttonSize, buttonSize, Image.SCALE_SMOOTH))));
    movementButtons.put(Directions.UPRIGHT,
        new JButton(new ImageIcon(LoaderUtils.getImageIcon("ArrowButtonUpRight").getImage()
            .getScaledInstance(buttonSize, buttonSize, Image.SCALE_SMOOTH))));
    for (JButton button : movementButtons.map.values()) {
      add(button);
      button.addActionListener(this);
      button.setVisible(false);
      button.setBounds(0, 0, buttonSize, buttonSize);
    }
    rotateMovementButton.addActionListener(this);
    rotateMovementButton.setVisible(false);
    rotateMovementButton.setBounds(0, 0, buttonSize + 32, buttonSize);
    add(rotateMovementButton);
    increaseThreatButton.addActionListener(e -> performIncreaseThreat());
    increaseThreatButton.setVisible(false);
    add(increaseThreatButton);
    nextRoundButton.addActionListener(e -> performAdvanceStatusPhase());
    nextRoundButton.setVisible(false);
    add(nextRoundButton);
    restartGameButton.addActionListener(e -> performRestartGame());
    restartGameButton.setVisible(false);
    add(restartGameButton);
    System.out.println("INITIALIZED BUTTONS");
  }

  private CompletableFuture<String> beginPrompt(long promptId, PromptKind kind, String name, String explanation) {
    CompletableFuture<String> future = new CompletableFuture<>();
    Runnable setup = () -> {
      activePromptId = promptId;
      activePromptKind = Optional.of(kind);
      activePromptResponse = Optional.of(future);
      promptTitleLabel.setText(name);
      promptMessageArea.setText(explanation);
      promptActionsPanel.removeAll();
      numericPromptField.setText("");
      numericPromptField.setVisible(kind == PromptKind.NUMERIC);
      numericSubmitButton.setVisible(kind == PromptKind.NUMERIC);
      promptPanel.setVisible(true);
      revalidate();
      repaint();
      if (kind == PromptKind.NUMERIC) {
        numericPromptField.requestFocusInWindow();
      }
      if (pendingPromptCancels.remove(promptId)) {
        future.cancel(true);
      }
    };
    future.whenComplete((value, error) -> SwingUtilities.invokeLater(() -> {
      if (activePromptResponse.filter(active -> active == future).isPresent()) {
        clearPromptPanel();
      }
    }));
    if (SwingUtilities.isEventDispatchThread()) {
      setup.run();
    } else {
      SwingUtilities.invokeLater(setup);
    }
    return future;
  }

  private void clearPromptPanel() {
    activePromptId = -1L;
    activePromptKind = Optional.empty();
    activePromptResponse = Optional.empty();
    promptActionsPanel.removeAll();
    numericPromptField.setText("");
    promptPanel.setVisible(false);
    revalidate();
    repaint();
  }

  private void completePrompt(String value) {
    activePromptResponse
        .filter(response -> !response.isDone())
        .ifPresent(response -> response.complete(value));
  }

  private void submitNumericPrompt() {
    if (!activePromptKind.equals(Optional.of(PromptKind.NUMERIC))) {
      return;
    }
    String trimmed = numericPromptField.getText().trim();
    try {
      int parsed = Integer.parseInt(trimmed);
      if (parsed < numericPromptMinValue || parsed > numericPromptMaxValue) {
        promptMessageArea.setText("Enter a value from " + numericPromptMinValue + " to "
            + numericPromptMaxValue + ".");
        numericPromptField.requestFocusInWindow();
        return;
      }
      completePrompt(trimmed);
    } catch (NumberFormatException ex) {
      promptMessageArea.setText("Enter a valid number from " + numericPromptMinValue + " to "
          + numericPromptMaxValue + ".");
      numericPromptField.requestFocusInWindow();
    }
  }

  private void addPromptButton(String label, String value) {
    JButton button = new JButton(label);
    button.addActionListener(e -> completePrompt(value));
    promptActionsPanel.add(button);
  }

  private void submitMissionSelection(MissionOption mission) {
    localMissionSelection = Optional.of(mission);
    missionSelectionAction.accept(mission);
    refreshLobbyControls();
  }

  private void refreshLobbyControls() {
    if (!remoteMode || readOnly) {
      missionOneButton.setVisible(false);
      missionTwoButton.setVisible(false);
      return;
    }
    boolean showControls = lobbySnapshot
        .filter(snapshot -> !gameStarted && !snapshot.allMissionsMatch())
        .isPresent();
    missionOneButton.setVisible(showControls);
    missionTwoButton.setVisible(showControls);
    missionOneButton.setEnabled(showControls);
    missionTwoButton.setEnabled(showControls);
    if (localMissionSelection.equals(Optional.of(MissionOption.MISSION_ONE))) {
      missionOneButton.setText(selectedMissionLabel(MissionOption.MISSION_ONE));
      missionTwoButton.setText(MissionOption.MISSION_TWO.displayName());
    } else if (localMissionSelection.equals(Optional.of(MissionOption.MISSION_TWO))) {
      missionOneButton.setText(MissionOption.MISSION_ONE.displayName());
      missionTwoButton.setText(selectedMissionLabel(MissionOption.MISSION_TWO));
    } else {
      missionOneButton.setText(MissionOption.MISSION_ONE.displayName());
      missionTwoButton.setText(MissionOption.MISSION_TWO.displayName());
    }
  }

  private String selectedMissionLabel(MissionOption mission) {
    return mission.displayName() + " selected";
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(layoutHandler.getPreferredScreenWidth(), layoutHandler.getPreferredScreenHeight());
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    updateLayoutState();
    positionOverlayComponents();
    layoutSelectedDeploymentCard();
    if (gameEnd) {
      g.setColor(new Color(0, 0, 0));
      Rectangle startBounds = layoutHandler.getStartScreenBounds();
      g.drawRect(startBounds.x, startBounds.y, startBounds.width, startBounds.height);
      g.setColor(new Color(255, 255, 255));
      Rectangle textBounds = layoutHandler.getGameOverTextBounds();
      g.drawString("Game over, " + (rebelsWin ? "rebels " : "imperials") + " won", textBounds.x,
          textBounds.y + textBounds.height);
    } else if (gameStarted) {
      g.setColor(new Color(25, 25, 25));
      Rectangle backdropBounds = layoutHandler.getBoardBackdropBounds();
      g.fillRect(backdropBounds.x, backdropBounds.y, backdropBounds.width, backdropBounds.height);
      game.drawGame(g);
      drawTurnHud(g);
    } else {
      Rectangle startBounds = layoutHandler.getStartScreenBounds();
      g.drawImage(startScreenimage, startBounds.x, startBounds.y, startBounds.x + startBounds.width,
          startBounds.y + startBounds.height, 0, 0,
          startScreenimage.getWidth(null), startScreenimage.getHeight(null), null);
      drawStartScreenInstructions(g);
      drawLobbyOverlay(g);
    }
    drawServerStatus(g);
  }

  private void drawStartScreenInstructions(Graphics g) {
    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
    Composite original = g2.getComposite();
    int padding = layoutHandler.getInlinePadding();
    int smallPadding = layoutHandler.getSmallPadding();
    int panelWidth = Math.min(getScreenWidth() - padding * 2, Math.max(360, getScreenWidth() / 4));
    int panelX = padding;
    int panelY = padding;
    int panelHeight = Math.min(getScreenHeight() - padding * 2, Math.max(420, getScreenHeight() - padding * 2));

    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
    g2.setColor(new Color(10, 10, 10));
    g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, padding, padding);
    g2.setComposite(original);
    g2.setColor(Color.WHITE);
    g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, padding, padding);

    int textX = panelX + padding;
    int textY = panelY + padding;
    int textWidth = panelWidth - padding * 2;
    Font baseFont = g2.getFont();
    g2.setFont(baseFont.deriveFont(Font.BOLD, 28f));
    textY = drawWrappedText(g2, START_SCREEN_INSTRUCTIONS[0], textX, textY, textWidth, panelY + panelHeight - padding);
    textY += smallPadding;

    g2.setFont(baseFont.deriveFont(Font.PLAIN, 18f));
    for (int i = 1; i < START_SCREEN_INSTRUCTIONS.length; i++) {
      textY = drawWrappedText(g2, START_SCREEN_INSTRUCTIONS[i], textX, textY, textWidth,
          panelY + panelHeight - padding);
      textY += smallPadding;
    }
    g2.setColor(new Color(95, 185, 255));
    int guideLinkY = textY;
    int guideLinkWidth = g2.getFontMetrics().stringWidth("Complete Game Guide");
    textY = drawWrappedText(g2, "Complete Game Guide", textX, textY, textWidth, panelY + panelHeight - padding);
    g2.drawLine(textX, guideLinkY + g2.getFontMetrics().getAscent() + 2,
        textX + Math.min(textWidth, guideLinkWidth), guideLinkY + g2.getFontMetrics().getAscent() + 2);
    completeGameGuideLinkBounds = Optional.of(new Rectangle(textX, guideLinkY,
        Math.min(textWidth, guideLinkWidth), Math.max(1, textY - guideLinkY)));
    g2.setColor(Color.WHITE);
    if (!remoteMode) {
      textY += smallPadding;
      g2.setFont(baseFont.deriveFont(Font.BOLD, 18f));
      drawWrappedText(g2, "Click anywhere to start.", textX, textY, textWidth, panelY + panelHeight - padding);
    }
  }

  private int drawWrappedText(java.awt.Graphics2D g2, String text, int x, int y, int width, int maxY) {
    FontMetrics metrics = g2.getFontMetrics();
    int lineHeight = metrics.getHeight();
    int currentY = y + metrics.getAscent();
    StringBuilder line = new StringBuilder();
    for (String word : text.split(" ")) {
      String candidate = line.length() == 0 ? word : line + " " + word;
      if (metrics.stringWidth(candidate) <= width) {
        line = new StringBuilder(candidate);
        continue;
      }
      if (currentY > maxY) {
        return currentY;
      }
      if (line.length() > 0) {
        g2.drawString(line.toString(), x, currentY);
        currentY += lineHeight;
      }
      line = new StringBuilder(word);
    }
    if (line.length() > 0 && currentY <= maxY) {
      g2.drawString(line.toString(), x, currentY);
      currentY += lineHeight;
    }
    return currentY;
  }

  private void drawLobbyOverlay(Graphics g) {
    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
    Composite original = g2.getComposite();
    Rectangle panelBounds = layoutHandler.getLobbyPanelBounds();
    int x = panelBounds.x;
    int y = panelBounds.y;
    int panelWidth = panelBounds.width;
    int panelHeight = panelBounds.height;
    int padding = layoutHandler.getInlinePadding();
    int smallPadding = layoutHandler.getSmallPadding();
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
    g2.setColor(new Color(10, 10, 10));
    g2.fillRoundRect(x, y, panelWidth, panelHeight, padding, padding);
    g2.setComposite(original);
    g2.setColor(Color.WHITE);
    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
    g2.drawString("Lobby", x + padding, y + padding + g2.getFontMetrics().getAscent());

    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20f));
    Optional<LobbySnapshot> visibleLobby = lobbySnapshot;
    String header;
    if (visibleLobby.isEmpty()) {
      header = "Waiting for players to connect";
    } else if (!visibleLobby.get().allSeatsFilled()) {
      header = "Waiting for all seats to fill";
    } else if (visibleLobby.get().allMissionsMatch()) {
      header = "All players chose " + formatMission(Optional.ofNullable(visibleLobby.get().selectedMission()));
    } else if (visibleLobby.get().allMissionsSelected()) {
      header = "All seats filled. Click a mission name at the bottom to vote";
    } else {
      header = "All seats filled. Vote by clicking a mission name at the bottom";
    }
    int rowStep = Math.max(g2.getFontMetrics().getHeight() + smallPadding, panelHeight / 10);
    int textY = y + padding + g2.getFontMetrics().getHeight() * 3;
    g2.drawString(header, x + padding, textY);

    if (visibleLobby.isEmpty()) {
      g2.drawString("Connecting...", x + padding, textY + rowStep);
      return;
    }

    LobbySnapshot snapshot = visibleLobby.get();
    int rowY = textY + rowStep;
    for (game.PlayerSeat seat : snapshot.config().requiredSeats()) {
      boolean occupied = snapshot.occupiedSeats().contains(seat);
      String label = formatSeat(seat);
      Optional<MissionOption> mission = Optional.ofNullable(snapshot.missionSelections().get(seat));
      String state = occupied ? formatMission(mission) : "open";
      g2.drawString(label, x + padding, rowY);
      int stateWidth = g2.getFontMetrics().stringWidth(state);
      g2.drawString(state, x + panelWidth - stateWidth - padding, rowY);
      rowY += rowStep;
    }

    if (serverStatusText.isPresent()) {
      g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
      g2.drawString(serverStatusText.get(), x + padding, y + panelHeight - padding);
    }
  }

  private void drawServerStatus(Graphics g) {
    Optional<String> visibleStatus = serverStatusText.filter(status -> !status.isBlank());
    if (visibleStatus.isEmpty()) {
      return;
    }
    String status = visibleStatus.get();
    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
    int paddingX = layoutHandler.getInlinePadding();
    int paddingY = layoutHandler.getSmallPadding();
    int textWidth = g2.getFontMetrics().stringWidth(status);
    Rectangle statusBounds = layoutHandler.getServerStatusBounds(textWidth);
    Composite original = g2.getComposite();
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
    g2.setColor(new Color(10, 10, 10));
    g2.fillRoundRect(statusBounds.x, statusBounds.y, statusBounds.width, statusBounds.height, paddingX, paddingX);
    g2.setComposite(original);
    g2.setColor(new Color(255, 255, 255));
    g2.drawRoundRect(statusBounds.x, statusBounds.y, statusBounds.width, statusBounds.height, paddingX, paddingX);
    g2.drawString(status, statusBounds.x + paddingX,
        statusBounds.y + paddingY + g2.getFontMetrics().getAscent());
  }

  private void drawTurnHud(Graphics g) {
    g.setFont(g.getFont().deriveFont(Font.BOLD, 22f));
    Rectangle hudBounds = layoutHandler.getTurnHudBounds();
    int padding = layoutHandler.getInlinePadding();
    g.setColor(new Color(0, 0, 0, 180));
    g.fillRoundRect(hudBounds.x, hudBounds.y, hudBounds.width, hudBounds.height, padding, padding);
    g.setColor(Color.WHITE);
    g.drawRoundRect(hudBounds.x, hudBounds.y, hudBounds.width, hudBounds.height, padding, padding);
    g.drawString(statusText, hudBounds.x + padding, hudBounds.y + padding + g.getFontMetrics().getAscent());

    Optional<String> bannerText = bannerState.text();
    long remaining = bannerState.remainingMs();
    if (bannerText.isEmpty() || remaining <= 0) {
      return;
    }
    float alpha = Math.max(0f, Math.min(1f, remaining / 1400f));
    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
    Composite original = g2.getComposite();
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    Rectangle bannerBounds = layoutHandler.getBannerBounds();
    g2.setColor(new Color(15, 15, 15));
    g2.fillRoundRect(bannerBounds.x, bannerBounds.y, bannerBounds.width, bannerBounds.height, padding, padding);
    g2.setColor(new Color(255, 255, 255));
    g2.drawRoundRect(bannerBounds.x, bannerBounds.y, bannerBounds.width, bannerBounds.height, padding, padding);
    g2.drawString(bannerText.get(), bannerBounds.x + padding,
        bannerBounds.y + padding + g2.getFontMetrics().getAscent());
    g2.setComposite(original);
  }

  private String formatSeat(game.PlayerSeat seat) {
    return switch (seat) {
      case IMPERIAL -> "Imperial";
      case REBEL_1 -> "Rebel 1";
      case REBEL_2 -> "Rebel 2";
      case REBEL_3 -> "Rebel 3";
      case REBEL_4 -> "Rebel 4";
    };
  }

  private String formatMission(Optional<MissionOption> mission) {
    return mission.map(MissionOption::displayName).orElse("a mission");
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (readOnly) {
      return;
    }
    Object source = e.getSource();
    for (Directions direction : Directions.values()) {
      if (source.equals(movementButtons.get(direction))) {
        if (!completeRemoteDirectionPrompt(direction.name())) {
          movementButtonOutput.ifPresent(output -> output.complete(direction.name()));
        }
        continue;
      }
    }
    if (rotationButtonTokens.containsKey(source)) {
      if (!completeRemoteDirectionPrompt(rotationButtonTokens.get(source))) {
        movementButtonOutput.ifPresent(output -> output.complete(rotationButtonTokens.get(source)));
      }
    }
  }

  private boolean completeRemoteDirectionPrompt(String value) {
    Optional<RemoteBoardPrompt> directionPrompt = activeRemoteBoardPrompt
        .filter(prompt -> remoteMode && prompt.is(PromptType.DIRECTION));
    directionPrompt.ifPresent(prompt -> prompt.complete(value));
    return directionPrompt.isPresent();
  }

  @Override
  public void mousePressed(MouseEvent e) {
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (readOnly) {
      return;
    }
    if (!gameStarted && completeGameGuideLinkBounds.filter(bounds -> bounds.contains(e.getPoint())).isPresent()) {
      openCompleteGameGuide();
      return;
    }
    if (!gameStarted && !remoteMode) {
      gameStarted = true;
      repaint();
      mainGameLoop = new Thread(() -> game.playRound());
      mainGameLoop.start();
      return;
    }
    if (remoteMode && activeRemoteBoardPrompt.filter(prompt -> prompt.is(PromptType.TARGET)).isPresent()) {
      RemoteBoardPrompt boardPrompt = activeRemoteBoardPrompt.orElseThrow();
      Optional<Personnel> personnel = game
          .getPersonnelAtPos(new Pos(e.getX() / Constants.tileSize, e.getY() / Constants.tileSize));
      Optional<String> selectedId = personnel
          .map(Personnel::getId)
          .filter(boardPrompt.prompt().allowedValues()::contains);
      if (selectedId.isPresent()) {
        boardPrompt.complete(selectedId.orElseThrow());
        return;
      }
    }
    switch (currentSelectionType) {
      case COMBAT:
      case SPECIAL:
        Optional<Personnel> personnel = game
            .getPersonnelAtPos(new Pos(e.getX() / Constants.tileSize, e.getY() / Constants.tileSize));
        if (!remoteMode && personnel.filter(game::trySetTarget).isPresent()) {
          currentSelectionType = SelectionType.EXPLANATION;
        }
        break;
      case EXPLANATION:
        selectedDeploymentCard.ifPresent(card -> card.setVisible(false));
        Optional<DeploymentCard> newDeploymentCard = game
            .getDeploymentCard(new Pos(e.getX() / Constants.tileSize, e.getY() / Constants.tileSize));
        selectedDeploymentCard = newDeploymentCard;
        selectedDeploymentCard.ifPresent(card -> card.setVisible(true));
    }
    repaint();
  }

  private void openCompleteGameGuide() {
    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      JOptionPane.showMessageDialog(this, "Could not open link: " + COMPLETE_GAME_GUIDE_URL, "Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    try {
      Desktop.getDesktop().browse(URI.create(COMPLETE_GAME_GUIDE_URL));
    } catch (IOException | IllegalArgumentException ex) {
      JOptionPane.showMessageDialog(this, "Could not open link: " + COMPLETE_GAME_GUIDE_URL, "Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  @Override
  public void keyPressed(KeyEvent e) {
  }

  @Override
  public void keyReleased(KeyEvent e) {
    handleShortcutKeyCode(e.getKeyCode());
  }

  @Override
  public void keyTyped(KeyEvent e) {
  }

  public JButton moveAndActivateButton(Directions direction, int x, int y, double angleRads) {
    int xDiff = (int) (Constants.tileSize * Math.cos(angleRads));
    int yDiff = -1 * (int) (Constants.tileSize * Math.sin(angleRads));
    movementButtons.get(direction).setBounds((int) ((x + 0.25) * Constants.tileSize) + xDiff,
        (int) ((y + 0.25) * Constants.tileSize) + yDiff, buttonSize, buttonSize);
    movementButtons.get(direction).setEnabled(true);
    movementButtons.get(direction).setVisible(true);
    revalidate();
    repaint();
    return movementButtons.get(direction);
  }

  public JButton moveAndActivateRotateButton(int x, int y) {
    return moveAndActivateRotateButton("ROTATE", x, y);
  }

  public JButton moveAndActivateRotateButton(RotationMove rotationMove) {
    return moveAndActivateRotateButton(rotationMove.token(), rotationMove.anchor().getX(), rotationMove.anchor().getY());
  }

  private JButton moveAndActivateRotateButton(String token, int x, int y) {
    JButton button = rotationButtonTokens.isEmpty() ? rotateMovementButton : new JButton("Rotate");
    if (button != rotateMovementButton) {
      button.addActionListener(this);
      add(button);
    }
    int offset = rotationButtonTokens.size() * 4;
    rotationButtonTokens.put(button, token);
    button.setBounds((int) ((x + 0.15) * Constants.tileSize) + offset,
        (int) ((y + 0.15) * Constants.tileSize) + offset, buttonSize + 32, buttonSize);
    button.setEnabled(true);
    button.setVisible(true);
    revalidate();
    repaint();
    return button;
  }

  public void setMovementButtonOutput(CompletableFuture<String> output) {
    movementButtonOutput = Optional.of(output);
  }

  public int promptMultipleChoice(String name, String explanation, Object[] options) {
    return promptMultipleChoice(-1L, name, explanation, options);
  }

  public int promptMultipleChoice(long promptId, String name, String explanation, Object[] options) {
    CompletableFuture<String> future = beginPrompt(promptId, PromptKind.MULTIPLE_CHOICE, name, explanation);
    game.setActivePromptCancelAction(() -> future.cancel(true));
    try {
      if (!future.isCancelled()) {
        SwingUtilities.invokeLater(() -> {
          promptActionsPanel.removeAll();
          for (int i = 0; i < options.length; i++) {
            addPromptButton(String.valueOf(options[i]), String.valueOf(i));
          }
          promptPanel.revalidate();
          promptPanel.repaint();
        });
      }
      return Integer.parseInt(future.join());
    } finally {
      game.clearActivePromptCancelAction();
    }
  }

  public boolean promptYesNo(String name, String explanation) {
    return promptYesNo(-1L, name, explanation);
  }

  public boolean promptYesNo(long promptId, String name, String explanation) {
    CompletableFuture<String> future = beginPrompt(promptId, PromptKind.YES_NO, name, explanation);
    game.setActivePromptCancelAction(() -> future.cancel(true));
    try {
      if (!future.isCancelled()) {
        SwingUtilities.invokeLater(() -> {
          promptActionsPanel.removeAll();
          addPromptButton("No", String.valueOf(false));
          addPromptButton("Yes", String.valueOf(true));
          promptPanel.revalidate();
          promptPanel.repaint();
        });
      }
      return Boolean.parseBoolean(future.join());
    } finally {
      game.clearActivePromptCancelAction();
    }
  }

  public int promptNumericChoice(String name, String explanation, int minValue, int maxValue) {
    return promptNumericChoice(-1L, name, explanation, minValue, maxValue);
  }

  public int promptNumericChoice(long promptId, String name, String explanation, int minValue, int maxValue) {
    numericPromptMinValue = minValue;
    numericPromptMaxValue = maxValue;
    CompletableFuture<String> future = beginPrompt(promptId, PromptKind.NUMERIC, name,
        explanation + " (" + minValue + " to " + maxValue + ")");
    game.setActivePromptCancelAction(() -> future.cancel(true));
    try {
      if (!future.isCancelled()) {
        SwingUtilities.invokeLater(() -> {
          numericPromptField.setToolTipText("Enter a value from " + minValue + " to " + maxValue);
        });
      }
      return Integer.parseInt(future.join());
    } finally {
      game.clearActivePromptCancelAction();
    }
  }

  public void deactivateMovementButton(Directions dir) {
    movementButtons.get(dir).setVisible(false);
    movementButtons.get(dir).setEnabled(false);
  }

  public void deactivateRotateButton() {
    for (JButton button : rotationButtonTokens.keySet()) {
      button.setVisible(false);
      button.setEnabled(false);
      if (button != rotateMovementButton) {
        remove(button);
      }
    }
    rotationButtonTokens.clear();
    rotateMovementButton.setVisible(false);
    rotateMovementButton.setEnabled(false);
  }

  public void deactiveateMovementButtons() {
    for (Directions direction : Directions.values()) {
      deactivateMovementButton(direction);
    }
    deactivateRotateButton();
    repaint();
  }

  public void reset() {
    gameEnd = false;
    deactiveateMovementButtons();
    game.reset();
    if (mainGameLoop != null) {
      mainGameLoop.run();
    }
    repaint();
  }

  public void setSelectionType(SelectionType value) {
    currentSelectionType = value;
    UiContext.setSelectionType(value);
  }

  public static SelectionType getSelectionType() {
    return currentSelectionType;
  }

  public void endGame(boolean rebelsWin) {
    this.rebelsWin = rebelsWin;
    deactiveateMovementButtons();
    gameEnd = true;
    LoaderUtils.playSound("Applause");
  }

  @Override
  public void resumeGame() {
    gameEnd = false;
    repaint();
  }

  public CompletableFuture<String> beginRemoteBoardPrompt(RemotePrompt prompt) {
    if (readOnly) {
      return CompletableFuture.completedFuture(null);
    }
    activePromptId = prompt.promptId();
    CompletableFuture<String> selection = new CompletableFuture<>();
    RemoteBoardPrompt boardPrompt = new RemoteBoardPrompt(prompt, selection);
    activeRemoteBoardPrompt = Optional.of(boardPrompt);
    selection.whenComplete((value, error) -> SwingUtilities.invokeLater(() -> {
      if (activeRemoteBoardPrompt.filter(active -> active.matches(prompt.promptId())).isPresent()) {
        clearRemotePrompt();
      }
    }));
    if (pendingPromptCancels.remove(prompt.promptId())) {
      selection.cancel(true);
      return selection;
    }
    if (prompt.selectionType() != null) {
      setSelectionType(prompt.selectionType());
    }
    if (prompt.type() == PromptType.DIRECTION) {
      showRemoteDirectionPrompt(prompt);
    }
    repaint();
    return selection;
  }

  private void showRemoteDirectionPrompt(RemotePrompt prompt) {
    Optional<Personnel> selectedFigure = game.getPersonnelById(prompt.subjectId());
    if (selectedFigure.isEmpty()) {
      return;
    }
    Personnel activeFigure = selectedFigure.orElseThrow();
    deactivateRotateButton();
    double[] angleRads = { Math.PI / 4.0 };
    MyArrayList<String> allowedValues = prompt.allowedValues();
    for (Directions direction : Directions.values()) {
      angleRads[0] += Math.PI / 4;
      if (allowedValues.contains(direction.name())) {
        moveAndActivateButton(direction, activeFigure.getPos().getX(), activeFigure.getPos().getY(),
            angleRads[0]);
      } else {
        deactivateMovementButton(direction);
      }
    }
    if (allowedValues.contains("ROTATE")) {
      moveAndActivateRotateButton(activeFigure.getPos().getX(), activeFigure.getPos().getY());
    }
    for (String allowedValue : allowedValues) {
      if (RotationMove.isToken(allowedValue)) {
        moveAndActivateRotateButton(RotationMove.fromToken(allowedValue));
      }
    }
  }

  public void clearRemotePrompt() {
    activeRemoteBoardPrompt = Optional.empty();
    activePromptId = -1L;
    deactiveateMovementButtons();
    setSelectionType(SelectionType.EXPLANATION);
    repaint();
  }

  public void cancelPrompt(long promptId) {
    if (activePromptId != promptId) {
      pendingPromptCancels.add(promptId);
      return;
    }
    activePromptResponse
        .filter(response -> !response.isDone())
        .ifPresent(response -> response.cancel(true));
    activeRemoteBoardPrompt.ifPresent(RemoteBoardPrompt::cancelIfOpen);
    clearRemotePrompt();
  }

  @Override
  public void resetTransientTurnState() {
    Runnable reset = () -> {
      activePromptResponse
          .filter(response -> !response.isDone())
          .ifPresent(response -> response.cancel(true));
      activeRemoteBoardPrompt.ifPresent(RemoteBoardPrompt::cancelIfOpen);
      activeRemoteBoardPrompt = Optional.empty();
      activePromptId = -1L;
      deactiveateMovementButtons();
      setSelectionType(SelectionType.EXPLANATION);
      repaint();
    };
    if (SwingUtilities.isEventDispatchThread()) {
      reset.run();
    } else {
      SwingUtilities.invokeLater(reset);
    }
  }

  public void setTurnStatus(game.PlayerSeat seat) {
    String newStatus = "Turn: " + formatSeat(seat);
    if (!newStatus.equals(statusText)) {
      statusText = newStatus;
      repaint();
      return;
    }
    statusText = newStatus;
  }

  public void setServerStatusText(String serverStatusText) {
    this.serverStatusText = Optional.ofNullable(serverStatusText);
    repaint();
  }

  public void showBanner(String text) {
    showBanner(text, 1400L);
  }

  public void showBanner(String text, long durationMs) {
    runOnUiThread(() -> {
      long token = bannerState.show(text, durationMs);
      repaint();
      startBannerTimer(token);
    });
  }

  public void showBannerFromSnapshot(String text, long remainingMs) {
    Optional.ofNullable(text)
        .filter(value -> remainingMs > 0)
        .ifPresent(value -> showBanner(value, remainingMs));
  }

  public void updateLobbySnapshot(LobbySnapshot lobbySnapshot) {
    this.lobbySnapshot = Optional.ofNullable(lobbySnapshot);
    updateLocalMissionSelection();
    refreshLobbyControls();
    repaint();
  }

  public void setMissionSelectionAction(Consumer<MissionOption> missionSelectionAction) {
    this.missionSelectionAction = Objects.requireNonNullElse(missionSelectionAction, NO_MISSION_SELECTION);
  }

  public void setLocalSeat(game.PlayerSeat localSeat) {
    this.localSeat = Optional.ofNullable(localSeat);
    updateLocalMissionSelection();
    refreshLobbyControls();
  }

  private void updateLocalMissionSelection() {
    localMissionSelection = lobbySnapshot.flatMap(snapshot -> localSeat
        .flatMap(seat -> Optional.ofNullable(snapshot.missionSelections().get(seat))));
  }

  public void markGameStarted() {
    gameStarted = true;
    missionOneButton.setVisible(false);
    missionTwoButton.setVisible(false);
    repaint();
  }

  public boolean isGameStarted() {
    return gameStarted;
  }

  public Game getGame() {
    return game;
  }

  private void updateLayoutState() {
    layoutHandler.setSidebarState(getScreenWidth(), getScreenHeight(), game.getMapDrawWidth(), promptPanel.isVisible());
    increaseThreatButton.setVisible(shouldShowThreatButton());
    nextRoundButton.setVisible(shouldShowNextRoundButton());
    restartGameButton.setVisible(shouldShowRestartButton());
    layoutHandler.addVisualComponent(increaseThreatButton, LayoutHandler.Priority.HIGH,
        layoutHandler.getSidebarButtonWidth(), layoutHandler.getSidebarButtonHeight());
    layoutHandler.addVisualComponent(nextRoundButton, LayoutHandler.Priority.HIGH,
        layoutHandler.getSidebarButtonWidth(), layoutHandler.getSidebarButtonHeight());
    layoutHandler.addVisualComponent(restartGameButton, LayoutHandler.Priority.HIGH,
        layoutHandler.getSidebarButtonWidth(), layoutHandler.getSidebarButtonHeight());
    buttonSize = layoutHandler.getMovementButtonSize();
  }

  private void positionOverlayComponents() {
    promptPanel.setBounds(layoutHandler.getPromptPanelBounds());
    missionOneButton.setBounds(layoutHandler.getMissionOneButtonBounds());
    missionTwoButton.setBounds(layoutHandler.getMissionTwoButtonBounds());
  }

  private void layoutSelectedDeploymentCard() {
    if (selectedDeploymentCard.isEmpty()) {
      return;
    }
    DeploymentCard card = selectedDeploymentCard.orElseThrow();
    Rectangle cardBounds = layoutHandler.getDeploymentCardBounds(card.getBaseImageWidth(),
        card.getBaseImageHeight());
    card.setLayoutBounds(cardBounds, layoutHandler.getSidebarDetailBounds(cardBounds));
  }

  public int getSidebarDiceX() {
    updateLayoutState();
    return layoutHandler.getSidebarDiceX();
  }

  public int getDiceStartY() {
    updateLayoutState();
    return layoutHandler.getDiceStartY();
  }

  public int getSidebarDetailWidth() {
    updateLayoutState();
    return layoutHandler.getSidebarDetailWidth();
  }

  private int getScreenWidth() {
    int currentWidth = getWidth();
    return currentWidth > 0 ? currentWidth : Math.max(LayoutHandler.DEFAULT_SCREEN_WIDTH, game.getMapDrawWidth() * 2);
  }

  private int getScreenHeight() {
    int currentHeight = getHeight();
    return currentHeight > 0 ? currentHeight : Math.max(LayoutHandler.DEFAULT_SCREEN_HEIGHT, game.getMapDrawWidth());
  }

  private boolean shouldShowThreatButton() {
    return readOnly && gameStarted && !gameEnd;
  }

  private boolean shouldShowNextRoundButton() {
    return shouldShowThreatButton();
  }

  private boolean shouldShowRestartButton() {
    return readOnly && gameStarted;
  }

  private void performIncreaseThreat() {
    if (readOnly) {
      increaseThreatAction.run();
      return;
    }
    if (!remoteMode) {
      game.increaseThreat();
      repaint();
    }
  }

  private void performAdvanceStatusPhase() {
    if (readOnly) {
      nextRoundAction.run();
      return;
    }
    if (!remoteMode) {
      game.requestAdvanceStatusPhase();
      repaint();
    }
  }

  private void performFinishGame() {
    if (readOnly) {
      finishGameAction.run();
      return;
    }
    if (remoteMode) {
      finishGameAction.run();
      return;
    }
    if (!remoteMode) {
      game.skipToEndScreen();
      repaint();
    }
  }

  private void performRestartGame() {
    if (readOnly) {
      gameEnd = false;
      restartGameAction.run();
      repaint();
      return;
    }
    if (!remoteMode) {
      reset();
    }
  }

  public void setIncreaseThreatAction(Runnable increaseThreatAction) {
    this.increaseThreatAction = Objects.requireNonNullElse(increaseThreatAction, NO_ACTION);
  }

  public void setNextRoundAction(Runnable nextRoundAction) {
    this.nextRoundAction = Objects.requireNonNullElse(nextRoundAction, NO_ACTION);
  }

  public void setFinishGameAction(Runnable finishGameAction) {
    this.finishGameAction = Objects.requireNonNullElse(finishGameAction, NO_ACTION);
  }

  public void setRestartGameAction(Runnable restartGameAction) {
    this.restartGameAction = Objects.requireNonNullElse(restartGameAction, NO_ACTION);
  }

  private void startBannerTimer(long token) {
    if (bannerTimer != null) {
      bannerTimer.stop();
    }
    bannerTimer = new Timer(1000, e -> {
      if (!bannerState.matches(token)) {
        ((Timer) e.getSource()).stop();
        return;
      }
      if (bannerState.expired()) {
        bannerState.clear();
        repaint();
        ((Timer) e.getSource()).stop();
        return;
      }
      repaint();
    });
    bannerTimer.setRepeats(true);
    bannerTimer.start();
  }

  private void runOnUiThread(Runnable action) {
    if (SwingUtilities.isEventDispatchThread()) {
      action.run();
    } else {
      SwingUtilities.invokeLater(action);
    }
  }

  private static final class BannerState {
    private long token;
    private Optional<String> text = Optional.empty();
    private long expiresAt;

    long show(String text, long durationMs) {
      token++;
      this.text = Optional.of(text);
      expiresAt = System.currentTimeMillis() + Math.max(1L, durationMs);
      return token;
    }

    Optional<String> text() {
      return text;
    }

    long remainingMs() {
      return expiresAt - System.currentTimeMillis();
    }

    boolean matches(long token) {
      return this.token == token;
    }

    boolean expired() {
      return remainingMs() <= 0;
    }

    void clear() {
      text = Optional.empty();
      expiresAt = 0L;
    }
  }
}
