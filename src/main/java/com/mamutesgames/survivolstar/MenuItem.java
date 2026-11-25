package com.mamutesgames.survivolstar;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Item de um {@link MenuList}: uma ação simples ou uma opção com valor ajustável. */
final class MenuItem {

    private final String label;
    private final Supplier<String> value;
    private final Runnable activate;
    private final IntConsumer adjust;
    private final boolean repeatable;

    private MenuItem(String label, Supplier<String> value, Runnable activate, IntConsumer adjust,
                     boolean repeatable) {
        this.label = label;
        this.value = value;
        this.activate = activate;
        this.adjust = adjust;
        this.repeatable = repeatable;
    }

    /** Executa {@code action} ao confirmar. */
    static MenuItem action(String label, Runnable action) {
        return new MenuItem(label, null, action, null, false);
    }

    /**
     * Opção ajustada com esquerda/direita; confirmar avança para o próximo valor.
     *
     * @param adjust recebe -1 ou +1
     */
    static MenuItem option(String label, Supplier<String> value, IntConsumer adjust) {
        return new MenuItem(label, value, () -> adjust.accept(1), adjust, true);
    }

    /** Opção que percorre uma lista de valores (com volta ao início). */
    static <T> MenuItem choice(String label, Supplier<List<T>> values, Supplier<T> current, Consumer<T> set,
                               Function<T, String> format) {
        return option(label, () -> format.apply(current.get()), direction -> {
            List<T> list = values.get();
            int index = list.indexOf(current.get());
            set.accept(list.get(Math.floorMod(index + direction, list.size())));
        });
    }

    /** Opção liga/desliga; não repete ao segurar a tecla (a tela cheia piscaria). */
    static MenuItem toggle(String label, BooleanSupplier current, Consumer<Boolean> set) {
        IntConsumer flip = direction -> set.accept(!current.getAsBoolean());
        return new MenuItem(label, () -> current.getAsBoolean() ? "Sim" : "Não",
                () -> flip.accept(1), flip, false);
    }

    String label() {
        return label;
    }

    /** Valor exibido à direita, ou {@code null} para itens de ação. */
    String value() {
        return value == null ? null : value.get();
    }

    boolean isAdjustable() {
        return adjust != null;
    }

    /** Aceita ajustes repetidos enquanto a tecla é segurada. */
    boolean isRepeatable() {
        return repeatable;
    }

    void activate() {
        activate.run();
    }

    void adjust(int direction) {
        if (adjust != null) {
            adjust.accept(direction);
        }
    }
}
