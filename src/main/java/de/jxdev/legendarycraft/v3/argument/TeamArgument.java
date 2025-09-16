package de.jxdev.legendarycraft.v3.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.data.cache.TeamCache;
import de.jxdev.legendarycraft.v3.data.models.team.Team;
import de.jxdev.legendarycraft.v3.data.models.team.TeamCacheRecord;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class TeamArgument implements CustomArgumentType.Converted<@NotNull TeamCacheRecord, @NotNull String> {

    private static final DynamicCommandExceptionType TEAM_NOT_FOUND =
            new DynamicCommandExceptionType(name ->
                    MessageComponentSerializer.message().serialize(
                            Component.translatable("team.error.no_team_with_name", Component.text(String.valueOf(name)))
                    )
            );

    private final TeamCache cache = LegendaryCraft.getInstance().getTeamCache();

    // Native/client-visible arg
    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }

    // Convert the parsed native String to Team
    @Override
    public TeamCacheRecord convert(String nativeType) throws CommandSyntaxException {
        Optional<TeamCacheRecord> team = cache.getTeam(nativeType);
        if (team.isEmpty()) throw TEAM_NOT_FOUND.create(nativeType);
        return team.get();
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> ctx, SuggestionsBuilder builder) {
        String rem = builder.getRemainingLowerCase(); // already lower-cased
        for (TeamCacheRecord team : cache.getAll()) {
            var name = team.getName();

            if (name.toLowerCase(Locale.ROOT).startsWith(rem)) builder.suggest(name);
        }
        return builder.buildFuture();
    }
}