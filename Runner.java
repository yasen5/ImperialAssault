import javax.swing.JFrame;

import src.Screen;

public class Runner {
	private static JFrame frame;

	// Basic package to run graphics
	public static void main(String args[]) {
		Screen game = new Screen();
		frame = new JFrame("Card Game");

		frame.add(game);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	public static JFrame getFrame() {
		return frame;
	}
}
