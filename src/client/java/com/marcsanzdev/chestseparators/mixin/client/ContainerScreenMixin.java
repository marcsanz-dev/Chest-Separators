package com.marcsanzdev.chestseparators.mixin.client;

import com.marcsanzdev.chestseparators.client.EditorSession;
import com.marcsanzdev.chestseparators.config.ConfigManager;
import com.marcsanzdev.chestseparators.core.SeparatorData;
import com.marcsanzdev.chestseparators.core.SeparatorPosition;
import com.mojang.blaze3d.systems.RenderSystem; // <--- NUEVO IMPORT VITAL
import net.minecraft.client.gui.Click;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(HandledScreen.class)
public abstract class ContainerScreenMixin<T extends ScreenHandler> extends Screen {

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected T handler;
    @Shadow protected Slot focusedSlot;

    // ID Temporal
    private static final String TEMP_CHEST_ID = "chest_v2_debug";

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

    // --- DETECCIÓN Y GUARDADO (Con el fix de 'Click' que hicimos antes) ---
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onEditorClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> info) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (this.handler instanceof GenericContainerScreenHandler && EditorSession.getInstance().isActive() && button == 0) {

            if (this.focusedSlot != null) {
                SeparatorPosition pos = getClosestPosition(this.focusedSlot, (int)mouseX, (int)mouseY);

                if (pos != null) {
                    // ROJO OPACO para debug
                    int debugColor = 0xFFFF0000;
                    SeparatorData data = new SeparatorData(pos, debugColor);

                    ConfigManager.getInstance().toggleSeparator(TEMP_CHEST_ID, this.focusedSlot.id, data);
                    System.out.println("DEBUG: Guardado separador en slot " + this.focusedSlot.id);
                    info.setReturnValue(true);
                }
            }
        }
    }

    // --- RENDERIZADO (FIX MATRICES Y Z-INDEX) ---
    @Inject(method = "render", at = @At("TAIL"))
    private void renderSeparators(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!(this.handler instanceof GenericContainerScreenHandler)) return;

        // YA NO USAMOS RenderSystem NI Matrices.
        // Usaremos la altura Z (300) directamente en el método de dibujo.

        // A) DIBUJAR LÍNEAS GUARDADAS
        for (Slot slot : this.handler.slots) {
            Set<SeparatorData> separators = ConfigManager.getInstance().getSlotSeparators(TEMP_CHEST_ID, slot.id);
            if (!separators.isEmpty()) {
                for (SeparatorData data : separators) {
                    drawSeparatorLine(context, slot, data.position(), data.colorHex(), 3);
                }
            }
        }

        // B) DIBUJAR PREVIEW (Modo Editor)
        if (EditorSession.getInstance().isActive()) {
            // Texto de aviso (Z=300 para que flote encima de todo)
            context.drawText(this.textRenderer, "MODO EDITOR", this.x, this.y - 30, 0xFF5555, true);

            if (this.focusedSlot != null) {
                SeparatorPosition pos = getClosestPosition(this.focusedSlot, mouseX, mouseY);
                if (pos != null) {
                    drawSeparatorLine(context, this.focusedSlot, pos, 0xAAFFFF00, 3);
                }
            }
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        EditorSession.getInstance().setActive(false);
    }

    @Unique
    private SeparatorPosition getClosestPosition(Slot slot, int mouseX, int mouseY) {
        int sx = this.x + slot.x;
        int sy = this.y + slot.y;

        int distLeft = mouseX - sx;
        int distRight = (sx + 16) - mouseX;
        int distTop = mouseY - sy;
        int distBottom = (sy + 16) - mouseY;

        int min = Math.min(Math.min(distLeft, distRight), Math.min(distTop, distBottom));

        if (mouseX < sx - 2 || mouseX > sx + 18 || mouseY < sy - 2 || mouseY > sy + 18) return null;

        if (min == distLeft) return SeparatorPosition.LEFT;
        if (min == distRight) return SeparatorPosition.RIGHT;
        if (min == distTop) return SeparatorPosition.TOP;
        return SeparatorPosition.BOTTOM;
    }

    @Unique
    private void drawSeparatorLine(DrawContext context, Slot slot, SeparatorPosition pos, int color, int thickness) {
        int sx = this.x + slot.x;
        int sy = this.y + slot.y;

        // NOTA: Hemos quitado la variable zLevel y el argumento extra en fill.
        switch (pos) {
            case LEFT -> context.fill(sx - 1, sy, sx - 1 + thickness, sy + 16, color);
            case RIGHT -> context.fill(sx + 16 - 1, sy, sx + 16 - 1 + thickness, sy + 16, color);
            case TOP -> context.fill(sx, sy - 1, sx + 16, sy - 1 + thickness, color);
            case BOTTOM -> context.fill(sx, sy + 16 - 1, sx + 16, sy + 16 - 1 + thickness, color);
        }
    }
}