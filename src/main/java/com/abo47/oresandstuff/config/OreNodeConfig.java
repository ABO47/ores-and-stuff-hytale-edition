package com.abo47.oresandstuff.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class OreNodeConfig {

    public static final BuilderCodec<OreNodeConfig> CODEC = BuilderCodec.builder(OreNodeConfig.class, OreNodeConfig::new)
            .append(new KeyedCodec<>("OutputItem", Codec.STRING),
                    (config, value, extraInfo) -> config.outputItem = value,
                    (config, extraInfo) -> config.outputItem)
            .add()
            .append(new KeyedCodec<>("BaseRatePerSecond", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.baseRatePerSecond = value,
                    (config, extraInfo) -> config.baseRatePerSecond)
            .add()
            .append(new KeyedCodec<>("QualityMin", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.qualityMin = value,
                    (config, extraInfo) -> config.qualityMin)
            .add()
            .append(new KeyedCodec<>("QualityMax", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.qualityMax = value,
                    (config, extraInfo) -> config.qualityMax)
            .add()
            .append(new KeyedCodec<>("Hardness", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.hardness = value,
                    (config, extraInfo) -> config.hardness)
            .add()
            .build();

    private String outputItem = "";
    private double baseRatePerSecond = 0.5;
    private double qualityMin = 20.0;
    private double qualityMax = 200.0;
    private double hardness = 80.0;

    private OreNodeConfig() {}

    public String getOutputItem() {
        return outputItem;
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
}
