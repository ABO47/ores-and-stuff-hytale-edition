package com.abo47.oresandstuff.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OreNodeConfigDeserializer implements JsonDeserializer<OreNodeConfig> {

    @Override
    public OreNodeConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        OreNodeConfig cfg = new OreNodeConfig();
        if (!json.isJsonObject()) return cfg;
        JsonObject o = json.getAsJsonObject();

        cfg.id = getString(o, "id", cfg.id);
        cfg.outputItem = getString(o, "output_item", cfg.outputItem);
        cfg.drops = getMapStringInt(o, "drops", cfg.drops);
        cfg.enabled = getBoolean(o, "enabled", cfg.enabled);
        cfg.hardness = getDouble(o, "hardness", cfg.hardness);
        cfg.baseRatePerSecond = getDouble(o, "base_rate_per_second", cfg.baseRatePerSecond);
        cfg.scannerColor = getString(o, "scanner_color", cfg.scannerColor);
        cfg.scannerRadius = getInt(o, "scanner_radius", cfg.scannerRadius);
        cfg.qualityMin = getDouble(o, "quality_min", cfg.qualityMin);
        cfg.qualityMax = getDouble(o, "quality_max", cfg.qualityMax);
        cfg.qualityVisuals = getQualityVisuals(o, cfg.qualityVisuals);
        cfg.dimensions = getStringList(o, "dimensions", cfg.dimensions);
        cfg.biomes = getBiomesMap(o, cfg.biomes);
        cfg.biomeOverrides = getBiomeOverrides(o, cfg.biomeOverrides);
        cfg.minNodesPerChunk = getInt(o, "min_nodes_per_chunk", cfg.minNodesPerChunk);
        cfg.maxNodesPerChunk = getInt(o, "max_nodes_per_chunk", cfg.maxNodesPerChunk);
        cfg.maxMinersPerNode = getInt(o, "max_miners_per_node", cfg.maxMinersPerNode);
        cfg.minSpacingBlocks = getInt(o, "min_spacing_blocks", cfg.minSpacingBlocks);
        cfg.placementAttempts = getInt(o, "placement_attempts", cfg.placementAttempts);
        cfg.clusterRadius = getInt(o, "cluster_radius", cfg.clusterRadius);
        cfg.scatterCount = getInt(o, "scatter_count", cfg.scatterCount);
        cfg.surfaceSpawn = getBoolean(o, "surface_spawn", cfg.surfaceSpawn);
        cfg.minY = getInt(o, "min_y", cfg.minY);
        cfg.maxY = getInt(o, "max_y", cfg.maxY);
        return cfg;
    }

    private String getString(JsonObject o, String key, String def) {
        if (o.has(key) && o.get(key).isJsonPrimitive()) {
            try { return o.get(key).getAsString(); } catch (Exception ignored) {}
        }
        return def;
    }

    private double getDouble(JsonObject o, String key, double def) {
        if (o.has(key) && o.get(key).isJsonPrimitive()) {
            try { return o.get(key).getAsDouble(); } catch (Exception ignored) {}
        }
        return def;
    }

    private int getInt(JsonObject o, String key, int def) {
        if (o.has(key) && o.get(key).isJsonPrimitive()) {
            try { return o.get(key).getAsInt(); } catch (Exception ignored) {}
        }
        return def;
    }

    private boolean getBoolean(JsonObject o, String key, boolean def) {
        if (o.has(key) && o.get(key).isJsonPrimitive()) {
            try { return o.get(key).getAsBoolean(); } catch (Exception ignored) {}
            try { return Boolean.parseBoolean(o.get(key).getAsString()); } catch (Exception ignored) {}
        }
        return def;
    }

    private Map<String, Integer> getMapStringInt(JsonObject o, String key, Map<String, Integer> def) {
        if (!o.has(key) || !o.get(key).isJsonObject()) return def;
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject(key).entrySet()) {
            try { out.put(e.getKey(), e.getValue().getAsInt()); } catch (Exception ignored) {}
        }
        return out;
    }

    private List<String> getStringList(JsonObject o, String key, List<String> def) {
        if (!o.has(key)) return def;
        JsonElement el = o.get(key);
        List<String> out = new ArrayList<>();
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) try { out.add(e.getAsString()); } catch (Exception ignored) {}
            return out;
        } else if (el.isJsonPrimitive()) {
            String s = el.getAsString();
            if (!s.isBlank()) {
                for (String p : s.split(",")) {
                    String t = p.trim();
                    if (!t.isEmpty()) out.add(t);
                }
                return out;
            }
        }
        return def;
    }

    private Map<String, Integer> getBiomesMap(JsonObject o, Map<String, Integer> def) {
        if (!o.has("biomes") || !o.get("biomes").isJsonObject()) return def;
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("biomes").entrySet()) {
            try { out.put(e.getKey(), e.getValue().getAsInt()); } catch (Exception ignored) {}
        }
        return out;
    }

    private List<QualityVisual> getQualityVisuals(JsonObject o, List<QualityVisual> def) {
        if (!o.has("quality_visuals") || !o.get("quality_visuals").isJsonArray()) return def;
        List<QualityVisual> out = new ArrayList<>();
        for (JsonElement el : o.getAsJsonArray("quality_visuals")) {
            if (!el.isJsonObject()) continue;
            JsonObject q = el.getAsJsonObject();
            QualityVisual qv = new QualityVisual();
            qv.min = getDouble(q, "min", 20.0);
            qv.max = getDouble(q, "max", 200.0);
            qv.nodeBlock = getString(q, "node_block", "Rock_Stone");
            qv.visualBlock = getString(q, "visual_block", "Ore_Iron_Stone");
            qv.dimensions = getStringList(q, "dimensions", new ArrayList<>());
            out.add(qv);
        }
        return out;
    }

    private Map<String, BiomeOverride> getBiomeOverrides(JsonObject o, Map<String, BiomeOverride> def) {
        if (!o.has("biome_overrides") || !o.get("biome_overrides").isJsonObject()) return def;
        Map<String, BiomeOverride> out = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("biome_overrides").entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            JsonObject q = e.getValue().getAsJsonObject();
            BiomeOverride bo = new BiomeOverride();
            if (q.has("quality_min")) bo.qualityMin = q.get("quality_min").getAsDouble();
            if (q.has("quality_max")) bo.qualityMax = q.get("quality_max").getAsDouble();
            if (q.has("min_y")) bo.minY = q.get("min_y").getAsInt();
            if (q.has("max_y")) bo.maxY = q.get("max_y").getAsInt();
            if (q.has("surface_spawn")) try { bo.surfaceSpawn = q.get("surface_spawn").getAsBoolean(); } catch (Exception ignored) { bo.surfaceSpawn = Boolean.parseBoolean(q.get("surface_spawn").getAsString()); }
            if (q.has("cluster_radius")) bo.clusterRadius = q.get("cluster_radius").getAsInt();
            if (q.has("scatter_count")) bo.scatterCount = q.get("scatter_count").getAsInt();
            if (q.has("min_nodes_per_chunk")) bo.minNodesPerChunk = q.get("min_nodes_per_chunk").getAsInt();
            if (q.has("max_nodes_per_chunk")) bo.maxNodesPerChunk = q.get("max_nodes_per_chunk").getAsInt();
            if (q.has("min_spacing_blocks")) bo.minSpacingBlocks = q.get("min_spacing_blocks").getAsInt();
            if (q.has("placement_attempts")) bo.placementAttempts = q.get("placement_attempts").getAsInt();
            out.put(e.getKey(), bo);
        }
        return out;
    }
}
