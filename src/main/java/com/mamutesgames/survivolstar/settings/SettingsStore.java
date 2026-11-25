package com.mamutesgames.survivolstar.settings;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Lê e grava as preferências num arquivo {@code .properties}. */
public final class SettingsStore {

    private final Path file;

    public SettingsStore(Path file) {
        this.file = file;
    }

    /** Preferências salvas, ou o padrão se o arquivo não existir ou estiver ilegível. */
    public GameSettings load() {
        if (!Files.isReadable(file)) {
            return new GameSettings();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            return GameSettings.fromProperties(properties);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Configurações ilegíveis, usando o padrão: " + e.getMessage());
            return new GameSettings();
        }
    }

    public void save(GameSettings settings) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                settings.toProperties().store(writer, "Survivol Star - configurações");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
