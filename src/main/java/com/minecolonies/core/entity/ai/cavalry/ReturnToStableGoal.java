package com.minecolonies.core.entity.ai.cavalry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import javax.annotation.Nullable;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStable;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import java.util.EnumSet;

public class ReturnToStableGoal extends Goal
{
    private final CavalryHorseEntity horse;
    private final PathNavigation nav;
    private final double speed;

    // Begin returning if further than this.
    private final double startDistanceSqr;

    private int stuckTimer = 0;
    private double lastDistToTarget = Double.MAX_VALUE;
    
    @Nullable
    private BlockPos lastTarget = BlockPos.ZERO;
    private BlockPos targetStable = null;
    private BlockPos targetStall = null;
    
    private boolean foundStall = false;

    public ReturnToStableGoal(CavalryHorseEntity horse, double speed, double startDistance) 
    {
        this.horse = horse;
        this.nav = horse.getNavigation();
        this.speed = speed;
        this.startDistanceSqr = startDistance * startDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Check if the goal can be used.
     * <p>
     * Conditions to return false:
     * <ul>
     *     <li>The horse has a passenger.</li>
     *     <li>The horse does not have a stable block position.</li>
     *     <li>The horse is within the start distance (blocks) of the stable block position.</li>
     * </ul>
     * <p>
     * If the goal is usable, the stable block position is set to be the immutable target block position.
     *
     * @return true if the goal can be used, false otherwise
     */
    @Override
    public boolean canUse()
    {
        if (horse.getControllingPassenger() != null || horse.hasReservation() || horse.getAnimalData() == null)
        {
            // Log.getLogger().info("ReturnToStable - Horse {} at {} has a rider or a reservation to be ridden and cannot run.", this.horse.getUUID(), this.horse.getOnPos());
            return false;
        }

        // If a citizen has the horse leashed, let this goal run.
        Entity holder = horse.getLeashHolder();
        if (holder instanceof EntityCitizen) return true;

        if (horse.isInStable()) return false;

        IBuilding building = horse.getAnimalData().getHomeBuilding();
        if (building == null) return false;

        double distSqr = horse.distanceToSqr(building.getPosition().getX() + 0.5, building.getPosition().getY() + 0.5, building.getPosition().getZ() + 0.5);
        if (distSqr <= startDistanceSqr) return false;

        targetStable = building.getPosition().immutable();
        lastTarget = targetStable;

        return true;
    }

    /**
     * Check if the goal can continue to be used.
     * <p>
     * Conditions to return false:
     * <ul>
     *     <li>The horse has a passenger.</li>
     *     <li>The horse does not have a stable block position (targetStable is null).</li>
     *     <li>The horse is within the stop distance (blocks) of the stable block position, and has not found a stall.</li>
     *     <li>The navigation path is done.</li>
     * </ul>
     * <p>
     * If the goal is usable, the horse will continue to move towards the stable block position.
     *
     * @return true if the goal can continue to be used, false otherwise
     */
    @Override
    public boolean canContinueToUse()
    {
        
        if (horse.getControllingPassenger() != null || horse.hasReservation())
        {
            // Log.getLogger().info("ReturnToStable - Horse {} at {} has a rider or a reservation to be ridden and cannot continue.", this.horse.getUUID(), this.horse.getOnPos());
            return false;
        }
        
        BlockPos targetPostion = targetDestination();

        if (targetPostion == null) 
        {
            Log.getLogger().info("ReturnToStable - Horse {} at {} has no targetPostion.", this.horse.getUUID(), this.horse.getOnPos());
            return false;
        }

        if (horse.isInStable()) 
        {
            if (!foundStall)
            {
                return true;
            }

            Log.getLogger().info("ReturnToStable - Horse {} at {} is in the stable.", this.horse.getUUID(), this.horse.getOnPos());
            return false;
        }

        return true;
    }

    /**
     * Resets the repath cooldown and stuck timer, and sets the last distance to target to max value.
     * Then tries to repath the horse to the target stable block position.
     */
    @Override
    public void start()
    {
        foundStall = false;
        targetStall = null;
        
        EntityCitizen rider = (EntityCitizen) horse.getControllingPassenger();
        Log.getLogger().info("ReturnToStable - rider {} Starting horse {} at {}", rider == null ? "null" : rider.getName(), this.horse.getUUID(), this.horse.getOnPos());

        stuckTimer = 0;
        lastDistToTarget = Double.MAX_VALUE;
        tryRepath();
    }

    /**
     * Stops the horse navigation goal and resets the target stable block position to null.
     */
    @Override
    public void stop()
    {
        foundStall = false;
        EntityCitizen rider = (EntityCitizen) horse.getControllingPassenger();
        Log.getLogger().info("ReturnToStable - rider {} Stopping horse {} at {}", rider == null ? "null" : rider.getName(), this.horse.getUUID(), this.horse.getOnPos());

        nav.stop();
        targetStable = null;
        targetStall = null;
    }

    /**
     * Gets the target destination block position for the horse navigation goal.
     * <p>
     * If the horse is in a stable, the target destination is the next available stable position.
     * Otherwise, the target destination is the stable block position set by the goal.
     * <p>
     * If the horse is not in a stable and the target stable block position is null, returns null.
     * @return the target destination block position, or null if the horse is not in a stable and the target stable block position is null
     */
    public BlockPos targetDestination()
    {
        BuildingStable stable = (BuildingStable) horse.getStableBuilding();

        if (stable == null)
        {
            // Log.getLogger().info("ReturnToStable - No stable for horse {} at {}", this.horse.getUUID(), this.horse.getOnPos());
            return null;
        }

        targetStable = stable.getPosition();

        if (horse.isInStable())
        {
            if (targetStall == null)
            {
                targetStall = stable.getNextStallPosition();
                // Log.getLogger().info("ReturnToStable - targetStall for horse {} is {}", this.horse.getUUID(), targetStall);
            }

            return targetStall;
        }
        else
        {
            // Log.getLogger().info("ReturnToStable - horse {} not in stable, targetStable is {}", this.horse.getUUID(), targetStable);
            return targetStable;
        }
    }

    /**
     * Ticks the horse navigation goal. If the horse is being ridden by a guard with the JobCavalry, moves the horse to the target
     * position of the rider and sets the horse's rotation to face the target position.
     * 
     * @see #canUse()
     */
    @Override
    public void tick()
    {
        BlockPos targetDestination = targetDestination();
        if (targetDestination == null) 
        {
            return;
        }

        // Repath if nav is reporting as done, or the target destination changes
        // but we're not back to the stable yet (goal still active)
        if (nav.isDone() || !lastTarget.equals(targetDestination))
        {
            lastTarget = targetDestination;
            tryRepath();
        }

        // Face next node for stability
        if (nav.getPath() != null && !nav.getPath().isDone())
        {
            BlockPos next = nav.getPath().getNextNodePos();
            horse.getLookControl().setLookAt(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, 30.0F, 30.0F);
        }

        // No-progress breaker: if we aren't getting closer for ~1s, recompute
        double distNow = horse.distanceToSqr(targetDestination.getX() + 0.5, targetDestination.getY() + 0.5, targetDestination.getZ() + 0.5);

        // +epsilon to avoid float jitter
        if (distNow + 0.01 >= lastDistToTarget)
        { 
            if (++stuckTimer > 20)
            {
                nav.recomputePath();
                stuckTimer = 0;
            }
        }
        else
        {
            stuckTimer = 0;
        }

        lastDistToTarget = distNow;
    }


    /**
     * Attempts to recompute the path to the target position. If the target position is null, does nothing.
     */
    private void tryRepath()
    {
        BlockPos targetDestination = targetDestination();
        if (targetDestination == null) 
        {
            return;
        }

        boolean foundTarget = EntityNavigationUtils.walkToPos(horse, targetDestination, 2, false, speed);

        if (foundTarget && !targetDestination.equals(targetStable))
        {
            foundStall = true;
        }

    }
}
