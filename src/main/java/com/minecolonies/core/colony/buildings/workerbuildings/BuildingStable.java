package com.minecolonies.core.colony.buildings.workerbuildings;

import java.util.List;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.AnimalHerdingModule;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.Items;

public class BuildingStable extends AbstractBuilding
{
    protected final static String STALL_STRUCTURE_TAG = "stall";

    private List<BlockPos> stablePositions;
    private int lastStable = -1;
    private boolean initStables = false;
    
    public BuildingStable(@NotNull IColony colony, BlockPos pos)
    {
        super(colony, pos);
    }

    /**
     * Gets the schematic name of the building.
     *
     * @return the schematic name of the building.
     */
    @Override
    public String getSchematicName()
    {
        return ModBuildings.STABLE_ID;
    }

    public static class HerdingModule extends AnimalHerdingModule
    {

        public HerdingModule()
        {
            super(ModJobs.stablemaster.get(), a -> a instanceof Horse, new ItemStorage(Items.GOLDEN_APPLE, 2));
        }
    }


    /**
     * Called when the building has finished upgrading. Resets the flag to re-check for stable positions.
     * <p>
     * This is necessary because the when the building is upgraded, we need to re-read the
     * positions to ensure that the new stable positions are known.
     */
    @Override
    public void onUpgradeComplete(final int newlevel)
    {
        initStables = false;
        super.onUpgradeComplete(newlevel);
    }


    /**
     * Reads the tag positions
     */
    public void initTagPositions()
    {
        if (initStables)
        {
            return;
        }

        stablePositions = getLocationsFromTag(STALL_STRUCTURE_TAG);
        
        if (stablePositions.isEmpty())
        {
            Log.getLogger().warn("No stall positions found for stable at {}. Use the '" + STALL_STRUCTURE_TAG + "' tag to add some.", getPosition());
        }

        initStables = true;
    }

    /**
     * Gets the next stable position to use for a horse. Just keeps iterating the aviable positions, 
     * so we do not have to keep track of what horse is where.
     *
     * @return horse stable position
     */
    public BlockPos getNextStallPosition()
    {
        initTagPositions();

        if (stablePositions.isEmpty())
        {
            return null;
        }

        lastStable++;

        if (lastStable >= stablePositions.size())
        {
            lastStable = 0;
        }

        return stablePositions.get(lastStable);
    }
}