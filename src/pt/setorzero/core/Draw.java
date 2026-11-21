package pt.setorzero.core;

/** Toda a arte do jogo é desenhada por código: terreno, edifícios, unidades e efeitos. */
public final class Draw {
    public static final int C_PLAYER = 0xFFF28C28, C_ENEMY = 0xFF2EC4D6, C_NEUTRAL = 0xFF9A9A92;
    static final float T = World.TILE;
    private static final float[] POLY = new float[64];

    private Draw() { }

    public static int teamColor(int team) {
        return team == World.PLAYER ? C_PLAYER : team == World.ENEMY ? C_ENEMY : C_NEUTRAL;
    }

    public static int shade(int c, float f) {
        int a = c >>> 24, r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        if (f < 1) { r = (int) (r * f); g = (int) (g * f); b = (int) (b * f); }
        else { float k = f - 1; r = (int) (r + (255 - r) * k); g = (int) (g + (255 - g) * k); b = (int) (b + (255 - b) * k); }
        return (a << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int alpha(int c, float a) {
        return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (c & 0xFFFFFF);
    }

    private static int clamp(int v) { return v < 0 ? 0 : v > 255 ? 255 : v; }

    private static int hash(int x, int y, int k) {
        int h = x * 374761393 + y * 668265263 + k * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static float rnd01(int x, int y, int k) { return (hash(x, y, k) & 0xFFFF) / 65535f; }

    // ---------------------------------------------------------------- terreno

    /** Preenche como uma só mancha todas as quadrículas de um tipo, com cantos exteriores arredondados. */
    private static void mass(Gfx g, World w, int code, float grow, float rad, float ox, float oy) {
        for (int y = 0; y < w.H; y++) {
            for (int x = 0; x < w.W; x++) {
                if (w.tileAt(x, y) != code) continue;
                float px = x * T + ox, py = y * T + oy;
                g.fillRoundRect(px - grow, py - grow, T + grow * 2, T + grow * 2, rad);
                boolean r = x + 1 < w.W && w.tileAt(x + 1, y) == code;
                boolean d = y + 1 < w.H && w.tileAt(x, y + 1) == code;
                if (r) g.fillRect(px + T / 2, py - grow, T, T + grow * 2);
                if (d) g.fillRect(px - grow, py + T / 2, T + grow * 2, T);
                if (r && d && w.tileAt(x + 1, y + 1) == code) g.fillRect(px + T / 2, py + T / 2, T, T);
            }
        }
    }

    private static boolean is(World w, int x, int y, int code) {
        return x >= 0 && y >= 0 && x < w.W && y < w.H && w.tileAt(x, y) == code;
    }

    public static void paintTerrain(Gfx g, World w) {
        int W = w.W, H = w.H;
        // relva com variação suave
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                float v = rnd01(x / 2, y / 2, 1) * 0.6f + rnd01(x, y, 1) * 0.4f;
                g.color(shade(0xFF5E8A3A, 0.95f + v * 0.08f));
                g.fillRect(x * T, y * T, T + 0.5f, T + 0.5f);
            }
        }
        // terra batida
        g.color(0xFF8E7349);
        mass(g, w, World.DIRT, 3, 14, 0, 0);
        g.color(0xFF9B7E52);
        mass(g, w, World.DIRT, -1, 12, 0, 0);
        // tufos de erva e pedrinhas
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int c = w.tileAt(x, y);
                if (c != World.GRASS && c != World.DIRT) continue;
                for (int k = 0; k < 3; k++) {
                    float px = (x + rnd01(x, y, 10 + k)) * T, py = (y + rnd01(x, y, 20 + k)) * T;
                    if (c == World.GRASS) {
                        g.color(rnd01(x, y, 30 + k) > 0.5f ? 0x55A8D06A : 0x40254A18);
                        g.fillOval(px, py, 2.2f, 1.4f);
                    } else {
                        g.color(rnd01(x, y, 30 + k) > 0.5f ? 0x55705430 : 0x40C8AE80);
                        g.fillCircle(px, py, 1.4f + rnd01(x, y, 40 + k));
                    }
                }
            }
        }
        // água: margem de areia, água funda e reflexos
        g.color(0xFFC9B47E);
        mass(g, w, World.WATER, 6, 16, 0, 0);
        g.color(0xFF2B6390);
        mass(g, w, World.WATER, 1, 14, 0, 0);
        g.color(0xFF3477A8);
        mass(g, w, World.WATER, -5, 10, 0, 0);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (w.tileAt(x, y) != World.WATER) continue;
                g.color(0x55A8D8F0);
                float rx = (x + 0.2f + rnd01(x, y, 5) * 0.6f) * T, ry = (y + 0.2f + rnd01(x, y, 6) * 0.6f) * T;
                g.line(rx - 5, ry, rx + 5, ry, 1.2f);
                g.line(rx - 2, ry + 4, rx + 3, ry + 4, 1f);
            }
        }
        // rocha: sombra, massa, relevo
        g.color(0x50000000);
        mass(g, w, World.ROCK, 1, 12, 5, 6);
        g.color(0xFF55544F);
        mass(g, w, World.ROCK, 1, 12, 0, 0);
        g.color(0xFF6E6C66);
        mass(g, w, World.ROCK, -3, 10, -1, -2);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (w.tileAt(x, y) != World.ROCK) continue;
                float x0 = x * T, y0 = y * T;
                if (!is(w, x, y - 1, World.ROCK)) {
                    g.color(0x40FFFFFF);
                    g.fillRoundRect(x0 + 2, y0 + 1, T - 4, 4, 2);
                }
                for (int k = 0; k < 2; k++) {
                    float cx = x0 + (0.25f + rnd01(x, y, 50 + k) * 0.5f) * T, cy = y0 + (0.25f + rnd01(x, y, 60 + k) * 0.5f) * T;
                    float r = T * (0.14f + rnd01(x, y, 70 + k) * 0.12f);
                    int n = 6;
                    for (int i = 0; i < n; i++) {
                        double a = i * Math.PI * 2 / n + rnd01(x, y, 80 + k) * 2;
                        float rr = r * (0.75f + rnd01(x, y, 90 + i + k * 7) * 0.5f);
                        POLY[i * 2] = cx + (float) Math.cos(a) * rr;
                        POLY[i * 2 + 1] = cy + (float) Math.sin(a) * rr;
                    }
                    g.color(0x40000000);
                    for (int i = 0; i < n; i++) { POLY[i * 2] += 2; POLY[i * 2 + 1] += 2; }
                    g.fillPoly(POLY, n);
                    for (int i = 0; i < n; i++) { POLY[i * 2] -= 2; POLY[i * 2 + 1] -= 2; }
                    g.color(shade(0xFF8C8A82, 0.92f + rnd01(x, y, 99 + k) * 0.15f));
                    g.fillPoly(POLY, n);
                }
                g.color(0x50302E2A);
                float v = rnd01(x, y, 3);
                g.line(x0 + 5 + v * 8, y0 + T * 0.7f, x0 + 15 + v * 10, y0 + T * 0.6f + v * 6, 1.3f);
            }
        }
        // árvores
        for (int pass = 0; pass < 3; pass++) {
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    if (w.tileAt(x, y) != World.TREE) continue;
                    float v = rnd01(x, y, 4);
                    float cx = (x + 0.35f + v * 0.3f) * T, cy = (y + 0.35f + rnd01(x, y, 8) * 0.3f) * T;
                    if (pass == 0) { g.color(0x66000000); g.fillCircle(cx + 6, cy + 7, T * 0.6f); }
                    else if (pass == 1) { g.color(shade(0xFF2E5A22, 0.9f + v * 0.2f)); g.fillCircle(cx, cy, T * 0.62f); }
                    else {
                        g.color(shade(0xFF3F7A2C, 0.95f + v * 0.15f));
                        g.fillCircle(cx - 4, cy - 4, T * 0.4f);
                        g.color(0x40B8E08A);
                        g.fillCircle(cx - 7, cy - 8, T * 0.16f);
                    }
                }
            }
        }
        // limites dos setores (tracejado)
        g.color(0x40FFFFFF);
        for (int c = 1; c < 3; c++) {
            float x = c * 16 * T;
            for (float y = T; y < (H - 1) * T; y += 18) g.line(x, y, x, y + 9, 1.5f);
        }
        for (int r = 1; r < 3; r++) {
            float y = r * 12 * T;
            for (float x = T; x < (W - 1) * T; x += 18) g.line(x, y, x + 9, y, 1.5f);
        }
    }

    public static void paintMini(Gfx g, World w) {
        for (int y = 0; y < w.H; y++) {
            for (int x = 0; x < w.W; x++) {
                int c;
                switch (w.tileAt(x, y)) {
                    case World.DIRT: c = 0xFF8C7450; break;
                    case World.ROCK: c = 0xFF5E5D59; break;
                    case World.WATER: c = 0xFF2F6A9A; break;
                    case World.TREE: c = 0xFF2E5A22; break;
                    default: c = 0xFF4E7A30;
                }
                g.color(c);
                g.fillRect(x * 4, y * 4, 4, 4);
            }
        }
    }

    // ---------------------------------------------------------------- mundo

    /** Desenha o mundo visto pela câmara (o chamador aplica a transformação). */
    public static void world(Gfx g, World w, float vx0, float vy0, float vx1, float vy1, float time, float cacheScale) {
        g.cached("terrain", (int) (w.W * T), (int) (w.H * T), cacheScale, true, new Gfx.Painter() {
            public void paint(Gfx gg) { paintTerrain(gg, w); }
        }, 0, 0, w.W * T, w.H * T);

        for (Sector s : w.sectors) {
            if (s.owner == World.NEUTRAL) continue;
            int c = teamColor(s.owner);
            float x = s.x0 * T, y = s.y0 * T, ww = (s.x1 - s.x0 + 1) * T, hh = (s.y1 - s.y0 + 1) * T;
            g.color(alpha(c, 0.07f + Math.max(0, s.flashT) * 0.15f));
            g.fillRect(x, y, ww, hh);
            g.color(alpha(c, 0.35f));
            g.strokeRect(x + 3, y + 3, ww - 6, hh - 6, 2);
        }

        for (Fx f : w.fx) if (f.kind == Fx.SCORCH && visible(f.x, f.y, f.size, vx0, vy0, vx1, vy1)) {
            float p = f.t / f.life;
            g.color(alpha(0xFF1A1410, 0.45f * (1 - p * p * p)));
            g.fillOval(f.x, f.y, f.size, f.size * 0.8f);
        }

        for (Building b : w.buildings) {
            if (!visible(b.x, b.y, 3 * T, vx0, vy0, vx1, vy1)) continue;
            building(g, b, time);
        }
        for (Sector s : w.sectors) if (visible(s.fx, s.fy, T, vx0, vy0, vx1, vy1)) flag(g, s, time);

        for (int pass = 0; pass < 2; pass++) {
            for (Unit u : w.units) {
                if (!u.alive || u.type.vehicle != (pass == 1)) continue;
                if (!visible(u.x, u.y, u.radius * 2, vx0, vy0, vx1, vy1)) continue;
                if (u.selected) {
                    g.color(0xFF7CFC6A);
                    g.strokeOval(u.x, u.y + 1, u.radius + 4, (u.radius + 4) * 0.85f, 1.6f);
                }
                g.save();
                g.translate(u.x, u.y);
                unitShape(g, u.type, u.team, u.angle, u.aim, u.anim, u.flash > 0);
                g.restore();
            }
        }

        for (Projectile p : w.shots) {
            if (!visible(p.x, p.y, 8, vx0, vy0, vx1, vy1)) continue;
            if (p.weapon.weapon == UnitType.W_ROCKET) {
                float a = (float) Math.atan2(p.vy, p.vx);
                g.save();
                g.translate(p.x, p.y);
                g.rotate(a);
                g.color(0xFFFFB347);
                g.fillCircle(-5, 0, 2.6f);
                g.color(0xFF505A48);
                g.fillRoundRect(-4, -1.6f, 9, 3.2f, 1.5f);
                g.restore();
            } else {
                g.color(0x66FFD27A);
                g.fillCircle(p.x, p.y, 4.5f);
                g.color(0xFF2A2A2A);
                g.fillCircle(p.x, p.y, 2.4f);
            }
        }

        for (Fx f : w.fx) {
            if (f.kind == Fx.SCORCH) continue;
            float reach = f.kind == Fx.TRACER ? 400 : f.size * 3;
            if (!visible(f.x, f.y, reach, vx0, vy0, vx1, vy1)) continue;
            effect(g, f);
        }

        for (Unit u : w.units) {
            if (!u.alive || u.team == World.NEUTRAL) continue;
            if (!(u.selected || u.hp < u.maxHp)) continue;
            if (!visible(u.x, u.y, u.radius * 2, vx0, vy0, vx1, vy1)) continue;
            hpBar(g, u.x, u.y - u.radius - 7, Math.max(18, u.radius * 2), u.hp / u.maxHp);
        }
        for (Building b : w.buildings) {
            if (!b.alive || b.isFactory() || b.team == World.NEUTRAL) continue;
            if (b.kind == Building.TURRET && b.hp >= b.maxHp) continue;
            if (!visible(b.x, b.y, 3 * T, vx0, vy0, vx1, vy1)) continue;
            hpBar(g, b.x, b.ty * T - 6, b.tw * T * 0.8f, b.hp / b.maxHp);
        }
    }

    static boolean visible(float x, float y, float r, float vx0, float vy0, float vx1, float vy1) {
        return x + r >= vx0 && x - r <= vx1 && y + r >= vy0 && y - r <= vy1;
    }

    static void hpBar(Gfx g, float cx, float y, float w, float f) {
        g.color(0xAA000000);
        g.fillRect(cx - w / 2 - 1, y - 1, w + 2, 5);
        g.color(f > 0.6f ? 0xFF6BE05A : f > 0.3f ? 0xFFF0D040 : 0xFFF04A3A);
        g.fillRect(cx - w / 2, y, w * Math.max(0, f), 3);
    }

    static void effect(Gfx g, Fx f) {
        float p = f.t / f.life;
        switch (f.kind) {
            case Fx.TRACER:
                g.color(alpha(f.color, 1 - p));
                g.line(f.x, f.y, f.x2, f.y2, 1.6f);
                break;
            case Fx.FLASH:
                g.color(alpha(0xFFFFE9A0, 1 - p));
                g.fillCircle(f.x, f.y, f.size * (1.2f - p * 0.5f));
                break;
            case Fx.BOOM: {
                float r = f.size * (0.35f + 0.75f * p);
                g.color(alpha(0xFFFF7A1A, 0.9f * (1 - p)));
                g.fillCircle(f.x, f.y, r);
                g.color(alpha(0xFFFFE070, 1 - p * 1.6f));
                g.fillCircle(f.x, f.y, r * 0.55f * (1 - p));
                g.color(alpha(0xFF3A2A20, 0.5f * (1 - p)));
                g.strokeCircle(f.x, f.y, r * 1.1f, 2 + f.size * 0.1f);
                break;
            }
            case Fx.SMOKE:
                g.color(alpha(0xFF6E6A66, 0.45f * (1 - p)));
                g.fillCircle(f.x, f.y, f.size * (0.6f + p));
                break;
            case Fx.RING:
                g.color(alpha(f.color, 1 - p));
                g.strokeCircle(f.x, f.y, f.size * p, 3);
                break;
            case Fx.DEBRIS:
                g.color(alpha(0xFF2A2622, 1 - p));
                g.fillRect(f.x - f.size / 2, f.y - f.size / 2, f.size, f.size);
                break;
        }
    }

    // ---------------------------------------------------------------- bandeira

    static void flag(Gfx g, Sector s, float time) {
        float x = s.fx, y = s.fy;
        int c = teamColor(s.owner);
        if (s.owner == World.NEUTRAL) c = 0xFFE6E6DE;
        g.color(0x55000000);
        g.fillOval(x + 3, y + 2, 9, 5);
        g.color(0xFF7A7468);
        g.fillCircle(x, y, 6);
        g.color(0xFF4A453E);
        g.fillCircle(x, y, 3);
        float top = y - 28;
        g.color(0xFF2C2A26);
        g.line(x, y, x, top, 2.4f);
        int n = 6;
        int k = 0;
        for (int i = 0; i <= n; i++) {
            float f = (float) i / n;
            float wave = (float) Math.sin(time * 6 + f * 4 + s.idx) * 2.2f * f;
            POLY[k++] = x + f * 20;
            POLY[k++] = top + f * 4 + wave;
        }
        for (int i = n; i >= 0; i--) {
            float f = (float) i / n;
            float wave = (float) Math.sin(time * 6 + f * 4 + s.idx) * 2.2f * f;
            POLY[k++] = x + f * 20;
            POLY[k++] = top + 13 - f * 4 + wave;
        }
        g.color(shade(c, 0.6f));
        for (int i = 0; i < k; i += 2) POLY[i + 1] += 1.2f;
        g.fillPoly(POLY, k / 2);
        for (int i = 0; i < k; i += 2) POLY[i + 1] -= 1.2f;
        g.color(c);
        g.fillPoly(POLY, k / 2);
        g.color(0xFFF2E6C8);
        g.fillCircle(x, top - 1, 2.2f);
    }

    // ---------------------------------------------------------------- edifícios

    static void building(Gfx g, Building b, float time) {
        float x = b.tx * T, y = b.ty * T, w = b.tw * T, h = b.th * T;
        int c = teamColor(b.team);
        if (b.kind == Building.TURRET) { turret(g, b, c); return; }
        if (b.kind == Building.FORT) { fort(g, b, c, time); return; }
        // fábrica
        g.color(0x55000000);
        g.fillRect(x + 5, y + 6, w, h);
        g.color(0xFF3E4248);
        g.fillRoundRect(x + 1, y + 1, w - 2, h - 2, 4);
        g.color(0xFF7D838C);
        g.fillRoundRect(x + 3, y + 3, w - 6, h - 6, 3);
        g.color(0xFF62686F);
        g.fillRect(x + 6, y + h * 0.28f, w - 12, h * 0.52f);
        g.color(0x33FFFFFF);
        for (float lx = x + 10; lx < x + w - 8; lx += 9) g.line(lx, y + h * 0.3f, lx, y + h * 0.78f, 1.2f);
        // faixa da equipa
        g.color(c);
        g.fillRect(x + 3, b.doorUp ? y + h - 3 - h * 0.17f : y + 3, w - 6, h * 0.17f);
        // porta
        float dw = w * 0.32f, dh = h * 0.2f;
        float dx = x + (w - dw) / 2, dy = b.doorUp ? y + 1 : y + h - dh - 1;
        g.color(0xFF24272B);
        g.fillRect(dx, dy, dw, dh);
        g.color(shade(c, 0.8f));
        g.strokeRect(dx, dy, dw, dh, 1.5f);
        g.color(0x55FFD860);
        for (float lx = dx + 3; lx < dx + dw - 2; lx += 6) g.line(lx, dy + 2, lx + 3, dy + dh - 2, 1.2f);
        // chaminé
        float chx = x + w * 0.82f, chy = y + h * 0.4f;
        g.color(0xFF2E3135);
        g.fillCircle(chx, chy, 6);
        g.color(0xFF555A60);
        g.fillCircle(chx, chy, 3.5f);
        // ícone no telhado
        float ix = x + w * 0.38f, iy = y + h * 0.52f;
        g.color(0xFF2E3135);
        g.fillCircle(ix, iy, 9);
        g.color(b.team == World.NEUTRAL ? 0xFFBFC2C6 : c);
        if (b.kind == Building.FACT_ROBOT) {
            g.fillRoundRect(ix - 5, iy - 5, 10, 9, 2.5f);
            g.color(0xFF2E3135);
            g.fillRect(ix - 3.5f, iy - 2.5f, 7, 2.2f);
            g.color(b.team == World.NEUTRAL ? 0xFFBFC2C6 : c);
            g.line(ix, iy - 5, ix, iy - 7.5f, 1.4f);
        } else {
            g.strokeCircle(ix, iy, 5.5f, 2.2f);
            g.fillCircle(ix, iy, 2);
            for (int i = 0; i < 6; i++) {
                float a = i * 1.0472f + time * 0.5f;
                g.line(ix + (float) Math.cos(a) * 5.5f, iy + (float) Math.sin(a) * 5.5f,
                       ix + (float) Math.cos(a) * 8, iy + (float) Math.sin(a) * 8, 2.2f);
            }
        }
        if (b.team != World.NEUTRAL) {
            float by = b.doorUp ? y + h + 4 : y - 7;
            g.color(0xAA000000);
            g.fillRect(x + 4, by, w - 8, 4);
            g.color(c);
            g.fillRect(x + 5, by + 1, (w - 10) * Math.min(1, b.progress), 2);
        }
    }

    private static void turret(Gfx g, Building b, int c) {
        float cx = b.x, cy = b.y, r = b.tw * T * 0.46f;
        g.color(0x55000000);
        g.fillCircle(cx + 4, cy + 5, r);
        if (!b.alive) {
            g.color(0xFF3B3835);
            octagon(g, cx, cy, r * 0.9f);
            g.color(0xFF55504A);
            g.fillCircle(cx - 6, cy - 4, 5);
            g.fillCircle(cx + 7, cy + 3, 6);
            g.color(0xFF2A2725);
            g.line(cx - 10, cy + 8, cx + 4, cy - 9, 2.5f);
            return;
        }
        g.color(0xFF4A4D52);
        octagon(g, cx, cy, r);
        g.color(0xFF6E737A);
        octagon(g, cx, cy, r * 0.82f);
        g.color(b.team == World.NEUTRAL ? 0xFF8A8A84 : c);
        g.strokeCircle(cx, cy, r * 0.62f, 3);
        g.save();
        g.translate(cx, cy);
        g.rotate(b.aim);
        g.color(0xFF2E3135);
        g.fillRect(4, -5.5f, r * 1.15f, 3.6f);
        g.fillRect(4, 2, r * 1.15f, 3.6f);
        g.color(b.flash > 0 ? 0xFFFFFFFF : 0xFF5A5F66);
        g.fillCircle(0, 0, r * 0.48f);
        g.color(b.team == World.NEUTRAL ? 0xFF8A8A84 : shade(c, 0.85f));
        g.fillRoundRect(-r * 0.25f, -r * 0.2f, r * 0.45f, r * 0.4f, 2);
        g.restore();
    }

    private static void octagon(Gfx g, float cx, float cy, float r) {
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 8 + i * Math.PI / 4;
            POLY[i * 2] = cx + (float) Math.cos(a) * r;
            POLY[i * 2 + 1] = cy + (float) Math.sin(a) * r;
        }
        g.fillPoly(POLY, 8);
    }

    private static void fort(Gfx g, Building b, int c, float time) {
        float x = b.tx * T, y = b.ty * T, w = b.tw * T, h = b.th * T;
        g.color(0x66000000);
        g.fillRect(x + 6, y + 7, w, h);
        if (!b.alive) {
            g.color(0xFF35322F);
            g.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 6);
            g.color(0xFF4E4A45);
            for (int i = 0; i < 7; i++) g.fillCircle(x + 12 + (i * 37 % 70), y + 14 + (i * 23 % 64), 6 + i % 3 * 2);
            return;
        }
        g.color(0xFF3A3D42);
        g.fillRoundRect(x, y, w, h, 6);
        g.color(0xFF767C84);
        g.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 5);
        g.color(0xFF5B6168);
        g.fillRoundRect(x + 14, y + 14, w - 28, h - 28, 4);
        // torres
        float[][] corners = {{x + 9, y + 9}, {x + w - 9, y + 9}, {x + 9, y + h - 9}, {x + w - 9, y + h - 9}};
        for (float[] p : corners) {
            g.color(0xFF3A3D42);
            g.fillCircle(p[0], p[1], 11);
            g.color(0xFF8A9098);
            g.fillCircle(p[0], p[1], 8.5f);
            g.color(c);
            g.strokeCircle(p[0], p[1], 6, 2.2f);
        }
        // cúpula e arma
        float cx = b.x, cy = b.y;
        g.save();
        g.translate(cx, cy);
        g.rotate(b.aim);
        g.color(0xFF2E3135);
        g.fillRect(6, -2.5f, 24, 5);
        g.restore();
        g.color(b.flash > 0 ? 0xFFFFFFFF : shade(c, 0.75f));
        g.fillCircle(cx, cy, 14);
        g.color(c);
        g.fillCircle(cx, cy, 10.5f);
        g.color(0x55FFFFFF);
        g.fillCircle(cx - 3.5f, cy - 4, 4);
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 3);
        g.color(alpha(0xFFFFFFFF, 0.3f + 0.4f * pulse));
        g.fillCircle(cx, cy, 2.5f);
    }

    // ---------------------------------------------------------------- unidades

    /** Desenha uma unidade centrada em (0, 0). Usado no mapa e nos ícones do painel. */
    public static void unitShape(Gfx g, UnitType t, int team, float angle, float aim, float anim, boolean hit) {
        int c = teamColor(team);
        int dk = shade(c, 0.5f);
        float r = t.radius;
        g.color(0x40000000);
        g.fillOval(3, 4, r * 1.05f, r * 0.95f);
        if (!t.vehicle) {
            g.save();
            g.rotate(aim);
            float step = (float) Math.sin(anim * 0.35f) * r * 0.45f;
            g.color(0xFF2B2D30);
            g.fillOval(step, -r * 0.5f, r * 0.36f, r * 0.24f);
            g.fillOval(-step, r * 0.5f, r * 0.36f, r * 0.24f);
            // arma
            if (t == UnitType.ATIRADOR) {
                g.color(0xFF1E2022);
                g.line(-r * 0.2f, r * 0.45f, r * 1.9f, r * 0.45f, 2.4f);
                g.color(0xFF46494D);
                g.fillRect(r * 0.4f, r * 0.3f, r * 0.5f, r * 0.3f);
            } else if (t == UnitType.BAZUCA) {
                g.color(0xFF4F5A3A);
                g.fillRoundRect(-r * 0.8f, -r * 0.78f, r * 2.0f, r * 0.42f, r * 0.2f);
                g.color(0xFF2A2E22);
                g.fillRect(r * 1.05f, -r * 0.8f, r * 0.15f, r * 0.46f);
            } else {
                g.color(0xFF1E2022);
                g.line(r * 0.2f, r * 0.45f, r * 1.25f, r * 0.45f, 2.6f);
            }
            // corpo
            g.color(hit ? 0xFFFFFFFF : dk);
            g.fillOval(0, 0, r * 0.82f, r * 1.02f);
            g.color(hit ? 0xFFFFFFFF : c);
            g.fillOval(-r * 0.05f, 0, r * 0.68f, r * 0.88f);
            if (t == UnitType.BAZUCA) {
                g.color(dk);
                g.fillCircle(0, -r * 0.62f, r * 0.26f);
                g.fillCircle(0, r * 0.62f, r * 0.26f);
            }
            // cabeça e visor
            g.color(t == UnitType.ATIRADOR ? 0xFF6D7176 : 0xFFC9CDD2);
            g.fillCircle(r * 0.08f, 0, r * 0.44f);
            g.color(t == UnitType.ATIRADOR ? 0xFFFF5A4A : shade(c, 1.45f));
            g.fillRoundRect(r * 0.24f, -r * 0.3f, r * 0.2f, r * 0.6f, r * 0.08f);
            g.restore();
            return;
        }
        boolean neutral = team == World.NEUTRAL;
        int body = neutral ? 0xFF8A8A80 : c;
        int bodyDk = shade(body, 0.55f);
        g.save();
        g.rotate(angle);
        if (t == UnitType.JIPE) {
            g.color(0xFF1C1D1F);
            g.fillRoundRect(r * 0.3f, -r * 0.82f, r * 0.46f, r * 0.26f, 2);
            g.fillRoundRect(r * 0.3f, r * 0.56f, r * 0.46f, r * 0.26f, 2);
            g.fillRoundRect(-r * 0.78f, -r * 0.82f, r * 0.46f, r * 0.26f, 2);
            g.fillRoundRect(-r * 0.78f, r * 0.56f, r * 0.46f, r * 0.26f, 2);
            g.color(hit ? 0xFFFFFFFF : bodyDk);
            g.fillRoundRect(-r * 0.92f, -r * 0.6f, r * 1.84f, r * 1.2f, r * 0.3f);
            g.color(hit ? 0xFFFFFFFF : body);
            g.fillRoundRect(-r * 0.84f, -r * 0.52f, r * 1.68f, r * 1.04f, r * 0.26f);
            g.color(0xFF2A3A44);
            g.fillRoundRect(r * 0.18f, -r * 0.44f, r * 0.2f, r * 0.88f, 2);
            g.color(bodyDk);
            g.fillRect(r * 0.5f, -r * 0.08f, r * 0.3f, r * 0.16f);
            g.restore();
            g.save();
            g.rotate(aim);
            g.color(0xFF1E2022);
            g.line(-r * 0.2f, 0, r * 0.95f, 0, 2.6f);
            g.color(0xFF46494D);
            g.fillCircle(-r * 0.25f, 0, r * 0.22f);
            g.restore();
            return;
        }
        // tanque
        g.color(0xFF1C1D1F);
        g.fillRoundRect(-r * 0.95f, -r * 0.88f, r * 1.9f, r * 0.38f, 3);
        g.fillRoundRect(-r * 0.95f, r * 0.5f, r * 1.9f, r * 0.38f, 3);
        g.color(0xFF3D3F42);
        float off = (anim * 0.5f) % 6f;
        for (float lx = -r * 0.9f + off; lx < r * 0.9f; lx += 6) {
            g.line(lx, -r * 0.86f, lx, -r * 0.52f, 1.4f);
            g.line(lx, r * 0.52f, lx, r * 0.86f, 1.4f);
        }
        g.color(hit ? 0xFFFFFFFF : bodyDk);
        g.fillRoundRect(-r * 0.86f, -r * 0.58f, r * 1.72f, r * 1.16f, 4);
        g.color(hit ? 0xFFFFFFFF : body);
        g.fillRoundRect(-r * 0.78f, -r * 0.5f, r * 1.56f, r * 1.0f, 3.5f);
        g.color(0x30000000);
        g.fillRect(-r * 0.78f, r * 0.2f, r * 1.56f, r * 0.3f);
        g.restore();
        g.save();
        g.rotate(aim);
        g.color(0xFF1E2022);
        g.fillRoundRect(0, -r * 0.12f, r * 1.35f, r * 0.24f, 2);
        g.fillRect(r * 1.2f, -r * 0.17f, r * 0.2f, r * 0.34f);
        g.color(hit ? 0xFFFFFFFF : bodyDk);
        g.fillCircle(0, 0, r * 0.47f);
        g.color(hit ? 0xFFFFFFFF : shade(body, 0.85f));
        g.fillCircle(0, 0, r * 0.4f);
        g.color(bodyDk);
        g.fillCircle(-r * 0.12f, -r * 0.12f, r * 0.13f);
        g.restore();
    }

    // ---------------------------------------------------------------- minimapa e comandante

    public static void minimap(Gfx g, World w, float x, float y, float mw, float mh,
                               float camX, float camY, float viewW, float viewH) {
        g.color(0xCC101214);
        g.fillRoundRect(x - 4, y - 4, mw + 8, mh + 8, 6);
        g.cached("mini", w.W * 4, w.H * 4, 1, true, new Gfx.Painter() {
            public void paint(Gfx gg) { paintMini(gg, w); }
        }, x, y, mw, mh);
        float sx = mw / (w.W * T), sy = mh / (w.H * T);
        for (Sector s : w.sectors) {
            if (s.owner == World.NEUTRAL) continue;
            g.color(alpha(teamColor(s.owner), 0.28f));
            g.fillRect(x + s.x0 * T * sx, y + s.y0 * T * sy, (s.x1 - s.x0 + 1) * T * sx, (s.y1 - s.y0 + 1) * T * sy);
        }
        for (Building b : w.buildings) {
            if (!b.alive) continue;
            g.color(b.team == World.NEUTRAL ? 0xFFDDDDDD : teamColor(b.team));
            g.fillRect(x + b.tx * T * sx, y + b.ty * T * sy, b.tw * T * sx, b.th * T * sy);
        }
        for (Sector s : w.sectors) {
            g.color(0xFF000000);
            g.fillCircle(x + s.fx * sx, y + s.fy * sy, 3.2f);
            g.color(s.owner == World.NEUTRAL ? 0xFFFFFFFF : teamColor(s.owner));
            g.fillCircle(x + s.fx * sx, y + s.fy * sy, 2.2f);
        }
        for (Unit u : w.units) {
            if (!u.alive) continue;
            g.color(u.team == World.NEUTRAL ? 0xFFCCCCCC : u.selected ? 0xFF7CFC6A : shade(teamColor(u.team), 1.25f));
            float d = u.type.vehicle ? 2.4f : 1.8f;
            g.fillRect(x + u.x * sx - d / 2, y + u.y * sy - d / 2, d, d);
        }
        g.color(0xDDFFFFFF);
        g.strokeRect(x + camX * sx, y + camY * sy, viewW * sx, viewH * sy, 1.4f);
    }

    /** Retrato do Comandante Brix (robô veterano do lado laranja). */
    public static void brix(Gfx g, float cx, float cy, float s, float time) {
        g.color(0xFF2C2E31);
        g.line(cx + s * 0.18f, cy - s * 0.42f, cx + s * 0.28f, cy - s * 0.72f, s * 0.05f);
        boolean blink = (time % 1.6f) < 0.8f;
        g.color(blink ? 0xFFFF5A3A : 0xFF7A2A1A);
        g.fillCircle(cx + s * 0.28f, cy - s * 0.74f, s * 0.07f);
        g.color(0xFF2C2E31);
        g.fillRoundRect(cx - s * 0.52f, cy - s * 0.47f, s * 1.04f, s * 0.98f, s * 0.2f);
        g.color(0xFF9AA3AD);
        g.fillRoundRect(cx - s * 0.47f, cy - s * 0.42f, s * 0.94f, s * 0.88f, s * 0.17f);
        g.color(C_PLAYER);
        g.fillRect(cx - s * 0.47f, cy - s * 0.42f, s * 0.94f, s * 0.12f);
        g.color(0xFF6F7780);
        g.fillCircle(cx - s * 0.52f, cy, s * 0.1f);
        g.fillCircle(cx + s * 0.52f, cy, s * 0.1f);
        g.color(0xFF1B1D20);
        g.fillRoundRect(cx - s * 0.36f, cy - s * 0.2f, s * 0.72f, s * 0.22f, s * 0.1f);
        float look = (float) Math.sin(time * 0.9f) * s * 0.12f;
        g.color(0xFFFFB347);
        g.fillRoundRect(cx - s * 0.13f + look, cy - s * 0.16f, s * 0.26f, s * 0.14f, s * 0.06f);
        g.color(0xFF4E555C);
        g.fillRoundRect(cx - s * 0.28f, cy + s * 0.14f, s * 0.56f, s * 0.2f, s * 0.05f);
        g.color(0xFF2C2E31);
        for (int i = 0; i < 4; i++) {
            float lx = cx - s * 0.2f + i * s * 0.133f;
            g.line(lx, cy + s * 0.16f, lx, cy + s * 0.32f, s * 0.03f);
        }
        g.color(0xFF5A626B);
        g.line(cx - s * 0.36f, cy - s * 0.3f, cx - s * 0.16f, cy - s * 0.27f, s * 0.04f);
    }
}
