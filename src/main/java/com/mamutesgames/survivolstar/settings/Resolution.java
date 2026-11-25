package com.mamutesgames.survivolstar.settings;

/** Tamanho da tela em pixels. {@link #NATIVE} usa a resolução atual do monitor principal. */
public record Resolution(int width, int height) implements Comparable<Resolution> {

    public static final Resolution NATIVE = new Resolution(0, 0);
    public static final Resolution HD = new Resolution(1280, 720);

    public boolean isNative() {
        return width <= 0 || height <= 0;
    }

    public String label() {
        return isNative() ? "Nativa" : width + " × " + height;
    }

    /** Formato salvo nas configurações: "1920x1080" ou "native". */
    @Override
    public String toString() {
        return isNative() ? "native" : width + "x" + height;
    }

    /** Lê o formato de {@link #toString()}; valores inválidos viram {@code fallback}. */
    public static Resolution parse(String value, Resolution fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.trim().toLowerCase();
        if (text.equals("native")) {
            return NATIVE;
        }
        int x = text.indexOf('x');
        try {
            Resolution parsed = new Resolution(Integer.parseInt(text.substring(0, x)),
                    Integer.parseInt(text.substring(x + 1)));
            return parsed.isNative() ? fallback : parsed;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return fallback;
        }
    }

    @Override
    public int compareTo(Resolution other) {
        return width != other.width ? Integer.compare(width, other.width) : Integer.compare(height, other.height);
    }
}
