package src.net;

import java.io.Serializable;

import src.game.PlayerSeat;

public record JoinRequest(PlayerSeat requestedSeat) implements Serializable {
}
