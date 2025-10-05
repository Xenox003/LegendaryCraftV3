package de.jxdev.legendarycraft.v3.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.data.cache.OfflinePlayerCache;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class OfflinePlayerArgument implements CustomArgumentType.Converted<@NotNull OfflinePlayer, @NotNull String>{

    private static final DynamicCommandExceptionType PLAYER_NOT_FOUND =
            new DynamicCommandExceptionType(name ->
                    MessageComponentSerializer.message().serialize(
                            Component.translatable("common.error.no_offline_player_with_name", Component.text(String.valueOf(name)).color(NamedTextColor.AQUA))
                    )
            );

    private final OfflinePlayerCache offlinePlayerCache = LegendaryCraft.getInstance().getOfflinePlayerCache();

    // Native/client-visible arg
    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }

    // Convert the parsed native String to Team
    @Override
    public OfflinePlayer convert(String nativeType) throws CommandSyntaxException {
        OfflinePlayer player = offlinePlayerCache.resolve(nativeType);
        if (player == null) throw PLAYER_NOT_FOUND.create(nativeType);
        return player;
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> ctx, SuggestionsBuilder builder) {
        String rem = builder.getRemainingLowerCase();
        for (String username : offlinePlayerCache.getNames()) {
            if (username.toLowerCase(Locale.ROOT).startsWith(rem)) builder.suggest(username);
        }
        return builder.buildFuture();
    }
}
