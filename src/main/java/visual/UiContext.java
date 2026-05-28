package visual;

import javax.swing.JFrame;

import game.SelectionType;

// Centralized mutable UI state used by dialogs and shared rendering code.
public final class UiContext {
  private static volatile JFrame frame;
  private static volatile Screen screen;
  private static volatile SelectionType selectionType = SelectionType.EXPLANATION;

  private UiContext() {
  }

  public static JFrame getFrame() {
    return frame;
  }

  public static void setFrame(JFrame frame) {
    UiContext.frame = frame;
  }

  public static Screen getScreen() {
    return screen;
  }

  public static void setScreen(Screen screen) {
    UiContext.screen = screen;
  }

  public static SelectionType getSelectionType() {
    return selectionType;
  }

  public static void setSelectionType(SelectionType selectionType) {
    UiContext.selectionType = selectionType == null ? SelectionType.EXPLANATION : selectionType;
  }
}
