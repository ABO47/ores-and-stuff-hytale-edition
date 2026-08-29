package com.abo47.oresandstuff.config;

import com.abo47.oresandstuff.OresAndStuffPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConfigPaths {

    private ConfigPaths() {}

    public static Path configRoot() {
        var plugin = OresAndStuffPlugin.get();
        if (plugin != null && plugin.getDataDirectory() != null) {
            Path dataDir = plugin.getDataDirectory();
            Path configDir = dataDir.getParent();
            if (configDir != null) {
                Path modsDir = configDir.getParent();
                if (modsDir != null) {
                    return modsDir.resolve("oresandstuff");
                }
                return configDir.resolve("oresandstuff");
            }
        }
        return Paths.get("oresandstuff");
    }
}
