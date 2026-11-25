package com.mamutesgames.survivolstar.settings;

/** Nível de dificuldade do jogo. */
public enum Difficulty {
    EASY("Fácil"),
    NORMAL("Normal"),
    HARD("Difícil");

    public static final Difficulty DEFAULT = NORMAL;

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
