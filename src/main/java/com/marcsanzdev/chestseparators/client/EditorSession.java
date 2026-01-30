package com.marcsanzdev.chestseparators.client;

/**
 * Controla el estado de la interfaz de edición.
 * Es Singleton para poder acceder desde cualquier Mixin.
 */
public class EditorSession {
    private static final EditorSession INSTANCE = new EditorSession();

    private boolean active = false;
    private int selectedColor = 0xFFFFFF; // Blanco por defecto (Hex)

    private EditorSession() {}

    public static EditorSession getInstance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    public void toggle() {
        this.active = !this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(int color) {
        this.selectedColor = color;
    }
}