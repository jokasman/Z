package pt.setorzero.core;

import java.util.Random;

/**
 * Mapa "Vale das Bandeiras": 48×36 quadrículas em 9 setores de 16×12, simétrico por rotação de 180°
 * (o que existe num lado existe no lado oposto, para nenhuma das equipas ter vantagem).
 */
final class MapData {
    static final String[] NAMES = {
        "Pedreira Norte", "Planalto Norte", "Base Ciano",
        "Vale Oeste", "Lago Central", "Vale Este",
        "Base Laranja", "Planalto Sul", "Pedreira Sul"
    };

    private MapData() { }

    static void build(World w, int difficulty) {
        int W = w.W, H = w.H;
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                w.terrain[y * W + x] = (byte) ((x == 0 || y == 0 || x == W - 1 || y == H - 1) ? World.ROCK : World.GRASS);

        // terra batida decorativa (passável), sempre a mesma em todas as partidas
        Random deco = new Random(7);
        for (int i = 0; i < 16; i++) {
            float cx = 2 + deco.nextFloat() * (W - 4), cy = 2 + deco.nextFloat() * (H / 2f);
            blob(w, World.DIRT, cx, cy, 1 + deco.nextFloat() * 2.2f, 0.8f + deco.nextFloat() * 1.4f, true);
        }

        // cumeeiras de rocha entre setores (com passagens)
        rect(w, World.ROCK, 15, 1, 16, 5);
        rect(w, World.ROCK, 31, 6, 32, 10);
        rect(w, World.ROCK, 1, 11, 7, 12);
        rect(w, World.ROCK, 35, 11, 40, 12);
        blob(w, World.ROCK, 44.5f, 8.5f, 1.5f, 1.2f, false);
        blob(w, World.ROCK, 24.5f, 12.5f, 2f, 0.9f, false);
        // água
        blob(w, World.WATER, 22f, 2.5f, 3f, 1.4f, false);
        blob(w, World.WATER, 20.5f, 18.5f, 1.6f, 2.4f, false);
        // árvores
        blob(w, World.TREE, 12.5f, 9.5f, 1.3f, 1.1f, false);
        blob(w, World.TREE, 2.5f, 7.5f, 1.2f, 1.6f, false);
        blob(w, World.TREE, 28f, 2f, 1.2f, 1f, false);
        blob(w, World.TREE, 2f, 20f, 1f, 1.5f, false);
        blob(w, World.TREE, 13f, 15.5f, 1.2f, 1.2f, false);

        // setores
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) {
                int i = r * 3 + c;
                w.sectors[i] = new Sector(i, NAMES[i], c * 16, r * 12, c * 16 + 15, r * 12 + 11);
            }
        flag(w, 0, 8, 8);
        flag(w, 1, 20, 7);
        flag(w, 2, 38, 8);
        flag(w, 3, 9, 19);
        Sector mid = w.sectors[4];
        mid.fx = 24 * World.TILE;
        mid.fy = 18 * World.TILE;
        clear(w, 22, 16, 25, 19, World.DIRT);

        place(w, Building.FORT, 41, 2, World.ENEMY);
        place(w, Building.FACT_ROBOT, 34, 3, World.ENEMY);
        place(w, Building.TURRET, 41, 9, World.ENEMY);
        place(w, Building.FACT_ROBOT, 4, 3, World.NEUTRAL);
        place(w, Building.TURRET, 25, 4, World.NEUTRAL);
        place(w, Building.FACT_VEH, 4, 15, World.NEUTRAL);
        place(w, Building.TURRET, 18, 13, World.NEUTRAL);
        w.sectors[2].owner = World.ENEMY;
        w.sectors[6].owner = World.PLAYER;

        // veículos abandonados
        abandoned(w, UnitType.JIPE, 12, 4);
        abandoned(w, UnitType.TANQUE, 28, 8);
        abandoned(w, UnitType.JIPE, 17, 20);

        // tropas iniciais
        UnitType[] start = {UnitType.BATEDOR, UnitType.BATEDOR, UnitType.BATEDOR, UnitType.ATIRADOR, UnitType.BAZUCA, UnitType.JIPE};
        for (int team = 1; team <= 2; team++) {
            float cx = (team == World.PLAYER ? 8.5f : 39.5f) * World.TILE;
            float cy = (team == World.PLAYER ? 28.5f : 7.5f) * World.TILE;
            for (UnitType t : start) {
                float[] p = w.freeSpot(cx, cy, t.radius);
                w.addUnit(t, team, p[0], p[1]);
            }
            if (team == World.ENEMY && difficulty >= 2) {
                for (int k = 0; k < 2; k++) {
                    float[] p = w.freeSpot(cx, cy, UnitType.BATEDOR.radius);
                    w.addUnit(UnitType.BATEDOR, team, p[0], p[1]);
                }
            }
        }
    }

    private static int mirrorTeam(int team) { return team == 0 ? 0 : 3 - team; }

    private static void set(World w, int x, int y, int code, boolean onlyGrass) {
        if (x <= 0 || y <= 0 || x >= w.W - 1 || y >= w.H - 1) return;
        int i = y * w.W + x;
        if (onlyGrass && w.terrain[i] != World.GRASS) return;
        w.terrain[i] = (byte) code;
        w.blocked[i] = code == World.ROCK || code == World.WATER || code == World.TREE;
    }

    private static void setM(World w, int x, int y, int code, boolean onlyGrass) {
        set(w, x, y, code, onlyGrass);
        set(w, w.W - 1 - x, w.H - 1 - y, code, onlyGrass);
    }

    private static void rect(World w, int code, int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) setM(w, x, y, code, false);
    }

    private static void blob(World w, int code, float cx, float cy, float rx, float ry, boolean onlyGrass) {
        for (int y = (int) (cy - ry - 1); y <= cy + ry + 1; y++)
            for (int x = (int) (cx - rx - 1); x <= cx + rx + 1; x++) {
                float dx = (x + 0.5f - cx) / rx, dy = (y + 0.5f - cy) / ry;
                if (dx * dx + dy * dy <= 1) setM(w, x, y, code, onlyGrass);
            }
    }

    private static void clear(World w, int x0, int y0, int x1, int y1, int code) {
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                if (x <= 0 || y <= 0 || x >= w.W - 1 || y >= w.H - 1) continue;
                int c = w.tileAt(x, y);
                setM(w, x, y, c == World.DIRT || code == World.DIRT ? World.DIRT : World.GRASS, false);
            }
    }

    private static void flag(World w, int sector, int tx, int ty) {
        Sector s = w.sectors[sector], m = w.sectors[8 - sector];
        s.fx = (tx + 0.5f) * World.TILE;
        s.fy = (ty + 0.5f) * World.TILE;
        m.fx = (w.W - 1 - tx + 0.5f) * World.TILE;
        m.fy = (w.H - 1 - ty + 0.5f) * World.TILE;
        clear(w, tx - 1, ty - 1, tx + 1, ty + 1, World.DIRT);
    }

    private static void place(World w, int kind, int tx, int ty, int team) {
        int tw = kind == Building.FORT ? 3 : kind == Building.TURRET ? 2 : 3;
        int th = kind == Building.FORT ? 3 : 2;
        clear(w, tx - 1, ty - 1, tx + tw, ty + th + (kind == Building.TURRET ? 0 : 1), World.GRASS);
        if (kind != Building.TURRET) clear(w, tx, ty + th, tx + tw - 1, ty + th + 1, World.DIRT);
        Building a = w.addBuilding(kind, tx, ty, team);
        Building b = w.addBuilding(kind, w.W - tx - tw, w.H - ty - th, mirrorTeam(team));
        a.sector = w.sectorAt(a.x, a.y);
        b.sector = w.sectorAt(b.x, b.y);
        b.doorUp = true;
        a.sector.buildings.add(a);
        b.sector.buildings.add(b);
        if (kind == Building.FORT) {
            a.aim = (float) (Math.PI * 0.75);
            b.aim = (float) (-Math.PI * 0.25);
        }
    }

    private static void abandoned(World w, UnitType t, int tx, int ty) {
        clear(w, tx - 1, ty - 1, tx + 1, ty + 1, World.GRASS);
        for (int k = 0; k < 2; k++) {
            float x = ((k == 0 ? tx : w.W - 1 - tx) + 0.5f) * World.TILE;
            float y = ((k == 0 ? ty : w.H - 1 - ty) + 0.5f) * World.TILE;
            Unit u = w.addUnit(t, World.NEUTRAL, x, y);
            u.crew = null;
            u.hp = u.maxHp * 0.7f;
            u.angle = u.aim = k == 0 ? 0.6f : 0.6f + (float) Math.PI;
        }
    }
}
