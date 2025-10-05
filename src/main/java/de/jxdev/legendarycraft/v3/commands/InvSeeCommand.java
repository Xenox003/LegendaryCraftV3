package de.jxdev.legendarycraft.v3.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.argument.OfflinePlayerArgument;
import de.jxdev.legendarycraft.v3.util.CommandUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class InvSeeCommand {
    public LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("invsee")
                .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                .requires(stack -> stack.getSender().hasPermission("legendarycraft.invsee"))
                .then(Commands.argument("player", new OfflinePlayerArgument())
                        .executes(this::invSeeExecutor)
                ).build();
    }

    private int invSeeExecutor(CommandContext<CommandSourceStack> context) {
        return 0;
    }
}
