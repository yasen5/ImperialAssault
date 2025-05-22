import javax.swing.JFrame;

import src.Screen;
import src.Constants;

public class Runner {
	// Basic package to run graphics
	public static void main(String args[]) {
		Screen game = new Screen();
		Constants.frame = new JFrame("Card Game");

		Constants.frame.add(game);

		Constants.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Constants.frame.pack();
		Constants.frame.setVisible(true);
	}
}
