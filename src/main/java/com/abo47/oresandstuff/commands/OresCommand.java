package com.abo47.oresandstuff.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.abo47.oresandstuff.node.NodeTracker;

import org.joml.Vector3d;

import javax.annotation.Nonnull;

/**
 * /ores - debug helper. Teleports the player to the nearest tracked ore node.
 * Nodes are tracked as they generate (see NodeSpawner/NodeTracker), so only nodes created after
 * the mod loaded this session are findable; explore new chunks to register more.
 */
public class OresCommand extends AbstractPlayerCommand {

    public OresCommand(String name, String description) {
        super(name, description);
    }

    @Override
    protected void execute(
            @Nonnull final CommandContext commandContext,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final PlayerRef playerRef,
            @Nonnull final World world
    ) {
        final TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            playerRef.sendMessage(Message.raw("Could not read your position."));
            return;
        }

        final Vector3d pos = transform.getPosition();
        final BlockPosition target = NodeTracker.nearest(world.getName(), pos.x, pos.y, pos.z);

        if (target == null) {
            playerRef.sendMessage(Message.raw(
                    "No ore nodes tracked yet (count: " + NodeTracker.count(world.getName())
                            + "). Explore/generate new chunks first - nodes are recorded as they spawn."));
            return;
        }

        final double dx = target.x - pos.x;
        final double dy = target.y - pos.y;
        final double dz = target.z - pos.z;
        final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        final Transform destination = new Transform(target.x, target.y + 1.0, target.z);
        final Teleport teleport = Teleport.createForPlayer(world, destination);
        store.addComponent(ref, Teleport.getComponentType(), teleport);

        playerRef.sendMessage(Message.raw(
                "Teleporting to ore node at " + target.x + ", " + target.y + ", " + target.z
                        + " (~" + Math.round(distance) + " blocks away)"));
    }
}
