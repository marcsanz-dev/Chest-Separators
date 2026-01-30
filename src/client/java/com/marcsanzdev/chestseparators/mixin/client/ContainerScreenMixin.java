package com.marcsanzdev.chestseparators.mixin.client;

import com.marcsanzdev.chestseparators.client.EditorSession;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class ContainerScreenMixin<T extends ScreenHandler> extends Screen {

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected T handler;
    @Shadow protected Slot focusedSlot; // El slot que tiene el ratón encima (Minecraft ya lo calcula)

    protected ContainerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addEditorButton(CallbackInfo ci) {
        if (this.handler instanceof GenericContainerScreenHandler) {
            int buttonX = this.x + this.backgroundWidth - 20;
            int buttonY = this.y - 18;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("||"), button -> {
                        EditorSession.getInstance().toggle();
                    })
                    .dimensions(buttonX, buttonY, 20, 16)
                    .build());
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderEditorOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Solo trabajamos si es un Cofre y el modo editor está ACTIVO
        if (this.handler instanceof GenericContainerScreenHandler && EditorSession.getInstance().isActive()) {

            // 1. Aviso de texto
            context.drawText(this.textRenderer, "MODO EDITOR", this.x, this.y - 30, 0xFF5555, true);

            // 2. Lógica de Detección de Bordes (Preview)
            if (this.focusedSlot != null) {
                drawPreviewLine(context, this.focusedSlot, mouseX, mouseY);
            }
        }
    }

    // Método auxiliar para mantener el código limpio
    private void drawPreviewLine(DrawContext context, Slot slot, int mouseX, int mouseY) {
        // Coordenadas absolutas del slot en pantalla
        int sx = this.x + slot.x;
        int sy = this.y + slot.y;

        // Distancias del ratón a los 4 bordes (0 a 16 píxeles aprox)
        int distLeft = mouseX - sx;
        int distRight = (sx + 16) - mouseX; // Los slots miden 16x16 visualmente (aunque el área es 18x18)
        int distTop = mouseY - sy;
        int distBottom = (sy + 16) - mouseY;

        // Encontrar la distancia mínima
        int min = Math.min(Math.min(distLeft, distRight), Math.min(distTop, distBottom));

        // Color de la línea "Fantasma" (Amarillo semitransparente: ARGB)
        int previewColor = 0xAAFFFF00;
        int thickness = 2; // Grosor de la línea

        // Dibujar en el borde ganador
        // fill(x1, y1, x2, y2, color)

        if (min == distLeft) {
            // Borde Izquierdo
            context.fill(sx - 1, sy, sx - 1 + thickness, sy + 16, previewColor);
        } else if (min == distRight) {
            // Borde Derecho
            context.fill(sx + 16 - 1, sy, sx + 16 - 1 + thickness, sy + 16, previewColor);
        } else if (min == distTop) {
            // Borde Superior
            context.fill(sx, sy - 1, sx + 16, sy - 1 + thickness, previewColor);
        } else {
            // Borde Inferior
            context.fill(sx, sy + 16 - 1, sx + 16, sy + 16 - 1 + thickness, previewColor);
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        EditorSession.getInstance().setActive(false);
    }
}