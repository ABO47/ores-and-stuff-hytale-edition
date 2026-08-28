package com.abo47.oresandstuff.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
                if (root == null || !root.has("tools") || !root.get("tools").isJsonArray()) continue;
                for (JsonElement el : root.getAsJsonArray("tools")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    if (!o.has("item")) continue;
                    String item = o.get("item").getAsString();
                    PickaxeEntry e2 = new PickaxeEntry();
                    e2.item = item;
                    e2.extract_amount = o.has("extract_amount") ? o.get("extract_amount").getAsInt() : 1;
                    e2.cooldown_ticks = o.has("cooldown_ticks") ? o.get("cooldown_ticks").getAsInt() : 40;
                    e2.durability_cost = o.has("durability_cost") ? o.get("durability_cost").getAsInt() : 1;
                    ENTRIES.put(item, e2);
                }
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
        try {
            var plugin = OresAndStuffPlugin.get();
            if (plugin != null) {
                Path dataDir = plugin.getDataDirectory();
                if (dataDir != null) {
                    return dataDir.getParent().getParent().resolve("config").resolve("oresandstuff").resolve("pickaxes");
                }
            }
        } catch (Exception ignored) {}
        return java.nio.file.Paths.get("config", "oresandstuff", "pickaxes");
    }

    private static void generateDefaults(Path folder) {
        try (Stream<Path> s = Files.list(folder)) {
            if (s.findAny().isPresent()) return;
        } catch (IOException ignored) {}
        Path file = folder.resolve("vanilla.json");
        if (Files.isRegularFile(file)) return;
        JsonObject root = new JsonObject();
        JsonArray tools = new JsonArray();
        tools.add(tool("Tool_Pickaxe_Wood", 1, 60, 2));
        tools.add(tool("Tool_Pickaxe_Copper", 1, 50, 2));
        tools.add(tool("Tool_Pickaxe_Iron", 1, 40, 1));
        tools.add(tool("Tool_Pickaxe_Thorium", 1, 30, 2));
        tools.add(tool("Tool_Pickaxe_Cobalt", 1, 25, 1));
        tools.add(tool("Tool_Pickaxe_Adamantite", 1, 20, 1));
        tools.add(tool("Tool_Pickaxe_Mithril", 1, 15, 1));
        tools.add(tool("Tool_Pickaxe_Onyxium", 1, 10, 1));
        root.add("tools", tools);
        try {
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
            OresAndStuffPlugin.get().getLogger().at(Level.INFO).log("Generated default pickaxe config " + file);
        } catch (IOException e) {
            OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Failed to write default pickaxe config: " + e.getMessage());
        }
    }

    private static JsonObject tool(String item, int extract, int cooldown, int durability) {
        JsonObject o = new JsonObject();
        o.addProperty("item", item);
        o.addProperty("extract_amount", extract);
        o.addProperty("cooldown_ticks", cooldown);
        o.addProperty("durability_cost", durability);
        return o;
    }
}
