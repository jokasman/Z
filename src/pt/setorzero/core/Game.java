package pt.setorzero.core;

import java.util.ArrayList;

/** Ecrãs, câmara, controlos por toque e interface do jogo. Independente do Android. */
public final class Game implements World.Listener {
    public static final int TITLE = 0, HELP = 1, PLAY = 2, PAUSE = 3, END = 4;
    static final float STEP = 1f / 30f;
    static final float T = World.TILE;
    static final String VERSION = "0.1.0";
    static final String[] DIFF = {"Fácil", "Normal", "Difícil"};

    public int state = TITLE;
    public World world;
    private World demo;
    private int difficulty = 1;
    private final Sfx sfx;
    public float cacheScale = 2f;

    private float sw = 960, sh = 540, dp = 1;
    float camX, camY, zoom = 1;
    private float dCamX, dCamY, dZoom = 1, dDir = 1;
    private float clock, acc, endT;
    private boolean sentEnd;

    public final ArrayList<Unit> sel = new ArrayList<>();
    Building panel;
    private final ArrayList<Msg> msgs = new ArrayList<>();
    private final ArrayList<Btn> btns = new ArrayList<>();
    private final float[] soundT = new float[Sfx.COUNT];

    // toque
    private int p1 = -1, p2 = -1;
    private float p1x, p1y, p2x, p2y, downX, downY;
    private boolean dragging, boxing, panning, miniDrag;
    private float pinchD, pinchZ, pinchWx, pinchWy;
    private float lastTapT = -10;
    private UnitType lastTapType;
    private float markX, markY, markT = -10;
    private int markColor;
    private float hintT;

    static final class Msg {
        String text; int color; float t; boolean brix;
    }

    static final class Btn {
        final float x, y, w, h; final String label; final int id; final boolean primary;
        Btn(float x, float y, float w, float h, String label, int id, boolean primary) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.label = label; this.id = id; this.primary = primary;
        }
        boolean hit(float px, float py) { return px >= x && px <= x + w && py >= y && py <= y + h; }
    }

    // ids dos botões
    static final int B_EASY = 1, B_NORMAL = 2, B_HARD = 3, B_HELP = 4, B_BACK = 5, B_RESUME = 6, B_RESTART = 7,
        B_MENU = 8, B_AGAIN = 9, B_PAUSE = 10, B_ALL = 11, B_NONE = 12, B_CLOSE = 13, B_MAKE = 100;

    public Game(Sfx sfx) {
        this.sfx = sfx == null ? Sfx.NONE : sfx;
        newDemo();
    }

    // ---------------------------------------------------------------- ciclo

    public void resize(float w, float h, float density) {
        sw = w; sh = h; dp = density;
        if (world != null) clampCam();
        dZoom = Math.max(sh / (14 * T), 0.6f * dp);
    }

    public void start(int diff) {
        difficulty = diff;
        world = new World(System.nanoTime(), diff);
        world.listener = this;
        world.ais.add(new Ai(world, World.ENEMY, diff));
        sel.clear();
        msgs.clear();
        panel = null;
        sentEnd = false;
        endT = 0;
        acc = 0;
        zoom = defaultZoom();
        Building f = world.forts[World.PLAYER];
        centerOn(f.x + 3 * T, f.y - 3 * T);
        state = PLAY;
        hintT = 14;
        brix("Comandante Brix: capturem as bandeiras! Mais setores, fábricas mais rápidas.");
        brix("Destruam o forte ciano no canto oposto do mapa.");
    }

    private float defaultZoom() { return clampZoom(38 * dp / T); }

    private float clampZoom(float z) {
        float min = Math.max(sw / (world != null ? world.W * T : 1536), sh / (40 * T)) * 0.98f;
        min = Math.min(min, 26 * dp / T);
        return Math.max(min, Math.min(z, 80 * dp / T));
    }

    private void newDemo() {
        demo = new World(System.nanoTime(), 1);
        demo.ais.add(new Ai(demo, World.PLAYER, 1));
        demo.ais.add(new Ai(demo, World.ENEMY, 1));
        dCamX = 2 * T;
        dCamY = 10 * T;
    }

    public void update(float dt) {
        dt = Math.min(dt, 0.1f);
        clock += dt;
        for (int i = 0; i < soundT.length; i++) soundT[i] -= dt;
        for (int i = msgs.size() - 1; i >= 0; i--) {
            Msg m = msgs.get(i);
            m.t -= dt;
            if (m.t <= 0) msgs.remove(i);
        }
        hintT -= dt;
        if (state == TITLE || state == HELP) {
            demo.update(dt);
            if (demo.winner >= 0 || demo.time > 600) newDemo();
            float vw = sw / dZoom;
            dCamX += dDir * 14 * dt;
            if (dCamX > demo.W * T - vw) dDir = -1;
            if (dCamX < 0) dDir = 1;
            dCamY = (demo.H * T - sh / dZoom) * 0.5f + (float) Math.sin(clock * 0.07f) * 4 * T;
            return;
        }
        if (world == null) return;
        if (state == PLAY || state == END) {
            acc += dt;
            int n = 0;
            while (acc >= STEP && n++ < 5) {
                world.update(STEP);
                acc -= STEP;
            }
            if (acc > STEP) acc = 0;
        }
        for (int i = sel.size() - 1; i >= 0; i--) {
            Unit u = sel.get(i);
            if (u.alive && u.team == World.PLAYER) continue;
            u.selected = false;
            sel.remove(i);
            if (u.boardedInto != null && u.boardedInto.alive && u.boardedInto.team == World.PLAYER && !sel.contains(u.boardedInto)) {
                u.boardedInto.selected = true;
                sel.add(u.boardedInto);
            }
        }
        if (panel != null && panel.team != World.PLAYER) panel = null;
        if (world.winner >= 0 && state == PLAY) {
            endT += dt;
            if (!sentEnd) {
                sentEnd = true;
                boolean win = world.winner == World.PLAYER;
                sfx.play(win ? Sfx.WIN : Sfx.LOSE, 1f);
                brix(win ? "Comandante Brix: vitória! O forte ciano caiu." : "Comandante Brix: perdemos a base... retirada!");
            }
            if (endT > 2.5f) { state = END; panel = null; }
        }
    }

    /** Botão "voltar" do Android. Devolve false quando o jogo deve fechar. */
    public boolean back() {
        switch (state) {
            case PLAY:
                if (panel != null) { panel = null; return true; }
                state = PAUSE; return true;
            case PAUSE: state = PLAY; return true;
            case HELP: state = TITLE; return true;
            case END: state = TITLE; world = null; return true;
            default: return false;
        }
    }

    /** A aplicação foi para segundo plano. */
    public void pause() {
        if (state == PLAY) state = PAUSE;
        p1 = p2 = -1;
        panning = boxing = dragging = miniDrag = false;
    }

    // ---------------------------------------------------------------- mensagens e sons

    @Override public void onMessage(String text, int team, boolean important) {
        Msg m = new Msg();
        m.text = text;
        m.color = team == World.PLAYER ? 0xFFFFC27A : team == World.ENEMY ? 0xFF8FE6F0 : 0xFFE8E8E8;
        m.t = important ? 6 : 4;
        addMsg(m);
    }

    private void brix(String text) {
        Msg m = new Msg();
        m.text = text;
        m.color = 0xFFFFFFFF;
        m.t = 7;
        m.brix = true;
        addMsg(m);
    }

    private void addMsg(Msg m) {
        msgs.add(m);
        while (msgs.size() > 4) msgs.remove(0);
    }

    @Override public void onSound(int id, float x, float y) {
        if (state != PLAY || world == null) return;
        float vw = sw / zoom, vh = sh / zoom;
        float m = 4 * T;
        boolean ui = id == Sfx.READY || id == Sfx.CAPTURE || id == Sfx.ALERT || id == Sfx.BOARD;
        boolean inView = x >= camX && x <= camX + vw && y >= camY && y <= camY + vh;
        boolean near = x >= camX - m && x <= camX + vw + m && y >= camY - m && y <= camY + vh + m;
        if (!ui && !near) return;
        float gap = id == Sfx.MG || id == Sfx.SHOT ? 0.07f : id == Sfx.EXPLODE ? 0.09f : 0.04f;
        if (soundT[id] > 0) return;
        soundT[id] = gap;
        sfx.play(id, ui || inView ? 0.9f : 0.35f);
    }

    // ---------------------------------------------------------------- câmara

    private void centerOn(float wx, float wy) {
        camX = wx - sw / zoom / 2;
        camY = wy - sh / zoom / 2;
        clampCam();
    }

    private void clampCam() {
        float vw = sw / zoom, vh = sh / zoom;
        float mw = world.W * T, mh = world.H * T;
        float top = 40 * dp / zoom;
        if (vw >= mw) camX = (mw - vw) / 2;
        else camX = Math.max(0, Math.min(camX, mw - vw));
        if (vh >= mh + top) camY = (mh - vh) / 2 - top / 2;
        else camY = Math.max(-top, Math.min(camY, mh - vh));
    }

    // ---------------------------------------------------------------- toque

    public void pointerDown(int id, float x, float y) {
        if (p1 == -1) {
            p1 = id; p1x = x; p1y = y;
            downX = x; downY = y;
            dragging = boxing = miniDrag = false;
            if (state == PLAY && inMinimap(x, y)) { miniDrag = true; miniJump(x, y); }
        } else if (p2 == -1 && state == PLAY) {
            p2 = id; p2x = x; p2y = y;
            panning = true;
            boxing = false;
            miniDrag = false;
            pinchD = Math.max(10, World.dist(p1x, p1y, p2x, p2y));
            pinchZ = zoom;
            float mx = (p1x + p2x) / 2, my = (p1y + p2y) / 2;
            pinchWx = camX + mx / zoom;
            pinchWy = camY + my / zoom;
        }
    }

    public void pointerMove(int id, float x, float y) {
        if (id == p1) { p1x = x; p1y = y; }
        else if (id == p2) { p2x = x; p2y = y; }
        else return;
        if (panning && p1 != -1 && p2 != -1) {
            float d = Math.max(10, World.dist(p1x, p1y, p2x, p2y));
            zoom = clampZoom(pinchZ * d / pinchD);
            float mx = (p1x + p2x) / 2, my = (p1y + p2y) / 2;
            camX = pinchWx - mx / zoom;
            camY = pinchWy - my / zoom;
            clampCam();
            return;
        }
        if (id != p1 || panning) return;
        if (miniDrag) { miniJump(x, y); return; }
        if (!dragging && World.dist(downX, downY, x, y) > 12 * dp) {
            dragging = true;
            if (state == PLAY && !overHud(downX, downY)) boxing = true;
        }
    }

    public void pointerUp(int id, float x, float y) {
        if (id == p2) {
            p2 = -1;
        } else if (id == p1) {
            if (!panning) {
                if (boxing) boxSelect(downX, downY, x, y);
                else if (!dragging && !miniDrag) tap(x, y);
            }
            p1 = -1;
            if (p2 != -1) { p1 = p2; p1x = p2x; p1y = p2y; p2 = -1; }
            boxing = dragging = miniDrag = false;
        }
        if (p1 == -1 && p2 == -1) panning = false;
    }

    public void pointerCancel() {
        p1 = p2 = -1;
        panning = boxing = dragging = miniDrag = false;
    }

    private void tap(float x, float y) {
        for (int i = btns.size() - 1; i >= 0; i--) {
            Btn b = btns.get(i);
            if (b.hit(x, y)) { button(b.id); return; }
        }
        if (state != PLAY || world == null || world.winner >= 0) return;
        if (panel != null) {
            float[] r = panelRect();
            if (!(x >= r[0] && x <= r[0] + r[2] && y >= r[1] && y <= r[1] + r[3])) panel = null;
            return;
        }
        if (overHud(x, y)) return;
        worldTap(camX + x / zoom, camY + y / zoom);
    }

    private void worldTap(float wx, float wy) {
        float tol = 18 * dp / zoom;
        Unit u = null;
        float bd = Float.MAX_VALUE;
        for (Unit o : world.units) {
            if (!o.alive) continue;
            float d = World.dist(wx, wy, o.x, o.y) - o.radius;
            if (d < tol && d < bd) { bd = d; u = o; }
        }
        Building b = null;
        for (Building o : world.buildings) if (o.contains(wx, wy, 4)) b = o;

        if (u != null && u.team == World.PLAYER) {
            if (clock - lastTapT < 0.4f && lastTapType == u.type) {
                clearSel();
                float vw = sw / zoom, vh = sh / zoom;
                for (Unit o : world.units)
                    if (o.alive && o.team == World.PLAYER && o.type == u.type
                        && o.x >= camX && o.x <= camX + vw && o.y >= camY && o.y <= camY + vh) addSel(o);
            } else {
                clearSel();
                addSel(u);
            }
            lastTapT = clock;
            lastTapType = u.type;
            sfx.play(Sfx.CLICK, 0.6f);
            return;
        }
        lastTapType = null;
        if (u != null && u.team == World.ENEMY && !sel.isEmpty()) {
            world.orderAttack(sel, u);
            mark(u.x, u.y, 0xFFFF5040);
            return;
        }
        if (u != null && u.team == World.NEUTRAL && !sel.isEmpty()) {
            Unit pilot = null;
            float pd = Float.MAX_VALUE;
            for (Unit s : sel) {
                if (s.type.vehicle) continue;
                float d = World.dist(s.x, s.y, u.x, u.y);
                if (d < pd) { pd = d; pilot = s; }
            }
            if (pilot != null) {
                ArrayList<Unit> rest = new ArrayList<>(sel);
                rest.remove(pilot);
                if (!rest.isEmpty()) world.orderMove(rest, u.x, u.y + T * 1.5f, false);
                world.orderBoard(pilot, u);
                mark(u.x, u.y, 0xFF7CFC6A);
                return;
            }
        }
        if (b != null) {
            if (b.isFactory() && b.team == World.PLAYER) {
                panel = b;
                sfx.play(Sfx.CLICK, 0.6f);
                return;
            }
            if (b.attackable() && b.team == World.ENEMY && !sel.isEmpty()) {
                world.orderAttack(sel, b);
                mark(b.x, b.y, 0xFFFF5040);
                return;
            }
            if (b.isFactory() && b.sector != null) {
                if (sel.isEmpty()) {
                    onMessage("Captura a bandeira de " + b.sector.name + " para ficares com esta fábrica.", World.NEUTRAL, false);
                } else {
                    world.orderMove(sel, b.sector.fx, b.sector.fy, false);
                    mark(b.sector.fx, b.sector.fy, 0xFF7CFC6A);
                }
                return;
            }
        }
        if (!sel.isEmpty()) {
            world.orderMove(sel, wx, wy, false);
            mark(wx, wy, 0xFF7CFC6A);
        }
    }

    private void mark(float x, float y, int color) {
        markX = x; markY = y; markT = clock; markColor = color;
        sfx.play(Sfx.CLICK, 0.5f);
    }

    private void boxSelect(float ax, float ay, float bx, float by) {
        float x0 = camX + Math.min(ax, bx) / zoom, x1 = camX + Math.max(ax, bx) / zoom;
        float y0 = camY + Math.min(ay, by) / zoom, y1 = camY + Math.max(ay, by) / zoom;
        clearSel();
        for (Unit u : world.units)
            if (u.alive && u.team == World.PLAYER && u.x + u.radius >= x0 && u.x - u.radius <= x1
                && u.y + u.radius >= y0 && u.y - u.radius <= y1) addSel(u);
        if (!sel.isEmpty()) sfx.play(Sfx.CLICK, 0.6f);
    }

    private void addSel(Unit u) { if (!u.selected) { u.selected = true; sel.add(u); } }

    private void clearSel() {
        for (Unit u : sel) u.selected = false;
        sel.clear();
    }

    private void button(int id) {
        sfx.play(Sfx.CLICK, 0.7f);
        switch (id) {
            case B_EASY: start(0); break;
            case B_NORMAL: start(1); break;
            case B_HARD: start(2); break;
            case B_HELP: state = HELP; break;
            case B_BACK: state = TITLE; break;
            case B_RESUME: state = PLAY; break;
            case B_RESTART: case B_AGAIN: start(difficulty); break;
            case B_MENU: state = TITLE; world = null; break;
            case B_PAUSE: state = PAUSE; break;
            case B_ALL:
                clearSel();
                for (Unit u : world.units) if (u.alive && u.team == World.PLAYER) addSel(u);
                break;
            case B_NONE: clearSel(); break;
            case B_CLOSE: panel = null; break;
            default:
                if (id >= B_MAKE && panel != null) {
                    UnitType t = panel.buildable()[id - B_MAKE];
                    if (panel.making != t) { panel.making = t; panel.progress = 0; }
                }
        }
    }

    // ---------------------------------------------------------------- layout

    private float barH() { return 38 * dp; }

    private float[] miniRect() {
        float mw = Math.min(170 * dp, sw * 0.26f);
        float mh = mw * 36 / 48f;
        return new float[]{sw - mw - 10 * dp, sh - mh - 10 * dp, mw, mh};
    }

    private boolean inMinimap(float x, float y) {
        float[] r = miniRect();
        return x >= r[0] - 6 * dp && x <= r[0] + r[2] + 6 * dp && y >= r[1] - 6 * dp && y <= r[1] + r[3] + 6 * dp;
    }

    private void miniJump(float x, float y) {
        float[] r = miniRect();
        float fx = Math.max(0, Math.min(1, (x - r[0]) / r[2])), fy = Math.max(0, Math.min(1, (y - r[1]) / r[3]));
        centerOn(fx * world.W * T, fy * world.H * T);
    }

    private boolean overHud(float x, float y) {
        if (y < barH()) return true;
        if (inMinimap(x, y)) return true;
        for (Btn b : btns) if (b.hit(x, y)) return true;
        return false;
    }

    private float[] panelRect() {
        float w = Math.min(sw - 20 * dp, 520 * dp), h = 150 * dp;
        return new float[]{(sw - w) / 2, sh - h - 8 * dp, w, h};
    }

    // ---------------------------------------------------------------- desenho

    public void render(Gfx g) {
        btns.clear();
        if (state == TITLE || state == HELP) {
            renderWorld(g, demo, dCamX, dCamY, dZoom);
            if (state == TITLE) renderTitle(g); else renderHelp(g);
            return;
        }
        if (world == null) return;
        renderWorld(g, world, camX, camY, zoom);
        g.save();
        g.scale(zoom);
        g.translate(-camX, -camY);
        float since = clock - markT;
        if (since < 0.6f) {
            float p = since / 0.6f;
            g.color(Draw.alpha(markColor, 1 - p));
            g.strokeOval(markX, markY, 14 * (1 - p * 0.6f), 10 * (1 - p * 0.6f), 2.2f);
            g.strokeOval(markX, markY, 5, 3.5f, 1.6f);
        }
        g.restore();
        if (boxing && p1 != -1) {
            float x0 = Math.min(downX, p1x), y0 = Math.min(downY, p1y);
            float bw = Math.abs(p1x - downX), bh = Math.abs(p1y - downY);
            g.color(0x2A7CFC6A);
            g.fillRect(x0, y0, bw, bh);
            g.color(0xCC7CFC6A);
            g.strokeRect(x0, y0, bw, bh, 1.5f * dp);
        }
        renderHud(g);
        if (state == PAUSE) renderPause(g);
        else if (state == END) renderEnd(g);
    }

    private void renderWorld(Gfx g, World w, float cx, float cy, float z) {
        g.color(0xFF2A2C24);
        g.fillRect(0, 0, sw, sh);
        g.save();
        g.scale(z);
        g.translate(-cx, -cy);
        Draw.world(g, w, cx, cy, cx + sw / z, cy + sh / z, clock, cacheScale);
        g.restore();
    }

    private void renderHud(Gfx g) {
        float bh = barH();
        g.color(0xC0101214);
        g.fillRect(0, 0, sw, bh);
        float ts = (sw / dp < 720 ? 12.5f : 14) * dp;
        float x = 12 * dp;
        x = teamStat(g, x, bh / 2, World.PLAYER, "Tu", ts);
        x += 14 * dp;
        x = teamStat(g, x, bh / 2, World.ENEMY, "Inimigo", ts);
        int secs = (int) world.time;
        g.color(0xFFE8E8E8);
        g.text(String.format("%d:%02d", secs / 60, secs % 60), x + 18 * dp, bh / 2, ts, Gfx.LEFT, true);
        // pausa
        float pb = bh - 8 * dp;
        Btn pause = new Btn(sw - pb - 6 * dp, 4 * dp, pb, pb, "", B_PAUSE, false);
        btns.add(pause);
        g.color(0x30FFFFFF);
        g.fillRoundRect(pause.x, pause.y, pause.w, pause.h, 6 * dp);
        g.color(0xFFFFFFFF);
        g.fillRect(pause.x + pb * 0.33f, pause.y + pb * 0.28f, pb * 0.12f, pb * 0.44f);
        g.fillRect(pause.x + pb * 0.55f, pause.y + pb * 0.28f, pb * 0.12f, pb * 0.44f);
        // território
        float tf = world.territoryFactor(World.PLAYER) * world.prodMult[World.PLAYER];
        g.color(0xFFBFC4C9);
        g.text((sw / dp < 720 ? "×" : "Produção ×") + String.format("%.1f", tf), pause.x - 10 * dp, bh / 2, 12 * dp, Gfx.RIGHT, false);

        // mensagens
        float my = bh + 8 * dp;
        for (Msg m : state == PLAY ? msgs : new ArrayList<Msg>()) {
            float a = Math.min(1, m.t);
            float size = 13 * dp;
            float tw = g.textWidth(m.text, size, true);
            float extra = m.brix ? 30 * dp : 0;
            float mw = tw + 20 * dp + extra, mh = 24 * dp;
            float mx = (sw - mw) / 2;
            g.color(Draw.alpha(0xFF101214, 0.75f * a));
            g.fillRoundRect(mx, my, mw, mh, 6 * dp);
            if (m.brix) Draw.brix(g, mx + 16 * dp, my + mh / 2 + dp, 20 * dp, clock);
            g.color(Draw.alpha(m.color, a));
            g.text(m.text, mx + 10 * dp + extra, my + mh / 2, size, Gfx.LEFT, true);
            my += mh + 4 * dp;
        }

        // minimapa
        float[] r = miniRect();
        Draw.minimap(g, world, r[0], r[1], r[2], r[3], camX, camY, sw / zoom, sh / zoom);

        // botões de seleção
        float bw = 84 * dp, bhh = 40 * dp, by = sh - bhh - 10 * dp;
        Btn all = new Btn(10 * dp, by, bw, bhh, "Todos", B_ALL, false);
        Btn none = new Btn(10 * dp + bw + 8 * dp, by, bw, bhh, "Nenhum", B_NONE, false);
        if (panel == null) {
            drawBtn(g, all);
            drawBtn(g, none);
            btns.add(all);
            btns.add(none);
            if (!sel.isEmpty()) {
                g.color(0xFF7CFC6A);
                g.text(selSummary(), 12 * dp, by - 14 * dp, 13 * dp, Gfx.LEFT, true);
            }
        }
        if (hintT > 0 && panel == null) {
            float a = Math.min(1, hintT);
            String h = "Arrasta para selecionar · toca no chão para mover · dois dedos movem e aproximam a câmara";
            float size = 12 * dp;
            float tw = g.textWidth(h, size, false);
            if (tw > miniRect()[0] - 30 * dp) {
                h = "Arrastar: selecionar · tocar: mover · dois dedos: câmara";
                tw = g.textWidth(h, size, false);
            }
            float hy = sh - 110 * dp;
            float hx = Math.max(tw / 2 + 12 * dp, Math.min(sw / 2, miniRect()[0] - 14 * dp - tw / 2));
            g.color(Draw.alpha(0xFF101214, 0.7f * a));
            g.fillRoundRect(hx - tw / 2 - 10 * dp, hy - 13 * dp, tw + 20 * dp, 26 * dp, 6 * dp);
            g.color(Draw.alpha(0xFFFFFFFF, a));
            g.text(h, hx, hy, size, Gfx.CENTER, false);
        }
        if (panel != null) renderPanel(g);
    }

    private String selSummary() {
        int[] n = new int[5];
        for (Unit u : sel) if (u.type.id < 5) n[u.type.id]++;
        StringBuilder sb = new StringBuilder();
        for (UnitType t : UnitType.ALL) {
            if (n[t.id] == 0) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(n[t.id]).append(' ').append(t.name);
        }
        return sb.toString();
    }

    private float teamStat(Gfx g, float x, float cy, int team, String label, float ts) {
        int c = Draw.teamColor(team);
        g.color(c);
        g.fillRoundRect(x, cy - 7 * dp, 14 * dp, 14 * dp, 3 * dp);
        x += 20 * dp;
        int ns = world.ownedSectors(team);
        String s = label + ": " + ns + (ns == 1 ? " setor · " : " setores · ") + world.unitCount(team) + " unid.";
        g.color(0xFFF2F2F2);
        g.text(s, x, cy, ts, Gfx.LEFT, true);
        return x + g.textWidth(s, ts, true);
    }

    private void renderPanel(Gfx g) {
        float[] r = panelRect();
        float x = r[0], y = r[1], w = r[2], h = r[3];
        g.color(0xF0151719);
        g.fillRoundRect(x, y, w, h, 10 * dp);
        g.color(Draw.C_PLAYER);
        g.strokeRoundRect(x, y, w, h, 10 * dp, 1.5f * dp);
        g.color(0xFFFFFFFF);
        g.text(panel.title() + " · " + panel.sector.name, x + 14 * dp, y + 18 * dp, 14 * dp, Gfx.LEFT, true);
        Btn close = new Btn(x + w - 44 * dp, y + 2 * dp, 42 * dp, 34 * dp, "", B_CLOSE, false);
        btns.add(close);
        float ccx = close.x + close.w / 2, ccy = close.y + close.h / 2, cr = 6 * dp;
        g.color(0xFFBBBBBB);
        g.line(ccx - cr, ccy - cr, ccx + cr, ccy + cr, 2 * dp);
        g.line(ccx - cr, ccy + cr, ccx + cr, ccy - cr, 2 * dp);
        // progresso
        UnitType cur = panel.making;
        float py = y + 38 * dp;
        g.color(0xFF2A2D31);
        g.fillRoundRect(x + 14 * dp, py, w - 28 * dp, 8 * dp, 4 * dp);
        g.color(Draw.C_PLAYER);
        g.fillRoundRect(x + 14 * dp, py, (w - 28 * dp) * Math.min(1, panel.progress), 8 * dp, 4 * dp);
        if (cur != null) {
            float left = (1 - panel.progress) * world.buildSeconds(cur, World.PLAYER);
            boolean cap = world.unitCount(World.PLAYER) >= World.UNIT_CAP;
            g.color(0xFFCFD3D7);
            g.text(cap ? "Limite de " + World.UNIT_CAP + " unidades atingido" : "A fabricar " + cur.name + " · faltam " + (int) Math.ceil(left) + " s",
                x + 14 * dp, py + 18 * dp, 12 * dp, Gfx.LEFT, false);
        }
        // escolhas
        UnitType[] types = panel.buildable();
        float gap = 8 * dp;
        float bw = (w - 28 * dp - gap * (types.length - 1)) / types.length;
        float bh = h - (py + 32 * dp - y) - 10 * dp;
        for (int i = 0; i < types.length; i++) {
            UnitType t = types[i];
            Btn b = new Btn(x + 14 * dp + i * (bw + gap), py + 30 * dp, bw, bh, t.name, B_MAKE + i, false);
            btns.add(b);
            boolean on = t == cur;
            g.color(on ? 0xFF3A2A18 : 0xFF24272B);
            g.fillRoundRect(b.x, b.y, b.w, b.h, 8 * dp);
            if (on) {
                g.color(Draw.C_PLAYER);
                g.strokeRoundRect(b.x, b.y, b.w, b.h, 8 * dp, 2 * dp);
            }
            g.save();
            g.translate(b.x + 26 * dp, b.y + b.h / 2);
            float s = 30 * dp / (t.radius * 2.6f);
            g.scale(s);
            Draw.unitShape(g, t, World.PLAYER, (float) -Math.PI / 2, (float) -Math.PI / 2, clock * 30 * (on ? 1 : 0), false);
            g.restore();
            float tx = b.x + 52 * dp;
            g.color(0xFFFFFFFF);
            g.text(t.name, tx, b.y + b.h * 0.3f, 13 * dp, Gfx.LEFT, true);
            g.color(0xFFAEB4BA);
            g.text(t.desc, tx, b.y + b.h * 0.55f, 10.5f * dp, Gfx.LEFT, false);
            g.text((int) Math.ceil(world.buildSeconds(t, World.PLAYER)) + " s", tx, b.y + b.h * 0.78f, 10.5f * dp, Gfx.LEFT, false);
        }
    }

    private void drawBtn(Gfx g, Btn b) {
        g.color(b.primary ? Draw.C_PLAYER : 0xD0202326);
        g.fillRoundRect(b.x, b.y, b.w, b.h, 8 * dp);
        g.color(b.primary ? 0xFFFFD2A0 : 0x50FFFFFF);
        g.strokeRoundRect(b.x, b.y, b.w, b.h, 8 * dp, 1.2f * dp);
        g.color(b.primary ? 0xFF1A1206 : 0xFFFFFFFF);
        g.text(b.label, b.x + b.w / 2, b.y + b.h / 2, 15 * dp, Gfx.CENTER, true);
    }

    private void dim(Gfx g, float a) {
        g.color(Draw.alpha(0xFF0B0C0E, a));
        g.fillRect(0, 0, sw, sh);
    }

    private void renderTitle(Gfx g) {
        dim(g, 0.55f);
        float s = Math.min(sw / 960f, sh / 540f) * 1f;
        s = Math.max(s, 0.6f * dp);
        float cx = sw / 2;
        float ty = sh * 0.2f;
        float size = Math.min(64 * dp, sw / 9);
        g.color(0xFF000000);
        g.text("SETOR ZERO", cx + 3 * dp, ty + 3 * dp, size, Gfx.CENTER, true);
        g.color(Draw.C_PLAYER);
        g.text("SETOR ZERO", cx, ty, size, Gfx.CENTER, true);
        g.color(0xFFE8E8E8);
        g.text("Captura bandeiras · domina as fábricas · derruba o forte inimigo", cx, ty + size * 0.75f, 14 * dp, Gfx.CENTER, false);

        float bw = Math.min(150 * dp, (sw - 60 * dp) / 3), bh = 50 * dp, gap = 12 * dp;
        float total = bw * 3 + gap * 2;
        float by = sh * 0.52f;
        g.color(0xFFBFC4C9);
        g.text("Novo jogo", cx, by - 18 * dp, 13 * dp, Gfx.CENTER, true);
        int[] ids = {B_EASY, B_NORMAL, B_HARD};
        for (int i = 0; i < 3; i++) {
            Btn b = new Btn(cx - total / 2 + i * (bw + gap), by, bw, bh, DIFF[i], ids[i], i == 1);
            btns.add(b);
            drawBtn(g, b);
        }
        float hw = 150 * dp, hh = 44 * dp;
        Btn help = new Btn(sw - hw - 16 * dp, sh - hh - 30 * dp, hw, hh, "Como jogar", B_HELP, false);
        btns.add(help);
        drawBtn(g, help);

        // Comandante Brix
        float bs = Math.min(90 * dp, sh * 0.18f);
        float bx = 24 * dp + bs * 0.6f, byy = sh - bs * 0.75f - 14 * dp;
        Draw.brix(g, bx, byy, bs, clock);
        g.color(0xFFFFFFFF);
        g.text("Comandante Brix", bx + bs * 0.7f, byy - bs * 0.15f, 13 * dp, Gfx.LEFT, true);
        g.color(0xFFBFC4C9);
        g.text("\"Mais território, mais fábricas a trabalhar. Avançar!\"", bx + bs * 0.7f, byy + bs * 0.12f, 12 * dp, Gfx.LEFT, false);
        g.color(0x80FFFFFF);
        g.text("v" + VERSION, sw - 16 * dp, sh - 14 * dp, 11 * dp, Gfx.RIGHT, false);
    }

    private void renderHelp(Gfx g) {
        dim(g, 0.78f);
        String[] lines = {
            "OBJETIVO",
            "Destrói o forte ciano (canto superior direito) antes que destruam o teu.",
            "",
            "SETORES E BANDEIRAS",
            "O mapa tem 9 setores. Basta uma unidade tocar na bandeira para o setor ser teu,",
            "com as fábricas e os canhões que lá estão. Quantos mais setores, mais rápido fabricas.",
            "A bandeira de cada base só pode ser tomada depois de o forte dessa base cair.",
            "",
            "CONTROLOS",
            "Arrastar um dedo: caixa de seleção · Tocar numa unidade: selecionar (2 toques: todas do mesmo tipo)",
            "Tocar no chão: mover · Tocar num inimigo: atacar · Tocar num veículo abandonado: ocupá-lo",
            "Tocar numa fábrica tua: escolher o que fabrica · Dois dedos: mover e aproximar a câmara",
            "",
            "UNIDADES",
            "Batedor: rápido e barato · Atirador: alcance longo contra robôs · Bazuca: contra veículos e edifícios",
            "Jipe: muito rápido · Tanque: lento mas muito resistente",
        };
        float size = Math.min(13 * dp, sw / 70);
        float lh = size * 1.55f;
        float y = Math.max(20 * dp, (sh - lines.length * lh - 70 * dp) / 2);
        for (String l : lines) {
            boolean head = l.equals(l.toUpperCase()) && l.length() > 0;
            g.color(head ? Draw.C_PLAYER : 0xFFE8E8E8);
            g.text(l, sw / 2, y, head ? size * 1.1f : size, Gfx.CENTER, head);
            y += lh;
        }
        Btn back = new Btn(sw / 2 - 75 * dp, Math.min(y + 10 * dp, sh - 58 * dp), 150 * dp, 46 * dp, "Voltar", B_BACK, true);
        btns.add(back);
        drawBtn(g, back);
    }

    private void renderPause(Gfx g) {
        btns.clear();
        dim(g, 0.65f);
        g.color(0xFFFFFFFF);
        g.text("Pausa", sw / 2, sh * 0.25f, 36 * dp, Gfx.CENTER, true);
        g.color(0xFFBFC4C9);
        g.text("Dificuldade: " + DIFF[difficulty], sw / 2, sh * 0.25f + 32 * dp, 13 * dp, Gfx.CENTER, false);
        float bw = 210 * dp, bh = 46 * dp, gap = 10 * dp, y = sh * 0.42f;
        Btn[] bs = {
            new Btn(sw / 2 - bw / 2, y, bw, bh, "Continuar", B_RESUME, true),
            new Btn(sw / 2 - bw / 2, y + bh + gap, bw, bh, "Recomeçar", B_RESTART, false),
            new Btn(sw / 2 - bw / 2, y + 2 * (bh + gap), bw, bh, "Sair para o menu", B_MENU, false),
        };
        for (Btn b : bs) { btns.add(b); drawBtn(g, b); }
    }

    private void renderEnd(Gfx g) {
        btns.clear();
        dim(g, 0.6f);
        boolean win = world.winner == World.PLAYER;
        g.color(win ? Draw.C_PLAYER : 0xFFFF6A5A);
        g.text(win ? "VITÓRIA" : "DERROTA", sw / 2, sh * 0.2f, 48 * dp, Gfx.CENTER, true);
        int secs = (int) world.time;
        String[] st = {
            "Duração: " + secs / 60 + " min " + secs % 60 + " s · Dificuldade: " + DIFF[difficulty],
            "Unidades fabricadas: " + world.produced[1] + "  ·  Perdidas: " + world.lost[1] + "  ·  Abatidas: " + world.kills[1],
            "Bandeiras capturadas: " + world.captures[1] + "  ·  Setores no fim: " + world.ownedSectors(1) + " de 9",
        };
        float y = sh * 0.2f + 46 * dp;
        for (String s : st) {
            g.color(0xFFE8E8E8);
            g.text(s, sw / 2, y, 14 * dp, Gfx.CENTER, false);
            y += 24 * dp;
        }
        float bw = 190 * dp, bh = 46 * dp, gap = 12 * dp;
        float by = Math.min(y + 20 * dp, sh - bh - 16 * dp);
        Btn again = new Btn(sw / 2 - bw - gap / 2, by, bw, bh, "Jogar de novo", B_AGAIN, true);
        Btn menu = new Btn(sw / 2 + gap / 2, by, bw, bh, "Menu", B_MENU, false);
        btns.add(again);
        btns.add(menu);
        drawBtn(g, again);
        drawBtn(g, menu);
    }
}
