package com.mamutesgames.survivolstar;

import com.mamutesgames.survivolstar.save.SaveGame;
import com.mamutesgames.survivolstar.save.SaveStore;
import com.mamutesgames.survivolstar.save.SaveSummary;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Slots de jogo salvo: um painel com a lista dos slots, um por linha, com a data à direita.
 * Abaixo da lista aparece o progresso do slot selecionado.
 * <ul>
 * <li>Para carregar, os slots vazios ficam em cinza e são pulados; Enter (ou clique) carrega.</li>
 * <li>Para salvar, todos ficam disponíveis; um slot ocupado pede confirmação antes de ser
 * sobrescrito.</li>
 * </ul>
 * Nos dois modos Delete (ou botão direito) apaga um slot, depois de confirmar, e Esc volta.
 */
final class SaveSlotsMenu {

    enum Mode {
        LOAD,
        SAVE
    }

    /** Ação esperando confirmação. */
    private enum Pending {
        NONE,
        DELETE,
        OVERWRITE
    }

    private static final float PANEL_WIDTH = 760f;
    private static final float HEADER = 78f;
    private static final float PADDING = 22f;
    private static final float INFO_LINE = 28f;
    private static final int INFO_LINES = 2;

    private final SaveStore store;
    private final BiConsumer<Integer, SaveGame> onLoad;
    private final Supplier<SaveGame> snapshot;
    private final IntConsumer onSaved;
    private final Runnable close;
    private final MenuList list = new MenuList();
    private Mode mode = Mode.LOAD;
    private List<SaveSummary> slots = List.of();
    private Pending pending = Pending.NONE;
    /** Slot da ação esperando confirmação. */
    private int pendingSlot;
    /** Resultado da última ação (ex.: "Jogo do slot 2 apagado."), ou vazio. */
    private String message = "";
    private double mouseX;
    private double mouseY;

    /**
     * @param onLoad recebe o número do slot e o jogo lido dele
     * @param snapshot o jogo atual, para salvar
     * @param onSaved recebe o número do slot depois de salvar
     * @param close volta para a tela anterior
     */
    SaveSlotsMenu(SaveStore store, BiConsumer<Integer, SaveGame> onLoad, Supplier<SaveGame> snapshot,
                  IntConsumer onSaved, Runnable close) {
        this.store = store;
        this.onLoad = onLoad;
        this.snapshot = snapshot;
        this.onSaved = onSaved;
        this.close = close;
        for (int i = 0; i < SaveStore.SLOTS; i++) {
            int index = i;
            list.add(MenuItem.detail("Slot " + (i + 1), () -> slotDetail(index),
                    () -> mode == Mode.SAVE || filled(index), () -> choose(index + 1)));
        }
        list.add(MenuItem.action("Voltar", close));
    }

    /**
     * Relê os jogos salvos e abre no modo pedido. Para carregar, seleciona o primeiro slot com
     * jogo; para salvar, o primeiro livre.
     */
    void open(Mode mode) {
        this.mode = mode;
        refresh();
        pending = Pending.NONE;
        message = "";
        list.select(initialFocus());
        list.ensureSelectable();
    }

    private int initialFocus() {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).present() == (mode == Mode.LOAD)) {
                return i;
            }
        }
        return mode == Mode.LOAD ? SaveStore.SLOTS : 0;
    }

    private void refresh() {
        slots = store.summaries();
    }

    private String slotDetail(int index) {
        if (filled(index)) {
            return slots.get(index).date();
        }
        return mode == Mode.LOAD ? "Vazio" : "Livre";
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

    private void choose(int slot) {
        if (mode == Mode.LOAD) {
            load(slot);
        } else if (filled(slot - 1)) {
            pending = Pending.OVERWRITE;
            pendingSlot = slot;
        } else {
            save(slot);
        }
    }

    private void load(int slot) {
        store.load(slot).ifPresentOrElse(game -> onLoad.accept(slot, game), () -> {
            // O arquivo sumiu ou corrompeu desde que a lista foi lida
            refresh();
            message = "Não foi possível ler o jogo do slot " + slot + ".";
        });
    }

    private void save(int slot) {
        try {
            store.save(slot, snapshot.get());
            onSaved.accept(slot);
        } catch (IOException e) {
            message = "Não foi possível salvar no slot " + slot + ": " + e.getMessage();
        }
    }

    private void askDelete() {
        int index = selectedSlot();
        if (index >= 0 && filled(index)) {
            pending = Pending.DELETE;
            pendingSlot = index + 1;
            message = "";
        }
    }

    private void confirm() {
        Pending action = pending;
        pending = Pending.NONE;
        if (action == Pending.OVERWRITE) {
            save(pendingSlot);
            return;
        }
        try {
            store.delete(pendingSlot);
            message = "Jogo do slot " + pendingSlot + " apagado.";
        } catch (IOException e) {
            message = "Não foi possível apagar o slot " + pendingSlot + ": " + e.getMessage();
        }
        refresh();
        list.ensureSelectable();
    }

    // ---------------------------------------------------------------- entrada

    void keyPressed(int key, boolean repeat) {
        if (pending != Pending.NONE) {
            if (!repeat) {
                if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                    confirm();
                } else {
                    pending = Pending.NONE;
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
        if (pending != Pending.NONE) {
            return;
        }
        int row = list.rowAt(x, y);
        if (row >= 0 && row != list.selectedIndex()) {
            list.select(row);
            message = "";
        }
    }

    void mouseClicked() {
        if (pending != Pending.NONE) {
            pending = Pending.NONE;
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
        if (pending != Pending.NONE) {
            pending = Pending.NONE;
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
        r.text(mode == Mode.LOAD ? "CARREGAR JOGO" : "SALVAR JOGO", 34f * s, cx, y + 44f * s,
                GameRenderer.CYAN, 1f, 0.5f);
        r.fill(cx - 60f * s, y + 68f * s, 120f * s, 2f * s, GameRenderer.ACCENT, 0.85f);

        float top = y + (HEADER + PADDING) * s;
        list.setActive(pending == Pending.NONE);
        list.draw(r, x + PADDING * s, top, panelWidth - 2 * PADDING * s, s);

        float infoTop = top + list.height(s) + 10f * s;
        float line1 = infoTop + INFO_LINE * 0.5f * s;
        float line2 = line1 + INFO_LINE * s;
        if (pending != Pending.NONE) {
            String question = pending == Pending.DELETE
                    ? "Apagar o jogo do slot " + pendingSlot + "?"
                    : "Sobrescrever o jogo do slot " + pendingSlot + "?";
            r.text(question, 18f * s, cx, line1, GameRenderer.WARNING, 1f, 0.5f);
            r.text("Enter confirma  ·  Esc cancela", 15f * s, cx, line2, GameRenderer.LABEL, 1f, 0.5f);
            return;
        }
        r.text(infoText(), 17f * s, cx, line1, GameRenderer.TEXT, 1f, 0.5f);
        r.text(hint(), 15f * s, cx, line2, GameRenderer.LABEL, 1f, 0.5f);
    }

    private String hint() {
        String delete = hasAnySave() ? "  ·  Delete ou botão direito apaga" : "";
        return mode == Mode.LOAD
                ? (hasAnySave() ? "Enter carrega" + delete + "  ·  Esc volta" : "Esc volta")
                : "Enter salva" + delete + "  ·  Esc volta";
    }

    /** Primeira linha abaixo da lista: a mensagem da última ação ou o progresso do slot. */
    private String infoText() {
        if (!message.isEmpty()) {
            return message;
        }
        if (mode == Mode.LOAD && !hasAnySave()) {
            return "Nenhum jogo salvo ainda.";
        }
        int index = selectedSlot();
        return index >= 0 && filled(index) ? slots.get(index).detailLine() : "";
    }
}
