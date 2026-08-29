package com.abo47.oresandstuff.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.abo47.oresandstuff.OresAndStuffPlugin;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        return ConfigPaths.configRoot().resolve("orenodes");
    }

    private static void generateDefaults(Path folder) {
        try (Stream<Path> s = Files.list(folder)) {
            if (s.findAny().isPresent()) return;
        } catch (IOException ignored) {}

        Map<String, String> defaults = new LinkedHashMap<>();
        for (OreSpec spec : allOres()) {
            defaults.put(spec.fileName, buildOreJson(spec));
        }

        for (Map.Entry<String, String> e : defaults.entrySet()) {
            try {
                Files.writeString(folder.resolve(e.getKey()), e.getValue(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Failed to write default " + e.getKey() + ": " + ex.getMessage());
            }
        }
        OresAndStuffPlugin.get().getLogger().at(Level.INFO).log("Generated " + defaults.size() + " default orenode configs in " + folder);
    }

    private static String buildOreJson(OreSpec spec) {
        JsonObject o = new JsonObject();
        o.addProperty("id", spec.id);
        o.addProperty("output_item", spec.outputItem);
        JsonObject drops = new JsonObject();
        for (var e : spec.drops.entrySet()) drops.addProperty(e.getKey(), e.getValue());
        o.add("drops", drops);
        o.addProperty("enabled", true);
        o.addProperty("hardness", spec.hardness);
        o.addProperty("base_rate_per_second", spec.baseRate);
        o.addProperty("scanner_color", spec.scannerColor);
        o.addProperty("scanner_radius", spec.scannerRadius);
        o.addProperty("quality_min", spec.qualityMin);
        o.addProperty("quality_max", spec.qualityMax);
        o.addProperty("surface_spawn", spec.surface);
        o.addProperty("min_y", spec.minY);
        o.addProperty("max_y", spec.maxY);
        o.addProperty("min_nodes_per_chunk", 0);
        o.addProperty("max_nodes_per_chunk", 1);
        o.addProperty("max_miners_per_node", 1);
        o.addProperty("min_spacing_blocks", spec.spacing);
        o.addProperty("placement_attempts", 2);
        o.addProperty("cluster_radius", spec.cluster);
        o.addProperty("scatter_count", spec.scatter);
        JsonArray visuals = new JsonArray();
        for (Tier t : spec.tiers) {
            JsonObject v = new JsonObject();
            v.addProperty("min", t.min);
            v.addProperty("max", t.max);
            v.addProperty("node_block", safeBlock(t.nodeBlock));
            v.addProperty("visual_block", fixVisualBlock(t.visualBlock, t.nodeBlock));
            v.add("dimensions", new JsonArray());
            visuals.add(v);
        }
        o.add("quality_visuals", visuals);
        o.add("dimensions", new JsonArray());
        JsonObject dims = new JsonObject();
        for (Map.Entry<String, Map<String, BiomeSpec>> dimEntry : spec.dimensionBiomes.entrySet()) {
            JsonObject biomesObj = new JsonObject();
            for (Map.Entry<String, BiomeSpec> biomeEntry : dimEntry.getValue().entrySet()) {
                BiomeSpec b = biomeEntry.getValue();
                JsonObject bo = new JsonObject();
                bo.addProperty("weight", b.weight);
                bo.addProperty("surface_spawn", b.surface);
                bo.addProperty("min_y", b.minY);
                bo.addProperty("max_y", b.maxY);
                JsonArray bands = new JsonArray();
                for (Band band : b.bands) {
                    JsonObject qb = new JsonObject();
                    qb.addProperty("min_y", band.minY);
                    qb.addProperty("max_y", band.maxY);
                    qb.addProperty("quality_min", band.qMin);
                    qb.addProperty("quality_max", band.qMax);
                    bands.add(qb);
                }
                bo.add("quality_bands", bands);
                biomesObj.add(biomeEntry.getKey(), bo);
            }
            dims.add(dimEntry.getKey(), biomesObj);
        }
        o.add("dimension_biomes", dims);
        return GSON.toJson(o);
    }

    private static Band band(int minY, int maxY, double qMin, double qMax) {
        return new Band(minY, maxY, qMin, qMax);
    }

    private static BiomeSpec biome(int weight, int minY, int maxY, boolean surface, Band... bands) {
        BiomeSpec b = new BiomeSpec();
        b.weight = weight;
        b.minY = minY;
        b.maxY = maxY;
        b.surface = surface;
        for (Band band : bands) b.bands.add(band);
        return b;
    }

    private static List<OreSpec> allOres() {
        List<OreSpec> ores = new ArrayList<>();

        OreSpec iron = new OreSpec();
        iron.id = "oresandstuff:iron";
        iron.fileName = "iron.json";
        iron.outputItem = "Ore_Iron";
        iron.scannerColor = "#D8A030";
        iron.qualityMin = 20.0; iron.qualityMax = 200.0;
        iron.hardness = 80.0; iron.baseRate = 0.6;
        iron.minY = 0; iron.maxY = 63; iron.spacing = 90;         iron.cluster = 5; iron.scatter = 18; iron.surface = true;
        iron.tiers.add(new Tier(20.0, 90.0, "Rock_Stone", "Ore_Iron_Stone"));
        iron.tiers.add(new Tier(90.0, 150.0, "Rock_Shale", "Ore_Iron_Shale"));
        iron.tiers.add(new Tier(150.0, 200.0, "Rock_Basalt", "Ore_Iron_Basalt"));
        iron.drops.put("Ore_Iron", 100);
        iron.drops.put("Rock_Stone_Cobble", 60);
        Map<String, BiomeSpec> ironBiomes = new LinkedHashMap<>();
        ironBiomes.put("Env_Zone1_Caves", biome(30, 8, 50, false, band(8, 25, 80, 140), band(25, 50, 140, 200)));
        ironBiomes.put("Env_Zone2_Caves", biome(25, 8, 50, false, band(8, 25, 80, 140), band(25, 50, 140, 200)));
        ironBiomes.put("Env_Zone3_Caves", biome(20, 12, 55, false, band(12, 30, 100, 150), band(30, 55, 150, 200)));
        ironBiomes.put("Env_Zone1_Plains", biome(25, 0, 40, true, band(0, 40, 20, 120)));
        ironBiomes.put("Env_Zone1_Forests", biome(20, 0, 45, true, band(0, 45, 20, 130)));
        iron.dimensionBiomes.put("*", ironBiomes);
        ores.add(iron);

        OreSpec copper = new OreSpec();
        copper.id = "oresandstuff:copper";
        copper.fileName = "copper.json";
        copper.outputItem = "Ore_Copper";
        copper.scannerColor = "#B87333";
        copper.qualityMin = 20.0; copper.qualityMax = 180.0;
        copper.hardness = 60.0; copper.baseRate = 0.7;
        copper.minY = 0; copper.maxY = 63; copper.spacing = 90;         copper.cluster = 5; copper.scatter = 18; copper.surface = true;
        copper.tiers.add(new Tier(20.0, 90.0, "Rock_Stone", "Ore_Copper_Stone"));
        copper.tiers.add(new Tier(90.0, 180.0, "Rock_Volcanic", "Ore_Copper_Volcanic"));
        copper.drops.put("Ore_Copper", 100);
        copper.drops.put("Rock_Stone_Cobble", 60);
        Map<String, BiomeSpec> copperBiomes = new LinkedHashMap<>();
        copperBiomes.put("Env_Zone1_Forests", biome(25, 0, 45, true, band(0, 45, 20, 130)));
        copperBiomes.put("Env_Zone1_Plains", biome(20, 0, 40, true, band(0, 40, 20, 120)));
        copperBiomes.put("Env_Zone1_Caves", biome(22, 8, 50, false, band(8, 25, 60, 120), band(25, 50, 120, 180)));
        copperBiomes.put("Env_Zone2_Caves", biome(18, 8, 50, false, band(8, 25, 60, 120), band(25, 50, 120, 180)));
        copperBiomes.put("Env_Zone3_Caves", biome(14, 12, 55, false, band(12, 30, 80, 140), band(30, 55, 140, 180)));
        copper.dimensionBiomes.put("*", copperBiomes);
        ores.add(copper);

        OreSpec gold = new OreSpec();
        gold.id = "oresandstuff:gold";
        gold.fileName = "gold.json";
        gold.outputItem = "Ore_Gold";
        gold.scannerColor = "#FCEA52";
        gold.qualityMin = 30.0; gold.qualityMax = 220.0;
        gold.hardness = 90.0; gold.baseRate = 0.5;
        gold.minY = 10; gold.maxY = 50; gold.spacing = 110;         gold.cluster = 5; gold.scatter = 14; gold.surface = false;
        gold.tiers.add(new Tier(30.0, 110.0, "Rock_Stone", "Ore_Gold_Stone"));
        gold.tiers.add(new Tier(110.0, 170.0, "Rock_Basalt", "Ore_Gold_Basalt"));
        gold.tiers.add(new Tier(170.0, 220.0, "Rock_Volcanic", "Ore_Gold_Volcanic"));
        gold.drops.put("Ore_Gold", 100);
        gold.drops.put("Rock_Stone_Cobble", 50);
        gold.drops.put("Rock_Basalt_Cobble", 30);
        Map<String, BiomeSpec> goldBiomes = new LinkedHashMap<>();
        goldBiomes.put("Env_Zone1_Caves", biome(18, 10, 50, false, band(10, 30, 80, 160), band(30, 50, 160, 220)));
        goldBiomes.put("Env_Zone2_Caves", biome(15, 10, 50, false, band(10, 30, 80, 160), band(30, 50, 160, 220)));
        goldBiomes.put("Env_Zone3_Caves", biome(12, 10, 52, false, band(10, 32, 90, 170), band(32, 52, 170, 220)));
        goldBiomes.put("Env_Zone1_Caves_Volcanic_T1", biome(8, 10, 55, false, band(10, 32, 100, 180), band(32, 55, 180, 220)));
        goldBiomes.put("Env_Zone2_Caves_Volcanic_T1", biome(8, 10, 55, false, band(10, 32, 100, 180), band(32, 55, 180, 220)));
        goldBiomes.put("Env_Zone4_Volcanoes", biome(6, 10, 55, false, band(10, 32, 100, 180), band(32, 55, 180, 220)));
        gold.dimensionBiomes.put("*", goldBiomes);
        ores.add(gold);

        OreSpec silver = new OreSpec();
        silver.id = "oresandstuff:silver";
        silver.fileName = "silver.json";
        silver.outputItem = "Ore_Silver";
        silver.scannerColor = "#C7C9D1";
        silver.qualityMin = 40.0; silver.qualityMax = 240.0;
        silver.hardness = 95.0; silver.baseRate = 0.5;
        silver.minY = 15; silver.maxY = 52; silver.spacing = 110;         silver.cluster = 5; silver.scatter = 14; silver.surface = false;
        silver.tiers.add(new Tier(40.0, 120.0, "Rock_Stone", "Ore_Silver_Stone"));
        silver.tiers.add(new Tier(120.0, 180.0, "Rock_Basalt", "Ore_Silver_Basalt"));
        silver.tiers.add(new Tier(180.0, 240.0, "Rock_Volcanic", "Ore_Silver_Volcanic"));
        silver.drops.put("Ore_Silver", 100);
        silver.drops.put("Rock_Basalt_Cobble", 40);
        Map<String, BiomeSpec> silverBiomes = new LinkedHashMap<>();
        silverBiomes.put("Env_Zone2_Caves", biome(14, 15, 52, false, band(15, 32, 100, 180), band(32, 52, 180, 240)));
        silverBiomes.put("Env_Zone3_Caves", biome(12, 15, 52, false, band(15, 32, 100, 180), band(32, 52, 180, 240)));
        silverBiomes.put("Env_Zone1_Caves_Volcanic_T1", biome(8, 15, 55, false, band(15, 35, 110, 190), band(35, 55, 190, 240)));
        silverBiomes.put("Env_Zone2_Caves_Volcanic_T1", biome(8, 15, 55, false, band(15, 35, 110, 190), band(35, 55, 190, 240)));
        silverBiomes.put("Env_Zone3_Caves_Volcanic_T1", biome(6, 15, 55, false, band(15, 35, 110, 190), band(35, 55, 190, 240)));
        silver.dimensionBiomes.put("*", silverBiomes);
        ores.add(silver);

        OreSpec cobalt = new OreSpec();
        cobalt.id = "oresandstuff:cobalt";
        cobalt.fileName = "cobalt.json";
        cobalt.outputItem = "Ore_Cobalt";
        cobalt.scannerColor = "#3B6FB6";
        cobalt.qualityMin = 60.0; cobalt.qualityMax = 260.0;
        cobalt.hardness = 110.0; cobalt.baseRate = 0.45;
        cobalt.minY = 20; cobalt.maxY = 55; cobalt.spacing = 130;         cobalt.cluster = 5; cobalt.scatter = 16; cobalt.surface = false;
        cobalt.tiers.add(new Tier(60.0, 140.0, "Rock_Shale", "Ore_Cobalt_Shale"));
        cobalt.tiers.add(new Tier(140.0, 200.0, "Rock_Slate", "Ore_Cobalt_Slate"));
        cobalt.tiers.add(new Tier(200.0, 260.0, "Rock_Volcanic", "Ore_Cobalt_Volcanic"));
        cobalt.drops.put("Ore_Cobalt", 100);
        cobalt.drops.put("Rock_Slate_Cobble", 40);
        Map<String, BiomeSpec> cobaltBiomes = new LinkedHashMap<>();
        cobaltBiomes.put("Env_Zone1_Caves_Volcanic_T2", biome(10, 20, 55, false, band(20, 38, 120, 200), band(38, 55, 200, 260)));
        cobaltBiomes.put("Env_Zone2_Caves_Volcanic_T2", biome(10, 20, 55, false, band(20, 38, 120, 200), band(38, 55, 200, 260)));
        cobaltBiomes.put("Env_Zone3_Caves_Volcanic_T2", biome(8, 20, 55, false, band(20, 38, 120, 200), band(38, 55, 200, 260)));
        cobaltBiomes.put("Env_Zone4_Volcanoes", biome(10, 20, 55, false, band(20, 38, 120, 200), band(38, 55, 200, 260)));
        cobaltBiomes.put("Env_Zone4_Wastes", biome(6, 20, 55, false, band(20, 38, 120, 200), band(38, 55, 200, 260)));
        cobalt.dimensionBiomes.put("*", cobaltBiomes);
        ores.add(cobalt);

        OreSpec mithril = new OreSpec();
        mithril.id = "oresandstuff:mithril";
        mithril.fileName = "mithril.json";
        mithril.outputItem = "Ore_Mithril";
        mithril.scannerColor = "#8FD4D0";
        mithril.qualityMin = 70.0; mithril.qualityMax = 280.0;
        mithril.hardness = 120.0; mithril.baseRate = 0.45;
        mithril.minY = 25; mithril.maxY = 60; mithril.spacing = 150;         mithril.cluster = 5; mithril.scatter = 16; mithril.surface = false;
        mithril.tiers.add(new Tier(70.0, 150.0, "Rock_Slate", "Ore_Mithril_Slate"));
        mithril.tiers.add(new Tier(150.0, 220.0, "Rock_Magma", "Ore_Mithril_Magma"));
        mithril.tiers.add(new Tier(220.0, 280.0, "Rock_Volcanic", "Ore_Mithril_Volcanic"));
        mithril.drops.put("Ore_Mithril", 100);
        mithril.drops.put("Rock_Slate_Cobble", 40);
        Map<String, BiomeSpec> mithrilBiomes = new LinkedHashMap<>();
        mithrilBiomes.put("Env_Zone2_Caves_Volcanic_T3", biome(8, 25, 60, false, band(25, 42, 140, 220), band(42, 60, 220, 280)));
        mithrilBiomes.put("Env_Zone3_Caves_Volcanic_T3", biome(8, 25, 60, false, band(25, 42, 140, 220), band(42, 60, 220, 280)));
        mithrilBiomes.put("Env_Zone4_Volcanoes", biome(8, 25, 60, false, band(25, 42, 140, 220), band(42, 60, 220, 280)));
        mithrilBiomes.put("Env_Zone4_Wastes", biome(6, 25, 60, false, band(25, 42, 140, 220), band(42, 60, 220, 280)));
        mithrilBiomes.put("Env_Zone4_Crucible", biome(5, 25, 60, false, band(25, 42, 140, 220), band(42, 60, 220, 280)));
        mithril.dimensionBiomes.put("*", mithrilBiomes);
        ores.add(mithril);

        OreSpec thorium = new OreSpec();
        thorium.id = "oresandstuff:thorium";
        thorium.fileName = "thorium.json";
        thorium.outputItem = "Ore_Thorium";
        thorium.scannerColor = "#B5651D";
        thorium.qualityMin = 60.0; thorium.qualityMax = 260.0;
        thorium.hardness = 110.0; thorium.baseRate = 0.45;
        thorium.minY = 20; thorium.maxY = 55; thorium.spacing = 130;         thorium.cluster = 5; thorium.scatter = 16; thorium.surface = false;
        thorium.tiers.add(new Tier(60.0, 140.0, "Rock_Stone", "Ore_Thorium_Stone"));
        thorium.tiers.add(new Tier(140.0, 200.0, "Rock_Mud", "Ore_Thorium_Mud"));
        thorium.tiers.add(new Tier(200.0, 260.0, "Rock_Sandstone", "Ore_Thorium_Sandstone"));
        thorium.drops.put("Ore_Thorium", 100);
        thorium.drops.put("Rock_Stone_Cobble", 40);
        Map<String, BiomeSpec> thoriumBiomes = new LinkedHashMap<>();
        thoriumBiomes.put("Env_Zone1_Swamps", biome(10, 20, 55, false, band(20, 38, 120, 200), band(38, 55, 200, 260)));
        thoriumBiomes.put("Env_Zone2_Oasis", biome(8, 20, 55, false, band(20, 38, 120, 200), band(38, 55, 200, 260)));
        thoriumBiomes.put("Env_Zone3_Forests", biome(8, 20, 55, false, band(20, 38, 120, 200), band(38, 55, 200, 260)));
        thoriumBiomes.put("Env_Zone4_Forests", biome(8, 20, 55, false, band(20, 38, 120, 200), band(38, 55, 200, 260)));
        thoriumBiomes.put("Env_Zone4_Jungles", biome(8, 20, 55, false, band(20, 38, 120, 200), band(38, 55, 200, 260)));
        thorium.dimensionBiomes.put("*", thoriumBiomes);
        ores.add(thorium);

        OreSpec adamantite = new OreSpec();
        adamantite.id = "oresandstuff:adamantite";
        adamantite.fileName = "adamantite.json";
        adamantite.outputItem = "Ore_Adamantite";
        adamantite.scannerColor = "#5FE0C0";
        adamantite.qualityMin = 80.0; adamantite.qualityMax = 300.0;
        adamantite.hardness = 140.0; adamantite.baseRate = 0.4;
        adamantite.minY = 30; adamantite.maxY = 63; adamantite.spacing = 170;         adamantite.cluster = 5; adamantite.scatter = 20; adamantite.surface = false;
        adamantite.tiers.add(new Tier(80.0, 160.0, "Rock_Slate", "Ore_Adamantite_Slate"));
        adamantite.tiers.add(new Tier(160.0, 240.0, "Rock_Volcanic", "Ore_Adamantite_Volcanic"));
        adamantite.tiers.add(new Tier(240.0, 300.0, "Rock_Basalt", "Ore_Adamantite_Basalt"));
        adamantite.drops.put("Ore_Adamantite", 100);
        adamantite.drops.put("Rock_Basalt_Cobble", 40);
        Map<String, BiomeSpec> adamantiteBiomes = new LinkedHashMap<>();
        adamantiteBiomes.put("Env_Zone3_Caves_Volcanic_T3", biome(8, 30, 63, false, band(30, 46, 160, 240), band(46, 63, 240, 300)));
        adamantiteBiomes.put("Env_Zone4_Caves_Volcanic", biome(10, 30, 63, false, band(30, 46, 160, 240), band(46, 63, 240, 300)));
        adamantiteBiomes.put("Env_Zone4_Volcanoes", biome(10, 30, 63, false, band(30, 46, 160, 240), band(46, 63, 240, 300)));
        adamantiteBiomes.put("Env_Zone4_Wastes", biome(8, 30, 63, false, band(30, 46, 160, 240), band(46, 63, 240, 300)));
        adamantiteBiomes.put("Env_Zone4_Crucible", biome(8, 30, 63, false, band(30, 46, 160, 240), band(46, 63, 240, 300)));
        adamantite.dimensionBiomes.put("*", adamantiteBiomes);
        ores.add(adamantite);

        OreSpec onyxium = new OreSpec();
        onyxium.id = "oresandstuff:onyxium";
        onyxium.fileName = "onyxium.json";
        onyxium.outputItem = "Ore_Onyxium";
        onyxium.scannerColor = "#6B4FA0";
        onyxium.qualityMin = 90.0; onyxium.qualityMax = 320.0;
        onyxium.hardness = 150.0; onyxium.baseRate = 0.4;
        onyxium.minY = 25; onyxium.maxY = 60; onyxium.spacing = 180;         onyxium.cluster = 5; onyxium.scatter = 20; onyxium.surface = false;
        onyxium.tiers.add(new Tier(90.0, 170.0, "Rock_Stone", "Ore_Onyxium_Stone"));
        onyxium.tiers.add(new Tier(170.0, 250.0, "Rock_Shale", "Ore_Onyxium_Shale"));
        onyxium.tiers.add(new Tier(250.0, 320.0, "Rock_Volcanic", "Ore_Onyxium_Volcanic"));
        onyxium.drops.put("Ore_Onyxium", 100);
        onyxium.drops.put("Rock_Stone_Cobble", 40);
        Map<String, BiomeSpec> onyxiumBiomes = new LinkedHashMap<>();
        onyxiumBiomes.put("Env_Zone3_Mountains", biome(10, 25, 60, false, band(25, 42, 170, 250), band(42, 60, 250, 320)));
        onyxiumBiomes.put("Env_Zone3_Caves_Mountains", biome(10, 25, 60, false, band(25, 42, 170, 250), band(42, 60, 250, 320)));
        onyxiumBiomes.put("Env_Zone4_Forests", biome(6, 25, 60, false, band(25, 42, 170, 250), band(42, 60, 250, 320)));
        onyxiumBiomes.put("Env_Zone4_Jungles", biome(6, 25, 60, false, band(25, 42, 170, 250), band(42, 60, 250, 320)));
        onyxiumBiomes.put("Env_Zone4_Volcanoes", biome(4, 25, 60, false, band(25, 42, 170, 250), band(42, 60, 250, 320)));
        onyxium.dimensionBiomes.put("*", onyxiumBiomes);
        ores.add(onyxium);

        OreSpec prisma = new OreSpec();
        prisma.id = "oresandstuff:prisma";
        prisma.fileName = "prisma.json";
        prisma.outputItem = "Ore_Prisma";
        prisma.scannerColor = "#FF6AD5";
        prisma.qualityMin = 100.0; prisma.qualityMax = 340.0;
        prisma.hardness = 170.0; prisma.baseRate = 0.35;
        prisma.minY = 35; prisma.maxY = 63; prisma.spacing = 200;         prisma.cluster = 5; prisma.scatter = 24; prisma.surface = false;
        prisma.tiers.add(new Tier(100.0, 340.0, "Rock_Volcanic", "Ore_Prisma"));
        prisma.drops.put("Ore_Prisma", 100);
        Map<String, BiomeSpec> prismaBiomes = new LinkedHashMap<>();
        prismaBiomes.put("Env_Zone4_Crucible", biome(6, 35, 63, false, band(35, 50, 180, 280), band(50, 63, 280, 340)));
        prismaBiomes.put("Env_Zone4_Wastes", biome(6, 35, 63, false, band(35, 50, 180, 280), band(50, 63, 280, 340)));
        prismaBiomes.put("Env_Zone4_Volcanoes", biome(5, 35, 63, false, band(35, 50, 180, 280), band(50, 63, 280, 340)));
        prismaBiomes.put("Env_Zone3_Caves_Volcanic_T3", biome(4, 35, 63, false, band(35, 50, 180, 280), band(50, 63, 280, 340)));
        prisma.dimensionBiomes.put("*", prismaBiomes);
        ores.add(prisma);

        return ores;
    }

    private static boolean assetsReady() {
        try {
            return BlockType.getAssetMap().getIndex("Rock_Stone") >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isValidBlock(String id) {
        if (id == null || id.isBlank()) return false;
        if (!assetsReady()) return true;
        try {
            int idx = BlockType.getAssetMap().getIndex(id);
            if (idx < 0) return false;
            BlockType bt = BlockType.getAssetMap().getAsset(idx);
            return bt != null && bt != BlockType.EMPTY && bt != BlockType.UNKNOWN;
        } catch (Exception e) {
            return true;
        }
    }

    private static String safeBlock(String id) {
        return isValidBlock(id) ? id : "Rock_Stone";
    }

    private static String fixVisualBlock(String visual, String nodeBlock) {
        if (isValidBlock(visual)) return visual;
        if (visual != null && visual.startsWith("Ore_")) {
            String[] parts = visual.split("_");
            if (parts.length >= 3) {
                String stone = "Ore_" + parts[1] + "_Stone";
                if (isValidBlock(stone)) return stone;
            }
        }
        return isValidBlock(nodeBlock) ? nodeBlock : "Rock_Stone";
    }

    private static final class OreSpec {
        String id;
        String fileName;
        String outputItem;
        String scannerColor = "#FFFFFF";
        int scannerRadius = 128;
        double qualityMin = 20.0;
        double qualityMax = 200.0;
        double hardness = 80.0;
        double baseRate = 0.6;
        int minY = 0;
        int maxY = 63;
        int spacing = 90;
        int cluster = 5;
        int scatter = 4;
        boolean surface = true;
        Map<String, Integer> drops = new LinkedHashMap<>();
        List<Tier> tiers = new ArrayList<>();
        Map<String, Map<String, BiomeSpec>> dimensionBiomes = new LinkedHashMap<>();
    }

    private static final class BiomeSpec {
        int weight = 1;
        int minY = 0;
        int maxY = 63;
        boolean surface = false;
        List<Band> bands = new ArrayList<>();
    }

    private static final class Band {
        final int minY;
        final int maxY;
        final double qMin;
        final double qMax;
        Band(int minY, int maxY, double qMin, double qMax) {
            this.minY = minY;
            this.maxY = maxY;
            this.qMin = qMin;
            this.qMax = qMax;
        }
    }

    private static final class Tier {
        final double min;
        final double max;
        final String nodeBlock;
        final String visualBlock;
        Tier(double min, double max, String nodeBlock, String visualBlock) {
            this.min = min;
            this.max = max;
            this.nodeBlock = nodeBlock;
            this.visualBlock = visualBlock;
        }
    }
}
