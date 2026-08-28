package com.abo47.oresandstuff;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.abo47.oresandstuff.commands.OresCommand;
import com.abo47.oresandstuff.config.OresConfig;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class OresAndStuffPlugin extends JavaPlugin {

    private static Config<OresConfig> config = null;

    public OresAndStuffPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        config = this.withConfig("ores_config", OresConfig.CODEC);
    }

    @Override
    protected void setup() {
        config.save();
        getLogger().at(Level.INFO).log("Ores and Stuff loaded with " + config.get().getNodes().size() + " node definition(s)");
        this.getCommandRegistry().registerCommand(new OresCommand("ores", "Ores and stuff command"));
    }

    public static Config<OresConfig> getConfig() {
        return config;
    }
}
