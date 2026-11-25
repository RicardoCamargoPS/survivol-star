package com.mamutesgames.survivolstar.core;

import com.mamutesgames.survivolstar.GameInfo;
import com.mamutesgames.survivolstar.game.GameSession;
import com.mamutesgames.survivolstar.render.GameRenderer;
import com.mamutesgames.survivolstar.save.SaveGame;
import com.mamutesgames.survivolstar.save.SaveStore;
import com.mamutesgames.survivolstar.settings.GameSettings;
import com.mamutesgames.survivolstar.settings.Resolution;
import com.mamutesgames.survivolstar.settings.SettingsStore;
import com.mamutesgames.survivolstar.ui.PauseMenu;
import com.mamutesgames.survivolstar.ui.SaveSlotsMenu;
import com.mamutesgames.survivolstar.ui.SettingsMenu;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.lwjgl.glfw.GLFWVidMode;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Game implements AutoCloseable {

    private static final double SPLASH_DURATION = 3.2;
    /** Ignora teclas nos primeiros instantes: a tecla que abriu o jogo não pula a abertura. */
    private static final double SPLASH_SKIP_DELAY = 0.3;
    /** Menor largura oferecida na lista de resoluções. */
    private static final int MIN_RESOLUTION_WIDTH = 800;
    /** Espaço da borda e da barra de título, para a janela caber na área de trabalho. */
    private static final int WINDOW_DECORATION_MARGIN = 80;
    private static final String[] MENU_OPTIONS = {
            "Novo jogo",
            "Configurações",
            "Carregar jogo",
            "Sair"
    };

    private enum State {
        SPLASH,
        TITLE,
        SETTINGS,
        SAVE_SLOTS,
        PLAYING,
        PAUSED
    }

    private final long window;
    private final GameRenderer renderer;
    private final SettingsStore settingsStore = new SettingsStore(GameInfo.settingsFile());
    private final GameSettings settings;
    private final List<Resolution> fullscreenResolutions;
    private final List<Resolution> windowedResolutions;
    private final SettingsMenu settingsMenu;
    private final SaveSlotsMenu saveSlotsMenu;
    private final PauseMenu pauseMenu;
    private State state = State.SPLASH;
    /** Para onde voltam as configurações e os slots: o título ou a pausa. */
    private State returnState = State.TITLE;
    /** Partida em andamento (jogando ou pausada), ou {@code null}. */
    private GameSession session;
    private int width;
    private int height;
    private int selectedOption;
    private double splashElapsed;
    private double mouseX;
    private double mouseY;
    private String statusMessage = "";

    private Boolean appliedFullscreen;
    private Resolution appliedResolution;

    private int frames;
    private double fpsTimer;
    private int fps;

    public Game(long window) {
        this.window = window;
        renderer = new GameRenderer();
        settings = settingsStore.load();
        fullscreenResolutions = findResolutions(false);
        windowedResolutions = findResolutions(true);
        settingsMenu = new SettingsMenu(settings, this::applySettings, this::resolutions,
                () -> state = returnState);
        saveSlotsMenu = new SaveSlotsMenu(new SaveStore(GameInfo.savesDirectory()), this::gameLoaded,
                () -> session.snapshot(), this::gameSaved, () -> state = returnState);
        pauseMenu = new PauseMenu(this::resume, this::openSaveSlots, this::openSettingsFromPause,
                this::quitToTitle, () -> glfwSetWindowShouldClose(window, true), this::pauseStatus);
        applyVideo();
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void update(float delta) {
        if (state == State.SPLASH) {
            splashElapsed += delta;
            if (splashElapsed >= SPLASH_DURATION) {
                state = State.TITLE;
            }
        } else if (state == State.PLAYING) {
            session.update(delta);
        }
        frames++;
        fpsTimer += delta;
        if (fpsTimer >= 1.0) {
            fps = frames;
            frames = 0;
            fpsTimer -= 1.0;
        }
    }

    /** @param repeat a tecla está sendo segurada */
    public void keyPressed(int key, boolean repeat) {
        switch (state) {
            case SPLASH -> {
                if (!repeat) {
                    skipSplash();
                }
            }
            case TITLE -> titleKey(key, repeat);
            case SETTINGS -> settingsMenu.keyPressed(key, repeat);
            case SAVE_SLOTS -> saveSlotsMenu.keyPressed(key, repeat);
            case PLAYING -> {
                if (key == GLFW_KEY_ESCAPE && !repeat) {
                    pause();
                }
            }
            case PAUSED -> pauseMenu.keyPressed(key, repeat);
            default -> throw new IllegalStateException("Estado desconhecido: " + state);
        }
    }

    private void titleKey(int key, boolean repeat) {
        if (key == GLFW_KEY_ESCAPE && !repeat) {
            glfwSetWindowShouldClose(window, true);
        } else if (key == GLFW_KEY_UP) {
            selectedOption = (selectedOption + MENU_OPTIONS.length - 1) % MENU_OPTIONS.length;
            statusMessage = "";
        } else if (key == GLFW_KEY_DOWN) {
            selectedOption = (selectedOption + 1) % MENU_OPTIONS.length;
            statusMessage = "";
        } else if ((key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) && !repeat) {
            activateSelection();
        }
    }

    private void skipSplash() {
        if (splashElapsed > SPLASH_SKIP_DELAY) {
            state = State.TITLE;
        }
    }

    public void mouseMoved(double x, double y) {
        mouseX = x;
        mouseY = y;
        switch (state) {
            case TITLE -> {
                int hoveredOption = optionAt(x, y);
                if (hoveredOption >= 0) {
                    selectedOption = hoveredOption;
                }
            }
            case SETTINGS -> settingsMenu.mouseMoved(x, y);
            case SAVE_SLOTS -> saveSlotsMenu.mouseMoved(x, y);
            case PAUSED -> pauseMenu.mouseMoved(x, y);
            default -> {
            }
        }
    }

    public void mouseRightClicked() {
        if (state == State.SAVE_SLOTS) {
            saveSlotsMenu.mouseRightClicked();
        }
    }

    public void mouseClicked() {
        switch (state) {
            case SPLASH -> skipSplash();
            case SETTINGS -> settingsMenu.mouseClicked();
            case SAVE_SLOTS -> saveSlotsMenu.mouseClicked();
            case PAUSED -> pauseMenu.mouseClicked();
            case TITLE -> {
                int clickedOption = optionAt(mouseX, mouseY);
                if (clickedOption >= 0) {
                    selectedOption = clickedOption;
                    activateSelection();
                }
            }
            default -> {
            }
        }
    }

    /** @param amount rolagem vertical da roda; positivo = para cima */
    public void scrolled(double amount) {
        if (state == State.SETTINGS) {
            settingsMenu.scrolled(amount);
        }
    }

    /** A janela perdeu o foco: uma partida em andamento pausa sozinha. */
    public void focusLost() {
        if (state == State.PLAYING) {
            pause();
        }
    }

    public void render(float time) {
        renderer.beginFrame(width, height);
        // Com uma partida aberta, a cena (congelada na pausa) fica atrás dos menus
        if (session != null) {
            renderer.drawScene(width, height, time, session.angle());
        } else if (state != State.SPLASH && state != State.TITLE) {
            renderer.drawBackground(width, height);
        }
        switch (state) {
            case SPLASH -> renderer.drawSplash(width, height, splashElapsed);
            case TITLE -> renderer.drawTitle(width, height, time, MENU_OPTIONS, selectedOption, statusMessage);
            case SETTINGS -> settingsMenu.draw(renderer, width, height);
            case SAVE_SLOTS -> saveSlotsMenu.draw(renderer, width, height);
            case PLAYING -> drawHud();
            case PAUSED -> pauseMenu.draw(renderer, width, height);
            default -> throw new IllegalStateException("Estado desconhecido: " + state);
        }
        if (settings.showFps()) {
            renderer.drawFps(fps, width, height);
        }
    }

    /** Tempo de jogo e dificuldade no canto, e como pausar embaixo. */
    private void drawHud() {
        float s = GameRenderer.scale(width, height);
        renderer.text(session.playTimeText(), 26f * s, 24f * s, 30f * s, GameRenderer.TEXT, 1f, 0f);
        renderer.text(session.difficulty().label().toUpperCase(java.util.Locale.ROOT), 14f * s, 26f * s, 58f * s,
                GameRenderer.LABEL, 1f, 0f);
        renderer.text("Esc  ·  Pausar", 15f * s, width * 0.5f, height - 24f * s, GameRenderer.LABEL, 0.8f, 0.5f);
    }

    private int optionAt(double x, double y) {
        if (x < width * 0.25 || x > width * 0.75) {
            return -1;
        }
        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            double centerY = height * 0.48 + i * height * 0.09;
            if (Math.abs(y - centerY) <= 28) {
                return i;
            }
        }
        return -1;
    }

    private void activateSelection() {
        statusMessage = "";
        returnState = State.TITLE;
        switch (selectedOption) {
            case 0 -> startSession(GameSession.start(settings.difficulty()));
            case 1 -> {
                settingsMenu.reset();
                state = State.SETTINGS;
            }
            case 2 -> {
                saveSlotsMenu.open(SaveSlotsMenu.Mode.LOAD);
                state = State.SAVE_SLOTS;
            }
            case 3 -> glfwSetWindowShouldClose(window, true);
            default -> throw new IllegalStateException("Opção de menu desconhecida: " + selectedOption);
        }
    }

    // ---------------------------------------------------------------- partida

    private void startSession(GameSession newSession) {
        session = newSession;
        state = State.PLAYING;
    }

    private void gameLoaded(int slot, SaveGame game) {
        startSession(GameSession.resume(game));
    }

    private void pause() {
        pauseMenu.open();
        state = State.PAUSED;
    }

    private void resume() {
        state = State.PLAYING;
    }

    private void openSaveSlots() {
        returnState = State.PAUSED;
        saveSlotsMenu.open(SaveSlotsMenu.Mode.SAVE);
        state = State.SAVE_SLOTS;
    }

    private void gameSaved(int slot) {
        pauseMenu.showMessage("Jogo salvo no slot " + slot + ".");
        state = State.PAUSED;
    }

    private void openSettingsFromPause() {
        returnState = State.PAUSED;
        settingsMenu.reset();
        state = State.SETTINGS;
    }

    private void quitToTitle() {
        session = null;
        returnState = State.TITLE;
        selectedOption = 0;
        statusMessage = "";
        state = State.TITLE;
    }

    private String pauseStatus() {
        return "Tempo de jogo " + session.playTimeText() + "  ·  " + session.difficulty().label();
    }

    // ---------------------------------------------------------------- configurações

    /** Aplica as opções de vídeo e grava as configurações. */
    private void applySettings() {
        applyVideo();
        try {
            settingsStore.save(settings);
        } catch (UncheckedIOException e) {
            System.err.println("Não foi possível salvar as configurações: " + e.getCause().getMessage());
        }
    }

    /**
     * Resoluções oferecidas nas configurações: em tela cheia, todos os modos do monitor; em
     * janela, só os tamanhos que cabem na área de trabalho (os maiores seriam cortados e
     * ficariam todos iguais).
     */
    private List<Resolution> resolutions() {
        return settings.fullscreen() ? fullscreenResolutions : windowedResolutions;
    }

    private void applyVideo() {
        glfwSwapInterval(settings.vsync() ? 1 : 0);
        boolean fullscreen = settings.fullscreen();
        if (!fullscreen && !windowedResolutions.contains(settings.resolution())) {
            // Saiu da tela cheia com um tamanho que não cabe em janela: usa o maior que cabe
            settings.setResolution(windowedResolutions.getLast());
        }
        Resolution resolution = settings.resolution();
        if (appliedFullscreen != null && appliedFullscreen == fullscreen && resolution.equals(appliedResolution)) {
            return;
        }
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode desktop = monitor == NULL ? null : glfwGetVideoMode(monitor);
        if (desktop == null) {
            return;
        }
        int targetWidth = resolution.isNative() ? desktop.width() : resolution.width();
        int targetHeight = resolution.isNative() ? desktop.height() : resolution.height();
        if (fullscreen) {
            glfwSetWindowMonitor(window, monitor, 0, 0, targetWidth, targetHeight,
                    resolution.isNative() ? desktop.refreshRate() : GLFW_DONT_CARE);
        } else {
            // A janela não passa do tamanho da área de trabalho
            targetWidth = Math.min(targetWidth, desktop.width());
            targetHeight = Math.min(targetHeight, desktop.height() - WINDOW_DECORATION_MARGIN);
            glfwSetWindowMonitor(window, NULL, (desktop.width() - targetWidth) / 2,
                    (desktop.height() - targetHeight) / 2, targetWidth, targetHeight, GLFW_DONT_CARE);
        }
        // Trocar de modo pode recriar a troca de buffers: reaplica o VSync
        glfwSwapInterval(settings.vsync() ? 1 : 0);
        appliedFullscreen = fullscreen;
        appliedResolution = resolution;
    }

    /**
     * Resoluções que o monitor principal aceita (sem repetir taxas de atualização), da menor
     * para a maior, começando pela nativa. Inclui 1280 × 720, o tamanho padrão da janela.
     *
     * @param windowed só os tamanhos que cabem numa janela na área de trabalho
     */
    private static List<Resolution> findResolutions(boolean windowed) {
        TreeSet<Resolution> sizes = new TreeSet<>();
        sizes.add(Resolution.HD);
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode desktop = monitor == NULL ? null : glfwGetVideoMode(monitor);
        GLFWVidMode.Buffer modes = monitor == NULL ? null : glfwGetVideoModes(monitor);
        if (modes != null) {
            for (int i = 0; i < modes.limit(); i++) {
                GLFWVidMode mode = modes.get(i);
                if (mode.width() >= MIN_RESOLUTION_WIDTH) {
                    sizes.add(new Resolution(mode.width(), mode.height()));
                }
            }
        }
        if (windowed && desktop != null) {
            int maxHeight = desktop.height() - WINDOW_DECORATION_MARGIN;
            sizes.removeIf(size -> size.width() > desktop.width() || size.height() > maxHeight);
        }
        List<Resolution> result = new ArrayList<>();
        result.add(Resolution.NATIVE);
        result.addAll(sizes);
        return List.copyOf(result);
    }

    @Override
    public void close() {
        renderer.close();
    }
}
