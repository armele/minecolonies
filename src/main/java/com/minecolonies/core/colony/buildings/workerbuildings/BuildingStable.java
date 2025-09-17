package com.minecolonies.core.colony.buildings.workerbuildings;

import org.jetbrains.annotations.NotNull;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.AnimalHerdingModule;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.Items;

public class BuildingStable extends AbstractBuilding
{
    
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
}