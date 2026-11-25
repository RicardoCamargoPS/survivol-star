package com.mamutesgames.survivolstar;

import com.mamutesgames.survivolstar.save.SaveGame;
import com.mamutesgames.survivolstar.save.SaveStore;
import com.mamutesgames.survivolstar.save.SaveSummary;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Carregar jogo: um painel com a lista dos slots de jogo salvo, um por linha, com a data à
 * direita. Os slots vazios ficam em cinza e são pulados. Abaixo da lista aparece o progresso
 * do slot selecionado. Enter (ou clique) carrega; Delete (ou botão direito) apaga, depois de
 * confirmar; Esc volta.
 */
final class LoadGameMenu {

    private static final float PANEL_WIDTH = 760f;
    private static final float HEADER = 78f;
    private static final float PADDING = 22f;
    private static final float INFO_LINE = 28f;
    private static final int INFO_LINES = 2;

    private final SaveStore store;
    private final BiConsumer<Integer, SaveGame> onLoad;
    private final Runnable close;
    private final MenuList list = new MenuList();
    private List<SaveSummary> slots = List.of();
    /** Slot esperando a confirmação para ser apagado, ou 0. */
    private int pendingDelete;
    /** Resultado da última ação (ex.: "Jogo do slot 2 apagado."), ou vazio. */
    private String message = "";
    private double mouseX;
    private double mouseY;

    /**
     * @param onLoad recebe o número do slot e o jogo lido dele
     * @param close volta para a tela anterior
     */
    LoadGameMenu(SaveStore store, BiConsumer<Integer, SaveGame> onLoad, Runnable close) {
        this.store = store;
        this.onLoad = onLoad;
        this.close = close;
        for (int i = 0; i < SaveStore.SLOTS; i++) {
            int index = i;
            list.add(MenuItem.detail("Slot " + (i + 1),
                    () -> filled(index) ? slots.get(index).date() : "Vazio",
                    () -> filled(index), () -> load(index + 1)));
        }
        list.add(MenuItem.action("Voltar", close));
    }

    /** Relê os jogos salvos e seleciona o primeiro slot com jogo. Chamado ao abrir a tela. */
    void open() {
        refresh();
        pendingDelete = 0;
        message = "";
        list.select(0);
        list.ensureSelectable();
    }

    private void refresh() {
        slots = store.summaries();
    }

    private boolean filled(int index) {
        return index < slots.size() && slots.get(index).present();
    }

    private boolean hasAnySave() {
        return slots.stream().anyMatch(SaveSummary::present);
    }

    /** Índice do slot selecionado, ou -1 se a seleção está em "Voltar". */
    private int selectedSlot() {
        int index = list.selectedIndex();
        return index < SaveStore.SLOTS ? index : -1;
    }

    private void load(int slot) {
        store.load(slot).ifPresentOrElse(game -> onLoad.accept(slot, game), () -> {
            // O arquivo sumiu ou corrompeu desde que a lista foi lida
            refresh();
            message = "Não foi possível ler o jogo do slot " + slot + ".";
        });
    }

    private void askDelete() {
        int index = selectedSlot();
        if (index >= 0 && filled(index)) {
            pendingDelete = index + 1;
            message = "";
        }
    }

    private void confirmDelete() {
        int slot = pendingDelete;
        pendingDelete = 0;
        try {
            store.delete(slot);
            message = "Jogo do slot " + slot + " apagado.";
        } catch (IOException e) {
            message = "Não foi possível apagar o slot " + slot + ": " + e.getMessage();
        }
        refresh();
        list.ensureSelectable();
    }

    // ---------------------------------------------------------------- entrada

    void keyPressed(int key, boolean repeat) {
        if (pendingDelete > 0) {
            if (!repeat) {
                if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                    confirmDelete();
                } else {
                    pendingDelete = 0;
                }
            }
            return;
        }
        switch (key) {
            case GLFW_KEY_ESCAPE, GLFW_KEY_BACKSPACE -> {
                if (!repeat) {
                    close.run();
                }
            }
            case GLFW_KEY_UP -> moveSelection(-1);
            case GLFW_KEY_DOWN -> moveSelection(1);
            case GLFW_KEY_DELETE -> {
                if (!repeat) {
                    askDelete();
                }
            }
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                if (!repeat) {
                    message = "";
                    list.selectedItem().activate();
                }
            }
            default -> {
            }
        }
    }

    private void moveSelection(int direction) {
        list.move(direction);
        message = "";
    }

    void mouseMoved(double x, double y) {
        mouseX = x;
        mouseY = y;
        if (pendingDelete > 0) {
            return;
        }
        int row = list.rowAt(x, y);
        if (row >= 0 && row != list.selectedIndex()) {
            list.select(row);
            message = "";
        }
    }

    void mouseClicked() {
        if (pendingDelete > 0) {
            pendingDelete = 0;
            return;
        }
        int row = list.rowAt(mouseX, mouseY);
        if (row >= 0) {
            list.select(row);
            message = "";
            list.selectedItem().activate();
        }
    }

    /** Botão direito sobre um slot com jogo pede para apagá-lo. */
    void mouseRightClicked() {
        if (pendingDelete > 0) {
            pendingDelete = 0;
            return;
        }
        int row = list.rowAt(mouseX, mouseY);
        if (row >= 0) {
            list.select(row);
            askDelete();
        }
    }

    // ---------------------------------------------------------------- desenho

    void draw(GameRenderer r, int width, int height) {
        float s = GameRenderer.scale(width, height);
        r.fill(0f, 0f, width, height, GameRenderer.SHADE, 0.45f);

        float panelWidth = Math.min(PANEL_WIDTH * s, width - 40f * s);
        float panelHeight = (HEADER + 2 * PADDING + INFO_LINES * INFO_LINE + 10f) * s + list.height(s);
        float x = (width - panelWidth) * 0.5f;
        float y = Math.max(12f * s, (height - panelHeight) * 0.45f);
        r.panel(x, y, panelWidth, panelHeight, s);

        float cx = x + panelWidth * 0.5f;
        r.text("CARREGAR JOGO", 34f * s, cx, y + 44f * s, GameRenderer.CYAN, 1f, 0.5f);
        r.fill(cx - 60f * s, y + 68f * s, 120f * s, 2f * s, GameRenderer.ACCENT, 0.85f);

        float top = y + (HEADER + PADDING) * s;
        list.setActive(pendingDelete == 0);
        list.draw(r, x + PADDING * s, top, panelWidth - 2 * PADDING * s, s);

        float infoTop = top + list.height(s) + 10f * s;
        float line1 = infoTop + INFO_LINE * 0.5f * s;
        float line2 = line1 + INFO_LINE * s;
        if (pendingDelete > 0) {
            r.text("Apagar o jogo do slot " + pendingDelete + "?", 18f * s, cx, line1,
                    GameRenderer.WARNING, 1f, 0.5f);
            r.text("Enter confirma  ·  Esc cancela", 15f * s, cx, line2, GameRenderer.LABEL, 1f, 0.5f);
            return;
        }
        r.text(infoText(), 17f * s, cx, line1, GameRenderer.TEXT, 1f, 0.5f);
        r.text(hasAnySave() ? "Enter carrega  ·  Delete ou botão direito apaga  ·  Esc volta" : "Esc volta",
                15f * s, cx, line2, GameRenderer.LABEL, 1f, 0.5f);
    }

    /** Primeira linha abaixo da lista: a mensagem da última ação ou o progresso do slot. */
    private String infoText() {
        if (!message.isEmpty()) {
            return message;
        }
        if (!hasAnySave()) {
            return "Nenhum jogo salvo ainda.";
        }
        int index = selectedSlot();
        return index >= 0 && filled(index) ? slots.get(index).detailLine() : "";
    }
}
