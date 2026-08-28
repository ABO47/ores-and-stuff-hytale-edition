package dev.hytalemodding;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import dev.hytalemodding.commands.OresCommand;
import dev.hytalemodding.config.OresConfig;
import dev.hytalemodding.events.OresEvent;

import javax.annotation.Nonnull;

public class OresAndStuffPlugin extends JavaPlugin {

    private static Config<OresConfig> config = null;

    public OresAndStuffPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        config = this.withConfig("ores_config", OresConfig.CODEC);
    }

    @Override
    protected void setup() {
        config.save();
        this.getCommandRegistry().registerCommand(new OresCommand("ores", "An ores command"));
        if (getConfig().get().isEnabledWelcomeMessage()) {
            this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, OresEvent::onPlayerReady);
        }
    }

    public static Config<OresConfig> getConfig() {
        return config;
    }
}