package pt.setorzero.core;

/** Efeitos sonoros (gerados por código em SoundSynth). */
public interface Sfx {
    int SHOT = 0, MG = 1, SNIPE = 2, ROCKET = 3, CANNON = 4, EXPLODE = 5, CAPTURE = 6, READY = 7,
        CLICK = 8, WIN = 9, LOSE = 10, ALERT = 11, BOARD = 12;
    int COUNT = 13;

    void play(int id, float volume);

    Sfx NONE = new Sfx() { public void play(int id, float volume) { } };
}
