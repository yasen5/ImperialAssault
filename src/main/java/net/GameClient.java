package net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.structs.MissionOption;
import visual.Screen;
import game.Game;
import game.PlayerSeat;
import visual.UiContext;
import net.structs.LobbySnapshot;
import net.structs.JoinRequest;
import net.structs.JoinResponse;
import net.structs.MatchSnapshot;
import net.structs.RemotePrompt;
import net.structs.PromptResponse;
import net.structs.ClientMissionSelection;

public class GameClient {
  private final String host;
  private final int port;
  private final PlayerSeat requestedSeat;
  private ObjectOutputStream out;
  private Screen screen;
  private Game game;

  public GameClient(String host, int port, PlayerSeat requestedSeat) {
    this.host = host;
    this.port = port;
    this.requestedSeat = requestedSeat;
  }

  public void run() throws Exception {
    Socket socket = new Socket(host, port);
    out = new ObjectOutputStream(socket.getOutputStream());
    out.flush();
    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
    send(new JoinRequest(requestedSeat));
    JoinResponse response = (JoinResponse) in.readObject();
    if (!response.accepted()) {
      throw new IllegalStateException(response.message());
    }
    game = new Game(null, response.config(), null, false);
    SwingUtilities.invokeAndWait(() -> {
      screen = new Screen(game, true);
      game.setUi(screen);
      screen.setLocalSeat(response.seat());
      screen.setMissionSelectionAction((MissionOption mission) -> {
        try {
          send(new ClientMissionSelection(mission));
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      });
      if (response.lobbySnapshot() != null) {
        screen.updateLobbySnapshot(response.lobbySnapshot());
      }
      JFrame frame = new JFrame("Imperial Assault Client - " + response.seat());
      UiContext.setFrame(frame);
      frame.add(screen);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
    });
    while (true) {
      Object message = in.readObject();
      if (message instanceof LobbySnapshot lobbySnapshot) {
        SwingUtilities.invokeLater(() -> {
          screen.updateLobbySnapshot(lobbySnapshot);
          screen.repaint();
        });
        continue;
      }
      if (message instanceof MatchSnapshot snapshot) {
        SwingUtilities.invokeLater(() -> {
          screen.markGameStarted();
          game.loadSnapshot(snapshot);
          screen.repaint();
        });
      } else if (message instanceof RemotePrompt prompt) {
        new Thread(() -> handlePrompt(prompt), "Remote Prompt").start();
      }
    }
  }

  private void handlePrompt(RemotePrompt prompt) {
    try {
      String value = switch (prompt.type()) {
        case MULTIPLE_CHOICE -> String.valueOf(
            screen.promptMultipleChoice(prompt.title(), prompt.message(), prompt.optionLabels().toArray()));
        case YES_NO -> String.valueOf(screen.promptYesNo(prompt.title(), prompt.message()));
        case NUMERIC -> String.valueOf(
            screen.promptNumericChoice(prompt.title(), prompt.message(), prompt.minValue(),
                prompt.maxValue()));
        case DIRECTION, TARGET -> {
          CompletableFuture<String> selection;
          if (screen == null) {
            throw new IllegalStateException("Screen not initialized");
          }
          final CompletableFuture<String>[] selectionRef = new CompletableFuture[1];
          SwingUtilities.invokeAndWait(() -> selectionRef[0] = screen.beginRemoteBoardPrompt(prompt));
          selection = selectionRef[0];
          String result = selection.join();
          SwingUtilities.invokeLater(() -> screen.clearRemotePrompt());
          yield result;
        }
      };
      send(new PromptResponse(prompt.promptId(), value));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private void send(Object object) throws Exception {
    synchronized (out) {
      out.writeObject(object);
      out.flush();
      out.reset();
    }
  }
}
