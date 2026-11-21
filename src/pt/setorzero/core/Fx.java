package pt.setorzero.core;

/** Efeito visual de curta duração. */
public final class Fx {
    public static final int TRACER = 0, FLASH = 1, BOOM = 2, SMOKE = 3, SCORCH = 4, RING = 5, DEBRIS = 6;

    public int kind, color;
    public float x, y, x2, y2, vx, vy, t, life, size;
}
