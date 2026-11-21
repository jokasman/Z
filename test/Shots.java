package pt.setorzero.core;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/** Gera capturas de ecrã dos vários ecrãs do jogo (tamanho de um telemóvel deitado). */
public final class Shots {
    static int W = 1600, H = 720;
    static float DP = 1.85f;

    public static void main(String[] a) throws Exception {
        String out = a.length > 0 ? a[0] : "build/shots";
        if (a.length > 3) { W = Integer.parseInt(a[1]); H = Integer.parseInt(a[2]); DP = Float.parseFloat(a[3]); }
        new File(out).mkdirs();
        Game g = new Game(Sfx.NONE);
        g.cacheScale = 1.5f;
        g.resize(W, H, DP);
        for (int i = 0; i < 30 * 25; i++) g.update(1 / 30f);
        shot(g, out + "/1-titulo.png");

        g.start(1);
        for (int i = 0; i < 30; i++) g.update(1 / 30f);
        tap(g, 10 * DP + 40 * DP, H - 30 * DP); // botão "Todos"
        shot(g, out + "/2-inicio.png");

        // painel da fábrica
        Building home = null;
        for (Building b : g.world.buildings) if (b.isFactory() && b.team == World.PLAYER) home = b;
        float sx = (home.x - g.camX) * g.zoom, sy = (home.y - g.camY) * g.zoom;
        tap(g, sx, sy);
        for (int i = 0; i < 20; i++) g.update(1 / 30f);
        shot(g, out + "/3-fabrica.png");
        g.panel = null;

        // a meio do jogo: o computador também controla o lado laranja
        g.world.ais.add(new Ai(g.world, World.PLAYER, 1));
        for (int i = 0; i < 30 * 75; i++) g.update(1 / 30f);
        Unit focus = busiest(g.world);
        g.camX = focus.x - W / g.zoom / 2;
        g.camY = focus.y - H / g.zoom / 2;
        g.sel.clear();
        for (Unit u : g.world.units) {
            if (u.alive && u.team == World.PLAYER && World.dist(u.x, u.y, focus.x, focus.y) < 200) { u.selected = true; g.sel.add(u); }
        }
        shot(g, out + "/4-combate.png");

        g.pointerDown(0, 400, 300);
        g.pointerMove(0, 700, 500);
        BufferedImage box = render(g);
        g.pointerCancel();
        ImageIO.write(box, "png", new File(out + "/5-selecao.png"));

        g.back();
        shot(g, out + "/6-pausa.png");
        g.back();

        for (int i = 0; i < 30 * 60 * 20 && g.world.winner < 0; i++) g.world.update(1 / 30f);
        for (int i = 0; i < 30 * 4; i++) g.update(1 / 30f);
        shot(g, out + "/7-fim.png");

        g.state = Game.HELP;
        shot(g, out + "/8-ajuda.png");
        System.out.println("capturas em " + out);
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
        g.render(new DesktopGfx(new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB).createGraphics()));
        g.pointerDown(0, x, y);
        g.pointerUp(0, x, y);
    }

    static BufferedImage render(Game g) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = img.createGraphics();
        g.render(new DesktopGfx(gr));
        gr.dispose();
        return img;
    }

    static void shot(Game g, String path) throws Exception {
        ImageIO.write(render(g), "png", new File(path));
    }
}
