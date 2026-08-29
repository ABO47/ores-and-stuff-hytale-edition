package com.abo47.oresandstuff.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import com.abo47.oresandstuff.OresAndStuffPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class PickaxeLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, PickaxeEntry> ENTRIES = new HashMap<>();

    private PickaxeLoader() {}

    public static void loadAll() {
        ENTRIES.clear();
        Path folder = getPerWorldFolder();
        if (folder == null) return;

        try {
            Files.createDirectories(folder);
        } catch (IOException ignored) {}

        try (Stream<Path> s = Files.list(folder)) {
            if (s.findAny().isEmpty()) generateDefaults(folder);
        } catch (IOException ignored) {}

        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(folder)) {
            stream.filter(p -> p.toString().endsWith(".json")).sorted().forEach(files::add);
        } catch (IOException e) {
            OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Failed to list pickaxes folder " + folder + ": " + e.getMessage());
            return;
        }

        for (Path file : files) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                JsonObject root = GSON.fromJson(json, JsonObject.class);
                if (root == null || !root.has("item")) continue;
                String item = root.get("item").getAsString();
                PickaxeEntry e2 = new PickaxeEntry();
                e2.item = item;
                e2.extract_amount = root.has("extract_amount") ? root.get("extract_amount").getAsInt() : 1;
                e2.cooldown_ticks = root.has("cooldown_ticks") ? root.get("cooldown_ticks").getAsInt() : 40;
                e2.durability_cost = root.has("durability_cost") ? root.get("durability_cost").getAsInt() : 1;
                ENTRIES.put(item, e2);
            } catch (Exception e) {
                OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Failed to parse pickaxe config " + file + ": " + e.getMessage());
            }
        }
        OresAndStuffPlugin.get().getLogger().at(Level.INFO).log("Loaded " + ENTRIES.size() + " pickaxe tool(s)");
    }

    public static Map<String, PickaxeEntry> getAll() {
        return Map.copyOf(ENTRIES);
    }

    private static Path getPerWorldFolder() {
        return ConfigPaths.configRoot().resolve("pickaxes");
    }

    private static void generateDefaults(Path folder) {
        String[][] tools = {
            {"Tool_Pickaxe_Wood", "1", "60", "2"},
            {"Tool_Pickaxe_Copper", "1", "50", "2"},
            {"Tool_Pickaxe_Iron", "1", "40", "1"},
            {"Tool_Pickaxe_Thorium", "1", "30", "2"},
            {"Tool_Pickaxe_Cobalt", "1", "25", "1"},
            {"Tool_Pickaxe_Adamantite", "1", "20", "1"},
            {"Tool_Pickaxe_Mithril", "1", "15", "1"},
            {"Tool_Pickaxe_Onyxium", "1", "10", "1"},
        };
        for (String[] t : tools) {
            JsonObject o = new JsonObject();
            o.addProperty("item", t[0]);
            o.addProperty("extract_amount", Integer.parseInt(t[1]));
            o.addProperty("cooldown_ticks", Integer.parseInt(t[2]));
            o.addProperty("durability_cost", Integer.parseInt(t[3]));
            Path file = folder.resolve(t[0] + ".json");
            try {
                Files.writeString(file, GSON.toJson(o), StandardCharsets.UTF_8);
            } catch (IOException e) {
                OresAndStuffPlugin.get().getLogger().at(Level.WARNING)
                        .log("Failed to write default pickaxe config " + file + ": " + e.getMessage());
            }
        }
        OresAndStuffPlugin.get().getLogger().at(Level.INFO).log("Generated " + tools.length + " default pickaxe config(s)");
    }
}
