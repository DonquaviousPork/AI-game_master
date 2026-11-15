package gamemaster.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CoverArtTest {

    private static int colours(BufferedImage img) {
        Set<Integer> set = new HashSet<>();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                set.add(img.getRGB(x, y));
            }
        }
        return set.size();
    }

    @Test
    void bothVersionsAreFullOfDetailAndHaveTheRightSize() {
        for (boolean dark : new boolean[] {true, false}) {
            BufferedImage f = new CoverArt(dark).frame(0.5);
            assertEquals(CoverArt.W, f.getWidth());
            assertEquals(CoverArt.H, f.getHeight());
            assertTrue(colours(f) > 60, "cover looks flat, dark=" + dark);
        }
    }

    @Test
    void lightAndDarkLookDifferentAndTheArtMoves() {
        BufferedImage dark = new CoverArt(true).frame(1.0);
        BufferedImage light = new CoverArt(false).frame(1.0);
        assertTrue(dark.getRGB(5, 5) != light.getRGB(5, 5), "sky colours should differ");
        CoverArt art = new CoverArt(true);
        int[] a = art.frame(0.0).getRGB(0, 0, CoverArt.W, CoverArt.H, null, 0, CoverArt.W);
        int[] b = art.frame(1.7).getRGB(0, 0, CoverArt.W, CoverArt.H, null, 0, CoverArt.W);
        assertTrue(java.util.Arrays.hashCode(a) != java.util.Arrays.hashCode(b), "animation should change frames");
    }
}
