package src;

import java.awt.Color;
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
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import src.game.BiMap;
import src.game.Game;
import src.game.Imperial;
import src.game.Personnel;

import src.game.Personnel.Directions;

public class Screen extends JPanel implements ActionListener, MouseListener, KeyListener {
	private Game game;
	private boolean gameStarted = false; // Turn to false when submitting
	private BufferedImage startScreenimage;
	private static int buttonSize = 20;
	private CompletableFuture<Directions> movementButtonOutput;
	private CompletableFuture<Personnel> selectionButtonOutput;
	private boolean selectingImperials;
	private boolean gameEnd = false;
	private Thread mainGameLoop;

	public BiMap<Directions, JButton> movementButtons = new BiMap<Directions, JButton>();

	public Screen() {
		setFocusable(true);
		setLayout(null);
		ImageIcon originalIcon = new ImageIcon("path/to/image.png");
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
		try {
			startScreenimage = ImageIO
					.read(new File(Constants.baseImgFilePath + "IACoverArt.png"));
		} catch (IOException e) {
			System.out.println("Haha l no cover art");
		}
		for (JButton button : game.getSelectionButtons()) {
			add(button);
			button.addActionListener(this);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		// Sets the size of the panel
		return new Dimension(1920, 1080);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (gameEnd) {
			System.out.println("SO THEN WHY ISN'T THIS GETTING DRAWN");
			g.setColor(new Color(0, 0, 0));
			g.drawRect(0, 0, 1920, 1080);
			g.setColor(new Color(255, 255, 255));
			g.drawString("Game over", 100, 100);
		}
		else if (gameStarted) {
			game.drawGame(g);
		}  
		else {
			g.drawImage(startScreenimage, 0,
					0,
					getPreferredSize().width,
					getPreferredSize().height, 0, 0, startScreenimage.getWidth(null), startScreenimage.getHeight(null),
					null);
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		for (Directions direction : Directions.values()) {
			if (source.equals(movementButtons.get(direction))) {
				movementButtonOutput.complete(direction);
				continue;
			}
		}
		if (selectingImperials) {
			for (Imperial imperial : game.getImperials()) {
				if (source.equals(imperial.getSelectionButton())) {
					selectionButtonOutput.complete(imperial);
				}
			}
		}
	}

	public void mousePressed(MouseEvent e) {

	}

	public void mouseReleased(MouseEvent e) {
		if (!gameStarted) {
			gameStarted = true;
			repaint();
			mainGameLoop = new Thread(() -> game.playRound());
			mainGameLoop.start();
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

	public void keyReleased(KeyEvent e) {
		game.handleKeyboardInput(e.getKeyCode());
		if (e.getKeyCode() == KeyEvent.VK_F1) {
			gameEnd = true;
		} else if (e.getKeyCode() == KeyEvent.VK_R) {
			reset();
		}
		repaint();
	}

	public void keyTyped(KeyEvent e) {
		
	}

	public void Repaint() {
		repaint();
	}

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

	public void setSelectionButtonOutput(CompletableFuture<Personnel> output, boolean selectingImperials) {
		this.selectionButtonOutput = output;
		this.selectingImperials = selectingImperials;
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
		repaint();
		mainGameLoop = new Thread(() -> game.playRound());
		mainGameLoop.start();
	}
}
