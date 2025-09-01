package com.minecolonies.core.entity.ai.workers.guard;

import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.combat.CombatAIStates;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.workers.util.GuardGear;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.jobs.JobCavalry;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
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


    public EntityAICavalry(@NotNull final JobCavalry job)
    {
        super(job);
        super.registerTargets(
            new AITarget(CombatAIStates.FIND_MOUNT, this::findMount, GUARD_MOUNT_INTERVAL)
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
     * Finds a horse to ride. If the horse is already assigned, ride it. Otherwise, find the closest horse and assign it to the guard.
     * If no horse is found, return NO_TARGET.
     * If the guard can't reach the horse, return FIND_MOUNT to try again.
     *
     * @return the next state to go to.
     */
    protected IAIState findMount()
    {
        AbstractHorse horse = null;

        if (!validateTarget(targetMount))
        {
            horse = findNearestHorse();
            targetMount = horse;
        }

        if (targetMount == null)
        {
            // TODO: Do we want a warning here about insufficient available horses?
            Log.getLogger().info("No horses found.");
            return CombatAIStates.NO_TARGET;
        }

        reserveFor(targetMount, worker);

        if (!EntityNavigationUtils.walkToPos(worker, targetMount.blockPosition(), 2, true))
        {
            Log.getLogger().info("Walking to horse.");

            return CombatAIStates.FIND_MOUNT;
        }

        // We are within range; attempt to mount once.
        if (worker.isPassenger())
        {
            // Already riding something (maybe this horse). Mark mounted and exit.
            Log.getLogger().info("Already mounted.");
            return CombatAIStates.NO_TARGET;
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
            Log.getLogger().info("Horse got a different passenger; releasing reservation.");
            clearIfMine(targetMount, worker.getUUID());
            targetMount = null;
            return CombatAIStates.FIND_MOUNT;
        }

        boolean mounted = worker.startRiding(targetMount, true); // force=true
        Log.getLogger().info("Mount attempt result={}", mounted);

        if (mounted)
        {
            clearIfMine(targetMount, worker.getUUID());
            return CombatAIStates.NO_TARGET;
        }
        else
        {
            // If this keeps failing, you’re likely slightly out of contact—nudge closer again
            return CombatAIStates.FIND_MOUNT;
        }
    }

    private static void reserveFor(@Nonnull AbstractHorse horse, @Nonnull Entity reserver)
    {
        CompoundTag data = horse.getPersistentData();
        data.putUUID(RESERVE_KEY, reserver.getUUID());
    }

    private static boolean hasReservation(AbstractHorse horse)
    {
        return horse.getPersistentData().contains(RESERVE_KEY, Tag.TAG_INT_ARRAY);
    }

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

    /**
     * Validate whether a horse is a valid target for the guard to ride.
     * A horse is valid if it is not null, passes the horse filter, and is in the same level as the guard.
     * @param horse the horse to validate
     * @return true if the horse is valid, false otherwise
     */
    private boolean validateTarget(AbstractHorse horse) 
    {
        if (horse == null)
        {
            return false;
        }
        
        return horseFilterFor(worker).test(horse) && horse.level() == worker.level();
    }

    private static UUID reservedBy(AbstractHorse horse)
    {
        CompoundTag data = horse.getPersistentData();
        return data.contains(RESERVE_KEY, Tag.TAG_INT_ARRAY) ? data.getUUID(RESERVE_KEY) : null;
    }

    private Predicate<AbstractHorse> horseFilterFor(@Nonnull Entity reserver) 
    {
        final UUID me = reserver.getUUID();

        return horse ->
            horse.isAlive()
            && horse.getPassengers().isEmpty()
            && !horse.isBaby()
            // Allow if no reservation OR reserved specifically for me:
            && (!hasReservation(horse) || me.equals(reservedBy(horse)))
            // Keep or drop this depending on your test setup:
            && horse.isTamed()
            && EntitySelector.NO_SPECTATORS.test(horse);
    }
    /**
     * Finds the nearest horse to the worker within a certain range.
     * 
     * @return the nearest horse, or null if none found
     */
    protected AbstractHorse findNearestHorse() {
        final Level level = worker.level();
        final AABB box = worker.getBoundingBox().inflate(HORSE_SEARCH_RADIUS, 8.0, HORSE_SEARCH_RADIUS);

        // Pull a small candidate set, then pick the closest
        List<AbstractHorse> candidates = level.getEntitiesOfClass(AbstractHorse.class, box, EntitySelector.NO_SPECTATORS);

        if (candidates.isEmpty()) 
        {
            Log.getLogger().info("No candidates found.");
            return null;
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(worker::distanceToSqr))
                .orElse(null);
    }

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
