package com.minecolonies.core.entity.ai.cavalry;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStable;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

public class ValidateStableGoal extends Goal 
{
    private final CavalryHorseEntity horse;
    private static final int MAX_COOLDOWN = 200;
    private int cooldown = MAX_COOLDOWN;

    public ValidateStableGoal(CavalryHorseEntity horse) 
    {
        this.horse = horse;
    }

    /**
     * Returns true if the horse is currently not set to a valid stable, meaning the horse should find a new stable.
     * This handles use cases of stables being destroyed or moved.
     *
     * @return true if the horse is not set to a valid stable, false otherwise
     */
    @Override
    public boolean canUse()
    {
        if (cooldown-- > 0) return false;
        if (horse.getAnimalData() == null) return true;
        if (horse.getAnimalData().getHomeBuilding() == BlockPos.ZERO) return true;

        cooldown = MAX_COOLDOWN;
        boolean validStable = false;

        // Verify that the position set as a stable is really still a stable.
        IBuilding stable = horse.getAnimalData().getHomeBuilding();
    
        if (stable != null && stable instanceof BuildingStable)
        {
            validStable = true;
        }

        return !validStable;
    }

    /**
     * This goal will never continue to be used once it has been started.
     * <p>
     * This is because the goal is only used to find the closest stable to the horse's current position and set it as the horse's stable.
     */
    @Override
    public boolean canContinueToUse() 
    {
        return false;
    }

    /**
     * Called when the goal is started. This goal will find the closest stable to the horse's current position and set it as the horse's stable.
     * <p>
     * This goal will only be active while the horse is finding a stable, and will be stopped once the horse has a stable set.
     */
    @Override
    public void start()
    {
        IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(horse.level(), horse.getOnPos());
        BlockPos stablePos = colony.getBuildingManager().getBestBuilding(horse.getOnPos(), BuildingStable.class);
        IBuilding building = colony.getBuildingManager().getBuilding(stablePos);

        horse.getAnimalData().setHomeBuilding(building);

        Log.getLogger().info("ValidateStableGoal - set stable position for horse {}: {}", horse.getUUID(), stablePos);
    }
    
}
