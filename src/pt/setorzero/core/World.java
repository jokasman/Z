package pt.setorzero.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Estado e simulação de uma partida: terreno, setores, edifícios, unidades, tiros e efeitos. */
public final class World {
    public static final float TILE = 32f;
    public static final int GRASS = 0, DIRT = 1, ROCK = 2, WATER = 3, TREE = 4, BRIDGE = 5;
    public static final int NEUTRAL = 0, PLAYER = 1, ENEMY = 2;
    public static final int UNIT_CAP = 32;
    static final float LEASH = 7 * TILE;

    public static final int EV_READY = 1, EV_CAPTURE = 2, EV_BOARD = 3, EV_FORT = 4, EV_TURRET = 5;

    /** Acontecimentos da partida; quem os mostra decide o texto conforme a equipa de quem joga. */
    public interface Listener {
        void onEvent(int kind, int team, int other, String a, String b);
        /** team: equipa a quem o som interessa (0 = todos). */
        void onSound(int id, float x, float y, int team);
    }

    public final int W, H, theme;
    public final Level level;
    public final boolean versus;
    public final byte[] terrain;
    public final boolean[] blocked;
    public final ArrayList<Unit> units = new ArrayList<>();
    public final ArrayList<Building> buildings = new ArrayList<>();
    public Sector[] sectors;
    public final ArrayList<Projectile> shots = new ArrayList<>();
    public final ArrayList<Fx> fx = new ArrayList<>();
    public final ArrayList<Ai> ais = new ArrayList<>();
    public final Building[] forts = new Building[3];
    public final Random rnd;
    final PathFinder pf;
    public Listener listener;
    public float time;
    public int winner = -1;
    public final float[] prodMult = {1, 1, 1};
    public final int[] produced = new int[3], lost = new int[3], kills = new int[3], captures = new int[3];
    private int nextId = 1;
    private final float[] fortAlertT = new float[3];
    /** Anfitrião em rede: efeitos novos a enviar ao convidado. */
    public ArrayList<Fx> netFx;
    private boolean recordFx = true;
    private int scorchCount;

    public World(long seed, int difficulty) { this(seed, difficulty, Level.ALL[0]); }

    public World(long seed, int difficulty, Level level) { this(seed, difficulty, level, false); }

    /** versus: dois jogadores humanos (sem reforços nem bónus de nenhum dos lados). */
    public World(long seed, int difficulty, Level level, boolean versus) {
        rnd = new Random(seed);
        this.versus = versus;
        this.level = level;
        W = level.W;
        H = level.H;
        theme = level.theme;
        terrain = new byte[W * H];
        blocked = new boolean[W * H];
        level.build(this, difficulty, versus);
        pf = new PathFinder(this);
    }

    // ---------------------------------------------------------------- terreno

    public int tileAt(int x, int y) { return terrain[y * W + x]; }

    private int[] art;

    /** Terreno em pixel art (calculado uma vez). */
    public int[] art() {
        if (art == null) art = TerrainArt.paint(this);
        return art;
    }

    public boolean blockedTile(int x, int y) {
        return x < 0 || y < 0 || x >= W || y >= H || blocked[y * W + x];
    }

    public boolean freeCircle(float x, float y, float r) {
        int x0 = (int) Math.floor((x - r) / TILE), x1 = (int) Math.floor((x + r) / TILE);
        int y0 = (int) Math.floor((y - r) / TILE), y1 = (int) Math.floor((y + r) / TILE);
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                if (!blockedTile(tx, ty)) continue;
                float cx = Math.max(tx * TILE, Math.min(x, tx * TILE + TILE));
                float cy = Math.max(ty * TILE, Math.min(y, ty * TILE + TILE));
                float dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy < r * r) return false;
            }
        }
        return true;
    }

    public Sector sectorAt(float x, float y) {
        for (Sector s : sectors) if (s.contains(x, y)) return s;
        return null;
    }

    // ---------------------------------------------------------------- criação

    Unit addUnit(UnitType type, int team, float x, float y) {
        Unit u = new Unit(type);
        u.id = nextId++;
        u.team = team;
        u.x = x; u.y = y;
        u.ax = x; u.ay = y;
        u.lastX = x; u.lastY = y;
        u.angle = u.aim = team == ENEMY ? (float) Math.PI * 0.75f : -(float) Math.PI * 0.25f;
        u.scan = rnd.nextFloat() * 0.3f;
        if (type.vehicle) u.crew = UnitType.BATEDOR;
        units.add(u);
        return u;
    }

    Building addBuilding(int kind, int tx, int ty, int team) {
        Building b = new Building(kind, tx, ty);
        b.id = nextId++;
        b.team = team;
        for (int y = ty; y < ty + b.th; y++)
            for (int x = tx; x < tx + b.tw; x++) blocked[y * W + x] = true;
        buildings.add(b);
        if (kind == Building.FORT) forts[team] = b;
        return b;
    }

    /** Procura um lugar livre (terreno e outras unidades) perto de (x, y). */
    float[] freeSpot(float x, float y, float r) {
        for (int ring = 0; ring < 12; ring++) {
            int n = ring == 0 ? 1 : ring * 8;
            float rad = ring * r * 2.1f;
            float a0 = rnd.nextFloat() * 6.283f;
            for (int i = 0; i < n; i++) {
                float a = a0 + i * 6.2832f / n;
                float px = x + (float) Math.cos(a) * rad, py = y + (float) Math.sin(a) * rad;
                if (!freeCircle(px, py, r)) continue;
                boolean ok = true;
                for (Unit o : units) {
                    if (!o.alive) continue;
                    float dx = o.x - px, dy = o.y - py, rr = o.radius + r;
                    if (dx * dx + dy * dy < rr * rr) { ok = false; break; }
                }
                if (ok) return new float[]{px, py};
            }
        }
        return new float[]{x, y};
    }

    // ---------------------------------------------------------------- consultas

    public int unitCount(int team) {
        int n = 0;
        for (Unit u : units) if (u.alive && u.team == team) n++;
        return n;
    }

    public int ownedSectors(int team) {
        int n = 0;
        for (Sector s : sectors) if (s.owner == team) n++;
        return n;
    }

    public boolean ownsFactory(int team) {
        for (Building b : buildings) if (b.isFactory() && b.team == team) return true;
        return false;
    }

    /** Multiplicador da velocidade de produção: mais setores, fábricas mais rápidas. */
    public float territoryFactor(int team) {
        return 0.7f + 0.1f * ownedSectors(team);
    }

    /** Segundos para fabricar um tipo numa fábrica desta equipa, com o território atual. */
    public float buildSeconds(UnitType t, int team) {
        return t.buildTime / (territoryFactor(team) * prodMult[team]);
    }

    static float dist(float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    static float angleDiff(float a, float b) {
        float d = b - a;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return d;
    }

    static float approach(float a, float b, float max) {
        float d = angleDiff(a, b);
        if (Math.abs(d) <= max) return b;
        return a + Math.signum(d) * max;
    }

    boolean inRange(Entity a, Entity b, float range) {
        return dist(a.x, a.y, b.x, b.y) - b.radius <= range;
    }

    Entity findTarget(Entity src, int team, float range, UnitType w) {
        Entity best = null;
        float bs = Float.MAX_VALUE;
        for (Unit u : units) {
            if (!u.alive || u.team == NEUTRAL || u.team == team) continue;
            float d = dist(src.x, src.y, u.x, u.y) - u.radius;
            if (d > range) continue;
            float s = d / Math.max(0.25f, w.mult(u.armor()));
            if (s < bs) { bs = s; best = u; }
        }
        for (Building b : buildings) {
            if (!b.attackable() || b.team == team) continue;
            float d = dist(src.x, src.y, b.x, b.y) - b.radius;
            if (d > range) continue;
            float s = d / Math.max(0.25f, w.mult(UnitType.BLD)) + TILE;
            if (s < bs) { bs = s; best = b; }
        }
        return best;
    }

    // ---------------------------------------------------------------- ordens

    void setIdle(Unit u) {
        u.order = Unit.IDLE;
        u.ax = u.x; u.ay = u.y;
        u.path = null;
        u.target = null;
    }

    /** Move um grupo para (x, y) em formação. Com attackMove, param para combater pelo caminho. */
    public void orderMove(List<Unit> list, float x, float y, boolean attackMove) {
        int n = 0;
        for (Unit u : list) if (u.alive && u.team != NEUTRAL) n++;
        if (n == 0) return;
        ArrayList<float[]> spots = new ArrayList<>();
        int ctx = Math.max(0, Math.min(W - 1, (int) (x / TILE))), cty = Math.max(0, Math.min(H - 1, (int) (y / TILE)));
        if (n == 1 && freeCircle(x, y, 6)) spots.add(new float[]{x, y});
        else {
            int start = pf.nearestFree(cty * W + ctx, 10);
            if (start < 0) return;
            int sx = start % W, sy = start / W;
            for (int r = 0; r < 12 && spots.size() < n; r++) {
                for (int yy = sy - r; yy <= sy + r && spots.size() < n; yy++) {
                    for (int xx = sx - r; xx <= sx + r && spots.size() < n; xx++) {
                        if (Math.max(Math.abs(xx - sx), Math.abs(yy - sy)) != r) continue;
                        if (blockedTile(xx, yy)) continue;
                        spots.add(new float[]{(xx + 0.5f) * TILE, (yy + 0.5f) * TILE});
                    }
                }
            }
        }
        ArrayList<Unit> todo = new ArrayList<>();
        for (Unit u : list) if (u.alive && u.team != NEUTRAL) todo.add(u);
        for (float[] sp : spots) {
            if (todo.isEmpty()) break;
            Unit best = null;
            float bd = Float.MAX_VALUE;
            for (Unit u : todo) {
                float d = dist(u.x, u.y, sp[0], sp[1]);
                if (d < bd) { bd = d; best = u; }
            }
            todo.remove(best);
            give(best, attackMove ? Unit.AMOVE : Unit.MOVE, sp[0], sp[1], null);
        }
        for (Unit u : todo) give(u, attackMove ? Unit.AMOVE : Unit.MOVE, x, y, null);
    }

    public void orderAttack(List<Unit> list, Entity target) {
        for (Unit u : list) if (u.alive && u.team != NEUTRAL) give(u, Unit.ATTACK, target.x, target.y, target);
    }

    public void orderBoard(Unit u, Unit vehicle) {
        if (u.type.vehicle || !vehicle.type.vehicle || vehicle.team != NEUTRAL) return;
        give(u, Unit.BOARD, vehicle.x, vehicle.y, vehicle);
    }

    private void give(Unit u, int order, float x, float y, Entity target) {
        u.order = order;
        u.ox = x; u.oy = y;
        u.target = target;
        u.path = null;
        u.pathFailed = false;
        u.stuckN = 0;
        u.stuckT = 0;
    }

    // ---------------------------------------------------------------- simulação

    public void update(float dt) {
        updateFx(dt);
        if (winner >= 0) return;
        time += dt;
        for (int i = 0; i < 3; i++) fortAlertT[i] -= dt;
        for (int i = 0; i < ais.size(); i++) ais.get(i).update(dt);
        for (int i = 0; i < buildings.size(); i++) updateBuilding(buildings.get(i), dt);
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (u.alive) updateUnit(u, dt);
        }
        separate();
        updateShots(dt);
        captureCheck(dt);
        for (int i = units.size() - 1; i >= 0; i--) if (!units.get(i).alive) units.remove(i);
        checkWin();
    }

    private void updateBuilding(Building b, float dt) {
        b.flash -= dt;
        if (!b.alive) return;
        if (b.isFactory()) {
            if (b.team == NEUTRAL) return;
            if (b.making == null) b.making = b.buildable()[0];
            if (unitCount(b.team) < UNIT_CAP) {
                b.progress += dt / buildSeconds(b.making, b.team);
                b.smokeT -= dt;
                if (b.smokeT <= 0) {
                    b.smokeT = 0.5f + rnd.nextFloat() * 0.4f;
                    smoke((b.tx + b.tw * 0.82f) * TILE, (b.ty + b.th * 0.3f) * TILE, 7, 0, -12);
                }
                if (b.progress >= 1) {
                    b.progress = 0;
                    UnitType t = b.making;
                    float[] p = freeSpot(b.doorX(), b.doorY() + (b.doorUp ? -t.radius : t.radius), t.radius);
                    Unit u = addUnit(t, b.team, p[0], p[1]);
                    u.angle = u.aim = (float) (b.doorUp ? -Math.PI / 2 : Math.PI / 2);
                    produced[b.team]++;
                    b.justMade = true;
                    event(EV_READY, b.team, 0, t.name, b.sector.name);
                    sound(Sfx.READY, b.x, b.y, b.team);
                }
            }
            return;
        }
        if (b.team == NEUTRAL) return;
        UnitType w = b.weapon();
        b.cd -= dt;
        b.scan -= dt;
        if (b.scan <= 0) {
            b.scan = 0.3f;
            b.target = findTarget(b, b.team, w.range, w);
        }
        if (b.target != null && (!b.target.alive || b.target.team == b.team || !inRange(b, b.target, w.range))) b.target = null;
        if (b.target != null) {
            float want = (float) Math.atan2(b.target.y - b.y, b.target.x - b.x);
            b.aim = approach(b.aim, want, 2.5f * dt);
            if (b.cd <= 0 && Math.abs(angleDiff(b.aim, want)) < 0.15f) {
                fire(b, b.team, w, b.aim, b.target);
                b.cd = w.cooldown * (0.9f + rnd.nextFloat() * 0.2f);
            }
        }
    }

    private void updateUnit(Unit u, float dt) {
        u.flash -= dt;
        if (u.team == NEUTRAL) return;
        UnitType t = u.type;
        u.cd -= dt;
        u.scan -= dt;
        u.repath -= dt;
        boolean looking = u.order == Unit.IDLE || u.order == Unit.AMOVE;
        float sight = t.range + (looking ? 2.5f * TILE : 0.3f * TILE);
        if (u.scan <= 0) {
            u.scan = 0.25f + rnd.nextFloat() * 0.1f;
            Entity f = findTarget(u, u.team, sight, t);
            if (f != null) u.auto = f;
        }
        if (u.auto != null && (!u.auto.alive || u.auto.team == u.team || u.auto.team == NEUTRAL
                || dist(u.x, u.y, u.auto.x, u.auto.y) > sight + 4 * TILE)) u.auto = null;
        if (u.target != null && !u.target.alive) setIdle(u);
        if (u.order == Unit.ATTACK && u.target != null && u.target.team == u.team) setIdle(u);
        if (u.order == Unit.BOARD && (u.target == null || u.target.team != NEUTRAL)) setIdle(u);

        Entity shoot = null;
        if (u.order == Unit.ATTACK && u.target != null && inRange(u, u.target, t.range)) shoot = u.target;
        else if (u.auto != null && inRange(u, u.auto, t.range)) shoot = u.auto;

        boolean moving = false;
        switch (u.order) {
            case Unit.IDLE:
                if (shoot == null && u.auto != null && dist(u.ax, u.ay, u.auto.x, u.auto.y) < LEASH)
                    moving = moveTo(u, u.auto.x, u.auto.y, t.range * 0.85f + u.auto.radius, dt);
                else if (shoot == null && dist(u.x, u.y, u.ax, u.ay) > TILE * 0.9f)
                    moving = moveTo(u, u.ax, u.ay, 4, dt);
                break;
            case Unit.MOVE:
                moving = moveTo(u, u.ox, u.oy, 3, dt);
                if (!moving) setIdle(u);
                break;
            case Unit.AMOVE:
                if (shoot != null) break;
                if (u.auto != null) moving = moveTo(u, u.auto.x, u.auto.y, t.range * 0.85f + u.auto.radius, dt);
                else {
                    moving = moveTo(u, u.ox, u.oy, 3, dt);
                    if (!moving) setIdle(u);
                }
                break;
            case Unit.ATTACK:
                if (shoot != u.target) {
                    moving = moveTo(u, u.target.x, u.target.y, t.range * 0.85f + u.target.radius, dt);
                    if (!moving && !inRange(u, u.target, t.range) && u.pathFailed) setIdle(u);
                }
                break;
            case Unit.BOARD:
                if (dist(u.x, u.y, u.target.x, u.target.y) < u.radius + u.target.radius + 8) board(u, (Unit) u.target);
                else {
                    moving = moveTo(u, u.target.x, u.target.y, 0, dt);
                    if (!moving && u.pathFailed) setIdle(u);
                }
                break;
        }
        if (!u.alive) return;

        float want = shoot != null ? (float) Math.atan2(shoot.y - u.y, shoot.x - u.x) : u.angle;
        u.aim = approach(u.aim, want, (t.vehicle ? 4f : 14f) * dt);
        if (!t.vehicle && shoot != null && !moving) u.angle = u.aim;
        if (shoot != null && u.cd <= 0 && Math.abs(angleDiff(u.aim, want)) < 0.2f) {
            fire(u, u.team, t, u.aim, shoot);
            u.cd = t.cooldown * (0.9f + rnd.nextFloat() * 0.2f);
        }
    }

    /** Segue (ou calcula) o caminho até (x, y). Devolve false quando chegou ou não há caminho. */
    private boolean moveTo(Unit u, float x, float y, float stop, float dt) {
        if (dist(u.x, u.y, x, y) <= stop + 0.5f) { u.path = null; return false; }
        boolean need = u.path == null || (u.repath <= 0 && dist(u.px, u.py, x, y) > TILE);
        if (need) {
            if (u.pathFailed && u.repath > 0) return false;
            u.path = pf.find(u.x, u.y, x, y, u.radius);
            u.pi = 0;
            u.px = x; u.py = y;
            u.repath = 0.8f + rnd.nextFloat() * 0.4f;
            u.pathFailed = u.path == null;
            if (u.path == null) return false;
        }
        int last = u.path.length / 2 - 1;
        float wx = u.path[u.pi * 2], wy = u.path[u.pi * 2 + 1];
        float dx = wx - u.x, dy = wy - u.y;
        float dd = (float) Math.sqrt(dx * dx + dy * dy);
        if (u.pi < last && dd < 8) {
            u.pi++;
            wx = u.path[u.pi * 2]; wy = u.path[u.pi * 2 + 1];
            dx = wx - u.x; dy = wy - u.y;
            dd = (float) Math.sqrt(dx * dx + dy * dy);
        }
        if (u.pi == last && dd < 3) { u.path = null; return false; }
        steer(u, dx / dd, dy / dd, Math.min(dd, u.type.speed * dt), dt);

        u.stuckT += dt;
        if (u.stuckT > 1f) {
            if (dist(u.x, u.y, u.lastX, u.lastY) < u.type.speed * 0.2f) {
                u.stuckN++;
                u.path = null;
                if (u.stuckN >= 3 && dist(u.x, u.y, x, y) < 2.5f * TILE + stop) {
                    u.stuckN = 0;
                    u.stuckT = 0;
                    return false;
                }
            } else u.stuckN = 0;
            u.stuckT = 0;
            u.lastX = u.x; u.lastY = u.y;
        }
        return true;
    }

    private void steer(Unit u, float dirx, float diry, float maxStep, float dt) {
        UnitType t = u.type;
        float want = (float) Math.atan2(diry, dirx);
        u.angle = approach(u.angle, want, (t.vehicle ? 3.2f : 14f) * dt);
        float step = maxStep;
        float mx, my;
        if (t.vehicle) {
            float diff = Math.abs(angleDiff(u.angle, want));
            step *= Math.max(0f, (float) Math.cos(diff * 1.3f));
            mx = (float) Math.cos(u.angle) * step;
            my = (float) Math.sin(u.angle) * step;
        } else {
            mx = dirx * step;
            my = diry * step;
        }
        tryMove(u, mx, my);
        u.anim += step;
    }

    private void tryMove(Unit u, float mx, float my) {
        float r = u.radius * 0.95f;
        if (!freeCircle(u.x, u.y, r)) { u.x += mx; u.y += my; return; }
        if (freeCircle(u.x + mx, u.y + my, r)) { u.x += mx; u.y += my; }
        else if (freeCircle(u.x + mx, u.y, r)) u.x += mx;
        else if (freeCircle(u.x, u.y + my, r)) u.y += my;
    }

    private void separate() {
        int n = units.size();
        for (int i = 0; i < n; i++) {
            Unit a = units.get(i);
            if (!a.alive) continue;
            for (int j = i + 1; j < n; j++) {
                Unit b = units.get(j);
                if (!b.alive) continue;
                float dx = b.x - a.x, dy = b.y - a.y, rr = a.radius + b.radius;
                float d2 = dx * dx + dy * dy;
                if (d2 >= rr * rr) continue;
                float d = (float) Math.sqrt(d2);
                if (d < 0.01f) { dx = rnd.nextFloat() - 0.5f; dy = rnd.nextFloat() - 0.5f; d = 0.7f; }
                float over = (rr - d) * 0.5f;
                float nx = dx / d, ny = dy / d;
                float wa, wb;
                if (a.team == NEUTRAL) { wa = 0; wb = 1; }
                else if (b.team == NEUTRAL) { wa = 1; wb = 0; }
                else {
                    float ma = a.type.vehicle ? 3 : 1, mb = b.type.vehicle ? 3 : 1;
                    wa = mb / (ma + mb); wb = ma / (ma + mb);
                    // quem está parado cede mais a quem se desloca
                    if (a.order != Unit.IDLE && b.order == Unit.IDLE) { wa *= 0.5f; wb = 1 - wa; }
                    else if (b.order != Unit.IDLE && a.order == Unit.IDLE) { wb *= 0.5f; wa = 1 - wb; }
                }
                push(a, -nx * over * 2 * wa, -ny * over * 2 * wa);
                push(b, nx * over * 2 * wb, ny * over * 2 * wb);
            }
        }
    }

    private void push(Unit u, float mx, float my) {
        if (mx == 0 && my == 0) return;
        float r = u.radius * 0.95f;
        if (freeCircle(u.x + mx, u.y + my, r)) { u.x += mx; u.y += my; }
        else if (freeCircle(u.x + mx, u.y, r)) u.x += mx;
        else if (freeCircle(u.x, u.y + my, r)) u.y += my;
        if (u.order == Unit.IDLE) { u.ax += mx * 0.5f; u.ay += my * 0.5f; }
    }

    // ---------------------------------------------------------------- combate

    private void fire(Entity src, int team, UnitType w, float aim, Entity tgt) {
        float mx = src.x + (float) Math.cos(aim) * src.radius * 1.15f;
        float my = src.y + (float) Math.sin(aim) * src.radius * 1.15f;
        switch (w.weapon) {
            case UnitType.W_ROCKET:
            case UnitType.W_SHELL: {
                Projectile p = new Projectile();
                p.team = team; p.weapon = w; p.owner = src;
                p.x = mx; p.y = my;
                float scatter = w.weapon == UnitType.W_SHELL ? 0.35f * TILE : 0.15f * TILE;
                p.tx = tgt.x + (rnd.nextFloat() - 0.5f) * 2 * scatter;
                p.ty = tgt.y + (rnd.nextFloat() - 0.5f) * 2 * scatter;
                float sp = (w.weapon == UnitType.W_ROCKET ? 8f : 12f) * TILE;
                float d = Math.max(1, dist(mx, my, p.tx, p.ty));
                p.vx = (p.tx - mx) / d * sp; p.vy = (p.ty - my) / d * sp;
                p.life = d / sp;
                shots.add(p);
                flashFx(mx, my, w.weapon == UnitType.W_SHELL ? 9 : 6);
                sound(w.weapon == UnitType.W_ROCKET ? Sfx.ROCKET : Sfx.CANNON, src.x, src.y);
                break;
            }
            default: {
                float j = tgt.radius * 0.6f;
                Fx f = add(Fx.TRACER, mx, my, w.weapon == UnitType.W_SNIPER ? 0.16f : 0.07f, 0);
                f.x2 = tgt.x + (rnd.nextFloat() - 0.5f) * j;
                f.y2 = tgt.y + (rnd.nextFloat() - 0.5f) * j;
                f.color = w.weapon == UnitType.W_SNIPER ? 0xFFE8F4FF : 0xFFFFE27A;
                flashFx(mx, my, w.weapon == UnitType.W_MG ? 4 : 5);
                damage(tgt, w.damage * w.mult(tgt.armor()), src, team);
                sound(w.weapon == UnitType.W_SNIPER ? Sfx.SNIPE : w.weapon == UnitType.W_MG ? Sfx.MG : Sfx.SHOT, src.x, src.y);
            }
        }
    }

    private void updateShots(float dt) {
        for (int i = shots.size() - 1; i >= 0; i--) {
            Projectile p = shots.get(i);
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.life -= dt;
            if (p.weapon.weapon == UnitType.W_ROCKET) {
                p.trailT -= dt;
                if (p.trailT <= 0) { p.trailT = 0.03f; recordFx = false; smoke(p.x, p.y, 3.5f, 0, 0); recordFx = true; }
            }
            if (p.life <= 0) {
                shots.remove(i);
                explode(p.tx, p.ty, p.weapon, p.owner, p.team);
            }
        }
    }

    private void explode(float x, float y, UnitType w, Entity by, int team) {
        float r = w.splash;
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (!u.alive || u.team == team || u.team == NEUTRAL) continue;
            float d = dist(x, y, u.x, u.y) - u.radius;
            if (d > r) continue;
            float f = 1f - 0.5f * Math.max(0, d) / r;
            damage(u, w.damage * w.mult(u.armor()) * f, by, team);
        }
        for (int i = 0; i < buildings.size(); i++) {
            Building b = buildings.get(i);
            if (!b.attackable() || b.team == team) continue;
            if (!b.contains(x, y, r)) continue;
            damage(b, w.damage * w.mult(UnitType.BLD), by, team);
        }
        boom(x, y, r * 0.9f, false);
        sound(Sfx.EXPLODE, x, y);
    }

    void damage(Entity e, float amount, Entity by, int byTeam) {
        if (!e.alive) return;
        e.hp -= amount;
        e.flash = 0.12f;
        if (e instanceof Unit) {
            Unit u = (Unit) e;
            if (by != null && by.alive && by.team != u.team && (u.auto == null || !u.auto.alive)) u.auto = by;
        } else if (e instanceof Building && ((Building) e).kind == Building.FORT && e.team > 0 && fortAlertT[e.team] <= 0) {
            fortAlertT[e.team] = 20;
            event(EV_FORT, e.team, 0, null, null);
            sound(Sfx.ALERT, e.x, e.y, e.team);
        }
        if (e.hp <= 0) kill(e, byTeam);
    }

    private void kill(Entity e, int byTeam) {
        e.alive = false;
        e.hp = 0;
        if (byTeam > 0) kills[byTeam]++;
        if (e instanceof Unit) {
            Unit u = (Unit) e;
            lost[u.team]++;
            boom(u.x, u.y, u.type.vehicle ? 26 : 14, true);
            scorch(u.x, u.y, u.type.vehicle ? 20 : 11);
            sound(Sfx.EXPLODE, u.x, u.y);
            if (u.type.vehicle && u.crew != null && rnd.nextFloat() < 0.5f) {
                float[] p = freeSpot(u.x, u.y, u.crew.radius);
                Unit c = addUnit(u.crew, u.team, p[0], p[1]);
                c.hp = c.maxHp * 0.5f;
            }
        } else {
            Building b = (Building) e;
            b.destroyed = true;
            boom(b.x, b.y, b.kind == Building.FORT ? 70 : 40, true);
            boom(b.x + 14, b.y - 10, 30, false);
            scorch(b.x, b.y, b.kind == Building.FORT ? 50 : 30);
            sound(Sfx.EXPLODE, b.x, b.y);
            if (b.kind == Building.TURRET && b.sector != null)
                event(EV_TURRET, b.team, 0, b.sector.name, null);
        }
    }

    private void board(Unit u, Unit v) {
        v.team = u.team;
        v.crew = u.type;
        v.order = Unit.IDLE;
        v.ax = v.x; v.ay = v.y;
        v.scan = 0;
        u.alive = false;
        u.boardedInto = v;
        event(EV_BOARD, u.team, 0, v.type.name, null);
        sound(Sfx.BOARD, v.x, v.y, u.team);
    }

    // ---------------------------------------------------------------- bandeiras

    private void captureCheck(float dt) {
        for (Sector s : sectors) {
            s.flashT -= dt;
            int who = -1;
            boolean contested = false;
            for (int i = 0; i < units.size(); i++) {
                Unit u = units.get(i);
                if (!u.alive || u.team == NEUTRAL) continue;
                float rr = u.radius + TILE * 0.45f;
                float dx = u.x - s.fx, dy = u.y - s.fy;
                if (dx * dx + dy * dy > rr * rr) continue;
                if (who < 0) who = u.team;
                else if (who != u.team) contested = true;
            }
            if (who > 0 && !contested && s.owner != who && !homeLocked(s)) capture(s, who);
        }
    }

    /** A bandeira da base só pode ser tomada depois de o forte dessa equipa cair. */
    public boolean homeLocked(Sector s) {
        for (Building b : s.buildings) if (b.kind == Building.FORT && b.alive && b.team == s.owner) return true;
        return false;
    }

    void capture(Sector s, int team) {
        int prev = s.owner;
        s.owner = team;
        s.flashT = 1.2f;
        captures[team]++;
        for (Building b : s.buildings) {
            if (b.kind == Building.FORT || !b.alive) continue;
            b.team = team;
            b.progress = 0;
            b.making = null;
            b.target = null;
        }
        Fx f = add(Fx.RING, s.fx, s.fy, 0.9f, 3 * TILE);
        f.color = team == PLAYER ? 0xFFF28C28 : 0xFF2EC4D6;
        event(EV_CAPTURE, team, prev, s.name, null);
        sound(Sfx.CAPTURE, s.fx, s.fy, team);
        if (prev != NEUTRAL) sound(Sfx.ALERT, s.fx, s.fy, prev);
    }

    private void checkWin() {
        for (int team = 1; team <= 2; team++) {
            Building f = forts[team];
            boolean dead = f == null || !f.alive || (unitCount(team) == 0 && !ownsFactory(team));
            if (dead) {
                winner = 3 - team;
                return;
            }
        }
    }

    // ---------------------------------------------------------------- efeitos

    Fx add(int kind, float x, float y, float life, float size) {
        Fx f = new Fx();
        f.kind = kind; f.x = x; f.y = y; f.life = life; f.size = size;
        if (fx.size() < 700) {
            fx.add(f);
            if (netFx != null && recordFx) netFx.add(f);
        }
        return f;
    }

    private void flashFx(float x, float y, float size) { add(Fx.FLASH, x, y, 0.06f, size); }

    private void smoke(float x, float y, float size, float vx, float vy) {
        Fx f = add(Fx.SMOKE, x, y, 0.9f + rnd.nextFloat() * 0.4f, size);
        f.vx = vx + (rnd.nextFloat() - 0.5f) * 8;
        f.vy = vy + (rnd.nextFloat() - 0.5f) * 8;
    }

    private void scorch(float x, float y, float size) {
        if (scorchCount > 50) return;
        scorchCount++;
        add(Fx.SCORCH, x, y, 40, size);
    }

    void boom(float x, float y, float size, boolean debris) {
        add(Fx.BOOM, x, y, 0.45f + size / 120f, size);
        for (int i = 0; i < 3; i++) smoke(x + (rnd.nextFloat() - 0.5f) * size, y + (rnd.nextFloat() - 0.5f) * size, size * 0.4f, 0, -8);
        if (debris) {
            for (int i = 0; i < 6; i++) {
                Fx d = add(Fx.DEBRIS, x, y, 0.5f + rnd.nextFloat() * 0.4f, 2 + rnd.nextFloat() * 2.5f);
                float a = rnd.nextFloat() * 6.283f, sp = 40 + rnd.nextFloat() * 90;
                d.vx = (float) Math.cos(a) * sp;
                d.vy = (float) Math.sin(a) * sp;
            }
        }
    }

    private void updateFx(float dt) {
        for (int i = fx.size() - 1; i >= 0; i--) {
            Fx f = fx.get(i);
            f.t += dt;
            f.x += f.vx * dt;
            f.y += f.vy * dt;
            if (f.kind == Fx.DEBRIS) { f.vx *= 1 - 3 * dt; f.vy *= 1 - 3 * dt; }
            if (f.t >= f.life) {
                if (f.kind == Fx.SCORCH) scorchCount--;
                fx.remove(i);
            }
        }
    }

    void event(int kind, int team, int other, String a, String b) {
        if (listener != null) listener.onEvent(kind, team, other, a, b);
    }

    void sound(int id, float x, float y) { sound(id, x, y, 0); }

    void sound(int id, float x, float y, int team) {
        if (listener != null) listener.onSound(id, x, y, team);
    }

    /**
     * Convidado em rede: não simula, só anima entre as imagens de estado enviadas pelo anfitrião
     * (unidades deslizam para a última posição recebida, tiros avançam, efeitos correm).
     */
    public void clientUpdate(float dt) {
        updateFx(dt);
        time += dt;
        float k = Math.min(1, dt * 12);
        for (Unit u : units) {
            float dx = u.netX - u.x, dy = u.netY - u.y;
            if (dx * dx + dy * dy > 4 * TILE * TILE) { u.x = u.netX; u.y = u.netY; }
            else { u.x += dx * k; u.y += dy * k; u.anim += (float) Math.sqrt(dx * dx + dy * dy) * k; }
            u.flash -= dt;
        }
        for (Building b : buildings) b.flash -= dt;
        for (Sector s : sectors) s.flashT -= dt;
        for (int i = shots.size() - 1; i >= 0; i--) {
            Projectile p = shots.get(i);
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.life -= dt;
            if (p.weapon.weapon == UnitType.W_ROCKET) {
                p.trailT -= dt;
                if (p.trailT <= 0) { p.trailT = 0.03f; smoke(p.x, p.y, 3.5f, 0, 0); }
            }
            if (p.life <= 0) shots.remove(i);
        }
    }

    Unit addUnitWithId(UnitType type, int team, float x, float y, int id) {
        Unit u = addUnit(type, team, x, y);
        u.id = id;
        u.netX = x; u.netY = y;
        return u;
    }
}
