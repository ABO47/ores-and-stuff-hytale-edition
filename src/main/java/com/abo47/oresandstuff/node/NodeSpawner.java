package com.abo47.oresandstuff.node;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;

import com.abo47.oresandstuff.OresAndStuffPlugin;
import com.abo47.oresandstuff.config.OreNodeConfig;
import com.abo47.oresandstuff.config.QualityVisual;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

public final class NodeSpawner {

    private NodeSpawner() {}

    private static String resolveBlock(String id) {
        if (id == null || id.isBlank()) return "Ore_Node";
        int idx = BlockType.getAssetMap().getIndex(id);
        if (idx >= 0) {
            BlockType bt = BlockType.getAssetMap().getAsset(idx);
            if (bt != null && bt != BlockType.EMPTY && bt != BlockType.UNKNOWN) return id;
        }
        return id;
    }

    public static void onChunkPreLoad(@Nonnull final ChunkPreLoadProcessEvent event) {
        if (!event.isNewlyGenerated()) return;
        WorldChunk chunk = event.getChunk();
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        Map<String, OreNodeConfig> nodes = com.abo47.oresandstuff.config.NodeConfigLoader.loadAll();
        if (nodes.isEmpty()) return;

        long worldSeed = world.getWorldConfig().getSeed();
        Random chunkRand = new Random(worldSeed ^ ((long) chunkX * 0x9E3779B1L) ^ ((long) chunkZ * 0xC2B2AE3DL));
        List<Placement> placements = new ArrayList<>();

        for (Map.Entry<String, OreNodeConfig> entry : nodes.entrySet()) {
            String nodeId = entry.getKey();
            OreNodeConfig cfg = entry.getValue();
            if (!cfg.isEnabled()) continue;
            List<String> dims = cfg.getDimensionList();
            if (!dims.isEmpty() && !dims.contains(world.getName())) continue;

            long nodeSeed = chunkRand.nextLong() ^ ((long) nodeId.hashCode() * 0x1000193L);
            Random rng = new Random(nodeSeed);

            int attempts = cfg.getPlacementAttempts();
            int clusters = randInt(rng, cfg.getMinPerChunk(), cfg.getMaxPerChunk());
            Set<Long> used = new HashSet<>();
            boolean surface = cfg.isSurfacePlacement();
            int minSpacing = cfg.getMinSpacingBlocks();

            for (int c = 0; c < clusters; c++) {
                double span = Math.max(0.0, cfg.getQualityMax() - cfg.getQualityMin());
                double quality = cfg.getQualityMin() + rng.nextDouble() * span;
                QualityVisual vis = cfg.resolveVisual(quality, world.getName());
                String blockId = vis != null ? vis.visualBlock : cfg.getOutputItem();
                if (blockId == null || blockId.isBlank()) blockId = "Ore_Node";
                else blockId = resolveBlock(blockId);

                boolean placedCluster = false;
                for (int attempt = 0; attempt < attempts && !placedCluster; attempt++) {
                    int cx = rng.nextInt(ChunkUtil.SIZE);
                    int cz = rng.nextInt(ChunkUtil.SIZE);
                    int cy = randInt(rng, cfg.getMinY(), cfg.getMaxY());

                    if (minSpacing > 0 && isTooCloseToExisting(world.getName(), chunkX, chunkZ, cx, cz, cy, minSpacing, placements)) {
                        continue;
                    }

                    List<int[]> cluster = generateConnectedCluster(rng, cx, cy, cz, cfg.getClusterRadius(), cfg.getScatterCount(), surface, cfg);
                    if (cluster.isEmpty()) continue;

                    for (int[] pos : cluster) {
                        int x = pos[0], y = pos[1], z = pos[2];
                        long key = surface ? (((long) x << 21) | ((long) z << 42)) : (((long) x << 21) | y | ((long) z << 42));
                        if (!used.add(key)) continue;
                        int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkX, x);
                        int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, z);
                        placements.add(new Placement(worldX, y, worldZ, nodeId, quality, surface, blockId));
                    }
                    placedCluster = true;
                }
            }
        }

        if (placements.isEmpty()) return;

        world.execute(() -> {
            int placed = 0;
            for (Placement p : placements) {
                int y = p.surface ? findSurfaceY(world, p.x, p.z) : p.y;
                if (y < 0) y = p.y;
                String blockId = resolveBlock(p.blockId);
                try {
                    world.setBlock(p.x, y, p.z, blockId);
                } catch (IllegalArgumentException ex) {
                    OresAndStuffPlugin.get().getLogger().at(Level.WARNING).log("Failed to place " + p.blockId + " at " + p.x + "," + y + "," + p.z + ": " + ex.getMessage());
                    continue;
                }
                NodeTracker.add(world.getName(), p.x, y, p.z, p.nodeId, p.quality, p.blockId);
                placed++;
            }
            if (placed > 0) {
                OresAndStuffPlugin.get().getLogger().at(Level.INFO).log("Spawned " + placed + " ore blocks in chunk " + chunkX + "," + chunkZ + " (" + world.getName() + ")");
            }
        });
    }

    private static List<int[]> generateConnectedCluster(Random rng, int cx, int cy, int cz, int radius, int count, boolean surface, OreNodeConfig cfg) {
        List<int[]> cluster = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        cluster.add(new int[]{cx, cy, cz});
        seen.add(key(cx, cy, cz, surface));
        int attempts = 0;
        while (cluster.size() < count && attempts < count * 12) {
            attempts++;
            int[] base = cluster.get(rng.nextInt(cluster.size()));
            int dx = rng.nextInt(3) - 1;
            int dy = surface ? 0 : rng.nextInt(3) - 1;
            int dz = rng.nextInt(3) - 1;
            if (dx == 0 && dy == 0 && dz == 0) continue;
            int nx = clamp(base[0] + dx, 0, ChunkUtil.SIZE - 1);
            int nz = clamp(base[2] + dz, 0, ChunkUtil.SIZE - 1);
            int ny = surface ? base[1] : clamp(base[1] + dy, cfg.getMinY(), cfg.getMaxY());
            if (Math.max(Math.abs(nx - cx), Math.max(Math.abs(ny - cy), Math.abs(nz - cz))) > radius) continue;
            long k = key(nx, ny, nz, surface);
            if (seen.contains(k)) continue;
            seen.add(k);
            cluster.add(new int[]{nx, ny, nz});
        }
        return cluster;
    }

    private static long key(int x, int y, int z, boolean surface) {
        return surface ? (((long) x << 21) | ((long) z << 42)) : (((long) x << 21) | y | ((long) z << 42));
    }

    private static boolean isTooCloseToExisting(String worldName, int chunkX, int chunkZ, int cx, int cz, int cy, int minSpacing, List<Placement> placements) {
        if (minSpacing <= 0) return false;
        int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkX, cx);
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, cz);
        for (Placement p : placements) {
            double dx = p.x - worldX, dz = p.z - worldZ, dy = p.y - cy;
            if (Math.sqrt(dx*dx + dz*dz + dy*dy) < minSpacing) return true;
        }
        var nearest = NodeTracker.nearest(worldName, worldX, cy, worldZ);
        if (nearest != null) {
            double dx = nearest.x - worldX, dz = nearest.z - worldZ, dy = nearest.y - cy;
            if (Math.sqrt(dx*dx + dz*dz + dy*dy) < minSpacing) return true;
        }
        return false;
    }

    private static int randInt(Random rng, int min, int max) {
        if (max <= min) return min;
        return min + rng.nextInt(max - min + 1);
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static int findSurfaceY(World world, int x, int z) {
        for (int y = ChunkUtil.HEIGHT - 1; y >= ChunkUtil.MIN_Y; y--) {
            if (world.getBlock(x, y, z) != BlockType.EMPTY_ID) return y;
        }
        return -1;
    }

    private static final class Placement {
        final int x, y, z;
        final String nodeId;
        final double quality;
        final boolean surface;
        final String blockId;
        Placement(int x, int y, int z, String nodeId, double quality, boolean surface, String blockId) {
            this.x = x; this.y = y; this.z = z; this.nodeId = nodeId; this.quality = quality; this.surface = surface; this.blockId = blockId;
        }
    }
}
