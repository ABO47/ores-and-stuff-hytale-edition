package com.abo47.oresandstuff.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.HashMap;
import java.util.Map;

public class OresConfig {

    public static final BuilderCodec<OresConfig> CODEC = BuilderCodec.builder(OresConfig.class, OresConfig::new)
            .append(new KeyedCodec<>("Nodes", new MapCodec<>(OreNodeConfig.CODEC, HashMap::new)),
                    (config, nodes, extraInfo) -> config.nodes = nodes,
                    (config, extraInfo) -> config.nodes)
            .add()
            .build();

    private Map<String, OreNodeConfig> nodes = new HashMap<>();

    private OresConfig() {}

    public Map<String, OreNodeConfig> getNodes() {
        return nodes;
    }
}
