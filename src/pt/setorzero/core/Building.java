package pt.setorzero.core;

public final class Building extends Entity {
    public static final int FORT = 0, FACT_ROBOT = 1, FACT_VEH = 2, TURRET = 3;

    public final int kind, tx, ty, tw, th;
    public Sector sector;
    public UnitType making;
    public float progress;
    public boolean justMade;
    public float aim, cd, scan, smokeT;
    public Entity target;
    public boolean destroyed;
    public boolean doorUp;

    public Building(int kind, int tx, int ty) {
        this.kind = kind;
        this.tx = tx;
        this.ty = ty;
        this.tw = kind == FORT ? 3 : kind == TURRET ? 2 : 3;
        this.th = kind == FORT ? 3 : 2;
        float t = World.TILE;
        this.x = (tx + tw * 0.5f) * t;
        this.y = (ty + th * 0.5f) * t;
        this.radius = Math.min(tw, th) * t * 0.5f;
        this.maxHp = kind == FORT ? UnitType.FORT_GUN.hp : kind == TURRET ? UnitType.TURRET_GUN.hp : 1000;
        this.hp = maxHp;
        this.aim = (float) (Math.PI / 2);
    }

    public boolean isFactory() { return kind == FACT_ROBOT || kind == FACT_VEH; }

    public boolean attackable() { return alive && team != World.NEUTRAL && (kind == FORT || kind == TURRET); }

    public UnitType[] buildable() { return kind == FACT_VEH ? UnitType.VEHICLES : UnitType.ROBOTS; }

    public UnitType weapon() { return kind == FORT ? UnitType.FORT_GUN : UnitType.TURRET_GUN; }

    public float doorX() { return (tx + tw * 0.5f) * World.TILE; }

    public float doorY() { return (doorUp ? ty - 0.6f : ty + th + 0.6f) * World.TILE; }

    public String title() {
        switch (kind) {
            case FORT: return "Forte";
            case FACT_ROBOT: return "Fábrica de robôs";
            case FACT_VEH: return "Fábrica de veículos";
            default: return "Canhão";
        }
    }

    public boolean contains(float px, float py, float tol) {
        float t = World.TILE;
        return px >= tx * t - tol && px <= (tx + tw) * t + tol && py >= ty * t - tol && py <= (ty + th) * t + tol;
    }

    @Override public int armor() { return UnitType.BLD; }
}
