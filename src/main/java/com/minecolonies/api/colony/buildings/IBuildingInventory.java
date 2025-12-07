package com.minecolonies.api.colony.buildings;
import net.minecraft.core.BlockPos;
import java.util.List;

import com.minecolonies.api.colony.IColony;

/**
 * Minimal interface to get building container positions.
 * Usable on either server or client side.
 */
public interface IBuildingInventory
{
    /**
     * Get the BlockPos of the Containers.
     *
     * @return containerList.
     */
    List<BlockPos> getContainers();

    /**
     * Get the colony from a building.
     * @return the colony it belongs to.
     */
    IColony getColony();
}