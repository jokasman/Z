package pt.setorzero.core;

/** Gera o terreno em pixel art (16 píxeis por quadrícula) com paletas por tema. */
public final class TerrainArt {
    public static final int TX = 16;
    public static final int TEMPERATE = 0, DESERT = 1, SNOW = 2, VOLCANO = 3, CITY = 4;
    public static final String[] THEME_NAMES = {"Prados", "Deserto", "Neve", "Vulcão", "Cidade"};

    /** Paleta: chão(3), terra(3), rocha(base, luz, sombra), água(funda, média, reflexo, espuma), árvore(escura, média, luz, contorno), margem, ponte(2). */
    static final int[][] PAL = {
        {0xFF4C7A2C, 0xFF588A34, 0xFF6E9E3E, 0xFF7A5A34, 0xFF8C6A3E, 0xFF5E4426, 0xFF7C7C74, 0xFFA8A89C, 0xFF4A4A44,
         0xFF1E4C7C, 0xFF2A62A0, 0xFF6EA8DC, 0xFFC8E4F4, 0xFF1C4A18, 0xFF2E6E24, 0xFF5AA03C, 0xFF10240C, 0xFFC4AC6C, 0xFF8A6034, 0xFF5A3C1E},
        {0xFFD8B468, 0xFFE4C27A, 0xFFC8A058, 0xFFB88A4C, 0xFFA87A40, 0xFF8C6232, 0xFFB0603A, 0xFFD88A56, 0xFF703820,
         0xFF1E6C78, 0xFF2A8C96, 0xFF7CD4D4, 0xFFE8F8F0, 0xFF2E6A28, 0xFF4A8C34, 0xFF7CBC4C, 0xFF183814, 0xFFE8D498, 0xFF8A6034, 0xFF5A3C1E},
        {0xFFE4ECF4, 0xFFD0DCEC, 0xFFF8FCFF, 0xFFA8A4A0, 0xFFB8B4AC, 0xFF8C8884, 0xFF6C7488, 0xFF9CA8C0, 0xFF3C4458,
         0xFF2C4C7C, 0xFF3C66A0, 0xFF8CB8E4, 0xFFE0F0FF, 0xFF1C3C2C, 0xFF2C5C3C, 0xFF3E7A50, 0xFF0C1C14, 0xFFC8D4E0, 0xFF7A5A3A, 0xFF4A3420},
        {0xFF3C3634, 0xFF484240, 0xFF2E2A28, 0xFF5A3A2C, 0xFF6C4634, 0xFF40281E, 0xFF242026, 0xFF4A4250, 0xFF100E12,
         0xFFC0300C, 0xFFF06A10, 0xFFFFD040, 0xFFFFF4A0, 0xFF2A2420, 0xFF3C3430, 0xFF5A4C44, 0xFF0C0A08, 0xFF1C1614, 0xFF4A4A4A, 0xFF242424},
        {0xFF6C6C70, 0xFF78787C, 0xFF606064, 0xFF8A7C6C, 0xFF9A8C7A, 0xFF6C6052, 0xFF5C5A66, 0xFF8A8A98, 0xFF2C2A34,
         0xFF2A4A5C, 0xFF365E74, 0xFF7AA4BC, 0xFFC4DCE8, 0xFF2C5A28, 0xFF3E7A34, 0xFF64A84C, 0xFF14280E, 0xFF9A9A9C, 0xFF5C4A3A, 0xFF3A2E24},
    };
    static final int G0 = 0, G1 = 1, G2 = 2, D0 = 3, D1 = 4, D2 = 5, R0 = 6, RL = 7, RD = 8, W0 = 9, W1 = 10, WL = 11, WF = 12,
        T0 = 13, T1 = 14, TL = 15, TO = 16, SHORE = 17, B0 = 18, B1 = 19;

    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    private TerrainArt() { }

    static int hash(int x, int y, int k) {
        int h = x * 374761393 + y * 668265263 + k * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    static float n01(int x, int y, int k) { return (hash(x, y, k) & 0xFFFF) / 65535f; }

    /** Ruído de valor suave (interpolação bilinear numa grelha de passo s). */
    static float smooth(int x, int y, int s, int k) {
        int gx = Math.floorDiv(x, s), gy = Math.floorDiv(y, s);
        float fx = (x - gx * s) / (float) s, fy = (y - gy * s) / (float) s;
        float a = n01(gx, gy, k), b = n01(gx + 1, gy, k), c = n01(gx, gy + 1, k), d = n01(gx + 1, gy + 1, k);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    private static boolean dither(int x, int y, float v) { return v * 16 > BAYER[(y & 3) * 4 + (x & 3)]; }

    /** Matriz de materiais por píxel, com bordas irregulares. */
    static byte[] materials(World w) {
        int TW = w.W * TX, TH = w.H * TX;
        byte[] m = new byte[TW * TH];
        for (int y = 0; y < TH; y++) {
            for (int x = 0; x < TW; x++) {
                int ox = Math.round((smooth(x, y, 5, 11) - 0.5f) * 5), oy = Math.round((smooth(x, y, 5, 12) - 0.5f) * 5);
                int tx = Math.max(0, Math.min(w.W - 1, (x + ox) / TX)), ty = Math.max(0, Math.min(w.H - 1, (y + oy) / TX));
                int t = w.tileAt(tx, ty);
                int own = w.tileAt(x / TX, y / TX);
                // pontes e bordas do mapa não se deformam
                if (own == World.BRIDGE || t == World.BRIDGE) t = own;
                m[y * TW + x] = (byte) t;
            }
        }
        return m;
    }

    public static int[] paint(World w) {
        int TW = w.W * TX, TH = w.H * TX;
        int[] p = PAL[w.theme];
        int theme = w.theme;
        byte[] m = materials(w);
        int[] out = new int[TW * TH];
        for (int y = 0; y < TH; y++) {
            for (int x = 0; x < TW; x++) {
                int mat = m[y * TW + x];
                int c;
                switch (mat) {
                    case World.ROCK: c = rock(w, m, TW, TH, x, y, p, theme); break;
                    case World.WATER: c = water(m, TW, TH, x, y, p, theme); break;
                    case World.DIRT: c = dirt(x, y, p); break;
                    case World.BRIDGE: c = bridge(w, x, y, p); break;
                    default: c = ground(x, y, p, theme);
                }
                if (mat == World.GRASS || mat == World.DIRT || mat == World.TREE) {
                    if (near(m, TW, TH, x, y, World.WATER, 3) && theme != TerrainArt.VOLCANO) c = dither(x, y, 0.5f) ? p[SHORE] : Draw.shade(p[SHORE], 0.9f);
                    if (at(m, TW, TH, x - 4, y - 4) == World.ROCK) c = Draw.shade(c, 0.7f);
                }
                out[y * TW + x] = c;
            }
        }
        // árvores (por cima do chão)
        for (int ty = 0; ty < w.H; ty++)
            for (int tx = 0; tx < w.W; tx++)
                if (w.tileAt(tx, ty) == World.TREE) tree(out, TW, TH, tx, ty, p, theme);
        // limites dos setores, tracejados
        for (Sector s : w.sectors) {
            int x0 = s.x0 * TX, y0 = s.y0 * TX, x1 = (s.x1 + 1) * TX, y1 = (s.y1 + 1) * TX;
            for (int x = x0; x < x1; x++) { dash(out, TW, TH, x, y0, x); dash(out, TW, TH, x, y1 - 1, x); }
            for (int y = y0; y < y1; y++) { dash(out, TW, TH, x0, y, y); dash(out, TW, TH, x1 - 1, y, y); }
        }
        return out;
    }

    private static void dash(int[] out, int TW, int TH, int x, int y, int k) {
        if (x < TX || y < TX || x >= TW - TX || y >= TH - TX) return;
        if ((k / 3) % 2 != 0) return;
        out[y * TW + x] = Draw.shade(out[y * TW + x], 1.25f);
    }

    private static int at(byte[] m, int TW, int TH, int x, int y) {
        if (x < 0 || y < 0 || x >= TW || y >= TH) return World.ROCK;
        return m[y * TW + x];
    }

    private static boolean near(byte[] m, int TW, int TH, int x, int y, int mat, int r) {
        for (int dy = -r; dy <= r; dy += r)
            for (int dx = -r; dx <= r; dx += r)
                if (at(m, TW, TH, x + dx, y + dy) == mat) return true;
        return false;
    }

    private static int ground(int x, int y, int[] p, int theme) {
        float v = smooth(x, y, 12, 1) * 0.7f + smooth(x, y, 4, 2) * 0.3f;
        int c = dither(x, y, v) ? p[G1] : p[G0];
        if (theme == DESERT) {
            float r = (float) Math.sin((x * 0.5f + y * 0.9f + smooth(x, y, 16, 3) * 18) * 0.55f);
            if (r > 0.82f) c = p[G2];
            else if (r < -0.9f) c = Draw.shade(p[G0], 0.92f);
        } else if (theme == CITY) {
            if (x % 32 == 0 || y % 32 == 0) c = Draw.shade(p[G0], 0.85f);
            if (n01(x / 2, y / 2, 4) > 0.97f) c = p[G2];
        } else if (theme == SNOW) {
            if (n01(x, y, 5) > 0.985f) c = p[G2];
            if (smooth(x, y, 9, 6) > 0.72f && dither(x, y, 0.5f)) c = Draw.shade(p[G1], 0.95f);
        } else {
            float k = n01(x, y, 7);
            if (k > 0.965f) c = p[G2];
            else if (k < 0.02f) c = Draw.shade(p[G0], 0.8f);
            if (theme == VOLCANO && smooth(x, y, 6, 8) > 0.8f && n01(x, y, 9) > 0.7f) c = 0xFF7A2A10;
        }
        return c;
    }

    private static int dirt(int x, int y, int[] p) {
        float v = smooth(x, y, 6, 21);
        int c = dither(x, y, v) ? p[D1] : p[D0];
        float k = n01(x, y, 22);
        if (k > 0.94f) c = p[D2];
        else if (k < 0.04f) c = Draw.shade(p[D1], 1.15f);
        return c;
    }

    private static int water(byte[] m, int TW, int TH, int x, int y, int[] p, int theme) {
        if (near(m, TW, TH, x, y, World.GRASS, 1) || near(m, TW, TH, x, y, World.DIRT, 1) || near(m, TW, TH, x, y, World.ROCK, 1))
            return theme == VOLCANO ? 0xFF2A1A14 : p[WF];
        float v = smooth(x, y, 10, 31);
        int c = dither(x, y, v) ? p[W1] : p[W0];
        if (theme == VOLCANO) {
            if (smooth(x, y, 5, 33) > 0.7f) c = p[WL];
            if (smooth(x, y, 7, 34) < 0.18f) c = 0xFF3A1A10;
            return c;
        }
        // reflexos curtos
        int cx = x / 7, cy = y / 4;
        if (n01(cx, cy, 32) > 0.82f && (y % 4) == 0 && (x % 7) < 4) c = p[WL];
        if (theme == SNOW && smooth(x, y, 8, 35) > 0.75f) c = dither(x, y, 0.6f) ? p[WF] : p[WL];
        return c;
    }

    private static int rock(World w, byte[] m, int TW, int TH, int x, int y, int[] p, int theme) {
        boolean lit = at(m, TW, TH, x - 2, y - 2) != World.ROCK;
        boolean shadow = at(m, TW, TH, x + 2, y + 2) != World.ROCK || at(m, TW, TH, x, y + 3) != World.ROCK;
        if (at(m, TW, TH, x - 1, y) != World.ROCK || at(m, TW, TH, x + 1, y) != World.ROCK
            || at(m, TW, TH, x, y - 1) != World.ROCK || at(m, TW, TH, x, y + 1) != World.ROCK) return 0xFF141414;
        if (theme == CITY) {
            // ruínas: telhados com janelas
            int bx = x % 16, by = y % 16;
            int c = dither(x, y, smooth(x, y, 8, 41)) ? p[R0] : Draw.shade(p[R0], 0.92f);
            if (bx > 2 && bx < 14 && by > 2 && by < 14 && (bx % 4 == 1) && (by % 4 == 1)) c = n01(x / 4, y / 4, 42) > 0.6f ? 0xFFE8C860 : p[RD];
            if (lit) c = p[RL];
            if (shadow) c = p[RD];
            if (n01(x / 3, y / 3, 43) > 0.93f) c = 0xFF3A3640;
            return c;
        }
        // pedregulhos: células com luz em cima à esquerda
        int s = 6;
        int cx = Math.floorDiv(x, s), cy = Math.floorDiv(y, s);
        float jx = n01(cx, cy, 44), jy = n01(cx, cy, 45);
        float fx = (x - cx * s) / (float) s - jx * 0.4f, fy = (y - cy * s) / (float) s - jy * 0.4f;
        int c = dither(x, y, smooth(x, y, 7, 46)) ? p[R0] : Draw.shade(p[R0], 0.9f);
        if (fx + fy < 0.35f) c = Draw.shade(p[RL], 0.95f);
        else if (fx + fy > 1.25f) c = Draw.shade(p[RD], 1.1f);
        if (lit) c = p[RL];
        if (shadow) c = p[RD];
        if (theme == SNOW && (lit || fy < 0.2f) && dither(x, y, 0.7f)) c = 0xFFF0F6FF;
        return c;
    }

    private static int bridge(World w, int x, int y, int[] p) {
        int tx = x / TX, ty = y / TX;
        boolean vertical = (w.tileAt(Math.max(0, tx - 1), ty) == World.WATER) || (w.tileAt(Math.min(w.W - 1, tx + 1), ty) == World.WATER);
        int along = vertical ? y : x, across = vertical ? x % TX : y % TX;
        if (across <= 1 || across >= TX - 2) return p[B1];
        int c = (along % 4 == 0) ? p[B1] : p[B0];
        if (n01(x / 2, y / 4, 51) > 0.9f) c = Draw.shade(p[B0], 0.85f);
        return c;
    }

    private static void tree(int[] out, int TW, int TH, int tx, int ty, int[] p, int theme) {
        float cx = tx * TX + 8 + (n01(tx, ty, 61) - 0.5f) * 4, cy = ty * TX + 7 + (n01(tx, ty, 62) - 0.5f) * 4;
        float r = 7.5f + n01(tx, ty, 63) * 2;
        // sombra
        for (int y = (int) (cy - r); y <= cy + r + 4; y++)
            for (int x = (int) (cx - r); x <= cx + r + 4; x++) {
                if (x < 0 || y < 0 || x >= TW || y >= TH) continue;
                float dx = x - cx - 3, dy = y - cy - 3;
                if (dx * dx + dy * dy <= r * r && dither(x, y, 0.6f)) out[y * TW + x] = Draw.shade(out[y * TW + x], 0.6f);
            }
        for (int y = (int) (cy - r - 4); y <= cy + r; y++)
            for (int x = (int) (cx - r); x <= cx + r; x++) {
                if (x < 0 || y < 0 || x >= TW || y >= TH) continue;
                float dx = x - cx, dy = y - cy;
                boolean in, edge;
                if (theme == SNOW) {
                    // pinheiro visto de cima: estrela de 8 pontas
                    double a = Math.atan2(dy, dx);
                    float rr = r * (0.72f + 0.28f * (float) Math.abs(Math.cos(a * 4)));
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    in = d <= rr; edge = d > rr - 1.2f;
                } else if (theme == DESERT) {
                    // palmeira: folhas em cruz
                    float ax = Math.abs(dx), ay = Math.abs(dy);
                    in = (ax < 2.5f && ay < r) || (ay < 2.5f && ax < r) || (Math.abs(ax - ay) < 2f && ax < r * 0.75f);
                    edge = false;
                } else {
                    float d2 = dx * dx + dy * dy;
                    float rr = r + (smooth(x, y, 3, 64) - 0.5f) * 3;
                    in = d2 <= rr * rr; edge = d2 > (rr - 1.3f) * (rr - 1.3f);
                }
                if (!in) continue;
                float light = (-dx - dy) / (r * 1.4f);
                int c = light > 0.35f ? p[TL] : light > -0.25f ? (dither(x, y, 0.5f + light) ? p[T1] : p[T0]) : p[T0];
                if (theme == VOLCANO) c = light > 0.3f ? 0xFF5A4C44 : dither(x, y, 0.5f) ? 0xFF3C3430 : 0xFF2A2420;
                if (theme == SNOW && light > 0.1f && n01(x, y, 65) > 0.55f) c = 0xFFF0F6FF;
                if (edge) c = p[TO];
                out[y * TW + x] = c;
            }
        if (theme == DESERT) {
            for (int y = (int) cy - 2; y <= cy + 1; y++)
                for (int x = (int) cx - 1; x <= cx + 1; x++)
                    if (x >= 0 && y >= 0 && x < TW && y < TH) out[y * TW + x] = 0xFF6A4A24;
        }
    }

    /** Minimapa: 4 píxeis por quadrícula, tirados do terreno. */
    public static int[] mini(int[] terrain, World w) {
        int TW = w.W * TX, MW = w.W * 4, MH = w.H * 4;
        int[] out = new int[MW * MH];
        for (int y = 0; y < MH; y++)
            for (int x = 0; x < MW; x++) out[y * MW + x] = terrain[(y * 4 + 2) * TW + x * 4 + 2];
        return out;
    }
}
