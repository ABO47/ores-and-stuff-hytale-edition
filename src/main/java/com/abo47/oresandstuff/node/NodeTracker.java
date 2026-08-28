package com.abo47.oresandstuff.node;

import com.hypixel.hytale.protocol.BlockPosition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of ore node positions, keyed by world name. Populated as nodes are placed
 * (see {@link NodeSpawner}) so commands can locate the nearest node without scanning chunks.
 * Not persisted - rebuilt each session as chunks generate.
 */
public final class NodeTracker {

    public static final class NodeData {
        public final String nodeId;
        public final double quality;
        public final String blockId;
        public NodeData(String nodeId, double quality, String blockId) {
            this.nodeId = nodeId; this.quality = quality; this.blockId = blockId;
        }
    }

    private static final Map<String, Map<BlockPosition, NodeData>> NODES = new ConcurrentHashMap<>();

    private NodeTracker() {}

    public static void add(String worldName, int x, int y, int z, String nodeId, double quality, String blockId) {
        NODES.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>()).put(new BlockPosition(x, y, z), new NodeData(nodeId, quality, blockId));
    }

    public static void add(String worldName, int x, int y, int z) {
        add(worldName, x, y, z, "oresandstuff:iron", 100.0, "Rock_Stone");
    }

    public static void remove(String worldName, int x, int y, int z) {
        Map<BlockPosition, NodeData> map = NODES.get(worldName);
        if (map != null) map.remove(new BlockPosition(x, y, z));
    }

    public static NodeData get(String worldName, int x, int y, int z) {
        Map<BlockPosition, NodeData> map = NODES.get(worldName);
        return map == null ? null : map.get(new BlockPosition(x, y, z));
    }

    public static boolean contains(String worldName, int x, int y, int z) {
        return get(worldName, x, y, z) != null;
    }

    public static BlockPosition nearest(String worldName, double x, double y, double z) {
        Map<BlockPosition, NodeData> map = NODES.get(worldName);
        if (map == null || map.isEmpty()) return null;
        BlockPosition best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPosition p : map.keySet()) {
            double dx = p.x - x, dy = p.y - y, dz = p.z - z;
            double d = dx*dx + dy*dy + dz*dz;
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }

    public static int count(String worldName) {
        Map<BlockPosition, NodeData> map = NODES.get(worldName);
        return map == null ? 0 : map.size();
    }
}
