package net;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import util.MyArrayList;
import util.MyHashMap;
import java.util.Enumeration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import game.Game;
import net.structs.MissionOption;
import game.Personnel;
import game.PlayerSeat;
import game.SelectionType;
import visual.UiContext;
import game.Personnel.Directions;
import net.GameDecisionProvider;
import net.NetworkConfig;
import net.structs.ClientMissionSelection;
import net.structs.GameSessionConfig;
import net.structs.JoinRequest;
import net.structs.JoinResponse;
import net.structs.LobbySnapshot;
import net.structs.MatchSnapshot;
import net.structs.PromptResponse;
import net.structs.RemotePromptCancel;
import net.structs.RemotePrompt;
import visual.Screen;

public class GameServer {
  private final int port;
  private final GameSessionConfig config;
  private final MyHashMap<PlayerSeat, ClientConnection> clients = new MyHashMap<>(PlayerSeat.class);
  private final AtomicLong promptIds = new AtomicLong(1);
  private final Object lobbyLock = new Object();
  private volatile Game spectatorGame;
  private volatile Screen spectatorScreen;
  private volatile String hostAddress;

  public GameServer(int port, int rebelPlayers) {
    this.port = port;
    this.config = new GameSessionConfig(rebelPlayers);
  }

  public void run() throws Exception {
    hostAddress = resolveHostAddress();
    startSpectatorDisplay();
    try (ServerSocket serverSocket = new ServerSocket(port, 50,
        InetAddress.getByName(NetworkConfig.SERVER_BIND_ADDRESS))) {
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
        LobbySnapshot lobbySnapshot = config.rebelPlayerCount() == 0 ? null : createLobbySnapshot();
        connection.out.writeObject(
            new JoinResponse(true, "Joined", request.requestedSeat(), config, lobbySnapshot));
        connection.out.flush();
        connection.startReader();
        if (config.rebelPlayerCount() > 0) {
          broadcastLobbyState();
        }
        if (allSeatsFilled()) {
          break;
        }
      }
      if (config.rebelPlayerCount() > 0) {
        waitForAllMissionSelections();
      }
      MissionOption mission = config.rebelPlayerCount() == 0 ? MissionOption.MISSION_ONE
          : getSelectedMission();
      Game game = createGameForMission(mission);
      game.setSnapshotListener(this::broadcastSnapshot);
      SwingUtilities.invokeLater(() -> {
        if (spectatorScreen != null) {
          spectatorScreen.setIncreaseThreatAction(() -> new Thread(game::increaseThreat, "Manual Threat").start());
          spectatorScreen.setNextRoundAction(game::requestAdvanceStatusPhase);
        }
      });
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
      MyArrayList<PlayerSeat> occupiedSeats = new MyArrayList<>(clients.keySet());
      MyHashMap<PlayerSeat, MissionOption> missionSelections = new MyHashMap<>(PlayerSeat.class);
      MissionOption selectedMission = null;
      boolean allMissionSelections = true;
      for (MyHashMap.Entry<PlayerSeat, ClientConnection> entry : clients.entrySet()) {
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
    updateSpectatorLobbySnapshot(snapshot);
    MyArrayList<ClientConnection> connections;
    synchronized (lobbyLock) {
      connections = new MyArrayList<>(clients.values());
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
    Game game = switch (mission) {
      case MISSION_ONE, MISSION_TWO -> new Game(null, config, null, true);
    };
    game.setDecisionProvider(new RemoteDecisionProvider(game));
    return game;
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
    updateSpectatorSnapshot(snapshot);
    MyArrayList<ClientConnection> connections;
    synchronized (lobbyLock) {
      connections = new MyArrayList<>(clients.values());
    }
    for (ClientConnection connection : connections) {
      connection.send(snapshot);
    }
  }

  private void startSpectatorDisplay() throws Exception {
    spectatorGame = new Game(null, config, null, false);
    SwingUtilities.invokeAndWait(() -> {
      spectatorScreen = new Screen(spectatorGame, true, true);
      spectatorGame.setUi(spectatorScreen);
      spectatorScreen.setServerStatusText("Hosting on " + hostAddress + ":" + port + " | read-only spectator");
      JFrame frame = new JFrame("Imperial Assault Server - " + hostAddress + ":" + port);
      UiContext.setFrame(frame);
      frame.add(spectatorScreen);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
      spectatorScreen.updateLobbySnapshot(createLobbySnapshot());
    });
  }

  private void updateSpectatorLobbySnapshot(LobbySnapshot snapshot) {
    Screen screen = spectatorScreen;
    if (screen == null || snapshot == null) {
      return;
    }
    SwingUtilities.invokeLater(() -> {
      if (spectatorScreen != null) {
        spectatorScreen.updateLobbySnapshot(snapshot);
      }
    });
  }

  private void updateSpectatorSnapshot(MatchSnapshot snapshot) {
    if (spectatorGame == null) {
      return;
    }
    SwingUtilities.invokeLater(() -> {
      if (spectatorScreen != null) {
        spectatorScreen.markGameStarted();
      }
      spectatorGame.loadSnapshot(snapshot);
    });
  }

  private String resolveHostAddress() {
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface networkInterface = interfaces.nextElement();
        if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
          continue;
        }
        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (address instanceof Inet4Address && !address.isLoopbackAddress() && !address.isAnyLocalAddress()) {
            return address.getHostAddress();
          }
        }
      }
      InetAddress localHost = InetAddress.getLocalHost();
      if (localHost != null) {
        return localHost.getHostAddress();
      }
    } catch (Exception ex) {
      // Fall back to localhost below.
    }
    return "127.0.0.1";
  }

  private class RemoteDecisionProvider implements GameDecisionProvider {
    private final Game game;

    private RemoteDecisionProvider(Game game) {
      this.game = game;
    }

    @Override
    public int chooseMultipleChoice(PlayerSeat seat, String name, String explanation, Object[] options) {
      MyArrayList<String> labels = new MyArrayList<>();
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
          name, explanation, MyArrayList.of("No", "Yes"), 0, 1, MyArrayList.of("false", "true"), null, null);
      return Boolean.parseBoolean(requestResponse(prompt));
    }

    @Override
    public int chooseNumericChoice(PlayerSeat seat, String name, int minValue, int maxValue) {
      RemotePrompt prompt = new RemotePrompt(promptIds.getAndIncrement(), seat, RemotePrompt.PromptType.NUMERIC,
          name, name + " (" + minValue + " to " + maxValue + ")", MyArrayList.of(), minValue, maxValue,
          MyArrayList.of(),
          null, null);
      return Integer.parseInt(requestResponse(prompt));
    }

    @Override
    public Directions chooseDirection(PlayerSeat seat, Personnel activeFigure,
        MyArrayList<Directions> allowedDirections) {
      MyArrayList<String> values = new MyArrayList<>();
      for (Directions direction : allowedDirections) {
        values.add(direction.name());
      }
      RemotePrompt prompt = new RemotePrompt(promptIds.getAndIncrement(), seat, RemotePrompt.PromptType.DIRECTION,
          "Movement", "Choose a direction", values, 0, 0, values, activeFigure.getId(), null);
      return Directions.valueOf(requestResponse(prompt));
    }

    @Override
    public Personnel chooseTarget(PlayerSeat seat, SelectionType selectionType, MyArrayList<Personnel> availableTargets) {
      MyArrayList<String> values = new MyArrayList<>();
      MyArrayList<String> labels = new MyArrayList<>();
      for (Personnel target : availableTargets) {
        values.add(target.getId());
        labels.add(target.getName());
      }
      RemotePrompt prompt = new RemotePrompt(promptIds.getAndIncrement(), seat, RemotePrompt.PromptType.TARGET,
          "Target Selection", "Choose a target", labels, 0, 0, values, null, selectionType);
      String response = requestResponse(prompt);
      Personnel target = game.getPersonnelById(response);
      if (target == null) {
        throw new IllegalStateException("Unknown target id " + response);
      }
      return target;
    }

    private String requestResponse(RemotePrompt prompt) {
      ClientConnection connection = clients.get(prompt.seat());
      Thread waitingThread = Thread.currentThread();
      game.setActivePromptCancelAction(() -> {
        connection.send(new RemotePromptCancel(prompt.promptId()));
        waitingThread.interrupt();
      });
      try {
        connection.send(game.createSnapshot());
        connection.send(prompt);
        PromptResponse response;
        do {
          response = connection.takeResponse();
        } while (response.promptId() != prompt.promptId());
        return response.value();
      } finally {
        game.clearActivePromptCancelAction();
      }
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
        throw new CancellationException("Prompt cancelled");
      }
    }
  }
}
