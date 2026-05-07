package game;

import javax.swing.JFrame;

// Centralized mutable UI state used by dialogs and shared rendering code.
public final class UiContext {
    private static volatile JFrame frame;
    private static volatile Screen screen;

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
}
