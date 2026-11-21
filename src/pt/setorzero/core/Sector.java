package pt.setorzero.core;

import java.util.ArrayList;

public final class Sector {
    public final int idx;
    public final String name;
    public final int x0, y0, x1, y1; // quadrículas, inclusivo
    public float fx, fy;             // bandeira (píxeis do mundo)
    public int owner;
    public float flashT;
    public final ArrayList<Building> buildings = new ArrayList<>();

    public Sector(int idx, String name, int x0, int y0, int x1, int y1) {
        this.idx = idx; this.name = name; this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1;
    }

    public boolean contains(float px, float py) {
        float t = World.TILE;
        return px >= x0 * t && px < (x1 + 1) * t && py >= y0 * t && py < (y1 + 1) * t;
    }
}
