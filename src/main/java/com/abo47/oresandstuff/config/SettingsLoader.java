package com.abo47.oresandstuff.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.abo47.oresandstuff.OresAndStuffPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public final class SettingsLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GlobalSettings cached = null;

    private SettingsLoader() {}

    public static Path getSettingsFile() {
        return ConfigPaths.configRoot().resolve("settings.json");
    }

    public static synchronized GlobalSettings load() {
        if (cached != null) return cached;
        Path file = getSettingsFile();
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException ignored) {}
        if (!Files.isRegularFile(file)) {
            writeDefaults(file);
        }
        try {
            GlobalSettings s = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), GlobalSettings.class);
            cached = (s != null) ? s : new GlobalSettings();
        } catch (Exception e) {
            OresAndStuffPlugin.get().getLogger().at(Level.WARNING)
                    .log("Failed to parse settings.json, using defaults: " + e.getMessage());
            cached = new GlobalSettings();
        }
        GlobalSettings.setCurrent(cached);
        return cached;
    }

    public static synchronized void reload() {
        cached = null;
        load();
    }

    private static void writeDefaults(Path file) {
        try {
            Files.writeString(file, GSON.toJson(new GlobalSettings()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            OresAndStuffPlugin.get().getLogger().at(Level.WARNING)
                    .log("Failed to write default settings.json: " + e.getMessage());
        }
    }
}
