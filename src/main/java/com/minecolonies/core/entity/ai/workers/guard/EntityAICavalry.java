package com.minecolonies.core.entity.ai.workers.guard;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.combat.CombatAIStates;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.workers.util.GuardGear;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStable;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobCavalry;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.minecolonies.api.research.util.ResearchConstants.SHIELD_USAGE;
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_MAXIMUM;
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_WOOD_OR_GOLD;
import static com.minecolonies.api.util.constant.GuardConstants.SHIELD_BUILDING_LEVEL_RANGE;
import static com.minecolonies.api.util.constant.GuardConstants.SHIELD_LEVEL_RANGE;
import static com.minecolonies.api.util.constant.SchematicTagConstants.TAG_GROUNDLEVEL;
import static com.minecolonies.api.util.constant.SchematicTagConstants.TAG_PATROL_POINT;
import static com.minecolonies.api.util.constant.TranslationConstants.CAVALRY_NOHORSE;

/**
 * Cavalry AI
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class EntityAICavalry extends AbstractEntityAIGuard<JobCavalry, AbstractBuildingGuards>
{

    public final static int GUARD_MOUNT_INTERVAL = 50;
    public final static int HORSE_SEARCH_RADIUS = 50;

    protected CavalryHorseEntity targetMount = null;
    protected BlockPos stablePos = null;
    protected boolean stableChecked = false;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public EntityAICavalry(@NotNull final JobCavalry job)
    {
        super(job);
        super.registerTargets(
            new AITarget(CombatAIStates.FIND_MOUNT, this::findMount, GUARD_MOUNT_INTERVAL),
            new AITarget(CombatAIStates.FIND_STABLE, this::findStable, GUARD_MOUNT_INTERVAL)
        );

        toolsNeeded.add(ModEquipmentTypes.sword.get());

        for (final List<GuardGear> list : itemsNeeded)
        {
            list.add(new GuardGear(ModEquipmentTypes.shield.get(),
              EquipmentSlot.OFFHAND,
              TOOL_LEVEL_WOOD_OR_GOLD,
              TOOL_LEVEL_MAXIMUM,
              SHIELD_LEVEL_RANGE,
              SHIELD_BUILDING_LEVEL_RANGE));
        }

        new KnightCombatAI((EntityCitizen) worker, getStateAI(), this);
    }

    /**
     * Decides the AI state the citizen should be in, and transitions as necessary.
     * If the guard isn't mounted, will transition to the mount finding state.
     * Otherwise, will call the super decide method.
     *
     * @return the next AI state.
     */
    protected IAIState decide()
    {
        if (!worker.isPassenger())
        {
            return CombatAIStates.FIND_MOUNT;
        }
    
        return super.decide();
    }

    /**
     * Sleep activity
     *
     * If the guard is mounted, dismounts and clears the horse of the guard's reservation.
     *
     * @return the next state to go into
     */
    protected IAIState sleep()
    {
        if ((targetMount !=  null) && targetMount.getPassengers().contains(worker))
        {
            worker.stopRiding();
            targetMount.clearFor(worker);
            targetMount = null;
        }

        return super.sleep();
    }

    /**
     * Find a stable that might have available horses within range.
     *
     * @return the next AI state.
     */
    protected IAIState findStable()
    {
        IColony colony = worker.getCitizenColonyHandler().getColonyOrRegister();

        if (stablePos == null)
        {
            stablePos = colony.getServerBuildingManager().getBestBuilding(worker.blockPosition(), BuildingStable.class);
        }

        if (stablePos != null)
        {
            if (!EntityNavigationUtils.walkToPos(worker, stablePos, 2, true))
            {
                return CombatAIStates.FIND_STABLE;
            } 

            stableChecked = true;

            return CombatAIStates.FIND_MOUNT;
        }

        return CombatAIStates.NO_TARGET;
    }


    /**
     * Finds a horse to ride. If the horse is already assigned, ride it. Otherwise, find the closest horse and assign it to the guard.
     * If no horse is found, return NO_TARGET.
     * If the guard can't reach the horse, return FIND_MOUNT to try again.
     *
     * @return the next state to go to.
     */
    protected IAIState findMount()
    {
        CavalryHorseEntity horse = null;

        if (!validateMountTarget(targetMount))
        {
            horse = findNearestHorse();
            targetMount = horse;
        }

        if (targetMount == null)
        {
            // No cavalry horses found nearby - let's go to the stable.

            if (stableChecked)
            {
                // Already checked the stable - trigger an interaction indicating that the cavalry unit needs a horse.
                worker.getCitizenData().triggerInteraction(new StandardInteraction(
                    Component.translatable(CAVALRY_NOHORSE),
                    ChatPriority.IMPORTANT));

                JobCavalry cav = (JobCavalry) worker.getCitizenData().getJob();
                cav.setMissingMount(true);
                stableChecked = false;

                EntityNavigationUtils.walkToRandomPos(worker, 15, 0.6D);
                setDelay(200);

                return CombatAIStates.FIND_MOUNT;
            }

            return CombatAIStates.FIND_STABLE;
        }
        
        JobCavalry cav = (JobCavalry) worker.getCitizenData().getJob();
        cav.setMissingMount(false);

        if (worker.isPassenger())
        {
            // Already riding something
            return CombatAIStates.NO_TARGET;
        }

        targetMount.reserve(worker);

        if (!EntityNavigationUtils.walkToPos(worker, targetMount.blockPosition(), 2, true))
        {
            return CombatAIStates.FIND_MOUNT;
        }

        if (!targetMount.isAlive())
        {
            // Horse is dead... clear it out and try again.
            targetMount.clearFor(worker);
            targetMount = null;
            return CombatAIStates.FIND_MOUNT;
        }

        if (!targetMount.getPassengers().isEmpty() && !targetMount.getPassengers().contains(worker))
        {
            // Horse got a different passenger; releasing reservation
            targetMount.clearFor(worker);
            targetMount = null;
            return CombatAIStates.NO_TARGET;
        }

        boolean mounted = worker.startRiding(targetMount, true);

        if (mounted)
        {
            targetMount.clearFor(worker);
            return CombatAIStates.NO_TARGET;
        }
        else
        {
            return CombatAIStates.FIND_MOUNT;
        }
    }

    /**
     * If the building structure includes potential patrol points, pick one and use it.
     * Otherwise, use the hut (or tagged ground-level) Y and nominate one of the exterior corners.
     *
     * @param buildingPos The building position (hut block position) to patrol.
     * @return A patrol point designated by a tag, or a corner of the building.
     */
    protected BlockPos patrolPointForBuilding(final BlockPos targetPos)
    {
        if (targetPos == null || BlockPos.ZERO.equals(targetPos))
        {
            return null;
        }

        IBuilding targetBuilding = buildingGuards.getColony().getServerBuildingManager().getBuilding(targetPos);

        // The proposed patrol point was not a building location - just use it.
        if (targetBuilding == null)
        {
            return targetPos;
        }

        // Prefer explicit patrol points
        final List<BlockPos> patrolPoints = building.getLocationsFromTag(TAG_PATROL_POINT);
        final RandomSource rand = worker.getRandom();

        if (patrolPoints != null && !patrolPoints.isEmpty())
        {
            return patrolPoints.get(rand.nextInt(patrolPoints.size()));
        }

        // If our building has a parent building, use that
        if (targetBuilding.getParent() != null && !BlockPos.ZERO.equals(targetBuilding.getParent())) 
        {
            return patrolPointForBuilding(targetBuilding.getParent());
        }

        // Determine ground Y: from TAG_GROUNDLEVEL if present, else hut Y - 1
        final List<BlockPos> groundLevel = targetBuilding.getLocationsFromTag(TAG_GROUNDLEVEL);
        final int groundY =
            (groundLevel != null && !groundLevel.isEmpty()) ? groundLevel.get(0).getY() : targetBuilding.getPosition().below().getY();

        // Corners fallback
        final Tuple<BlockPos, BlockPos> corners = targetBuilding.getCorners();
        if (corners == null)
        {
            // Last resort: hut position (at computed ground Y)
            final BlockPos hut = targetBuilding.getPosition();
            return new BlockPos(hut.getX(), groundY, hut.getZ());
        }

        final BlockPos a = corners.getA();
        final BlockPos b = corners.getB();

        BlockPos patrolCorner = null;

        // Pick one of the four outside corners
        switch (rand.nextInt(4))
        {
            case 0:
                patrolCorner = new BlockPos(a.getX(), groundY, a.getZ());
            case 1:
                patrolCorner = new BlockPos(a.getX(), groundY, b.getZ());
            case 2:
                patrolCorner = new BlockPos(b.getX(), groundY, b.getZ());
            default:
                patrolCorner = new BlockPos(b.getX(), groundY, a.getZ());
        }

        return patrolCorner;
    }


    /**
     * Patrol between a list of patrol points.
     *
     * @return the next patrol point to go to.
     */
    public IAIState patrol()
    {
        if (buildingGuards.requiresManualTarget())
        {
            if (currentPatrolPoint == null || EntityNavigationUtils.walkCloseToXNearY(worker, currentPatrolPoint, currentPatrolPoint, 3, true, 1.0))
            {
                currentPatrolPoint = null;
                if (!EntityNavigationUtils.walkToRandomPos(worker, 20, 1.0))
                {
                    return getState();
                }

                if (worker.getRandom().nextInt(5) <= 1)
                {
                    BlockPos buildingPos = buildingGuards.getColony().getServerBuildingManager().getRandomBuilding(b -> true);
                    currentPatrolPoint = patrolPointForBuilding(buildingPos);

                    if (currentPatrolPoint != null && !BlockPos.ZERO.equals(currentPatrolPoint))
                    {
                        EntityNavigationUtils.walkCloseToXNearY(worker, currentPatrolPoint, buildingPos, 3, true, 1.0);
                    }
                }
            }
        }
        else
        {
            BlockPos buildingPos = buildingGuards.getNextPatrolTarget(false);

            if (buildingPos == null || BlockPos.ZERO.equals(buildingPos))
            {
                buildingPos = buildingGuards.getColony().getServerBuildingManager().getRandomBuilding(b -> true);
            }

            currentPatrolPoint = patrolPointForBuilding(buildingPos);

            if (currentPatrolPoint != null && !BlockPos.ZERO.equals(currentPatrolPoint) && (EntityNavigationUtils.walkCloseToXNearY(worker, currentPatrolPoint, buildingPos, 3, true, 1.0)))
            {
                setCurrentDelay(10);
                buildingGuards.arrivedAtPatrolPoint(worker);
            }
        }

        return null;
    }


    /** Validates a horse target for mounting.
     * 
     * The horse is a valid mount if it is not null, passes the horse filter, 
     * is on the same level as the worker, and is not reserved by anyone else.
     * 
     * @param horse the horse to validate
     * @return true if the horse is a valid target, false otherwise
     */
    private boolean validateMountTarget(CavalryHorseEntity horse)
    {
        if (horse == null || horse.level() != worker.level()) 
        {
            return false;
        }

        boolean baseOk = isAvailable(horse);

        if (!baseOk) 
        {
            return false;
        }
        
        UUID me = worker.getUUID();
        UUID who = horse.reservedBy();

        // valid if unreserved OR reserved by me
        return who == null || who.equals(me);
    }

    /** 
     * Available = alive, riderless, adult, tamed, not reserved 
     * 
     * @param h the horse to check
     */
    private static boolean isAvailable(CavalryHorseEntity h)
    {
        return h.isAlive() 
            && h.getPassengers().isEmpty() 
            && h instanceof CavalryHorseEntity
            && !h.isBaby() 
            && !h.hasReservation() 
            && h.isReadyForCombat()
            && EntitySelector.NO_SPECTATORS.test(h);
    }


    /**
     * Finds the nearest horse to the worker that is available for riding.
     * If a horse is found that is reserved by the worker, it is prioritized over other available horses.
     * @return the nearest available horse, or null if none are found
     */
    @Nullable protected CavalryHorseEntity findNearestHorse()
    {
        final Level level = worker.level();
        if (level.isClientSide) return null;

        final AABB box = worker.getBoundingBox().inflate(HORSE_SEARCH_RADIUS, 20.0, HORSE_SEARCH_RADIUS);
        final UUID me = worker.getUUID();

        // Pull a pool, then sort by reservation priority and distance
        List<CavalryHorseEntity> pool = level.getEntitiesOfClass(CavalryHorseEntity.class, box, EntitySelector.NO_SPECTATORS);

        if (pool.isEmpty())
        {
            return null;
        }

        // 1) Prefer a horse reserved by me (if any and still riderless/adult/tamed)
        CavalryHorseEntity mine = pool.stream()
            .filter(h -> h.reservedBy() != null && h.reservedBy().equals(me))
            .filter(h -> h.getPassengers().isEmpty() && !h.isBaby() && h.isAlive())
            .min(Comparator.comparingDouble(worker::distanceToSqr))
            .orElse(null);

        if (mine != null) return mine;

        // 2) Otherwise pick the nearest available cavalry horse
        CavalryHorseEntity available =
            pool.stream().filter(EntityAICavalry::isAvailable).min(Comparator.comparingDouble(worker::distanceToSqr)).orElse(null);

        return available;
    }
    
    /**
     * Adds a shield to the list of items that are nice to have if the shield usage research is enabled.
     * @return the list of items nice to have.
     */
    @NotNull
    @Override
    protected List<ItemStorage> itemsNiceToHave()
    {
        final List<ItemStorage> list = super.itemsNiceToHave();
        if (worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(SHIELD_USAGE) > 0)
        {
            list.add(new ItemStorage(Items.SHIELD, 1));
        }
        return list;
    }
}
