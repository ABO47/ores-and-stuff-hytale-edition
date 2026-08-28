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

public final class NodeConfigLoader {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(OreNodeConfig.class, new OreNodeConfigDeserializer())
            .setPrettyPrinting()
            .create();

    private NodeConfigLoader() {}

    public static Map<String, OreNodeConfig> loadAll() {
        Map<String, OreNodeConfig> out = new HashMap<>();
        Path folder = getPerWorldFolder();
        if (folder == null) return out;

        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Failed to create orenodes folder " + folder + ": " + e.getMessage());
            return out;
        }

        try (Stream<Path> s = Files.list(folder)) {
            if (s.findAny().isEmpty()) generateDefaults(folder);
        } catch (IOException ignored) {}

        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(folder)) {
            stream.filter(p -> p.toString().endsWith(".json")).sorted().forEach(files::add);
        } catch (IOException e) {
            OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Failed to list orenodes folder " + folder + ": " + e.getMessage());
            return out;
        }

        for (Path file : files) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                OreNodeConfig cfg = GSON.fromJson(json, OreNodeConfig.class);
                if (cfg == null) continue;
                String id = cfg.getId();
                if (id == null || id.isBlank()) {
                    String name = file.getFileName().toString();
                    if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
                    id = "oresandstuff:" + name;
                    cfg.id = id;
                }
                if (!cfg.isEnabled()) continue;
                out.put(id, cfg);
            } catch (Exception e) {
                OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Failed to parse orenode " + file + ": " + e.getMessage());
            }
        }
        return out;
    }

    private static Path getPerWorldFolder() {
        try {
            var plugin = OresAndStuffPlugin.get();
            if (plugin != null) {
                Path dataDir = plugin.getDataDirectory();
                if (dataDir != null) {
                    return dataDir.getParent().getParent().resolve("config").resolve("oresandstuff").resolve("orenodes");
                }
            }
        } catch (Exception ignored) {}
        return Paths.get("config", "oresandstuff", "orenodes");
    }

    private static void generateDefaults(Path folder) {
        try (Stream<Path> s = Files.list(folder)) {
            if (s.findAny().isPresent()) return;
        } catch (IOException ignored) {}

        Map<String, String> defaults = new HashMap<>();
        defaults.put("iron.json", defaultIronJson());
        defaults.put("copper.json", defaultCopperJson());

        for (Map.Entry<String, String> e : defaults.entrySet()) {
            try {
                Files.writeString(folder.resolve(e.getKey()), e.getValue(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Failed to write default " + e.getKey() + ": " + ex.getMessage());
            }
        }
        OresAndStuffPlugin.get().getLogger().at(Level.INFO).log("Generated " + defaults.size() + " default orenode configs in " + folder);
    }

    private static String defaultIronJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", "oresandstuff:iron");
        o.addProperty("output_item", "Ore_Iron");
        JsonObject drops = new JsonObject();
        drops.addProperty("Ore_Iron", 100);
        drops.addProperty("Rock_Stone_Cobble", 60);
        o.add("drops", drops);
        o.addProperty("enabled", true);
        o.addProperty("hardness", 80.0);
        o.addProperty("base_rate_per_second", 0.6);
        o.addProperty("scanner_color", "#D8A030");
        o.addProperty("scanner_radius", 128);
        o.addProperty("quality_min", 20.0);
        o.addProperty("quality_max", 200.0);
        com.google.gson.JsonArray visuals = new com.google.gson.JsonArray();
        visuals.add(visualTier(20.0, 110.0, "Rock_Stone", "Ore_Iron_Stone", ""));
        visuals.add(visualTier(110.0, 200.0, "Rock_Shale", "Ore_Iron_Shale", ""));
        o.add("quality_visuals", visuals);
        o.add("dimensions", new com.google.gson.JsonArray());
        JsonObject biomes = new JsonObject();
        biomes.addProperty("Env_Zone1_Plains", 25);
        biomes.addProperty("Env_Forest_Temperate", 20);
        biomes.addProperty("Env_Zone1_Desert", 10);
        biomes.addProperty("Env_Cave", 30);
        o.add("biomes", biomes);
        o.add("biome_overrides", new JsonObject());
        o.addProperty("min_nodes_per_chunk", 0);
        o.addProperty("max_nodes_per_chunk", 1);
        o.addProperty("max_miners_per_node", 1);
        o.addProperty("min_spacing_blocks", 90);
        o.addProperty("placement_attempts", 2);
        o.addProperty("cluster_radius", 2);
        o.addProperty("scatter_count", 4);
        o.addProperty("surface_spawn", true);
        o.addProperty("min_y", 0);
        o.addProperty("max_y", 63);
        return new GsonBuilder().setPrettyPrinting().create().toJson(o);
    }

    private static String defaultCopperJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", "oresandstuff:copper");
        o.addProperty("output_item", "Ore_Copper");
        JsonObject drops = new JsonObject();
        drops.addProperty("Ore_Copper", 100);
        drops.addProperty("Rock_Stone_Cobble", 60);
        o.add("drops", drops);
        o.addProperty("enabled", true);
        o.addProperty("hardness", 60.0);
        o.addProperty("base_rate_per_second", 0.7);
        o.addProperty("scanner_color", "#B87333");
        o.addProperty("scanner_radius", 128);
        o.addProperty("quality_min", 20.0);
        o.addProperty("quality_max", 180.0);
        com.google.gson.JsonArray visuals = new com.google.gson.JsonArray();
        visuals.add(visualTier(20.0, 180.0, "Rock_Stone", "Ore_Copper_Stone", ""));
        o.add("quality_visuals", visuals);
        o.add("dimensions", new com.google.gson.JsonArray());
        JsonObject biomes = new JsonObject();
        biomes.addProperty("Env_Forest_Temperate", 25);
        biomes.addProperty("Env_Zone1_Plains", 20);
        o.add("biomes", biomes);
        o.add("biome_overrides", new JsonObject());
        o.addProperty("min_nodes_per_chunk", 0);
        o.addProperty("max_nodes_per_chunk", 1);
        o.addProperty("max_miners_per_node", 1);
        o.addProperty("min_spacing_blocks", 100);
        o.addProperty("placement_attempts", 2);
        o.addProperty("cluster_radius", 2);
        o.addProperty("scatter_count", 5);
        o.addProperty("surface_spawn", true);
        o.addProperty("min_y", 0);
        o.addProperty("max_y", 63);
        return new GsonBuilder().setPrettyPrinting().create().toJson(o);
    }

    private static JsonObject visualTier(double min, double max, String nodeBlock, String visualBlock, String dimension) {
        JsonObject o = new JsonObject();
        o.addProperty("min", min);
        o.addProperty("max", max);
        o.addProperty("node_block", nodeBlock);
        o.addProperty("visual_block", visualBlock);
        if (dimension == null || dimension.isBlank()) o.add("dimensions", new com.google.gson.JsonArray());
        else o.add("dimensions", array(dimension));
        return o;
    }

    private static com.google.gson.JsonArray array(String... vals) {
        com.google.gson.JsonArray a = new com.google.gson.JsonArray();
        for (String v : vals) a.add(v);
        return a;
    }
}
