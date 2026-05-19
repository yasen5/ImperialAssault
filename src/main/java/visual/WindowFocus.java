package visual;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class WindowFocus {
  private WindowFocus() {
  }

  public static void configureProcessForNonActivatingWindows() {
    if (isMac()) {
      System.setProperty("apple.awt.UIElement", "true");
    }
  }

  public static void packAndShowWithoutTakingFocus(JFrame frame) {
    configureFrameForNonActivatingShow(frame);
    frame.pack();
    showWithoutTakingFocus(frame);
  }

  public static void showWithoutTakingFocus(JFrame frame) {
    configureFrameForNonActivatingShow(frame);
    frame.setVisible(true);
    EventQueue.invokeLater(() -> {
      frame.toBack();
      SwingUtilities.invokeLater(() -> frame.setFocusableWindowState(true));
    });
  }

  private static void configureFrameForNonActivatingShow(JFrame frame) {
    frame.setAutoRequestFocus(false);
    frame.setFocusableWindowState(false);
  }

  private static boolean isMac() {
    return System.getProperty("os.name", "").toLowerCase().contains("mac");
  }
}
