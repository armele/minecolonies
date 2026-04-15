package com.minecolonies.core.entity.ai.cavalry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStable;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import java.util.EnumSet;

/**
 * Goal for a cavalry horse to return to its stable.
 */
public class ReturnToStableGoal extends Goal
{
    /**
     * The cavalry horse entity.
     */
    private final CavalryHorseEntity horse;

    /**
     * The speed at which the cavalry horse moves.
     */
    private final double speed;

    /**
     * The number of ticks to wait after dismounting before returning to the stable.
     */
    private final int LINGER_AFTER_DISMOUNT = 6000;

    /** 
     * Begin returning if further than this.
     */
    private final double startDistanceSqr;

    /**
     * The stable block position.
     */
    private BlockPos targetStable = null;

    /**
     * The stall block position.
     */
    private BlockPos targetStall = null;
    
    /**
     * Whether a stall has been found.
     */
    private boolean foundStall = false;

    /**
     * Creates a new ReturnToStableGoal.
     *
     * @param horse The cavalry horse entity.
     * @param speed The speed at which the cavalry horse moves.
     * @param startDistance The distance at which the cavalry horse starts returning to the stable.
     */
    public ReturnToStableGoal(CavalryHorseEntity horse, double speed, double startDistance) 
    {
        this.horse = horse;
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
        if (horse.level().isClientSide) return false;

        long lastDismountTime = horse.getLastDismountTime();

        if (lastDismountTime > 0 && horse.level().getGameTime() - lastDismountTime < LINGER_AFTER_DISMOUNT)
        {
            return false;
        }

        if (horse.getControllingPassenger() != null || horse.hasReservation() || horse.getAnimalData() == null)
        {
            return false;
        }

        if (horse.isInStable()) return false;

        validateHomeStable();

        IBuilding building = horse.getStableBuilding();
        if  (!(building instanceof BuildingStable))
        {
            return false;
        }

        double distSqr = horse.distanceToSqr(building.getPosition().getX() + 0.5, building.getPosition().getY() + 0.5, building.getPosition().getZ() + 0.5);

        if (distSqr <= startDistanceSqr)
        {
            return false;
        }

        targetStable = building.getPosition().immutable();

        return true;
    }

    /**
     * Validates the home stable of the horse.
     * <p>
     * If the horse is on the client side, does nothing.
     * If no colony is found at the horse's position, sets the horse's home building to null.
     * If no stable is found at the horse's position, sets the horse's home building to null.
     * Otherwise, sets the horse's home building to the found stable.
     */
    protected void validateHomeStable()
    {
        if (horse.level().isClientSide) return;

        IBuilding building = horse.getAnimalData().getHomeBuilding();

        // If the horse's stable is already built, do nothing
        if (building != null && building instanceof BuildingStable && building.isBuilt())
        {
            return;
        }

        IColony colony = IColonyManager.getInstance().getClosestColony(horse.level(), horse.blockPosition());

        // No colony found - set the stable to null
        if (colony == null) 
        {
            horse.getAnimalData().setHomeBuilding(null);
            return;
        }

        BlockPos stablePos = colony.getServerBuildingManager().getBestBuilding(horse.blockPosition(), BuildingStable.class);

        // No stable found - set the stable to null
        if (stablePos == null) 
        {
            horse.getAnimalData().setHomeBuilding(null);
            return;
        }

        // Set the stable as the horse's home
        building = colony.getServerBuildingManager().getBuilding(stablePos);

        horse.getAnimalData().setHomeBuilding(building);
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
        if (horse.level().isClientSide) return false;

        if (horse.getControllingPassenger() != null || horse.hasReservation())
        {
            return false;
        }
        
        BlockPos targetPostion = targetDestination();

        if (targetPostion == null) 
        {
            return false;
        }

        if (horse.isInStable()) 
        {
            if (!foundStall)
            {
                return true;
            }

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
    }

    /**
     * Stops the horse navigation goal and resets the target stable block position to null.
     */
    @Override
    public void stop()
    {
        foundStall = false;
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
        IBuilding b = horse.getStableBuilding();

        if (!(b instanceof BuildingStable stable)) return null;

        targetStable = b.getPosition().immutable();

        if (horse.isInStable())
        {
            if (targetStall == null)
            {
                targetStall = stable.getNextStallPosition();
            }

            return targetStall;
        }
        else
        {
            return targetStable;
        }
    }

    /**
     * Ticks the ReturnToStableGoal.
     * <p>
     * Retrieves the target destination block position using {@link #targetDestination()}.
     * If the target destination is null, returns immediately.
     * <p>
     * Walks to the target destination using {@link EntityNavigationUtils#walkToPos(Entity, BlockPos, int, boolean, double)}.
     * If the target destination is found and it is not equal to the target stable block position, sets foundStall to true.
     */
    @Override
    public void tick()
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
