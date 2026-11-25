package com.mamutesgames.survivolstar.save;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/** Slots de jogos salvos, um arquivo {@code .properties} por slot. */
public final class SaveStore {

    public static final int SLOTS = 5;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final Path directory;

    public SaveStore(Path directory) {
        this.directory = directory;
    }

    /** Grava num arquivo temporário e troca no fim, para não corromper o save anterior. */
    public void save(int slot, SaveGame game) throws IOException {
        Files.createDirectories(directory);
        Path target = file(slot);
        Path temporary = directory.resolve(target.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            game.toProperties().store(writer, "Survivol Star - jogo salvo");
        }
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** Apaga o jogo do slot (um slot já vazio não é erro). */
    public void delete(int slot) throws IOException {
        Files.deleteIfExists(file(slot));
    }

    /** O jogo do slot, ou vazio se não houver ou estiver corrompido. */
    public Optional<SaveGame> load(int slot) {
        Path file = file(slot);
        if (!Files.isReadable(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            return Optional.of(SaveGame.fromProperties(properties));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Save do slot " + slot + " ilegível: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Um resumo por slot, na ordem dos slots, para a lista de jogos salvos. */
    public List<SaveSummary> summaries() {
        List<SaveSummary> result = new ArrayList<>(SLOTS);
        for (int slot = 1; slot <= SLOTS; slot++) {
            int number = slot;
            result.add(load(slot).map(game -> summarize(number, game)).orElseGet(() -> SaveSummary.empty(number)));
        }
        return result;
    }

    private static SaveSummary summarize(int slot, SaveGame game) {
        List<String> details = new ArrayList<>();
        details.add(game.difficulty().label());
        details.add(formatPlayTime(game.playSeconds()));
        if (!game.summary().isEmpty()) {
            details.add(game.summary());
        }
        return new SaveSummary(slot, true, DATE_FORMAT.format(game.savedAt()), details);
    }

    /** Tempo de jogo como "2h 05min" ou "12min". */
    static String formatPlayTime(long seconds) {
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return hours > 0 ? String.format("%dh %02dmin", hours, minutes % 60) : minutes + "min";
    }

    private Path file(int slot) {
        if (slot < 1 || slot > SLOTS) {
            throw new IllegalArgumentException("Slot inválido: " + slot);
        }
        return directory.resolve("slot" + slot + ".properties");
    }
}
