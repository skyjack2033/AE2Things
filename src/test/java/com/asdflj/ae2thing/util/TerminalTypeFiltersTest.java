package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import org.junit.Test;

public class TerminalTypeFiltersTest {

    private static final UUID FIRST_PLAYER = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
    private static final UUID SECOND_PLAYER = UUID.fromString("ffeeddcc-bbaa-9988-7766-554433221100");

    @Test
    public void migratesEachPlayersDisabledTypesWithoutChangingTheOriginal() {
        NBTTagCompound original = new NBTTagCompound();
        NBTTagList legacyPlayers = new NBTTagList();
        legacyPlayers.appendTag(legacyPlayer(FIRST_PLAYER.toString(), "item"));
        legacyPlayers.appendTag(legacyPlayer(SECOND_PLAYER.toString(), "fluid"));
        original.setTag("typeFilters", legacyPlayers);
        original.setString("unrelated", "keep");

        NBTTagCompound saved = TerminalTypeFilters.migrateLegacyFilters(original);
        assertTrue(original.hasKey("typeFilters"));
        assertFalse(original.hasKey("terminalSettings"));

        assertFalse(saved.hasKey("typeFilters"));
        assertEquals("keep", saved.getString("unrelated"));
        NBTTagList players = saved.getTagList("terminalSettings", NBT.TAG_COMPOUND);
        assertEquals(2, players.tagCount());
        assertDisabledType(findPlayer(players, FIRST_PLAYER), "item");
        assertDisabledType(findPlayer(players, SECOND_PLAYER), "fluid");

        NBTTagCompound roundTrip = TerminalTypeFilters.migrateLegacyFilters(saved);
        assertSame(saved, roundTrip);
        NBTTagList reloadedPlayers = roundTrip.getTagList("terminalSettings", NBT.TAG_COMPOUND);
        assertDisabledType(findPlayer(reloadedPlayers, FIRST_PLAYER), "item");
        assertDisabledType(findPlayer(reloadedPlayers, SECOND_PLAYER), "fluid");

        findPlayer(players, FIRST_PLAYER).getTagList("map", NBT.TAG_COMPOUND)
            .getCompoundTagAt(0)
            .setBoolean("value", true);
        assertDisabledType(legacyPlayers.getCompoundTagAt(0), "item");
    }

    @Test
    public void currentSettingsTakePriorityAndPreserveSavedSearch() {
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList legacyPlayers = new NBTTagList();
        legacyPlayers.appendTag(legacyPlayer(FIRST_PLAYER.toString(), "item"));
        data.setTag("typeFilters", legacyPlayers);

        NBTTagCompound currentPlayer = new NBTTagCompound();
        currentPlayer.setLong("uuid_m", FIRST_PLAYER.getMostSignificantBits());
        currentPlayer.setLong("uuid_l", FIRST_PLAYER.getLeastSignificantBits());
        currentPlayer.setTag("map", disabledTypes("fluid"));
        currentPlayer.setString("savedString", "@ae2 copper");
        NBTTagList currentPlayers = new NBTTagList();
        currentPlayers.appendTag(currentPlayer);
        data.setTag("terminalSettings", currentPlayers);

        assertSame(data, TerminalTypeFilters.migrateLegacyFilters(data));

        NBTTagCompound savedPlayer = findPlayer(data.getTagList("terminalSettings", NBT.TAG_COMPOUND), FIRST_PLAYER);
        assertDisabledType(savedPlayer, "fluid");
        assertEquals("@ae2 copper", savedPlayer.getString("savedString"));
        assertTrue(data.hasKey("typeFilters"));
    }

    @Test
    public void emptyCurrentSettingsDoNotRestoreLegacyFilters() {
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList legacyPlayers = new NBTTagList();
        legacyPlayers.appendTag(legacyPlayer(FIRST_PLAYER.toString(), "item"));
        data.setTag("typeFilters", legacyPlayers);
        data.setTag("terminalSettings", new NBTTagList());

        NBTTagCompound migrated = TerminalTypeFilters.migrateLegacyFilters(data);

        assertSame(data, migrated);
        assertEquals(
            0,
            migrated.getTagList("terminalSettings", NBT.TAG_COMPOUND)
                .tagCount());
    }

    @Test
    public void ignoresMalformedLegacyPlayers() {
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList legacyPlayers = new NBTTagList();
        legacyPlayers.appendTag(legacyPlayer("invalid-uuid", "item"));
        legacyPlayers.appendTag(legacyPlayer("", "fluid"));
        legacyPlayers.appendTag(legacyPlayer(FIRST_PLAYER.toString(), "item"));
        data.setTag("typeFilters", legacyPlayers);

        NBTTagCompound saved = TerminalTypeFilters.migrateLegacyFilters(data);
        assertEquals(
            1,
            saved.getTagList("terminalSettings", NBT.TAG_COMPOUND)
                .tagCount());

        assertDisabledType(findPlayer(saved.getTagList("terminalSettings", NBT.TAG_COMPOUND), FIRST_PLAYER), "item");
    }

    @Test
    public void passesThroughNullAndFreshSettings() {
        assertNull(TerminalTypeFilters.migrateLegacyFilters(null));
        NBTTagCompound fresh = new NBTTagCompound();
        assertSame(fresh, TerminalTypeFilters.migrateLegacyFilters(fresh));
        assertFalse(fresh.hasKey("terminalSettings"));
    }

    private static NBTTagCompound legacyPlayer(String uuid, String disabledType) {
        NBTTagCompound player = new NBTTagCompound();
        player.setString("uuid", uuid);
        player.setTag("map", disabledTypes(disabledType));
        return player;
    }

    private static NBTTagList disabledTypes(String type) {
        NBTTagCompound filter = new NBTTagCompound();
        filter.setString("typeId", type);
        filter.setBoolean("value", false);
        NBTTagList filters = new NBTTagList();
        filters.appendTag(filter);
        return filters;
    }

    private static NBTTagCompound findPlayer(NBTTagList players, UUID uuid) {
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound player = players.getCompoundTagAt(i);
            if (player.getLong("uuid_m") == uuid.getMostSignificantBits()
                && player.getLong("uuid_l") == uuid.getLeastSignificantBits()) {
                return player;
            }
        }
        throw new AssertionError("Missing settings for player " + uuid);
    }

    private static void assertDisabledType(NBTTagCompound player, String type) {
        NBTTagList filters = player.getTagList("map", NBT.TAG_COMPOUND);
        assertEquals(1, filters.tagCount());
        assertEquals(
            type,
            filters.getCompoundTagAt(0)
                .getString("typeId"));
        assertFalse(
            filters.getCompoundTagAt(0)
                .getBoolean("value"));
    }
}
