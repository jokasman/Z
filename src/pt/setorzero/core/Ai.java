package pt.setorzero.core;

import java.util.ArrayList;

/** Comandante controlado pelo computador: escolhe a produção, defende, captura bandeiras e ataca o forte. */
public final class Ai {
    private final World w;
    public final int team;
    private final float think;
    private final int assault;
    private float t;

    /** difficulty: 0 fácil, 1 normal, 2 difícil. */
    public Ai(World w, int team, int difficulty) {
        this.w = w;
        this.team = team;
        this.think = difficulty == 0 ? 3f : difficulty == 1 ? 1.6f : 1f;
        this.assault = difficulty == 0 ? 14 : difficulty == 1 ? 11 : 9;
        w.prodMult[team] = (difficulty == 0 ? 0.75f : difficulty == 1 ? 1f : 1.25f) * (team == World.ENEMY ? w.level.enemyProd : 1f);
        t = think * 0.5f;
    }

    void update(float dt) {
        t -= dt;
        if (t > 0) return;
        t = think;
        production();
        ArrayList<Unit> mine = new ArrayList<>();
        for (Unit u : w.units) if (u.alive && u.team == team) mine.add(u);
        if (mine.isEmpty()) return;
        defend(mine);
        board(mine);
        attackOrCapture(mine);
    }

    private int enemy() { return 3 - team; }

    private void production() {
        int enemyVeh = 0, enemyAll = 0;
        for (Unit u : w.units) {
            if (!u.alive || u.team != enemy()) continue;
            enemyAll++;
            if (u.type.vehicle) enemyVeh++;
        }
        float v = enemyAll == 0 ? 0.2f : (float) enemyVeh / enemyAll;
        for (Building b : w.buildings) {
            if (!b.isFactory() || b.team != team) continue;
            if (b.making != null && !b.justMade) continue;
            b.justMade = false;
            if (b.kind == Building.FACT_VEH) {
                b.making = w.rnd.nextFloat() < 0.4f ? UnitType.JIPE : UnitType.TANQUE;
            } else {
                float wb = 0.4f * (1 - v) + 0.15f, wa = 0.25f * (1 - v) + 0.05f, wz = 0.2f + 0.6f * v;
                float r = w.rnd.nextFloat() * (wb + wa + wz);
                b.making = r < wb ? UnitType.BATEDOR : r < wb + wa ? UnitType.ATIRADOR : UnitType.BAZUCA;
            }
        }
    }

    private boolean available(Unit u) {
        return u.order == Unit.IDLE && u.auto == null;
    }

    /** Responde a inimigos perto do forte e das fábricas. */
    private void defend(ArrayList<Unit> mine) {
        for (Building b : w.buildings) {
            if (b.team != team || !b.alive || !(b.kind == Building.FORT || b.isFactory())) continue;
            Unit threat = null;
            float td = 7 * World.TILE;
            for (Unit e : w.units) {
                if (!e.alive || e.team != enemy()) continue;
                float d = World.dist(b.x, b.y, e.x, e.y);
                if (d < td) { td = d; threat = e; }
            }
            if (threat == null) continue;
            ArrayList<Unit> send = new ArrayList<>();
            for (Unit u : mine) {
                if (u.order == Unit.ATTACK || u.order == Unit.BOARD) continue;
                if (World.dist(u.x, u.y, threat.x, threat.y) < 14 * World.TILE) send.add(u);
                if (send.size() >= 6) break;
            }
            if (!send.isEmpty()) w.orderMove(send, threat.x, threat.y, true);
        }
    }

    /** Infantaria parada perto de um veículo abandonado vai ocupá-lo. */
    private void board(ArrayList<Unit> mine) {
        for (Unit v : w.units) {
            if (!v.alive || v.team != World.NEUTRAL) continue;
            boolean claimed = false;
            for (Unit u : mine) if (u.order == Unit.BOARD && u.target == v) { claimed = true; break; }
            if (claimed) continue;
            Unit best = null;
            float bd = 16 * World.TILE;
            for (Unit u : mine) {
                if (u.type.vehicle || !available(u)) continue;
                float d = World.dist(u.x, u.y, v.x, v.y);
                if (d < bd) { bd = d; best = u; }
            }
            if (best != null) w.orderBoard(best, v);
        }
    }

    private void attackOrCapture(ArrayList<Unit> mine) {
        Building target = w.forts[enemy()];
        Building home = w.forts[team];
        // guarnição: os dois mais próximos do forte ficam
        ArrayList<Unit> free = new ArrayList<>();
        for (Unit u : mine) if (available(u)) free.add(u);
        if (mine.size() > 6 && home != null) {
            for (int k = 0; k < 2 && !free.isEmpty(); k++) {
                Unit best = null;
                float bd = Float.MAX_VALUE;
                for (Unit u : free) {
                    float d = World.dist(u.x, u.y, home.x, home.y);
                    if (d < bd) { bd = d; best = u; }
                }
                if (bd < 8 * World.TILE) free.remove(best);
            }
        }
        if (free.isEmpty()) return;

        int owned = w.ownedSectors(team);
        boolean go = target != null && target.alive && mine.size() >= assault && (owned >= 4 || w.time > 240);
        if (go) {
            ArrayList<Unit> army = new ArrayList<>();
            for (Unit u : mine) if (available(u) || u.order == Unit.AMOVE) army.add(u);
            int keep = Math.min(2, army.size() / 5);
            for (int i = 0; i < keep; i++) army.remove(army.size() - 1);
            if (army.size() >= assault * 0.6f) {
                w.orderMove(army, target.x, target.y, true);
                return;
            }
        }

        // capturar bandeiras: os setores mais vantajosos e mais perto
        for (Unit u : free) {
            Sector best = null;
            float bs = Float.MAX_VALUE;
            for (Sector s : w.sectors) {
                if (s.owner == team) continue;
                int need = s.owner == World.NEUTRAL ? 2 : 4;
                if (hasGun(s)) need += 2;
                if (heading(mine, s) >= need) continue;
                float score = World.dist(u.x, u.y, s.fx, s.fy) / World.TILE;
                if (s.owner == enemy()) score += 6;
                if (hasGun(s)) score += 6;
                for (Building b : s.buildings) if (b.isFactory()) score -= 5;
                if (score < bs) { bs = score; best = s; }
            }
            if (best == null) {
                // tudo atribuído: junta-se ao setor inimigo mais próximo, ou ao forte
                if (target != null && target.alive && mine.size() >= assault / 2) {
                    ArrayList<Unit> one = new ArrayList<>();
                    one.add(u);
                    w.orderMove(one, target.x, target.y, true);
                }
                continue;
            }
            ArrayList<Unit> one = new ArrayList<>();
            one.add(u);
            float ox = (w.rnd.nextFloat() - 0.5f) * 0.6f * World.TILE, oy = (w.rnd.nextFloat() - 0.5f) * 0.6f * World.TILE;
            w.orderMove(one, best.fx + ox, best.fy + oy, true);
        }
    }

    private boolean hasGun(Sector s) {
        for (Building b : s.buildings) if (b.kind == Building.TURRET && b.alive && b.team == enemy()) return true;
        return false;
    }

    private int heading(ArrayList<Unit> mine, Sector s) {
        int n = 0;
        for (Unit u : mine)
            if (u.order == Unit.AMOVE && World.dist(u.ox, u.oy, s.fx, s.fy) < 2 * World.TILE) n++;
        return n;
    }
}
