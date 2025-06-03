package src.game;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import src.Constants;
import src.Constants.WallLine;

public abstract class Interactable<ValidInteractors extends Personnel> {
    private final Pos pos;
    private final int xSize, ySize;
    private BufferedImage image;
    private final Class<ValidInteractors> validInteractorClass;
    private final WallLine[] wallLines;

    public Interactable(Pos pos, Class<ValidInteractors> validInteractorClass, String imgName, int xSize, int ySize, WallLine[] wallLines) {
        this.pos = pos;
        this.validInteractorClass = validInteractorClass;
        try {
            image = ImageIO.read(new File(Constants.baseImgFilePath + imgName));
        } catch (IOException e) {
            System.out.println("No terminal token image");
            System.exit(0);
        }
        this.xSize = xSize;
        this.ySize = ySize;
        this.wallLines = wallLines;
    }

    public Interactable(Pos pos, Class<ValidInteractors> validInteractorClass, String imgName, WallLine[] wallLines) {
        this.pos = pos;
        this.validInteractorClass = validInteractorClass;
        try {
            image = ImageIO.read(new File(Constants.baseImgFilePath + imgName));
        } catch (IOException e) {
            System.out.println("No terminal token image");
            System.exit(0);
        }
        this.xSize = Constants.tileSize;
        this.ySize = Constants.tileSize;
        this.wallLines = wallLines;
    }

    public void draw(Graphics g) {
        g.drawImage(image, pos.getFullX(),
                pos.getFullY(),
                pos.getFullX() + xSize,
                pos.getFullY() + ySize, 0, 0, image.getWidth(null),
                image.getHeight(null),
                null);
    }

    public void interact(Personnel interactor) {
        if (validInteractorClass.isInstance(interactor)) {
            safeInteract(validInteractorClass.cast(interactor));
        }
    }

    public abstract void safeInteract(ValidInteractors safeInteractor);

    public Pos getPos() {
        return pos;
    }

    public boolean canInteract() {
        return true;
    }

    public boolean blocking() {
        return wallLines != null && canInteract();
    }

    public WallLine[] getWallLines() {
        return wallLines;
    }
}
