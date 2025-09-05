package com.minecolonies.core.entity.ai.minimal;

import java.util.EnumSet;

import com.ldtteam.blockui.mod.Log;
import com.minecolonies.core.colony.jobs.JobCavalry;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.other.CavalryHorseEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

public class CavalryHorseNavigationGoal extends Goal
{
    private final CavalryHorseEntity horse;
    private final double speed;
    private int repathCooldownTimer = 0;
    private static final int REPATH_COOLDOWN = 10;
    private BlockPos lastDest = null;

    public CavalryHorseNavigationGoal(CavalryHorseEntity horse, double speed)
    {
        this.horse = horse;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Check if the horse is being ridden by a guard with the JobCavalry
     * 
     * @return true if the horse is being ridden by a guard with the JobCavalry, false otherwise
     */
    @Override
    public boolean canUse()
    {
        LivingEntity rider = horse.getControllingPassenger();

        if (!(rider instanceof EntityCitizen guard) || guard.getCitizenJobHandler() == null)
        {
            Log.getLogger().info("CavalryHorseNavigationGoal: No rider, or rider is not a guard");

            return false;
        }

        boolean canUse = guard.getCitizenJobHandler().getColonyJob() instanceof JobCavalry;

        Log.getLogger().info("CavalryHorseNavigationGoal: {}", canUse);

        return canUse;
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
        EntityCitizen rider = (EntityCitizen) horse.getControllingPassenger();

        if (rider == null)
        {
            return;
        }

        BlockPos dest = rider.getNavigation().getTargetPos();

        if (dest == null)
        {
            return;
        }

        PathNavigation nav = horse.getNavigation();

        boolean destChanged = !dest.equals(lastDest);
        if (repathCooldownTimer-- <= 0 || destChanged || nav.isDone())
        {
            nav.moveTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, speed);
            repathCooldownTimer = REPATH_COOLDOWN;
            lastDest = dest;

            Log.getLogger().info("CavHorse repath to {}", dest);
        }

        nav.moveTo(dest.getX() + .5, dest.getY(), dest.getZ() + .5, speed);
        horse.getLookControl().setLookAt(dest.getX() + .5, dest.getY() + 1, dest.getZ() + .5, 30, 30);

        if (nav.getPath() != null && !nav.getPath().isDone())
        {
            BlockPos node = nav.getPath().getNextNodePos();
            horse.getLookControl().setLookAt(node.getX() + 0.5, node.getY(), node.getZ() + 0.5, 30.0F, 30.0F);

            // small jump assist on 1-block rises (optional)
            if (node.getY() > horse.getBlockY() && node.getY() - horse.getBlockY() <= 1)
            {
                if (horse.distanceToSqr(node.getX() + 0.5, node.getY(), node.getZ() + 0.5) < 1.2)
                {
                    horse.getJumpControl().jump();
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse()
    {
        return canUse() && !horse.getNavigation().isDone();
    }
}
