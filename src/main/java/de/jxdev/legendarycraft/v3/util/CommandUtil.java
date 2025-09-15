package de.jxdev.legendarycraft.v3.util;

import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

public class CommandUtil {
    public static final Predicate<CommandSourceStack> PLAYER_ONLY_REQUIREMENT =
            src -> src.getSender() instanceof Player;

    private static final SimpleCommandExceptionType PLAYER_ONLY_ERROR = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("common.error.player_only", NamedTextColor.RED)
    ));

    public static final SimpleCommandExceptionType INTERNAL_ERROR = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
            Component.translatable("common.error.internal", NamedTextColor.RED)
    ));

    /**
     * Gets a Player from a CommandSender
     * Makes sure the CommandSender is a Player
     */
    public static Player getPlayerFromCommandSender(CommandSender sender) throws CommandSyntaxException {
        Player player = (Player) sender;
        if (player == null) {
            throw PLAYER_ONLY_ERROR.create();
        }
        return (Player) sender;
    }
}
