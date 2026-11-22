package pt.setorzero.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.HashMap;

import pt.setorzero.core.Gfx;

/** Implementação de Gfx sobre o Canvas do Android. */
final class AndroidGfx implements Gfx {
    private Canvas c;
    private int col = 0xFFFFFFFF;
    // sem suavização: o jogo é desenhado em píxeis grandes
    private final Paint fill = new Paint();
    private final Paint stroke = new Paint();
    private final Paint bmp = new Paint();
    private final Path path = new Path();
    private final RectF rf = new RectF();
    private final HashMap<String, Bitmap> cache;

    AndroidGfx() { this(new HashMap<String, Bitmap>()); }

    AndroidGfx(HashMap<String, Bitmap> cache) {
        this.cache = cache;
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.SQUARE);
        bmp.setFilterBitmap(false);
    }

    void set(Canvas c) { this.c = c; }

    @Override public void color(int argb) { col = argb; }

    private Paint f() { fill.setColor(col); return fill; }

    private Paint s(float w) { stroke.setColor(col); stroke.setStrokeWidth(w); return stroke; }

    @Override public void fillRect(float x, float y, float w, float h) { c.drawRect(x, y, x + w, y + h, f()); }

    @Override public void fillRoundRect(float x, float y, float w, float h, float r) {
        rf.set(x, y, x + w, y + h);
        c.drawRoundRect(rf, r, r, f());
    }

    @Override public void strokeRect(float x, float y, float w, float h, float width) { c.drawRect(x, y, x + w, y + h, s(width)); }

    @Override public void strokeRoundRect(float x, float y, float w, float h, float r, float width) {
        rf.set(x, y, x + w, y + h);
        c.drawRoundRect(rf, r, r, s(width));
    }

    @Override public void fillCircle(float cx, float cy, float r) { c.drawCircle(cx, cy, r, f()); }

    @Override public void strokeCircle(float cx, float cy, float r, float width) { c.drawCircle(cx, cy, r, s(width)); }

    @Override public void fillOval(float cx, float cy, float rx, float ry) {
        rf.set(cx - rx, cy - ry, cx + rx, cy + ry);
        c.drawOval(rf, f());
    }

    @Override public void strokeOval(float cx, float cy, float rx, float ry, float width) {
        rf.set(cx - rx, cy - ry, cx + rx, cy + ry);
        c.drawOval(rf, s(width));
    }

    @Override public void line(float x1, float y1, float x2, float y2, float width) { c.drawLine(x1, y1, x2, y2, s(width)); }

    @Override public void fillPoly(float[] pts, int n) {
        path.reset();
        path.moveTo(pts[0], pts[1]);
        for (int i = 1; i < n; i++) path.lineTo(pts[i * 2], pts[i * 2 + 1]);
        path.close();
        c.drawPath(path, f());
    }

    @Override public void save() { c.save(); }

    @Override public void restore() { c.restore(); }

    @Override public void translate(float x, float y) { c.translate(x, y); }

    @Override public void rotate(float rad) { c.rotate((float) Math.toDegrees(rad)); }

    @Override public void scale(float sc) { c.scale(sc, sc); }

    @Override public void clip(float x, float y, float w, float h) { c.clipRect(x, y, x + w, y + h); }

    @Override public void image(String key, int w, int h, PixelSource src, float dx, float dy, float dw, float dh) {
        Bitmap b = cache.get(key);
        if (b == null) {
            if (cache.size() > 10) cache.clear();
            b = Bitmap.createBitmap(src.pixels(), w, h, Bitmap.Config.ARGB_8888);
            cache.put(key, b);
        }
        rf.set(dx, dy, dx + dw, dy + dh);
        c.drawBitmap(b, null, rf, bmp);
    }
}
