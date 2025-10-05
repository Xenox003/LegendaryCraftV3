package de.jxdev.legendarycraft.v3.invsee.target;

import de.jxdev.legendarycraft.v3.LegendaryCraft;
import de.jxdev.legendarycraft.v3.invsee.InvSeeUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.NBTFileHandle;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.util.Objects;

public class InvseePlayerSession implements InvseeSession {
    private final OfflinePlayer player;
    private final Inventory gui;
    private final File playerDataDir = new File(Bukkit.getWorlds().getFirst().getWorldFolder(), "playerdata");

    public InvseePlayerSession(OfflinePlayer player) {
        this.player = player;

        Component title = Component.text(Objects.requireNonNullElse(player.getName(), player.getUniqueId().toString()))
                .color(NamedTextColor.AQUA)
                .append(Component.text("'s Inventory"));
        this.gui = InvSeeUtils.createInventory(54, title);

        // Placeholder Items \\
        for (int s = 36; s <= 44; s++) gui.setItem(s, InvSeeUtils.createBlockerItem());
        for (int s = 49; s <= 52; s++) gui.setItem(s, InvSeeUtils.createBlockerItem());
    }

    @Override
    public void pushToTarget() {
        Player online = this.player.getPlayer();
        if (online == null) return;

        PlayerInventory dest = online.getInventory();

        for (int i = 0; i < 36; i++) dest.setItem(i, InvSeeUtils.cloneOrNull(gui.getItem(i)));
        dest.setHelmet(InvSeeUtils.cloneOrNull(gui.getItem(45)));
        dest.setChestplate(InvSeeUtils.cloneOrNull(gui.getItem(46)));
        dest.setLeggings(InvSeeUtils.cloneOrNull(gui.getItem(47)));
        dest.setBoots(InvSeeUtils.cloneOrNull(gui.getItem(48)));
        dest.setItemInOffHand(InvSeeUtils.cloneOrNull(gui.getItem(53)));

        online.updateInventory();
    }

    @Override
    public void pullToGui() {
        Player online = this.player.getPlayer();
        if (online == null) return;

        PlayerInventory src = online.getInventory();

        for (int i = 0; i < 36; i++) gui.setItem(i, InvSeeUtils.cloneOrAir(src.getItem(i)));
        gui.setItem(45, InvSeeUtils.cloneOrAir(src.getHelmet()));
        gui.setItem(46, InvSeeUtils.cloneOrAir(src.getChestplate()));
        gui.setItem(47, InvSeeUtils.cloneOrAir(src.getLeggings()));
        gui.setItem(48, InvSeeUtils.cloneOrAir(src.getBoots()));
        gui.setItem(53, InvSeeUtils.cloneOrAir(src.getItemInOffHand()));
    }

    @Override
    public void save() {
        if (this.player.isOnline()) {
            this.pushToTarget();
            return;
        };

        try {
            File dat = new File(playerDataDir, this.player.getUniqueId() + ".dat");
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
            if (chestplate != null) equipmentList.setItemStack("chest", chestplate);
            if (leggings != null) equipmentList.setItemStack("legs", leggings);
            if (boots != null) equipmentList.setItemStack("feet", boots);
            if (offhand != null) equipmentList.setItemStack("offhand", offhand);

            nbt.save();
        } catch (Exception ex) {
            LegendaryCraft.getInstance().getLogger().severe(ex.toString());
        }
    }

    @Override
    public void load() {
        if (this.player.isOnline()) {
            this.pullToGui();
            return;
        };

        try {
            File dat = new File(playerDataDir, this.player.getUniqueId() + ".dat");
            if (!dat.exists()) return;

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
        } catch (Exception ex) {
            LegendaryCraft.getInstance().getLogger().severe(ex.toString());
        }
    }

    @Override
    public boolean canMove(int slot) {
        boolean blocked = (slot >= 36 && slot <= 44) || (slot >= 49 && slot <= 52);
        return !blocked;
    }

    @Override
    public Inventory getGui() {
        return gui;
    }

    @Override
    public String getTargetKey() {
        return InvSeeUtils.getPlayerTargetKey(this.player.getUniqueId());
    }
}
