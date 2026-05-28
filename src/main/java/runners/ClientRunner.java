package runners;

import game.PlayerSeat;
import net.GameClient;
import net.NetworkConfig;

public class ClientRunner {
    public static void main(String[] args) throws Exception {
        String host = NetworkConfig.SERVER_HOST;
        int port = NetworkConfig.PORT;
        PlayerSeat seat = null;
        boolean debugWallLines = false;
        for (String arg : args) {
            String mode = arg.trim();
            if ("--debug".equalsIgnoreCase(mode)) {
                debugWallLines = true;
            } else {
                seat = PlayerSeat.valueOf(mode);
            }
        }
        new GameClient(host, port, seat, debugWallLines).run();
    }
}
