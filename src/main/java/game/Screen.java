package game;

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.SwingUtilities;

import game.BiMap;
import game.DeploymentCard;
import game.Game;
import game.LoaderUtils;
import game.Personnel;
import game.Personnel.Directions;
import game.Pos;
import net.LobbySnapshot;
import net.RemotePrompt;
import net.RemotePrompt.PromptType;

public class Screen extends JPanel implements ActionListener, MouseListener, KeyListener {
    private final boolean remoteMode;
    private final Game game;
    private boolean gameStarted = false;
    private BufferedImage startScreenimage;
    private static int buttonSize = 20;
    private CompletableFuture<Directions> movementButtonOutput;
    private static boolean gameEnd = false;
    private Thread mainGameLoop;
    private static SelectingType currentSelectionType = SelectingType.EXPLANATION;
    private static DeploymentCard previousSelectedCard;
    private JEditorPane editorPane;
    private boolean rebelsWin = true;
    private CompletableFuture<String> remoteBoardSelection;
    private RemotePrompt activeRemotePrompt;
    private CompletableFuture<String> activePromptResponse;
    private PromptKind activePromptKind;
    private final JPanel promptPanel = new JPanel(new BorderLayout(8, 8));
    private final JLabel promptTitleLabel = new JLabel();
    private final JTextArea promptMessageArea = new JTextArea();
    private final JPanel promptActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JTextField numericPromptField = new JTextField();
    private final JButton numericSubmitButton = new JButton("Submit");
    private final JButton readyButton = new JButton("Ready");
    private int numericPromptMinValue;
    private int numericPromptMaxValue;
    private final AtomicLong bannerToken = new AtomicLong(0L);
    private Timer bannerTimer;
    private volatile String statusText = "Turn: --";
    private volatile String bannerText;
    private volatile long bannerExpiresAt;
    private volatile LobbySnapshot lobbySnapshot;
    private boolean readySubmitted;
    private Runnable readyAction = () -> {
    };

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
                    "<h3>Changes:</h3>" +
                    "<p>The main changes are that you are only playing with two rebels, and without the droid or e-web engineer. There are no bleeding effects. There are no crates. Specials can be activated multiple times per activation. Melee vs ranged weapon types are not important. Heroes do not flip to a wounded side but are rather immediately defeated. You can move through any figure. Keywords other than \"stun\" and \"focused\" are irrelevant</p>"
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

    public static enum SelectingType {
        EXPLANATION,
        COMBAT,
        SPECIAL
    }

    private static enum PromptKind {
        MULTIPLE_CHOICE,
        YES_NO,
        NUMERIC
    }

    public Screen() {
        this(new Game(null), false);
        game.setUi(this);
    }

    public Screen(Game game, boolean remoteMode) {
        this.game = game;
        this.remoteMode = remoteMode;
        this.game.setUi(this);
        Constants.screen = this;
        setFocusable(true);
        setLayout(null);
        if (!remoteMode) {
            showInstructionsChain();
        }
        initializeButtons();
        initializePromptPanel();
        addMouseListener(this);
        addKeyListener(this);
        setBackground(new Color(0, 0, 0));
        startScreenimage = LoaderUtils.getImage("IACoverArt");
        if (!game.getHeroes().isEmpty()) {
            previousSelectedCard = game.getHeroes().get(0).getDeploymentCard();
            previousSelectedCard.setVisible(true);
        }
        if (remoteMode) {
            initializeLobbyControls();
        }
    }

    private void initializeLobbyControls() {
        readyButton.setVisible(false);
        readyButton.addActionListener(e -> {
            readySubmitted = true;
            readyButton.setEnabled(false);
            readyButton.setText("Ready sent");
            readyAction.run();
        });
        readyButton.setBounds(getPreferredSize().width / 2 - 90, 900, 180, 44);
        add(readyButton);
    }

    private void initializePromptPanel() {
        promptPanel.setBounds(980, 760, 900, 260);
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

    private CompletableFuture<String> beginPrompt(PromptKind kind, String name, String explanation) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Runnable setup = () -> {
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

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1920, 1080);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gameEnd) {
            g.setColor(new Color(0, 0, 0));
            g.drawRect(0, 0, 1920, 1080);
            g.setColor(new Color(255, 255, 255));
            g.drawString("Game over, " + (rebelsWin ? "rebels " : "imperials") + " won", 100, 100);
        } else if (gameStarted) {
            g.setColor(new Color(25, 25, 25));
            g.fillRect(getPreferredSize().width / 2 - 200, 0, getPreferredSize().width / 2 + 200,
                    getPreferredSize().height);
            game.drawGame(g);
            drawTurnHud(g);
        } else {
            g.drawImage(startScreenimage, 0, 0, getPreferredSize().width, getPreferredSize().height, 0, 0,
                    startScreenimage.getWidth(null), startScreenimage.getHeight(null), null);
            drawLobbyOverlay(g);
        }
    }

    private void drawLobbyOverlay(Graphics g) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
        Composite original = g2.getComposite();
        int panelWidth = 760;
        int panelHeight = 430;
        int x = getPreferredSize().width / 2 - panelWidth / 2;
        int y = 170;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
        g2.setColor(new Color(10, 10, 10));
        g2.fillRoundRect(x, y, panelWidth, panelHeight, 28, 28);
        g2.setComposite(original);
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
        g2.drawString("Lobby", x + 28, y + 44);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20f));
        String header = lobbySnapshot == null ? "Waiting for players to connect"
                : lobbySnapshot.allSeatsFilled() ? (lobbySnapshot.allReady() ? "All players are ready"
                        : "All seats filled. Press Ready to begin")
                        : "Waiting for all seats to fill";
        g2.drawString(header, x + 28, y + 82);

        if (lobbySnapshot == null) {
            g2.drawString("Connecting...", x + 28, y + 128);
            return;
        }

        int rowY = y + 132;
        for (game.PlayerSeat seat : lobbySnapshot.config().requiredSeats()) {
            boolean occupied = lobbySnapshot.occupiedSeats().contains(seat);
            boolean ready = lobbySnapshot.readySeats().contains(seat);
            String label = formatSeat(seat);
            String state = occupied ? (ready ? "ready" : "joined") : "open";
            g2.drawString(label, x + 28, rowY);
            g2.drawString(state, x + panelWidth - 120, rowY);
            rowY += 42;
        }
    }

    private void drawTurnHud(Graphics g) {
        g.setFont(g.getFont().deriveFont(Font.BOLD, 22f));
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(20, 20, 280, 54, 18, 18);
        g.setColor(Color.WHITE);
        g.drawRoundRect(20, 20, 280, 54, 18, 18);
        g.drawString(statusText, 38, 54);

        long remaining = bannerExpiresAt - System.currentTimeMillis();
        if (bannerText == null || remaining <= 0) {
            return;
        }
        float alpha = Math.max(0f, Math.min(1f, remaining / 1400f));
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
        Composite original = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        int width = 560;
        int x = getPreferredSize().width / 2 - width / 2;
        int y = 18;
        g2.setColor(new Color(15, 15, 15));
        g2.fillRoundRect(x, y, width, 56, 20, 20);
        g2.setColor(new Color(255, 255, 255));
        g2.drawRoundRect(x, y, width, 56, 20, 20);
        g2.drawString(bannerText, x + 24, y + 35);
        g2.setComposite(original);
    }

    private String formatSeat(game.PlayerSeat seat) {
        return switch (seat) {
            case IMPERIAL -> "Imperial";
            case REBEL_1 -> "Rebel 1";
            case REBEL_2 -> "Rebel 2";
        };
    }

    @Override
    public void actionPerformed(ActionEvent e) {
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
                        .getPersonnelAtPosInternal(new Pos(e.getX() / Constants.tileSize, e.getY() / Constants.tileSize));
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
                        .getPersonnelAtPosInternal(new Pos(e.getX() / Constants.tileSize, e.getY() / Constants.tileSize));
                if (!remoteMode && personnel != null && game.trySetTarget(personnel)) {
                    currentSelectionType = SelectingType.EXPLANATION;
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
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            gameEnd = true;
        } else if (!remoteMode && e.getKeyCode() == KeyEvent.VK_R) {
            reset();
        }
        repaint();
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
        CompletableFuture<String> future = beginPrompt(PromptKind.MULTIPLE_CHOICE, name, explanation);
        SwingUtilities.invokeLater(() -> {
            promptActionsPanel.removeAll();
            for (int i = 0; i < options.length; i++) {
                addPromptButton(String.valueOf(options[i]), String.valueOf(i));
            }
            promptPanel.revalidate();
            promptPanel.repaint();
        });
        return Integer.parseInt(future.join());
    }

    public boolean promptYesNo(String name, String explanation) {
        CompletableFuture<String> future = beginPrompt(PromptKind.YES_NO, name, explanation);
        SwingUtilities.invokeLater(() -> {
            promptActionsPanel.removeAll();
            addPromptButton("No", String.valueOf(false));
            addPromptButton("Yes", String.valueOf(true));
            promptPanel.revalidate();
            promptPanel.repaint();
        });
        return Boolean.parseBoolean(future.join());
    }

    public int promptNumericChoice(String name, String explanation, int minValue, int maxValue) {
        numericPromptMinValue = minValue;
        numericPromptMaxValue = maxValue;
        CompletableFuture<String> future = beginPrompt(PromptKind.NUMERIC, name,
                explanation + " (" + minValue + " to " + maxValue + ")");
        SwingUtilities.invokeLater(() -> {
            numericPromptField.setToolTipText("Enter a value from " + minValue + " to " + maxValue);
        });
        return Integer.parseInt(future.join());
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

    public void setSelectionType(SelectingType value) {
        currentSelectionType = value;
    }

    public static SelectingType getSelectionType() {
        return currentSelectionType;
    }

    public void endGame(boolean rebelsWin) {
        this.rebelsWin = rebelsWin;
        deactiveateMovementButtons();
        gameEnd = true;
        LoaderUtils.playSound("Applause");
    }

    public CompletableFuture<String> beginRemoteBoardPrompt(RemotePrompt prompt) {
        activeRemotePrompt = prompt;
        remoteBoardSelection = new CompletableFuture<>();
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
        List<String> allowedValues = prompt.allowedValues();
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
        deactiveateMovementButtons();
        setSelectionType(SelectingType.EXPLANATION);
        repaint();
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
        if (!gameStarted) {
            boolean canReady = lobbySnapshot != null && lobbySnapshot.allSeatsFilled() && !lobbySnapshot.allReady()
                    && !readySubmitted;
            readyButton.setVisible(canReady);
            readyButton.setEnabled(canReady);
            readyButton.setText("Ready");
        }
        repaint();
    }

    public void setReadyAction(Runnable readyAction) {
        this.readyAction = readyAction == null ? () -> {
        } : readyAction;
    }

    public void markGameStarted() {
        gameStarted = true;
        readySubmitted = true;
        readyButton.setVisible(false);
        readyButton.setEnabled(false);
        repaint();
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public Game getGame() {
        return game;
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
