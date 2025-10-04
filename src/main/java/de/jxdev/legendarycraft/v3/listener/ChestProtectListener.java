package de.jxdev.legendarycraft.v3.listener;

import de.jxdev.legendarycraft.v3.data.models.BlockPos;
import de.jxdev.legendarycraft.v3.data.models.LockedChest;
import de.jxdev.legendarycraft.v3.data.models.team.TeamCacheRecord;
import de.jxdev.legendarycraft.v3.service.ChestService;
import de.jxdev.legendarycraft.v3.service.TeamService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ChestProtectListener implements Listener {
    private final ChestService chestService;
    private final TeamService teamService;

    public ChestProtectListener(ChestService chestService, TeamService teamService) {
        this.chestService = chestService;
        this.teamService = teamService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        Optional<LockedChest> lockedChest = getLocked(event.getBlock());
        if (lockedChest.isPresent()) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(getLockedMessage(lockedChest.get()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Optional<LockedChest> lockedChest = getLocked(clicked);
        if (lockedChest.isEmpty()) return;

        Player p = event.getPlayer();
        Optional<TeamCacheRecord> team = teamService.getCachedTeamByPlayer(p.getUniqueId());
        Integer teamId = team.map(TeamCacheRecord::getId).orElse(-1);

        if (teamId != lockedChest.get().getTeamId()) {
            event.setCancelled(true);
            p.sendActionBar(getLockedMessage(lockedChest.get()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        InventoryHolder initiator = event.getInitiator().getHolder();
        boolean hopperInitiated = initiator instanceof Hopper || initiator instanceof HopperMinecart;
        if (!hopperInitiated) return;

        InventoryHolder src = event.getSource().getHolder();
        InventoryHolder dst = event.getDestination().getHolder();

        if (src != null && isLocked(src)) {
            event.setCancelled(true);
            return;
        }
        if (dst != null && isLocked(dst)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(this::isLocked);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(this::isLocked);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (isLocked(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBurn(BlockBurnEvent e) {
        if (isLocked(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onIgnite(BlockIgniteEvent e) {
        if (isLocked(e.getBlock())) {
            e.setCancelled(true);
        }
    }


    private Component getLockedMessage(LockedChest chest) {
        var team = teamService.getCachedTeam(chest.getTeamId());
        final Component teamName = team.isPresent() ? team.get().getChatComponent() : Component.text(chest.getTeamId());

        return Component.translatable("chest.info.locked", teamName);
    }

    private Optional<LockedChest> getLocked(Block b) {
        if (b.getType() != Material.CHEST) return Optional.empty();
        return chestService.get(BlockPos.fromBlock(b));
    }
    private boolean isLocked(Block b) {
        return getLocked(b).isPresent();
    }
    private boolean isLocked(InventoryHolder holder) {
        Block block = switch (holder) {
            case Chest chest -> chest.getLocation().getBlock();
            case DoubleChest db -> db.getLocation().getBlock();
            default -> null;
        };
        if (block == null) return false;
        return isLocked(block);
    }
}
