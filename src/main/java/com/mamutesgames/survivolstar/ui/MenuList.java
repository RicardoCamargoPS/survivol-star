package com.mamutesgames.survivolstar.ui;

import com.mamutesgames.survivolstar.render.GameRenderer;
import java.util.ArrayList;
import java.util.List;

/**
 * Lista vertical de {@link MenuItem}. Guarda a posição do último desenho para saber qual
 * linha está sob o mouse.
 */
final class MenuList {

    private static final float ROW_HEIGHT = 46f;
    private static final float FONT_SIZE = 24f;
    /** Fração direita da linha ocupada pelo valor de uma opção ajustável. */
    private static final float VALUE_FRACTION = 0.45f;

    private final List<MenuItem> items = new ArrayList<>();
    private int selected;
    private boolean active = true;

    private boolean laidOut;
    private float layoutLeft;
    private float layoutTop;
    private float layoutWidth;
    private float layoutRowHeight;

    MenuList add(MenuItem item) {
        items.add(item);
        return this;
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    int size() {
        return items.size();
    }

    int selectedIndex() {
        return selected;
    }

    MenuItem selectedItem() {
        return items.get(selected);
    }

    MenuItem item(int index) {
        return items.get(index);
    }

    void select(int index) {
        selected = Math.clamp(index, 0, Math.max(0, items.size() - 1));
    }

    /** Move a seleção pulando itens desabilitados, com volta nas pontas. */
    void move(int direction) {
        for (int step = 1; step <= items.size(); step++) {
            int candidate = Math.floorMod(selected + direction * step, items.size());
            if (items.get(candidate).isEnabled()) {
                selected = candidate;
                return;
            }
        }
    }

    /** Se o item selecionado ficou desabilitado, seleciona o próximo habilitado. */
    void ensureSelectable() {
        if (!items.isEmpty() && !items.get(selected).isEnabled()) {
            move(1);
        }
    }

    /** Liga ou desliga o destaque da linha selecionada (só o lado com o foco fica destacado). */
    void setActive(boolean active) {
        this.active = active;
    }

    /** Índice da linha habilitada sob o ponto, ou -1. */
    int rowAt(double x, double y) {
        if (!laidOut || x < layoutLeft || x > layoutLeft + layoutWidth || y < layoutTop) {
            return -1;
        }
        int row = (int) ((y - layoutTop) / layoutRowHeight);
        return row < items.size() && items.get(row).isEnabled() ? row : -1;
    }

    /**
     * Ajuste indicado por um clique em {@code x} na área do valor: -1 na metade esquerda,
     * +1 na direita, 0 fora dela.
     */
    int adjustDirectionAt(double x) {
        float valueLeft = layoutLeft + layoutWidth * (1f - VALUE_FRACTION);
        if (x < valueLeft) {
            return 0;
        }
        return x < valueLeft + layoutWidth * VALUE_FRACTION * 0.5f ? -1 : 1;
    }

    /** Altura de uma linha, em pixels. */
    static float rowHeight(float s) {
        return ROW_HEIGHT * s;
    }

    float height(float s) {
        return items.size() * ROW_HEIGHT * s;
    }

    /** Desenha a lista com o canto superior esquerdo em {@code (left, top)}. */
    void draw(GameRenderer r, float left, float top, float width, float s) {
        float rowHeight = ROW_HEIGHT * s;
        float pad = 18f * s;
        float right = left + width;
        laidOut = true;
        layoutLeft = left;
        layoutTop = top;
        layoutWidth = width;
        layoutRowHeight = rowHeight;
        ensureSelectable();

        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            float y = top + i * rowHeight;
            float middle = y + rowHeight * 0.5f;
            boolean enabled = item.isEnabled();
            boolean isSelected = i == selected && active && enabled;
            if (isSelected) {
                r.fill(left, y + 3f * s, width, rowHeight - 6f * s, GameRenderer.SELECTION, 0.88f);
                r.fill(left, y + 3f * s, 4f * s, rowHeight - 6f * s, GameRenderer.ACCENT, 1f);
            }
            int color = !enabled ? GameRenderer.DISABLED : isSelected ? GameRenderer.CYAN : GameRenderer.TEXT;
            String value = item.value();
            if (value == null) {
                r.text(item.label(), FONT_SIZE * s, left + width * 0.5f, middle, color, 1f, 0.5f);
                continue;
            }
            float size = FONT_SIZE * s * 0.85f;
            r.text(item.label(), size, left + pad, middle, color, 1f, 0f);
            if (!item.isAdjustable()) {
                // Detalhe informativo (ex.: data do save), em texto menor
                r.text(value, FONT_SIZE * s * 0.68f, right - pad, middle, color, 1f, 1f);
                continue;
            }
            float valueLeft = right - width * VALUE_FRACTION;
            if (isSelected) {
                r.text("<", size, valueLeft + pad, middle, GameRenderer.CYAN, 1f, 0f);
                r.text(">", size, right - pad, middle, GameRenderer.CYAN, 1f, 1f);
            }
            r.text(value, size, (valueLeft + right) * 0.5f, middle, color, 1f, 0.5f);
        }
    }
}
