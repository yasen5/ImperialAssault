package src.net;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import src.Screen.SelectingType;
import src.game.Game;
import src.game.GameDecisionProvider;
import src.game.GameSessionConfig;
import src.game.Personnel;
import src.game.PlayerSeat;
import src.game.Personnel.Directions;

public class GameServer {
    private final int port;
    private final GameSessionConfig config;
    private final EnumMap<PlayerSeat, ClientConnection> clients = new EnumMap<>(PlayerSeat.class);
    private final AtomicLong promptIds = new AtomicLong(1);

    public GameServer(int port, int rebelPlayers) {
        this.port = port;
        this.config = new GameSessionConfig(rebelPlayers);
    }

    public void run() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (clients.size() < config.requiredSeats().size()) {
                Socket socket = serverSocket.accept();
                ClientConnection connection = new ClientConnection(socket);
                JoinRequest request = (JoinRequest) connection.in.readObject();
                if (!config.requiredSeats().contains(request.requestedSeat()) || clients.containsKey(request.requestedSeat())) {
                    connection.out.writeObject(
                            new JoinResponse(false, "Seat unavailable", request.requestedSeat(), config));
                    connection.out.flush();
                    socket.close();
                    continue;
                }
                clients.put(request.requestedSeat(), connection);
                connection.seat = request.requestedSeat();
                connection.out.writeObject(new JoinResponse(true, "Joined", request.requestedSeat(), config));
                connection.out.flush();
                connection.startReader();
            }
            Game game = new Game(null, config, new RemoteDecisionProvider(), true);
            game.setSnapshotListener(this::broadcastSnapshot);
            broadcastSnapshot(game.createSnapshot());
            Thread gameThread = new Thread(game::playRound);
            gameThread.start();
            gameThread.join();
        }
    }

    private void broadcastSnapshot(MatchSnapshot snapshot) {
        for (ClientConnection connection : clients.values()) {
            connection.send(snapshot);
        }
    }

    private class RemoteDecisionProvider implements GameDecisionProvider {
        @Override
        public int chooseMultipleChoice(PlayerSeat seat, String name, String explanation, Object[] options) {
            ArrayList<String> labels = new ArrayList<>();
            for (Object option : options) {
                labels.add(String.valueOf(option));
            }
            RemotePrompt prompt = new RemotePrompt(promptIds.getAndIncrement(), seat, RemotePrompt.PromptType.MULTIPLE_CHOICE,
                    name, explanation, labels, 0, labels.size() - 1, labels, null, null);
            return Integer.parseInt(requestResponse(prompt));
        }

        @Override
        public boolean chooseYesNo(PlayerSeat seat, String name, String explanation) {
            RemotePrompt prompt = new RemotePrompt(promptIds.getAndIncrement(), seat, RemotePrompt.PromptType.YES_NO,
                    name, explanation, List.of("No", "Yes"), 0, 1, List.of("false", "true"), null, null);
            return Boolean.parseBoolean(requestResponse(prompt));
        }

        @Override
        public int chooseNumericChoice(PlayerSeat seat, String name, int minValue, int maxValue) {
            RemotePrompt prompt = new RemotePrompt(promptIds.getAndIncrement(), seat, RemotePrompt.PromptType.NUMERIC,
                    name, name, List.of(), minValue, maxValue, List.of(), null, null);
            return Integer.parseInt(requestResponse(prompt));
        }

        @Override
        public Directions chooseDirection(PlayerSeat seat, Personnel activeFigure, ArrayList<Directions> allowedDirections) {
            ArrayList<String> values = new ArrayList<>();
            for (Directions direction : allowedDirections) {
                values.add(direction.name());
            }
            RemotePrompt prompt = new RemotePrompt(promptIds.getAndIncrement(), seat, RemotePrompt.PromptType.DIRECTION,
                    "Movement", "Choose a direction", values, 0, 0, values, activeFigure.getId(), null);
            return Directions.valueOf(requestResponse(prompt));
        }

        @Override
        public Personnel chooseTarget(PlayerSeat seat, SelectingType selectionType, ArrayList<Personnel> availableTargets) {
            ArrayList<String> values = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();
            for (Personnel target : availableTargets) {
                values.add(target.getId());
                labels.add(target.getName());
            }
            RemotePrompt prompt = new RemotePrompt(promptIds.getAndIncrement(), seat, RemotePrompt.PromptType.TARGET,
                    "Target Selection", "Choose a target", labels, 0, 0, values, null, selectionType);
            String response = requestResponse(prompt);
            Personnel target = Game.current().getPersonnelById(response);
            if (target == null) {
                throw new IllegalStateException("Unknown target id " + response);
            }
            return target;
        }

        private String requestResponse(RemotePrompt prompt) {
            ClientConnection connection = clients.get(prompt.seat());
            connection.send(Game.current().createSnapshot());
            connection.send(prompt);
            PromptResponse response;
            do {
                response = connection.takeResponse();
            } while (response.promptId() != prompt.promptId());
            return response.value();
        }
    }

    private static class ClientConnection {
        private final Socket socket;
        private final ObjectOutputStream out;
        private final ObjectInputStream in;
        private final BlockingQueue<PromptResponse> responses = new LinkedBlockingQueue<>();
        private PlayerSeat seat;

        private ClientConnection(Socket socket) throws Exception {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        }

        private void startReader() {
            Thread readerThread = new Thread(() -> {
                try {
                    while (true) {
                        Object object = in.readObject();
                        if (object instanceof PromptResponse response) {
                            responses.put(response);
                        }
                        
                    }
                } catch (EOFException eof) {
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();
        }

        private void send(Object object) {
            synchronized (out) {
                try {
                    out.writeObject(object);
                    out.flush();
                    out.reset();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        private PromptResponse takeResponse() {
            try {
                return responses.take();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
        }
    }
}
