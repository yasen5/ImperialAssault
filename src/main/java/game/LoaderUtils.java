package game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.ImageIcon;

public class LoaderUtils {
    public static BufferedImage getImage(String name) {
        try {
            return ImageIO.read(new File("src/game/images/" + name + ".jpg"));
        } catch (IOException ex) {
            try {
                return ImageIO.read(new File("src/game/images/" + name + ".png"));
            } catch (IOException e) {
                throw new java.lang.RuntimeException(
                        "Couldn't read either jpg or png image for " + name, ex);
            }
        }
    }

    public static ImageIcon getImageIcon(String name) {
        return new ImageIcon("src/game/images/" + name + ".jpg");
    }

    public static void playSound(String name) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(new File("src/game/sounds/" + name + ".wav")));
            clip.start();
        } catch (Exception exc) {
            exc.printStackTrace(System.out);
        }
    }
}
