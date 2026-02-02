package com.minecolonies.core.entity.ai.cavalry;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStable;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.phys.Vec3;

/*
 * A simple override of WaterAvoidingRandomStrollGoal to prevent cavalry horses from wandering out of stables,
 * if they are in one.
 */
public class CavalryStrollGoal extends WaterAvoidingRandomStrollGoal
{
    private BlockPos stall = null;

    public CavalryStrollGoal(CavalryHorseEntity horse, double speed)
    {
        super(horse, speed);
    }

    @Override
    public void start() 
    {
        if (mob instanceof CavalryHorseEntity horse && horse.isInStable())
        {
            IBuilding building = horse.getStableBuilding();

            if (building instanceof BuildingStable stable && stall == null)
            {
                stall = stable.getNextStallPosition();
            }
        }

        super.start();
    }

    /**
     * Returns the position of the horse to move to. If the horse is in a stable, this will return the next available stall position.
     * Otherwise, it will call the superclass method to get the position.
     * 
     * @return the position to move to
     */
    protected Vec3 getPosition()
    {
        if (mob instanceof CavalryHorseEntity horse)
        {
            if (horse.isInStable() && stall != null) 
            {
                return new Vec3(stall.getX() + 0.5, stall.getY() + 0.5, stall.getZ() + 0.5);
            }
            else
            {
                // If the horse is not in a stable any more, reset the cached stall position
                stall = null;
            }
        }

        return super.getPosition();
    }

    /**
     * Resets the stall position to null and stops the goal.
     */
    @Override
    public void stop()
    {
        stall = null;
        super.stop();
    }
}
