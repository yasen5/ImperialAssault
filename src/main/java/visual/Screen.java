package visual;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import util.MyArrayList;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
  private final boolean remoteMode;
  private final boolean readOnly;
  private final Game game;
  private boolean gameStarted = false;
  private BufferedImage startScreenimage;
  private int buttonSize;
  private CompletableFuture<Directions> movementButtonOutput;
  private static boolean gameEnd = false;
  private Thread mainGameLoop;
  private static SelectionType currentSelectionType = SelectionType.EXPLANATION;
  private static DeploymentCard previousSelectedCard;
  private JEditorPane editorPane;
  private boolean rebelsWin = true;
  private CompletableFuture<String> remoteBoardSelection;
  private RemotePrompt activeRemotePrompt;
  private CompletableFuture<String> activePromptResponse;
  private long activePromptId = -1L;
  private PromptKind activePromptKind;
  private final Set<Long> pendingPromptCancels = ConcurrentHashMap.newKeySet();
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
  private final JButton finishGameButton = new JButton("Finish Game");
  private final JButton missionOneButton = new JButton("Mission 1");
  private final JButton missionTwoButton = new JButton("Mission 2");
  private final LayoutHandler layoutHandler = new LayoutHandler();
  private final Object deploymentInfoArea = new Object();
  private int numericPromptMinValue;
  private int numericPromptMaxValue;
  private final AtomicLong bannerToken = new AtomicLong(0L);
  private Timer bannerTimer;
  private volatile String statusText = "Turn: --";
  private volatile String bannerText;
  private volatile long bannerExpiresAt;
  private volatile LobbySnapshot lobbySnapshot;
  private MissionOption localMissionSelection;
  private Consumer<MissionOption> missionSelectionAction = mission -> {
  };
  private Runnable increaseThreatAction = () -> {
  };
  private Runnable nextRoundAction = () -> {
  };
  private Runnable finishGameAction = () -> {
  };
  private game.PlayerSeat localSeat;
  private volatile String serverStatusText;
  private final KeyEventDispatcher shortcutDispatcher = this::dispatchShortcutKeyEvent;

  private static String[] dialogChain = new String[] {
      "<html><body style='width: 300px; padding: 10px;'>" +
          "<h3>Welcome to Imperial Assault!</h3>" +
          "<p>To get started, read through pages 4-8 of this guide (NOTE: Some aspects of the game have been changed):</p>"
          +
          "<p><a href='https://images-cdn.fantasyflightgames.com/filer_public/89/06/8906c720-5ed5-4b22-aa1b-b58b4528956c/swi01_learn_to_play_v17.pdf'>Complete Game Guide</a></p>"
          +
          "<p>Select any text above to copy it, or click the links to open them in your browser.</p>" +
          "</body></html>",
      "<html><body style='width: 300px; padding: 10px;'>" +
          "<h3>Mission Rules:</h3>" +
          "<p>This implementation uses campaign mission rules for activations, threat, status phase, wounded heroes, figure blocking, crates, and common conditions. Some campaign progression features are still outside the mission board.</p>"
          +
          "</body></html>",
      "<html><body style='width: 300px; padding: 10px;'>" +
          "<h3>Instructions!</h3>" +
          "<p>You will first choose the figure(s) to move. Once you do, the current figure will be lit in green. To move, you will use the arrow buttons after you specify the number of moves you want to make.\nActions will be represented by dialogues, which you can respond to. When you attack, valid targets will be lit up and others will be grayed out.</p>"
          +
          "</body></html>",
      "<html><body style='width: 300px; padding: 10px;'>" +
          "<h3>Instructions!</h3>" +
          "<p>To view the abilities of an individual character, click on them while there isn't an option pane or you are selecting a target for combat (sorry it doesn't work otherwise). You will see their health, strain, and some conditions (although these unfortunately get covered up by option panes periodically)</p>"
          +
          "</body></html>"
  };

  public static BiMap<Directions, JButton> movementButtons = new BiMap<>();

  private static enum PromptKind {
    MULTIPLE_CHOICE,
    YES_NO,
    NUMERIC
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
    if (!remoteMode && !readOnly) {
      showInstructionsChain();
    }
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
      previousSelectedCard = game.getHeroes().get(0).getDeploymentCard();
      previousSelectedCard.setVisible(true);
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
      if (!remoteMode) {
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
      } else if (keyCode == KeyEvent.VK_F) {
        performFinishGame();
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

    JPanel promptTextPanel = new JPanel();
    promptTextPanel.setOpaque(false);
    promptTextPanel.setLayout(new BoxLayout(promptTextPanel, BoxLayout.Y_AXIS));
    promptTextPanel.add(promptTitleLabel);
    promptTextPanel.add(promptMessageArea);

    promptActionsPanel.setOpaque(false);

    JPanel numericPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    numericPanel.setOpaque(false);
    numericPromptField.setColumns(8);
    numericPanel.add(numericPromptField);
    numericSubmitButton.addActionListener(e -> submitNumericPrompt());
    numericPromptField.addActionListener(e -> submitNumericPrompt());
    numericPanel.add(numericSubmitButton);

    promptPanel.add(promptTextPanel, BorderLayout.NORTH);
    promptPanel.add(promptActionsPanel, BorderLayout.CENTER);
    promptPanel.add(numericPanel, BorderLayout.SOUTH);
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
    increaseThreatButton.addActionListener(e -> performIncreaseThreat());
    increaseThreatButton.setVisible(false);
    add(increaseThreatButton);
    nextRoundButton.addActionListener(e -> performAdvanceStatusPhase());
    nextRoundButton.setVisible(false);
    add(nextRoundButton);
    finishGameButton.addActionListener(e -> performFinishGame());
    finishGameButton.setVisible(false);
    add(finishGameButton);
    System.out.println("INITIALIZED BUTTONS");
  }

  public void showInstructionsChain() {
    for (int i = 0; i < dialogChain.length; i++) {
      editorPane = new JEditorPane("text/html", dialogChain[i]);
      editorPane.setEditable(false);
      editorPane.setOpaque(false);
      if (i == 0) {
        editorPane.addHyperlinkListener(e -> {
          if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            try {
              Desktop.getDesktop().browse(e.getURL().toURI());
            } catch (Exception ex) {
              JOptionPane.showMessageDialog(null, "Could not open link: " + e.getURL(), "Error",
                  JOptionPane.ERROR_MESSAGE);
            }
          }
        });
      }
      JOptionPane.showConfirmDialog(null, editorPane, "Instructions", JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  private CompletableFuture<String> beginPrompt(long promptId, PromptKind kind, String name, String explanation) {
    CompletableFuture<String> future = new CompletableFuture<>();
    Runnable setup = () -> {
      activePromptId = promptId;
      activePromptKind = kind;
      activePromptResponse = future;
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
      if (activePromptResponse == future) {
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
    activePromptKind = null;
    activePromptResponse = null;
    promptActionsPanel.removeAll();
    numericPromptField.setText("");
    promptPanel.setVisible(false);
    revalidate();
    repaint();
  }

  private void completePrompt(String value) {
    if (activePromptResponse != null && !activePromptResponse.isDone()) {
      activePromptResponse.complete(value);
    }
  }

  private void submitNumericPrompt() {
    if (activePromptKind != PromptKind.NUMERIC) {
      return;
    }
    String rawValue = numericPromptField.getText();
    if (rawValue == null) {
      return;
    }
    String trimmed = rawValue.trim();
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
    localMissionSelection = mission;
    missionSelectionAction.accept(mission);
    refreshLobbyControls();
  }

  private void refreshLobbyControls() {
    if (!remoteMode || readOnly) {
      missionOneButton.setVisible(false);
      missionTwoButton.setVisible(false);
      return;
    }
    boolean showControls = lobbySnapshot != null && !gameStarted && !lobbySnapshot.allMissionsMatch();
    missionOneButton.setVisible(showControls);
    missionTwoButton.setVisible(showControls);
    missionOneButton.setEnabled(showControls);
    missionTwoButton.setEnabled(showControls);
    if (localMissionSelection == MissionOption.MISSION_ONE) {
      missionOneButton.setText("Mission 1 selected");
      missionTwoButton.setText("Mission 2");
    } else if (localMissionSelection == MissionOption.MISSION_TWO) {
      missionOneButton.setText("Mission 1");
      missionTwoButton.setText("Mission 2 selected");
    } else {
      missionOneButton.setText("Mission 1");
      missionTwoButton.setText("Mission 2");
    }
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
      drawLobbyOverlay(g);
    }
    drawServerStatus(g);
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
    String header;
    if (lobbySnapshot == null) {
      header = "Waiting for players to connect";
    } else if (!lobbySnapshot.allSeatsFilled()) {
      header = "Waiting for all seats to fill";
    } else if (lobbySnapshot.allMissionsMatch()) {
      header = "All players chose " + formatMission(lobbySnapshot.selectedMission());
    } else if (lobbySnapshot.allMissionsSelected()) {
      header = "All seats filled. Pick the same mission to begin";
    } else {
      header = "All seats filled. Choose a mission";
    }
    int rowStep = Math.max(g2.getFontMetrics().getHeight() + smallPadding, panelHeight / 10);
    int textY = y + padding + g2.getFontMetrics().getHeight() * 3;
    g2.drawString(header, x + padding, textY);

    if (lobbySnapshot == null) {
      g2.drawString("Connecting...", x + padding, textY + rowStep);
      return;
    }

    int rowY = textY + rowStep;
    for (game.PlayerSeat seat : lobbySnapshot.config().requiredSeats()) {
      boolean occupied = lobbySnapshot.occupiedSeats().contains(seat);
      String label = formatSeat(seat);
      MissionOption mission = lobbySnapshot.missionSelections().get(seat);
      String state = occupied ? (mission == null ? "joined" : formatMission(mission)) : "open";
      g2.drawString(label, x + padding, rowY);
      int stateWidth = g2.getFontMetrics().stringWidth(state);
      g2.drawString(state, x + panelWidth - stateWidth - padding, rowY);
      rowY += rowStep;
    }

    if (serverStatusText != null) {
      g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
      g2.drawString(serverStatusText, x + padding, y + panelHeight - padding);
    }
  }

  private void drawServerStatus(Graphics g) {
    if (serverStatusText == null || serverStatusText.isBlank()) {
      return;
    }
    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
    int paddingX = layoutHandler.getInlinePadding();
    int paddingY = layoutHandler.getSmallPadding();
    int textWidth = g2.getFontMetrics().stringWidth(serverStatusText);
    Rectangle statusBounds = layoutHandler.getServerStatusBounds(textWidth);
    Composite original = g2.getComposite();
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
    g2.setColor(new Color(10, 10, 10));
    g2.fillRoundRect(statusBounds.x, statusBounds.y, statusBounds.width, statusBounds.height, paddingX, paddingX);
    g2.setComposite(original);
    g2.setColor(new Color(255, 255, 255));
    g2.drawRoundRect(statusBounds.x, statusBounds.y, statusBounds.width, statusBounds.height, paddingX, paddingX);
    g2.drawString(serverStatusText, statusBounds.x + paddingX,
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

    long remaining = bannerExpiresAt - System.currentTimeMillis();
    if (bannerText == null || remaining <= 0) {
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
    g2.drawString(bannerText, bannerBounds.x + padding,
        bannerBounds.y + padding + g2.getFontMetrics().getAscent());
    g2.setComposite(original);
  }

  private String formatSeat(game.PlayerSeat seat) {
    return switch (seat) {
      case IMPERIAL -> "Imperial";
      case REBEL_1 -> "Rebel 1";
      case REBEL_2 -> "Rebel 2";
    };
  }

  private String formatMission(MissionOption mission) {
    return mission == null ? "a mission" : mission.displayName();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (readOnly) {
      return;
    }
    Object source = e.getSource();
    for (Directions direction : Directions.values()) {
      if (source.equals(movementButtons.get(direction))) {
        if (remoteMode && remoteBoardSelection != null && activeRemotePrompt != null
            && activeRemotePrompt.type() == PromptType.DIRECTION) {
          remoteBoardSelection.complete(direction.name());
        } else if (movementButtonOutput != null) {
          movementButtonOutput.complete(direction);
        }
        continue;
      }
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (readOnly) {
      return;
    }
    if (!gameStarted && !remoteMode) {
      gameStarted = true;
      repaint();
      mainGameLoop = new Thread(() -> game.playRound());
      mainGameLoop.start();
      return;
    }
    if (remoteMode && activeRemotePrompt != null && remoteBoardSelection != null) {
      if (activeRemotePrompt.type() == PromptType.TARGET) {
        Personnel personnel = game
            .getPersonnelAtPos(new Pos(e.getX() / Constants.tileSize, e.getY() / Constants.tileSize));
        if (personnel != null && activeRemotePrompt.allowedValues().contains(personnel.getId())) {
          remoteBoardSelection.complete(personnel.getId());
          return;
        }
      }
    }
    switch (currentSelectionType) {
      case COMBAT:
      case SPECIAL:
        Personnel personnel = game
            .getPersonnelAtPos(new Pos(e.getX() / Constants.tileSize, e.getY() / Constants.tileSize));
        if (!remoteMode && personnel != null && game.trySetTarget(personnel)) {
          currentSelectionType = SelectionType.EXPLANATION;
        }
        break;
      case EXPLANATION:
        if (previousSelectedCard != null) {
          previousSelectedCard.setVisible(false);
        }
        DeploymentCard newDeploymentCard = game
            .getDeploymentCard(new Pos(e.getX() / Constants.tileSize, e.getY() / Constants.tileSize));
        if (newDeploymentCard != null) {
          previousSelectedCard = newDeploymentCard;
        }
        if (previousSelectedCard != null) {
          previousSelectedCard.setVisible(true);
        }
    }
    repaint();
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

  public void setMovementButtonOutput(CompletableFuture<Directions> output) {
    movementButtonOutput = output;
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

  public void deactiveateMovementButtons() {
    for (Directions direction : Directions.values()) {
      deactivateMovementButton(direction);
    }
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

  public CompletableFuture<String> beginRemoteBoardPrompt(RemotePrompt prompt) {
    if (readOnly) {
      return CompletableFuture.completedFuture(null);
    }
    activeRemotePrompt = prompt;
    activePromptId = prompt.promptId();
    remoteBoardSelection = new CompletableFuture<>();
    remoteBoardSelection.whenComplete((value, error) -> SwingUtilities.invokeLater(() -> {
      if (activeRemotePrompt != null && activeRemotePrompt.promptId() == prompt.promptId()) {
        clearRemotePrompt();
      }
    }));
    if (pendingPromptCancels.remove(prompt.promptId())) {
      remoteBoardSelection.cancel(true);
      return remoteBoardSelection;
    }
    if (prompt.selectionType() != null) {
      setSelectionType(prompt.selectionType());
    }
    if (prompt.type() == PromptType.DIRECTION) {
      showRemoteDirectionPrompt(prompt);
    }
    repaint();
    return remoteBoardSelection;
  }

  private void showRemoteDirectionPrompt(RemotePrompt prompt) {
    Personnel activeFigure = game.getPersonnelById(prompt.subjectId());
    if (activeFigure == null) {
      return;
    }
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
  }

  public void clearRemotePrompt() {
    activeRemotePrompt = null;
    remoteBoardSelection = null;
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
    if (activePromptResponse != null && !activePromptResponse.isDone()) {
      activePromptResponse.cancel(true);
    }
    if (remoteBoardSelection != null && !remoteBoardSelection.isDone()) {
      remoteBoardSelection.cancel(true);
    }
    clearRemotePrompt();
  }

  @Override
  public void resetTransientTurnState() {
    Runnable reset = () -> {
      if (activePromptResponse != null && !activePromptResponse.isDone()) {
        activePromptResponse.cancel(true);
      }
      if (remoteBoardSelection != null && !remoteBoardSelection.isDone()) {
        remoteBoardSelection.cancel(true);
      }
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
    this.serverStatusText = serverStatusText;
    repaint();
  }

  public void showBanner(String text) {
    showBanner(text, 1400L);
  }

  public void showBanner(String text, long durationMs) {
    long token = bannerToken.incrementAndGet();
    bannerText = text;
    bannerExpiresAt = System.currentTimeMillis() + Math.max(1L, durationMs);
    repaint();
    SwingUtilities.invokeLater(() -> startBannerTimer(token));
  }

  public void showBannerFromSnapshot(String text, long remainingMs) {
    if (text == null || remainingMs <= 0) {
      return;
    }
    showBanner(text, remainingMs);
  }

  public void updateLobbySnapshot(LobbySnapshot lobbySnapshot) {
    this.lobbySnapshot = lobbySnapshot;
    if (localSeat != null && lobbySnapshot != null) {
      localMissionSelection = lobbySnapshot.missionSelections().get(localSeat);
    }
    refreshLobbyControls();
    repaint();
  }

  public void setMissionSelectionAction(Consumer<MissionOption> missionSelectionAction) {
    this.missionSelectionAction = missionSelectionAction == null ? mission -> {
    } : missionSelectionAction;
  }

  public void setLocalSeat(game.PlayerSeat localSeat) {
    this.localSeat = localSeat;
    refreshLobbyControls();
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
    finishGameButton.setVisible(shouldShowFinishGameButton());
    layoutHandler.addVisualComponent(increaseThreatButton, LayoutHandler.Priority.HIGH,
        layoutHandler.getSidebarButtonWidth(), layoutHandler.getSidebarButtonHeight());
    layoutHandler.addVisualComponent(nextRoundButton, LayoutHandler.Priority.HIGH,
        layoutHandler.getSidebarButtonWidth(), layoutHandler.getSidebarButtonHeight());
    layoutHandler.addVisualComponent(finishGameButton, LayoutHandler.Priority.HIGH,
        layoutHandler.getSidebarButtonWidth(), layoutHandler.getSidebarButtonHeight());
    buttonSize = layoutHandler.getMovementButtonSize();
  }

  private void positionOverlayComponents() {
    promptPanel.setBounds(layoutHandler.getPromptPanelBounds());
    missionOneButton.setBounds(layoutHandler.getMissionOneButtonBounds());
    missionTwoButton.setBounds(layoutHandler.getMissionTwoButtonBounds());
  }

  private void layoutSelectedDeploymentCard() {
    if (previousSelectedCard == null) {
      return;
    }
    Rectangle cardBounds = layoutHandler.getDeploymentCardBounds(previousSelectedCard.getBaseImageWidth(),
        previousSelectedCard.getBaseImageHeight());
    previousSelectedCard.setLayoutBounds(cardBounds, layoutHandler.getSidebarDetailBounds(cardBounds));
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

  private boolean shouldShowFinishGameButton() {
    return shouldShowThreatButton();
  }

  private void performIncreaseThreat() {
    if (readOnly) {
      increaseThreatAction.run();
      return;
    }
    if (!remoteMode) {
      new Thread(game::increaseThreat, "Manual Threat").start();
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
    if (!remoteMode) {
      game.finishCurrentRound();
      repaint();
    }
  }

  public void setIncreaseThreatAction(Runnable increaseThreatAction) {
    this.increaseThreatAction = increaseThreatAction == null ? () -> {
    } : increaseThreatAction;
  }

  public void setNextRoundAction(Runnable nextRoundAction) {
    this.nextRoundAction = nextRoundAction == null ? () -> {
    } : nextRoundAction;
  }

  public void setFinishGameAction(Runnable finishGameAction) {
    this.finishGameAction = finishGameAction == null ? () -> {
    } : finishGameAction;
  }

  private void startBannerTimer(long token) {
    if (bannerTimer != null) {
      bannerTimer.stop();
    }
    bannerTimer = new Timer(1000, e -> {
      if (bannerToken.get() != token) {
        ((Timer) e.getSource()).stop();
        return;
      }
      if (System.currentTimeMillis() >= bannerExpiresAt) {
        bannerText = null;
        repaint();
        ((Timer) e.getSource()).stop();
        return;
      }
      repaint();
    });
    bannerTimer.setRepeats(true);
    bannerTimer.start();
  }
}
