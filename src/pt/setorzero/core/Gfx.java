package pt.setorzero.core;

/** Superfície de desenho mínima, implementada sobre o Canvas do Android e sobre o Java2D (testes). */
public interface Gfx {
    int LEFT = 0, CENTER = 1, RIGHT = 2;

    /** Fornece os píxeis (ARGB) de uma imagem, só chamado na primeira vez que a imagem é usada. */
    interface PixelSource { int[] pixels(); }

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
    void save();
    void restore();
    void translate(float x, float y);
    void rotate(float rad);
    void scale(float s);
    void clip(float x, float y, float w, float h);
    /** Desenha uma imagem w×h (guardada em memória pela chave) no retângulo dado, sem suavizar os píxeis. */
    void image(String key, int w, int h, PixelSource src, float dx, float dy, float dw, float dh);
}
