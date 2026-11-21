package pt.setorzero.core;

import java.util.ArrayDeque;
import java.util.ArrayList;

/** Testes da lógica: mapa ligado, captura, produção, ocupação de veículos e partidas IA contra IA. */
public final class SimTest {
    static int fails = 0;

    static void check(boolean ok, String what) {
        if (!ok) { fails++; System.out.println("FALHOU: " + what); }
        else System.out.println("ok: " + what);
    }

    public static void main(String[] a) {
        connectivity();
        symmetry();
        capture();
        production();
        boarding();
        combat();
        sounds();
        matches();
        System.out.println(fails == 0 ? "ALL PASS" : fails + " FALHAS");
        if (fails > 0) System.exit(1);
    }

    static boolean[] reach(World w, float x, float y) {
        boolean[] seen = new boolean[w.W * w.H];
        int s = ((int) (y / World.TILE)) * w.W + (int) (x / World.TILE);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        seen[s] = true;
        q.add(s);
        while (!q.isEmpty()) {
            int n = q.poll();
            int nx = n % w.W, ny = n / w.W;
            int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] k : d) {
                int ax = nx + k[0], ay = ny + k[1];
                if (w.blockedTile(ax, ay)) continue;
                int m = ay * w.W + ax;
                if (seen[m]) continue;
                seen[m] = true;
                q.add(m);
            }
        }
        return seen;
    }

    static boolean at(World w, boolean[] seen, float x, float y) {
        return seen[((int) (y / World.TILE)) * w.W + (int) (x / World.TILE)];
    }

    static void connectivity() {
        World w = new World(1, 1);
        Unit start = null;
        for (Unit u : w.units) if (u.team == World.PLAYER) { start = u; break; }
        boolean[] seen = reach(w, start.x, start.y);
        for (Sector s : w.sectors) check(at(w, seen, s.fx, s.fy) && !w.blockedTile((int) (s.fx / 32), (int) (s.fy / 32)), "bandeira acessível: " + s.name);
        for (Building b : w.buildings) {
            if (b.isFactory()) check(at(w, seen, b.doorX(), b.doorY()), "porta acessível: " + b.title() + " em " + b.sector.name);
            else {
                boolean any = false;
                for (int y = b.ty - 1; y <= b.ty + b.th; y++)
                    for (int x = b.tx - 1; x <= b.tx + b.tw; x++)
                        if (!w.blockedTile(x, y) && seen[y * w.W + x]) any = true;
                check(any, "acessível: " + b.title() + " em " + b.sector.name);
            }
        }
        for (Unit u : w.units) check(at(w, seen, u.x, u.y) && w.freeCircle(u.x, u.y, u.radius * 0.9f), "unidade inicial livre: " + u.type.name + " equipa " + u.team);
        // mapa em texto para inspeção
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < w.H; y++) {
            for (int x = 0; x < w.W; x++) {
                char c = ".,#~T".charAt(w.tileAt(x, y));
                for (Building b : w.buildings) if (x >= b.tx && x < b.tx + b.tw && y >= b.ty && y < b.ty + b.th) c = "FRVG".charAt(b.kind);
                for (Sector s : w.sectors) if ((int) (s.fx / 32) == x && (int) (s.fy / 32) == y) c = 'P';
                sb.append(c);
            }
            sb.append('\n');
        }
        System.out.print(sb);
    }

    static void symmetry() {
        World w = new World(1, 1);
        boolean ok = true;
        for (int y = 0; y < w.H; y++)
            for (int x = 0; x < w.W; x++)
                if (w.blockedTile(x, y) != w.blockedTile(w.W - 1 - x, w.H - 1 - y)) ok = false;
        check(ok, "mapa simétrico (rotação de 180°)");
    }

    static void capture() {
        World w = new World(2, 1);
        Sector s = w.sectors[3]; // Vale Oeste, fábrica de veículos
        Building f = s.buildings.get(0);
        check(f.team == World.NEUTRAL && f.kind == Building.FACT_VEH, "fábrica do Vale Oeste começa neutra");
        Unit u = w.addUnit(UnitType.BATEDOR, World.PLAYER, s.fx, s.fy);
        w.update(Game.STEP);
        check(s.owner == World.PLAYER, "tocar na bandeira captura o setor");
        check(f.team == World.PLAYER, "a fábrica do setor passa para quem captura");
        int before = w.unitCount(World.PLAYER);
        for (int i = 0; i < 30 * 40; i++) w.update(Game.STEP);
        check(w.unitCount(World.PLAYER) > before, "a fábrica capturada produz unidades");
        // contestado: duas equipas na bandeira não muda de dono
        World w2 = new World(3, 1);
        Sector m = w2.sectors[4];
        w2.addUnit(UnitType.BATEDOR, World.PLAYER, m.fx - 4, m.fy);
        w2.addUnit(UnitType.BATEDOR, World.ENEMY, m.fx + 4, m.fy);
        w2.update(Game.STEP);
        check(m.owner == World.NEUTRAL, "bandeira disputada não muda de dono");
    }

    static void production() {
        World w = new World(4, 1);
        float t1 = w.buildSeconds(UnitType.TANQUE, World.PLAYER);
        w.capture(w.sectors[3], World.PLAYER);
        w.capture(w.sectors[7], World.PLAYER);
        w.capture(w.sectors[0], World.PLAYER);
        float t2 = w.buildSeconds(UnitType.TANQUE, World.PLAYER);
        check(t2 < t1 * 0.75f, "mais setores, produção mais rápida (" + (int) t1 + " s → " + (int) t2 + " s)");
    }

    static void boarding() {
        World w = new World(5, 1);
        Unit jeep = null;
        for (Unit u : w.units) if (u.team == World.NEUTRAL && u.type == UnitType.JIPE) { jeep = u; break; }
        Unit u = w.addUnit(UnitType.BATEDOR, World.PLAYER, jeep.x - 70, jeep.y);
        w.orderBoard(u, jeep);
        for (int i = 0; i < 30 * 10 && jeep.team == World.NEUTRAL; i++) w.update(Game.STEP);
        check(jeep.team == World.PLAYER && !u.alive && u.boardedInto == jeep, "infantaria ocupa veículo abandonado");
    }

    static void combat() {
        World w = new World(6, 1);
        w.units.clear();
        float x = 24 * 32, y = 18 * 32;
        Unit tank = w.addUnit(UnitType.TANQUE, World.PLAYER, x - 100, y);
        ArrayList<Unit> enemies = new ArrayList<>();
        for (int i = 0; i < 3; i++) enemies.add(w.addUnit(UnitType.BATEDOR, World.ENEMY, x + 80, y - 40 + i * 30));
        for (int i = 0; i < 30 * 60; i++) w.update(Game.STEP);
        int alive = 0;
        for (Unit e : enemies) if (e.alive) alive++;
        check(tank.alive && alive == 0, "tanque vence 3 batedores (vida do tanque: " + (int) tank.hp + ")");
        World w2 = new World(7, 1);
        w2.units.clear();
        Unit t2 = w2.addUnit(UnitType.TANQUE, World.PLAYER, x - 100, y);
        for (int i = 0; i < 3; i++) w2.addUnit(UnitType.BAZUCA, World.ENEMY, x + 60, y - 40 + i * 30);
        for (int i = 0; i < 30 * 60; i++) w2.update(Game.STEP);
        check(!t2.alive, "3 bazucas destroem um tanque");
    }

    static void sounds() {
        boolean ok = true;
        for (int i = 0; i < Sfx.COUNT; i++) {
            byte[] b = SoundSynth.wav(i);
            if (b.length < 200 || b[0] != 'R' || b[8] != 'W') ok = false;
        }
        check(ok, "sons gerados em WAV");
    }

    static void matches() {
        int[] wins = new int[3];
        long t0 = System.currentTimeMillis();
        for (int seed = 0; seed < 8; seed++) {
            int diff = seed % 3;
            World w = new World(1000 + seed, diff);
            w.ais.add(new Ai(w, World.PLAYER, diff));
            w.ais.add(new Ai(w, World.ENEMY, diff));
            int steps = 0;
            int maxUnits = 0;
            boolean stuck = false;
            while (w.winner < 0 && steps < 30 * 60 * 25) {
                w.update(Game.STEP);
                steps++;
                maxUnits = Math.max(maxUnits, w.units.size());
                if (steps % 300 == 0) {
                    for (Unit u : w.units) if (!w.freeCircle(u.x, u.y, u.radius * 0.5f)) stuck = true;
                }
            }
            String res = w.winner < 0 ? "sem vencedor" : (w.winner == 1 ? "laranja" : "ciano");
            if (w.winner >= 0) wins[w.winner]++;
            System.out.printf("partida %d (dif %d): %s em %.1f min · fabricadas %d/%d · capturas %d/%d · máx. unidades %d%n",
                seed, diff, res, w.time / 60, w.produced[1], w.produced[2], w.captures[1], w.captures[2], maxUnits);
            check(w.captures[1] + w.captures[2] >= 4, "partida " + seed + ": houve disputa de bandeiras");
            check(!stuck, "partida " + seed + ": nenhuma unidade presa dentro de rocha/água");
        }
        check(wins[1] + wins[2] >= 6, "a maioria das partidas IA contra IA termina (" + (wins[1] + wins[2]) + "/8)");
        System.out.println("tempo das partidas: " + (System.currentTimeMillis() - t0) + " ms");
    }
}
