package gamemaster.audio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/**
 * Plays the game's sounds: button clicks, looping menu music and the victory and defeat jingles.
 * Volumes and mute are remembered between runs. Every call is safe on a machine with no sound
 * device: it just does nothing. Playback runs on one background thread so the window never waits.
 */
public final class Sound {

    private static final Sound INSTANCE = new Sound();

    public static Sound get() {
        return INSTANCE;
    }

    private static final AudioFormat FORMAT = new AudioFormat(ToneSynth.RATE, 16, 1, true, false);

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sound");
        t.setDaemon(true);
        return t;
    });
    private final Preferences prefs = Preferences.userNodeForPackage(Sound.class);

    private volatile int music = clamp(read("music", 60));
    private volatile int effects = clamp(read("effects", 80));
    private volatile boolean muted = readMuted();

    private volatile boolean menuWanted;
    private Clip menuClip;       // used only on the worker thread
    private Clip jingle;         // used only on the worker thread
    private Clip clickClip;      // used only on the worker thread
    private boolean broken;      // no usable sound device

    private Sound() {}

    // ---- settings ----

    public int musicVolume() { return music; }
    public int effectsVolume() { return effects; }
    public boolean isMuted() { return muted; }

    public void setMusicVolume(int percent) {
        music = clamp(percent);
        save();
        worker.execute(this::applyVolumes);
    }

    public void setEffectsVolume(int percent) {
        effects = clamp(percent);
        save();
        worker.execute(this::applyVolumes);
    }

    public void setMuted(boolean on) {
        muted = on;
        save();
        worker.execute(this::applyVolumes);
    }

    // ---- playing ----

    /** A short click; called for every button press. */
    public void click() {
        if (muted || effects == 0) {
            return;
        }
        worker.execute(() -> {
            try {
                if (clickClip == null) {
                    clickClip = open(ToneSynth.click());
                }
                if (clickClip != null) {
                    clickClip.stop();
                    clickClip.setFramePosition(0);
                    setGain(clickClip, effects);
                    clickClip.start();
                }
            } catch (RuntimeException e) {
                broken = true;
            }
        });
    }

    /** Starts the looping menu music (does nothing if it is already playing). */
    public void startMenuMusic() {
        menuWanted = true;
        worker.execute(() -> {
            try {
                stopJingle();
                if (!menuWanted || broken) {
                    return;
                }
                if (menuClip == null) {
                    menuClip = open(ToneSynth.menuLoop());
                }
                if (menuClip != null && !menuClip.isRunning()) {
                    setGain(menuClip, music);
                    menuClip.setFramePosition(0);
                    menuClip.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } catch (RuntimeException e) {
                broken = true;
            }
        });
    }

    public void stopMusic() {
        menuWanted = false;
        worker.execute(() -> {
            if (menuClip != null) {
                menuClip.stop();
            }
        });
    }

    public void victory() {
        playJingle(true);
    }

    public void defeat() {
        playJingle(false);
    }

    private void playJingle(boolean win) {
        menuWanted = false;
        worker.execute(() -> {
            try {
                if (menuClip != null) {
                    menuClip.stop();
                }
                stopJingle();
                jingle = open(win ? ToneSynth.victory() : ToneSynth.defeat());
                if (jingle != null) {
                    setGain(jingle, music);
                    jingle.start();
                }
            } catch (RuntimeException e) {
                broken = true;
            }
        });
    }

    // ---- internals (worker thread) ----

    private void stopJingle() {
        if (jingle != null) {
            jingle.stop();
            jingle.close();
            jingle = null;
        }
    }

    private Clip open(byte[] pcm) {
        if (broken) {
            return null;
        }
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(FORMAT, pcm, 0, pcm.length);
            return clip;
        } catch (Exception e) {
            broken = true;      // no device, or no permission: stay silent
            return null;
        }
    }

    private void applyVolumes() {
        if (menuClip != null) {
            setGain(menuClip, music);
        }
        if (jingle != null) {
            setGain(jingle, music);
        }
        if (clickClip != null) {
            setGain(clickClip, effects);
        }
    }

    private void setGain(Clip clip, int percent) {
        try {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float db = muted || percent <= 0 ? gain.getMinimum() : (float) (20 * Math.log10(percent / 100.0));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
        } catch (RuntimeException ignored) {
            // some mixers have no gain control; the sound just plays at full volume
        }
    }

    // ---- remembered settings ----

    private int read(String key, int fallback) {
        try {
            return prefs.getInt(key, fallback);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private boolean readMuted() {
        try {
            return prefs.getBoolean("muted", false);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void save() {
        try {
            prefs.putInt("music", music);
            prefs.putInt("effects", effects);
            prefs.putBoolean("muted", muted);
        } catch (RuntimeException ignored) {
            // remembering the choice is a convenience only
        }
    }

    private static int clamp(int percent) {
        return Math.max(0, Math.min(100, percent));
    }
}
