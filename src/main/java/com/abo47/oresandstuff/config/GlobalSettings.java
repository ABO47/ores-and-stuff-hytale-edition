package com.abo47.oresandstuff.config;

import java.util.HashMap;
import java.util.Map;

public final class GlobalSettings {

    public Map<String, Boolean> vanilla_ores = defaultVanillaOres();

    public String mining_mode = "drop";

    private static GlobalSettings current = null;

    public static void setCurrent(GlobalSettings settings) {
        current = settings;
    }

    public static GlobalSettings current() {
        return current != null ? current : new GlobalSettings();
    }

    public static MiningMode getMiningMode() {
        GlobalSettings s = current();
        if (s.mining_mode == null) return MiningMode.INVENTORY;
        try {
            return MiningMode.valueOf(s.mining_mode.trim().toUpperCase());
        } catch (Exception ignored) {
            return MiningMode.INVENTORY;
        }
    }

    public static boolean isVanillaOreEnabled(String metal) {
        return current().vanilla_ores.getOrDefault(normalize(metal), true);
    }

    private static String normalize(String metal) {
        return metal == null ? "" : metal.trim().toLowerCase();
    }

    private static Map<String, Boolean> defaultVanillaOres() {
        Map<String, Boolean> m = new HashMap<>();
        m.put("iron", false);
        m.put("copper", false);
        m.put("gold", false);
        m.put("silver", false);
        m.put("thorium", false);
        m.put("cobalt", false);
        m.put("adamantite", false);
        m.put("mithril", false);
        m.put("onyxium", false);
        m.put("prisma", false);
        return m;
    }
}
