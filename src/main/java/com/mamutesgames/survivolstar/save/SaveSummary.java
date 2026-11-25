package com.mamutesgames.survivolstar.save;

import java.util.List;

/**
 * Resumo de um slot para exibir em menus.
 *
 * @param date quando o jogo foi salvo (vazio se o slot está livre)
 * @param details o progresso do jogo salvo, uma informação por item (dificuldade, tempo de jogo,
 *                progresso); vazio se o slot está livre
 */
public record SaveSummary(int slot, boolean present, String date, List<String> details) {

    /** Separador entre as informações do resumo. */
    public static final String SEPARATOR = "  ·  ";

    public SaveSummary {
        details = List.copyOf(details);
    }

    static SaveSummary empty(int slot) {
        return new SaveSummary(slot, false, "", List.of());
    }

    /** Progresso numa linha só, ou vazio. */
    public String detailLine() {
        return String.join(SEPARATOR, details);
    }
}
