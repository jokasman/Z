package pt.setorzero.core;

public final class Unit extends Entity {
    public static final int IDLE = 0, MOVE = 1, ATTACK = 2, AMOVE = 3, BOARD = 4;

    public final UnitType type;
    public float angle, aim, anim;
    public float cd, scan, repath;
    public int order = IDLE;
    public float ox, oy;          // destino da ordem
    public float ax, ay;          // âncora (onde fica parado)
    public Entity target;         // alvo da ordem (ATTACK / BOARD)
    public Entity auto;           // alvo escolhido automaticamente
    public float[] path;
    public int pi;
    public float px, py;          // ponto para onde o caminho foi calculado
    public boolean pathFailed;
    public float stuckT, lastX, lastY;
    public int stuckN;
    public UnitType crew;         // veículos: tripulante que sai quando é destruído
    public Unit boardedInto;
    public boolean selected;

    public Unit(UnitType type) {
        this.type = type;
        this.radius = type.radius;
        this.maxHp = type.hp;
        this.hp = type.hp;
    }

    @Override public int armor() { return type.armor; }
}
