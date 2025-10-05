package de.jxdev.legendarycraft.v3.invsee;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.tr7zw.nbtapi.*;
import de.tr7zw.nbtapi.iface.NBTFileHandle;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class InvSeeController implements Listener {
    private final LegendaryCraft plugin;
    private final File playerDataDir;

    // viewer -> target
    private final Map<UUID, UUID> viewerToTarget = new HashMap<>();
    // target -> set of viewers
    private final Map<UUID, Set<UUID>> targetToViewers = new HashMap<>();

    // guard sets to prevent echo loops
    private final Set<UUID> applyingToTarget = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> applyingToGui  = Collections.synchronizedSet(new HashSet<>());

    public InvSeeController(LegendaryCraft plugin, File worldFolder) {
        this.plugin = plugin;
        this.playerDataDir = new File(worldFolder, "playerdata");

        Bukkit.getScheduler().runTaskTimer(plugin, this::tickRefreshAll, 10L, 10L);
    }

    /* -------------------- Live-Session Lifecycle -------------------- */
    public void open(Player viewer, OfflinePlayer target) {
        viewerToTarget.put(viewer.getUniqueId(), target.getUniqueId());
        targetToViewers.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>()).add(viewer.getUniqueId());

        // Create Inventory \\
        String titleName = Objects.requireNonNullElse(target.getName(), target.getUniqueId().toString());
        InvSeeHolder holder = new InvSeeHolder(target, true);

        Inventory viewerInv = Bukkit.createInventory(holder, 54, Component.text(titleName).color(NamedTextColor.AQUA).append(Component.text("'s Inventory")));

        viewerInv.setItem(36, getBlockerItem());
        viewerInv.setItem(37, getBlockerItem());
        viewerInv.setItem(38, getBlockerItem());
        viewerInv.setItem(39, getBlockerItem());
        viewerInv.setItem(40, getBlockerItem());
        viewerInv.setItem(41, getBlockerItem());
        viewerInv.setItem(42, getBlockerItem());
        viewerInv.setItem(43, getBlockerItem());
        viewerInv.setItem(44, getBlockerItem());

        viewerInv.setItem(49, getBlockerItem());
        viewerInv.setItem(50, getBlockerItem());
        viewerInv.setItem(51, getBlockerItem());
        viewerInv.setItem(52, getBlockerItem());

        // Initial pull from target if online \\
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null && online.isOnline()) {
            pullFromTargetIntoGui(online.getInventory(), viewerInv);
        } else {
            if (!loadOfflineIntoGui(target.getUniqueId(), viewerInv)) {
                viewer.closeInventory();
            }
        }

        // Open inv \\
        viewer.openInventory(viewerInv);
    }

    public static ItemStack getBlockerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").color(NamedTextColor.DARK_GRAY));
        item.setItemMeta(meta);

        return item;
    }

    public void registerClose(Player viewer) {
        UUID v = viewer.getUniqueId();
        UUID t = viewerToTarget.remove(v);
        if (t != null) {
            Set<UUID> set = targetToViewers.get(t);
            if (set != null) {
                set.remove(v);
                if (set.isEmpty()) targetToViewers.remove(t);
            }
        }
    }

    private List<Inventory> openGuisForTarget(UUID targetId) {
        Set<UUID> viewers = targetToViewers.getOrDefault(targetId, Collections.emptySet());
        List<Inventory> guis = new ArrayList<>();
        for (UUID vId : viewers) {
            Player v = Bukkit.getPlayer(vId);
            if (v == null) continue;
            Inventory top = v.getOpenInventory() != null ? v.getOpenInventory().getTopInventory() : null;
            if (top != null && top.getHolder() instanceof InvSeeHolder h && h.getTargetId().equals(targetId)) {
                guis.add(top);
            }
        }
        return guis;
    }

    private void pushGuiToTarget(Inventory gui, UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) return;

        // prevent echo when target events fire
        applyingToTarget.add(targetId);
        try {
            applyGuiToPlayer(gui, target.getInventory());
            target.updateInventory();
        } finally {
            // small debounce
            Bukkit.getScheduler().runTask(plugin, () -> applyingToTarget.remove(targetId));
        }
    }

    /* -------------------- GUI --> Player Handles -------------------- */

    @EventHandler(ignoreCancelled = true)
    public void onGuiClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof InvSeeHolder holder)) return;
        if (!holder.isEditable()) { event.setCancelled(true); return; }

        // Prevent Placeholder items from being edited \\
        int raw = event.getRawSlot();
        if ((raw >= 36 && raw <= 44) || (raw >= 49 && raw <= 52)) {
            event.setCancelled(true);
            return;
        }

        // Defer sync 1 tick later \\
        Bukkit.getScheduler().runTask(plugin, () -> pushGuiToTarget(event.getView().getTopInventory(), holder.getTargetId()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onGuiDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof InvSeeHolder holder)) return;
        if (!holder.isEditable()) { event.setCancelled(true); return; }

        // Prevent Placeholder items from being edited \\
        for (int raw : event.getRawSlots()) {
            if ((raw >= 36 && raw <= 44) || (raw >= 49 && raw <= 52)) {
                event.setCancelled(true);
                return;
            }
        }

        // Defer sync 1 tick later \\
        Bukkit.getScheduler().runTask(plugin, () -> pushGuiToTarget(event.getView().getTopInventory(), holder.getTargetId()));
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof InvSeeHolder holder)) return;
        registerClose((Player) e.getPlayer());

        if (!holder.isEditable()) return;
        // Final push on close
        UUID targetId = holder.getTargetId();
        Player online = Bukkit.getPlayer(targetId);

        if (online != null && online.isOnline()) {
            // live final push
            pushGuiToTarget(e.getInventory(), targetId);
        } else {
            saveOfflineDat(targetId, e.getInventory());
        }
    }

    /* -------------------- Player --> GUI Handles -------------------- */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTargetClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!targetToViewers.containsKey(p.getUniqueId())) return;
        if (applyingToTarget.contains(p.getUniqueId())) return;

        mirrorTargetToAllGuis(p);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTargetDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!targetToViewers.containsKey(p.getUniqueId())) return;
        if (applyingToTarget.contains(p.getUniqueId())) return;

        mirrorTargetToAllGuis(p);
    }

    // Other inventory mutators \\
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTargetSwapOffHand(PlayerSwapHandItemsEvent event) {
        if (!targetToViewers.containsKey(event.getPlayer().getUniqueId())) return;
        if (applyingToTarget.contains(event.getPlayer().getUniqueId())) return;

        mirrorTargetToAllGuis(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!targetToViewers.containsKey(event.getPlayer().getUniqueId())) return;
        if (applyingToTarget.contains(event.getPlayer().getUniqueId())) return;

        mirrorTargetToAllGuis(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!targetToViewers.containsKey(p.getUniqueId())) return;
        if (applyingToTarget.contains(p.getUniqueId())) return;

        // Next tick after pickup merges
        Bukkit.getScheduler().runTask(plugin, () -> mirrorTargetToAllGuis(p));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        if (!targetToViewers.containsKey(p.getUniqueId())) return;
        if (applyingToTarget.contains(p.getUniqueId())) return;

        Bukkit.getScheduler().runTask(plugin, () -> mirrorTargetToAllGuis(p));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player p = event.getPlayer();
        if (!targetToViewers.containsKey(p.getUniqueId())) return;
        if (applyingToTarget.contains(p.getUniqueId())) return;


        Bukkit.getScheduler().runTask(plugin, () -> mirrorTargetToAllGuis(p));
    }

    @EventHandler
    public void onTargetJoin(PlayerJoinEvent event) {
        UUID tid = event.getPlayer().getUniqueId();
        Set<UUID> viewers = targetToViewers.get(tid);
        if (viewers == null || viewers.isEmpty()) return;

        for (UUID vid : new HashSet<>(viewers)) {
            Player v = Bukkit.getPlayer(vid);
            if (v == null) continue;
            // close only if they are actually viewing this target’s GUI
            Inventory top = v.getOpenInventory() != null ? v.getOpenInventory().getTopInventory() : null;
            if (top != null && top.getHolder() instanceof InvSeeHolder h && h.getTargetId().equals(tid)) {
                v.closeInventory();
            }
        }
    }

    @EventHandler
    public void onTargetQuit(PlayerQuitEvent event) {
        UUID tid = event.getPlayer().getUniqueId();
        Set<UUID> viewers = targetToViewers.get(tid);
        if (viewers == null || viewers.isEmpty()) return;

        for (UUID vid : new HashSet<>(viewers)) {
            Player v = Bukkit.getPlayer(vid);
            if (v == null) continue;
            Inventory top = v.getOpenInventory() != null ? v.getOpenInventory().getTopInventory() : null;
            if (top != null && top.getHolder() instanceof InvSeeHolder h && h.getTargetId().equals(tid)) {
                v.closeInventory();
            }
        }
    }

    private void mirrorTargetToAllGuis(Player target) {
        UUID targetId = target.getUniqueId();
        if (!target.isOnline()) return;
        List<Inventory> guis = openGuisForTarget(targetId);
        if (guis.isEmpty()) return;

        applyingToGui.add(targetId);
        try {
            for (Inventory gui : guis) {
                pullFromTargetIntoGui(target.getInventory(), gui);
                // (viewers see GUI immediately; no need to call updateInventory on them)
            }
        } finally {
            Bukkit.getScheduler().runTask(plugin, () -> applyingToGui.remove(targetId));
        }
    }

    // Periodic “just-in-case” refresh
    private void tickRefreshAll() {
        for (UUID targetId : targetToViewers.keySet()) {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) continue;
            if (applyingToTarget.contains(targetId)) continue;
            mirrorTargetToAllGuis(target);
        }
    }

    /* -------------------- Copy Helpers -------------------- */
    private void applyGuiToPlayer(Inventory gui, PlayerInventory dest) {
        for (int i = 0; i < 36; i++) dest.setItem(i, cloneOrNull(gui.getItem(i)));
        dest.setHelmet(cloneOrNull(gui.getItem(45)));
        dest.setChestplate(cloneOrNull(gui.getItem(46)));
        dest.setLeggings(cloneOrNull(gui.getItem(47)));
        dest.setBoots(cloneOrNull(gui.getItem(48)));
        dest.setItemInOffHand(cloneOrNull(gui.getItem(53)));
    }

    private void pullFromTargetIntoGui(PlayerInventory src, Inventory gui) {
        // main
        for (int i = 0; i < 36; i++) gui.setItem(i, cloneOrAir(src.getItem(i)));
        // panes stay as-is (36..44 and 49..52)
        // armor/offhand
        gui.setItem(45, cloneOrAir(src.getHelmet()));
        gui.setItem(46, cloneOrAir(src.getChestplate()));
        gui.setItem(47, cloneOrAir(src.getLeggings()));
        gui.setItem(48, cloneOrAir(src.getBoots()));
        gui.setItem(53, cloneOrAir(src.getItemInOffHand()));
    }

    private ItemStack cloneOrNull(ItemStack it) {
        return (it == null || it.getType().isAir()) ? null : it.clone();
    }
    private ItemStack cloneOrAir(ItemStack it) {
        return (it == null || it.getType().isAir()) ? new ItemStack(Material.AIR) : it.clone();
    }


    /* -------------------- Offline Persistance API -------------------- */
    private boolean loadOfflineIntoGui(UUID targetId, Inventory gui) {
        try {
            File dat = new File(playerDataDir, targetId + ".dat");
            if (!dat.exists()) return false;

            NBTFileHandle nbt = NBT.getFileHandle(dat);
            var inventoryList = nbt.getCompoundList("Inventory");
            var equipmentList = nbt.getCompound("equipment");

            // Write Inventory \\
            for (var compound : inventoryList) {
                if (!compound.hasTag("Slot")) continue;
                int slot = compound.getByte("Slot");
                ItemStack item = NBT.itemStackFromNBT(compound);
                if (slot < 0 || slot > 53) continue;
                gui.setItem(slot, item);
            }

            // Write Equipment \\
            if (equipmentList != null) {
                var helmet = equipmentList.getCompound("head");
                var chestplate = equipmentList.getCompound("chest");
                var leggings = equipmentList.getCompound("legs");
                var boots = equipmentList.getCompound("feet");
                var offhand = equipmentList.getCompound("offhand");

                if (helmet != null) gui.setItem(45, NBT.itemStackFromNBT(helmet));
                if (chestplate != null) gui.setItem(46, NBT.itemStackFromNBT(chestplate));
                if (leggings != null) gui.setItem(47, NBT.itemStackFromNBT(leggings));
                if (boots != null) gui.setItem(48, NBT.itemStackFromNBT(boots));
                if (offhand != null) gui.setItem(53, NBT.itemStackFromNBT(offhand));
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void saveOfflineDat(UUID targetId, Inventory gui) {
        try {
            File dat = new File(playerDataDir, targetId + ".dat");
            if (!dat.exists()) return;

            NBTFileHandle nbt = NBT.getFileHandle(dat);
            var inventoryList = nbt.getCompoundList("Inventory");
            inventoryList.clear();
            var equipmentList = nbt.getOrCreateCompound("equipment");
            equipmentList.clearNBT();

            // Write Inventory \\
            for (int i = 0; i < 36; i++) {
                ItemStack item = gui.getItem(i);
                if (item == null || item.getType().isAir()) continue;

                ReadWriteNBT entry = inventoryList.addCompound();
                entry.setByte("Slot", (byte) i);
                entry.mergeCompound(NBT.itemStackToNBT(item.clone()));
            }

            // Write Equipment \\
            ItemStack helmet = gui.getItem(45);
            ItemStack chestplate = gui.getItem(46);
            ItemStack leggings = gui.getItem(47);
            ItemStack boots = gui.getItem(48);
            ItemStack offhand = gui.getItem(53);

            if (helmet != null) equipmentList.setItemStack("head", helmet);
            if (helmet != null) equipmentList.setItemStack("chest", chestplate);
            if (helmet != null) equipmentList.setItemStack("legs", leggings);
            if (helmet != null) equipmentList.setItemStack("feet", boots);
            if (helmet != null) equipmentList.setItemStack("offhand", offhand);

            nbt.save();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
