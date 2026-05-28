package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

import game.Constants.WallLine;
import game.Die.GraphicDefenseDieResult;
import game.Die.GraphicOffenseDieResult;
import util.MyArrayList;

final class GameRenderer {
  private final Game game;

  GameRenderer(Game game) {
    this.game = game;
  }

  void drawGame(Graphics g) {
    Rectangle sourceBounds = game.mapTile.sourceBounds();
    g.drawImage(game.mapTile.img(), 0, 0, Constants.tileSize * game.mapTile.tileArray()[0].length,
        Constants.tileSize * game.mapTile.tileArray().length,
        sourceBounds.x, sourceBounds.y, sourceBounds.x + sourceBounds.width,
        sourceBounds.y + sourceBounds.height, null);
    if (game.debugWallLines) {
      drawDebugWallLines(g);
    }
    for (Hero hero : game.heroes) {
      hero.draw(g);
    }
    for (DeploymentGroup<? extends Imperial> deployment : game.imperialDeployments) {
      deployment.draw(g);
    }
    for (MissionTerminal terminal : game.missionTerminals) {
      terminal.draw(g);
    }
    drawThreatHud(g);
    drawDiceSection(g, game.offenseResults, true);
    drawDiceSection(g, game.defenseResults, false);
    for (Interactable<? extends Personnel> interactable : game.interactables) {
      interactable.draw(g);
    }
  }

  int getMapDrawWidth() {
    return Constants.tileSize * game.mapTile.tileArray()[0].length;
  }

  private void drawDebugWallLines(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    Stroke oldStroke = g2.getStroke();
    g2.setColor(Color.RED);
    g2.setStroke(new BasicStroke(3));
    for (WallLine wallLine : Constants.wallLines) {
      drawDebugWallLine(g2, wallLine);
    }
    for (Interactable<? extends Personnel> interactable : game.interactables) {
      if (!interactable.blocking()) {
        continue;
      }
      for (WallLine wallLine : interactable.getWallLines()) {
        drawDebugWallLine(g2, wallLine);
      }
    }
    g2.setStroke(oldStroke);
    g2.dispose();
  }

  private void drawDebugWallLine(Graphics2D g2, WallLine wallLine) {
    Pathfinder.FullPos start = wallLine.startPoint();
    Pathfinder.FullPos end = wallLine.endPoint();
    g2.drawLine((int) Math.round(start.x()), (int) Math.round(start.y()),
        (int) Math.round(end.x()), (int) Math.round(end.y()));
  }

  private <T> void drawDiceSection(Graphics g, MyArrayList<T> dice, boolean offense) {
    if (dice.isEmpty()) {
      return;
    }
    int startX = 960;
    int startY = offense ? 670 : 820;
    int rowSpacing = Die.ySize + 14;
    int columnSpacing = Die.xSize + 10;
    if (game.ui != null) {
      startX = game.ui.getSidebarDiceX();
      startY = offense ? game.ui.getDiceStartY() : game.ui.getDiceStartY() + 150;
    }
    int availableWidth = game.ui != null ? game.ui.getSidebarDetailWidth() : 300;
    int maxPerRow = Math.max(1, (availableWidth + 10) / columnSpacing);
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.setColor(new Color(0, 0, 0, 90));
    int rows = (dice.size() + maxPerRow - 1) / maxPerRow;
    int panelHeight = rows * rowSpacing + 22;
    g2.fillRoundRect(startX - 12, startY - 18, Math.min(availableWidth + 24, maxPerRow * columnSpacing + 14),
        panelHeight, 22, 22);
    g2.setColor(new Color(255, 255, 255, 30));
    g2.drawRoundRect(startX - 12, startY - 18, Math.min(availableWidth + 24, maxPerRow * columnSpacing + 14),
        panelHeight, 22, 22);
    for (int i = 0; i < dice.size(); i++) {
      BufferedImage image = offense ? Die.offenseDieFaces.get((GraphicOffenseDieResult) game.offenseResults.get(i))
          : Die.defenseDieFaces.get((GraphicDefenseDieResult) game.defenseResults.get(i));
      int column = i % maxPerRow;
      int row = i / maxPerRow;
      int x = startX + column * columnSpacing;
      int y = startY + row * rowSpacing;
      g2.drawImage(image, x, y, x + Die.xSize, y + Die.ySize, 0, 0, image.getWidth(null),
          image.getHeight(null), null);
    }
    g2.dispose();
  }

  private void drawThreatHud(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setColor(new Color(0, 0, 0, 180));
    g2.fillRoundRect(20, 82, 300, 102, 18, 18);
    g2.setColor(Color.WHITE);
    g2.drawRoundRect(20, 82, 300, 102, 18, 18);
    g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 20f));
    g2.drawString("Threat Dial: " + game.threatDial, 38, 113);
    g2.setFont(g2.getFont().deriveFont(java.awt.Font.PLAIN, 14f));
    g2.drawString("Threat Level: " + game.threatLevel, 38, 139);
    String roundText = game.roundLimit > 0 ? ("Round: " + game.roundDial + "/" + game.roundLimit)
        : ("Round: " + game.roundDial);
    g2.drawString(roundText, 38, 164);
    g2.dispose();
  }
}
