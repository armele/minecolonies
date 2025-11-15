package com.minecolonies.api.colony.buildings;

import javax.annotation.Nonnull;

import com.minecolonies.api.inventory.api.CombinedItemHandler;
import com.minecolonies.core.util.SortingUtils;

import net.minecraft.core.HolderLookup;

public interface ISortableBuilding
{
    public final int DEFAULT_REQUIRED_SORT_LEVEL = 3;

    /**
     * Returns the required sort level for this building.
     * By default, this will return {@link #DEFAULT_REQUIRED_SORT_LEVEL}.
     * Implementations of this method should return the sort level required for this building to be sorted.
     * @return The required sort level for this building.
     */
    default public int getRequiredSortLevel()
    {
        return DEFAULT_REQUIRED_SORT_LEVEL;
    }

    /**
     * Sort the inventory of this building using the given provider.
     * The implementation of this method is usually a call to {@link SortingUtils#sort(HolderLookup.Provider, CombinedItemHandler)}.
     * @param provider the provider to use for sorting.
     * @param inventoryHandler the inventory handler to sort.
     */
    default public void sort(@Nonnull final HolderLookup.Provider provider, final CombinedItemHandler inventoryHandler)
    {
        SortingUtils.sort(provider, inventoryHandler);
    }
}