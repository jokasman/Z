package pt.setorzero.core;

import java.util.HashMap;

/** Guarda definições e progresso (SharedPreferences no Android). */
public interface Store {
    int get(String key, int def);
    void put(String key, int value);

    final class Memory implements Store {
        private final HashMap<String, Integer> m = new HashMap<>();
        public int get(String key, int def) { Integer v = m.get(key); return v == null ? def : v; }
        public void put(String key, int value) { m.put(key, value); }
    }
}
