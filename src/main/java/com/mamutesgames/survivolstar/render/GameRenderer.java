package com.mamutesgames.survivolstar.render;

import com.mamutesgames.survivolstar.GameInfo;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

public final class GameRenderer implements AutoCloseable {

    private static final String BACKGROUND_RESOURCE = "/images/title-background.png";

    // Paleta compartilhada pelas telas (RGB)
    public static final int TEXT = 0xF5F7FF;
    public static final int CYAN = 0x54D8FF;
    public static final int LABEL = 0x9CA8C0;
    public static final int DISABLED = 0x5C6378;
    public static final int WARNING = 0xFFC93C;
    public static final int ACCENT = 0x38C7FF;
    public static final int SELECTION = 0x0A1F30;
    public static final int SHADE = 0x000008;
    public static final int PANEL = 0x05081A;
    public static final int OUTLINE = 0x99BFFF;

    private final Map<String, TextTexture> textTextures = new HashMap<>();
    private final int backgroundTexture;

    public GameRenderer() {
        backgroundTexture = loadBackground();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
    }

    /** Fator de escala das telas em relação a 1280 × 720. */
    public static float scale(int width, int height) {
        return Math.max(0.65f, Math.min(width / 1280f, height / 720f));
    }

    /** Prepara a projeção em pixels e limpa a tela. */
    public void beginFrame(int width, int height) {
        glViewport(0, 0, width, height);
        useScreenProjection(width, height);
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    /** Projeção 2D em pixels, origem no canto superior esquerdo (textos e menus). */
    private static void useScreenProjection(int width, int height) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, Math.max(width, 1), Math.max(height, 1), 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    // ---------------------------------------------------------------- cena da partida

    private static final int STAR_COUNT = 260;
    /** Estrelas fixas: x e y (0..1), tamanho e fase da cintilação. */
    private static final float[][] STARS = createStars();

    private static float[][] createStars() {
        java.util.Random random = new java.util.Random(20261125L);
        float[][] stars = new float[STAR_COUNT][];
        for (int i = 0; i < STAR_COUNT; i++) {
            stars[i] = new float[] {random.nextFloat(), random.nextFloat(), 1f + random.nextFloat() * 1.6f,
                    random.nextFloat() * 6.283f};
        }
        return stars;
    }

    /** Faces do cubo: 4 vértices cada (x, y, z) e o tom da face (luz fixa, sem iluminação do GL). */
    private static final float[][] CUBE_FACES = {
            {-1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1},        // frente
            {1, -1, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1},    // trás
            {-1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, -1},        // cima
            {-1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, 1},    // baixo
            {1, -1, 1, 1, -1, -1, 1, 1, -1, 1, 1, 1},        // direita
            {-1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1}     // esquerda
    };
    private static final float[] CUBE_SHADES = {1f, 0.55f, 0.85f, 0.4f, 0.7f, 0.62f};

    /**
     * Cena da partida: um campo de estrelas cintilando e um cubo girando em perspectiva no
     * centro. Volta à projeção 2D no fim, para o HUD e os menus.
     *
     * @param angle giro do cubo, em graus
     */
    public void drawScene(int width, int height, float time, float angle) {
        for (float[] star : STARS) {
            float twinkle = 0.45f + 0.55f * (0.5f + 0.5f * (float) Math.sin(time * 1.7f + star[3]));
            fill(star[0] * width, star[1] * height, star[2], star[2], TEXT, twinkle);
        }

        float aspect = Math.max(width, 1) / (float) Math.max(height, 1);
        float near = 0.1f;
        float top = near * (float) Math.tan(Math.toRadians(25.0));
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glFrustum(-top * aspect, top * aspect, -top, top, near, 50.0);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glTranslatef(0f, 0f, -5.5f);
        glRotatef(20f + angle * 0.6f, 1f, 0f, 0f);
        glRotatef(angle, 0f, 1f, 0f);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1f, 1f);
        glBegin(GL_QUADS);
        for (int f = 0; f < CUBE_FACES.length; f++) {
            float shade = CUBE_SHADES[f];
            glColor4f(0.06f * shade, 0.32f * shade, 0.55f * shade, 1f);
            float[] v = CUBE_FACES[f];
            for (int i = 0; i < 12; i += 3) {
                glVertex3f(v[i], v[i + 1], v[i + 2]);
            }
        }
        glEnd();
        glDisable(GL_POLYGON_OFFSET_FILL);

        // Arestas acesas, pulsando devagar
        float glow = 0.7f + 0.3f * (float) Math.sin(time * 2f);
        glLineWidth(2f);
        glColor4f(0.33f, 0.85f, 1f, glow);
        for (float[] v : CUBE_FACES) {
            glBegin(GL_LINE_LOOP);
            for (int i = 0; i < 12; i += 3) {
                glVertex3f(v[i], v[i + 1], v[i + 2]);
            }
            glEnd();
        }
        glLineWidth(1f);
        glDisable(GL_DEPTH_TEST);
        useScreenProjection(width, height);
    }

    /** Imagem de fundo da tela de título (se existir), escurecida para o texto ficar legível. */
    public void drawBackground(int width, int height) {
        if (backgroundTexture != 0) {
            drawTexture(backgroundTexture, 0f, 0f, width, height);
            drawRect(0f, 0f, width, height, 0f, 0f, 0f, 0.62f);
        }
    }

    /** Contador de quadros por segundo no canto superior direito. */
    public void drawFps(int fps, int width, int height) {
        float s = scale(width, height);
        text("FPS " + fps, 15f * s, width - 14f * s, 18f * s, LABEL, 1f, 1f);
    }

    public void drawSplash(int width, int height, double elapsed) {
        float alpha;
        if (elapsed < 0.8) {
            alpha = smooth((float) (elapsed / 0.8));
        } else if (elapsed < 2.4) {
            alpha = 1f;
        } else {
            alpha = smooth((float) ((3.2 - elapsed) / 0.8));
        }

        float centerX = width * 0.5f;
        float centerY = height * 0.5f;
        drawText(GameInfo.STUDIO.toUpperCase(Locale.ROOT), 46, centerX, centerY, 0xF1F4FF, alpha, true);
        drawRect(centerX - width * 0.13f * alpha, centerY + 36f,
                centerX + width * 0.13f * alpha, centerY + 39f, 0.24f, 0.82f, 1f, alpha * 0.8f);
    }

    public void drawTitle(int width, int height, float time, String[] options,
                   int selectedOption, String statusMessage) {
        drawBackground(width, height);

        float centerX = width * 0.5f;
        float scale = scale(width, height);
        drawText(GameInfo.NAME.toUpperCase(Locale.ROOT), Math.round(68f * scale), centerX, height * 0.22f,
                0xF4F7FF, 1f, true);
        drawRect(centerX - 135f * scale, height * 0.22f + 48f * scale,
                centerX + 135f * scale, height * 0.22f + 51f * scale, 0.2f, 0.78f, 1f, 0.85f);

        for (int i = 0; i < options.length; i++) {
            float centerY = height * 0.48f + i * height * 0.09f;
            boolean selected = i == selectedOption;
            if (selected) {
                drawRect(centerX - width * 0.22f, centerY - 25f * scale,
                        centerX + width * 0.22f, centerY + 25f * scale, 0.04f, 0.12f, 0.19f, 0.88f);
                drawRect(centerX - width * 0.22f, centerY - 25f * scale,
                        centerX - width * 0.22f + 4f * scale, centerY + 25f * scale,
                        0.22f, 0.78f, 1f, 1f);
            }
            drawText(options[i], Math.round(29f * scale), centerX, centerY,
                    selected ? CYAN : TEXT, 1f, true);
        }

        if (!statusMessage.isEmpty()) {
            drawText(statusMessage, Math.round(18f * scale), centerX, height * 0.9f,
                    0xB9C1D4, 1f, true);
        } else {
            float blink = 0.55f + 0.45f * (0.5f + 0.5f * (float) Math.sin(time * 2f));
            drawText(GameInfo.STUDIO.toUpperCase(Locale.ROOT), Math.round(14f * scale), centerX,
                    height * 0.94f, LABEL, blink, true);
        }
    }

    /**
     * Texto centralizado verticalmente em {@code y}.
     *
     * @param anchor posição horizontal de {@code x} no texto: 0 esquerda, 0.5 centro, 1 direita
     */
    public void text(String text, float size, float x, float y, int color, float alpha, float anchor) {
        int pixels = Math.max(1, Math.round(size));
        String key = text + '\u0000' + pixels;
        TextTexture texture = textTextures.computeIfAbsent(key, ignored -> createTextTexture(text, pixels));
        drawTintedTexture(texture.id, x - texture.width * anchor, y - texture.height * 0.5f,
                texture.width, texture.height, color, alpha);
    }

    private void drawText(String text, int size, float x, float y, int color, float alpha, boolean centered) {
        text(text, size, x, y, color, alpha, centered ? 0.5f : 0f);
    }

    /** Retângulo preenchido. */
    public void fill(float x, float y, float width, float height, int color, float alpha) {
        drawRect(x, y, x + width, y + height, ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f, alpha);
    }

    /** Painel translúcido com borda, o fundo das telas de menu. */
    public void panel(float x, float y, float width, float height, float s) {
        fill(x, y, width, height, PANEL, 0.85f);
        outline(x, y, width, height, 2f * s, OUTLINE, 0.8f);
    }

    /** Contorno de um retângulo, com a espessura para dentro. */
    public void outline(float x, float y, float width, float height, float thickness, int color, float alpha) {
        fill(x, y, width, thickness, color, alpha);
        fill(x, y + height - thickness, width, thickness, color, alpha);
        fill(x, y + thickness, thickness, height - 2 * thickness, color, alpha);
        fill(x + width - thickness, y + thickness, thickness, height - 2 * thickness, color, alpha);
    }

    private static TextTexture createTextTexture(String text, int size) {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(size, 1));
        BufferedImage measurement = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measurementGraphics = measurement.createGraphics();
        measurementGraphics.setFont(font);
        FontMetrics metrics = measurementGraphics.getFontMetrics();
        int width = Math.max(1, metrics.stringWidth(text) + 12);
        int height = Math.max(1, metrics.getHeight() + 8);
        measurementGraphics.dispose();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(font);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.drawString(text, 6, 4 + graphics.getFontMetrics().getAscent());
        graphics.dispose();
        return new TextTexture(uploadImage(image), width, height);
    }

    private int loadBackground() {
        try (InputStream input = GameRenderer.class.getResourceAsStream(BACKGROUND_RESOURCE)) {
            if (input == null) {
                return 0;
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalStateException("A imagem de fundo não é um arquivo de imagem válido: "
                        + BACKGROUND_RESOURCE);
            }
            return uploadImage(image);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível carregar a imagem de fundo: "
                    + BACKGROUND_RESOURCE, e);
        }
    }

    private static int uploadImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer pixels = memAlloc(width * height * 4);
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getRGB(x, y);
                    pixels.put((byte) ((argb >> 16) & 0xFF));
                    pixels.put((byte) ((argb >> 8) & 0xFF));
                    pixels.put((byte) (argb & 0xFF));
                    pixels.put((byte) ((argb >> 24) & 0xFF));
                }
            }
            pixels.flip();

            int texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA,
                    GL_UNSIGNED_BYTE, pixels);
            glBindTexture(GL_TEXTURE_2D, 0);
            return texture;
        } finally {
            memFree(pixels);
        }
    }

    private static void drawTexture(int texture, float x, float y, float width, float height) {
        drawTintedTexture(texture, x, y, width, height, 0xFFFFFF, 1f);
    }

    private static void drawTintedTexture(int texture, float x, float y, float width, float height,
                                          int color, float alpha) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texture);
        glColor4f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f, alpha);
        glBegin(GL_QUADS);
        glTexCoord2f(0f, 0f);
        glVertex2f(x, y);
        glTexCoord2f(1f, 0f);
        glVertex2f(x + width, y);
        glTexCoord2f(1f, 1f);
        glVertex2f(x + width, y + height);
        glTexCoord2f(0f, 1f);
        glVertex2f(x, y + height);
        glEnd();
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_TEXTURE_2D);
    }

    private static void drawRect(float left, float top, float right, float bottom,
                                 float red, float green, float blue, float alpha) {
        glDisable(GL_TEXTURE_2D);
        glColor4f(red, green, blue, alpha);
        glBegin(GL_QUADS);
        glVertex2f(left, top);
        glVertex2f(right, top);
        glVertex2f(right, bottom);
        glVertex2f(left, bottom);
        glEnd();
    }

    private static float smooth(float value) {
        float clamped = Math.max(0f, Math.min(value, 1f));
        return clamped * clamped * (3f - 2f * clamped);
    }

    @Override
    public void close() {
        for (TextTexture texture : textTextures.values()) {
            glDeleteTextures(texture.id);
        }
        if (backgroundTexture != 0) {
            glDeleteTextures(backgroundTexture);
        }
    }

    private record TextTexture(int id, int width, int height) {
    }
}
