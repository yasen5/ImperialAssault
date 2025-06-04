package src.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import src.Constants;

public class LoaderUtils {
    public static BufferedImage getImage(String name) {
        String adjustedName = Constants.baseImgFilePath + name;
        try {
            return ImageIO.read(new File(adjustedName + ".jpg"));
        } catch (IOException ex) {
            try {
                return ImageIO.read(new File(adjustedName + ".png"));
            } catch (IOException e) {
                throw new java.lang.RuntimeException("Couldn't read either in jpg or png, tried " + adjustedName + ".jpg" + " and "
                        + adjustedName + ".png" + ex);
            }
        }
    }

    
}
