package com.marcsanzdev.chestseparators.core;

import net.minecraft.util.math.BlockPos;

/**
 * Identificador inmutable para contenedores.
 * Clave compuesta: ID del mundo + Coordenadas (X,Y,Z).
 */
public record ContainerIdentifier(String worldId, BlockPos pos) {

    // Convierte el ID a un String único para guardar en JSON (Key)
    public String toUniqueString() {
        return worldId + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    // Reconstruye el ID desde un String (usado al cargar JSON)
    public static ContainerIdentifier fromUniqueString(String str) {
        try {
            String[] parts = str.split("@");
            if (parts.length != 2) return null;

            String wId = parts[0];
            String[] coords = parts[1].split(",");

            BlockPos p = new BlockPos(
                    Integer.parseInt(coords[0]),
                    Integer.parseInt(coords[1]),
                    Integer.parseInt(coords[2])
            );
            return new ContainerIdentifier(wId, p);
        } catch (Exception e) {
            // Si el formato es inválido, retornamos null para evitar crash
            return null;
        }
    }
}