package com.mamutesgames.survivolstar.save;

import com.mamutesgames.survivolstar.settings.Difficulty;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Properties;

/**
 * Jogo salvo.
 *
 * @param savedAt quando o jogo foi salvo
 * @param summary texto curto do progresso para a lista de saves, ex.: "Setor 3 · Nível 5"
 * @param difficulty dificuldade da partida
 * @param playSeconds tempo total de jogo, em segundos
 */
public record SaveGame(Instant savedAt, String summary, Difficulty difficulty, long playSeconds) {

    static final int FORMAT_VERSION = 1;

    public SaveGame {
        summary = summary == null ? "" : summary.strip();
        playSeconds = Math.max(0L, playSeconds);
    }

    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty("version", Integer.toString(FORMAT_VERSION));
        p.setProperty("savedAt", savedAt.toString());
        p.setProperty("summary", summary);
        p.setProperty("difficulty", difficulty.name());
        p.setProperty("playSeconds", Long.toString(playSeconds));
        return p;
    }

    /**
     * Lê um jogo salvo.
     *
     * @throws IllegalArgumentException se faltar a data ou algum valor for inválido
     */
    public static SaveGame fromProperties(Properties p) {
        String savedAt = p.getProperty("savedAt");
        if (savedAt == null) {
            throw new IllegalArgumentException("Jogo salvo sem data");
        }
        try {
            return new SaveGame(Instant.parse(savedAt.trim()), p.getProperty("summary", ""),
                    Difficulty.valueOf(p.getProperty("difficulty", Difficulty.DEFAULT.name()).trim()),
                    Long.parseLong(p.getProperty("playSeconds", "0").trim()));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: " + savedAt, e);
        }
    }
}
