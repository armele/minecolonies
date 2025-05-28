package com.minecolonies.api.util;

import static com.minecolonies.core.colony.buildings.modules.BuildingModules.STATS_MODULE;

import java.util.Map;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.buildings.modules.BuildingStatisticsModule;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

/**
 * A variety of helper functions to facilitate statistics collection by buildings.
 * 
 */
public class StatsUtil {
    /**
     * Safely return the name of the item in the given slot of the given furnace for use in statistics.
     * Returns empty if no furnace, or no item in the slot.
     *
     * @param furnace the furnace to get the item from.
     * @param slot    the slot to get the item from.
     * @return the name of the item.
     */
    public static String nameForStats(final FurnaceBlockEntity furnace, final int slot)
    {
        String name = "";

        if (furnace != null) 
        {
            ItemStack item = furnace.getItem(slot);
            if (item != null) 
            {
                name = item.getDescriptionId();
            }
        }

        return name;
    }

    public static void trackStat(IBuilding building, String statName, Map<ItemStack, Integer> itemMap)
    {
        for (Map.Entry<ItemStack, Integer> entry : itemMap.entrySet())
        {
            ItemStack stack = entry.getKey();
            int count = entry.getValue();
            trackStat(building, statName, stack, count);
        }
    }

    /**
     * Track a stat for a given building using the standard STATS_MODULE, with some null safety built in.
     * 
     * @param building the building to track the stat for.
     * @param statIdentifier the identifier for the stat.
     * @param displayName the display name of the item to track the stat for.
     * @param count the number of the item to track the stat for.
     */
    public static void trackStat(IBuilding building, String statIdentifier, String displayName, int count) 
    {
        if (building == null) 
        {
            Log.getLogger().warn("Attempted to track stat '{}' with null building: ", statIdentifier);
            return;
        }

        String statKey = statIdentifier + ";" + displayName;
        BuildingStatisticsModule statsModule = building.getModule(STATS_MODULE);
        
        if (statsModule != null) 
        {
            statsModule.incrementBy(statKey, count);
        } else {
            Log.getLogger().error("Attempt to track stats on a building that has no statistics module: {}", building);
        }
    }

    public static void trackStat(IBuilding building, String statIdentifier, Component displayName, int count) 
    {
        if (displayName == null) 
        {
            Log.getLogger().warn("Attempted to track stat '{}' with null displayName as component: ", statIdentifier);
            return;
        }

        trackStat(building, statIdentifier, displayName.getString(), count);
    }

    public static void trackStat(IBuilding building, String statIdentifier, ItemStack stack, int count) 
    {
        if (stack == null) 
        {
            Log.getLogger().warn("Attempted to track stat '{}' with null stack: ", statIdentifier);
            return;
        }

        trackStat(building, statIdentifier, stack.getDescriptionId(), count);
    }
}
