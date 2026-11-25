package com.mamutesgames.survivolstar.game;

import com.mamutesgames.survivolstar.save.SaveGame;
import com.mamutesgames.survivolstar.settings.Difficulty;
import java.time.Instant;

/**
 * Uma partida em andamento. Por enquanto a cena é só um cubo girando; a velocidade do giro
 * depende da dificuldade. O tempo de jogo só corre enquanto a partida não está pausada.
 */
public final class GameSession {

    private final Difficulty difficulty;
    private double playSeconds;
    /** Ângulo do cubo, em graus. */
    private float angle;

    private GameSession(Difficulty difficulty, double playSeconds) {
        this.difficulty = difficulty;
        this.playSeconds = playSeconds;
        // Continua de onde o giro estaria, para um jogo carregado não recomeçar do zero
        this.angle = (float) (playSeconds * spinSpeed(difficulty) % 360.0);
    }

    public static GameSession start(Difficulty difficulty) {
        return new GameSession(difficulty, 0.0);
    }

    public static GameSession resume(SaveGame game) {
        return new GameSession(game.difficulty(), game.playSeconds());
    }

    /** Graus por segundo. */
    private static float spinSpeed(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 35f;
            case NORMAL -> 60f;
            case HARD -> 100f;
        };
    }

    public void update(float delta) {
        playSeconds += delta;
        angle = (angle + spinSpeed(difficulty) * delta) % 360f;
    }

    public float angle() {
        return angle;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public double playSeconds() {
        return playSeconds;
    }

    /** Tempo de jogo como "mm:ss" ou "h:mm:ss". */
    public String playTimeText() {
        long total = (long) playSeconds;
        long hours = total / 3600;
        long minutes = total / 60 % 60;
        long seconds = total % 60;
        return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);
    }

    /** O estado atual, para gravar num slot. */
    public SaveGame snapshot() {
        return new SaveGame(Instant.now(), "Protótipo  ·  Cubo", difficulty, (long) playSeconds);
    }
}
