package com.abo47.oresandstuff.extraction;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.abo47.oresandstuff.OresAndStuffPlugin;
import com.abo47.oresandstuff.config.GlobalSettings;
import com.abo47.oresandstuff.config.MiningMode;
import com.abo47.oresandstuff.config.OreNodeConfig;
import com.abo47.oresandstuff.config.PickaxeEntry;
import com.abo47.oresandstuff.config.PickaxeLoader;
import com.abo47.oresandstuff.node.OreNodeComponent;

import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.logging.Level;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ManualExtractionHandler {

    private static final Map<UUID, Long> LAST_HIT = new ConcurrentHashMap<>();
    private static final Random RNG = new Random();

    private ManualExtractionHandler() {}

    public static class DamageSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {
        public DamageSystem() { super(DamageBlockEvent.class); }
        @Override public Query<EntityStore> getQuery() { return Query.any(); }
        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull DamageBlockEvent event) {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            onDamageBlock(event, store, ref);
        }
    }

    public static class BreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        public BreakSystem() { super(BreakBlockEvent.class); }
        @Override public Query<EntityStore> getQuery() { return Query.any(); }
        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull BreakBlockEvent event) {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            onBreakBlock(event, store, ref);
        }
    }

    public static void onDamageBlock(DamageBlockEvent event, Store<EntityStore> store, Ref<EntityStore> ref) {
        Vector3i pos = event.getTargetBlock();
        World world = store.getExternalData().getWorld();
        if (world == null) return;
        var data = com.abo47.oresandstuff.node.NodeTracker.get(world.getName(), pos.x, pos.y, pos.z);
        if (data == null) {
            var blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z);
            if (blockRef == null || !blockRef.isValid()) return;
            var chunkStore = world.getChunkStore().getStore();
            if (chunkStore.getComponent(blockRef, OreNodeComponent.getComponentType()) == null) return;
            data = new com.abo47.oresandstuff.node.NodeTracker.NodeData("oresandstuff:iron", 100.0, "Rock_Stone");
        }

        ItemStack hand = event.getItemInHand();
        if (hand == null || hand.isEmpty()) {
            event.setCancelled(true);
            return;
        }
        String itemId = hand.getItemId();
        PickaxeEntry spec = findSpec(itemId);
        if (spec == null) {
            event.setCancelled(true);
            return;
        }

        var playerComp = store.getComponent(ref, PlayerRef.getComponentType());
        UUID uuid = playerComp != null ? playerComp.getUuid() : null;
        long now = System.currentTimeMillis();
        if (uuid != null) {
            Long last = LAST_HIT.get(uuid);
            int cooldownMs = (int) (spec.cooldown_ticks * 50.0 / Math.max(0.1, data.quality / 100.0));
            if (last != null && now - last < cooldownMs) {
                event.setCancelled(true);
                return;
            }
            LAST_HIT.put(uuid, now);
        }

        OreNodeConfig cfg = resolveConfig(data.nodeId);
        if (cfg == null) {
            event.setCancelled(true);
            return;
        }

        int rolls = Math.max(1, (int) Math.round(spec.extract_amount * Math.max(0.01, data.quality / 100.0)));
        boolean gave = false;
        for (int i = 0; i < rolls; i++) {
            for (Map.Entry<String, Integer> e : cfg.getDrops().entrySet()) {
                int chance = e.getValue();
                if (chance >= 100 || RNG.nextInt(100) < chance) {
                    grantItem(store, ref, pos, e.getKey(), 1);
                    gave = true;
                }
            }
        }
        if (!gave) {
            String fallback = cfg.getOutputItem();
            if (fallback != null && !fallback.isBlank()) grantItem(store, ref, pos, fallback, 1);
        }
        event.setCancelled(true);
    }

    public static void onBreakBlock(BreakBlockEvent event, Store<EntityStore> store, Ref<EntityStore> ref) {
        Vector3i pos = event.getTargetBlock();
        World world = store.getExternalData().getWorld();
        if (world == null) return;
        if (com.abo47.oresandstuff.node.NodeTracker.contains(world.getName(), pos.x, pos.y, pos.z)) {
            event.setCancelled(true);
            return;
        }
        var blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z);
        if (blockRef == null || !blockRef.isValid()) return;
        var chunkStore = world.getChunkStore().getStore();
        if (chunkStore.getComponent(blockRef, OreNodeComponent.getComponentType()) != null) {
            event.setCancelled(true);
        }
    }

    private static PickaxeEntry findSpec(String itemId) {
        return PickaxeLoader.getAll().get(itemId);
    }

    private static OreNodeConfig resolveConfig(String nodeId) {
        return com.abo47.oresandstuff.config.NodeConfigLoader.loadAll().get(nodeId);
    }

    private static void grantItem(Store<EntityStore> store, Ref<EntityStore> ref, Vector3i pos, String itemId, int count) {
        try {
            Item item = Item.getAssetMap().getAsset(itemId);
            if (item == null) {
                OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Drop item id not registered, skipping: " + itemId);
                return;
            }
            ItemStack stack = new ItemStack(itemId, count);

            if (GlobalSettings.getMiningMode() == MiningMode.DROP) {
                World w = store.getExternalData().getWorld();
                Vector3d dropPos = new Vector3d(pos.x + 0.5, pos.y + 1.1, pos.z + 0.5);
                if (w != null) {
                    w.execute(() -> {
                        try {
                            var holder = ItemComponent.generateItemDrop(store, stack, dropPos, Rotation3f.IDENTITY, 0f, 0.25f, 0f);
                            if (holder != null) {
                                var dropped = holder.getComponent(ItemComponent.getComponentType());
                                if (dropped != null) dropped.setPickupDelay(ItemComponent.PICKUP_DELAY_DROPPED);
                                store.addEntity(holder, AddReason.SPAWN);
                            }
                        } catch (Exception ex) {
                            OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Drop spawn failed for " + itemId + ": " + ex);
                        }
                    });
                }
            } else {
                var inv = InventoryComponent.getCombined(store, ref, InventoryComponent.HOTBAR_FIRST);
                inv.addItemStack(stack);
            }
        } catch (Exception e) {
            OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Failed to grant item " + itemId + ": " + e);
        }
    }
}
