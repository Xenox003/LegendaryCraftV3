package de.jxdev.legendarycraft.v3.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.argument.OfflinePlayerArgument;
import de.jxdev.legendarycraft.v3.invsee.target.InvseePlayerSession;
import de.jxdev.legendarycraft.v3.util.CommandUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class InvSeeCommand {
    public LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("invsee")
                .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                .requires(stack -> stack.getSender().hasPermission("legendarycraft.invsee"))
                .then(Commands.argument("player", new OfflinePlayerArgument())
                        .executes(this::invSeeExecutor)
                ).build();
    }

    private int invSeeExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player viewer = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        OfflinePlayer offlineTarget = context.getArgument("player", OfflinePlayer.class);

        LegendaryCraft.getInstance().getInvSeeController().open(viewer, new InvseePlayerSession(offlineTarget));
        return 0;
    }
}
