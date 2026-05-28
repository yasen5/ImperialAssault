package net.structs;

import java.io.Serializable;
import util.MyArrayList;



import game.PlayerSeat;

public record GameSessionConfig(int rebelPlayerCount) implements Serializable {
    public GameSessionConfig {
        if (rebelPlayerCount < 0 || rebelPlayerCount > 4) {
            rebelPlayerCount = Math.max(0, Math.min(4, rebelPlayerCount));
        }
    }

    public MyArrayList<PlayerSeat> rebelTurnOrder() {
        MyArrayList<PlayerSeat> seats = new MyArrayList<>();
        if (rebelPlayerCount >= 1) {
            seats.add(PlayerSeat.REBEL_1);
        }
        if (rebelPlayerCount >= 2) {
            seats.add(PlayerSeat.REBEL_2);
        }
        if (rebelPlayerCount >= 3) {
            seats.add(PlayerSeat.REBEL_3);
        }
        if (rebelPlayerCount == 4) {
            seats.add(PlayerSeat.REBEL_4);
        }
        return seats;
    }

    public MyArrayList<PlayerSeat> requiredSeats() {
        MyArrayList<PlayerSeat> seats = new MyArrayList<>();
        seats.add(PlayerSeat.IMPERIAL);
        if (rebelPlayerCount >= 1) {
            seats.add(PlayerSeat.REBEL_1);
        }
        if (rebelPlayerCount >= 2) {
            seats.add(PlayerSeat.REBEL_2);
        }
        if (rebelPlayerCount >= 3) {
            seats.add(PlayerSeat.REBEL_3);
        }
        if (rebelPlayerCount == 4) {
            seats.add(PlayerSeat.REBEL_4);
        }
        return seats;
    }
}
