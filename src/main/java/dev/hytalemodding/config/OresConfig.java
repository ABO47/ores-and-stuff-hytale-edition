package dev.hytalemodding.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class OresConfig {

    public static final BuilderCodec<OresConfig> CODEC = BuilderCodec.builder(OresConfig.class, OresConfig::new)
            .append(
                new KeyedCodec<>("EnableWelcomeMessage", Codec.BOOLEAN),
                (exConfig, aBoolean, extraInfo) -> exConfig.enabledWelcomeMessage = aBoolean,
                (exConfig, extraInfo) -> exConfig.enabledWelcomeMessage
            )
            .add()
            .build();

    private boolean enabledWelcomeMessage;

    private OresConfig() {}

    public boolean isEnabledWelcomeMessage() {
        return enabledWelcomeMessage;
    }
}
