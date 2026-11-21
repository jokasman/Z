package pt.setorzero.core;

/** Tipos de unidade e armas. Velocidades e alcances são dados em quadrículas e guardados em píxeis do mundo. */
public final class UnitType {
    public static final int INF = 0, VEH = 1, BLD = 2;
    public static final int W_GUN = 0, W_MG = 1, W_SNIPER = 2, W_ROCKET = 3, W_SHELL = 4;

    public final int id;
    public final String name, desc;
    public final int armor, weapon;
    public final float hp, speed, range, damage, cooldown, splash, buildTime, radius;
    public final float vsInf, vsVeh, vsBld;
    public final boolean vehicle;

    private UnitType(int id, String name, String desc, int armor, int weapon, float hp, float speed, float range,
                     float damage, float cooldown, float splash, float buildTime, float radius,
                     float vsInf, float vsVeh, float vsBld) {
        float t = World.TILE;
        this.id = id; this.name = name; this.desc = desc; this.armor = armor; this.weapon = weapon;
        this.hp = hp; this.speed = speed * t; this.range = range * t; this.damage = damage;
        this.cooldown = cooldown; this.splash = splash * t; this.buildTime = buildTime; this.radius = radius * t;
        this.vsInf = vsInf; this.vsVeh = vsVeh; this.vsBld = vsBld;
        this.vehicle = armor == VEH;
    }

    public float mult(int targetArmor) {
        return targetArmor == INF ? vsInf : targetArmor == VEH ? vsVeh : vsBld;
    }

    public static final UnitType BATEDOR = new UnitType(0, "Batedor", "Rápido e barato",
        INF, W_GUN, 110, 1.7f, 4.5f, 9, 0.7f, 0, 20, 0.32f, 1f, 0.35f, 0.25f);
    public static final UnitType ATIRADOR = new UnitType(1, "Atirador", "Longo alcance",
        INF, W_SNIPER, 90, 1.5f, 7.5f, 34, 2.6f, 0, 28, 0.32f, 1.4f, 0.3f, 0.2f);
    public static final UnitType BAZUCA = new UnitType(2, "Bazuca", "Contra veículos",
        INF, W_ROCKET, 120, 1.4f, 5.5f, 50, 3.0f, 0.7f, 30, 0.34f, 0.5f, 1.6f, 1.5f);
    public static final UnitType JIPE = new UnitType(3, "Jipe", "Muito rápido",
        VEH, W_MG, 320, 3.0f, 5f, 8, 0.32f, 0, 36, 0.48f, 1.2f, 0.4f, 0.2f);
    public static final UnitType TANQUE = new UnitType(4, "Tanque", "Blindado e forte",
        VEH, W_SHELL, 760, 1.2f, 6f, 70, 3.0f, 1f, 56, 0.6f, 0.8f, 1f, 1.4f);

    /** Armas dos edifícios (não se fabricam). */
    public static final UnitType TURRET_GUN = new UnitType(10, "Canhão", "",
        BLD, W_SHELL, 900, 0, 7f, 45, 2.2f, 0.8f, 0, 0.9f, 0.9f, 1.1f, 1f);
    public static final UnitType FORT_GUN = new UnitType(11, "Forte", "",
        BLD, W_SHELL, 8000, 0, 7f, 40, 1.6f, 1f, 0, 1.4f, 1f, 0.9f, 0.3f);

    public static final UnitType[] ROBOTS = {BATEDOR, ATIRADOR, BAZUCA};
    public static final UnitType[] VEHICLES = {JIPE, TANQUE};
    public static final UnitType[] ALL = {BATEDOR, ATIRADOR, BAZUCA, JIPE, TANQUE};
}
