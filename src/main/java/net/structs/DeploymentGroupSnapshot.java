package net.structs;

import java.io.Serializable;
import java.util.List;

import game.PlayerSeat;

public record DeploymentGroupSnapshot(
        String id,
        String name,
        boolean exhausted,
        PlayerSeat ownerSeat,
        List<FigureSnapshot> members) implements Serializable {
}
