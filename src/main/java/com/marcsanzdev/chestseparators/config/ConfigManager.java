package com.marcsanzdev.chestseparators.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    public static final Logger LOGGER = LoggerFactory.getLogger("ChestSeparators");
    // Ruta: .minecraft/config/chestseparators_data.json
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("chestseparators_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Memoria: "mundo@x,y,z" -> [0, 4, 26] (Indices de slots con separador)
    private final Map<String, Set<Integer>> separatorData;

    private static final ConfigManager INSTANCE = new ConfigManager();

    private ConfigManager() {
        this.separatorData = new HashMap<>();
        load();
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    public void load() {
        // Si no existe el archivo, lo creamos vacío
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            // Leemos el JSON y lo convertimos al Mapa
            Type type = new TypeToken<Map<String, Set<Integer>>>(){}.getType();
            Map<String, Set<Integer>> loaded = GSON.fromJson(reader, type);

            if (loaded != null) {
                this.separatorData.clear();
                this.separatorData.putAll(loaded);
                LOGGER.info("ChestSeparators: Cargados datos de " + separatorData.size() + " contenedores.");
            }
        } catch (IOException e) {
            LOGGER.error("ChestSeparators: Error cargando config", e);
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this.separatorData, writer);
        } catch (IOException e) {
            LOGGER.error("ChestSeparators: Error guardando config", e);
        }
    }

    // --- Métodos de Lógica (API Interna) ---

    /**
     * Obtiene los slots separados para un contenedor específico.
     */
    public Set<Integer> getSeparators(String uniqueId) {
        return separatorData.computeIfAbsent(uniqueId, k -> new HashSet<>());
    }

    /**
     * Añade o quita un separador en un slot y guarda inmediatamente.
     */
    public void toggleSeparator(String uniqueId, int slotIndex) {
        Set<Integer> slots = getSeparators(uniqueId);

        if (slots.contains(slotIndex)) {
            slots.remove(slotIndex); // Si existe, lo borra
        } else {
            slots.add(slotIndex);    // Si no existe, lo añade
        }

        // Limpieza: si un cofre se queda vacío de separadores, borramos la entrada para ahorrar espacio en disco
        if (slots.isEmpty()) {
            separatorData.remove(uniqueId);
        }

        save();
    }
}