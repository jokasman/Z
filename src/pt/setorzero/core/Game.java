package pt.setorzero.core;

import java.util.ArrayList;

/**
 * Ecrãs, câmara, controlos por toque e interface. Tudo é desenhado numa resolução baixa
 * (cerca de 270 píxeis de altura) que a plataforma amplia sem suavizar, para o aspeto dos anos 90.
 */
public final class Game implements World.Listener {
    public static final int TITLE = 0, HELP = 1, PLAY = 2, PAUSE = 3, END = 4, LEVELS = 5, NETMENU = 6, HOSTING = 7, JOINING = 8, KEYPAD = 9;
    static final float STEP = 1f / 30f;
    static final float T = World.TILE;
    public static final String VERSION = "0.3.0";
    static final String[] DIFF = {"FÁCIL", "NORMAL", "DIFÍCIL"};
    static final float[] ZOOMS = {0.5f, 1f, 1.5f};

    // paleta da interface
    static final int METAL = 0xFF4E5564, METAL_L = 0xFF8A93A6, METAL_D = 0xFF22252C, DARK = 0xFF16181E,
        TXT = 0xFFF0ECD8, DIM = 0xFFA4ACB8, ACCENT = 0xFFF0A020, ACCENT_L = 0xFFFFD860, GOOD = 0xFF70E060, BAD = 0xFFF05040,
        SHADOW = 0xFF000000;

    public int state = TITLE;
    public World world;
    private World demo;
    private int difficulty;
    private int levelIdx;
    private final Sfx sfx;
    private final Store store;
    private int music = -2;

    private float sw = 480, sh = 270;
    float camX, camY, zoom = 1;
    private int zoomIdx = 1;
    private float dCamX, dCamY, dDir = 1;
    private float clock, acc, endT;
    private boolean sentEnd;

    public final ArrayList<Unit> sel = new ArrayList<>();
    /** Equipa de quem joga neste telemóvel (o convidado em rede joga com o ciano). */
    int me = World.PLAYER;
    Versus vs;
    private Net net;
    private boolean versusPick, helloSent, netLost, connecting;
    private int hostLevel;
    private String ipText = "";
    private java.util.List<String> ips;
    private float ipsT;
    Building panel;
    private final ArrayList<Msg> msgs = new ArrayList<>();
    private final ArrayList<Btn> btns = new ArrayList<>();
    private final float[] soundT = new float[Sfx.COUNT];

    private int p1 = -1, p2 = -1;
    private float p1x, p1y, p2x, p2y, downX, downY;
    private boolean dragging, boxing, panning, miniDrag;
    private float pinchD, pinchWx, pinchWy;
    private int pinchZoomIdx;
    private float lastTapT = -10;
    private UnitType lastTapType;
    private float markX, markY, markT = -10;
    private int markColor;
    private float hintT;
    private int pressed = -1;

    static final class Msg { String text; int color; float t; boolean brix; }

    static final class Btn {
        final float x, y, w, h; final String label; final int id; final boolean primary;
        Btn(float x, float y, float w, float h, String label, int id, boolean primary) {
            this.x = Math.round(x); this.y = Math.round(y); this.w = Math.round(w); this.h = Math.round(h);
            this.label = label; this.id = id; this.primary = primary;
        }
        boolean hit(float px, float py) { return px >= x - 2 && px <= x + w + 2 && py >= y - 2 && py <= y + h + 2; }
    }

    static final int B_CAMPAIGN = 1, B_HELP = 4, B_BACK = 5, B_RESUME = 6, B_RESTART = 7, B_MENU = 8, B_NEXT = 9,
        B_PAUSE = 10, B_ALL = 11, B_NONE = 12, B_CLOSE = 13, B_LEVELS = 14, B_SOUND = 15, B_MUSIC = 16, B_CRT = 17,
        B_DIFF = 20, B_LEVEL = 40, B_VERSUS = 60, B_CREATE = 61, B_JOINMENU = 62, B_KEYPAD = 63, B_CONNECT = 64,
        B_QUIT = 65, B_CANCEL = 66, B_FOUND = 70, B_KEY = 80, B_MAKE = 100;

    private int them() { return 3 - me; }

    public Game(Sfx sfx, Store store) {
        this.sfx = sfx == null ? Sfx.NONE : sfx;
        this.store = store == null ? new Store.Memory() : store;
        difficulty = this.store.get("dif", 1);
        newDemo();
    }

    public Game(Sfx sfx) { this(sfx, null); }

    public boolean crt() { return store.get("crt", 1) == 1; }

    private boolean soundOn() { return store.get("som", 1) == 1; }

    private boolean musicOn() { return store.get("musica", 1) == 1; }

    private void play(int id, float vol) { if (soundOn()) sfx.play(id, vol); }

    private void setMusic(int track) {
        int t = musicOn() ? track : Sfx.MUSIC_NONE;
        if (t != music) { music = t; sfx.music(t); }
    }

    // ---------------------------------------------------------------- ciclo

    /** Tamanho da superfície virtual (em píxeis grandes). */
    public void resize(float w, float h, float density) {
        sw = w; sh = h;
        if (world != null) clampCam();
    }

    public void start(int lvl) {
        closeNet();
        me = World.PLAYER;
        begin(lvl, System.nanoTime());
    }

    /** Começa uma partida (a solo, ou em rede quando vs está definido). */
    private void begin(int lvl, long seed) {
        levelIdx = Math.max(0, Math.min(Level.ALL.length - 1, lvl));
        Level L = Level.ALL[levelIdx];
        world = new World(seed, difficulty, L, vs != null);
        if (vs != null) vs.attach(world, this);
        else {
            world.listener = this;
            world.ais.add(new Ai(world, World.ENEMY, difficulty));
        }
        netLost = false;
        sel.clear();
        msgs.clear();
        panel = null;
        sentEnd = false;
        endT = 0;
        acc = 0;
        zoomIdx = 1;
        zoom = ZOOMS[zoomIdx];
        Building f = world.forts[me];
        float dir = me == World.PLAYER ? 1 : -1;
        centerOn(f.x + 3 * T * dir, f.y - 3 * T * dir);
        state = PLAY;
        hintT = 14;
        if (vs != null) {
            brix("2 JOGADORES · " + L.name.toUpperCase());
            brix(me == World.PLAYER ? "Tu és o LARANJA (em baixo à esquerda). Destrói o forte ciano!"
                : "Tu és o CIANO (em cima à direita). Destrói o forte laranja!");
            return;
        }
        brix("NÍVEL " + (levelIdx + 1) + ": " + L.name.toUpperCase());
        brix(L.brief);
        if (levelIdx == 0) brix("Mais setores, fábricas mais rápidas. Destruam o forte ciano!");
    }

    private void closeNet() {
        if (net != null) {
            if (net.connected && !net.closed) net.send(Versus.simple(Versus.M_BYE, ""));
            final Net n = net;
            new Thread(new Runnable() { public void run() {
                try { Thread.sleep(300); } catch (InterruptedException ignored) { }
                n.close();
            } }).start();
        }
        net = null;
        vs = null;
        helloSent = false;
        connecting = false;
    }

    private void newDemo() {
        demo = new World(System.nanoTime(), 1, Level.ALL[0]);
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
        if (state == HOSTING || state == JOINING || state == KEYPAD) lobby();
        if (state == TITLE || state == HELP || state == LEVELS || state == NETMENU || state == HOSTING
            || state == JOINING || state == KEYPAD) {
            setMusic(Sfx.MUSIC_MENU);
            demo.update(dt);
            if (demo.winner >= 0 || demo.time > 600) newDemo();
            float vw = sw;
            dCamX += dDir * 10 * dt;
            if (dCamX > demo.W * T - vw) dDir = -1;
            if (dCamX < 0) dDir = 1;
            dCamY = (demo.H * T - sh) * 0.5f + (float) Math.sin(clock * 0.07f) * 3 * T;
            return;
        }
        if (world == null) return;
        setMusic(state == END ? Sfx.MUSIC_MENU : Sfx.MUSIC_BATTLE);
        if (vs != null) netPlay();
        if (vs != null && !vs.host) {
            world.clientUpdate(dt);
        } else if (state == PLAY || state == END || (vs != null && state == PAUSE)) {
            acc += dt;
            int n = 0;
            while (acc >= STEP && n++ < 5) {
                world.update(STEP);
                if (vs != null) vs.tick(STEP);
                acc -= STEP;
            }
            if (acc > STEP) acc = 0;
        }
        for (int i = sel.size() - 1; i >= 0; i--) {
            Unit u = sel.get(i);
            if (u.alive && u.team == me) continue;
            u.selected = false;
            sel.remove(i);
            if (u.boardedInto != null && u.boardedInto.alive && u.boardedInto.team == me && !sel.contains(u.boardedInto)) {
                u.boardedInto.selected = true;
                sel.add(u.boardedInto);
            }
        }
        if (panel != null && panel.team != me) panel = null;
        if (vs != null && netLost && state != END) { state = END; panel = null; }
        if (world.winner >= 0 && (state == PLAY || state == PAUSE)) {
            endT += dt;
            if (!sentEnd) {
                sentEnd = true;
                boolean win = world.winner == me;
                play(win ? Sfx.WIN : Sfx.LOSE, 1f);
                brix(win ? "Vitória! O forte inimigo caiu." : "Perdemos a base... retirada!");
                if (win && vs == null) store.put("lvl" + levelIdx, Math.max(store.get("lvl" + levelIdx, 0), difficulty + 1));
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
            case LEVELS: state = versusPick ? NETMENU : TITLE; return true;
            case NETMENU: state = TITLE; return true;
            case HOSTING: case JOINING: closeNet(); state = NETMENU; return true;
            case KEYPAD: closeNet(); state = JOINING; net = new Net(); net.discover(); return true;
            case END:
                if (vs != null) { closeNet(); state = NETMENU; } else state = LEVELS;
                world = null; return true;
            default: return false;
        }
    }

    public void pause() {
        if (state == PLAY && vs == null) state = PAUSE;
        p1 = p2 = -1;
        panning = boxing = dragging = miniDrag = false;
        setMusic(Sfx.MUSIC_NONE);
        music = -2;
    }

    // ---------------------------------------------------------------- mensagens e sons

    private void say(String text, boolean good, boolean important) {
        Msg m = new Msg();
        m.text = text;
        m.color = good ? ACCENT_L : 0xFFFF8070;
        m.t = important ? 6 : 4;
        addMsg(m);
    }

    private void info(String text) {
        Msg m = new Msg();
        m.text = text;
        m.color = TXT;
        m.t = 4;
        addMsg(m);
    }

    @Override public void onEvent(int kind, int team, int other, String a, String b) {
        switch (kind) {
            case World.EV_READY: if (team == me) say(a + " pronto · " + b, true, false); break;
            case World.EV_BOARD: if (team == me) say(a + " recuperado!", true, false); break;
            case World.EV_FORT: if (team == me) say("O forte está a ser atacado!", false, true); break;
            case World.EV_CAPTURE:
                if (team == me) say("Setor capturado · " + a, true, true);
                else if (other == me) say("Perdemos o setor " + a + "!", false, true);
                else say("O inimigo tomou " + a, false, false);
                break;
            case World.EV_TURRET:
                if (team == me) say("Perdemos o canhão de " + a, false, false);
                else if (team == them()) say("Canhão inimigo destruído · " + a, true, false);
                break;
        }
    }

    private void brix(String text) {
        Msg m = new Msg();
        m.text = text;
        m.color = TXT;
        m.t = 7;
        m.brix = true;
        addMsg(m);
    }

    private void addMsg(Msg m) {
        msgs.add(m);
        while (msgs.size() > 3) msgs.remove(0);
    }

    @Override public void onSound(int id, float x, float y, int team) {
        if (state != PLAY || world == null) return;
        if (team != 0 && team != me) return;
        float vw = sw / zoom, vh = sh / zoom;
        float m = 4 * T;
        boolean ui = id == Sfx.READY || id == Sfx.CAPTURE || id == Sfx.ALERT || id == Sfx.BOARD;
        boolean inView = x >= camX && x <= camX + vw && y >= camY && y <= camY + vh;
        boolean near = x >= camX - m && x <= camX + vw + m && y >= camY - m && y <= camY + vh + m;
        if (!ui && !near) return;
        float gap = id == Sfx.MG || id == Sfx.SHOT ? 0.07f : id == Sfx.EXPLODE ? 0.09f : 0.04f;
        if (soundT[id] > 0) return;
        soundT[id] = gap;
        play(id, ui || inView ? 0.9f : 0.35f);
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
        float top = 16 / zoom;
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
            pressed = -1;
            for (int i = btns.size() - 1; i >= 0; i--) if (btns.get(i).hit(x, y)) { pressed = btns.get(i).id; break; }
            if (state == PLAY && pressed < 0 && inMinimap(x, y)) { miniDrag = true; miniJump(x, y); }
        } else if (p2 == -1 && state == PLAY) {
            p2 = id; p2x = x; p2y = y;
            panning = true;
            boxing = false;
            miniDrag = false;
            pressed = -1;
            pinchD = Math.max(4, World.dist(p1x, p1y, p2x, p2y));
            pinchZoomIdx = zoomIdx;
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
            float d = Math.max(4, World.dist(p1x, p1y, p2x, p2y));
            float want = ZOOMS[pinchZoomIdx] * d / pinchD;
            int best = 0;
            for (int i = 1; i < ZOOMS.length; i++)
                if (Math.abs(Math.log(ZOOMS[i] / want)) < Math.abs(Math.log(ZOOMS[best] / want))) best = i;
            zoomIdx = best;
            zoom = ZOOMS[best];
            float mx = (p1x + p2x) / 2, my = (p1y + p2y) / 2;
            camX = pinchWx - mx / zoom;
            camY = pinchWy - my / zoom;
            clampCam();
            return;
        }
        if (id != p1 || panning) return;
        if (miniDrag) { miniJump(x, y); return; }
        if (!dragging && World.dist(downX, downY, x, y) > 5) {
            dragging = true;
            pressed = -1;
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
            pressed = -1;
        }
        if (p1 == -1 && p2 == -1) panning = false;
    }

    public void pointerCancel() {
        p1 = p2 = -1;
        panning = boxing = dragging = miniDrag = false;
        pressed = -1;
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
        float tol = 9 / zoom;
        Unit u = null;
        float bd = Float.MAX_VALUE;
        for (Unit o : world.units) {
            if (!o.alive) continue;
            float d = World.dist(wx, wy, o.x, o.y) - o.radius;
            if (d < tol && d < bd) { bd = d; u = o; }
        }
        Building b = null;
        for (Building o : world.buildings) if (o.contains(wx, wy, 4)) b = o;

        if (u != null && u.team == me) {
            if (clock - lastTapT < 0.4f && lastTapType == u.type) {
                clearSel();
                float vw = sw / zoom, vh = sh / zoom;
                for (Unit o : world.units)
                    if (o.alive && o.team == me && o.type == u.type
                        && o.x >= camX && o.x <= camX + vw && o.y >= camY && o.y <= camY + vh) addSel(o);
            } else {
                clearSel();
                addSel(u);
            }
            lastTapT = clock;
            lastTapType = u.type;
            play(Sfx.CLICK, 0.6f);
            return;
        }
        lastTapType = null;
        if (u != null && u.team == them() && !sel.isEmpty()) {
            doAttack(sel, u);
            mark(u.x, u.y, BAD);
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
                if (!rest.isEmpty()) doMove(rest, u.x, u.y + T * 1.5f);
                doBoard(pilot, u);
                mark(u.x, u.y, GOOD);
                return;
            }
        }
        if (b != null) {
            if (b.isFactory() && b.team == me) {
                panel = b;
                play(Sfx.CLICK, 0.6f);
                return;
            }
            if (b.attackable() && b.team == them() && !sel.isEmpty()) {
                doAttack(sel, b);
                mark(b.x, b.y, BAD);
                return;
            }
            if (b.isFactory() && b.sector != null) {
                if (sel.isEmpty()) {
                    info("Captura a bandeira de " + b.sector.name + " para ficares com a fábrica.");
                } else {
                    doMove(sel, b.sector.fx, b.sector.fy);
                    mark(b.sector.fx, b.sector.fy, GOOD);
                }
                return;
            }
        }
        if (!sel.isEmpty()) {
            doMove(sel, wx, wy);
            mark(wx, wy, GOOD);
        }
    }

    // ---------------------------------------------------------------- rede

    void joinAddress(String ip) {
        closeNet();
        ipText = ip;
        net = new Net();
        net.connect(ip);
        connecting = true;
        if (state != KEYPAD) state = JOINING;
    }

    void hostGame(int lvl) {
        closeNet();
        hostLevel = lvl;
        net = new Net();
        net.host(Level.ALL[lvl].name);
        state = HOSTING;
    }

    /** Sala de espera: troca de apresentações e início da partida. */
    private void lobby() {
        if (net == null) return;
        if (net.connected && !helloSent && state != HOSTING) { net.send(Versus.hello()); helloSent = true; }
        byte[] m;
        while (net != null && (m = net.poll()) != null) {
            try {
                java.io.DataInputStream in = Versus.reader(m);
                int type = in.readByte();
                if (state == HOSTING && type == Versus.M_HELLO) {
                    int proto = in.readInt();
                    String ver = in.readUTF();
                    if (proto != Net.PROTO) {
                        net.send(Versus.simple(Versus.M_REJECT, "Versões diferentes do jogo: " + VERSION + " e " + ver + "."));
                        net.error = "O outro telemóvel tem a versão " + ver + ". Instalem a mesma versão.";
                        continue;
                    }
                    long seed = System.nanoTime();
                    net.send(Versus.start(hostLevel, seed));
                    vs = new Versus(net, true);
                    me = World.PLAYER;
                    begin(hostLevel, seed);
                    return;
                } else if (type == Versus.M_START && state != HOSTING) {
                    int lvl = in.readInt();
                    long seed = in.readLong();
                    vs = new Versus(net, false);
                    me = World.ENEMY;
                    begin(lvl, seed);
                    return;
                } else if (type == Versus.M_REJECT) {
                    net.error = in.readUTF();
                    net.close();
                }
            } catch (Exception ignored) {
                // mensagem inválida
            }
        }
    }

    /** Durante a partida em rede: imagens de estado (convidado) ou ordens (anfitrião). */
    private void netPlay() {
        byte[] m;
        int n = 0;
        while ((m = net.poll()) != null && n++ < 60) {
            try {
                java.io.DataInputStream in = Versus.reader(m);
                int type = in.readByte();
                if (type == Versus.M_SNAP && !vs.host) vs.applySnapshot(in);
                else if (type == Versus.M_CMD && vs.host) vs.applyCmd(in);
                else if (type == Versus.M_BYE && world.winner < 0) netLost = true;
            } catch (Exception ignored) {
                // mensagem inválida
            }
        }
        if (net.closed && world.winner < 0) netLost = true;
    }

    private void doMove(java.util.List<Unit> list, float x, float y) {
        if (vs != null && !vs.host) vs.move(list, x, y);
        else world.orderMove(list, x, y, false);
    }

    private void doAttack(java.util.List<Unit> list, Entity t) {
        if (vs != null && !vs.host) vs.attack(list, t);
        else world.orderAttack(list, t);
    }

    private void doBoard(Unit u, Unit v) {
        if (vs != null && !vs.host) vs.board(u, v);
        else world.orderBoard(u, v);
    }

    private void mark(float x, float y, int color) {
        markX = x; markY = y; markT = clock; markColor = color;
        play(Sfx.CLICK, 0.5f);
    }

    private void boxSelect(float ax, float ay, float bx, float by) {
        float x0 = camX + Math.min(ax, bx) / zoom, x1 = camX + Math.max(ax, bx) / zoom;
        float y0 = camY + Math.min(ay, by) / zoom, y1 = camY + Math.max(ay, by) / zoom;
        clearSel();
        for (Unit u : world.units)
            if (u.alive && u.team == me && u.x + u.radius >= x0 && u.x - u.radius <= x1
                && u.y + u.radius >= y0 && u.y - u.radius <= y1) addSel(u);
        if (!sel.isEmpty()) play(Sfx.CLICK, 0.6f);
    }

    private void addSel(Unit u) { if (!u.selected) { u.selected = true; sel.add(u); } }

    private void clearSel() {
        for (Unit u : sel) u.selected = false;
        sel.clear();
    }

    private void button(int id) {
        play(Sfx.CLICK, 0.7f);
        if (id >= B_LEVEL && id < B_LEVEL + Level.ALL.length && versusPick) { hostGame(id - B_LEVEL); return; }
        if (id >= B_LEVEL && id < B_LEVEL + Level.ALL.length) { start(id - B_LEVEL); return; }
        if (id >= B_DIFF && id < B_DIFF + 3) { difficulty = id - B_DIFF; store.put("dif", difficulty); return; }
        if (id >= B_FOUND && id < B_FOUND + 8 && net != null) {
            java.util.List<String[]> f = net.found();
            if (id - B_FOUND < f.size()) { ipText = f.get(id - B_FOUND)[0]; net.connect(ipText); connecting = true; }
            return;
        }
        if (id >= B_KEY && id < B_KEY + 12) {
            int k = id - B_KEY;
            if (k < 10 && ipText.length() < 15) ipText += k;
            else if (k == 10 && ipText.length() < 15) ipText += ".";
            else if (k == 11 && ipText.length() > 0) ipText = ipText.substring(0, ipText.length() - 1);
            return;
        }
        switch (id) {
            case B_CAMPAIGN: case B_LEVELS: closeNet(); versusPick = false; state = LEVELS; world = null; break;
            case B_VERSUS: closeNet(); state = NETMENU; world = null; break;
            case B_CREATE: versusPick = true; state = LEVELS; break;
            case B_JOINMENU: closeNet(); net = new Net(); net.discover(); state = JOINING; break;
            case B_KEYPAD:
                if (ipText.isEmpty()) {
                    java.util.List<String> mine = Net.localAddresses();
                    if (!mine.isEmpty()) ipText = mine.get(0).substring(0, mine.get(0).lastIndexOf('.') + 1);
                }
                state = KEYPAD; break;
            case B_CONNECT:
                if (ipText.split("\\.").length == 4) joinAddress(ipText);
                break;
            case B_CANCEL: closeNet(); state = NETMENU; break;
            case B_QUIT: closeNet(); state = NETMENU; world = null; break;
            case B_HELP: state = HELP; break;
            case B_BACK: state = TITLE; break;
            case B_RESUME: state = PLAY; break;
            case B_RESTART: if (vs == null) start(levelIdx); break;
            case B_NEXT: start(levelIdx + 1); break;
            case B_MENU: closeNet(); state = TITLE; world = null; break;
            case B_PAUSE: state = PAUSE; break;
            case B_SOUND: store.put("som", soundOn() ? 0 : 1); break;
            case B_MUSIC: store.put("musica", musicOn() ? 0 : 1); music = -2; break;
            case B_CRT: store.put("crt", crt() ? 0 : 1); break;
            case B_ALL:
                clearSel();
                for (Unit u : world.units) if (u.alive && u.team == me) addSel(u);
                break;
            case B_NONE: clearSel(); break;
            case B_CLOSE: panel = null; break;
            default:
                if (id >= B_MAKE && panel != null) {
                    UnitType t = panel.buildable()[id - B_MAKE];
                    if (panel.making != t) {
                        if (vs != null && !vs.host) vs.produce(panel, t);
                        panel.making = t;
                        panel.progress = 0;
                    }
                }
        }
    }

    // ---------------------------------------------------------------- layout

    static final int BAR = 15;

    private float[] miniRect() {
        float mw = Math.min(sw * 0.24f, world.W * 2), mh = mw * world.H / world.W;
        if (mh > sh * 0.36f) { mh = sh * 0.36f; mw = mh * world.W / world.H; }
        mw = Math.round(mw); mh = Math.round(mh);
        return new float[]{sw - mw - 5, sh - mh - 5, mw, mh};
    }

    private boolean inMinimap(float x, float y) {
        float[] r = miniRect();
        return x >= r[0] - 3 && x <= r[0] + r[2] + 3 && y >= r[1] - 3 && y <= r[1] + r[3] + 3;
    }

    private void miniJump(float x, float y) {
        float[] r = miniRect();
        float fx = Math.max(0, Math.min(1, (x - r[0]) / r[2])), fy = Math.max(0, Math.min(1, (y - r[1]) / r[3]));
        centerOn(fx * world.W * T, fy * world.H * T);
    }

    private boolean overHud(float x, float y) {
        if (y < BAR) return true;
        if (inMinimap(x, y)) return true;
        for (Btn b : btns) if (b.hit(x, y)) return true;
        return false;
    }

    private float[] panelRect() {
        float w = Math.min(sw - 12, 300), h = 86;
        return new float[]{Math.round((sw - w) / 2), Math.round(sh - h - 4), Math.round(w), h};
    }

    // ---------------------------------------------------------------- primitivas da interface

    static void text(Gfx g, String s, float x, float y, int color, int scale, int align) {
        float w = PixelFont.width(s, scale);
        float ox = align == Gfx.CENTER ? -w / 2 : align == Gfx.RIGHT ? -w : 0;
        PixelFont.drawTop(g, s, Math.round(x + ox), Math.round(y), scale, color, SHADOW);
    }

    /** Texto centrado verticalmente em cy. */
    static void textC(Gfx g, String s, float x, float cy, int color, int scale, int align) {
        text(g, s, x, Math.round(cy - PixelFont.CAP * scale / 2f), color, scale, align);
    }

    /** Painel metálico em relevo (luz em cima à esquerda). */
    static void bevel(Gfx g, float x, float y, float w, float h, int base, boolean sunken) {
        x = Math.round(x); y = Math.round(y); w = Math.round(w); h = Math.round(h);
        g.color(SHADOW);
        g.fillRect(x - 1, y - 1, w + 2, h + 2);
        g.color(base);
        g.fillRect(x, y, w, h);
        int lt = Draw.shade(base, 1.45f), dk = Draw.shade(base, 0.5f);
        g.color(sunken ? dk : lt);
        g.fillRect(x, y, w, 1);
        g.fillRect(x, y, 1, h);
        g.color(sunken ? lt : dk);
        g.fillRect(x, y + h - 1, w, 1);
        g.fillRect(x + w - 1, y, 1, h);
    }

    private static void rivets(Gfx g, float x, float y, float w, float h) {
        g.color(METAL_L);
        float[][] p = {{x + 2, y + 2}, {x + w - 3, y + 2}, {x + 2, y + h - 3}, {x + w - 3, y + h - 3}};
        for (float[] q : p) { g.fillRect(Math.round(q[0]), Math.round(q[1]), 1, 1); }
    }

    private void drawBtn(Gfx g, Btn b) {
        boolean down = pressed == b.id;
        int base = b.primary ? 0xFFB86A14 : METAL;
        bevel(g, b.x, b.y, b.w, b.h, base, down);
        int c = b.primary ? 0xFFFFF0C0 : TXT;
        textC(g, b.label, b.x + b.w / 2 + (down ? 1 : 0), b.y + b.h / 2 + (down ? 1 : 0), c, 1, Gfx.CENTER);
    }

    private Btn btn(float x, float y, float w, float h, String label, int id, boolean primary, Gfx g) {
        Btn b = new Btn(x, y, w, h, label, id, primary);
        btns.add(b);
        drawBtn(g, b);
        return b;
    }

    private void dim(Gfx g, float a) {
        g.color(Draw.alpha(0xFF0A0C10, a));
        g.fillRect(0, 0, sw, sh);
        // trama de pontos, como os ecrãs de pausa antigos
        g.color(Draw.alpha(0xFF000000, a * 0.5f));
        for (int y = 0; y < sh; y += 2) g.fillRect(0, y, sw, 1);
    }

    private static void flagIcon(Gfx g, float x, float y, int c) {
        g.color(SHADOW); g.fillRect(x + 1, y, 1, 7);
        g.color(0xFFD0D0D0); g.fillRect(x, y, 1, 7);
        g.color(c); g.fillRect(x + 1, y, 4, 3);
    }

    private static void unitIcon(Gfx g, float x, float y, int c) {
        g.color(SHADOW); g.fillRect(x + 1, y + 1, 5, 6);
        g.color(c); g.fillRect(x, y, 5, 6);
        g.color(0xFF101010); g.fillRect(x + 1, y + 2, 3, 1);
        g.color(0xFFFFFFFF); g.fillRect(x + 3, y + 2, 1, 1);
    }

    // ---------------------------------------------------------------- desenho

    public void render(Gfx g) {
        btns.clear();
        if (state == TITLE || state == HELP || state == LEVELS || state == NETMENU || state == HOSTING
            || state == JOINING || state == KEYPAD) {
            renderWorld(g, demo, dCamX, dCamY, 1);
            if (state == TITLE) renderTitle(g);
            else if (state == HELP) renderHelp(g);
            else if (state == LEVELS) renderLevels(g);
            else if (state == NETMENU) renderNetMenu(g);
            else if (state == HOSTING) renderHosting(g);
            else if (state == JOINING) renderJoining(g);
            else renderKeypad(g);
            return;
        }
        if (world == null) return;
        renderWorld(g, world, camX, camY, zoom);
        g.save();
        float cx = Math.round(camX * zoom) / zoom, cy = Math.round(camY * zoom) / zoom;
        g.scale(zoom);
        g.translate(-cx, -cy);
        float since = clock - markT;
        if (since < 0.6f) {
            float p = since / 0.6f;
            g.color(markColor);
            float r = 12 * (1 - p * 0.6f);
            g.fillRect(markX - r, markY - 1, 4, 2);
            g.fillRect(markX + r - 4, markY - 1, 4, 2);
            g.fillRect(markX - 1, markY - r, 2, 4);
            g.fillRect(markX - 1, markY + r - 4, 2, 4);
        }
        g.restore();
        if (boxing && p1 != -1) {
            float x0 = Math.round(Math.min(downX, p1x)), y0 = Math.round(Math.min(downY, p1y));
            float bw = Math.round(Math.abs(p1x - downX)), bh = Math.round(Math.abs(p1y - downY));
            g.color(GOOD);
            for (float x = x0; x < x0 + bw; x += 4) { g.fillRect(x, y0, 2, 1); g.fillRect(x, y0 + bh, 2, 1); }
            for (float y = y0; y < y0 + bh; y += 4) { g.fillRect(x0, y, 1, 2); g.fillRect(x0 + bw, y, 1, 2); }
        }
        renderHud(g);
        if (state == PAUSE) renderPause(g);
        else if (state == END) renderEnd(g);
    }

    private void renderWorld(Gfx g, World w, float cx, float cy, float z) {
        g.color(0xFF101208);
        g.fillRect(0, 0, sw, sh);
        cx = Math.round(cx * z) / z;
        cy = Math.round(cy * z) / z;
        g.save();
        g.scale(z);
        g.translate(-cx, -cy);
        Draw.world(g, w, cx, cy, cx + sw / z, cy + sh / z, clock);
        g.restore();
    }

    private void renderHud(Gfx g) {
        bevel(g, 0, 0, sw, BAR, METAL, false);
        float x = 5, cy = BAR / 2f;
        for (int k = 0; k < 2; k++) {
            int team = k == 0 ? me : them();
            int c = Draw.teamColor(team);
            g.color(SHADOW); g.fillRect(x + 1, cy - 3, 7, 7);
            g.color(c); g.fillRect(x, cy - 4, 7, 7);
            x += 11;
            flagIcon(g, x, cy - 3.5f, c);
            x += 8;
            String s = String.valueOf(world.ownedSectors(team));
            textC(g, s, x, cy, TXT, 1, Gfx.LEFT);
            x += PixelFont.width(s) + 6;
            unitIcon(g, x, cy - 3.5f, c);
            x += 8;
            s = String.valueOf(world.unitCount(team));
            textC(g, s, x, cy, TXT, 1, Gfx.LEFT);
            x += PixelFont.width(s) + 14;
        }
        int secs = (int) world.time;
        textC(g, String.format("%d:%02d", secs / 60, secs % 60), x, cy, ACCENT_L, 1, Gfx.LEFT);
        Btn pause = new Btn(sw - 22, 1, 21, BAR - 2, "", B_PAUSE, false);
        btns.add(pause);
        bevel(g, pause.x, pause.y, pause.w, pause.h, pressed == B_PAUSE ? METAL_D : 0xFF666E80, pressed == B_PAUSE);
        g.color(TXT);
        g.fillRect(pause.x + 7, pause.y + 3, 2, 7);
        g.fillRect(pause.x + 12, pause.y + 3, 2, 7);
        float tf = world.territoryFactor(me) * world.prodMult[me];
        String prod = "PRODUÇÃO ×" + String.format("%.1f", tf);
        if (sw < 520) prod = "×" + String.format("%.1f", tf);
        textC(g, prod, pause.x - 6, cy, DIM, 1, Gfx.RIGHT);

        // mensagens
        float my = BAR + 4;
        if (state == PLAY) for (Msg m : msgs) {
            float tw = PixelFont.width(m.text);
            float extra = m.brix ? 16 : 0;
            float mw = tw + 10 + extra, mh = m.brix ? 16 : 12;
            if ((m.t % 0.5f) < 0.25f && m.t > 6.5f) continue; // pisca ao chegar
            float mx = Math.round((sw - mw) / 2);
            bevel(g, mx, my, mw, mh, DARK, true);
            if (m.brix) Draw.brix(g, mx + 8, my + mh / 2, 12, clock);
            textC(g, m.text, mx + 5 + extra, my + mh / 2, m.color, 1, Gfx.LEFT);
            my += mh + 3;
        }

        float[] r = miniRect();
        bevel(g, r[0] - 3, r[1] - 3, r[2] + 6, r[3] + 6, METAL, false);
        Draw.minimap(g, world, r[0], r[1], r[2], r[3], camX, camY, sw / zoom, sh / zoom);

        float bw = 48, bh = 20, by = sh - bh - 5;
        if (panel == null) {
            btn(5, by, bw, bh, "TODOS", B_ALL, false, g);
            btn(5 + bw + 4, by, bw, bh, "NENHUM", B_NONE, false, g);
            if (!sel.isEmpty()) text(g, selSummary(), 6, by - 11, GOOD, 1, Gfx.LEFT);
        }
        if (hintT > 0 && panel == null && state == PLAY) {
            String h = "ARRASTA: SELECIONAR · TOCA: MOVER · 2 DEDOS: CÂMARA E ZOOM";
            float tw = PixelFont.width(h);
            if (tw > r[0] - 20) { h = "ARRASTA: SELECIONAR · 2 DEDOS: CÂMARA"; tw = PixelFont.width(h); }
            float hx = Math.round(Math.max(tw / 2 + 6, Math.min(sw / 2, r[0] - 8 - tw / 2)));
            float hy = sh - 52;
            if ((int) (clock * 2) % 2 == 0 || hintT < 10) {
                bevel(g, hx - tw / 2 - 5, hy - 6, tw + 10, 12, DARK, true);
                textC(g, h, hx, hy, ACCENT_L, 1, Gfx.CENTER);
            }
        }
        if (panel != null) renderPanel(g);
        if (vs != null && !vs.host && net != null && net.silence() > 3000 && world.winner < 0 && !netLost) {
            String t = "À ESPERA DO OUTRO TELEMÓVEL...";
            float tw = PixelFont.width(t);
            bevel(g, sw / 2 - tw / 2 - 8, sh / 2 - 9, tw + 16, 18, DARK, false);
            textC(g, t, sw / 2, sh / 2, ACCENT_L, 1, Gfx.CENTER);
        }
    }

    private String selSummary() {
        int[] n = new int[5];
        for (Unit u : sel) if (u.type.id < 5) n[u.type.id]++;
        StringBuilder sb = new StringBuilder();
        for (UnitType t : UnitType.ALL) {
            if (n[t.id] == 0) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(n[t.id]).append(' ').append(t.name.toUpperCase());
        }
        return sb.toString();
    }

    private void renderPanel(Gfx g) {
        float[] r = panelRect();
        float x = r[0], y = r[1], w = r[2], h = r[3];
        bevel(g, x, y, w, h, METAL, false);
        rivets(g, x, y, w, h);
        text(g, (panel.title() + " · " + panel.sector.name).toUpperCase(), x + 6, y + 5, ACCENT_L, 1, Gfx.LEFT);
        Btn close = new Btn(x + w - 17, y + 3, 14, 12, "", B_CLOSE, false);
        btns.add(close);
        bevel(g, close.x, close.y, close.w, close.h, pressed == B_CLOSE ? METAL_D : 0xFF8A3030, pressed == B_CLOSE);
        g.color(TXT);
        for (int i = 0; i < 6; i++) {
            g.fillRect(close.x + 4 + i, close.y + 3 + i, 1, 1);
            g.fillRect(close.x + 9 - i, close.y + 3 + i, 1, 1);
        }
        UnitType cur = panel.making;
        float py = y + 17;
        bevel(g, x + 6, py, w - 12, 6, DARK, true);
        g.color(ACCENT);
        g.fillRect(x + 7, py + 1, Math.round((w - 14) * Math.min(1, panel.progress)), 4);
        if (cur != null) {
            float left = (1 - panel.progress) * world.buildSeconds(cur, me);
            boolean cap = world.unitCount(me) >= World.UNIT_CAP;
            text(g, cap ? "LIMITE DE " + World.UNIT_CAP + " UNIDADES" : "A FABRICAR: " + cur.name.toUpperCase() + " · " + (int) Math.ceil(left) + " S",
                x + 6, py + 9, TXT, 1, Gfx.LEFT);
        }
        UnitType[] types = panel.buildable();
        float gap = 4;
        float bw = Math.round((w - 12 - gap * (types.length - 1)) / types.length);
        float by = py + 20, bh = y + h - by - 5;
        for (int i = 0; i < types.length; i++) {
            UnitType t = types[i];
            Btn b = new Btn(x + 6 + i * (bw + gap), by, bw, bh, t.name, B_MAKE + i, false);
            btns.add(b);
            boolean on = t == cur;
            bevel(g, b.x, b.y, b.w, b.h, on ? 0xFF6A4A1C : (pressed == b.id ? METAL_D : 0xFF3A404C), pressed == b.id || on);
            g.save();
            g.translate(b.x + 13, b.y + b.h / 2);
            float s = 18 / (t.radius * 2.4f);
            g.scale(s);
            Draw.unitShape(g, t, me, (float) -Math.PI / 2, (float) -Math.PI / 2, on ? clock * 30 : 0, false);
            g.restore();
            float tx = b.x + 26;
            text(g, t.name.toUpperCase(), tx, b.y + 5, on ? ACCENT_L : TXT, 1, Gfx.LEFT);
            text(g, t.desc, tx, b.y + 15, DIM, 1, Gfx.LEFT);
            text(g, (int) Math.ceil(world.buildSeconds(t, me)) + " s", tx, b.y + 25, DIM, 1, Gfx.LEFT);
        }
    }

    private void renderTitle(Gfx g) {
        dim(g, 0.45f);
        float cx = sw / 2;
        int sc = sw > 560 ? 5 : 4;
        String title = "SETOR ZERO";
        float tw = PixelFont.width(title, sc);
        float tx = Math.round(cx - tw / 2), ty = Math.round(sh * 0.12f);
        // contorno
        g.color(SHADOW);
        for (int dy = -1; dy <= 2; dy++) for (int dx = -1; dx <= 2; dx++) if (dx != 0 || dy != 0) raw(g, title, tx + dx * 1, ty + dy * 1, sc, SHADOW);
        raw(g, title, tx, ty, sc, 0xFFC05010);
        g.save();
        g.clip(tx - 2, ty, tw + 4, PixelFont.CAP * sc * 0.62f);
        raw(g, title, tx, ty, sc, ACCENT);
        g.restore();
        g.save();
        g.clip(tx - 2, ty, tw + 4, sc * 2);
        raw(g, title, tx, ty, sc, ACCENT_L);
        g.restore();
        text(g, "CAPTURA BANDEIRAS · DOMINA FÁBRICAS · DERRUBA O FORTE", cx, ty + PixelFont.CAP * sc + 8, TXT, 1, Gfx.CENTER);

        float bw = 130, bh = 21, by = Math.round(sh * 0.40f);
        btn(cx - bw / 2, by, bw, bh, "CAMPANHA", B_CAMPAIGN, true, g);
        btn(cx - bw / 2, by + bh + 5, bw, bh, "2 JOGADORES", B_VERSUS, true, g);
        btn(cx - bw / 2, by + 2 * (bh + 5), bw, bh, "COMO JOGAR", B_HELP, false, g);

        float ow = 74, oh = 16, oy = sh - oh - 6;
        float ox = sw - (ow + 4) * 3 - 2;
        btn(ox, oy, ow, oh, soundOn() ? "SOM: SIM" : "SOM: NÃO", B_SOUND, false, g);
        btn(ox + ow + 4, oy, ow, oh, musicOn() ? "MÚSICA: SIM" : "MÚSICA: NÃO", B_MUSIC, false, g);
        btn(ox + 2 * (ow + 4), oy, ow, oh, crt() ? "CRT: SIM" : "CRT: NÃO", B_CRT, false, g);

        float bs = Math.min(40, sh * 0.16f);
        float bx = 8 + bs * 0.6f, byy = sh - bs * 0.6f - 8;
        bevel(g, bx - bs * 0.62f, byy - bs * 0.62f, bs * 1.24f, bs * 1.24f, DARK, true);
        Draw.brix(g, bx, byy, bs, clock);
        float qx = bx + bs * 0.75f;
        if (qx + 160 < ox) {
            text(g, "COMANDANTE BRIX", qx, byy - 10, ACCENT_L, 1, Gfx.LEFT);
            text(g, "\"Mais território, mais fábricas!\"", qx, byy + 2, DIM, 1, Gfx.LEFT);
        }
        text(g, "V" + VERSION, sw - 4, 4, DIM, 1, Gfx.RIGHT);
    }

    private static void raw(Gfx g, String s, float x, float y, int sc, int color) {
        PixelFont.drawTop(g, s, x, y, sc, color, 0);
    }

    private void renderLevels(Gfx g) {
        dim(g, 0.7f);
        text(g, versusPick ? "2 JOGADORES: ESCOLHE O MAPA" : "CAMPANHA", 8, 7, ACCENT, 2, Gfx.LEFT);
        float dw = 50, dh = 16;
        for (int i = 0; i < 3 && !versusPick; i++) {
            Btn b = new Btn(sw - (dw + 3) * (3 - i) - 4, 5, dw, dh, DIFF[i], B_DIFF + i, i == difficulty);
            btns.add(b);
            drawBtn(g, b);
        }
        int n = Level.ALL.length;
        int cols = 4, rows = (n + cols - 1) / cols;
        float top = 28, bottom = sh - 28;
        float gap = 5;
        float cw = Math.round((sw - 16 - gap * (cols - 1)) / cols);
        float ch = Math.round((bottom - top - gap * (rows - 1)) / rows);
        for (int i = 0; i < n; i++) {
            final Level L = Level.ALL[i];
            float x = 8 + (i % cols) * (cw + gap), y = top + (i / cols) * (ch + gap);
            Btn b = new Btn(x, y, cw, ch, L.name, B_LEVEL + i, false);
            btns.add(b);
            bevel(g, x, y, cw, ch, pressed == b.id ? METAL_D : METAL, pressed == b.id);
            float ph = ch - 14, pw = Math.min(cw - 6, ph * L.W / L.H);
            ph = pw * L.H / L.W;
            float px = Math.round(x + (cw - pw) / 2), py = y + 3;
            g.image("prev-" + i, L.W * 2, L.H * 2, new Gfx.PixelSource() {
                public int[] pixels() { return preview(L); }
            }, px, py, Math.round(pw), Math.round(ph));
            g.color(SHADOW);
            g.strokeRect(px - 0.5f, py - 0.5f, Math.round(pw) + 1, Math.round(ph) + 1, 1);
            String name = (i + 1) + ". " + L.name.toUpperCase();
            while (PixelFont.width(name) > cw - 6 && name.length() > 4) name = name.substring(0, name.length() - 2) + ".";
            text(g, name, x + 3, y + ch - 10, TXT, 1, Gfx.LEFT);
            int done = versusPick ? 0 : store.get("lvl" + i, 0);
            for (int k = 0; k < done; k++) {
                g.color(SHADOW); star(g, x + cw - 9 - k * 8 + 1, py + 3);
                g.color(ACCENT_L); star(g, x + cw - 9 - k * 8, py + 2);
            }
        }
        btn(8, sh - 22, 70, 17, "VOLTAR", versusPick ? B_VERSUS : B_BACK, false, g);
        text(g, versusPick ? "DEPOIS DE ESCOLHERES, O OUTRO TELEMÓVEL ENTRA NO JOGO"
            : "TOCA NUM NÍVEL PARA JOGAR · ESTRELAS: DIFICULDADE VENCIDA", sw - 8, sh - 17, DIM, 1, Gfx.RIGHT);
    }

    private static void star(Gfx g, float x, float y) {
        g.fillRect(x + 2, y, 1, 1);
        g.fillRect(x + 1, y + 1, 3, 1);
        g.fillRect(x, y + 2, 5, 1);
        g.fillRect(x + 1, y + 3, 3, 1);
        g.fillRect(x, y + 4, 2, 1);
        g.fillRect(x + 3, y + 4, 2, 1);
    }

    /** Pequena pré-visualização do mapa (2 píxeis por quadrícula). */
    static int[] preview(Level L) {
        World w = new World(1, 1, L);
        int[] pal = TerrainArt.PAL[L.theme];
        int PW = w.W * 2, PH = w.H * 2;
        int[] out = new int[PW * PH];
        for (int y = 0; y < PH; y++)
            for (int x = 0; x < PW; x++) {
                int t = w.tileAt(x / 2, y / 2);
                int c;
                switch (t) {
                    case World.ROCK: c = pal[TerrainArt.R0]; break;
                    case World.WATER: c = pal[TerrainArt.W1]; break;
                    case World.TREE: c = pal[TerrainArt.T1]; break;
                    case World.DIRT: c = pal[TerrainArt.D0]; break;
                    case World.BRIDGE: c = pal[TerrainArt.B0]; break;
                    default: c = pal[((x + y) & 1) == 0 ? TerrainArt.G0 : TerrainArt.G1];
                }
                out[y * PW + x] = c;
            }
        for (Building b : w.buildings)
            for (int y = b.ty * 2; y < (b.ty + b.th) * 2; y++)
                for (int x = b.tx * 2; x < (b.tx + b.tw) * 2; x++)
                    out[y * PW + x] = b.team == World.NEUTRAL ? 0xFFE0E0E0 : Draw.teamColor(b.team);
        for (Sector s : w.sectors) {
            int fx = (int) (s.fx / T * 2), fy = (int) (s.fy / T * 2);
            for (int y = fy - 1; y <= fy; y++) for (int x = fx - 1; x <= fx; x++) out[y * PW + x] = 0xFFFFFFFF;
        }
        return out;
    }

    private float[] box(Gfx g, float w, float h, String title) {
        float x = Math.round((sw - w) / 2), y = Math.round((sh - h) / 2);
        bevel(g, x, y, w, h, METAL, false);
        rivets(g, x, y, w, h);
        text(g, title, sw / 2, y + 7, ACCENT, 2, Gfx.CENTER);
        return new float[]{x, y, w, h};
    }

    private float lines(Gfx g, String[] ls, float y, int color) {
        for (String l : ls) { text(g, l, sw / 2, y, color, 1, Gfx.CENTER); y += 11; }
        return y;
    }

    private void netError(Gfx g, float y) {
        if (net != null && net.error != null) text(g, net.error, sw / 2, y, BAD, 1, Gfx.CENTER);
    }

    private void renderNetMenu(Gfx g) {
        dim(g, 0.6f);
        float[] r = box(g, Math.min(sw - 16, 380), 150, "2 JOGADORES");
        float y = lines(g, new String[]{
            "Dois telemóveis na mesma rede Wi-Fi, ou um ligado",
            "ao ponto de acesso (hotspot) do outro.",
            "Quem cria o jogo escolhe o mapa e joga com o laranja;",
            "quem entra joga com o ciano.",
        }, r[1] + 27, TXT);
        float bw = 130, bh = 20;
        btn(sw / 2 - bw - 3, y + 6, bw, bh, "CRIAR JOGO", B_CREATE, true, g);
        btn(sw / 2 + 3, y + 6, bw, bh, "ENTRAR NUM JOGO", B_JOINMENU, true, g);
        btn(sw / 2 - 40, y + 32, 80, 17, "VOLTAR", B_BACK, false, g);
    }

    private void renderHosting(Gfx g) {
        dim(g, 0.6f);
        float[] r = box(g, Math.min(sw - 16, 360), 140, "À ESPERA...");
        if (clock - ipsT > 1.5f || ips == null) { ips = Net.localAddresses(); ipsT = clock; }
        String dots = "...".substring(0, 1 + (int) (clock * 2) % 3);
        float y = lines(g, new String[]{
            "Mapa: " + Level.ALL[hostLevel].name,
            "No outro telemóvel: 2 JOGADORES > ENTRAR NUM JOGO",
            "À espera do outro jogador" + dots,
        }, r[1] + 27, TXT);
        text(g, ips.isEmpty() ? "SEM REDE: LIGA O WI-FI OU O HOTSPOT" : "O TEU ENDEREÇO: " + join(ips), sw / 2, y + 2, ACCENT_L, 1, Gfx.CENTER);
        netError(g, y + 14);
        btn(sw / 2 - 45, r[1] + r[3] - 26, 90, 18, "CANCELAR", B_CANCEL, false, g);
    }

    private static String join(java.util.List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) { if (sb.length() > 0) sb.append(" / "); sb.append(s); }
        return sb.toString();
    }

    private void renderJoining(Gfx g) {
        dim(g, 0.6f);
        float[] r = box(g, Math.min(sw - 16, 360), Math.min(sh - 16, 190), "ENTRAR NUM JOGO");
        java.util.List<String[]> f = net == null ? new ArrayList<String[]>() : net.found();
        float y = r[1] + 27;
        if (net != null && net.connected) {
            text(g, "LIGADO! A COMEÇAR...", sw / 2, y + 20, GOOD, 1, Gfx.CENTER);
        } else if (connecting && net != null && net.error == null) {
            text(g, "A LIGAR A " + ipText + "...", sw / 2, y + 20, DIM, 1, Gfx.CENTER);
        } else if (f.isEmpty()) {
            String dots = "...".substring(0, 1 + (int) (clock * 2) % 3);
            y = lines(g, new String[]{"À procura de jogos na rede" + dots, "(o outro telemóvel tem de ter o jogo criado)"}, y, TXT);
        } else {
            text(g, "JOGOS ENCONTRADOS:", sw / 2, y, DIM, 1, Gfx.CENTER);
            y += 12;
            for (int i = 0; i < f.size() && i < 4; i++) {
                btn(sw / 2 - 120, y, 240, 18, f.get(i)[1].toUpperCase() + " · " + f.get(i)[0], B_FOUND + i, true, g);
                y += 22;
            }
        }
        netError(g, r[1] + r[3] - 40);
        btn(sw / 2 - 128, r[1] + r[3] - 26, 124, 18, "ESCREVER ENDEREÇO", B_KEYPAD, false, g);
        btn(sw / 2 + 4, r[1] + r[3] - 26, 124, 18, "VOLTAR", B_VERSUS, false, g);
    }

    private void renderKeypad(Gfx g) {
        dim(g, 0.6f);
        float[] r = box(g, Math.min(sw - 16, 300), Math.min(sh - 10, 214), "ENDEREÇO");
        float y = r[1] + 26;
        bevel(g, sw / 2 - 90, y, 180, 16, DARK, true);
        String shown = ipText + ((int) (clock * 2) % 2 == 0 ? "_" : " ");
        textC(g, shown, sw / 2, y + 8, ACCENT_L, 1, Gfx.CENTER);
        y += 22;
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "<"};
        int[] ids = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0, 11};
        float kw = 40, kh = 22, gap = 4;
        float kx = sw / 2 - (kw * 3 + gap * 2) / 2;
        for (int i = 0; i < 12; i++) {
            btn(kx + (i % 3) * (kw + gap), y + (i / 3) * (kh + gap), kw, kh, keys[i], B_KEY + ids[i], false, g);
        }
        y += 4 * (kh + gap);
        if (net != null && net.connected) text(g, "LIGADO! A COMEÇAR...", sw / 2, y, GOOD, 1, Gfx.CENTER);
        else if (connecting && net != null && net.error == null) text(g, "A LIGAR...", sw / 2, y, DIM, 1, Gfx.CENTER);
        else netError(g, y);
        btn(sw / 2 - 92, r[1] + r[3] - 24, 88, 18, "LIGAR", B_CONNECT, true, g);
        btn(sw / 2 + 4, r[1] + r[3] - 24, 88, 18, "VOLTAR", B_JOINMENU, false, g);
    }

    private void renderHelp(Gfx g) {
        dim(g, 0.82f);
        String[] lines = {
            "#OBJETIVO",
            "Destrói o forte ciano antes que destruam o teu.",
            "#SETORES E BANDEIRAS",
            "Uma unidade na bandeira captura o setor, com fábricas e canhões.",
            "Mais setores = fábricas mais rápidas. A bandeira de uma base",
            "só pode ser tomada depois de o forte dessa base cair.",
            "#CONTROLOS",
            "Arrastar: selecionar · Tocar na unidade: selecionar (2x: tipo)",
            "Tocar no chão: mover · Tocar no inimigo: atacar",
            "Tocar num veículo abandonado: ocupar · Na fábrica: produção",
            "Dois dedos: mover a câmara e zoom · Minimapa: saltar",
            "#UNIDADES",
            "Batedor: barato · Atirador: alcance longo contra robôs",
            "Bazuca: contra veículos e edifícios · Jipe: rápido · Tanque: forte",
        };
        float lh = 11;
        float total = 0;
        for (String l : lines) total += l.startsWith("#") ? lh + 4 : lh;
        float y = Math.max(6, (sh - total - 28) / 2);
        for (String l : lines) {
            boolean head = l.startsWith("#");
            if (head) y += 4;
            text(g, head ? l.substring(1) : l, sw / 2, y, head ? ACCENT : TXT, 1, Gfx.CENTER);
            y += lh;
        }
        btn(sw / 2 - 40, Math.min(y + 6, sh - 24), 80, 18, "VOLTAR", B_BACK, true, g);
    }

    private void renderPause(Gfx g) {
        btns.clear();
        dim(g, 0.55f);
        float w = 160, h = 112, x = Math.round((sw - w) / 2), y = Math.round((sh - h) / 2);
        bevel(g, x, y, w, h, METAL, false);
        rivets(g, x, y, w, h);
        float bw = 130, bh = 18;
        if (vs != null) {
            text(g, "MENU", sw / 2, y + 7, ACCENT, 2, Gfx.CENTER);
            text(g, "O JOGO CONTINUA A CORRER", sw / 2, y + 25, DIM, 1, Gfx.CENTER);
            btn(sw / 2 - bw / 2, y + 44, bw, bh, "CONTINUAR", B_RESUME, true, g);
            btn(sw / 2 - bw / 2, y + 70, bw, bh, "SAIR DO JOGO", B_QUIT, false, g);
            return;
        }
        text(g, "PAUSA", sw / 2, y + 7, ACCENT, 2, Gfx.CENTER);
        text(g, Level.ALL[levelIdx].name.toUpperCase() + " · " + DIFF[difficulty], sw / 2, y + 25, DIM, 1, Gfx.CENTER);
        btn(sw / 2 - bw / 2, y + 38, bw, bh, "CONTINUAR", B_RESUME, true, g);
        btn(sw / 2 - bw / 2, y + 60, bw, bh, "RECOMEÇAR", B_RESTART, false, g);
        btn(sw / 2 - bw / 2, y + 82, bw, bh, "NÍVEIS", B_LEVELS, false, g);
    }

    private void renderEnd(Gfx g) {
        btns.clear();
        dim(g, 0.5f);
        boolean win = world.winner == me;
        float w = Math.min(sw - 20, 300), h = 130, x = Math.round((sw - w) / 2), y = Math.round((sh - h) / 2);
        bevel(g, x, y, w, h, METAL, false);
        rivets(g, x, y, w, h);
        boolean lost = vs != null && netLost && world.winner < 0;
        text(g, lost ? "LIGAÇÃO PERDIDA" : win ? "VITÓRIA!" : "DERROTA", sw / 2, y + 8, win ? ACCENT_L : BAD, lost ? 2 : 3, Gfx.CENTER);
        int secs = (int) world.time;
        String[] st = {
            Level.ALL[levelIdx].name.toUpperCase() + " · " + (vs != null ? "2 JOGADORES" : DIFF[difficulty]) + " · " + secs / 60 + " MIN " + secs % 60 + " S",
            "FABRICADAS " + world.produced[me] + " · PERDIDAS " + world.lost[me] + " · ABATIDAS " + world.kills[me],
            "BANDEIRAS " + world.captures[me] + " · SETORES NO FIM " + world.ownedSectors(me) + " DE " + world.sectors.length,
        };
        float ty = y + 38;
        for (String s : st) { text(g, s, sw / 2, ty, TXT, 1, Gfx.CENTER); ty += 12; }
        float bw = Math.round((w - 24) / 3), bh = 20, by = y + h - bh - 8;
        if (vs != null) {
            btn(sw / 2 - bw / 2, by, bw, bh, "MENU", B_QUIT, true, g);
            return;
        }
        boolean hasNext = win && levelIdx + 1 < Level.ALL.length;
        int nb = hasNext ? 3 : 2;
        float bx = Math.round(sw / 2 - (nb * bw + (nb - 1) * 4) / 2);
        if (hasNext) btn(bx, by, bw, bh, "SEGUINTE", B_NEXT, true, g);
        btn(bx + (bw + 4) * (hasNext ? 1 : 0), by, bw, bh, "REPETIR", B_RESTART, !hasNext, g);
        btn(bx + (bw + 4) * (hasNext ? 2 : 1), by, bw, bh, "NÍVEIS", B_LEVELS, false, g);
    }
}
