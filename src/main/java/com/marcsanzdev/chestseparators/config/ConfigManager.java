package com.marcsanzdev.chestseparators.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.marcsanzdev.chestseparators.core.SeparatorData;
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
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("chestseparators_data_v2.json"); // Cambiamos nombre por si acaso
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();

    // ESTRUCTURA NUEVA Y COMPLEJA:
    // String (ID Cofre) -> Map<Integer (Slot ID), Set<SeparatorData> (Lista de líneas en ese slot)>
    private final Map<String, Map<Integer, Set<SeparatorData>>> masterData;

    private static final ConfigManager INSTANCE = new ConfigManager();

    private ConfigManager() {
        this.masterData = new HashMap<>();
        load();
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            // Definimos el tipo complejo para GSON
            Type type = new TypeToken<Map<String, Map<Integer, Set<SeparatorData>>>>(){}.getType();
            Map<String, Map<Integer, Set<SeparatorData>>> loaded = GSON.fromJson(reader, type);

            if (loaded != null) {
                this.masterData.clear();
                this.masterData.putAll(loaded);
            }
            LOGGER.info("ChestSeparators V2 cargado.");
        } catch (IOException e) {
            LOGGER.error("Error cargando config V2", e);
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this.masterData, writer);
        } catch (IOException e) {
            LOGGER.error("Error guardando config V2", e);
        }
    }

    // --- NUEVA API INTERNA ---

    /**
     * Obtiene todos los separadores de un slot específico en un cofre específico.
     * Nunca retorna null, siempre un Set (vacío o con datos).
     */
    public Set<SeparatorData> getSlotSeparators(String chestId, int slotId) {
        return masterData
                .computeIfAbsent(chestId, k -> new HashMap<>())
                .computeIfAbsent(slotId, k -> new HashSet<>());
    }

    /**
     * Añade o quita un separador específico.
     */
    public void toggleSeparator(String chestId, int slotId, SeparatorData data) {
        Set<SeparatorData> slotData = getSlotSeparators(chestId, slotId);

        if (slotData.contains(data)) {
            slotData.remove(data); // Si existe (misma posición), lo borra
        } else {
            slotData.add(data);    // Si no, lo añade
        }

        // Limpieza profunda para no guardar basura en el JSON
        if (slotData.isEmpty()) {
            masterData.get(chestId).remove(slotId);
        }
        if (masterData.get(chestId).isEmpty()) {
            masterData.remove(chestId);
        }

        save();
    }
}