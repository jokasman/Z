package pt.setorzero.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Jogo a dois em rede. O anfitrião (laranja) corre a simulação e envia ~15 imagens de estado por
 * segundo; o convidado (ciano) mostra-as, anima entre elas e envia as suas ordens ao anfitrião.
 */
public final class Versus implements World.Listener {
    static final byte M_HELLO = 1, M_START = 2, M_SNAP = 3, M_CMD = 4, M_BYE = 5, M_REJECT = 6;
    static final byte C_MOVE = 1, C_ATTACK = 2, C_BOARD = 3, C_PRODUCE = 4;

    public final Net net;
    public final boolean host;
    public final int me;
    World world;
    private World.Listener ui;
    private float snapT;
    private final ArrayList<Object[]> events = new ArrayList<>();
    private final ArrayList<float[]> sounds = new ArrayList<>();

    Versus(Net net, boolean host) {
        this.net = net;
        this.host = host;
        this.me = host ? World.PLAYER : World.ENEMY;
    }

    void attach(World w, World.Listener ui) {
        this.world = w;
        this.ui = ui;
        if (host) {
            w.netFx = new ArrayList<>();
            w.listener = this;
        } else {
            w.listener = ui;
        }
    }

    // ---------------------------------------------------------------- acontecimentos (anfitrião)

    @Override public void onEvent(int kind, int team, int other, String a, String b) {
        ui.onEvent(kind, team, other, a, b);
        if (host) events.add(new Object[]{kind, team, other, a, b});
    }

    @Override public void onSound(int id, float x, float y, int team) {
        ui.onSound(id, x, y, team);
        if (host && sounds.size() < 40) sounds.add(new float[]{id, x, y, team});
    }

    // ---------------------------------------------------------------- mensagens simples

    static byte[] hello() {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(bo);
            o.writeByte(M_HELLO);
            o.writeInt(Net.PROTO);
            o.writeUTF(Game.VERSION);
            return bo.toByteArray();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    static byte[] start(int level, long seed) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(bo);
            o.writeByte(M_START);
            o.writeInt(level);
            o.writeLong(seed);
            return bo.toByteArray();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    static byte[] simple(byte type, String text) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(bo);
            o.writeByte(type);
            o.writeUTF(text == null ? "" : text);
            return bo.toByteArray();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ---------------------------------------------------------------- imagem de estado

    /** Anfitrião: chamado a cada passo da simulação; envia uma imagem a cada 1/15 s. */
    void tick(float dt) {
        snapT -= dt;
        if (snapT > 0) return;
        snapT = world.winner >= 0 ? 0.5f : 1f / 15;
        try { net.send(snapshot()); } catch (Exception ignored) { }
    }

    private static int typeCode(UnitType t) { return t.id; }

    private static UnitType typeOf(int id) {
        if (id < UnitType.ALL.length) return UnitType.ALL[id];
        return id == UnitType.FORT_GUN.id ? UnitType.FORT_GUN : UnitType.TURRET_GUN;
    }

    private static int ang(float a) {
        double t = a / (2 * Math.PI);
        return (int) Math.round((t - Math.floor(t)) * 256) & 255;
    }

    private static float unang(int b) { return (float) ((b & 255) / 256.0 * 2 * Math.PI); }

    byte[] snapshot() throws Exception {
        World w = world;
        ByteArrayOutputStream bo = new ByteArrayOutputStream(4096);
        DataOutputStream o = new DataOutputStream(bo);
        o.writeByte(M_SNAP);
        o.writeFloat(w.time);
        o.writeByte(w.winner + 1);
        o.writeByte(w.sectors.length);
        for (Sector s : w.sectors) o.writeByte(s.owner);
        o.writeShort(w.buildings.size());
        for (Building b : w.buildings) {
            o.writeByte(b.team);
            o.writeByte((b.alive ? 1 : 0) | (b.destroyed ? 2 : 0) | (b.flash > 0 ? 4 : 0));
            o.writeShort((int) Math.max(0, b.hp));
            o.writeByte(ang(b.aim));
            o.writeByte(b.making == null ? 255 : typeCode(b.making));
            o.writeByte((int) (Math.min(1, b.progress) * 255));
        }
        int n = 0;
        for (Unit u : w.units) if (u.alive) n++;
        o.writeShort(n);
        for (Unit u : w.units) {
            if (!u.alive) continue;
            o.writeInt(u.id);
            o.writeByte(typeCode(u.type));
            o.writeByte(u.team);
            o.writeShort((int) (u.x * 8));
            o.writeShort((int) (u.y * 8));
            o.writeByte(ang(u.angle));
            o.writeByte(ang(u.aim));
            o.writeShort((int) Math.max(0, u.hp));
            o.writeByte(u.flash > 0 ? 1 : 0);
        }
        o.writeShort(w.shots.size());
        for (Projectile p : w.shots) {
            o.writeFloat(p.x); o.writeFloat(p.y); o.writeFloat(p.vx); o.writeFloat(p.vy); o.writeFloat(p.life);
            o.writeByte(typeCode(p.weapon));
        }
        List<Fx> nf = w.netFx;
        int fn = Math.min(nf.size(), 300);
        o.writeShort(fn);
        for (int i = 0; i < fn; i++) {
            Fx f = nf.get(i);
            o.writeByte(f.kind);
            o.writeFloat(f.x); o.writeFloat(f.y); o.writeFloat(f.x2); o.writeFloat(f.y2);
            o.writeFloat(f.vx); o.writeFloat(f.vy); o.writeFloat(f.t); o.writeFloat(f.life); o.writeFloat(f.size);
            o.writeInt(f.color);
        }
        nf.clear();
        o.writeShort(events.size());
        for (Object[] e : events) {
            o.writeByte((Integer) e[0]); o.writeByte((Integer) e[1]); o.writeByte((Integer) e[2]);
            o.writeUTF(e[3] == null ? "" : (String) e[3]);
            o.writeUTF(e[4] == null ? "" : (String) e[4]);
        }
        events.clear();
        o.writeShort(sounds.size());
        for (float[] s : sounds) {
            o.writeByte((int) s[0]); o.writeFloat(s[1]); o.writeFloat(s[2]); o.writeByte((int) s[3]);
        }
        sounds.clear();
        for (int[] arr : new int[][]{w.produced, w.lost, w.kills, w.captures}) for (int v : arr) o.writeInt(v);
        return bo.toByteArray();
    }

    /** Convidado: aplica uma imagem de estado ao mundo local. */
    void applySnapshot(DataInputStream in) throws Exception {
        World w = world;
        w.time = in.readFloat();
        w.winner = in.readByte() - 1;
        int ns = in.readByte();
        for (int i = 0; i < ns; i++) {
            int owner = in.readByte();
            if (i < w.sectors.length) w.sectors[i].owner = owner;
        }
        int nb = in.readShort();
        for (int i = 0; i < nb; i++) {
            int team = in.readByte(), flags = in.readByte(), hp = in.readShort(), aim = in.readByte(),
                making = in.readByte() & 255, prog = in.readByte() & 255;
            if (i >= w.buildings.size()) continue;
            Building b = w.buildings.get(i);
            b.team = team;
            b.alive = (flags & 1) != 0;
            b.destroyed = (flags & 2) != 0;
            if ((flags & 4) != 0) b.flash = 0.1f;
            b.hp = hp;
            b.aim = unang(aim);
            b.making = making == 255 ? null : typeOf(making);
            b.progress = prog / 255f;
        }
        HashMap<Integer, Unit> byId = new HashMap<>();
        for (Unit u : w.units) byId.put(u.id, u);
        int nu = in.readShort();
        HashMap<Integer, Boolean> seen = new HashMap<>();
        for (int i = 0; i < nu; i++) {
            int id = in.readInt(), type = in.readByte(), team = in.readByte();
            float x = in.readShort() / 8f, y = in.readShort() / 8f;
            int a = in.readByte(), aim = in.readByte(), hp = in.readShort(), fl = in.readByte();
            Unit u = byId.get(id);
            if (u == null || u.type != typeOf(type)) {
                if (u != null) u.alive = false;
                u = w.addUnitWithId(typeOf(type), team, x, y, id);
            }
            u.team = team;
            u.alive = true;
            u.netX = x; u.netY = y;
            u.angle = unang(a);
            u.aim = unang(aim);
            u.hp = hp;
            if (fl != 0) u.flash = 0.1f;
            seen.put(id, true);
        }
        for (int i = w.units.size() - 1; i >= 0; i--) {
            Unit u = w.units.get(i);
            if (!u.alive || !seen.containsKey(u.id)) { u.alive = false; w.units.remove(i); }
        }
        w.shots.clear();
        int np = in.readShort();
        for (int i = 0; i < np; i++) {
            Projectile p = new Projectile();
            p.x = in.readFloat(); p.y = in.readFloat(); p.vx = in.readFloat(); p.vy = in.readFloat(); p.life = in.readFloat();
            p.weapon = typeOf(in.readByte());
            w.shots.add(p);
        }
        int nf = in.readShort();
        for (int i = 0; i < nf; i++) {
            Fx f = new Fx();
            f.kind = in.readByte();
            f.x = in.readFloat(); f.y = in.readFloat(); f.x2 = in.readFloat(); f.y2 = in.readFloat();
            f.vx = in.readFloat(); f.vy = in.readFloat(); f.t = in.readFloat(); f.life = in.readFloat(); f.size = in.readFloat();
            f.color = in.readInt();
            if (w.fx.size() < 700) w.fx.add(f);
        }
        int ne = in.readShort();
        for (int i = 0; i < ne; i++) {
            int kind = in.readByte(), team = in.readByte(), other = in.readByte();
            String a = in.readUTF(), b = in.readUTF();
            ui.onEvent(kind, team, other, a.isEmpty() ? null : a, b.isEmpty() ? null : b);
        }
        int nsd = in.readShort();
        for (int i = 0; i < nsd; i++) {
            int id = in.readByte();
            float x = in.readFloat(), y = in.readFloat();
            int team = in.readByte();
            ui.onSound(id, x, y, team);
        }
        for (int[] arr : new int[][]{w.produced, w.lost, w.kills, w.captures}) for (int k = 0; k < 3; k++) arr[k] = in.readInt();
    }

    // ---------------------------------------------------------------- ordens

    private static int[] ids(List<Unit> list) {
        int n = 0;
        for (Unit u : list) if (u.alive) n++;
        int[] r = new int[n];
        int i = 0;
        for (Unit u : list) if (u.alive) r[i++] = u.id;
        return r;
    }

    private void sendCmd(byte cmd, int[] units, float x, float y, int a, int b) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(bo);
            o.writeByte(M_CMD);
            o.writeByte(cmd);
            o.writeShort(units.length);
            for (int id : units) o.writeInt(id);
            o.writeFloat(x); o.writeFloat(y);
            o.writeInt(a); o.writeInt(b);
            net.send(bo.toByteArray());
        } catch (Exception ignored) { }
    }

    void move(List<Unit> list, float x, float y) { sendCmd(C_MOVE, ids(list), x, y, 0, 0); }

    void attack(List<Unit> list, Entity t) {
        boolean bld = t instanceof Building;
        sendCmd(C_ATTACK, ids(list), 0, 0, bld ? 1 : 0, bld ? world.buildings.indexOf(t) : t.id);
    }

    void board(Unit u, Unit v) { sendCmd(C_BOARD, new int[]{u.id}, 0, 0, v.id, 0); }

    void produce(Building b, UnitType t) { sendCmd(C_PRODUCE, new int[0], 0, 0, world.buildings.indexOf(b), t.id); }

    /** Anfitrião: executa uma ordem do convidado (só sobre as unidades e fábricas dele). */
    void applyCmd(DataInputStream in) throws Exception {
        World w = world;
        int team = World.ENEMY;
        int cmd = in.readByte();
        int n = in.readShort();
        HashMap<Integer, Unit> byId = new HashMap<>();
        for (Unit u : w.units) byId.put(u.id, u);
        ArrayList<Unit> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Unit u = byId.get(in.readInt());
            if (u != null && u.alive && u.team == team) list.add(u);
        }
        float x = in.readFloat(), y = in.readFloat();
        int a = in.readInt(), b = in.readInt();
        switch (cmd) {
            case C_MOVE:
                w.orderMove(list, x, y, false);
                break;
            case C_ATTACK: {
                Entity t = null;
                if (a == 1) { if (b >= 0 && b < w.buildings.size()) t = w.buildings.get(b); }
                else t = byId.get(b);
                if (t != null && t.alive && t.team != team && t.team != World.NEUTRAL) w.orderAttack(list, t);
                break;
            }
            case C_BOARD: {
                Unit v = byId.get(a);
                if (!list.isEmpty() && v != null && v.alive) w.orderBoard(list.get(0), v);
                break;
            }
            case C_PRODUCE: {
                if (a < 0 || a >= w.buildings.size() || b < 0 || b >= UnitType.ALL.length) break;
                Building f = w.buildings.get(a);
                UnitType t = UnitType.ALL[b];
                if (f.isFactory() && f.team == team && java.util.Arrays.asList(f.buildable()).contains(t) && f.making != t) {
                    f.making = t;
                    f.progress = 0;
                }
                break;
            }
        }
    }

    static DataInputStream reader(byte[] b) { return new DataInputStream(new ByteArrayInputStream(b)); }
}
