package com.abo47.oresandstuff.config;

import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OreNodeConfig {

    public static final BuilderCodec<OreNodeConfig> CODEC = BuilderCodec.builder(OreNodeConfig.class, OreNodeConfig::new).build();

    @SerializedName("id")
    public String id = "oresandstuff:iron";

    @SerializedName("output_item")
    public String outputItem = "Ore_Iron";

    @SerializedName("drops")
    public Map<String, Integer> drops = new HashMap<>();

    @SerializedName("enabled")
    public boolean enabled = true;

    @SerializedName("hardness")
    public double hardness = 80.0;

    @SerializedName("base_rate_per_second")
    public double baseRatePerSecond = 0.6;

    @SerializedName("scanner_color")
    public String scannerColor = "#D8A030";

    @SerializedName("scanner_radius")
    public int scannerRadius = 128;

    @SerializedName("quality_min")
    public double qualityMin = 20.0;

    @SerializedName("quality_max")
    public double qualityMax = 200.0;

    @SerializedName("quality_visuals")
    public List<QualityVisual> qualityVisuals = new ArrayList<>();

    @SerializedName("dimensions")
    public List<String> dimensions = new ArrayList<>();

    @SerializedName("dimension_biomes")
    public Map<String, Map<String, BiomeSpawnConfig>> dimensionBiomes = new HashMap<>();

    @SerializedName("min_nodes_per_chunk")
    public int minNodesPerChunk = 0;

    @SerializedName("max_nodes_per_chunk")
    public int maxNodesPerChunk = 1;

    @SerializedName("max_miners_per_node")
    public int maxMinersPerNode = 1;

    @SerializedName("min_spacing_blocks")
    public int minSpacingBlocks = 90;

    @SerializedName("placement_attempts")
    public int placementAttempts = 2;

    @SerializedName("cluster_radius")
    public int clusterRadius = 5;

    @SerializedName("scatter_count")
    public int scatterCount = 4;

    @SerializedName("surface_spawn")
    public boolean surfaceSpawn = true;

    @SerializedName("min_y")
    public int minY = 0;

    @SerializedName("max_y")
    public int maxY = 63;

    public OreNodeConfig() {
        drops.put("Ore_Iron", 100);
        drops.put("Rock_Stone_Cobble", 60);
    }

    public String getId() {
        return id != null && !id.isBlank() ? id : "oresandstuff:iron";
    }

    public String getOutputItem() {
        return outputItem;
    }

    public Map<String, Integer> getDrops() {
        return drops;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getBaseRatePerSecond() {
        return baseRatePerSecond;
    }

    public double getQualityMin() {
        return qualityMin;
    }

    public double getQualityMax() {
        return qualityMax;
    }

    public double getHardness() {
        return hardness;
    }

    public String getScannerColor() {
        return scannerColor;
    }

    public int getScannerRadius() {
        return scannerRadius;
    }

    public List<QualityVisual> getQualityVisuals() {
        return qualityVisuals;
    }

    public List<String> getDimensionList() {
        return new ArrayList<>(dimensions);
    }

    public int getMinPerChunk() {
        return Math.max(0, minNodesPerChunk);
    }

    public int getMaxPerChunk() {
        return Math.max(getMinPerChunk(), maxNodesPerChunk);
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return Math.max(minY, maxY);
    }

    public int getClusterRadius() {
        return Math.max(0, clusterRadius);
    }

    public int getScatterCount() {
        return Math.max(1, scatterCount);
    }

    public int getSpacing() {
        return Math.max(0, minSpacingBlocks / 8);
    }

    public int getMinSpacingBlocks() {
        return Math.max(0, minSpacingBlocks);
    }

    public int getPlacementAttempts() {
        return Math.max(1, placementAttempts);
    }

    public boolean isSurfacePlacement() {
        return surfaceSpawn;
    }

    public QualityVisual resolveVisual(double quality, String dimension) {
        if (qualityVisuals == null || qualityVisuals.isEmpty()) return null;
        QualityVisual fallback = null;
        for (QualityVisual qv : qualityVisuals) {
            boolean dimOk = qv.dimensions == null || qv.dimensions.isEmpty() || qv.dimensions.contains(dimension);
            boolean qualityOk = quality >= qv.min && quality < qv.max || (quality == qv.max && qv.max == qualityMax);
            if (!dimOk || !qualityOk) continue;
            if (qv.dimensions != null && !qv.dimensions.isEmpty()) return qv;
            if (fallback == null) fallback = qv;
        }
        return fallback;
    }
}
