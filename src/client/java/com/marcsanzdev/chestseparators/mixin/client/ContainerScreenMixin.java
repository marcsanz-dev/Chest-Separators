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

                    if (EditorSession.getInstance().isEraserMode()) {
                        // Si estamos en modo goma, borramos el separador si existe
                        ConfigManager.getInstance().getSlotSeparators(this.currentContainerId, this.focusedSlot.id).remove(data);
                        ConfigManager.getInstance().save();
                    } else {
                        // Si no, comportamiento normal (añadir/quitar)
                        ConfigManager.getInstance().toggleSeparator(this.currentContainerId, this.focusedSlot.id, data);
                    }
                    info.setReturnValue(true);
                }
            }
        }
    }

    @Unique
    private void drawPalette(DrawContext context, int mouseX, int mouseY) {
        int startX = this.x + PALETTE_X_OFFSET;
        int startY = this.y + 10;
        int[] colors = EditorSession.PALETTE;

        // 1. DIBUJAR LOS 16 COLORES (8 filas x 2 columnas)
        for (int i = 0; i < colors.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx = startX + (col * (PALETTE_BOX_SIZE + 2));
            int by = startY + (row * (PALETTE_BOX_SIZE + 2));

            // Cuadrado de color
            context.fill(bx, by, bx + PALETTE_BOX_SIZE, by + PALETTE_BOX_SIZE, colors[i] | 0xFF000000);

            // Borde de selección o hover
            int borderColor = 0;
            if (!EditorSession.getInstance().isEraserMode() && i == EditorSession.getInstance().getSelectedColorIndex()) {
                borderColor = 0xFFFFFFFF; // Blanco si este color está seleccionado
            } else if (mouseX >= bx && mouseX < bx + PALETTE_BOX_SIZE && mouseY >= by && mouseY < by + PALETTE_BOX_SIZE) {
                borderColor = 0xFFAAAAAA; // Gris hover
            }

            if (borderColor != 0) {
                drawSimpleBorder(context, bx, by, borderColor);
            }
        }

        // 2. DIBUJAR HERRAMIENTAS (Debajo de los colores)
        int toolY = startY + (8 * (PALETTE_BOX_SIZE + 2)) + 5;

        // GOMA (Rosa)
        drawToolButton(context, mouseX, mouseY, startX, toolY, 0xFFFFAFCD, EditorSession.getInstance().isEraserMode());

        // CLEAR (Rojo con una 'X' blanca dibujada con fill)
        int binX = startX + PALETTE_BOX_SIZE + 2;
        drawToolButton(context, mouseX, mouseY, binX, toolY, 0xFFFF4444, false);
        context.fill(binX + 3, toolY + 3, binX + 9, toolY + 4, 0xFFFFFFFF); // Icono minimalista de papelera
    }

    @Unique
    private void drawSimpleBorder(DrawContext context, int bx, int by, int color) {
        context.fill(bx - 1, by - 1, bx + PALETTE_BOX_SIZE + 1, by, color);
        context.fill(bx - 1, by + PALETTE_BOX_SIZE, bx + PALETTE_BOX_SIZE + 1, by + PALETTE_BOX_SIZE + 1, color);
        context.fill(bx - 1, by, bx, by + PALETTE_BOX_SIZE, color);
        context.fill(bx + PALETTE_BOX_SIZE, by, bx + PALETTE_BOX_SIZE + 1, by + PALETTE_BOX_SIZE, color);
    }

    @Unique
    private void drawToolButton(DrawContext context, int mx, int my, int bx, int by, int color, boolean active) {
        context.fill(bx, by, bx + PALETTE_BOX_SIZE, by + PALETTE_BOX_SIZE, color);
        if (active) {
            // Borde blanco si está seleccionado
            context.fill(bx - 1, by - 1, bx + PALETTE_BOX_SIZE + 1, by, 0xFFFFFFFF);
            context.fill(bx - 1, by + PALETTE_BOX_SIZE, bx + PALETTE_BOX_SIZE + 1, by + PALETTE_BOX_SIZE + 1, 0xFFFFFFFF);
            context.fill(bx - 1, by, bx, by + PALETTE_BOX_SIZE, 0xFFFFFFFF);
            context.fill(bx + PALETTE_BOX_SIZE, by, bx + PALETTE_BOX_SIZE + 1, by + PALETTE_BOX_SIZE, 0xFFFFFFFF);
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
                EditorSession.getInstance().setEraserMode(false); // Al elegir color, quitamos la goma
                return true;
            }
        }

        // Check de Herramientas
        int toolY = startY + (8 * (PALETTE_BOX_SIZE + 2)) + 5;

        // Clic en GOMA
        if (isHovering(mouseX, mouseY, startX, toolY)) {
            EditorSession.getInstance().setEraserMode(!EditorSession.getInstance().isEraserMode());
            return true;
        }

        // Clic en CLEAR (Papelera)
        if (isHovering(mouseX, mouseY, startX + PALETTE_BOX_SIZE + 2, toolY)) {
            // Borramos TODOS los slots de esta ID de cofre
            for (Slot slot : this.handler.slots) {
                ConfigManager.getInstance().getSlotSeparators(this.currentContainerId, slot.id).clear();
            }
            ConfigManager.getInstance().save();
            return true;
        }

        return false;
    }

    @Unique
    private boolean isHovering(double mx, double my, int bx, int by) {
        return mx >= bx && mx < bx + PALETTE_BOX_SIZE && my >= by && my < by + PALETTE_BOX_SIZE;
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

                // Filtro de eje (mantiene el trazo recto)
                if (pos != null && EditorSession.getInstance().isPosAllowed(pos)) {

                    // Creamos el objeto de datos para buscarlo o borrarlo
                    // (El color da igual para la goma, pero lo necesitamos para identificar la posición)
                    int currentColor = EditorSession.getInstance().getSelectedColor();
                    SeparatorData data = new SeparatorData(pos, currentColor);

                    if (EditorSession.getInstance().isEraserMode()) {
                        // --- MODO GOMA ARRASTRABLE ---
                        // Obtenemos los separadores actuales del slot
                        Set<SeparatorData> currentSeparators = ConfigManager.getInstance().getSlotSeparators(this.currentContainerId, this.focusedSlot.id);

                        // Buscamos si hay ALGÚN separador en esa posición (sin importar el color) y lo quitamos
                        boolean removed = currentSeparators.removeIf(s -> s.position() == pos);

                        if (removed) {
                            ConfigManager.getInstance().save();
                        }
                    } else {
                        // --- MODO PINTAR ARRASTRABLE ---
                        Set<SeparatorData> currentSeparators = ConfigManager.getInstance().getSlotSeparators(this.currentContainerId, this.focusedSlot.id);
                        if (!currentSeparators.contains(data)) {
                            ConfigManager.getInstance().toggleSeparator(this.currentContainerId, this.focusedSlot.id, data);
                        }
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