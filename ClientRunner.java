import src.net.GameClient;
import src.game.PlayerSeat;

public class ClientRunner {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5050;
        PlayerSeat seat = args.length > 2 ? PlayerSeat.valueOf(args[2]) : PlayerSeat.REBEL_1;
        new GameClient(host, port, seat).run();
    }
}
