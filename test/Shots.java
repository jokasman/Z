package pt.setorzero.core;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/** Capturas de ecrã: desenha em baixa resolução e amplia como o telemóvel (com o efeito CRT). */
public final class Shots {
    static int PW = 2400, PH = 1080, P, VW, VH;

    public static void main(String[] a) throws Exception {
        String out = a.length > 0 ? a[0] : "build/shots";
        if (a.length > 2) { PW = Integer.parseInt(a[1]); PH = Integer.parseInt(a[2]); }
        P = Math.max(1, Math.round(PH / 280f));
        VW = (PW + P - 1) / P;
        VH = (PH + P - 1) / P;
        new File(out).mkdirs();
        Game g = new Game(Sfx.NONE);
        g.resize(VW, VH, 1);
        for (int i = 0; i < 30 * 25; i++) g.update(1 / 30f);
        shot(g, out + "/01-titulo.png");
        g.state = Game.LEVELS;
        shot(g, out + "/02-niveis.png");
        g.state = Game.HELP;
        shot(g, out + "/03-ajuda.png");
        g.state = Game.NETMENU;
        shot(g, out + "/08-2jogadores.png");
        g.hostGame(2);
        for (int i = 0; i < 10; i++) g.update(1 / 30f);
        shot(g, out + "/09-espera.png");
        g.back();
        g.state = Game.KEYPAD;
        shot(g, out + "/10-endereco.png");
        g.back();
        shot(g, out + "/11-procurar.png");
        g.back();
        g.state = Game.TITLE;

        for (int lv = 0; lv < Level.ALL.length; lv++) {
            g.start(lv);
            for (int i = 0; i < 20; i++) g.update(1 / 30f);
            if (lv == 0) tap(g, 10 + 24, VH - 15); // "TODOS"
            shot(g, String.format("%s/1%02d-nivel.png", out, lv + 1));
        }

        g.start(0);
        for (int i = 0; i < 20; i++) g.update(1 / 30f);
        Building home = null;
        for (Building b : g.world.buildings) if (b.isFactory() && b.team == World.PLAYER) home = b;
        g.panel = home;
        shot(g, out + "/04-fabrica.png");
        g.panel = null;

        g.world.ais.add(new Ai(g.world, World.PLAYER, 1));
        for (int i = 0; i < 30 * 70; i++) g.update(1 / 30f);
        Unit focus = busiest(g.world);
        g.camX = focus.x - VW / g.zoom / 2;
        g.camY = focus.y - VH / g.zoom / 2;
        for (Unit u : g.world.units)
            if (u.alive && u.team == World.PLAYER && World.dist(u.x, u.y, focus.x, focus.y) < 200) { u.selected = true; g.sel.add(u); }
        shot(g, out + "/05-combate.png");

        g.back();
        shot(g, out + "/06-pausa.png");
        g.back();
        for (int i = 0; i < 30 * 60 * 20 && g.world.winner < 0; i++) g.world.update(1 / 30f);
        for (int i = 0; i < 30 * 4; i++) g.update(1 / 30f);
        shot(g, out + "/07-fim.png");
        System.out.println("capturas em " + out + " (" + VW + "×" + VH + " ampliado ×" + P + ")");
    }

    static Unit busiest(World w) {
        Unit best = null;
        int bn = -1;
        for (Unit u : w.units) {
            if (!u.alive || u.team == World.NEUTRAL) continue;
            int n = 0;
            for (Unit o : w.units) if (o.alive && World.dist(u.x, u.y, o.x, o.y) < 250) n += o.team != u.team ? 3 : 1;
            if (n > bn) { bn = n; best = u; }
        }
        return best;
    }

    static void tap(Game g, float x, float y) {
        render(g);
        g.pointerDown(0, x, y);
        g.pointerUp(0, x, y);
    }

    static BufferedImage render(Game g) {
        BufferedImage img = new BufferedImage(VW, VH, BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = img.createGraphics();
        g.render(new DesktopGfx(gr));
        gr.dispose();
        return img;
    }

    static void shot(Game g, String path) throws Exception {
        BufferedImage low = render(g);
        int w = PW / 2, h = PH / 2; // metade do tamanho real, para ficheiros menores
        float s = P / 2f;
        BufferedImage big = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = big.createGraphics();
        gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gr.drawImage(low, 0, 0, Math.round(VW * s), Math.round(VH * s), null);
        if (g.crt()) {
            gr.setColor(new java.awt.Color(0, 0, 0, 50));
            for (int y = 0; y < VH; y++) gr.fillRect(0, Math.round((y + 1) * s) - 1, w, 1);
        }
        gr.dispose();
        ImageIO.write(big, "png", new File(path));
    }
}
