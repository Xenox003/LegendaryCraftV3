package de.jxdev.legendarycraft.v3.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.util.CommandUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpawnCommand {
    public LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("spawn")
                .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                .executes(this::executeSpawn)
                .build();
    }

    private int executeSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        
        Location spawn = player.getWorld().getSpawnLocation();
        player.teleportAsync(spawn);
        return Command.SINGLE_SUCCESS;
    }
}
