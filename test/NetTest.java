package pt.setorzero.core;

import java.util.ArrayList;

/** Dois jogos ligados pela rede no mesmo computador: anfitrião (laranja) e convidado (ciano). */
public final class NetTest {
    static int fails = 0;

    static void check(boolean ok, String what) {
        if (!ok) { fails++; System.out.println("FALHOU: " + what); } else System.out.println("ok: " + what);
    }

    static void run(Game a, Game b, float seconds) throws Exception {
        int frames = (int) (seconds * 30);
        for (int i = 0; i < frames; i++) {
            a.update(1 / 30f);
            b.update(1 / 30f);
            Thread.sleep(5);
        }
    }

    public static void main(String[] args) throws Exception {
        for (int lv = 0; lv < Level.ALL.length; lv += 5) {
            Game host = new Game(Sfx.NONE), guest = new Game(Sfx.NONE);
            host.resize(600, 270, 1);
            guest.resize(600, 270, 1);
            host.hostGame(lv);
            run(host, guest, 0.5f);
            guest.joinAddress("127.0.0.1");
            for (int i = 0; i < 200 && (host.state != Game.PLAY || guest.state != Game.PLAY); i++) run(host, guest, 0.05f);
            check(host.state == Game.PLAY && guest.state == Game.PLAY, "nível " + (lv + 1) + ": os dois começam a partida");
            if (host.state != Game.PLAY || guest.state != Game.PLAY) continue;
            check(host.me == World.PLAYER && guest.me == World.ENEMY, "anfitrião é laranja, convidado é ciano");
            check(host.world.level == guest.world.level && host.world.W == guest.world.W, "mesmo mapa nos dois");
            run(host, guest, 1f);
            // o convidado manda as suas unidades para a bandeira do centro
            ArrayList<Unit> mine = new ArrayList<>();
            for (Unit u : guest.world.units) if (u.team == World.ENEMY) mine.add(u);
            check(!mine.isEmpty(), "o convidado vê as suas unidades");
            Sector target = guest.world.sectors[guest.world.sectors.length / 2];
            float before = 0;
            for (Unit u : host.world.units) if (u.team == World.ENEMY) before += World.dist(u.x, u.y, target.fx, target.fy);
            guest.vs.move(mine, target.fx, target.fy);
            run(host, guest, 6f);
            float after = 0;
            for (Unit u : host.world.units) if (u.team == World.ENEMY) after += World.dist(u.x, u.y, target.fx, target.fy);
            check(after < before * 0.9f, "as ordens do convidado chegam ao anfitrião (" + (int) before + " → " + (int) after + ")");
            // as posições no convidado acompanham as do anfitrião
            float err = 0;
            int n = 0;
            for (Unit hu : host.world.units) {
                for (Unit gu : guest.world.units) if (gu.id == hu.id) { err += World.dist(hu.x, hu.y, gu.x, gu.y); n++; }
            }
            check(n >= host.world.units.size() - 2 && err / Math.max(1, n) < 24, "o convidado vê o mesmo que o anfitrião (" + n + " unidades, desvio médio " + (int) (err / Math.max(1, n)) + " px)");
            // produção pedida pelo convidado
            Building fac = null;
            for (Building b : guest.world.buildings) if (b.isFactory() && b.team == World.ENEMY) { fac = b; break; }
            guest.vs.produce(fac, UnitType.BAZUCA);
            run(host, guest, 1f);
            Building hf = host.world.buildings.get(guest.world.buildings.indexOf(fac));
            check(hf.making == UnitType.BAZUCA && fac.making == UnitType.BAZUCA, "o convidado escolhe o que a fábrica produz");
            // fim: o convidado sai e o anfitrião é avisado
            guest.back();
            check(guest.state == Game.PAUSE, "voltar abre o menu sem parar o jogo");
            guest.render(new DesktopGfx(new java.awt.image.BufferedImage(600, 270, 1).createGraphics()));
            guest.pointerDown(0, 300, 270 / 2f - 56 + 70 + 9);
            guest.pointerUp(0, 300, 270 / 2f - 56 + 70 + 9);
            run(host, guest, 1.5f);
            check(host.state == Game.END, "o anfitrião vê que o outro jogador saiu");
            host.back();
        }
        System.out.println(fails == 0 ? "ALL PASS" : fails + " FALHAS");
        System.exit(fails == 0 ? 0 : 1);
    }
}
