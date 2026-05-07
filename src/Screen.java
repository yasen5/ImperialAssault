package src;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
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

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import src.game.BiMap;
import src.game.DeploymentCard;
import src.game.Game;
import src.game.LoaderUtils;
import src.game.Personnel;
import src.game.Personnel.Directions;
import src.game.Pos;
import src.net.RemotePrompt;
import src.net.RemotePrompt.PromptType;

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
    private int animationTextPos = 0;
    private boolean rebelsWin = true;
    private CompletableFuture<String> remoteBoardSelection;
    private RemotePrompt activeRemotePrompt;

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

    public Screen() {
        this(new Game(null), false);
        game.setUi(this);
    }

    public Screen(Game game, boolean remoteMode) {
        this.game = game;
        this.remoteMode = remoteMode;
        this.game.setUi(this);
        setFocusable(true);
        setLayout(null);
        if (!remoteMode) {
            showInstructionsChain();
        }
        initializeButtons();
        addMouseListener(this);
        addKeyListener(this);
        setBackground(new Color(0, 0, 0));
        startScreenimage = LoaderUtils.getImage("IACoverArt");
        if (!game.getHeroes().isEmpty()) {
            previousSelectedCard = game.getHeroes().get(0).getDeploymentCard();
            previousSelectedCard.setVisible(true);
        }
        if (remoteMode) {
            gameStarted = true;
        } else {
            animate();
        }
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
        } else {
            g.drawImage(startScreenimage, 0, 0, getPreferredSize().width, getPreferredSize().height, 0, 0,
                    startScreenimage.getWidth(null), startScreenimage.getHeight(null), null);
            g.setColor(new Color(0, 0, 0));
            g.drawString("Woo animation yeah", animationTextPos, 50);
        }
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

    public void animate() {
        Thread animationThread = new Thread(() -> {
            while (!gameStarted) {
                animationTextPos += 5;
                if (animationTextPos > 800) {
                    animationTextPos = 0;
                }
                try {
                    Thread.sleep(50);
                    javax.swing.SwingUtilities.invokeLater(() -> repaint());
                } catch (Exception e) {
                    break;
                }
            }
        });
        animationThread.start();
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

    public Game getGame() {
        return game;
    }
}
