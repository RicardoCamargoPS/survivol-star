package com.mamutesgames.survivolstar;

import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Pausa: um painel sobre a partida congelada com Continuar, Salvar jogo, Configurações, Menu
 * principal e Sair do jogo. Voltar ao menu e sair perguntam antes (Sim/Não, com "Não"
 * selecionado), pois o progresso não salvo se perde. Esc continua a partida (ou, numa
 * pergunta, responde "Não").
 */
final class PauseMenu {

    private static final float PANEL_WIDTH = 480f;
    private static final float HEADER = 78f;
    private static final float PADDING = 22f;
    private static final float STATUS_HEIGHT = 34f;
    private static final float MESSAGE_HEIGHT = 30f;

    private final Runnable resume;
    private final Supplier<String> status;
    private final MenuList menu = new MenuList();
    private final MenuList confirmMenu = new MenuList();

    /** Pergunta aberta, ou {@code null}. */
    private String question;
    private Runnable onYes;
    /** Resultado da última ação (ex.: "Jogo salvo no slot 2."), ou vazio. */
    private String message = "";
    private double mouseX;
    private double mouseY;

    /**
     * @param status linha de situação da partida (tempo, dificuldade)
     */
    PauseMenu(Runnable resume, Runnable saveGame, Runnable openSettings, Runnable quitToTitle, Runnable exitGame,
              Supplier<String> status) {
        this.resume = resume;
        this.status = status;
        menu.add(MenuItem.action("Continuar", resume));
        menu.add(MenuItem.action("Salvar jogo", saveGame));
        menu.add(MenuItem.action("Configurações", openSettings));
        menu.add(MenuItem.action("Menu principal", () -> ask("Voltar ao menu principal?", quitToTitle)));
        menu.add(MenuItem.action("Sair do jogo", () -> ask("Sair do jogo?", exitGame)));

        confirmMenu.add(MenuItem.action("Sim", () -> {
            Runnable action = onYes;
            closeQuestion();
            action.run();
        }));
        confirmMenu.add(MenuItem.action("Não", this::closeQuestion));
    }

    /** Abre a pausa com "Continuar" selecionado. */
    void open() {
        menu.select(0);
        closeQuestion();
        message = "";
    }

    /** Mostra o resultado de uma ação feita a partir da pausa (ex.: salvar). */
    void showMessage(String text) {
        message = text;
    }

    private void ask(String text, Runnable action) {
        question = text;
        onYes = action;
        confirmMenu.select(1);
        message = "";
    }

    private void closeQuestion() {
        question = null;
        onYes = null;
    }

    private MenuList activeList() {
        return question == null ? menu : confirmMenu;
    }

    // ---------------------------------------------------------------- entrada

    void keyPressed(int key, boolean repeat) {
        MenuList list = activeList();
        switch (key) {
            case GLFW_KEY_ESCAPE, GLFW_KEY_BACKSPACE -> {
                if (!repeat) {
                    if (question != null) {
                        closeQuestion();
                    } else {
                        resume.run();
                    }
                }
            }
            case GLFW_KEY_UP -> list.move(-1);
            case GLFW_KEY_DOWN -> list.move(1);
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                if (!repeat) {
                    list.selectedItem().activate();
                }
            }
            default -> {
            }
        }
    }

    void mouseMoved(double x, double y) {
        mouseX = x;
        mouseY = y;
        MenuList list = activeList();
        int row = list.rowAt(x, y);
        if (row >= 0) {
            list.select(row);
        }
    }

    void mouseClicked() {
        MenuList list = activeList();
        int row = list.rowAt(mouseX, mouseY);
        if (row >= 0) {
            list.select(row);
            list.selectedItem().activate();
        }
    }

    // ---------------------------------------------------------------- desenho

    void draw(GameRenderer r, int width, int height) {
        float s = GameRenderer.scale(width, height);
        r.fill(0f, 0f, width, height, GameRenderer.SHADE, 0.55f);

        MenuList list = activeList();
        boolean asking = question != null;
        float content = asking ? MESSAGE_HEIGHT : STATUS_HEIGHT;
        float footer = !asking && !message.isEmpty() ? MESSAGE_HEIGHT : 0f;
        float panelWidth = Math.min(PANEL_WIDTH * s, width - 40f * s);
        float panelHeight = (HEADER + 2 * PADDING + content + footer) * s + list.height(s);
        float x = (width - panelWidth) * 0.5f;
        float y = Math.max(12f * s, (height - panelHeight) * 0.45f);
        r.panel(x, y, panelWidth, panelHeight, s);

        float cx = x + panelWidth * 0.5f;
        r.text(asking ? question : "PAUSADO", (asking ? 28f : 34f) * s, cx, y + 44f * s,
                asking ? GameRenderer.WARNING : GameRenderer.CYAN, 1f, 0.5f);
        r.fill(cx - 60f * s, y + 68f * s, 120f * s, 2f * s, GameRenderer.ACCENT, 0.85f);

        float top = y + (HEADER + PADDING) * s;
        String line = asking ? "O progresso não salvo será perdido." : status.get();
        r.text(line, 16f * s, cx, top + content * 0.4f * s, GameRenderer.LABEL, 1f, 0.5f);

        float listTop = top + content * s;
        list.draw(r, x + PADDING * s, listTop, panelWidth - 2 * PADDING * s, s);
        if (footer > 0f) {
            r.text(message, 16f * s, cx, listTop + list.height(s) + MESSAGE_HEIGHT * 0.5f * s,
                    GameRenderer.TEXT, 1f, 0.5f);
        }
    }
}
