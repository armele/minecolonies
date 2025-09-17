package com.minecolonies.core.entity.ai.minimal;

import com.minecolonies.core.entity.other.CavalryHorseEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;

public class CavalryHorseReturnToStableGoal extends Goal
{
    private final CavalryHorseEntity horse;
    private final PathNavigation nav;
    private final double speed;

    // Begin returning if further than this.
    private final double startDistanceSqr;

    // Stop returning if closer than this.
    private final double stopDistanceSqr;

    private int repathCooldown = 0;
    private int stuckTimer = 0;
    private double lastDistToTarget = Double.MAX_VALUE;
    @Nullable
    private BlockPos targetStable = null;


    public CavalryHorseReturnToStableGoal(CavalryHorseEntity horse, double speed, double startDistance, double stopDistance) {
        this.horse = horse;
        this.nav = horse.getNavigation();
        this.speed = speed;
        this.startDistanceSqr = startDistance * startDistance;
        this.stopDistanceSqr  = stopDistance  * stopDistance;
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
        if (horse.isPassenger()) return false;

        Optional<BlockPos> opt = horse.getStablePos();
        if (opt.isEmpty()) return false;

        BlockPos stable = opt.get();
        double distSqr = horse.distanceToSqr(stable.getX() + 0.5, stable.getY() + 0.5, stable.getZ() + 0.5);
        if (distSqr <= startDistanceSqr) return false;

        targetStable = stable.immutable();
        return true;
    }

    /**
     * Check if the goal can continue to be used.
     * <p>
     * Conditions to return false:
     * <ul>
     *     <li>The horse has a passenger.</li>
     *     <li>The horse does not have a stable block position (targetStable is null).</li>
     *     <li>The horse is within the stop distance (blocks) of the stable block position.</li>
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
        if (horse.isPassenger()) return false;
        if (targetStable == null) return false;

        double distSqr = horse.distanceToSqr(targetStable.getX() + 0.5, targetStable.getY() + 0.5, targetStable.getZ() + 0.5);
        if (distSqr <= stopDistanceSqr) return false;
        return !nav.isDone();
    }

    /**
     * Resets the repath cooldown and stuck timer, and sets the last distance to target to max value.
     * Then tries to repath the horse to the target stable block position.
     */
    @Override
    public void start()
    {
        repathCooldown = 0;
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
        nav.stop();
        targetStable = null;
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
        if (targetStable == null) 
        {
            return;
        }

        // Repath occasionally or if nav finished early
        if (repathCooldown-- <= 0 || nav.isDone())
        {
            tryRepath();
        }

        // Face next node for stability
        if (nav.getPath() != null && !nav.getPath().isDone())
        {
            BlockPos next = nav.getPath().getNextNodePos();
            horse.getLookControl().setLookAt(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, 30.0F, 30.0F);
        }

        // No-progress breaker: if we aren't getting closer for ~1s, recompute
        double distNow = horse.distanceToSqr(targetStable.getX() + 0.5, targetStable.getY() + 0.5, targetStable.getZ() + 0.5);
        if (distNow + 0.01 >= lastDistToTarget)
        { // +epsilon to avoid float jitter
            if (++stuckTimer > 20)
            {
                nav.recomputePath();
                stuckTimer = 0;
                repathCooldown = 10;
            }
        }
        else
        {
            stuckTimer = 0;
        }
        lastDistToTarget = distNow;
    }


    /**
     * Attempts to recompute the path to the target stable position. If the target stable block position is null, does nothing.
     * Sets the repath cooldown to 10 ticks (~0.5 seconds) after attempting to recompute the path.
     */
    private void tryRepath()
    {
        if (targetStable == null) return;

        Vec3 dest = new Vec3(targetStable.getX() + 0.5, targetStable.getY(), targetStable.getZ() + 0.5);
        nav.moveTo(dest.x, dest.y, dest.z, speed);
        repathCooldown = 10;
    }
}
