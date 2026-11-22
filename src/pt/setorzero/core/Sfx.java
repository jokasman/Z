package pt.setorzero.core;

/** Efeitos sonoros (gerados por código em SoundSynth). */
public interface Sfx {
    int SHOT = 0, MG = 1, SNIPE = 2, ROCKET = 3, CANNON = 4, EXPLODE = 5, CAPTURE = 6, READY = 7,
        CLICK = 8, WIN = 9, LOSE = 10, ALERT = 11, BOARD = 12;
    int COUNT = 13;

    int MUSIC_MENU = 0, MUSIC_BATTLE = 1, MUSIC_NONE = -1;

    void play(int id, float volume);

    /** Muda a música de fundo (MUSIC_NONE para parar). */
    void music(int track);

    Sfx NONE = new Sfx() {
        public void play(int id, float volume) { }
        public void music(int track) { }
    };
}
