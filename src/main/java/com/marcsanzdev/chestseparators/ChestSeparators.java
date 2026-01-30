package com.marcsanzdev.chestseparators;

import com.marcsanzdev.chestseparators.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChestSeparators implements ModInitializer {
	public static final String MOD_ID = "chestseparators";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Inicializando ChestSeparators Backend...");

		// Forzamos la carga de la configuración al inicio para verificar que funciona
		ConfigManager.getInstance();
	}
}