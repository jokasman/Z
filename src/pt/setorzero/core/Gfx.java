package pt.setorzero.core;

/** Superfície de desenho mínima, implementada sobre o Canvas do Android e sobre o Java2D (testes). */
public interface Gfx {
    int LEFT = 0, CENTER = 1, RIGHT = 2;

    interface Painter { void paint(Gfx g); }

    void color(int argb);
    void fillRect(float x, float y, float w, float h);
    void fillRoundRect(float x, float y, float w, float h, float r);
    void strokeRect(float x, float y, float w, float h, float width);
    void strokeRoundRect(float x, float y, float w, float h, float r, float width);
    void fillCircle(float cx, float cy, float r);
    void strokeCircle(float cx, float cy, float r, float width);
    void fillOval(float cx, float cy, float rx, float ry);
    void strokeOval(float cx, float cy, float rx, float ry, float width);
    void line(float x1, float y1, float x2, float y2, float width);
    /** Polígono preenchido: pts = x0, y0, x1, y1, ... */
    void fillPoly(float[] pts, int n);
    /** Texto com y no centro vertical. */
    void text(String s, float x, float y, float size, int align, boolean bold);
    float textWidth(String s, float size, boolean bold);
    void save();
    void restore();
    void translate(float x, float y);
    void rotate(float rad);
    void scale(float s);
    void clip(float x, float y, float w, float h);
    /** Desenha (e guarda em memória na primeira vez) uma camada estática de w×h unidades. */
    void cached(String key, int w, int h, float scale, boolean opaque, Painter p, float dx, float dy, float dw, float dh);
}
