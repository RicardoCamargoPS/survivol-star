package com.mamutesgames.survivolstar;

import com.mamutesgames.survivolstar.settings.GameSettings;
import com.mamutesgames.survivolstar.settings.Resolution;
import com.mamutesgames.survivolstar.settings.SettingsStore;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.lwjgl.glfw.GLFWVidMode;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

final class Game implements AutoCloseable {

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
        SETTINGS
    }

    private final long window;
    private final GameRenderer renderer;
    private final SettingsStore settingsStore = new SettingsStore(GameInfo.settingsFile());
    private final GameSettings settings;
    private final List<Resolution> resolutions;
    private final SettingsMenu settingsMenu;
    private State state = State.SPLASH;
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

    Game(long window) {
        this.window = window;
        renderer = new GameRenderer();
        settings = settingsStore.load();
        resolutions = findResolutions();
        settingsMenu = new SettingsMenu(settings, this::applySettings, () -> resolutions,
                () -> state = State.TITLE);
        applyVideo();
    }

    void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    void update(float delta) {
        if (state == State.SPLASH) {
            splashElapsed += delta;
            if (splashElapsed >= SPLASH_DURATION) {
                state = State.TITLE;
            }
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
    void keyPressed(int key, boolean repeat) {
        switch (state) {
            case SPLASH -> {
                if (!repeat) {
                    skipSplash();
                }
            }
            case SETTINGS -> settingsMenu.keyPressed(key, repeat);
            case TITLE -> titleKey(key, repeat);
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

    void mouseMoved(double x, double y) {
        mouseX = x;
        mouseY = y;
        if (state == State.TITLE) {
            int hoveredOption = optionAt(x, y);
            if (hoveredOption >= 0) {
                selectedOption = hoveredOption;
            }
        } else if (state == State.SETTINGS) {
            settingsMenu.mouseMoved(x, y);
        }
    }

    void mouseClicked() {
        switch (state) {
            case SPLASH -> skipSplash();
            case SETTINGS -> settingsMenu.mouseClicked();
            case TITLE -> {
                int clickedOption = optionAt(mouseX, mouseY);
                if (clickedOption >= 0) {
                    selectedOption = clickedOption;
                    activateSelection();
                }
            }
            default -> throw new IllegalStateException("Estado desconhecido: " + state);
        }
    }

    /** @param amount rolagem vertical da roda; positivo = para cima */
    void scrolled(double amount) {
        if (state == State.SETTINGS) {
            settingsMenu.scrolled(amount);
        }
    }

    void render(float time) {
        renderer.beginFrame(width, height);
        switch (state) {
            case SPLASH -> renderer.drawSplash(width, height, splashElapsed);
            case TITLE -> renderer.drawTitle(width, height, time, MENU_OPTIONS, selectedOption, statusMessage);
            case SETTINGS -> {
                renderer.drawBackground(width, height);
                settingsMenu.draw(renderer, width, height);
            }
            default -> throw new IllegalStateException("Estado desconhecido: " + state);
        }
        if (settings.showFps()) {
            renderer.drawFps(fps, width, height);
        }
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
        switch (selectedOption) {
            case 0 -> statusMessage = "Novo jogo - em desenvolvimento";
            case 1 -> {
                settingsMenu.reset();
                state = State.SETTINGS;
            }
            case 2 -> statusMessage = "Carregar jogo - em desenvolvimento";
            case 3 -> glfwSetWindowShouldClose(window, true);
            default -> throw new IllegalStateException("Opção de menu desconhecida: " + selectedOption);
        }
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

    private void applyVideo() {
        glfwSwapInterval(settings.vsync() ? 1 : 0);
        boolean fullscreen = settings.fullscreen();
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
     * para a maior, começando pela nativa. Inclui sempre 1280 × 720, o tamanho padrão da janela.
     */
    private static List<Resolution> findResolutions() {
        TreeSet<Resolution> sizes = new TreeSet<>();
        sizes.add(Resolution.HD);
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode.Buffer modes = monitor == NULL ? null : glfwGetVideoModes(monitor);
        if (modes != null) {
            for (int i = 0; i < modes.limit(); i++) {
                GLFWVidMode mode = modes.get(i);
                if (mode.width() >= MIN_RESOLUTION_WIDTH) {
                    sizes.add(new Resolution(mode.width(), mode.height()));
                }
            }
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
