package gamemaster.gui;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** A 5 by 7 pixel letter font for the cover screen, so titles and menu items match the pixel-art look. */
final class PixelFont {

    static final int W = 5, H = 7;

    private static final Map<Character, String[]> GLYPHS = new HashMap<>();

    private static void g(char c, String... rows) {
        GLYPHS.put(c, rows);
    }

    static {
        g('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        g('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.");
        g('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.");
        g('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
        g('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####");
        g('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....");
        g('G', ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".###.");
        g('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        g('I', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####");
        g('J', "..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##..");
        g('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#");
        g('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
        g('M', "#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#");
        g('N', "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#");
        g('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....");
        g('Q', ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#");
        g('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#");
        g('S', ".####", "#....", "#....", ".###.", "....#", "....#", "####.");
        g('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
        g('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('V', "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#..");
        g('W', "#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#");
        g('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
        g('Y', "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..");
        g('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####");
        g('0', ".###.", "#..##", "#.#.#", "#.#.#", "#.#.#", "##..#", ".###.");
        g('1', "..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###.");
        g('2', ".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####");
        g('3', "####.", "....#", "....#", ".###.", "....#", "....#", "####.");
        g('4', "...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#.");
        g('5', "#####", "#....", "####.", "....#", "....#", "#...#", ".###.");
        g('6', ".###.", "#....", "#....", "####.", "#...#", "#...#", ".###.");
        g('7', "#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#...");
        g('8', ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.");
        g('9', ".###.", "#...#", "#...#", ".####", "....#", "....#", ".###.");
        g('-', ".....", ".....", ".....", "#####", ".....", ".....", ".....");
        g('.', ".....", ".....", ".....", ".....", ".....", ".##..", ".##..");
        g('>', "#....", ".#...", "..#..", "...#.", "..#..", ".#...", "#....");
        g(' ', ".....", ".....", ".....", ".....", ".....", ".....", ".....");
    }

    private PixelFont() {}

    /** Width in pixels of {@code text} at {@code scale}, with one blank column between letters. */
    static int width(String text, int scale) {
        return text.isEmpty() ? 0 : (text.length() * (W + 1) - 1) * scale;
    }

    static int height(int scale) {
        return H * scale;
    }

    /** Draws {@code text} into {@code img} with its top-left at (x, y); each font pixel becomes a scale by scale block. */
    static void draw(BufferedImage img, String text, int x, int y, int scale, Color top, Color bottom) {
        int cx = x;
        for (char raw : text.toUpperCase().toCharArray()) {
            String[] rows = GLYPHS.getOrDefault(raw, GLYPHS.get(' '));
            for (int ry = 0; ry < H; ry++) {
                Color c = blend(top, bottom, ry / (double) (H - 1));
                for (int rx = 0; rx < W; rx++) {
                    if (rows[ry].charAt(rx) == '#') {
                        fill(img, cx + rx * scale, y + ry * scale, scale, scale, c.getRGB());
                    }
                }
            }
            cx += (W + 1) * scale;
        }
    }

    /** Draws {@code text} straight onto a Graphics2D in one flat colour (used for menu items). */
    static void paint(java.awt.Graphics2D g2, String text, int x, int y, int scale, Color colour) {
        g2.setColor(colour);
        int cx = x;
        for (char raw : text.toUpperCase().toCharArray()) {
            String[] rows = GLYPHS.getOrDefault(raw, GLYPHS.get(' '));
            for (int ry = 0; ry < H; ry++) {
                for (int rx = 0; rx < W; rx++) {
                    if (rows[ry].charAt(rx) == '#') {
                        g2.fillRect(cx + rx * scale, y + ry * scale, scale, scale);
                    }
                }
            }
            cx += (W + 1) * scale;
        }
    }

    static Color blend(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static void fill(BufferedImage img, int x, int y, int w, int h, int argb) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                if (i >= 0 && j >= 0 && i < img.getWidth() && j < img.getHeight()) {
                    img.setRGB(i, j, argb);
                }
            }
        }
    }
}
