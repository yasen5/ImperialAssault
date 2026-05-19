package visual;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class WindowFocus {
  private WindowFocus() {
  }

  public static void showWithoutTakingFocus(JFrame frame) {
    frame.setAutoRequestFocus(false);
    frame.setFocusableWindowState(false);
    frame.setVisible(true);
    SwingUtilities.invokeLater(() -> frame.setFocusableWindowState(true));
  }
}
