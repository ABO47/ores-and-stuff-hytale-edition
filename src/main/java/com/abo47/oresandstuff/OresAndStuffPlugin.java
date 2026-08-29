package com.abo47.oresandstuff;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.abo47.oresandstuff.commands.OresCommand;
import com.abo47.oresandstuff.config.NodeConfigLoader;
import com.abo47.oresandstuff.config.PickaxeLoader;
import com.abo47.oresandstuff.config.SettingsLoader;
import com.abo47.oresandstuff.node.NodeSpawner;
import com.abo47.oresandstuff.node.OreNodeComponent;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class OresAndStuffPlugin extends JavaPlugin {

    private static OresAndStuffPlugin instance = null;

    private ComponentType<ChunkStore, OreNodeComponent> oreNodeComponentType = null;

    public OresAndStuffPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        int totalNodes = 0;
        try {
            var folderNodes = NodeConfigLoader.loadAll();
            PickaxeLoader.loadAll();
            SettingsLoader.load();
            totalNodes = folderNodes.size();
            getLogger().at(Level.INFO).log("Loaded " + folderNodes.size() + " orenode(s) and " + PickaxeLoader.getAll().size() + " pickaxe type(s)");
        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Failed to load folder configs: " + e.getMessage());
        }
        getLogger().at(Level.INFO).log("Ores and Stuff loaded with " + totalNodes + " effective node definition(s)");

        this.oreNodeComponentType = this.getChunkStoreRegistry()
                .registerComponent(OreNodeComponent.class, "OreNode", OreNodeComponent.CODEC);

        getEntityStoreRegistry().registerSystem(new com.abo47.oresandstuff.extraction.ManualExtractionHandler.DamageSystem());
        getEntityStoreRegistry().registerSystem(new com.abo47.oresandstuff.extraction.ManualExtractionHandler.BreakSystem());

        getEventRegistry().registerGlobal(ChunkPreLoadProcessEvent.class, NodeSpawner::onChunkPreLoad);

        this.getCommandRegistry().registerCommand(new OresCommand("ores", "Ores and stuff command"));
    }

    public static OresAndStuffPlugin get() {
        return instance;
    }

    public ComponentType<ChunkStore, OreNodeComponent> getOreNodeComponentType() {
        return oreNodeComponentType;
    }
}
