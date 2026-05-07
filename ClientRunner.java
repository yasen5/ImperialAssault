import src.game.PlayerSeat;
import src.net.GameClient;

public class ClientRunner {
    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 5050;
        PlayerSeat seat = args.length > 0 ? PlayerSeat.valueOf(args[0]) : PlayerSeat.REBEL_1;
        new GameClient(host, port, seat).run();
    }
}
