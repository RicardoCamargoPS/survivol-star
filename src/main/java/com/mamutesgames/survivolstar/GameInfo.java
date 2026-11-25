package com.mamutesgames.survivolstar;

import java.nio.file.Path;

/** Identidade do jogo e local dos dados do usuário. */
public final class GameInfo {

    public static final String NAME = "Survivol Star";
    public static final String VERSION = "1.0.0-SNAPSHOT";
    /** Desenvolvedora, mostrada na abertura e na tela de título. */
    public static final String STUDIO = "Mamutes Games";

    private GameInfo() {
    }

    /** Pasta de configurações e jogos salvos: {@code ~/.survivolstar}. */
    public static Path dataDirectory() {
        return Path.of(System.getProperty("user.home"), ".survivolstar");
    }

    public static Path settingsFile() {
        return dataDirectory().resolve("settings.properties");
    }

    /** Pasta dos jogos salvos, um arquivo por slot. */
    public static Path savesDirectory() {
        return dataDirectory().resolve("saves");
    }
}
