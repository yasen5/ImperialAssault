package game;

import java.io.Serializable;

public enum PlayerSeat implements Serializable {
    IMPERIAL,
    REBEL_1,
    REBEL_2;

    public boolean isRebel() {
        return this == REBEL_1 || this == REBEL_2;
    }
}
