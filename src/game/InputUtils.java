package src.game;

import javax.swing.JOptionPane;

import src.Constants;

public class InputUtils {
    // Get which response they chose, keep asking until response is valid
    public static int getMultipleChoice(String name, String explanation, Object[] options) {
        int selectedIndex;
        do {
            selectedIndex = JOptionPane.showOptionDialog(
                    Constants.frame,
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
        int response = JOptionPane.showConfirmDialog(
                Constants.frame,
                explanation,
                name,
                JOptionPane.YES_NO_OPTION);
        return response == JOptionPane.YES_OPTION;
    }

    // Get a numeric answer, force it to be valid
    public static int getNumericChoice(String name, int minValue, int maxValue) {
        int input;
        do {
            input = Integer
                    .valueOf(JOptionPane.showInputDialog(name));
        } while (input < minValue || input > maxValue);
        return input;
    }
}
