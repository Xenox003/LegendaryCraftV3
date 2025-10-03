package de.jxdev.legendarycraft.v3.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.service.LinkService;
import de.jxdev.legendarycraft.v3.util.CommandUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class LinkCommand {
    private final LegendaryCraft plugin = LegendaryCraft.getInstance();

    private static final SimpleCommandExceptionType INTERNAL_ERROR = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("common.error.internal_error", NamedTextColor.RED)
    ));

    public LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("link")
                .requires(CommandUtil.PLAYER_ONLY_REQUIREMENT)
                .executes(this::linkExecutor)
                .then(Commands.literal("unlink")
                        .executes(this::unlinkExecutor)
                )
                .build();
    }

    private int linkExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        LinkService linkService = plugin.getLinkService();
        try {
            String code = linkService.generateLinkCode(player.getUniqueId());
            Component msg = Component.translatable("link.info.code_generated", Component.text(code))
                    .append(Component.newline())
                    .append(Component.translatable("link.info.discord_hint"))
                    .append(Component.space())
                    .append(Component.text("/link code:" + code, NamedTextColor.AQUA).clickEvent(ClickEvent.copyToClipboard(code)));
            player.sendMessage(msg);
            return Command.SINGLE_SUCCESS;
        } catch (SQLException e) {
            throw INTERNAL_ERROR.create();
        }
    }

    private int unlinkExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = CommandUtil.getPlayerFromCommandSender(context.getSource().getSender());
        LinkService linkService = plugin.getLinkService();
        try {
            boolean unlinked = linkService.unlinkByPlayer(player.getUniqueId());
            if (unlinked) {
                player.sendMessage(Component.translatable("link.success.unlinked"));
            } else {
                player.sendMessage(Component.translatable("link.error.not_linked", NamedTextColor.YELLOW));
            }
            return Command.SINGLE_SUCCESS;
        } catch (SQLException e) {
            throw INTERNAL_ERROR.create();
        }
    }
}
