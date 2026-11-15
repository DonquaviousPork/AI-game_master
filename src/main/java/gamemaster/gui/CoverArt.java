package gamemaster.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * The cover picture, drawn in code as pixel art on a 400 by 225 grid (the window scales it up): a battlefield with
 * a hero party (fighter, rogue, wizard) against a gang of monsters, a castle on the ridge, and sparks, embers and
 * magic animated on top. The dark version is a dusk under a huge moon; the light version is a vivid sunny day.
 * The characters reuse the same sprites as the in-game cards. Everything is drawn from rectangles, lines and
 * circles, so there are no image files.
 */
final class CoverArt {

    static final int W = 400, H = 225;
    private static final int CLASH_X = 188, CLASH_Y = 156;

    private static final int OUTLINE = 0xFF0A0614;
    private static final int[][] BAYER = {{0, 2}, {3, 1}};

    private final boolean dark;
    private final BufferedImage base = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    private final BufferedImage frame = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    private final int[][] stars = new int[80][4];
    private final double[][] embers = new double[46][4];
    private int orbX, orbY;     // the wizard's staff orb, for the animated magic bolt

    CoverArt(boolean dark) {
        this.dark = dark;
        Random rnd = new Random(11);
        for (int[] s : stars) {
            s[0] = rnd.nextInt(W);
            s[1] = rnd.nextInt(112);
            s[2] = rnd.nextInt(100);      // twinkle phase
            s[3] = 1 + rnd.nextInt(3);    // brightness class
        }
        for (double[] e : embers) {
            e[0] = rnd.nextDouble() * W;
            e[1] = rnd.nextDouble() * H;
            e[2] = 6 + rnd.nextDouble() * 14;      // rise speed
            e[3] = rnd.nextDouble() * 6.28;        // sway phase
        }
        paintBase();
    }

    // ---------------------------------------------------------------- the still picture

    private void paintBase() {
        sky();
        if (dark) {
            moon();
        } else {
            sun();
            clouds();
        }
        glow(CLASH_X + 20, CLASH_Y - 10, 150, dark ? 0xF2692F : 0xFFE9A0, dark ? 0.65 : 0.55);
        ridge(dark ? 0x3B1B5A : 0x6FB4E6, 138, 11, 0.021, 1.3, 0.057, 0.7);
        castle();
        ridge(dark ? 0x26113F : 0x35B36F, 152, 9, 0.017, 2.1, 0.049, 0.4);
        ridge(dark ? 0x170A2B : 0x1F8A45, 164, 5, 0.03, 0.2, 0.08, 1.1);
        ground();
        BufferedImage fg = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        propsBack(fg);
        battle(fg);
        outline(fg);
        composite(fg);
        propsFront();
        title();
    }

    private void sky() {
        int[] c = dark
                ? new int[] {0x07041A, 0x0E0A2C, 0x1A0F40, 0x2D1255, 0x4A1760, 0x741F62, 0xA52E5E, 0xD4505A, 0xF0814D}
                : new int[] {0x1C6FE0, 0x2E8AF0, 0x4DA6F6, 0x78C3FA, 0xA6DCFB, 0xD2F0FB, 0xFFF3B8, 0xFFD36E, 0xFFB04F};
        int horizon = 160;
        for (int y = 0; y < H; y++) {
            double t = Math.pow(Math.min(1.0, y / (double) horizon), dark ? 1.5 : 1.2) * (c.length - 1);
            int i = Math.min(c.length - 2, (int) t);
            double frac = t - i;
            for (int x = 0; x < W; x++) {
                double thr = (BAYER[y & 1][x & 1] + 0.5) / 4.0;
                base.setRGB(x, y, frac > thr ? c[i + 1] : c[i]);
            }
        }
    }

    private void moon() {
        int cx = 330, cy = 74;
        glow(cx, cy, 60, 0xBFA8FF, 0.35);
        disc(base, cx, cy, 24, 0xEDE3C4);
        disc(base, cx + 3, cy + 2, 21, 0xF7F0D8);
        disc(base, cx - 9, cy - 6, 4, 0xD2C4A0);
        disc(base, cx + 8, cy + 8, 5, 0xD2C4A0);
        disc(base, cx - 4, cy + 12, 3, 0xD9CCA8);
        disc(base, cx + 12, cy - 9, 2, 0xD9CCA8);
    }

    private void sun() {
        int cx = 330, cy = 72;
        glow(cx, cy, 75, 0xFFF4A8, 0.7);
        disc(base, cx, cy, 25, 0xFFC93C);
        disc(base, cx, cy, 21, 0xFFE066);
        disc(base, cx - 3, cy - 3, 14, 0xFFF3A0);
        for (int i = 0; i < 16; i++) {                              // rays
            double a = i * Math.PI / 8;
            for (int d = 29; d < 38 + (i % 2) * 7; d++) {
                int x = cx + (int) Math.round(Math.cos(a) * d), y = cy + (int) Math.round(Math.sin(a) * d);
                if (x >= 0 && y >= 0 && x < W && y < H) {
                    base.setRGB(x, y, 0xFFE066);
                }
            }
        }
    }

    private void clouds() {
        int[][] spec = {{46, 78, 1}, {132, 58, 0}, {262, 104, 1}, {372, 118, 0}, {18, 128, 0}, {210, 36, 0}};
        for (int[] c : spec) {
            int x = c[0], y = c[1];
            int r = c[2] == 1 ? 11 : 8;
            disc(base, x, y, r, 0xFFFFFF);
            disc(base, x + r, y + 2, r - 2, 0xFFFFFF);
            disc(base, x - r, y + 3, r - 3, 0xFFFFFF);
            disc(base, x + r * 2, y + 4, r - 4, 0xFFFFFF);
            box(x - r - 6, y + 4, r * 4 + 6, r - 2, 0xFFFFFF);
            box(x - r - 6, y + r + 1, r * 4 + 6, 2, 0xD5E8FA);   // soft shadow underneath
        }
    }

    /** A soft glow: colour blended in steps from the centre out, dithered so it keeps the pixel look. */
    private void glow(int cx, int cy, int radius, int rgb, double strength) {
        for (int y = Math.max(0, cy - radius); y < Math.min(H, cy + radius); y++) {
            for (int x = Math.max(0, cx - radius); x < Math.min(W, cx + radius); x++) {
                double d = Math.hypot((x - cx) * 0.8, y - cy) / radius;
                if (d >= 1) {
                    continue;
                }
                double f = Math.pow(1 - d, 2.2) * strength * 6;
                int level = (int) f;
                double thr = (BAYER[y & 1][x & 1] + 0.5) / 4.0;
                if (f - level > thr) {
                    level++;
                }
                if (level > 0) {
                    base.setRGB(x, y, mix(base.getRGB(x, y), rgb, level / 6.0));
                }
            }
        }
    }

    private void ridge(int rgb, int baseY, double a1, double f1, double p1, double f2, double p2) {
        for (int x = 0; x < W; x++) {
            int y = (int) Math.round(baseY - a1 * Math.sin(x * f1 + p1) - a1 * 0.5 * Math.sin(x * f2 * 3.1 + p2)
                    - a1 * 0.3 * Math.sin(x * 0.011 + 4));
            int lit = dark ? mix(rgb, 0xC2476A, 0.18) : mix(rgb, 0xFFFFFF, 0.3);
            for (int yy = Math.max(0, y); yy < H; yy++) {
                base.setRGB(x, yy, yy == y ? lit : rgb);
            }
        }
    }

    private void castle() {
        int c = dark ? 0x1B0D31 : 0x8C9BD6;
        int roof = dark ? c : 0xE04848;
        int x0 = 36, y0 = 134;
        // the walls run down behind the front hill, so the castle sits on the ridge instead of floating above it
        box(x0, y0 - 18, 44, 46, c);
        box(x0 - 6, y0 - 34, 12, 62, c);
        box(x0 + 40, y0 - 40, 12, 68, c);
        box(x0 + 18, y0 - 28, 10, 40, c);
        spire(x0, y0 - 34, 8, 14, roof);
        spire(x0 + 46, y0 - 40, 8, 16, roof);
        spire(x0 + 23, y0 - 28, 6, 12, roof);
        if (!dark) {
            box(x0, y0 - 18, 44, 2, 0xB7C3EE);
            box(x0 - 6, y0 - 34, 3, 62, 0xB7C3EE);
            box(x0 + 40, y0 - 40, 3, 68, 0xB7C3EE);
        }
        for (int[] w : new int[][] {{x0 - 1, y0 - 26}, {x0 + 45, y0 - 32}, {x0 + 21, y0 - 20}, {x0 + 10, y0 - 10},
                {x0 + 32, y0 - 10}, {x0 - 1, y0 - 12}}) {
            box(w[0], w[1], 2, 3, dark ? 0xFFCF5A : 0x3B4C8F);
        }
        box(x0 + 21, y0 - 7, 4, 9, dark ? 0x0F0720 : 0x4A3320);   // gate
    }

    private void spire(int cx, int top, int halfWidth, int height, int rgb) {
        for (int i = 0; i < height; i++) {
            int half = (int) Math.round(halfWidth * (i / (double) height));
            box(cx - half, top - height + i, half * 2 + 1, 1, rgb);
        }
    }

    private void ground() {
        Random r = new Random(5);
        for (int y = 168; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int rgb;
                if (dark) {
                    rgb = y < 171 ? 0x4A1D46 : y < 190 ? 0x0F0819 : 0x08040F;
                } else {
                    rgb = y < 170 ? 0xA6EA55 : y < 190 ? 0x3FB04A : 0x2A8F3C;
                }
                base.setRGB(x, y, rgb);
            }
        }
        for (int i = 0; i < 160; i++) {
            int x = r.nextInt(W), y = 172 + r.nextInt(50);
            base.setRGB(x, y, dark ? mix(base.getRGB(x, y), 0x6B3A63, 0.6) : mix(base.getRGB(x, y), 0xB6F26A, 0.6));
        }
        for (int i = 0; i < 18; i++) {                 // pebbles
            int x = r.nextInt(W), y = 178 + r.nextInt(40);
            box(x, y, 3, 2, dark ? 0x1D1030 : 0x8D7B66);
            box(x, y, 3, 1, dark ? 0x34204D : 0xC9B79C);
        }
        if (!dark) {                                    // flowers
            int[] petals = {0xFF6FA5, 0xFFE14D, 0xFFFFFF, 0xB05CFF};
            for (int i = 0; i < 46; i++) {
                int x = r.nextInt(W), y = 172 + r.nextInt(42);
                int p = petals[r.nextInt(petals.length)];
                base.setRGB(x, y, p);
                base.setRGB(x + 1, y, p);
                base.setRGB(x, y - 1, 0x2A8F3C);
            }
        }
    }

    private void propsBack(BufferedImage fg) {
        banner(fg, 24, 112, dark ? 0xB83A47 : 0xE8454E);
        banner(fg, 376, 106, dark ? 0x7A3FC2 : 0x3F7BE0);
    }

    private void propsFront() {
        int band = dark ? 0x05030A : 0x145A2A;
        box(0, 214, W, 11, band);
        Random r = new Random(9);
        for (int i = 0; i < 26; i++) {                 // grass tufts and rubble on the near edge
            int x = r.nextInt(W);
            int h = 2 + r.nextInt(5);
            box(x, 214 - h, 1, h, band);
            box(x + 1, 214 - h / 2, 1, h / 2, band);
        }
    }

    private void banner(BufferedImage fg, int x, int top, int rgb) {
        box(fg, x, top, 3, 80, 0xFF2E1D14);
        for (int i = 0; i < 26; i++) {
            int wave = (int) Math.round(Math.sin(i * 0.5) * 2);
            int len = 14 - i / 3;
            box(fg, x + 3 + wave, top + 4 + i, len, 1, 0xFF000000 | mix(rgb, 0x000000, i / 60.0));
        }
        box(fg, x - 1, top - 3, 5, 3, 0xFFC99A2E);
    }

    private void sword(BufferedImage fg, int x, int y, double lean) {
        for (int i = 0; i < 26; i++) {
            box(fg, x + (int) (-i * lean), y - i, 2, 1, 0xFF8A93A6);
        }
        box(fg, x - 5 - (int) (26 * lean), y - 28, 12, 2, 0xFFC99A2E);
        box(fg, x - (int) (26 * lean), y - 34, 2, 6, 0xFF5B3A22);
    }

    // ---------------------------------------------------------------- the battle

    /** Draws the party and the gang: far rows first so nearer fighters overlap them. */
    private void battle(BufferedImage fg) {
        // spears of the gang behind the enemy lines
        for (int x : new int[] {292, 312, 340, 366}) {
            box(fg, x, 124 + (x % 9), 2, 56, 0xFF4B2E1C);
            polygon(fg, new int[] {x - 2, x + 1, x + 4}, new int[] {124 + (x % 9), 114 + (x % 9), 124 + (x % 9)}, 0xFFC9D1D9);
        }
        // back rows (small)
        creature(fg, Sprites.BEAST, "Fenrir", 252, 178, 2, false);
        creature(fg, Sprites.ROBED, "Mordo", 300, 176, 2, true);
        creature(fg, Sprites.UNDEAD, "Bonny", 326, 180, 2, true);
        creature(fg, Sprites.SOLDIER, "Gruk", 352, 176, 2, true);
        creature(fg, Sprites.BEAST, "Ogre", 374, 182, 4, false);      // the boss
        creature(fg, Sprites.WIZARD, "Mira", 60, 180, 3, false);      // wizard behind the front line
        // front rows
        creature(fg, Sprites.UNDEAD, "Rattle", 270, 186, 3, true);
        creature(fg, Sprites.SOLDIER, "Skarn", 232, 190, 3, true);
        creature(fg, Sprites.BEAST, "Fang", 212, 200, 2, false);
        creature(fg, Sprites.FIGHTER, "Brenna", 118, 190, 3, true);
        creature(fg, Sprites.ROGUE, "Kit", 158, 198, 3, false);
        // weapons: the fighter's blade and the soldier's axe meet at the clash point
        int[] hand = handOf(118, 190, 3, 11, 7);
        thickLine(fg, hand[0], hand[1], CLASH_X + 6, CLASH_Y - 3, 3, 0xFFEAF2FF);
        thickLine(fg, hand[0], hand[1] + 2, CLASH_X + 6, CLASH_Y - 1, 1, 0xFF9DB4D8);
        int[] foe = handOf(232, 190, 3, 0, 7);
        thickLine(fg, foe[0], foe[1], CLASH_X - 4, CLASH_Y - 10, 3, 0xFF8B6840);
        polygon(fg, new int[] {CLASH_X - 12, CLASH_X - 3, CLASH_X - 2, CLASH_X - 9},
                new int[] {CLASH_Y - 18, CLASH_Y - 20, CLASH_Y - 2, CLASH_Y - 2}, 0xFFB7C0D1);
        box(fg, CLASH_X - 13, CLASH_Y - 17, 2, 14, 0xFFFF5A3C);
        // the wizard's staff orb
        int[] orb = handOf(60, 180, 3, 11, 0);
        orbX = orb[0];
        orbY = orb[1] - 2;
        disc(fg, orbX, orbY, 3, 0xFF7CFFB0);
        disc(fg, orbX - 1, orbY - 1, 1, 0xFFFFFFFF);
        // the rogue's dagger
        int[] dagger = handOf(158, 198, 3, 11, 6);
        thickLine(fg, dagger[0], dagger[1], dagger[0] + 16, dagger[1] - 9, 2, 0xFFEAF2FF);
    }

    /** Pixel position of grid cell (col, row) of a sprite whose feet are at (cx, feetY). */
    private static int[] handOf(int cx, int feetY, int scale, int col, int row) {
        return new int[] {cx - Sprites.WIDTH * scale / 2 + col * scale + scale / 2,
                feetY - Sprites.HEIGHT * scale + row * scale + scale / 2};
    }

    private void creature(BufferedImage fg, String look, String seed, int cx, int feetY, int scale, boolean flip) {
        String[] rows = Sprites.grid(look);
        int x0 = cx - Sprites.WIDTH * scale / 2;
        int y0 = feetY - Sprites.HEIGHT * scale;
        shadowEllipse(cx, feetY, Sprites.WIDTH * scale / 2 + 2, Math.max(2, scale));
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                Color c = Sprites.colour(rows[y].charAt(x), look, seed, true);
                if (c != null) {
                    int px = flip ? Sprites.WIDTH - 1 - x : x;
                    box(fg, x0 + px * scale, y0 + y * scale, scale, scale, 0xFF000000 | c.getRGB());
                }
            }
        }
    }

    private void shadowEllipse(int cx, int cy, int rx, int ry) {
        for (int y = -ry; y <= ry; y++) {
            for (int x = -rx; x <= rx; x++) {
                if (x * x / (double) (rx * rx) + y * y / (double) (ry * ry) <= 1) {
                    int px = cx + x, py = cy + y;
                    if (px >= 0 && py >= 0 && px < W && py < H) {
                        base.setRGB(px, py, mix(base.getRGB(px, py), 0x000000, dark ? 0.7 : 0.4));
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- the title

    private void title() {
        Color top = new Color(0xFFF3B0), bottom = new Color(0xF0912E);
        int edge = dark ? 0x0A0614 : 0x0B1B3A;
        int drop = dark ? 0x1A0830 : 0x14305F;
        String text = "AI GAME MASTER";
        int scale = 3;
        int x = (W - PixelFont.width(text, scale)) / 2;
        int y = 12;
        BufferedImage tl = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        PixelFont.draw(tl, text, x, y, scale, top, bottom);
        BufferedImage mask = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for (int yy = 0; yy < H; yy++) {
            for (int xx = 0; xx < W; xx++) {
                if (tl.getRGB(xx, yy) >>> 24 != 0) {
                    stampSolid(mask, xx, yy, drop, 2, 3);
                }
            }
        }
        for (int yy = 0; yy < H; yy++) {
            for (int xx = 0; xx < W; xx++) {
                if (tl.getRGB(xx, yy) >>> 24 != 0) {
                    stampSolid(mask, xx, yy, edge, 1, 1, 0);
                }
            }
        }
        composite(mask);
        composite(tl);
        String sub = "AN AI-POWERED TEXT ADVENTURE";
        int sx = (W - PixelFont.width(sub, 1)) / 2;
        BufferedImage st = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        PixelFont.draw(st, sub, sx, 41, 1, dark ? new Color(0xE6DAFF) : new Color(0xFFFFFF),
                dark ? new Color(0xB79CF2) : new Color(0xDDEBFF));
        BufferedImage sm = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for (int yy = 0; yy < H; yy++) {
            for (int xx = 0; xx < W; xx++) {
                if (st.getRGB(xx, yy) >>> 24 != 0) {
                    stampSolid(sm, xx, yy, edge, 1, 1, 0);
                }
            }
        }
        composite(sm);
        composite(st);
        int ly = 44;                                           // ornament either side of the subtitle
        int a = dark ? 0xB79CF2 : 0xFFFFFF, b = dark ? 0x5E3FA8 : 0x1B4FB0;
        for (int i = 0; i < 34; i++) {
            base.setRGB(sx - 8 - i, ly, i % 2 == 0 ? a : b);
            base.setRGB(sx + PixelFont.width(sub, 1) + 8 + i, ly, i % 2 == 0 ? a : b);
        }
        box(sx - 12, ly - 1, 3, 3, 0xFFD35E);
        box(sx + PixelFont.width(sub, 1) + 8 - 4 + 1, ly - 1, 3, 3, 0xFFD35E);
    }

    private static void stampSolid(BufferedImage layer, int x, int y, int rgb, int dx, int dy) {
        stampSolid(layer, x, y, rgb, dx, dy, dx);
    }

    /** Stamps a solid pixel at (x+dx, y+dy); with a third offset it stamps a 1-pixel ring (outline) instead. */
    private static void stampSolid(BufferedImage layer, int x, int y, int rgb, int dx, int dy, int ring) {
        if (ring == 0 && dx == 1 && dy == 1) {
            for (int j = -1; j <= 1; j++) {
                for (int i = -1; i <= 1; i++) {
                    put(layer, x + i, y + j, 0xFF000000 | rgb);
                }
            }
        } else {
            put(layer, x + dx, y + dy, 0xFF000000 | rgb);
        }
    }

    private static void put(BufferedImage img, int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) {
            img.setRGB(x, y, argb);
        }
    }

    // ---------------------------------------------------------------- the moving parts

    /** The picture at time {@code t} seconds: still scene plus ambient motion, the clash sparks and the wizard's bolt. */
    BufferedImage frame(double t) {
        Graphics2D g = frame.createGraphics();
        g.drawImage(base, 0, 0, null);
        g.dispose();
        if (dark) {
            for (int[] s : stars) {
                double tw = 0.5 + 0.5 * Math.sin(t * 2.2 + s[2]);
                int rgb = tw > 0.85 ? 0xFFFFFF : tw > 0.45 ? 0xBFB6F0 : 0x6B5FA8;
                if (s[3] == 3 && tw > 0.7) {
                    plot(s[0] - 1, s[1], 0x8F86D0);
                    plot(s[0] + 1, s[1], 0x8F86D0);
                    plot(s[0], s[1] - 1, 0x8F86D0);
                    plot(s[0], s[1] + 1, 0x8F86D0);
                }
                plot(s[0], s[1], rgb);
            }
        } else {
            for (int i = 0; i < 3; i++) {                      // birds drifting across the sky
                int bx = (int) ((t * (9 + i * 3) + i * 140) % (W + 40)) - 20;
                int by = 70 + i * 17 + (int) (Math.sin(t * 2 + i) * 3);
                boolean up = ((int) (t * 4 + i)) % 2 == 0;
                plot(bx - 2, by + (up ? 0 : 1), 0x1B2A55);
                plot(bx - 1, by + (up ? 1 : 0), 0x1B2A55);
                plot(bx, by + 1, 0x1B2A55);
                plot(bx + 1, by + (up ? 1 : 0), 0x1B2A55);
                plot(bx + 2, by + (up ? 0 : 1), 0x1B2A55);
            }
        }
        for (double[] e : embers) {
            double y = ((e[1] - t * e[2]) % H + H) % H;
            double x = e[0] + Math.sin(t * 0.8 + e[3]) * 6;
            double heat = y / H;
            int rgb = dark ? (heat > 0.7 ? 0xFFD35E : heat > 0.4 ? 0xFF8A3A : 0xC2474A)
                    : (heat > 0.6 ? 0xFFFFFF : 0xFFF3A0);
            plot((int) x, (int) y, rgb);
        }
        sparks(t);
        magic(t);
        return frame;
    }

    /** Sparks where the fighter's blade meets the axe, and a few smaller ones where the rogue is fighting. */
    private void sparks(double t) {
        burst(CLASH_X, CLASH_Y, 0.5 + 0.5 * Math.sin(t * 7), 1.0, t);
        burst(246, 170, 0.5 + 0.5 * Math.sin(t * 5.3 + 1.7), 0.55, t + 3);
    }

    private void burst(int cx, int cy, double pulse, double size, double t) {
        int r = (int) ((4 + pulse * 3) * size) + 2;
        for (int d = 0; d < 8; d++) {
            double a = d * Math.PI / 4 + 0.2 * Math.sin(t * 3);
            int len = (int) (((d % 2 == 0 ? 11 : 7) + pulse * 4) * size) + 2;
            for (int i = 2; i < len; i++) {
                plot(cx + (int) Math.round(Math.cos(a) * i), cy + (int) Math.round(Math.sin(a) * i),
                        i < len / 2 ? 0xFFF3B0 : i < len * 3 / 4 ? 0xFFB347 : 0xFF6A33);
            }
        }
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                int d2 = x * x + y * y;
                if (d2 <= 2 * size * size + 1) {
                    plot(cx + x, cy + y, 0xFFFFFF);
                } else if (d2 <= r * r / 2) {
                    plot(cx + x, cy + y, 0xFFE27A);
                } else if (d2 <= r * r && ((x + y) & 1) == 0) {
                    plot(cx + x, cy + y, 0xFF8A3A);
                }
            }
        }
        for (int i = 0; i < 8; i++) {                          // flying sparks
            double a = i * 2.399 + Math.floor(t * 2) * 0.7;
            double life = (t * 2 + i * 0.37) % 1.0;
            plot(cx + (int) Math.round(Math.cos(a) * (10 + life * 22 * size)),
                    cy + (int) Math.round(Math.sin(a) * (8 + life * 16 * size) + life * life * 10),
                    life < 0.5 ? 0xFFF3B0 : 0xFF8A3A);
        }
    }

    /** The wizard's orb glows and a green magic bolt flies from it to the enemy line, over and over. */
    private void magic(double t) {
        double pulse = 0.5 + 0.5 * Math.sin(t * 5);
        int r = 4 + (int) (pulse * 2);
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                int d2 = x * x + y * y;
                if (d2 <= r * r && d2 > (r - 1) * (r - 1) + 1 && ((x + y) & 1) == 0) {
                    plot(orbX + x, orbY + y, 0x7CFFB0);
                }
            }
        }
        double life = (t * 0.7) % 1.0;
        double fx = orbX + (258 - orbX) * life;
        double fy = orbY + (150 - orbY) * life - Math.sin(life * Math.PI) * 24;
        for (int i = 0; i < 7; i++) {                          // trail
            double l = Math.max(0, life - i * 0.012);
            int tx = (int) (orbX + (258 - orbX) * l);
            int ty = (int) (orbY + (150 - orbY) * l - Math.sin(l * Math.PI) * 24);
            plot(tx, ty, i < 2 ? 0xFFFFFF : i < 4 ? 0x7CFFB0 : 0x2EAA6A);
            plot(tx, ty + 1, i < 4 ? 0x7CFFB0 : 0x2EAA6A);
        }
        plot((int) fx, (int) fy, 0xFFFFFF);
        plot((int) fx + 1, (int) fy, 0xBFFFE0);
        plot((int) fx, (int) fy - 1, 0xBFFFE0);
        plot((int) fx - 1, (int) fy, 0xBFFFE0);
        plot((int) fx, (int) fy + 1, 0xBFFFE0);
    }

    private void plot(int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < W && y < H) {
            frame.setRGB(x, y, rgb);
        }
    }

    // ---------------------------------------------------------------- drawing helpers

    private void box(int x, int y, int w, int h, int rgb) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                if (i >= 0 && j >= 0 && i < W && j < H) {
                    base.setRGB(i, j, rgb);
                }
            }
        }
    }

    private static void box(BufferedImage img, int x, int y, int w, int h, int argb) {
        int a = (argb >>> 24) == 0 ? 0xFF000000 | argb : argb;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                put(img, i, j, a);
            }
        }
    }

    private static void polygon(BufferedImage img, int[] xs, int[] ys, int argb) {
        java.awt.Polygon poly = new java.awt.Polygon(xs, ys, xs.length);
        java.awt.Rectangle b = poly.getBounds();
        for (int y = b.y; y <= b.y + b.height; y++) {
            for (int x = b.x; x <= b.x + b.width; x++) {
                if (poly.contains(x, y)) {
                    put(img, x, y, argb);
                }
            }
        }
    }

    private static void disc(BufferedImage img, int cx, int cy, int r, int rgb) {
        int argb = (rgb >>> 24) == 0 ? 0xFF000000 | rgb : rgb;
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                if (x * x + y * y <= r * r) {
                    put(img, cx + x, cy + y, argb);
                }
            }
        }
    }

    private static void thickLine(BufferedImage img, int x1, int y1, int x2, int y2, int thickness, int argb) {
        int a = (argb >>> 24) == 0 ? 0xFF000000 | argb : argb;
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        int half = thickness / 2;
        for (int i = 0; i <= steps; i++) {
            double f = steps == 0 ? 0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * f);
            int y = (int) Math.round(y1 + (y2 - y1) * f);
            for (int j = -half; j <= half; j++) {
                for (int k = -half; k <= half; k++) {
                    if (thickness <= 2 || j * j + k * k <= half * half + half) {
                        put(img, x + k, y + j, a);
                    }
                }
            }
        }
    }

    /** Draws a 1-pixel dark outline around everything painted on the layer. */
    private static void outline(BufferedImage layer) {
        int[][] copy = new int[H][W];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                copy[y][x] = layer.getRGB(x, y) >>> 24;
            }
        }
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (copy[y][x] == 0 && (x > 0 && copy[y][x - 1] != 0 || x < W - 1 && copy[y][x + 1] != 0
                        || y > 0 && copy[y - 1][x] != 0 || y < H - 1 && copy[y + 1][x] != 0)) {
                    layer.setRGB(x, y, OUTLINE);
                }
            }
        }
    }

    private void composite(BufferedImage layer) {
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int p = layer.getRGB(x, y);
                if (p >>> 24 != 0) {
                    base.setRGB(x, y, p & 0xFFFFFF);
                }
            }
        }
    }

    private static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
