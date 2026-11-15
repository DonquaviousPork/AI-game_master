package gamemaster.audio;

/**
 * Builds every sound the game plays as raw 16-bit mono PCM, so the project needs no audio files.
 * Sounds are sums of simple tones with a volume envelope; the music is a looping minor-key arpeggio.
 */
public final class ToneSynth {

    /** Samples per second of every sound produced here. */
    public static final float RATE = 22_050f;

    private ToneSynth() {}

    /** Frequency of the note {@code semitones} above (or below, when negative) A4 = 440 Hz. */
    static double freq(int semitones) {
        return 440.0 * Math.pow(2, semitones / 12.0);
    }

    /** A short soft blip for button presses (about 60 ms). */
    public static byte[] click() {
        double[] b = new double[(int) (RATE * 0.06)];
        addNote(b, 0, freq(19), 0.06, 0.55, 0.002, 18);
        addNote(b, 0, freq(7), 0.04, 0.25, 0.002, 30);
        return toPcm(b);
    }

    /** Sixteen seconds of calm A-minor arpeggios (Am, F, C, G) that loop without a break. */
    public static byte[] menuLoop() {
        double chordSeconds = 4.0;
        double[] b = new double[(int) (RATE * chordSeconds * 4)];
        // chord roots and the three chord tones, as semitones from A4
        int[][] chords = {{-12, 0, 3, 7}, {-16, -4, 0, 5}, {-21, -9, -5, -2}, {-14, -2, 2, 7}};
        for (int c = 0; c < chords.length; c++) {
            double start = c * chordSeconds;
            int[] ch = chords[c];
            addNote(b, (int) (start * RATE), freq(ch[0] - 12), chordSeconds - 0.05, 0.20, 0.3, 0.6); // soft pad
            int[] pattern = {1, 2, 3, 2, 1, 2, 3, 2, 1, 2, 3, 2, 3, 2, 1, 2};
            for (int i = 0; i < pattern.length; i++) {
                double t = start + i * 0.25;
                addNote(b, (int) (t * RATE), freq(ch[pattern[i]]), 0.45, 0.22, 0.01, 6);
            }
        }
        return toPcm(b);
    }

    /** A bright rising fanfare (about 3 seconds). */
    public static byte[] victory() {
        double[] b = new double[(int) (RATE * 3.2)];
        int[] notes = {-9, -5, -2, 3, -2, 3, 7};
        double[] at = {0.0, 0.25, 0.5, 0.8, 1.2, 1.45, 1.8};
        double[] len = {0.3, 0.3, 0.3, 0.4, 0.3, 0.3, 1.3};
        for (int i = 0; i < notes.length; i++) {
            addNote(b, (int) (at[i] * RATE), freq(notes[i]), len[i], 0.45, 0.01, 2.5);
            addNote(b, (int) (at[i] * RATE), freq(notes[i] - 12), len[i], 0.2, 0.01, 2.5);
        }
        return toPcm(b);
    }

    /** A slow falling minor phrase (about 4 seconds). */
    public static byte[] defeat() {
        double[] b = new double[(int) (RATE * 4.2)];
        int[] notes = {0, -2, -5, -9, -12};
        double[] at = {0.0, 0.6, 1.2, 1.9, 2.7};
        double[] len = {0.7, 0.7, 0.8, 0.9, 1.5};
        for (int i = 0; i < notes.length; i++) {
            addNote(b, (int) (at[i] * RATE), freq(notes[i] - 12), len[i], 0.5, 0.03, 1.8);
            addNote(b, (int) (at[i] * RATE), freq(notes[i] - 24), len[i], 0.25, 0.03, 1.8);
        }
        return toPcm(b);
    }

    /**
     * Adds one note to {@code buf} starting at sample {@code start}: a sine with a little second harmonic,
     * a linear attack of {@code attack} seconds and an exponential decay of rate {@code decay} per second.
     */
    static void addNote(double[] buf, int start, double freq, double seconds, double amp, double attack, double decay) {
        int n = (int) (seconds * RATE);
        for (int i = 0; i < n && start + i < buf.length; i++) {
            double t = i / (double) RATE;
            double env = Math.min(1.0, t / attack) * Math.exp(-decay * t);
            // fade out at the very end of the note so nothing ends on a click
            double tail = Math.min(1.0, (n - i) / (RATE * 0.01));
            double wave = Math.sin(2 * Math.PI * freq * t) + 0.25 * Math.sin(4 * Math.PI * freq * t);
            buf[start + i] += amp * env * tail * wave;
        }
    }

    /** Scales the mix down if it would clip, then converts to little-endian 16-bit PCM. */
    static byte[] toPcm(double[] mix) {
        double peak = 0;
        for (double v : mix) {
            peak = Math.max(peak, Math.abs(v));
        }
        double scale = peak > 0.9 ? 0.9 / peak : 1.0;
        byte[] out = new byte[mix.length * 2];
        for (int i = 0; i < mix.length; i++) {
            int s = (int) Math.round(mix[i] * scale * 32767);
            out[2 * i] = (byte) (s & 0xFF);
            out[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }
}
