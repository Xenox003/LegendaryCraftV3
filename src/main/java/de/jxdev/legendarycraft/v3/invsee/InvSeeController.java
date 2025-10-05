package de.jxdev.legendarycraft.v3.invsee;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.invsee.target.InvseeSession;
import de.tr7zw.nbtapi.*;
import de.tr7zw.nbtapi.iface.NBTFileHandle;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.util.*;

public class InvSeeController implements Listener {
    private final LegendaryCraft plugin;
    private final File playerDataDir;

    // viewer -> target
    private final Map<UUID, InvseeSession> viewerToSession = new HashMap<>();
    // target -> set of viewers
    private final Map<String, Set<UUID>> targetToViewers = new HashMap<>();

    // guard sets to prevent echo loops
    private final Set<String> applyingToTarget = Collections.synchronizedSet(new HashSet<>());

    public InvSeeController(LegendaryCraft plugin, File worldFolder) {
        this.plugin = plugin;
        this.playerDataDir = new File(worldFolder, "playerdata");

        Bukkit.getScheduler().runTaskTimer(plugin, this::tickRefreshAll, 10L, 10L);
    }

    /* -------------------- Live-Session Lifecycle -------------------- */
    public void open(Player viewer, InvseeSession session) {
        viewerToSession.put(viewer.getUniqueId(), session);
        targetToViewers.computeIfAbsent(session.getTargetKey(), k -> new HashSet<>()).add(viewer.getUniqueId());

        // Load Inventory \\
        session.load();

        // Open inv \\
        viewer.openInventory(session.getGui());
    }

    public void registerClose(Player viewer) {
        UUID v = viewer.getUniqueId();
        InvseeSession t = viewerToSession.remove(viewer.getUniqueId());
        if (t != null) {
            Set<UUID> set = targetToViewers.get(t.getTargetKey());
            if (set != null) {
                set.remove(v);
                if (set.isEmpty()) targetToViewers.remove(t.getTargetKey());
            }
        }
    }

    /* -------------------- GUI --> Player Handles -------------------- */

    @EventHandler(ignoreCancelled = true)
    public void onGuiClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof InvSeeHolder)) return;
        InvseeSession session = viewerToSession.get(event.getWhoClicked().getUniqueId());

        // Prevent Placeholder items from being edited \\
        int raw = event.getRawSlot();
        if (!session.canMove(raw)) {
            event.setCancelled(true);
            return;
        }

        // Sync Change to session \\
        Bukkit.getScheduler().runTask(plugin, session::pushToTarget);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGuiDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof InvSeeHolder)) return;
        InvseeSession session = viewerToSession.get(event.getWhoClicked().getUniqueId());

        // Prevent Placeholder items from being edited \\
        for (int raw : event.getRawSlots()) {
            if (!session.canMove(raw)) {
                event.setCancelled(true);
                return;
            }
        }

        // Defer sync 1 tick later \\
        Bukkit.getScheduler().runTask(plugin, session::pushToTarget);
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof InvSeeHolder)) return;
        InvseeSession session = viewerToSession.get(e.getPlayer().getUniqueId());
        registerClose((Player) e.getPlayer());

        session.save();
    }

    /* -------------------- Player --> GUI Handles -------------------- */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTargetClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        String targetKey = InvSeeUtils.getPlayerTargetKey(p.getUniqueId());
        if (!targetToViewers.containsKey(targetKey)) return;
        if (applyingToTarget.contains(targetKey)) return;

        mirrorTargetToAllSessions(targetKey);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTargetDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        String targetKey = InvSeeUtils.getPlayerTargetKey(p.getUniqueId());
        if (!targetToViewers.containsKey(targetKey)) return;
        if (applyingToTarget.contains(targetKey)) return;

        mirrorTargetToAllSessions(targetKey);
    }

    // Other inventory mutators \\
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTargetSwapOffHand(PlayerSwapHandItemsEvent event) {
        Player p = event.getPlayer();
        String targetKey = InvSeeUtils.getPlayerTargetKey(p.getUniqueId());
        if (!targetToViewers.containsKey(targetKey)) return;
        if (applyingToTarget.contains(targetKey)) return;

        mirrorTargetToAllSessions(targetKey);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        String targetKey = InvSeeUtils.getPlayerTargetKey(p.getUniqueId());
        if (!targetToViewers.containsKey(targetKey)) return;
        if (applyingToTarget.contains(targetKey)) return;

        mirrorTargetToAllSessions(targetKey);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        String targetKey = InvSeeUtils.getPlayerTargetKey(p.getUniqueId());
        if (!targetToViewers.containsKey(targetKey)) return;
        if (applyingToTarget.contains(targetKey)) return;

        // Next tick after pickup merges
        Bukkit.getScheduler().runTask(plugin, () -> mirrorTargetToAllSessions(targetKey));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        String targetKey = InvSeeUtils.getPlayerTargetKey(p.getUniqueId());
        if (!targetToViewers.containsKey(targetKey)) return;
        if (applyingToTarget.contains(targetKey)) return;

        Bukkit.getScheduler().runTask(plugin, () -> mirrorTargetToAllSessions(targetKey));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player p = event.getPlayer();
        String targetKey = InvSeeUtils.getPlayerTargetKey(p.getUniqueId());
        if (!targetToViewers.containsKey(targetKey)) return;
        if (applyingToTarget.contains(targetKey)) return;


        Bukkit.getScheduler().runTask(plugin, () -> mirrorTargetToAllSessions(targetKey));
    }

    @EventHandler
    public void onTargetJoin(PlayerJoinEvent event) {
        String playerTargetKey = InvSeeUtils.getPlayerTargetKey(event.getPlayer().getUniqueId());
        Set<UUID> viewers = targetToViewers.get(playerTargetKey);
        if (viewers == null || viewers.isEmpty()) return;

        for (UUID vid : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(vid);
            if (player == null) continue;
            InvseeSession session = viewerToSession.get(vid);
            if (session == null) continue;
            Inventory gui = session.getGui();
            if (gui == null) continue;

            if (player.getOpenInventory().getTopInventory().equals(gui)) {
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onTargetQuit(PlayerQuitEvent event) {
        String playerTargetKey = InvSeeUtils.getPlayerTargetKey(event.getPlayer().getUniqueId());
        Set<UUID> viewers = targetToViewers.get(playerTargetKey);
        if (viewers == null || viewers.isEmpty()) return;

        for (UUID vid : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(vid);
            if (player == null) continue;
            InvseeSession session = viewerToSession.get(vid);
            if (session == null) continue;
            Inventory gui = session.getGui();
            if (gui == null) continue;

            if (player.getOpenInventory().getTopInventory().equals(gui)) {
                player.closeInventory();
            }
        }
    }

    private void mirrorTargetToAllSessions(String targetId) {
        targetToViewers
                .get(targetId)
                .stream()
                .map(viewerToSession::get)
                .forEach(InvseeSession::pullToGui);

    }

    // Periodic “just-in-case” refresh
    private void tickRefreshAll() {
        for (String targetId: targetToViewers.keySet()) {
            Player target = Bukkit.getPlayer(targetId);
            if (applyingToTarget.contains(targetId)) continue;
            mirrorTargetToAllSessions(targetId);
        }
    }
}
