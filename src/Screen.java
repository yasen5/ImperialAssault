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
import java.io.File;
import java.io.IOException;
import java.security.KeyStore.LoadStoreParameter;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
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

public class Screen extends JPanel implements ActionListener, MouseListener, KeyListener {
	// Instance variables
	private Game game;
	private boolean gameStarted = false; // Turn to false when submitting
	private BufferedImage startScreenimage;
	private static int buttonSize = 20;
	private CompletableFuture<Directions> movementButtonOutput;
	private static boolean gameEnd = false;
	private Thread mainGameLoop;
	private static SelectingType currentSelectionType = SelectingType.EXPLANATION;
	private static DeploymentCard previousSelectedCard;
	private JEditorPane editorPane;
	private int animationTextPos = 0;
	// Chain of dialogues before the game begins
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
	private boolean rebelsWin = true;

	public static BiMap<Directions, JButton> movementButtons = new BiMap<Directions, JButton>();

	public static enum SelectingType {
		EXPLANATION,
		COMBAT,
		SPECIAL
	}

	// Run through the dialogue chain until it finishes (will block the thread)
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
							JOptionPane.showMessageDialog(null,
									"Could not open link: " + e.getURL(),
									"Error",
									JOptionPane.ERROR_MESSAGE);
						}
					}
				});
			}
			JOptionPane.showConfirmDialog(
					null,
					editorPane,
					"Instructions",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.INFORMATION_MESSAGE);

		}
	}

	// Constructor, sets everything up
	public Screen() {
		setFocusable(true);
		setLayout(null);
		showInstructionsChain();
		movementButtons.put(Directions.UP,
				new JButton(new ImageIcon(new ImageIcon(Constants.baseImgFilePath + "ArrowButtonUp.png").getImage()
						.getScaledInstance(buttonSize,
								buttonSize, Image.SCALE_SMOOTH))));
		movementButtons.put(Directions.UPLEFT,
				new JButton(new ImageIcon(new ImageIcon(Constants.baseImgFilePath + "ArrowButtonUpLeft.png").getImage()
						.getScaledInstance(buttonSize,
								buttonSize, Image.SCALE_SMOOTH))));
		movementButtons.put(Directions.LEFT,
				new JButton(new ImageIcon(new ImageIcon(Constants.baseImgFilePath + "ArrowButtonLeft.png").getImage()
						.getScaledInstance(buttonSize,
								buttonSize, Image.SCALE_SMOOTH))));
		movementButtons.put(Directions.DOWNLEFT,
				new JButton(
						new ImageIcon(new ImageIcon(Constants.baseImgFilePath + "ArrowButtonDownLeft.png").getImage()
								.getScaledInstance(buttonSize,
										buttonSize, Image.SCALE_SMOOTH))));
		movementButtons.put(Directions.DOWN,
				new JButton(new ImageIcon(new ImageIcon(Constants.baseImgFilePath + "ArrowButtonDown.png").getImage()
						.getScaledInstance(buttonSize,
								buttonSize, Image.SCALE_SMOOTH))));
		movementButtons.put(Directions.DOWNRIGHT,
				new JButton(
						new ImageIcon(new ImageIcon(Constants.baseImgFilePath + "ArrowButtonDownRight.png").getImage()
								.getScaledInstance(buttonSize,
										buttonSize, Image.SCALE_SMOOTH))));
		movementButtons.put(Directions.RIGHT,
				new JButton(new ImageIcon(new ImageIcon(Constants.baseImgFilePath + "ArrowButtonRight.png").getImage()
						.getScaledInstance(buttonSize,
								buttonSize, Image.SCALE_SMOOTH))));
		movementButtons.put(Directions.UPRIGHT,
				new JButton(new ImageIcon(new ImageIcon(Constants.baseImgFilePath + "ArrowButtonUpRight.png").getImage()
						.getScaledInstance(buttonSize,
								buttonSize, Image.SCALE_SMOOTH))));
		for (JButton button : movementButtons.map.values()) {
			add(button);
			button.addActionListener(this);
			button.setVisible(false);
			button.setBounds(0, 0, buttonSize, buttonSize);
		}
		// Instantiate a game object
		addMouseListener(this);
		addKeyListener(this);
		game = new Game(this);
		setBackground(new Color(0, 0, 0));
		startScreenimage = LoaderUtils.getImage("IACoverArt");
		previousSelectedCard = game.getHeroes().get(0).getDeploymentCard();
		previousSelectedCard.setVisible(true);
		animate();
	}

	@Override
	public Dimension getPreferredSize() {
		// Sets the size of the panel
		return new Dimension(1920, 1080);
	}

	// Draw based on stage of the game
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (gameEnd) {
			// End screen
			g.setColor(new Color(0, 0, 0));
			g.drawRect(0, 0, 1920, 1080);
			g.setColor(new Color(255, 255, 255));
			g.drawString("Game over, " + (rebelsWin ? "rebels " : "imperials") + " won", 100, 100);
		} else if (gameStarted) {
			// Regular game
			g.setColor(new Color(25, 25, 25));
			g.fillRect(getPreferredSize().width / 2 - 200, 0, getPreferredSize().width / 2 + 200,
					getPreferredSize().height);
			game.drawGame(g);
		} else {
			// Start screen
			g.drawImage(startScreenimage, 0,
					0,
					getPreferredSize().width,
					getPreferredSize().height, 0, 0, startScreenimage.getWidth(null), startScreenimage.getHeight(null),
					null);
			g.setColor(new Color(0, 0, 0));
			g.drawString("Woo animation yeah", animationTextPos, 50);
		}
	}

	// Report movement button output
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		for (Directions direction : Directions.values()) {
			if (source.equals(movementButtons.get(direction))) {
				movementButtonOutput.complete(direction);
				continue;
			}
		}

	}

	public void mousePressed(MouseEvent e) {

	}

	// Perform the necessary tasks based on the type of selection
	public void mouseReleased(MouseEvent e) {
		if (!gameStarted) {
			gameStarted = true;
			repaint();
			mainGameLoop = new Thread(() -> game.playRound());
			mainGameLoop.start();
		} else {
			switch (currentSelectionType) {
				case COMBAT:
				case SPECIAL:
					Personnel personnel = Game
							.getPersonnelAtPos(new Pos(e.getX() / Constants.tileSize, e.getY() / Constants.tileSize));
					if (personnel != null && Game.setTarget(personnel)) {
						currentSelectionType = SelectingType.EXPLANATION;
					}
					break;
				case EXPLANATION:
					previousSelectedCard.setVisible(false);
					DeploymentCard newDeploymentCard = game
							.getDeploymentCard(new Pos(e.getX() / Constants.tileSize, e.getY() / Constants.tileSize));
					if (newDeploymentCard != null) {
						previousSelectedCard = newDeploymentCard;
					}
					previousSelectedCard.setVisible(true);
			}
		}
		repaint();
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void keyPressed(KeyEvent e) {

	}

	// Cheat key and reset
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_F1) {
			gameEnd = true;
		} else if (e.getKeyCode() == KeyEvent.VK_R) {
			reset();
		}
		repaint();
	}

	public void keyTyped(KeyEvent e) {

	}

	// Moves and activates the button at a specific area (used for movement buttons)
	public JButton moveAndActivateButton(Directions direction, int x, int y, double angleRads) {
		int xDiff = (int) (Constants.tileSize *
				Math.cos(angleRads));
		int yDiff = -1 * (int) (Constants.tileSize * Math.sin(angleRads));
		movementButtons.get(direction).setBounds((int) ((x + 0.25) * Constants.tileSize) + xDiff,
				(int) ((y + 0.25) * Constants.tileSize) + yDiff, buttonSize, buttonSize);
		movementButtons.get(direction).setEnabled(true);
		movementButtons.get(direction).setVisible(true);
		revalidate();
		repaint();
		return movementButtons.get(direction);
	}

	public BiMap<Directions, JButton> getMovementButtons() {
		return movementButtons;
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

	// Reset everything
	public void reset() {
		gameEnd = false;
		deactiveateMovementButtons();
		game.reset();
		mainGameLoop.run();
		repaint();
	}

	public void setSelectionType(SelectingType value) {
		currentSelectionType = value;
	}

	public static SelectingType getSelectionType() {
		return currentSelectionType;
	}

	public void endGame(boolean rebelsWin) {
		deactiveateMovementButtons();
		gameEnd = true;
		try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(new File("/Users/yasen/Documents/Quarter4Project/src/game/sounds/Applause.wav")));
            clip.start();
        } catch (Exception exc) {
            exc.printStackTrace(System.out);
        }
	}

	// Animation in a boardgame for some reason
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
					System.out.println("Animation thread interrupted");
					break;
				}
			}
		});
		animationThread.start();
	}
}
