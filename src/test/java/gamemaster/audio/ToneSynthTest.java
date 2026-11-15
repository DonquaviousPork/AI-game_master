package gamemaster.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ToneSynthTest {

    private static int peak(byte[] pcm) {
        int peak = 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            peak = Math.max(peak, Math.abs((short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8))));
        }
        return peak;
    }

    private static int sample(byte[] pcm, int i) {
        return (short) ((pcm[2 * i] & 0xFF) | (pcm[2 * i + 1] << 8));
    }

    @Test
    void everySoundIsNonSilentAndDoesNotClip() {
        for (byte[] pcm : new byte[][] {ToneSynth.click(), ToneSynth.menuLoop(), ToneSynth.victory(), ToneSynth.defeat()}) {
            assertEquals(0, pcm.length % 2);
            assertTrue(peak(pcm) > 1000, "sound is too quiet");
            assertTrue(peak(pcm) <= 32767 * 0.9 + 1, "sound clips");
        }
    }

    @Test
    void lengthsMatchWhatWasDesigned() {
        assertEquals(16 * (int) ToneSynth.RATE, ToneSynth.menuLoop().length / 2);
        assertTrue(ToneSynth.click().length / 2 < ToneSynth.RATE * 0.1);
        assertTrue(ToneSynth.victory().length / 2 > ToneSynth.RATE * 2);
        assertTrue(ToneSynth.defeat().length / 2 > ToneSynth.RATE * 3);
    }

    @Test
    void theMusicLoopStartsAndEndsNearSilenceSoItRepeatsWithoutAClick() {
        byte[] loop = ToneSynth.menuLoop();
        int n = loop.length / 2;
        assertTrue(Math.abs(sample(loop, n - 1)) < 1500, "loop end is " + sample(loop, n - 1));
        assertTrue(Math.abs(sample(loop, 0)) < 1500, "loop start is " + sample(loop, 0));
    }

    @Test
    void aNoteOneOctaveUpDoublesTheFrequency() {
        assertTrue(Math.abs(440.0 - ToneSynth.freq(0)) < 1e-9);
        assertTrue(Math.abs(880.0 - ToneSynth.freq(12)) < 1e-9);
        assertTrue(Math.abs(220.0 - ToneSynth.freq(-12)) < 1e-9);
    }

    @Test
    void soundCallsAreSafeWithoutAnAudioDevice() {
        Sound s = Sound.get();
        boolean before = s.isMuted();
        s.setMuted(true);          // keeps this test silent on a developer machine
        s.click();
        s.startMenuMusic();
        s.stopMusic();
        s.victory();
        s.defeat();
        s.setMuted(before);
        assertTrue(s.musicVolume() >= 0 && s.musicVolume() <= 100);
    }
}
