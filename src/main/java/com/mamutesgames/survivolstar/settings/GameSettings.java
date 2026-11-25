package com.mamutesgames.survivolstar.settings;

import java.util.Properties;

/**
 * Preferências do jogador. A dificuldade vale a partir do próximo jogo; as opções de vídeo
 * são aplicadas na hora.
 */
public final class GameSettings {

    public static final int MAX_VOLUME = 100;
    private static final int VERSION = 1;

    private Difficulty difficulty = Difficulty.DEFAULT;
    private boolean fullscreen;
    private Resolution resolution = Resolution.HD;
    private boolean vsync = true;
    private boolean showFps;
    private int masterVolume = 100;
    private int musicVolume = 70;
    private int effectsVolume = 100;

    public Difficulty difficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public boolean fullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    /** Tamanho da tela: modo de vídeo em tela cheia, tamanho da janela fora dela. */
    public Resolution resolution() {
        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }

    public boolean vsync() {
        return vsync;
    }

    public void setVsync(boolean vsync) {
        this.vsync = vsync;
    }

    public boolean showFps() {
        return showFps;
    }

    public void setShowFps(boolean showFps) {
        this.showFps = showFps;
    }

    /** Volume geral, de 0 a 100. */
    public int masterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(int volume) {
        masterVolume = Math.clamp(volume, 0, MAX_VOLUME);
    }

    /** Volume da música, de 0 a 100. */
    public int musicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(int volume) {
        musicVolume = Math.clamp(volume, 0, MAX_VOLUME);
    }

    /** Volume dos efeitos e dos sons dos menus, de 0 a 100. */
    public int effectsVolume() {
        return effectsVolume;
    }

    public void setEffectsVolume(int volume) {
        effectsVolume = Math.clamp(volume, 0, MAX_VOLUME);
    }

    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty("version", Integer.toString(VERSION));
        p.setProperty("difficulty", difficulty.name());
        p.setProperty("fullscreen", Boolean.toString(fullscreen));
        p.setProperty("resolution", resolution.toString());
        p.setProperty("vsync", Boolean.toString(vsync));
        p.setProperty("showFps", Boolean.toString(showFps));
        p.setProperty("volume.master", Integer.toString(masterVolume));
        p.setProperty("volume.music", Integer.toString(musicVolume));
        p.setProperty("volume.effects", Integer.toString(effectsVolume));
        return p;
    }

    /** Lê as preferências; valores ausentes ou inválidos ficam no padrão. */
    public static GameSettings fromProperties(Properties p) {
        GameSettings s = new GameSettings();
        s.setDifficulty(parseEnum(Difficulty.class, p.getProperty("difficulty"), s.difficulty));
        s.setFullscreen(Boolean.parseBoolean(p.getProperty("fullscreen", Boolean.toString(s.fullscreen))));
        s.setResolution(Resolution.parse(p.getProperty("resolution"), s.resolution));
        s.setVsync(Boolean.parseBoolean(p.getProperty("vsync", Boolean.toString(s.vsync))));
        s.setShowFps(Boolean.parseBoolean(p.getProperty("showFps", Boolean.toString(s.showFps))));
        s.setMasterVolume(parseInt(p.getProperty("volume.master"), s.masterVolume));
        s.setMusicVolume(parseInt(p.getProperty("volume.music"), s.musicVolume));
        s.setEffectsVolume(parseInt(p.getProperty("volume.effects"), s.effectsVolume));
        return s;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        try {
            return value == null ? fallback : Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
