package com.abo47.oresandstuff.node;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import com.abo47.oresandstuff.OresAndStuffPlugin;

import javax.annotation.Nullable;

/**
 * Block-entity component attached to every placed {@code ore_node} block.
 * Holds the data-driven identity of the node and its permanent rolled quality %.
 */
public class OreNodeComponent implements Component<ChunkStore> {

    public static final BuilderCodec<OreNodeComponent> CODEC = BuilderCodec.builder(OreNodeComponent.class, OreNodeComponent::new)
            .append(new KeyedCodec<>("NodeId", Codec.STRING), (data, value) -> data.nodeId = value, data -> data.nodeId).add()
            .append(new KeyedCodec<>("Quality", Codec.DOUBLE), (data, value) -> data.quality = value, data -> data.quality).add()
            .append(new KeyedCodec<>("VisualBlock", Codec.STRING), (data, value) -> data.visualBlock = value, data -> data.visualBlock).add()
            .append(new KeyedCodec<>("NodeBlock", Codec.STRING), (data, value) -> data.nodeBlock = value, data -> data.nodeBlock).add()
            .build();

    private String nodeId = "";
    private double quality = 0.0;
    private String visualBlock = "Ore_Iron_Stone";
    private String nodeBlock = "Rock_Stone";

    public OreNodeComponent() {}

    public OreNodeComponent(String nodeId, double quality, String visualBlock, String nodeBlock) {
        this.nodeId = nodeId;
        this.quality = quality;
        this.visualBlock = visualBlock;
        this.nodeBlock = nodeBlock;
    }

    public OreNodeComponent(String nodeId, double quality) {
        this(nodeId, quality, "Ore_Iron_Stone", "Rock_Stone");
    }

    public String getNodeId() { return nodeId; }
    public double getQuality() { return quality; }
    public String getVisualBlock() { return visualBlock; }
    public String getNodeBlock() { return nodeBlock; }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new OreNodeComponent(nodeId, quality, visualBlock, nodeBlock);
    }

    public static ComponentType<ChunkStore, OreNodeComponent> getComponentType() {
        return OresAndStuffPlugin.get().getOreNodeComponentType();
    }
}
