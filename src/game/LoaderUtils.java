package src.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

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
                throw new java.lang.RuntimeException(
                        "Couldn't read either in jpg or png, tried " + adjustedName + ".jpg" + " and "
                                + adjustedName + ".png" + ex);
            }
        }
    }

    public static void playSound(String name) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem
                    .getAudioInputStream(new File(Constants.baseSoundFilePath + name + ".wav").getAbsoluteFile()));
            clip.start();
        } catch (Exception exc) {
            exc.printStackTrace(System.out);
        }
    }
}
