package pt.setorzero.core;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.HashMap;

/** Implementação de Gfx em Java2D, para testes e capturas de ecrã no computador. */
public final class DesktopGfx implements Gfx {
    private final Graphics2D g;
    private final ArrayDeque<AffineTransform> stack = new ArrayDeque<>();
    private final ArrayDeque<Shape> clips = new ArrayDeque<>();
    private static final HashMap<String, BufferedImage> CACHE = new HashMap<>();

    public DesktopGfx(Graphics2D g) {
        this.g = g;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    public void color(int argb) { g.setColor(new Color(argb, true)); }
    public void fillRect(float x, float y, float w, float h) { g.fill(new Rectangle2D.Float(x, y, w, h)); }
    public void fillRoundRect(float x, float y, float w, float h, float r) { g.fill(new RoundRectangle2D.Float(x, y, w, h, r * 2, r * 2)); }
    public void strokeRect(float x, float y, float w, float h, float width) { stroke(width); g.draw(new Rectangle2D.Float(x, y, w, h)); }
    public void strokeRoundRect(float x, float y, float w, float h, float r, float width) { stroke(width); g.draw(new RoundRectangle2D.Float(x, y, w, h, r * 2, r * 2)); }
    public void fillCircle(float cx, float cy, float r) { g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2)); }
    public void strokeCircle(float cx, float cy, float r, float width) { stroke(width); g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2)); }
    public void fillOval(float cx, float cy, float rx, float ry) { g.fill(new Ellipse2D.Float(cx - rx, cy - ry, rx * 2, ry * 2)); }
    public void strokeOval(float cx, float cy, float rx, float ry, float width) { stroke(width); g.draw(new Ellipse2D.Float(cx - rx, cy - ry, rx * 2, ry * 2)); }
    public void line(float x1, float y1, float x2, float y2, float width) { stroke(width); g.draw(new Line2D.Float(x1, y1, x2, y2)); }

    private void stroke(float w) { g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); }

    public void fillPoly(float[] pts, int n) {
        Path2D.Float p = new Path2D.Float();
        p.moveTo(pts[0], pts[1]);
        for (int i = 1; i < n; i++) p.lineTo(pts[i * 2], pts[i * 2 + 1]);
        p.closePath();
        g.fill(p);
    }

    public void save() { stack.push(g.getTransform()); clips.push(g.getClip() == null ? new Rectangle2D.Float(-1e6f, -1e6f, 2e6f, 2e6f) : g.getClip()); }
    public void restore() { g.setTransform(stack.pop()); g.setClip(clips.pop()); }
    public void translate(float x, float y) { g.translate(x, y); }
    public void rotate(float rad) { g.rotate(rad); }
    public void scale(float s) { g.scale(s, s); }
    public void clip(float x, float y, float w, float h) { g.clip(new Rectangle2D.Float(x, y, w, h)); }

    public void image(String key, int w, int h, PixelSource src, float dx, float dy, float dw, float dh) {
        BufferedImage img = CACHE.get(key);
        if (img == null) {
            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, w, h, src.pixels(), 0, w);
            CACHE.put(key, img);
        }
        AffineTransform at = g.getTransform();
        g.translate(dx, dy);
        g.scale(dw / img.getWidth(), dh / img.getHeight());
        g.drawImage(img, 0, 0, null);
        g.setTransform(at);
        g.setComposite(AlphaComposite.SrcOver);
    }
}
