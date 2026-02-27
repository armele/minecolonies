package com.minecolonies.core.colony.buildings.workerbuildings;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.modules.settings.ISettingKey;
import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.buildings.modules.AnimalHerdingModule;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.settings.IntSetting;
import com.minecolonies.core.colony.buildings.modules.settings.SettingKey;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.Items;

import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;

public class BuildingStable extends AbstractBuildingGuards
{
    private final static String STALL_STRUCTURE_TAG = "stall";
    private static final String NBT_LAST_PATROL_TAG    = "lastPatrolTime";

    public static final ISettingKey<IntSetting> PATROL_INTERVAL =
      new SettingKey<>(IntSetting.class, new ResourceLocation(com.minecolonies.api.util.constant.Constants.MOD_ID, "patrolinterval"));

    private long lastPatrolTime = 0;

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

    /**
     * Deserializes the compound tag and sets the last patrol time.
     * @param compound the compound tag to read from.
     */
    @Override
    public void deserializeNBT(final CompoundTag compound)
    {
        super.deserializeNBT(compound);
        this.lastPatrolTime = compound.getLong(NBT_LAST_PATROL_TAG);
    }


    /**
     * Serializes the data of this building to NBT.
     * @return the serialized compound tag.
     */
    @Override
    public CompoundTag serializeNBT()
    {        
        final CompoundTag compound = super.serializeNBT();
        compound.putLong(NBT_LAST_PATROL_TAG, lastPatrolTime);
        return compound;
    }

    /**
     * Returns the task that the guards should perform when patrolling.
     * <p>
     * This can be either 'patrol', 'patrol_mine', or 'follow'.
     * <p>
     * The task is determined by the setting in the Stable Settings module.
     * @return the task to perform when patrolling
     */
    @Override
    public String getTask()
    {
        return getModule(BuildingModules.STABLE_SETTINGS).getSetting(GUARD_TASK).getValue();
    }

    /**
     * Gets the last time the guards patrolled from this stable.
     * @return the game time of when the guards last patrolled.
     */    
    public long getLastPatrolTime()
    {
        return lastPatrolTime;
    }

    /**
     * Sets the last time the guards patrolled from this stable.
     * @param lastPatrolTime the time in milliseconds since the epoch
     */
    public void setLastPatrolTime(long lastPatrolTime)
    {
        this.lastPatrolTime = lastPatrolTime;
    }

    /**
     * Returns the time in milliseconds since the last patrol from this stable.
     * This is based on the game time of the world.
     * @return the time in milliseconds since the last patrol.
     */
    public int minutesSinceLastPatrol()
    {
        long ticks = this.getColony().getWorld().getGameTime() - lastPatrolTime;
        int minutes = (int) ticks / TICKS_SECOND / 60;
        return minutes;
    }
}