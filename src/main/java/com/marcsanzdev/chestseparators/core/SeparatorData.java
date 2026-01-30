package com.marcsanzdev.chestseparators.core;

/**
 * Datos de un separador individual.
 * Por ahora solo posición, luego añadiremos color.
 */
public record SeparatorData(SeparatorPosition position, int colorHex) {
    // Constructor compacto para asignar blanco por defecto si no se especifica
    public SeparatorData(SeparatorPosition position) {
        this(position, 0xFFFFFFFF); // Blanco puro (ARGB)
    }

    // Importante para que GSON y los Sets funcionen bien:
    // Dos separadores son "iguales" si están en la misma posición (ignoramos color para la unicidad en el Set)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeparatorData that = (SeparatorData) o;
        return position == that.position;
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }
}