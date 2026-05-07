package runners;

import game.PlayerSeat;
import net.GameClient;
import net.NetworkConfig;

public class ClientRunner {
    public static void main(String[] args) throws Exception {
        String host = NetworkConfig.SERVER_HOST;
        int port = NetworkConfig.PORT;
        PlayerSeat seat = args.length > 0 ? PlayerSeat.valueOf(args[0]) : PlayerSeat.REBEL_1;
        new GameClient(host, port, seat).run();
    }
}
