package net.structs;

import java.io.Serializable;
import util.MyArrayList;

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
        PlayerSeat ownerSeat,
        MyArrayList<String> equipmentIds,
        MyArrayList<String> conditions,
        boolean wounded,
        boolean defeated) implements Serializable {
}
