package com.minecolonies.core.entity.ai.workers.guard;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.combat.CombatAIStates;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
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

import org.jetbrains.annotations.NotNull;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.swing.text.html.parser.Entity;

import static com.minecolonies.api.research.util.ResearchConstants.SHIELD_USAGE;
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_MAXIMUM;
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_WOOD_OR_GOLD;
import static com.minecolonies.api.util.constant.GuardConstants.SHIELD_BUILDING_LEVEL_RANGE;
import static com.minecolonies.api.util.constant.GuardConstants.SHIELD_LEVEL_RANGE;

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
            stablePos = colony.getBuildingManager().getBestBuilding(worker.getOnPos(), BuildingStable.class);
        }

        if (stablePos != null)
        {
            if (!EntityNavigationUtils.walkToPos(worker, stablePos, 2, true))
            {
                Log.getLogger().info("{}: Walking to stable.", worker.getName());

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
            Log.getLogger().info("No horses found nearby - let's go to the stable.");

            if (stableChecked)
            {
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
            Log.getLogger().info("Already mounted.");
            return CombatAIStates.NO_TARGET;
        }

        targetMount.reserve(worker);

        if (!EntityNavigationUtils.walkToPos(worker, targetMount.blockPosition(), 2, true))
        {
            Log.getLogger().info("{} walking to horse at {}", worker.getName(), worker.blockPosition());

            return CombatAIStates.FIND_MOUNT;
        }

        if (!targetMount.isAlive())
        {
            Log.getLogger().info("Horse died/despawned before mount.");
            targetMount.clearFor(worker);
            targetMount = null;
            return CombatAIStates.FIND_MOUNT;
        }

        if (!targetMount.getPassengers().isEmpty() && !targetMount.getPassengers().contains(worker))
        {
            Log.getLogger().info("Horse got a different passenger {}; releasing reservation.", targetMount.getPassengers().toArray());
            targetMount.clearFor(worker);
            targetMount = null;
            return CombatAIStates.NO_TARGET;
        }

        boolean mounted = worker.startRiding(targetMount, true);

        Log.getLogger().info("Mount attempt result={}", mounted);

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
     * Provides a random patrol point from all buildings in the colony when the guard is set to automatic patrol mode.
     * <p>
     * The algorithm works as follows:
     * <ol>
     *     <li>Choose a random building in the colony.</li>
     *     <li>Get the corners of the building's footprint.</li>
     *     <li>Surface each corner by finding the topmost solid block at that x,z position.</li>
     *     <li>Choose the closest valid surfaced corner to the guard.</li>
     * </ol>
     * @return a BlockPos of the patrol point.
     */
    protected BlockPos automaticPatrolPoint()
    {
        BlockPos buildingPos = buildingGuards.getColony().getBuildingManager().getRandomBuilding(b -> true);

        IBuilding building = buildingGuards.getColony().getBuildingManager().getBuilding(buildingPos);

        BlockPos patrolPoint = EntityNavigationUtils.closestOutsideCornerofBuilding(building, buildingGuards.getGuardPos(worker));

        Log.getLogger().info("Cavalry Patrol point: {}", patrolPoint);

        return patrolPoint;
    }


    /** Validates a horse target for mounting.
     * 
     * The horse is valid if it is not null, passes the horse filter, is on the same level as the worker, and is not reserved by anyone else.
     * 
     * @param horse the horse to validate
     * @return true if the horse is a valid target, false otherwise
     */
    private boolean validateMountTarget(CavalryHorseEntity horse)
    {
        if (horse == null || horse.level() != worker.level()) return false;

        boolean baseOk = isAvailable(horse);

        if (!baseOk) return false;

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
    protected CavalryHorseEntity findNearestHorse()
    {
        final Level level = worker.level();
        if (level.isClientSide) return null;

        final AABB box = worker.getBoundingBox().inflate(HORSE_SEARCH_RADIUS, 20.0, HORSE_SEARCH_RADIUS);
        final UUID me = worker.getUUID();

        // Pull a pool, then sort by reservation priority and distance
        List<CavalryHorseEntity> pool = level.getEntitiesOfClass(CavalryHorseEntity.class, box, EntitySelector.NO_SPECTATORS);
        if (pool.isEmpty())
        {
            Log.getLogger().info("No horses in search AABB.");
            return null;
        }

        // 1) Prefer a horse reserved by me (if any and still riderless/adult/tamed)
        CavalryHorseEntity mine = pool.stream()
            .filter(h -> h.reservedBy() != null && h.reservedBy().equals(me))
            .filter(h -> h.getPassengers().isEmpty() && !h.isBaby() && h.isAlive())
            .min(Comparator.comparingDouble(worker::distanceToSqr))
            .orElse(null);
        if (mine != null) return mine;

        // 2) Otherwise pick the nearest truly available horse (unreserved, riderless, adult, tamed)
        CavalryHorseEntity available =
            pool.stream().filter(EntityAICavalry::isAvailable).min(Comparator.comparingDouble(worker::distanceToSqr)).orElse(null);

        if (available == null)
        {            
            Log.getLogger().info("{}: No available (unreserved, riderless, adult, tamed) horses found.", worker.getName());
        }

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
