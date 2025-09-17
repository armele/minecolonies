package com.minecolonies.core.entity.ai.workers.guard;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.combat.CombatAIStates;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.workers.util.GuardGear;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStable;
import com.minecolonies.core.colony.jobs.JobCavalry;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.other.CavalryHorseEntity;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import static com.minecolonies.api.research.util.ResearchConstants.SHIELD_USAGE;
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_MAXIMUM;
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_WOOD_OR_GOLD;
import static com.minecolonies.api.util.constant.GuardConstants.SHIELD_BUILDING_LEVEL_RANGE;
import static com.minecolonies.api.util.constant.GuardConstants.SHIELD_LEVEL_RANGE;



/**
 * Cavalry AI
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class EntityAICavalry extends AbstractEntityAIGuard<JobCavalry, AbstractBuildingGuards>
{

    public final static int GUARD_MOUNT_INTERVAL = 50;
    public final static int HORSE_SEARCH_RADIUS = 50;
    private static final String RESERVE_KEY = "horse_reserved_for_cavalry";

    protected AbstractHorse targetMount = null;
    protected BlockPos stablePos = null;

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
            clearIfMine(targetMount, worker.getUUID());
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
                Log.getLogger().info("Walking to stable.");

                return CombatAIStates.FIND_STABLE;
            } 

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
        AbstractHorse horse = null;

        if (!validateMountTarget(targetMount))
        {
            horse = findNearestHorse();
            targetMount = horse;
        }

        if (targetMount == null)
        {
            Log.getLogger().info("No horses found nearby - let's go to the stable.");
            return CombatAIStates.FIND_STABLE;
        }

        if (worker.isPassenger())
        {
            // Already riding something
            Log.getLogger().info("Already mounted.");
            return CombatAIStates.NO_TARGET;
        }

        reserveFor(targetMount, worker);

        if (!EntityNavigationUtils.walkToPos(worker, targetMount.blockPosition(), 2, true))
        {
            Log.getLogger().info("Walking to horse.");

            return CombatAIStates.FIND_MOUNT;
        }

        if (!targetMount.isAlive())
        {
            Log.getLogger().info("Horse died/despawned before mount.");
            clearIfMine(targetMount, worker.getUUID());
            targetMount = null;
            return CombatAIStates.FIND_MOUNT;
        }

        if (!targetMount.getPassengers().isEmpty() && !targetMount.getPassengers().contains(worker))
        {
            Log.getLogger().info("Horse got a different passenger {}; releasing reservation.", targetMount.getPassengers().toArray());
            clearIfMine(targetMount, worker.getUUID());
            targetMount = null;
            return CombatAIStates.NO_TARGET;
        }

        targetMount = CavalryHorseEntity.createFromVanilla(worker.level, targetMount);

        if (targetMount == null)
        {
            Log.getLogger().info("Could not be converted to a CavalryHorseEntity; releasing reservation.");
            clearIfMine(targetMount, worker.getUUID());
            targetMount = null;
            return CombatAIStates.NO_TARGET;
        }

        boolean mounted = worker.startRiding(targetMount, true);
        Log.getLogger().info("Mount attempt result={}", mounted);

        if (mounted)
        {
            clearIfMine(targetMount, worker.getUUID());
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

    /**
     * Reserves the given horse for the given cavalry unit.
     * 
     * @param horse the horse to reserve
     * @param reserver the entity to reserve the horse for
     */
    private static void reserveFor(@Nonnull AbstractHorse horse, @Nonnull Entity reserver)
    {
        CompoundTag data = horse.getPersistentData();
        data.putUUID(RESERVE_KEY, reserver.getUUID());
    }

    /**
     * Clears the reservation on the horse if it is reserved by the entity with the given UUID.
     * @param horse the horse to check
     * @param me the UUID to check against
     */
    private static void clearIfMine(AbstractHorse horse, UUID me)
    {
        CompoundTag data = horse.getPersistentData();
        if (data.contains(RESERVE_KEY, Tag.TAG_INT_ARRAY))
        {
            UUID who = data.getUUID(RESERVE_KEY);
            if (me.equals(who))
            {
                data.remove(RESERVE_KEY);
            }
        }
    }


    /** Validates a horse target for mounting.
     * 
     * The horse is valid if it is not null, passes the horse filter, is on the same level as the worker, and is not reserved by anyone else.
     * 
     * @param horse the horse to validate
     * @return true if the horse is a valid target, false otherwise
     */
    private boolean validateMountTarget(AbstractHorse horse)
    {
        if (horse == null || horse.level() != worker.level()) return false;

        boolean baseOk = isAvailable(horse);

        if (!baseOk) return false;

        UUID me = worker.getUUID();
        UUID who = reservedBy(horse);

        // valid if unreserved OR reserved by me
        return who == null || who.equals(me);
    }

    /**
     * Retrieves the UUID of the entity that has reserved the horse, or null if no one has reserved it.
     * @param horse the horse to query
     * @return the UUID of the reserver, or null if no one has reserved it
     */
    private static UUID reservedBy(AbstractHorse horse)
    {
        CompoundTag data = horse.getPersistentData();
        return data.contains(RESERVE_KEY, Tag.TAG_INT_ARRAY) ? data.getUUID(RESERVE_KEY) : null;
    }

    /**
     * Returns true if the horse is reserved by any entity, false otherwise.
     * @param horse the horse to check
     * @return true if the horse is reserved, false otherwise
     */
    private static boolean hasReservation(AbstractHorse horse)
    {
        return reservedBy(horse) != null;
    }

    /** 
     * Available = alive, riderless, adult, tamed, not reserved 
     * 
     * @param h the horse to check
     */
    private static boolean isAvailable(AbstractHorse h)
    {
        return h.isAlive() && h.getPassengers()
            .isEmpty() && !h.isBaby() && !hasReservation(h) && EntitySelector.NO_SPECTATORS.test(h);
    }


    /**
     * Finds the nearest horse to the worker that is available for riding.
     * If a horse is found that is reserved by the worker, it is prioritized over other available horses.
     * @return the nearest available horse, or null if none are found
     */
    protected AbstractHorse findNearestHorse()
    {
        final Level level = worker.level();
        if (level.isClientSide) return null;

        final AABB box = worker.getBoundingBox().inflate(HORSE_SEARCH_RADIUS, 20.0, HORSE_SEARCH_RADIUS);
        final UUID me = worker.getUUID();

        // Pull a pool, then sort by reservation priority and distance
        List<AbstractHorse> pool = level.getEntitiesOfClass(AbstractHorse.class, box, EntitySelector.NO_SPECTATORS);
        if (pool.isEmpty())
        {
            Log.getLogger().info("No horses in search AABB.");
            return null;
        }

        // 1) Prefer a horse reserved by me (if any and still riderless/adult/tamed)
        AbstractHorse mine = pool.stream()
            .filter(h -> reservedBy(h) != null && reservedBy(h).equals(me))
            .filter(h -> h.getPassengers().isEmpty() && !h.isBaby() && h.isAlive())
            .min(Comparator.comparingDouble(worker::distanceToSqr))
            .orElse(null);
        if (mine != null) return mine;

        // 2) Otherwise pick the nearest truly available horse (unreserved, riderless, adult, tamed)
        AbstractHorse available =
            pool.stream().filter(EntityAICavalry::isAvailable).min(Comparator.comparingDouble(worker::distanceToSqr)).orElse(null);

        if (available == null)
        {
            // TODO: Make this a blocking interaction.
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
