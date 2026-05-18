package net.structs;

import java.io.Serializable;
import util.MyArrayList;


import game.PlayerSeat;

public record DeploymentGroupSnapshot(
        String id,
        String name,
        boolean exhausted,
        boolean deployed,
        int deploymentCost,
        int reinforcementCost,
        int maxMemberCount,
        PlayerSeat ownerSeat,
        MyArrayList<FigureSnapshot> members) implements Serializable {
}
