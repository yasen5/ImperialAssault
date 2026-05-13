package game;

import javax.swing.JOptionPane;

import visual.UiContext;
import visual.Screen;

public class InputUtils {
  // Get which response they chose, keep asking until response is valid
  public static int getMultipleChoice(String name, String explanation, Object[] options) {
    return showMultipleChoiceDialog(name, explanation, options);
  }

  public static int showMultipleChoiceDialog(String name, String explanation, Object[] options) {
    Screen screen = UiContext.getScreen();
    if (screen != null) {
      return screen.promptMultipleChoice(name, explanation, options);
    }
    int selectedIndex;
    do {
      selectedIndex = JOptionPane.showOptionDialog(
          UiContext.getFrame(),
          explanation,
          name,
          JOptionPane.DEFAULT_OPTION,
          JOptionPane.QUESTION_MESSAGE,
          null,
          options,
          null);
    } while (selectedIndex < 0 || selectedIndex >= options.length);
    return selectedIndex;
  }

  // Give yes/no prompt
  public static boolean getYesNo(String name, String explanation) {
    return showYesNoDialog(name, explanation);
  }

  public static boolean showYesNoDialog(String name, String explanation) {
    Screen screen = UiContext.getScreen();
    if (screen != null) {
      return screen.promptYesNo(name, explanation);
    }
    int response = JOptionPane.showConfirmDialog(
        UiContext.getFrame(),
        explanation,
        name,
        JOptionPane.YES_NO_OPTION);
    return response == JOptionPane.YES_OPTION;
  }

  // Get a numeric answer, force it to be valid
  public static int getNumericChoice(String name, int minValue, int maxValue) {
    return showNumericChoiceDialog(name, name, minValue, maxValue);
  }

  public static int showNumericChoiceDialog(String name, int minValue, int maxValue) {
    return showNumericChoiceDialog(name, name, minValue, maxValue);
  }

  public static int showNumericChoiceDialog(String name, String explanation, int minValue, int maxValue) {
    Screen screen = UiContext.getScreen();
    if (screen != null) {
      return screen.promptNumericChoice(name, explanation, minValue, maxValue);
    }
    int input = Integer.MIN_VALUE;
    String message = explanation == null || explanation.isBlank() ? name : explanation;
    message += " (" + minValue + " to " + maxValue + ")";
    do {
      String rawInput = JOptionPane.showInputDialog(UiContext.getFrame(), message, name,
          JOptionPane.QUESTION_MESSAGE);
      if (rawInput != null) {
        try {
          input = Integer.valueOf(rawInput.trim());
        } catch (NumberFormatException ex) {
          input = Integer.MIN_VALUE;
        }
      }
    } while (input < minValue || input > maxValue);
    return input;
  }
}
