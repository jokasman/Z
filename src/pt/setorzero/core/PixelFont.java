package pt.setorzero.core;

import java.util.HashMap;

/** Letra pixelizada 5×7 (estilo anos 90), com acentos portugueses. Desenhada com retângulos. */
public final class PixelFont {
    public static final int CAP = 7; // altura das maiúsculas em píxeis

    private static final class Glyph {
        final boolean[][] px; // [linha][coluna], linha 0 = topo das maiúsculas (pode ter 9 linhas)
        final int left, width;
        Glyph(String[] rows) {
            int h = rows.length, w = 0;
            for (String r : rows) w = Math.max(w, r.length());
            px = new boolean[h][w];
            int l = w, rr = -1;
            for (int y = 0; y < h; y++)
                for (int x = 0; x < rows[y].length(); x++)
                    if (rows[y].charAt(x) == '#') { px[y][x] = true; l = Math.min(l, x); rr = Math.max(rr, x); }
            if (rr < 0) { left = 0; width = 2; } else { left = l; width = rr - l + 1; }
        }
    }

    private static final HashMap<Character, Glyph> G = new HashMap<>();
    private static final HashMap<Character, char[]> COMPOSE = new HashMap<>();
    private static final String[] ACUTE = {"   # ", "  #  "}, GRAVE = {" #   ", "  #  "}, CIRC = {"  #  ", " # # "},
        TILDE = {" ## #", "#  # "}, CEDIL = {"  #  ", " #   "};

    private static void g(char c, String... rows) { G.put(c, new Glyph(rows)); }

    static {
        g('A', " ### ", "#   #", "#   #", "#####", "#   #", "#   #", "#   #");
        g('B', "#### ", "#   #", "#   #", "#### ", "#   #", "#   #", "#### ");
        g('C', " ### ", "#   #", "#    ", "#    ", "#    ", "#   #", " ### ");
        g('D', "#### ", "#   #", "#   #", "#   #", "#   #", "#   #", "#### ");
        g('E', "#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#####");
        g('F', "#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#    ");
        g('G', " ### ", "#   #", "#    ", "# ###", "#   #", "#   #", " ####");
        g('H', "#   #", "#   #", "#   #", "#####", "#   #", "#   #", "#   #");
        g('I', "###", " # ", " # ", " # ", " # ", " # ", "###");
        g('J', "  ###", "   # ", "   # ", "   # ", "   # ", "#  # ", " ##  ");
        g('K', "#   #", "#  # ", "# #  ", "##   ", "# #  ", "#  # ", "#   #");
        g('L', "#    ", "#    ", "#    ", "#    ", "#    ", "#    ", "#####");
        g('M', "#   #", "## ##", "# # #", "# # #", "#   #", "#   #", "#   #");
        g('N', "#   #", "##  #", "# # #", "#  ##", "#   #", "#   #", "#   #");
        g('O', " ### ", "#   #", "#   #", "#   #", "#   #", "#   #", " ### ");
        g('P', "#### ", "#   #", "#   #", "#### ", "#    ", "#    ", "#    ");
        g('Q', " ### ", "#   #", "#   #", "#   #", "# # #", "#  # ", " ## #");
        g('R', "#### ", "#   #", "#   #", "#### ", "# #  ", "#  # ", "#   #");
        g('S', " ####", "#    ", "#    ", " ### ", "    #", "    #", "#### ");
        g('T', "#####", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ");
        g('U', "#   #", "#   #", "#   #", "#   #", "#   #", "#   #", " ### ");
        g('V', "#   #", "#   #", "#   #", "#   #", "#   #", " # # ", "  #  ");
        g('W', "#   #", "#   #", "#   #", "# # #", "# # #", "## ##", "#   #");
        g('X', "#   #", "#   #", " # # ", "  #  ", " # # ", "#   #", "#   #");
        g('Y', "#   #", "#   #", " # # ", "  #  ", "  #  ", "  #  ", "  #  ");
        g('Z', "#####", "    #", "   # ", "  #  ", " #   ", "#    ", "#####");

        g('a', "     ", "     ", " ### ", "    #", " ####", "#   #", " ####");
        g('b', "#    ", "#    ", "# ## ", "##  #", "#   #", "#   #", "#### ");
        g('c', "     ", "     ", " ### ", "#    ", "#    ", "#   #", " ### ");
        g('d', "    #", "    #", " ## #", "#  ##", "#   #", "#   #", " ####");
        g('e', "     ", "     ", " ### ", "#   #", "#####", "#    ", " ### ");
        g('f', "  ## ", " #  #", " #   ", "###  ", " #   ", " #   ", " #   ");
        g('g', "     ", "     ", " ####", "#   #", "#   #", "#   #", " ####", "    #", " ### ");
        g('h', "#    ", "#    ", "# ## ", "##  #", "#   #", "#   #", "#   #");
        g('i', " # ", "   ", "## ", " # ", " # ", " # ", "###");
        g('ı', "   ", "   ", "## ", " # ", " # ", " # ", "###"); // i sem ponto
        g('j', "   #", "    ", "  ##", "   #", "   #", "   #", "   #", "#  #", " ## ");
        g('k', "#   ", "#   ", "#  #", "# # ", "##  ", "# # ", "#  #");
        g('l', "## ", " # ", " # ", " # ", " # ", " # ", "###");
        g('m', "     ", "     ", "## # ", "# # #", "# # #", "# # #", "# # #");
        g('n', "     ", "     ", "# ## ", "##  #", "#   #", "#   #", "#   #");
        g('o', "     ", "     ", " ### ", "#   #", "#   #", "#   #", " ### ");
        g('p', "     ", "     ", "#### ", "#   #", "#   #", "#   #", "#### ", "#    ", "#    ");
        g('q', "     ", "     ", " ####", "#   #", "#   #", "#   #", " ####", "    #", "    #");
        g('r', "    ", "    ", "# ##", "##  ", "#   ", "#   ", "#   ");
        g('s', "     ", "     ", " ####", "#    ", " ### ", "    #", "#### ");
        g('t', " #  ", " #  ", "### ", " #  ", " #  ", " #  #", "  ## ");
        g('u', "     ", "     ", "#   #", "#   #", "#   #", "#  ##", " ## #");
        g('v', "     ", "     ", "#   #", "#   #", "#   #", " # # ", "  #  ");
        g('w', "     ", "     ", "#   #", "#   #", "# # #", "# # #", " # # ");
        g('x', "     ", "     ", "#   #", " # # ", "  #  ", " # # ", "#   #");
        g('y', "     ", "     ", "#   #", "#   #", "#   #", "#   #", " ####", "    #", " ### ");
        g('z', "     ", "     ", "#####", "   # ", "  #  ", " #   ", "#####");

        g('0', " ### ", "#   #", "#  ##", "# # #", "##  #", "#   #", " ### ");
        g('1', " # ", "## ", " # ", " # ", " # ", " # ", "###");
        g('2', " ### ", "#   #", "    #", "   # ", "  #  ", " #   ", "#####");
        g('3', "#####", "   # ", "  #  ", "   # ", "    #", "#   #", " ### ");
        g('4', "   # ", "  ## ", " # # ", "#  # ", "#####", "   # ", "   # ");
        g('5', "#####", "#    ", "#### ", "    #", "    #", "#   #", " ### ");
        g('6', "  ## ", " #   ", "#    ", "#### ", "#   #", "#   #", " ### ");
        g('7', "#####", "    #", "   # ", "  #  ", " #   ", " #   ", " #   ");
        g('8', " ### ", "#   #", "#   #", " ### ", "#   #", "#   #", " ### ");
        g('9', " ### ", "#   #", "#   #", " ####", "    #", "   # ", " ##  ");

        g('.', " ", " ", " ", " ", " ", " ", "#");
        g(',', "  ", "  ", "  ", "  ", "  ", " #", " #", "# ");
        g(':', " ", " ", "#", " ", " ", "#", " ");
        g(';', "  ", "  ", " #", "  ", "  ", " #", " #", "# ");
        g('!', "#", "#", "#", "#", "#", " ", "#");
        g('?', " ### ", "#   #", "    #", "   # ", "  #  ", "     ", "  #  ");
        g('\'', "#", "#", " ", " ", " ", " ", " ");
        g('"', "# #", "# #", "   ", "   ", "   ", "   ", "   ");
        g('-', "    ", "    ", "    ", "####", "    ", "    ", "    ");
        g('+', "     ", "  #  ", "  #  ", "#####", "  #  ", "  #  ", "     ");
        g('×', "     ", "     ", "#   #", " # # ", "  #  ", " # # ", "#   #");
        g('·', "  ", "  ", "  ", "##", "##", "  ", "  ");
        g('(', "  #", " # ", "#  ", "#  ", "#  ", " # ", "  #");
        g(')', "#  ", " # ", "  #", "  #", "  #", " # ", "#  ");
        g('/', "    #", "    #", "   # ", "  #  ", " #   ", "#    ", "#    ");
        g('%', "##  #", "## # ", "   # ", "  #  ", " #   ", " # ##", "#  ##");
        g('>', "#   ", " #  ", "  # ", "   #", "  # ", " #  ", "#   ");
        g('<', "   #", "  # ", " #  ", "#   ", " #  ", "  # ", "   #");
        g('=', "    ", "    ", "####", "    ", "####", "    ", "    ");
        g('*', "     ", "# # #", " ### ", "#####", " ### ", "# # #", "     ");
        g('#', " # # ", " # # ", "#####", " # # ", "#####", " # # ", " # # ");

        String[][] comp = {
            {"á", "a", "1"}, {"à", "a", "2"}, {"â", "a", "3"}, {"ã", "a", "4"}, {"é", "e", "1"}, {"ê", "e", "3"},
            {"í", "ı", "1"}, {"ó", "o", "1"}, {"ô", "o", "3"}, {"õ", "o", "4"}, {"ú", "u", "1"}, {"ç", "c", "5"},
            {"Á", "A", "1"}, {"À", "A", "2"}, {"Â", "A", "3"}, {"Ã", "A", "4"}, {"É", "E", "1"}, {"Ê", "E", "3"},
            {"Í", "I", "1"}, {"Ó", "O", "1"}, {"Ô", "O", "3"}, {"Õ", "O", "4"}, {"Ú", "U", "1"}, {"Ç", "C", "5"},
        };
        for (String[] c : comp) COMPOSE.put(c[0].charAt(0), new char[]{c[1].charAt(0), c[2].charAt(0)});
    }

    private PixelFont() { }

    private static Glyph glyph(char c) {
        Glyph g = G.get(c);
        return g != null ? g : G.get('?');
    }

    /** Largura em píxeis (escala 1) de um texto. */
    public static int width(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') { w += 3; continue; }
            char[] cp = COMPOSE.get(c);
            w += glyph(cp != null ? cp[0] : c).width + 1;
        }
        return Math.max(0, w - 1);
    }

    public static float width(String s, float scale) { return width(s) * scale; }

    /** Desenha o texto com o topo das maiúsculas em y. */
    public static void drawTop(Gfx g, String s, float x, float y, float sc, int color, int shadow) {
        if (shadow != 0) { g.color(shadow); raw(g, s, x + sc, y + sc, sc); }
        g.color(color);
        raw(g, s, x, y, sc);
    }

    private static void raw(Gfx g, String s, float x, float y, float sc) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') { x += 3 * sc; continue; }
            char[] cp = COMPOSE.get(c);
            Glyph gl = glyph(cp != null ? cp[0] : c);
            for (int r = 0; r < gl.px.length; r++) {
                boolean[] row = gl.px[r];
                int run = -1;
                for (int col = 0; col <= row.length; col++) {
                    boolean on = col < row.length && row[col];
                    if (on && run < 0) run = col;
                    if (!on && run >= 0) {
                        g.fillRect(x + (run - gl.left) * sc, y + r * sc, (col - run) * sc, sc);
                        run = -1;
                    }
                }
            }
            if (cp != null) {
                String[] acc = cp[1] == '1' ? ACUTE : cp[1] == '2' ? GRAVE : cp[1] == '3' ? CIRC : cp[1] == '4' ? TILDE : CEDIL;
                boolean upper = Character.isUpperCase(cp[0]);
                float ay = cp[1] == '5' ? y + 7 * sc : (upper ? y - 3 * sc : y);
                // centrar o acento (5 colunas) sobre a letra
                float ax = x + (gl.width - 5) * sc / 2f;
                ax = Math.round(ax / sc) * sc;
                for (int r = 0; r < 2; r++)
                    for (int col = 0; col < acc[r].length(); col++)
                        if (acc[r].charAt(col) == '#') g.fillRect(ax + col * sc, ay + r * sc, sc, sc);
            }
            x += (gl.width + 1) * sc;
        }
    }
}
