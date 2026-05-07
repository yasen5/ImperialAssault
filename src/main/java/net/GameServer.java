package net;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import game.Screen.SelectingType;
import game.Game;
import game.GameDecisionProvider;
import game.GameSessionConfig;
import game.MissionOption;
import game.Personnel;
import game.PlayerSeat;
import game.Personnel.Directions;
import net.LobbySnapshot;

public class GameServer {
    private final int port;
    private final GameSessionConfig config;
    private final EnumMap<PlayerSeat, ClientConnection> clients = new EnumMap<>(PlayerSeat.class);
    private final AtomicLong promptIds = new AtomicLong(1);
    private final Object lobbyLock = new Object();

    public GameServer(int port, int rebelPlayers) {
        this.port = port;
        this.config = new GameSessionConfig(rebelPlayers);
    }

    public void run() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                ClientConnection connection;
                Socket socket = serverSocket.accept();
                connection = new ClientConnection(socket);
                JoinRequest request = (JoinRequest) connection.in.readObject();
                if (!config.requiredSeats().contains(request.requestedSeat()) || hasClient(request.requestedSeat())) {
                    JoinResponse response = new JoinResponse(false, "Seat unavailable", request.requestedSeat(), config,
                            createLobbySnapshot());
                    connection.out.writeObject(
                            response);
                    connection.out.flush();
                    socket.close();
                    continue;
                }
                synchronized (lobbyLock) {
                    clients.put(request.requestedSeat(), connection);
                    connection.seat = request.requestedSeat();
                    connection.mission = null;
                }
                connection.out.writeObject(
                        new JoinResponse(true, "Joined", request.requestedSeat(), config, createLobbySnapshot()));
                connection.out.flush();
                connection.startReader();
                broadcastLobbyState();
                if (allSeatsFilled()) {
                    break;
                }
            }
            waitForAllMissionSelections();
            Game game = createGameForMission(getSelectedMission());
            game.setSnapshotListener(this::broadcastSnapshot);
            broadcastSnapshot(game.createSnapshot());
            Thread gameThread = new Thread(game::playRound);
            gameThread.start();
            gameThread.join();
        }
    }

    private boolean hasClient(PlayerSeat seat) {
        synchronized (lobbyLock) {
            return clients.containsKey(seat);
        }
    }

    private boolean allSeatsFilled() {
        synchronized (lobbyLock) {
            return clients.size() >= config.requiredSeats().size();
        }
    }

    private void waitForAllMissionSelections() throws InterruptedException {
        synchronized (lobbyLock) {
            while (!allMissionSelectionsLocked()) {
                lobbyLock.wait();
            }
        }
    }

    private boolean allMissionSelectionsLocked() {
        if (clients.size() < config.requiredSeats().size()) {
            return false;
        }
        MissionOption mission = null;
        for (PlayerSeat seat : config.requiredSeats()) {
            ClientConnection connection = clients.get(seat);
            if (connection == null || connection.mission == null) {
                return false;
            }
            if (mission == null) {
                mission = connection.mission;
            } else if (mission != connection.mission) {
                return false;
            }
        }
        return true;
    }

    private LobbySnapshot createLobbySnapshot() {
        synchronized (lobbyLock) {
            ArrayList<PlayerSeat> occupiedSeats = new ArrayList<>(clients.keySet());
            EnumMap<PlayerSeat, MissionOption> missionSelections = new EnumMap<>(PlayerSeat.class);
            MissionOption selectedMission = null;
            boolean allMissionSelections = true;
            for (Map.Entry<PlayerSeat, ClientConnection> entry : clients.entrySet()) {
                MissionOption mission = entry.getValue().mission;
                if (mission != null) {
                    missionSelections.put(entry.getKey(), mission);
                    if (selectedMission == null) {
                        selectedMission = mission;
                    } else if (selectedMission != mission) {
                        allMissionSelections = false;
                    }
                } else {
                    allMissionSelections = false;
                }
            }
            boolean allSeatsFilled = clients.size() >= config.requiredSeats().size();
            boolean allMissionSelectionsMatch = allSeatsFilled && allMissionSelections && allMissionSelectionsLocked();
            if (!allMissionSelectionsMatch) {
                selectedMission = null;
            }
            return new LobbySnapshot(config, occupiedSeats, missionSelections,
                    allSeatsFilled, allMissionSelections && allSeatsFilled, allMissionSelectionsMatch,
                    selectedMission);
        }
    }

    private void broadcastLobbyState() {
        LobbySnapshot snapshot = createLobbySnapshot();
        ArrayList<ClientConnection> connections;
        synchronized (lobbyLock) {
            connections = new ArrayList<>(clients.values());
        }
        for (ClientConnection connection : connections) {
            connection.send(snapshot);
        }
    }

    private void handleClientMissionSelection(ClientConnection connection, ClientMissionSelection missionSelection) {
        synchronized (lobbyLock) {
            connection.mission = missionSelection.mission();
            lobbyLock.notifyAll();
        }
        broadcastLobbyState();
    }

    private Game createGameForMission(MissionOption mission) {
        return switch (mission) {
            case MISSION_ONE, MISSION_TWO -> new Game(null, config, new RemoteDecisionProvider(), true);
        };
    }

    private MissionOption getSelectedMission() {
        synchronized (lobbyLock) {
            for (PlayerSeat seat : config.requiredSeats()) {
                ClientConnection connection = clients.get(seat);
                if (connection != null && connection.mission != null) {
                    return connection.mission;
                }
            }
        }
        throw new IllegalStateException("No mission selected");
    }

    private void broadcastSnapshot(MatchSnapshot snapshot) {
        ArrayList<ClientConnection> connections;
        synchronized (lobbyLock) {
            connections = new ArrayList<>(clients.values());
        }
        for (ClientConnection connection : connections) {
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
                    name, name + " (" + minValue + " to " + maxValue + ")", List.of(), minValue, maxValue, List.of(),
                    null, null);
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

    private class ClientConnection {
        private final Socket socket;
        private final ObjectOutputStream out;
        private final ObjectInputStream in;
        private final BlockingQueue<PromptResponse> responses = new LinkedBlockingQueue<>();
        private PlayerSeat seat;
        private volatile MissionOption mission;

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
                        } else if (object instanceof ClientMissionSelection clientMissionSelection) {
                            handleClientMissionSelection(this, clientMissionSelection);
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
