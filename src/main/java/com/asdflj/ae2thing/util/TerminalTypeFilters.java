package com.asdflj.ae2thing.util;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import appeng.util.TerminalSettings;

/** Keeps terminal filters saved before AE2 replaced MonitorableTypeFilter with TerminalSettings. */
public class TerminalTypeFilters extends TerminalSettings {

    private static final String LEGACY_FILTERS = "typeFilters";
    private static final String TERMINAL_SETTINGS = "terminalSettings";

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(migrateLegacyFilters(tag));
    }

    static NBTTagCompound migrateLegacyFilters(NBTTagCompound tag) {
        if (tag == null || tag.hasKey(TERMINAL_SETTINGS, NBT.TAG_LIST) || !tag.hasKey(LEGACY_FILTERS, NBT.TAG_LIST)) {
            return tag;
        }

        final NBTTagList legacyPlayers = tag.getTagList(LEGACY_FILTERS, NBT.TAG_COMPOUND);
        final NBTTagList players = new NBTTagList();
        for (int i = 0; i < legacyPlayers.tagCount(); i++) {
            final NBTTagCompound legacyPlayer = legacyPlayers.getCompoundTagAt(i);
            final UUID id;
            try {
                id = UUID.fromString(legacyPlayer.getString("uuid"));
            } catch (IllegalArgumentException e) {
                continue;
            }

            final NBTTagCompound player = new NBTTagCompound();
            player.setLong("uuid_m", id.getMostSignificantBits());
            player.setLong("uuid_l", id.getLeastSignificantBits());
            player.setTag(
                "map",
                legacyPlayer.getTagList("map", NBT.TAG_COMPOUND)
                    .copy());
            players.appendTag(player);
        }

        final NBTTagCompound migrated = (NBTTagCompound) tag.copy();
        migrated.removeTag(LEGACY_FILTERS);
        migrated.setTag(TERMINAL_SETTINGS, players);
        return migrated;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.removeTag(LEGACY_FILTERS);
    }
}
