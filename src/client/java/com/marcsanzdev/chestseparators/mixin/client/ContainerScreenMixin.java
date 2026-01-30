package com.marcsanzdev.chestseparators.mixin.client;

import com.marcsanzdev.chestseparators.client.EditorSession;
import com.marcsanzdev.chestseparators.config.ConfigManager;
import com.marcsanzdev.chestseparators.core.ContainerIdentifier;
import com.marcsanzdev.chestseparators.core.SeparatorData;
import com.marcsanzdev.chestseparators.core.SeparatorPosition;
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

    @Unique
    private String currentContainerId;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected T handler;
    @Shadow protected Slot focusedSlot;

    private static final int PALETTE_BOX_SIZE = 12;
    private static final int PALETTE_X_OFFSET = -35;

    protected ContainerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addEditorButton(CallbackInfo ci) {
        if (this.handler instanceof GenericContainerScreenHandler) {
            // Capturamos la ID real justo al abrir el cofre
            this.currentContainerId = ContainerIdentifier.getIdFromCurrentTarget();
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

            // A) Primero revisamos si el clic fue en la paleta
            if (handlePaletteClick(mouseX, mouseY)) {
                info.setReturnValue(true);
                return;
            }

            // B) Si no, revisamos los slots
            if (this.focusedSlot != null) {
                SeparatorPosition pos = getClosestPosition(this.focusedSlot, (int)mouseX, (int)mouseY);
                if (pos != null) {
                    EditorSession.getInstance().setDragAxis(pos);
                    // --- CAMBIO CLAVE: Quitamos el rojo y usamos el color de la sesión ---
                    int selectedColor = EditorSession.getInstance().getSelectedColor();
                    SeparatorData data = new SeparatorData(pos, selectedColor);

                    ConfigManager.getInstance().toggleSeparator(this.currentContainerId, this.focusedSlot.id, data);
                    info.setReturnValue(true);
                }
            }
        }
    }

    // --- 3. DIBUJO DE LA PALETA (CORREGIDO POSICIÓN IZQUIERDA) ---
    @Unique
    private void drawPalette(DrawContext context, int mouseX, int mouseY) {
        // Ahora restamos el offset a la izquierda (this.x + PALETTE_X_OFFSET)
        int startX = this.x + PALETTE_X_OFFSET;
        int startY = this.y + 10;

        int[] colors = EditorSession.PALETTE;

        for (int i = 0; i < colors.length; i++) {
            int col = i % 2;
            int row = i / 2;

            int bx = startX + (col * (PALETTE_BOX_SIZE + 2));
            int by = startY + (row * (PALETTE_BOX_SIZE + 2));

            context.fill(bx, by, bx + PALETTE_BOX_SIZE, by + PALETTE_BOX_SIZE, colors[i] | 0xFF000000);

            int borderColor = 0;
            if (i == EditorSession.getInstance().getSelectedColorIndex()) {
                borderColor = 0xFFFFFFFF; // Blanco para el seleccionado
            } else if (mouseX >= bx && mouseX < bx + PALETTE_BOX_SIZE && mouseY >= by && mouseY < by + PALETTE_BOX_SIZE) {
                borderColor = 0xFFAAAAAA; // Gris para el hover
            }

            if (borderColor != 0) {
                context.fill(bx - 1, by - 1, bx + PALETTE_BOX_SIZE + 1, by, borderColor);
                context.fill(bx - 1, by + PALETTE_BOX_SIZE, bx + PALETTE_BOX_SIZE + 1, by + PALETTE_BOX_SIZE + 1, borderColor);
                context.fill(bx - 1, by, bx, by + PALETTE_BOX_SIZE, borderColor);
                context.fill(bx + PALETTE_BOX_SIZE, by, bx + PALETTE_BOX_SIZE + 1, by + PALETTE_BOX_SIZE, borderColor);
            }
        }
    }

    // --- 4. CLIC EN PALETA (CORREGIDO POSICIÓN IZQUIERDA) ---
    @Unique
    private boolean handlePaletteClick(double mouseX, double mouseY) {
        int startX = this.x + PALETTE_X_OFFSET;
        int startY = this.y + 10;

        for (int i = 0; i < EditorSession.PALETTE.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx = startX + (col * (PALETTE_BOX_SIZE + 2));
            int by = startY + (row * (PALETTE_BOX_SIZE + 2));

            if (mouseX >= bx && mouseX < bx + PALETTE_BOX_SIZE && mouseY >= by && mouseY < by + PALETTE_BOX_SIZE) {
                EditorSession.getInstance().setSelectedColorIndex(i);
                return true;
            }
        }
        return false;
    }

    // --- RENDERIZADO (FIX MATRICES Y Z-INDEX) ---
    @Inject(method = "render", at = @At("TAIL"))
    private void renderSeparators(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!(this.handler instanceof GenericContainerScreenHandler)) return;

        // YA NO USAMOS RenderSystem NI Matrices.
        // Usaremos la altura Z (300) directamente en el método de dibujo.

        // A) DIBUJAR LÍNEAS GUARDADAS
        for (Slot slot : this.handler.slots) {
            Set<SeparatorData> separators = ConfigManager.getInstance().getSlotSeparators(this.currentContainerId, slot.id);
            if (!separators.isEmpty()) {
                for (SeparatorData data : separators) {
                    drawSeparatorLine(context, slot, data.position(), data.colorHex(), 1);
                }
            }
        }

        // B) DIBUJAR PREVIEW (Modo Editor)
        if (EditorSession.getInstance().isActive()) {
            // Texto de aviso (Z=300 para que flote encima de todo)
            context.drawText(this.textRenderer, "MODO EDITOR", this.x, this.y - 30, 0xFF5555, true);

            drawPalette(context, mouseX, mouseY);

            if (this.focusedSlot != null) {
                SeparatorPosition pos = getClosestPosition(this.focusedSlot, mouseX, mouseY);
                if (pos != null) {
                    drawSeparatorLine(context, this.focusedSlot, pos, 0xAAFFFF00, 1);
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

        // Grosor forzado a 1 píxel según tu petición
        int t = 1;

        switch (pos) {
            // LEFT: Desde la esquina superior hasta la inferior
            case LEFT -> context.fill(sx - 1, sy - 1, sx - 1 + t, sy + 16 + 1, color);

            // RIGHT: Ajustamos para que no se vea más grande (usamos sx + 17)
            case RIGHT -> context.fill(sx + 16, sy - 1, sx + 16 + t, sy + 16 + 1, color);

            // TOP: Desde el borde izquierdo hasta el derecho
            case TOP -> context.fill(sx - 1, sy - 1, sx + 16 + 1, sy - 1 + t, color);

            // BOTTOM: Ajustamos para que coincida con el borde inferior real
            case BOTTOM -> context.fill(sx - 1, sy + 16, sx + 16 + 1, sy + 16 + t, color);
        }
    }

    // --- 5. PINTADO POR ARRASTRE (CORREGIDO PARA 1.21.11) ---
    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (this.handler instanceof GenericContainerScreenHandler && EditorSession.getInstance().isActive() && button == 0) {
            if (this.focusedSlot != null) {
                SeparatorPosition pos = getClosestPosition(this.focusedSlot, (int)mouseX, (int)mouseY);

                // FILTRO: Solo permite 'pos' si coincide con el eje bloqueado en el primer clic
                if (pos != null && EditorSession.getInstance().isPosAllowed(pos)) {
                    int currentColor = EditorSession.getInstance().getSelectedColor();
                    SeparatorData data = new SeparatorData(pos, currentColor);

                    Set<SeparatorData> currentSeparators = ConfigManager.getInstance().getSlotSeparators(this.currentContainerId, this.focusedSlot.id);

                    // Evitamos el parpadeo: Solo añadimos si no existe
                    if (!currentSeparators.contains(data)) {
                        ConfigManager.getInstance().toggleSeparator(this.currentContainerId, this.focusedSlot.id, data);
                    }
                }
            }
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        // Al soltar el ratón, permitimos que el siguiente clic elija un eje nuevo
        EditorSession.getInstance().setDragAxis(null);
        return super.mouseReleased(click);
    }
}