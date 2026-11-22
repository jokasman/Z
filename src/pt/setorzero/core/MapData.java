package pt.setorzero.core;

import java.util.ArrayDeque;

/**
 * Ferramentas para construir os mapas. Por omissão tudo é espelhado por rotação de 180°
 * (o que se põe num lado aparece no lado oposto, com a equipa trocada), para os mapas serem justos.
 */
public final class MapData {
    final World w;
    final int W, H;

    MapData(World w) {
        this.w = w;
        this.W = w.W;
        this.H = w.H;
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                setRaw(x, y, (x == 0 || y == 0 || x == W - 1 || y == H - 1) ? World.ROCK : World.GRASS);
    }

    static int mirrorTeam(int team) { return team == 0 ? 0 : 3 - team; }

    private void setRaw(int x, int y, int code) {
        int i = y * W + x;
        w.terrain[i] = (byte) code;
        w.blocked[i] = code == World.ROCK || code == World.WATER || code == World.TREE;
    }

    void set(int x, int y, int code, boolean onlyGround) {
        if (x <= 0 || y <= 0 || x >= W - 1 || y >= H - 1) return;
        if (onlyGround && w.tileAt(x, y) != World.GRASS) return;
        setRaw(x, y, code);
    }

    void setM(int x, int y, int code, boolean onlyGround) {
        set(x, y, code, onlyGround);
        set(W - 1 - x, H - 1 - y, code, onlyGround);
    }

    // ---------------------------------------------------------------- terreno

    MapData rect(int code, int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) setM(x, y, code, false);
        return this;
    }

    MapData blob(int code, float cx, float cy, float rx, float ry) { return blob(code, cx, cy, rx, ry, false); }

    MapData blob(int code, float cx, float cy, float rx, float ry, boolean onlyGround) {
        for (int y = (int) (cy - ry - 1); y <= cy + ry + 1; y++)
            for (int x = (int) (cx - rx - 1); x <= cx + rx + 1; x++) {
                float dx = (x + 0.5f - cx) / rx, dy = (y + 0.5f - cy) / ry;
                if (dx * dx + dy * dy <= 1) setM(x, y, code, onlyGround);
            }
        return this;
    }

    /** Linha grossa (rios, estradas, cordilheiras). */
    MapData line(int code, float x0, float y0, float x1, float y1, float width) {
        float d = World.dist(x0, y0, x1, y1);
        int steps = Math.max(1, (int) (d * 3));
        for (int i = 0; i <= steps; i++) {
            float f = (float) i / steps;
            blob(code, x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, width / 2, width / 2);
        }
        return this;
    }

    /** Manchas espalhadas ao acaso (sempre iguais para a mesma semente). */
    MapData scatter(int code, int count, float rmin, float rmax, long seed) {
        java.util.Random r = new java.util.Random(seed);
        for (int i = 0; i < count; i++) {
            float cx = 2 + r.nextFloat() * (W - 4), cy = 2 + r.nextFloat() * (H / 2f);
            blob(code, cx, cy, rmin + r.nextFloat() * (rmax - rmin), rmin + r.nextFloat() * (rmax - rmin), code == World.DIRT);
        }
        return this;
    }

    /** Preenche com um tipo onde o ruído passa o limiar (florestas, ruínas). */
    MapData noise(int code, float threshold, int scale, int k) {
        for (int y = 1; y < H / 2 + 1; y++)
            for (int x = 1; x < W - 1; x++)
                if (TerrainArt.smooth(x * 16, y * 16, scale * 16, k) > threshold) setM(x, y, code, true);
        return this;
    }

    MapData dirtPatches(int count, long seed) { return scatter(World.DIRT, count, 0.9f, 2.4f, seed); }

    // ---------------------------------------------------------------- setores e bandeiras

    MapData sectors(int cols, int rows, String... names) {
        int sw = W / cols, sh = H / rows;
        w.sectors = new Sector[cols * rows];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int i = r * cols + c;
                w.sectors[i] = new Sector(i, names[i], c * sw, r * sh, c * sw + sw - 1, r * sh + sh - 1);
            }
        return this;
    }

    /** Bandeira num setor (e a espelhada no setor oposto). */
    MapData flag(int sector, int tx, int ty) {
        int n = w.sectors.length;
        Sector s = w.sectors[sector], m = w.sectors[n - 1 - sector];
        s.fx = (tx + 0.5f) * World.TILE;
        s.fy = (ty + 0.5f) * World.TILE;
        m.fx = (W - 1 - tx + 0.5f) * World.TILE;
        m.fy = (H - 1 - ty + 0.5f) * World.TILE;
        clear(tx - 1, ty - 1, tx + 1, ty + 1, World.DIRT);
        return this;
    }

    /** Bandeira no centro exato do mapa (setor do meio, em mapas de 3×3). */
    MapData centerFlag(int sector) {
        Sector s = w.sectors[sector];
        s.fx = W / 2f * World.TILE;
        s.fy = H / 2f * World.TILE;
        clear(W / 2 - 2, H / 2 - 2, W / 2 + 1, H / 2 + 1, World.DIRT);
        return this;
    }

    void clear(int x0, int y0, int x1, int y1, int code) {
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                if (x <= 0 || y <= 0 || x >= W - 1 || y >= H - 1) continue;
                int c = w.tileAt(x, y);
                int to = c == World.BRIDGE ? World.BRIDGE : (c == World.DIRT || code == World.DIRT) ? World.DIRT : World.GRASS;
                setM(x, y, to, false);
            }
    }

    MapData bridge(int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) setM(x, y, World.BRIDGE, false);
        return this;
    }

    // ---------------------------------------------------------------- edifícios e veículos

    static int tw(int kind) { return kind == Building.FORT ? 3 : kind == Building.TURRET ? 2 : 3; }

    static int th(int kind) { return kind == Building.FORT ? 3 : 2; }

    /** Edifício e o seu espelho (equipa trocada). */
    MapData place(int kind, int tx, int ty, int team) {
        int tw = tw(kind), th = th(kind);
        clear(tx - 1, ty - 1, tx + tw, ty + th + (kind == Building.TURRET ? 0 : 1), World.GRASS);
        if (kind != Building.TURRET) clear(tx, ty + th, tx + tw - 1, ty + th + 1, World.DIRT);
        Building a = w.addBuilding(kind, tx, ty, team);
        Building b = w.addBuilding(kind, W - tx - tw, H - ty - th, mirrorTeam(team));
        b.doorUp = true;
        if (kind == Building.FORT) {
            a.aim = (float) (Math.PI * 0.75);
            b.aim = (float) (-Math.PI * 0.25);
        }
        return this;
    }

    /** Edifício só de um lado (para níveis desequilibrados de propósito). */
    MapData placeOne(int kind, int tx, int ty, int team) {
        int tw = tw(kind), th = th(kind);
        for (int y = ty - 1; y <= ty + th; y++)
            for (int x = tx - 1; x <= tx + tw; x++)
                if (x > 0 && y > 0 && x < W - 1 && y < H - 1 && w.tileAt(x, y) != World.BRIDGE) setRaw(x, y, World.GRASS);
        Building b = w.addBuilding(kind, tx, ty, team);
        b.doorUp = ty > H / 2;
        return this;
    }

    /** Base padrão da equipa inimiga no canto superior direito (e a do jogador no canto oposto). */
    MapData homes() {
        place(Building.FORT, W - 7, 2, World.ENEMY);
        place(Building.FACT_ROBOT, W - 14, 3, World.ENEMY);
        place(Building.TURRET, W - 7, 9, World.ENEMY);
        int n = w.sectors.length;
        int cols = W / (w.sectors[0].x1 - w.sectors[0].x0 + 1);
        flag(cols - 1, W - 10, 8);
        return this;
    }

    MapData vehicle(UnitType t, int tx, int ty) {
        clear(tx - 1, ty - 1, tx + 1, ty + 1, World.GRASS);
        for (int k = 0; k < 2; k++) {
            float x = ((k == 0 ? tx : W - 1 - tx) + 0.5f) * World.TILE;
            float y = ((k == 0 ? ty : H - 1 - ty) + 0.5f) * World.TILE;
            Unit u = w.addUnit(t, World.NEUTRAL, x, y);
            u.crew = null;
            u.hp = u.maxHp * 0.7f;
            u.angle = u.aim = k == 0 ? 0.6f : 0.6f + (float) Math.PI;
        }
        return this;
    }

    // ---------------------------------------------------------------- acabamento

    /** Liga setores aos edifícios, define donos iniciais, garante caminhos e coloca as tropas. */
    void finish(UnitType[] playerExtra, UnitType[] enemyExtra, int difficulty) {
        for (Building b : w.buildings) {
            b.sector = w.sectorAt(b.x, b.y);
            b.sector.buildings.add(b);
        }
        for (Sector s : w.sectors) {
            s.owner = World.NEUTRAL;
            for (Building b : s.buildings) if (b.kind == Building.FORT) s.owner = b.team;
        }
        for (Building b : w.buildings) if (b.team != World.NEUTRAL && b.sector.owner == World.NEUTRAL) b.team = World.NEUTRAL;
        for (Building b : w.buildings) if (b.sector.owner != World.NEUTRAL && b.kind != Building.FORT) b.team = b.sector.owner;
        connect();

        UnitType[] start = {UnitType.BATEDOR, UnitType.BATEDOR, UnitType.BATEDOR, UnitType.ATIRADOR, UnitType.BAZUCA, UnitType.JIPE};
        for (int team = 1; team <= 2; team++) {
            Building f = w.forts[team];
            // concentra as tropas à frente do forte, do lado do centro do mapa
            float cx = f.x + Math.signum(W * World.TILE / 2 - f.x) * 3.5f * World.TILE;
            float cy = f.y + Math.signum(H * World.TILE / 2 - f.y) * 3.5f * World.TILE;
            spawn(team, start, cx, cy);
            spawn(team, team == World.PLAYER ? playerExtra : enemyExtra, cx, cy);
            if (team == World.ENEMY && difficulty >= 2) spawn(team, new UnitType[]{UnitType.BATEDOR, UnitType.BATEDOR}, cx, cy);
        }
    }

    private void spawn(int team, UnitType[] list, float cx, float cy) {
        if (list == null) return;
        for (UnitType t : list) {
            float[] p = w.freeSpot(cx, cy, t.radius);
            Unit u = w.addUnit(t, team, p[0], p[1]);
            u.angle = u.aim = (float) Math.atan2(H * World.TILE / 2 - p[1], W * World.TILE / 2 - p[0]);
        }
    }

    /** Abre caminho (terra ou ponte) até tudo o que tem de ser alcançável a partir da base do jogador. */
    void connect() {
        for (int pass = 0; pass < 40; pass++) {
            boolean[] seen = reach();
            int[] target = null;
            for (Sector s : w.sectors) {
                int tx = (int) (s.fx / World.TILE), ty = (int) (s.fy / World.TILE);
                if (!seen[ty * W + tx]) { target = new int[]{tx, ty}; break; }
            }
            if (target == null) {
                for (Building b : w.buildings) {
                    int tx = (int) (b.isFactory() ? b.doorX() : b.x) / (int) World.TILE;
                    int ty = (int) (b.isFactory() ? b.doorY() : b.y + b.th * World.TILE / 2 + World.TILE / 2) / (int) World.TILE;
                    if (!b.isFactory()) ty = Math.min(H - 2, ty);
                    if (!w.blockedTile(tx, ty) && !seen[ty * W + tx]) { target = new int[]{tx, ty}; break; }
                }
            }
            if (target == null) return;
            // ponto alcançável mais próximo
            int best = -1;
            float bd = Float.MAX_VALUE;
            for (int i = 0; i < W * H; i++) {
                if (!seen[i]) continue;
                float d = World.dist(i % W, i / W, target[0], target[1]);
                if (d < bd) { bd = d; best = i; }
            }
            if (best < 0) return;
            carve(best % W, best / W, target[0], target[1]);
        }
    }

    private void carve(int x0, int y0, int x1, int y1) {
        float d = World.dist(x0, y0, x1, y1);
        int steps = Math.max(1, (int) (d * 2));
        for (int i = 0; i <= steps; i++) {
            float f = (float) i / steps;
            int x = Math.round(x0 + (x1 - x0) * f), y = Math.round(y0 + (y1 - y0) * f);
            for (int dy = 0; dy <= 1; dy++)
                for (int dx = 0; dx <= 1; dx++) {
                    int ax = x + dx, ay = y + dy;
                    if (ax <= 0 || ay <= 0 || ax >= W - 1 || ay >= H - 1) continue;
                    if (isBuilding(ax, ay)) continue;
                    int c = w.tileAt(ax, ay);
                    if (c == World.WATER) setRaw(ax, ay, World.BRIDGE);
                    else if (c == World.ROCK || c == World.TREE) setRaw(ax, ay, World.DIRT);
                }
        }
    }

    private boolean isBuilding(int x, int y) {
        for (Building b : w.buildings) if (x >= b.tx && x < b.tx + b.tw && y >= b.ty && y < b.ty + b.th) return true;
        return false;
    }

    boolean[] reach() {
        boolean[] seen = new boolean[W * H];
        Building f = w.forts[World.PLAYER];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int y = f.ty - 1; y <= f.ty + f.th; y++)
            for (int x = f.tx - 1; x <= f.tx + f.tw; x++)
                if (!w.blockedTile(x, y) && !seen[y * W + x]) { seen[y * W + x] = true; q.add(y * W + x); }
        while (!q.isEmpty()) {
            int n = q.poll();
            int nx = n % W, ny = n / W;
            int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] k : d) {
                int ax = nx + k[0], ay = ny + k[1];
                if (w.blockedTile(ax, ay)) continue;
                int m = ay * W + ax;
                if (seen[m]) continue;
                seen[m] = true;
                q.add(m);
            }
        }
        return seen;
    }
}
