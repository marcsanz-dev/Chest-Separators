package com.marcsanzdev.chestseparators.client;

import com.marcsanzdev.chestseparators.core.SeparatorPosition;

public class EditorSession {
    private static final EditorSession INSTANCE = new EditorSession();
    private SeparatorPosition dragAxis = null;

    private boolean active = false;
    private int selectedColorIndex = 0; // Por defecto el primero (Blanco)

    // Los 16 Colores estándar de Minecraft (Formato ARGB: 0xFF + RRGGBB)
    public static final int[] PALETTE = {
            0xFFFFFFFF, // 0: White
            0xFFFF8800, // 1: Orange
            0xFFFF00FF, // 2: Magenta
            0xFF6699FF, // 3: Light Blue
            0xFFFFFF00, // 4: Yellow
            0xFF88FF00, // 5: Lime
            0xFFFF8888, // 6: Pink
            0xFF888888, // 7: Gray
            0xFFAAAAAA, // 8: Light Gray
            0xFF00AAAA, // 9: Cyan
            0xFF8800FF, // 10: Purple
            0xFF0000AA, // 11: Blue
            0xFF884400, // 12: Brown
            0xFF00AA00, // 13: Green
            0xFFAA0000, // 14: Red
            0xFF000000  // 15: Black
    };

    private EditorSession() {}

    public static EditorSession getInstance() {
        return INSTANCE;
    }

    public boolean isActive() { return active; }
    public void toggle() { this.active = !this.active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSelectedColor() {
        return PALETTE[selectedColorIndex];
    }

    public void setSelectedColorIndex(int index) {
        if (index >= 0 && index < PALETTE.length) {
            this.selectedColorIndex = index;
        }
    }

    public int getSelectedColorIndex() {
        return selectedColorIndex;
    }

    public void setDragAxis(SeparatorPosition pos) {
        if (pos == null) {
            this.dragAxis = null;
            return;
        }
        // Si el primer clic es arriba/abajo, estamos haciendo una línea HORIZONTAL
        if (pos == SeparatorPosition.TOP || pos == SeparatorPosition.BOTTOM) {
            this.dragAxis = SeparatorPosition.TOP;
        } else {
            this.dragAxis = SeparatorPosition.LEFT;
        }
    }

    public boolean isPosAllowed(SeparatorPosition currentPos) {
        if (dragAxis == null) return true;
        if (dragAxis == SeparatorPosition.TOP || dragAxis == SeparatorPosition.BOTTOM) {
            return currentPos == SeparatorPosition.TOP || currentPos == SeparatorPosition.BOTTOM;
        }
        return currentPos == SeparatorPosition.LEFT || currentPos == SeparatorPosition.RIGHT;
    }
}