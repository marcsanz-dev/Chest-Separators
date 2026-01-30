package com.marcsanzdev.chestseparators.mixin.client;

import com.marcsanzdev.chestseparators.client.EditorSession;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
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
    @Shadow protected T handler; // Necesario para saber qué tipo de inventario es

    protected ContainerScreenMixin(Text title) {
        super(title);
    }

    // 1. INICIALIZACIÓN: Filtramos para que solo salga en Cofres/Barriles
    @Inject(method = "init", at = @At("TAIL"))
    private void addEditorButton(CallbackInfo ci) {
        // FILTRO DE SEGURIDAD:
        // Solo añadimos el botón si el 'handler' es de un contenedor genérico (Cofre/Barril)
        // Esto evita que salga en el inventario del jugador, hornos, etc.
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

    // 2. RENDERIZADO: Texto de aviso
    @Inject(method = "render", at = @At("TAIL"))
    private void renderEditorOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // También verificamos aquí para no dibujar el texto en el inventario normal
        if (this.handler instanceof GenericContainerScreenHandler && EditorSession.getInstance().isActive()) {
            context.drawText(this.textRenderer, "MODO EDITOR", this.x, this.y - 30, 0xFF5555, true);
        }
    }

    // 3. CIERRE SEGURO: Usamos Inject en lugar de Override
    @Inject(method = "close", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        // Al cerrar el GUI, desactivamos el modo editor
        EditorSession.getInstance().setActive(false);
        // NOTA: Al usar Inject, el código original de Minecraft se ejecutará justo después,
        // enviando el paquete correcto al servidor. ¡Adiós Protocol Error!
    }
}