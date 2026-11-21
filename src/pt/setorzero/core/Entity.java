package pt.setorzero.core;

public abstract class Entity {
    public int id, team;
    public float x, y, hp, maxHp, radius;
    public boolean alive = true;
    public float flash;

    public abstract int armor();
}
