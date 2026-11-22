package pt.setorzero.core;

import static pt.setorzero.core.Building.FACT_ROBOT;
import static pt.setorzero.core.Building.FACT_VEH;
import static pt.setorzero.core.Building.TURRET;
import static pt.setorzero.core.World.DIRT;
import static pt.setorzero.core.World.NEUTRAL;
import static pt.setorzero.core.World.ROCK;
import static pt.setorzero.core.World.TREE;
import static pt.setorzero.core.World.WATER;
import static pt.setorzero.core.World.ENEMY;
import static pt.setorzero.core.UnitType.BATEDOR;
import static pt.setorzero.core.UnitType.ATIRADOR;
import static pt.setorzero.core.UnitType.BAZUCA;
import static pt.setorzero.core.UnitType.JIPE;
import static pt.setorzero.core.UnitType.TANQUE;

/** Os níveis da campanha. */
public final class Level {
    interface Builder { void build(MapData m); }

    public final int idx;
    public final String name, brief;
    public final int theme, W, H;
    public final float enemyProd;
    final UnitType[] playerExtra, enemyExtra;
    final Builder builder;

    private Level(int idx, String name, int theme, int W, int H, float enemyProd, String brief,
                  UnitType[] playerExtra, UnitType[] enemyExtra, Builder builder) {
        this.idx = idx; this.name = name; this.theme = theme; this.W = W; this.H = H; this.enemyProd = enemyProd;
        this.brief = brief; this.playerExtra = playerExtra; this.enemyExtra = enemyExtra; this.builder = builder;
    }

    private static UnitType[] u(UnitType... t) { return t; }

    public static final Level[] ALL = {
        new Level(0, "Vale das Bandeiras", TerrainArt.TEMPERATE, 48, 36, 1f,
            "Um vale tranquilo. Capturem as bandeiras e a fábrica de veículos do Vale Oeste.", null, null, m -> {
            m.sectors(3, 3, "Pedreira Norte", "Planalto Norte", "Base Ciano", "Vale Oeste", "Lago Central", "Vale Este",
                "Base Laranja", "Planalto Sul", "Pedreira Sul");
            m.dirtPatches(16, 7);
            m.rect(ROCK, 15, 1, 16, 5).rect(ROCK, 31, 6, 32, 10).rect(ROCK, 1, 11, 7, 12).rect(ROCK, 35, 11, 40, 12);
            m.blob(ROCK, 44.5f, 8.5f, 1.5f, 1.2f).blob(ROCK, 24.5f, 12.5f, 2f, 0.9f);
            m.blob(WATER, 22f, 2.5f, 3f, 1.4f).blob(WATER, 20.5f, 18.5f, 1.6f, 2.4f);
            m.blob(TREE, 12.5f, 9.5f, 1.3f, 1.1f).blob(TREE, 2.5f, 7.5f, 1.2f, 1.6f).blob(TREE, 28f, 2f, 1.2f, 1f)
                .blob(TREE, 2f, 20f, 1f, 1.5f).blob(TREE, 13f, 15.5f, 1.2f, 1.2f);
            m.flag(0, 8, 8).flag(1, 20, 7).flag(3, 9, 19).centerFlag(4);
            m.homes();
            m.place(FACT_ROBOT, 4, 3, NEUTRAL).place(TURRET, 25, 4, NEUTRAL).place(FACT_VEH, 4, 15, NEUTRAL).place(TURRET, 18, 13, NEUTRAL);
            m.vehicle(JIPE, 12, 4).vehicle(TANQUE, 28, 8).vehicle(JIPE, 17, 20);
        }),

        new Level(1, "Dunas de Ferro", TerrainArt.DESERT, 48, 36, 1f,
            "Deserto aberto: os jipes são rápidos aqui. Há tanques abandonados nas mesas.", null, null, m -> {
            m.sectors(3, 3, "Mesa Alta", "Duna Norte", "Base Ciano", "Poço Seco", "Oásis", "Garganta",
                "Base Laranja", "Duna Sul", "Mesa Baixa");
            m.dirtPatches(10, 21);
            m.blob(ROCK, 10, 6, 3, 2).blob(ROCK, 24, 9.5f, 2.2f, 1.4f).blob(ROCK, 6, 17, 1.5f, 3).blob(ROCK, 17, 14, 2, 1.2f);
            m.rect(ROCK, 30, 3, 31, 8);
            m.blob(WATER, 20, 15.5f, 1.6f, 1.2f).blob(WATER, 3.5f, 9.5f, 1.4f, 1f);
            m.blob(TREE, 22, 16.5f, 0.9f, 0.9f).blob(TREE, 5, 10, 0.8f, 0.8f).blob(TREE, 27, 4, 0.8f, 0.8f);
            m.flag(0, 7, 10).flag(1, 22, 6).flag(3, 11, 19).centerFlag(4);
            m.homes();
            m.place(FACT_VEH, 3, 2, NEUTRAL).place(FACT_ROBOT, 19, 2, NEUTRAL).place(TURRET, 12, 15, NEUTRAL);
            m.vehicle(TANQUE, 13, 9).vehicle(JIPE, 27, 13).vehicle(TANQUE, 4, 21);
        }),

        new Level(2, "Rio Partido", TerrainArt.TEMPERATE, 64, 36, 1f,
            "Um rio corta o mapa ao meio. Só há três pontes: controlem-nas.", null, null, m -> {
            m.sectors(4, 3, "Moinho", "Ponte Norte", "Margem Norte", "Base Ciano", "Bosque Oeste", "Vau",
                "Vau Este", "Bosque Este", "Base Laranja", "Margem Sul", "Ponte Sul", "Moinho Este");
            m.dirtPatches(14, 33);
            m.rect(WATER, 31, 1, 32, 34);
            m.blob(WATER, 30.5f, 10, 1.6f, 2.5f).blob(WATER, 33, 14, 1.4f, 2);
            m.bridge(29, 6, 34, 7).bridge(28, 17, 35, 18);
            m.noise(TREE, 0.72f, 3, 5);
            m.blob(ROCK, 12, 12, 3, 0.9f).blob(ROCK, 44, 6, 1.2f, 2.2f);
            m.flag(0, 8, 7).flag(1, 22, 8).flag(2, 40, 5).flag(4, 7, 19).flag(5, 23, 17);
            m.homes();
            m.place(FACT_ROBOT, 3, 2, NEUTRAL).place(FACT_VEH, 18, 2, NEUTRAL).place(TURRET, 26, 5, NEUTRAL)
                .place(TURRET, 25, 20, NEUTRAL).place(FACT_VEH, 3, 15, NEUTRAL);
            m.vehicle(TANQUE, 13, 4).vehicle(JIPE, 20, 21).vehicle(JIPE, 44, 9);
        }),

        new Level(3, "Passo Gelado", TerrainArt.SNOW, 48, 36, 1.05f,
            "Montanhas geladas com poucas passagens. Os canhões guardam o passo central.", null, null, m -> {
            m.sectors(3, 3, "Glaciar", "Cume Norte", "Base Ciano", "Lago Gelado", "Passo", "Vertente Este",
                "Base Laranja", "Cume Sul", "Gruta");
            m.dirtPatches(8, 41);
            m.line(ROCK, 2, 13, 14, 13, 2.4f).line(ROCK, 18, 4, 18, 11, 2.2f).line(ROCK, 26, 13, 30, 9, 2.4f)
                .line(ROCK, 33, 13, 38, 13, 2.2f);
            m.blob(ROCK, 9, 5, 2, 1.4f).blob(ROCK, 24, 14.5f, 1.4f, 1);
            m.blob(WATER, 7, 19, 2.6f, 1.8f).blob(WATER, 26, 3, 2.2f, 1.2f);
            m.noise(TREE, 0.74f, 2, 9);
            m.flag(0, 6, 9).flag(1, 23, 7).flag(3, 12, 18).centerFlag(4);
            m.homes();
            m.place(FACT_ROBOT, 3, 2, NEUTRAL).place(TURRET, 20, 15, NEUTRAL).place(FACT_VEH, 2, 22, NEUTRAL)
                .place(TURRET, 27, 4, NEUTRAL);
            m.vehicle(JIPE, 12, 3).vehicle(TANQUE, 13, 21);
        }),

        new Level(4, "Arquipélago", TerrainArt.TEMPERATE, 48, 36, 1.05f,
            "Ilhas ligadas por pontes estreitas. Quem controla as pontes controla o mapa.", null, null, m -> {
            m.sectors(3, 3, "Ilha do Farol", "Ilha Norte", "Base Ciano", "Ilha Oeste", "Ilha Central", "Ilha Este",
                "Base Laranja", "Ilha Sul", "Ilha do Cabo");
            m.dirtPatches(10, 51);
            m.rect(WATER, 14, 1, 17, 34).rect(WATER, 30, 1, 33, 17).rect(WATER, 1, 10, 13, 13).rect(WATER, 18, 10, 29, 13);
            m.blob(WATER, 9, 6, 1.5f, 1.2f).blob(WATER, 24, 19, 1.2f, 1);
            m.bridge(14, 5, 17, 6).bridge(14, 17, 17, 18).bridge(6, 10, 7, 13).bridge(23, 10, 24, 13).bridge(30, 6, 33, 7);
            m.blob(TREE, 4, 4, 1.4f, 1).blob(TREE, 25, 6, 1.2f, 1.2f).blob(TREE, 11, 20, 1.2f, 1.2f);
            m.flag(0, 6, 7).flag(1, 22, 6).flag(3, 9, 17).centerFlag(4);
            m.homes();
            m.place(FACT_VEH, 2, 2, NEUTRAL).place(TURRET, 18, 2, NEUTRAL).place(FACT_ROBOT, 2, 15, NEUTRAL)
                .place(TURRET, 19, 15, NEUTRAL);
            m.vehicle(JIPE, 11, 3).vehicle(TANQUE, 26, 3);
        }),

        new Level(5, "Cratera", TerrainArt.VOLCANO, 48, 36, 1.1f,
            "Rios de lava e uma cratera no centro. A entrada da cratera é apertada.", null, u(BATEDOR), m -> {
            m.sectors(3, 3, "Escória", "Fumarola", "Base Ciano", "Lava Oeste", "Cratera", "Lava Este",
                "Base Laranja", "Cinzas", "Basalto");
            m.dirtPatches(12, 61);
            for (int y = 1; y < 35; y++)
                for (int x = 1; x < 47; x++) {
                    float dx = x + 0.5f - 24, dy = (y + 0.5f - 18) * 1.2f;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    double a = Math.atan2(dy, dx);
                    boolean gap = Math.abs(Math.sin(a - Math.PI / 4)) < 0.3;
                    if (d > 5.5f && d < 7.3f && !gap) m.set(x, y, ROCK, false);
                }
            m.line(WATER, 2, 16, 12, 22, 1.6f).line(WATER, 20, 1, 26, 8, 1.4f);
            m.bridge(6, 17, 7, 20).bridge(22, 3, 24, 4);
            m.blob(ROCK, 8, 5, 2, 1.4f).blob(ROCK, 14, 27, 1.4f, 1.2f).blob(TREE, 3, 3, 1, 1).blob(TREE, 27, 13, 1, 1);
            m.flag(0, 6, 9).flag(1, 22, 9).flag(3, 3, 13).centerFlag(4);
            m.homes();
            m.place(FACT_ROBOT, 9, 1, NEUTRAL).place(FACT_VEH, 2, 19, NEUTRAL).place(TURRET, 22, 15, NEUTRAL);
            m.vehicle(TANQUE, 28, 4).vehicle(JIPE, 13, 15);
        }),

        new Level(6, "Cidade Fantasma", TerrainArt.CITY, 64, 36, 1.1f,
            "Ruas estreitas entre prédios em ruínas, e um canal que só se atravessa pelas pontes.", null, u(BATEDOR, BAZUCA), m -> {
            m.sectors(4, 3, "Estação", "Mercado", "Teatro", "Base Ciano", "Docas Oeste", "Praça",
                "Câmara", "Docas Este", "Base Laranja", "Teatro Sul", "Mercado Sul", "Estação Sul");
            java.util.Random r = new java.util.Random(71);
            for (int by = 2; by < 17; by += 5)
                for (int bx = 2; bx < 62; bx += 6)
                    if (r.nextFloat() < 0.62f) m.rect(ROCK, bx, by, bx + 3 - r.nextInt(2), by + 2);
            m.rect(WATER, 1, 17, 62, 18);
            m.bridge(8, 16, 9, 19).bridge(24, 16, 25, 19).bridge(38, 16, 39, 19);
            m.scatter(DIRT, 18, 0.6f, 1.3f, 72);
            m.flag(0, 10, 7).flag(1, 21, 10).flag(2, 37, 7).flag(4, 5, 14).flag(5, 21, 14);
            m.homes();
            m.place(FACT_VEH, 2, 11, NEUTRAL).place(FACT_ROBOT, 26, 2, NEUTRAL).place(TURRET, 28, 12, NEUTRAL)
                .place(TURRET, 42, 11, NEUTRAL);
            m.vehicle(TANQUE, 15, 5).vehicle(JIPE, 31, 9);
        }),

        new Level(7, "Floresta Negra", TerrainArt.TEMPERATE, 64, 36, 1.1f,
            "Floresta cerrada com trilhos estreitos. Os atiradores são preciosos aqui.", u(ATIRADOR), u(ATIRADOR, BATEDOR), m -> {
            m.sectors(4, 3, "Clareira", "Trilho Norte", "Serração", "Base Ciano", "Pântano", "Carvalhos",
                "Fetos", "Pântano Este", "Base Laranja", "Serração Sul", "Trilho Sul", "Clareira Este");
            m.noise(TREE, 0.5f, 2, 81);
            m.line(DIRT, 8, 8, 30, 8, 2).line(DIRT, 30, 8, 52, 6, 2).line(DIRT, 8, 8, 6, 17, 2).line(DIRT, 22, 8, 22, 17, 2)
                .line(DIRT, 38, 6, 40, 17, 2).line(DIRT, 6, 17, 32, 18, 2);
            m.blob(WATER, 12, 14, 2.4f, 1.4f).blob(WATER, 46, 13, 1.6f, 1.2f);
            m.flag(0, 8, 8).flag(1, 22, 8).flag(2, 40, 6).flag(4, 6, 17).flag(5, 22, 17);
            m.homes();
            m.place(FACT_ROBOT, 3, 2, NEUTRAL).place(FACT_VEH, 34, 9, NEUTRAL).place(TURRET, 26, 12, NEUTRAL)
                .place(FACT_ROBOT, 15, 2, NEUTRAL);
            m.vehicle(JIPE, 27, 4);
        }),

        new Level(8, "Mar de Areia", TerrainArt.DESERT, 64, 48, 1.15f,
            "O maior deserto. O inimigo começa com uma fábrica de veículos a mais.", null, u(TANQUE), m -> {
            m.sectors(4, 4, "Ruínas", "Duna Alta", "Poço", "Base Ciano", "Mesa Oeste", "Canyon", "Miragem", "Mesa Este",
                "Mesa Oeste Sul", "Miragem Sul", "Canyon Sul", "Mesa Este Sul", "Base Laranja", "Poço Sul", "Duna Baixa", "Ruínas Sul");
            m.dirtPatches(14, 91);
            m.scatter(ROCK, 10, 1, 2.4f, 92);
            m.line(ROCK, 20, 16, 30, 22, 2.2f).line(ROCK, 40, 14, 44, 20, 2);
            m.blob(WATER, 34, 8, 2, 1.3f).blob(WATER, 10, 20, 1.6f, 1.2f).blob(TREE, 36.5f, 9.5f, 0.8f, 0.8f);
            m.flag(0, 7, 7).flag(1, 22, 6).flag(2, 38, 5).flag(4, 8, 18).flag(5, 22, 19).flag(6, 37, 17).flag(7, 54, 20);
            m.homes();
            m.place(FACT_ROBOT, 3, 2, NEUTRAL).place(FACT_VEH, 26, 2, NEUTRAL).place(TURRET, 16, 13, NEUTRAL)
                .place(FACT_VEH, 2, 14, NEUTRAL).place(TURRET, 33, 20, NEUTRAL).place(FACT_ROBOT, 49, 14, NEUTRAL);
            m.placeOne(FACT_VEH, 60, 6, ENEMY);
            m.vehicle(TANQUE, 13, 9).vehicle(JIPE, 44, 10).vehicle(TANQUE, 28, 15);
        }),

        new Level(9, "Fortaleza Gelada", TerrainArt.SNOW, 48, 36, 1.15f,
            "A base ciana está muito bem defendida. Começam com dois tanques.", u(TANQUE, TANQUE), null, m -> {
            m.sectors(3, 3, "Fiorde", "Muralha", "Base Ciano", "Pinhal", "Lago Central", "Bastião",
                "Base Laranja", "Muralha Sul", "Fiorde Sul");
            m.dirtPatches(8, 101);
            m.rect(ROCK, 30, 1, 31, 7).rect(ROCK, 33, 12, 44, 13).rect(ROCK, 1, 12, 9, 13);
            m.blob(WATER, 24, 18, 2.4f, 1.6f).blob(WATER, 8, 6, 1.6f, 1.2f);
            m.noise(TREE, 0.7f, 2, 102);
            m.flag(0, 5, 8).flag(1, 22, 7).flag(3, 12, 18).flag(4, 24, 14);
            m.homes();
            m.place(FACT_VEH, 11, 2, NEUTRAL).place(TURRET, 18, 3, NEUTRAL).place(FACT_ROBOT, 2, 20, NEUTRAL)
                .place(TURRET, 15, 15, NEUTRAL);
            m.placeOne(TURRET, 34, 8, ENEMY).placeOne(TURRET, 44, 9, ENEMY).placeOne(TURRET, 37, 1, ENEMY);
            m.vehicle(JIPE, 3, 3);
        }),

        new Level(10, "Coração do Vulcão", TerrainArt.VOLCANO, 64, 48, 1.25f,
            "A batalha final. Rios de lava, pontes e um inimigo com mais tropas. Boa sorte, comandante.", u(TANQUE), u(TANQUE, BAZUCA, BATEDOR), m -> {
            m.sectors(4, 4, "Escória", "Fenda", "Obsidiana", "Base Ciano", "Rio de Fogo", "Caldeira", "Fumarolas", "Basalto",
                "Basalto Sul", "Fumarolas Sul", "Caldeira Sul", "Rio de Fogo Sul", "Base Laranja", "Obsidiana Sul", "Fenda Sul", "Escória Sul");
            m.dirtPatches(12, 111);
            m.line(WATER, 1, 22, 20, 20, 1.6f).line(WATER, 20, 20, 30, 12, 1.6f).line(WATER, 30, 12, 40, 1, 1.6f);
            m.bridge(9, 20, 10, 23).bridge(24, 15, 26, 17).bridge(35, 5, 37, 7);
            m.scatter(ROCK, 12, 1, 2, 112);
            m.blob(TREE, 14, 9, 1.2f, 1).blob(TREE, 44, 18, 1, 1.4f);
            m.flag(0, 7, 7).flag(1, 20, 7).flag(2, 42, 5).flag(4, 7, 17).flag(5, 22, 13).flag(6, 37, 18).flag(7, 54, 19);
            m.homes();
            m.place(FACT_ROBOT, 3, 2, NEUTRAL).place(FACT_VEH, 24, 2, NEUTRAL).place(TURRET, 15, 15, NEUTRAL)
                .place(FACT_VEH, 2, 13, NEUTRAL).place(TURRET, 41, 20, NEUTRAL).place(FACT_ROBOT, 49, 14, NEUTRAL);
            m.placeOne(TURRET, 50, 9, ENEMY);
            m.vehicle(TANQUE, 31, 8).vehicle(JIPE, 12, 11).vehicle(TANQUE, 44, 12);
        }),
    };

    World build(World w, int difficulty) {
        MapData m = new MapData(w);
        builder.build(m);
        m.finish(playerExtra, enemyExtra, difficulty);
        return w;
    }
}
