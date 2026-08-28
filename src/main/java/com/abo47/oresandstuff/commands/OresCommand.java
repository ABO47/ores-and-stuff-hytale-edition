package com.abo47.oresandstuff.commands;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class OresCommand extends AbstractCommand {

    public OresCommand(String name, String description) {
        super(name, description);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        context.sendMessage(Message.raw("Hello from OresCommand!"));
        return CompletableFuture.completedFuture(null);
    }
}
