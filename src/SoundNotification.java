import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundNotification {
    private static final String SOUND_FILE_PATH = "./message-notification.wav";
    private Clip clip;

    SoundNotification() {
        try (AudioInputStream audioStream = AudioSystem
                .getAudioInputStream(new File(SOUND_FILE_PATH).getAbsoluteFile())) {
            this.clip = AudioSystem.getClip();
            this.clip.open(audioStream);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void play() {
        if (clip.isOpen()) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

}
