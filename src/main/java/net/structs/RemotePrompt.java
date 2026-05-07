package net;

import java.io.Serializable;
import java.util.List;

import game.Screen.SelectingType;
import game.PlayerSeat;

public record RemotePrompt(
        long promptId,
        PlayerSeat seat,
        PromptType type,
        String title,
        String message,
        List<String> optionLabels,
        int minValue,
        int maxValue,
        List<String> allowedValues,
        String subjectId,
        SelectingType selectionType) implements Serializable {
    public static enum PromptType {
        MULTIPLE_CHOICE,
        YES_NO,
        NUMERIC,
        DIRECTION,
        TARGET
    }
}
