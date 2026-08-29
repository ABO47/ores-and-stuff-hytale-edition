package com.abo47.oresandstuff.node;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;

import com.abo47.oresandstuff.OresAndStuffPlugin;
import com.abo47.oresandstuff.config.BiomeSpawnConfig;
import com.abo47.oresandstuff.config.GlobalSettings;
import com.abo47.oresandstuff.config.OreNodeConfig;
import com.abo47.oresandstuff.config.QualityBand;
import com.abo47.oresandstuff.config.QualityVisual;
import com.abo47.oresandstuff.config.SettingsLoader;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

public final class NodeSpawner {

    private NodeSpawner() {}

    private static final double VISUAL_RATIO = 0.5;

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

        GlobalSettings settings = SettingsLoader.load();
        Set<String> disabledPrefixes = new HashSet<>();
        for (Map.Entry<String, Boolean> e : settings.vanilla_ores.entrySet()) {
            if (e.getValue() != null && !e.getValue()) {
                disabledPrefixes.add("Ore_" + capitalize(e.getKey()));
            }
        }

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
            int minSpacing = cfg.getMinSpacingBlocks();

            Map<String, Map<String, BiomeSpawnConfig>> dimMap = cfg.dimensionBiomes;
            Map<String, BiomeSpawnConfig> biomeMap = dimMap.get(world.getName());
            if (biomeMap == null) biomeMap = dimMap.get("*");
            if (biomeMap == null || biomeMap.isEmpty()) continue;

            Map<String, Double> weights = new HashMap<>();
            for (Map.Entry<String, BiomeSpawnConfig> e : biomeMap.entrySet()) {
                weights.put(e.getKey(), (double) Math.max(1, e.getValue().weight));
            }

            for (int c = 0; c < clusters; c++) {
                String biome = weightedPick(weights, rng);
                BiomeSpawnConfig bc = biomeMap.get(biome);
                if (bc == null) continue;

                int bMinY = bc.minY;
                int bMaxY = bc.maxY;
                boolean surface = bc.surfaceSpawn;
                int clusterRadius = cfg.getClusterRadius();
                int scatterCount = cfg.getScatterCount();
                int spacing = minSpacing;

                int cy = randInt(rng, bMinY, bMaxY);
                double quality = qualityForY(bc, cy, cfg.qualityMin, cfg.qualityMax, rng);

                QualityVisual vis = cfg.resolveVisual(quality, world.getName());
                String visualBlock = vis != null ? vis.visualBlock : cfg.getOutputItem();
                if (visualBlock == null || visualBlock.isBlank()) visualBlock = "Ore_Node";
                visualBlock = resolveBlock(visualBlock);
                String nodeBlock = vis != null ? vis.nodeBlock : visualBlock;
                if (nodeBlock == null || nodeBlock.isBlank()) nodeBlock = visualBlock;
                nodeBlock = resolveBlock(nodeBlock);

                boolean placedCluster = false;
                for (int attempt = 0; attempt < attempts && !placedCluster; attempt++) {
                    int cx = rng.nextInt(ChunkUtil.SIZE);
                    int cz = rng.nextInt(ChunkUtil.SIZE);

                    if (spacing > 0 && isTooCloseToExisting(world.getName(), chunkX, chunkZ, cx, cz, cy, spacing, placements)) {
                        continue;
                    }

                    List<int[]> cluster = generateConnectedCluster(rng, cx, cy, cz, clusterRadius, scatterCount, surface, bMinY, bMaxY);
                    if (cluster.isEmpty()) continue;

                    for (int[] pos : cluster) {
                        int x = pos[0], y = pos[1], z = pos[2];
                        long key = surface ? (((long) x << 21) | ((long) z << 42)) : (((long) x << 21) | y | ((long) z << 42));
                        if (!used.add(key)) continue;
                        int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkX, x);
                        int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, z);
                        String blockId = rng.nextDouble() < VISUAL_RATIO ? visualBlock : nodeBlock;
                        placements.add(new Placement(worldX, y, worldZ, nodeId, quality, surface, blockId));
                    }
                    placedCluster = true;
                }
            }
        }

        if (placements.isEmpty() && disabledPrefixes.isEmpty()) return;

        final Set<String> disabled = disabledPrefixes;
        world.execute(() -> {
            if (!disabled.isEmpty()) {
                removeVanillaOres(world, chunkX, chunkZ, disabled);
            }
            if (placements.isEmpty()) return;
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

    private static void removeVanillaOres(World world, int chunkX, int chunkZ, Set<String> disabledPrefixes) {
        int baseX = ChunkUtil.worldCoordFromLocalCoord(chunkX, 0);
        int baseZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, 0);
        int[][] offsets = {{0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}};
        for (int x = 0; x < ChunkUtil.SIZE; x++) {
            for (int z = 0; z < ChunkUtil.SIZE; z++) {
                for (int y = ChunkUtil.MIN_Y; y <= ChunkUtil.HEIGHT; y++) {
                    int id = world.getBlock(baseX + x, y, baseZ + z);
                    if (id == BlockType.EMPTY_ID) continue;
                    String key = BlockType.getAssetMap().getAsset(id).getId();
                    if (key == null || !key.startsWith("Ore_") || key.equals("Ore_Node")) continue;
                    boolean disable = false;
                    for (String p : disabledPrefixes) {
                        if (key.startsWith(p)) { disable = true; break; }
                    }
                    if (!disable) continue;
                    String filler = findFiller(world, baseX + x, y, baseZ + z, offsets);
                    if (filler != null) {
                        try {
                            world.setBlock(baseX + x, y, baseZ + z, filler);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private static String findFiller(World world, int x, int y, int z, int[][] offsets) {
        for (int[] o : offsets) {
            int nid = world.getBlock(x + o[0], y + o[1], z + o[2]);
            if (nid == BlockType.EMPTY_ID) continue;
            BlockType bt = BlockType.getAssetMap().getAsset(nid);
            if (bt == null || bt == BlockType.EMPTY || bt == BlockType.UNKNOWN) continue;
            String nkey = bt.getId();
            if (nkey == null || nkey.startsWith("Ore_") || nkey.equals(BlockType.EMPTY_KEY)) continue;
            return nkey;
        }
        return null;
    }

    private static List<int[]> generateConnectedCluster(Random rng, int cx, int cy, int cz, int radius, int count, boolean surface, int minY, int maxY) {
        List<int[]> cells = new ArrayList<>();
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int dyMax = surface ? 0 : radius;
                for (int dy = -dyMax; dy <= dyMax; dy++) {
                    int dist2 = dx * dx + dy * dy + dz * dz;
                    if (dist2 > r2) continue;
                    int nx = cx + dx, ny = cy + dy, nz = cz + dz;
                    if (!surface && (ny < minY || ny > maxY)) continue;
                    if (nx < 0 || nx >= ChunkUtil.SIZE || nz < 0 || nz >= ChunkUtil.SIZE) continue;
                    cells.add(new int[]{nx, ny, nz, dist2});
                }
            }
        }
        cells.sort((a, b) -> Integer.compare(a[3], b[3]));
        int n = Math.min(count, cells.size());
        List<int[]> result = new ArrayList<>();
        for (int i = 0; i < n; i++) result.add(new int[]{cells.get(i)[0], cells.get(i)[1], cells.get(i)[2]});
        return result;
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

    private static String weightedPick(Map<String, Double> weights, Random rng) {
        double total = 0;
        for (double w : weights.values()) total += Math.max(0.0, w);
        if (total <= 0) return null;
        double r = rng.nextDouble() * total;
        for (Map.Entry<String, Double> e : weights.entrySet()) {
            r -= Math.max(0.0, e.getValue());
            if (r <= 0) return e.getKey();
        }
        return null;
    }

    private static double qualityForY(BiomeSpawnConfig bc, int y, double fallbackMin, double fallbackMax, Random rng) {
        if (bc.qualityBands != null) {
            for (QualityBand qb : bc.qualityBands) {
                if (y >= qb.minY && y <= qb.maxY) {
                    double span = Math.max(0.0, qb.qualityMax - qb.qualityMin);
                    return qb.qualityMin + rng.nextDouble() * span;
                }
            }
        }
        double span = Math.max(0.0, fallbackMax - fallbackMin);
        return fallbackMin + rng.nextDouble() * span;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static int randInt(Random rng, int min, int max) {
        if (max <= min) return min;
        return min + rng.nextInt(max - min + 1);
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private static int findSurfaceY(World world, int x, int z) {
        for (int y = ChunkUtil.HEIGHT - 1; y >= ChunkUtil.MIN_Y; y--) {
            int id = world.getBlock(x, y, z);
            if (id == BlockType.EMPTY_ID) continue;
            String key = BlockType.getAssetMap().getAsset(id).getId();
            if (key != null && isTreeBlock(key)) continue;
            return y;
        }
        return -1;
    }

    private static boolean isTreeBlock(String key) {
        return key.startsWith("Tree_") || key.startsWith("Bush_")
                || key.contains("Leaf") || key.contains("Leaves") || key.contains("Log");
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
