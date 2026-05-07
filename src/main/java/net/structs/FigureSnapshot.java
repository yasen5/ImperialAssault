package net;

import java.io.Serializable;

import game.PlayerSeat;

public record FigureSnapshot(
        String id,
        String name,
        int x,
        int y,
        int health,
        int strain,
        boolean stunned,
        boolean focused,
        boolean active,
        boolean possibleTarget,
        boolean exhausted,
        PlayerSeat ownerSeat) implements Serializable {
}
