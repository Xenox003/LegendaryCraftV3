package de.jxdev.legendarycraft.v3.service;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface PlayerNameService {
    void cleanup(Player player);

    void setNickname(Player player, String nickname);

    void clearNickname(Player player);

    void setPrefix(Player player, Component prefix);

    void clearPrefix(Player player);

    void setSuffix(Player player, Component suffix);

    void clearSuffix(Player player);

    void refreshEverywhere(Player player);
}
