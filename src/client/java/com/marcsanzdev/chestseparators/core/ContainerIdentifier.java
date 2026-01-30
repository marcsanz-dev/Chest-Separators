package com.marcsanzdev.chestseparators.core;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class ContainerIdentifier {

    public static String getIdFromCurrentTarget() {
        MinecraftClient client = MinecraftClient.getInstance();
        HitResult hit = client.crosshairTarget;

        if (hit != null && hit.getType() == HitResult.Type.BLOCK && client.world != null) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            BlockState state = client.world.getBlockState(pos);
            String worldId = client.world.getRegistryKey().getValue().toString();

            // Lógica para Cofres Dobles
            if (state.getBlock() instanceof ChestBlock) {
                ChestType type = state.get(ChestBlock.CHEST_TYPE);
                if (type != ChestType.SINGLE) {
                    // Si es doble, buscamos la posición de la "otra mitad" y nos quedamos
                    // siempre con la coordenada más pequeña para que ambos bloques generen la misma ID.
                    BlockPos otherPos = pos.offset(ChestBlock.getFacing(state));
                    if (type == ChestType.RIGHT) {
                        // Si estamos mirando la parte derecha, la ID se basa en la izquierda (la menor)
                        pos = otherPos;
                    }
                }
            }

            return worldId + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
        }

        return "unknown_container";
    }
}