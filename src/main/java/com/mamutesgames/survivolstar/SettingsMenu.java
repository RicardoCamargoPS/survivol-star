package com.mamutesgames.survivolstar;

import com.mamutesgames.survivolstar.settings.Difficulty;
import com.mamutesgames.survivolstar.settings.GameSettings;
import com.mamutesgames.survivolstar.settings.Resolution;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Configurações: as seções (Jogo, Vídeo, Áudio, Controles, Sobre) numa lista à esquerda e as
 * opções da seção sob o cursor num painel à direita. Enter, seta para a direita ou o mouse
 * levam o foco ao painel; Esc o devolve à lista, e Esc (ou "Voltar") na lista fecha as
 * configurações. Cada alteração é salva e as de vídeo aplicadas na hora.
 */
final class SettingsMenu {

    private static final float LIST_WIDTH = 260f;
    private static final float MARGIN = 60f;
    private static final float PANEL_GAP = 30f;
    private static final float RIGHT_MARGIN = 40f;
    private static final float PANEL_WIDTH = 760f;
    private static final float HEADER = 78f;
    private static final float PADDING = 22f;
    private static final float LINE_HEIGHT = 30f;
    private static final float NOTE_LINE = 24f;
    private static final int VOLUME_STEP = 10;

    /** Seções das configurações. */
    enum Tab {
        GAME("Jogo"),
        VIDEO("Vídeo"),
        AUDIO("Áudio"),
        CONTROLS("Controles"),
        ABOUT("Sobre");

        private final String label;

        Tab(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        String title() {
            return label.toUpperCase(Locale.ROOT);
        }
    }

    private static final Tab[] TABS = Tab.values();

    private final GameSettings settings;
    private final Runnable onChange;
    private final Supplier<List<Resolution>> resolutions;
    private final Runnable close;
    private final MenuList list = new MenuList();

    private Tab tab;
    private MenuList section;
    private List<String> lines = List.of();
    private String note;
    private boolean sectionFocused;
    /** Áreas do último desenho {x, y, w, h}, para o mouse. */
    private float[] listArea = new float[4];
    private float[] panelArea = new float[4];
    private double mouseX;
    private double mouseY;

    /**
     * @param onChange aplica e salva as configurações depois de cada alteração
     * @param resolutions resoluções aceitas pelo monitor, começando pela nativa
     * @param close volta para a tela anterior
     */
    SettingsMenu(GameSettings settings, Runnable onChange, Supplier<List<Resolution>> resolutions, Runnable close) {
        this.settings = settings;
        this.onChange = onChange;
        this.resolutions = resolutions;
        this.close = close;
        for (Tab each : TABS) {
            list.add(MenuItem.action(each.label(), () -> enter(each)));
        }
        list.add(MenuItem.action("Voltar", close));
        show(Tab.GAME);
    }

    /** Volta ao estado de entrada: seção Jogo, foco na lista. */
    void reset() {
        list.select(0);
        show(Tab.GAME);
    }

    Tab tab() {
        return tab;
    }

    boolean sectionFocused() {
        return sectionFocused;
    }

    private void show(Tab next) {
        tab = next;
        section = new MenuList();
        lines = List.of();
        note = null;
        switch (next) {
            case GAME -> {
                section.add(MenuItem.choice("Dificuldade", () -> List.of(Difficulty.values()),
                        settings::difficulty, v -> change(() -> settings.setDifficulty(v)), Difficulty::label));
                note = "A dificuldade vale a partir do próximo jogo.";
            }
            case VIDEO -> {
                section.add(MenuItem.toggle("Tela cheia", settings::fullscreen,
                        v -> change(() -> settings.setFullscreen(v))));
                section.add(MenuItem.choice("Resolução", resolutions, settings::resolution,
                        v -> change(() -> settings.setResolution(v)), Resolution::label));
                section.add(MenuItem.toggle("VSync", settings::vsync, v -> change(() -> settings.setVsync(v))));
                section.add(MenuItem.toggle("Mostrar FPS", settings::showFps,
                        v -> change(() -> settings.setShowFps(v))));
                note = "Fora da tela cheia, a resolução é o tamanho da janela.";
            }
            case AUDIO -> {
                section.add(volume("Volume geral", settings::masterVolume, settings::setMasterVolume));
                section.add(volume("Música", settings::musicVolume, settings::setMusicVolume));
                section.add(volume("Efeitos", settings::effectsVolume, settings::setEffectsVolume));
                note = "O jogo ainda não tem sons: os volumes ficam salvos para quando tiver.";
            }
            case CONTROLS -> lines = List.of(
                    "#Menus",
                    "Setas para cima/baixo  ·  Navegar",
                    "Setas para esquerda/direita  ·  Ajustar opção",
                    "Enter  ·  Confirmar",
                    "Esc  ·  Voltar",
                    "Mouse  ·  Apontar, clicar e rolar para ajustar");
            case ABOUT -> lines = List.of(
                    "#" + GameInfo.NAME.toUpperCase(Locale.ROOT) + "  v" + GameInfo.VERSION,
                    "© " + GameInfo.STUDIO,
                    "",
                    "#Licenças",
                    "LWJGL 3  ·  Lightweight Java Game Library  ·  BSD 3-Clause",
                    "GLFW  ·  zlib/libpng");
            default -> throw new IllegalStateException("Seção desconhecida: " + next);
        }
        sectionFocused = false;
    }

    private MenuItem volume(String label, IntSupplier value, IntConsumer set) {
        return MenuItem.option(label, () -> value.getAsInt() + "%",
                d -> change(() -> set.accept(value.getAsInt() + d * VOLUME_STEP)));
    }

    private void change(Runnable edit) {
        edit.run();
        onChange.run();
    }

    private void enter(Tab chosen) {
        if (chosen != tab) {
            show(chosen);
        }
        sectionFocused = !section.isEmpty();
    }

    // ---------------------------------------------------------------- entrada

    /** @param repeat a tecla está sendo segurada (opções liga/desliga ignoram) */
    void keyPressed(int key, boolean repeat) {
        if (sectionFocused) {
            sectionKey(key, repeat);
        } else {
            listKey(key, repeat);
        }
    }

    private void sectionKey(int key, boolean repeat) {
        MenuItem item = section.selectedItem();
        switch (key) {
            case GLFW_KEY_ESCAPE, GLFW_KEY_BACKSPACE -> {
                if (!repeat) {
                    sectionFocused = false;
                }
            }
            case GLFW_KEY_UP -> section.move(-1);
            case GLFW_KEY_DOWN -> section.move(1);
            case GLFW_KEY_LEFT -> {
                if (!repeat || item.isRepeatable()) {
                    item.adjust(-1);
                }
            }
            case GLFW_KEY_RIGHT -> {
                if (!repeat || item.isRepeatable()) {
                    item.adjust(1);
                }
            }
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                if (!repeat) {
                    item.activate();
                }
            }
            default -> {
            }
        }
    }

    private void listKey(int key, boolean repeat) {
        switch (key) {
            case GLFW_KEY_ESCAPE, GLFW_KEY_BACKSPACE -> {
                if (!repeat) {
                    close.run();
                }
            }
            case GLFW_KEY_UP -> moveList(-1);
            case GLFW_KEY_DOWN -> moveList(1);
            case GLFW_KEY_RIGHT -> {
                if (!repeat && list.selectedIndex() < TABS.length) {
                    enter(tab);
                }
            }
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                if (!repeat) {
                    list.selectedItem().activate();
                }
            }
            default -> {
            }
        }
    }

    private void moveList(int direction) {
        list.move(direction);
        showSelectedTab();
    }

    /** A seção sob o cursor aparece no painel na hora, antes mesmo de entrar nela. */
    private void showSelectedTab() {
        int index = list.selectedIndex();
        if (index < TABS.length && TABS[index] != tab) {
            show(TABS[index]);
        }
    }

    void mouseMoved(double x, double y) {
        mouseX = x;
        mouseY = y;
        int listRow = list.rowAt(x, y);
        if (listRow >= 0) {
            sectionFocused = false;
            list.select(listRow);
            showSelectedTab();
            return;
        }
        int sectionRow = section.rowAt(x, y);
        if (sectionRow >= 0) {
            sectionFocused = true;
            section.select(sectionRow);
        }
    }

    void mouseClicked() {
        int listRow = list.rowAt(mouseX, mouseY);
        if (listRow >= 0) {
            list.select(listRow);
            list.selectedItem().activate();
            return;
        }
        int sectionRow = section.rowAt(mouseX, mouseY);
        if (sectionRow >= 0) {
            sectionFocused = true;
            section.select(sectionRow);
            MenuItem item = section.selectedItem();
            int direction = item.isAdjustable() ? section.adjustDirectionAt(mouseX) : 0;
            if (direction != 0) {
                item.adjust(direction);
            } else {
                item.activate();
            }
        }
    }

    /** @param amount rolagem da roda; positivo = para cima */
    void scrolled(double amount) {
        int sectionRow = section.rowAt(mouseX, mouseY);
        if (sectionRow < 0 || amount == 0) {
            return;
        }
        MenuItem item = section.item(sectionRow);
        if (item.isAdjustable()) {
            sectionFocused = true;
            section.select(sectionRow);
            item.adjust(amount > 0 ? 1 : -1);
        }
    }

    // ---------------------------------------------------------------- desenho

    void draw(GameRenderer r, int width, int height) {
        float s = GameRenderer.scale(width, height);
        float margin = MARGIN * s;
        float listWidth = LIST_WIDTH * s;
        float shadeWidth = margin * 2f + listWidth;
        r.fill(0f, 0f, shadeWidth, height, GameRenderer.SHADE, 0.55f);
        for (int i = 0; i < 8; i++) {
            float step = 12f * s;
            r.fill(shadeWidth + i * step, 0f, step, height, GameRenderer.SHADE, 0.55f * (1f - (i + 1) / 9f));
        }

        r.text("CONFIGURAÇÕES", 30f * s, margin, height * 0.17f, GameRenderer.CYAN, 1f, 0f);
        float listTop = height * 0.28f;
        list.setActive(!sectionFocused);
        list.draw(r, margin, listTop, listWidth, s);
        listArea = new float[] {margin, listTop, listWidth, list.height(s)};

        float left = shadeWidth + PANEL_GAP * s;
        float available = width - left - RIGHT_MARGIN * s;
        float panelWidth = Math.min(PANEL_WIDTH * s, available);
        float panelHeight = panelHeight(s);
        float x = left + (available - panelWidth) * 0.5f;
        float y = Math.max(12f * s, (height - panelHeight) * 0.45f);
        drawPanel(r, x, y, panelWidth, panelHeight, s);
        panelArea = new float[] {x, y, panelWidth, panelHeight};

        // O lado sem o foco fica mais escuro
        float[] dimmed = sectionFocused ? listArea : panelArea;
        r.fill(dimmed[0], dimmed[1], dimmed[2], dimmed[3], GameRenderer.SHADE, 0.5f);
    }

    private float contentHeight(float s) {
        return lines.isEmpty() ? 0f : (lines.size() * LINE_HEIGHT + 12f) * s;
    }

    private float panelHeight(float s) {
        float noteHeight = note == null ? 0f : (NOTE_LINE + 12f) * s;
        return (HEADER + 2 * PADDING) * s + contentHeight(s) + section.height(s) + noteHeight;
    }

    private void drawPanel(GameRenderer r, float x, float y, float w, float h, float s) {
        r.panel(x, y, w, h, s);

        float cx = x + w * 0.5f;
        r.text(tab.title(), 34f * s, cx, y + 44f * s, GameRenderer.CYAN, 1f, 0.5f);
        r.fill(cx - 60f * s, y + 68f * s, 120f * s, 2f * s, GameRenderer.ACCENT, 0.85f);

        float top = y + (HEADER + PADDING) * s;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean heading = line.startsWith("#");
            int color = heading ? GameRenderer.LABEL : GameRenderer.TEXT;
            String text = heading ? line.substring(1).toUpperCase(Locale.ROOT) : line;
            r.text(text, (heading ? 19f : 17f) * s, cx, top + (i + 0.5f) * LINE_HEIGHT * s, color, 1f, 0.5f);
        }

        float menuTop = top + contentHeight(s);
        section.setActive(sectionFocused);
        section.draw(r, x + PADDING * s, menuTop, w - 2 * PADDING * s, s);

        if (note != null) {
            r.text(note, 15f * s, cx, menuTop + section.height(s) + (NOTE_LINE * 0.5f + 8f) * s,
                    GameRenderer.LABEL, 1f, 0.5f);
        }
    }
}
