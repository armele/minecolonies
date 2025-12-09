package com.minecolonies.core.entity.ai.workers.production.herders;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.items.ModTags;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.api.util.constant.ColonyConstants;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.constant.TranslationConstants;
import com.minecolonies.core.colony.buildings.modules.AnimalHerdingModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStable;
import com.minecolonies.core.colony.jobs.JobStablemaster;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.DECIDE;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.HERDER_GATHER_MOUNTS;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.HERDER_TRAIN;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.PREPARING;
import static com.minecolonies.api.util.constant.StatisticsConstants.HORSES_TRAINED;
import static com.minecolonies.api.util.constant.StatisticsConstants.MOUNTS_READIED;
import static com.minecolonies.api.util.constant.StatisticsConstants.ROUNDUPS_COMPLETED;
import static com.minecolonies.api.util.constant.StatisticsConstants.ITEM_USED;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.HERDER_READY_FOR_COMBAT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class EntityAIWorkStablemaster extends AbstractEntityAIHerder<JobStablemaster, BuildingStable>
{
    public enum MountMaintenance
    {
        FEEDING,
        READYING
    }

    public static final double TRAINING_CHANCE = .25;
    public static final double READY_MOUNT_FOR_COMBAT_CHANCE = .40;
    public static final double ROUND_UP_CHANCE = .15;

    public static final float BASE_COMBAT_READINESS_RECOVERY = 4.0f;
    
    public AbstractHorse horseToTrain = null;
    public CavalryHorseEntity horseToGetReady = null;
    public AbstractHorse horseToRetrieve = null;

    protected int RECOVERY_SKILL_PAR = 20;

    protected List<AbstractHorse> wanderingHorses = Collections.emptyList();

    /**
     * Get horse icon
     */
    private final static VisibleCitizenStatus FIND_HORSE =
      new VisibleCitizenStatus(new ResourceLocation(Constants.MOD_ID, "textures/icons/work/stablemaster.png"), "com.minecolonies.gui.visiblestatus.stablemaster");

    /**
     * Creates the abstract part of the AI. Always use this constructor!
     *
     * @param job the job to fulfill
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public EntityAIWorkStablemaster(@NotNull final JobStablemaster job)
    {
        super(job);
        super.registerTargets(
          new AITarget(HERDER_TRAIN, this::trainMount, 10),
          new AITarget(HERDER_READY_FOR_COMBAT, this::readyMountForCombat, 10),
          new AITarget(HERDER_GATHER_MOUNTS, this::gatherMounts, 10)
        );
    }

    @Override
    public Class<BuildingStable> getExpectedBuildingClass()
    {
        return BuildingStable.class;
    }

    /**
     * Ticks the ai state.
     * <p>
     * If the current state is not gathering mounts, and there is a horse to retrieve that is leashed to the worker, then the horse is unleased.
     * This prevents the horses from being dragged to bed when the citizen sleeps...
     */
    @Override
    public void tick()
    {
        super.tick();

        if (getState() != HERDER_GATHER_MOUNTS)
        {
            if (horseToRetrieve != null) {
                Entity holder = horseToRetrieve.getLeashHolder();
                if (holder != null && holder.equals(worker)) {
                    horseToRetrieve.dropLeash(true, false);
                }
            }
        }
    }

    /**
     * Get the extra tools needed for this job.
     *
     * @return a list of tools or empty.
     */
    @NotNull
    public List<EquipmentTypeEntry> getExtraToolsNeeded()
    {
        final List<EquipmentTypeEntry> toolsNeeded = new ArrayList<>();
        toolsNeeded.add(ModEquipmentTypes.lead.get());
        return toolsNeeded;
    }
    
    /**
     * Ensures that we have a appropriate tool available. Will set {@code needsTool} accordingly.
     * The Lead is level-less and so we need to override this here with such that a level is not checked.
     *
     * @param toolType type of tool we check for.
     * @return false if we have the tool
     */
    public boolean checkForToolOrWeapon(@NotNull final EquipmentTypeEntry toolType)
    {
        final boolean needTool = checkForToolOrWeapon(toolType, -1);
        if (needTool)
        {
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
        }
        else
        {
            worker.getCitizenData().setJobStatus(JobStatus.WORKING);
        }
        return needTool;
    }

    @Override
    protected IAIState breedAnimals()
    {
        worker.getCitizenData().setVisibleStatus(FIND_HORSE);
        return super.breedAnimals();
    }

    /**
     * Returns the chance to butcher an animal in the list of all animals in the building.
     * <p>
     * The chance is calculated based on the number of animals in the building and the max allowed.
     * <p>
     *
     * @param allAnimals the list of all animals in the building.
     * @return the chance to butcher an animal in the list of all animals in the building.
     */
    @Override
    public double chanceToButcher(final List<? extends Animal> allAnimals)
    {
        // No butchering the cavalry steeds!
        return 0;
    }

    /**
     * Decides what the AI should do next.
     * @return the next state to go to.
     */
    @Override
    public IAIState decideWhatToDo()
    {
        final IAIState result = super.decideWhatToDo();

        if (ColonyConstants.rand.nextDouble() < TRAINING_CHANCE)
        {
            return HERDER_TRAIN;
        }

        if (ColonyConstants.rand.nextDouble() < READY_MOUNT_FOR_COMBAT_CHANCE)
        {
            return HERDER_READY_FOR_COMBAT;
        }

        if (ColonyConstants.rand.nextDouble() < ROUND_UP_CHANCE)
        {
            wanderingHorses = findNearbyUnstabledHorses();

            if (!wanderingHorses.isEmpty())
            {
                return HERDER_GATHER_MOUNTS;
            }
        }

        return result;
    }

    /**
     * Trains a mount to become a cavalry horse. 
     * Capacity: at most (2 × building level) CavalryHorseEntity in the herded animals.
     */
    protected IAIState trainMount()
    {
        final int limit = Math.max(0, building.getBuildingLevel() * 2);

        // If we're mid-training, walk to the horse...
        if (horseToTrain != null && !walkToSafePos(horseToTrain.getOnPos()))
        {
            Log.getLogger().info("Moving to horse.");
            return HERDER_TRAIN;
        }

        // ... and then train it!
        if (horseToTrain != null)
        {
            final CavalryHorseEntity cav = CavalryHorseEntity.createFromVanilla(building.getColony(), worker.level, horseToTrain);
            if (cav == null)
            {
                Log.getLogger().warn("Could not convert candidate to CavalryHorseEntity");
            }
            else
            {
                cav.getAnimalData().setHomeBuilding(building);
                worker.getCitizenExperienceHandler().addExperience(XP_PER_ACTION);
                StatsUtil.trackStat(building, HORSES_TRAINED, 1);
                incrementActionsDoneAndDecSaturation();
                Log.getLogger().info("Trained cavalry horse.");
            }
            horseToTrain = null;
            return DECIDE;
        }

        int current = 0;
        AbstractHorse firstCandidate = null;

        // Single-pass scan: count existing CavalryHorseEntity and pick first convertible AbstractHorse
        for (final AnimalHerdingModule module : building.getModulesByType(AnimalHerdingModule.class))
        {
            final List<? extends Animal> animals = searchForAnimals(module::isCompatible);
            if (animals.isEmpty()) continue;

            for (final Animal a : animals)
            {
                if (a instanceof CavalryHorseEntity)
                {
                    current++;
                    if (current >= limit)
                    {
                        Log.getLogger().info("Stable at capacity: {}/{} cavalry mounts.", current, limit);
                        return DECIDE;
                    }
                    continue;
                }

                Log.getLogger().info("Trained Horses: {}", current);

                // Record first good vanilla horse candidate
                if (firstCandidate == null && a instanceof AbstractHorse h)
                {
                    if (h.isAlive() && !h.isBaby() && h.getPassengers().isEmpty())
                    {
                        firstCandidate = h;
                    }
                }
            }
        }

        if (firstCandidate != null)
        {
            if (current >= limit)
            {
                Log.getLogger().info("Skipping training at capacity of {}.", limit);
                return DECIDE;
            }

            horseToTrain = firstCandidate; 
            return HERDER_TRAIN;
        }

        Log.getLogger().info("No suitable horses found to train.");
        return DECIDE;
    }

    /**
     * Walks to the horse if already selected, and then gets it ready for combat.
     * If no horse is selected, it will single-pass scan for the first horse that needs to be readied for combat.
     * @return the next state to go to.
     */
    protected IAIState readyMountForCombat()
    {
        // Walk to the horse if already selected.
        if (horseToGetReady != null && !walkToSafePos(horseToGetReady.getOnPos()))
        {
            Log.getLogger().info("Moving to horse for combat readiness.");
            return HERDER_READY_FOR_COMBAT;
        }

        // ... and then get it ready for combat!
        if (horseToGetReady != null)
        {
            boolean didWork = false;

            if (horseToGetReady.getHealth() < horseToGetReady.getMaxHealth())
            {
                didWork = readyMount(MountMaintenance.FEEDING, horseToGetReady);
            }

            if (!horseToGetReady.isReadyForCombat())
            {
                didWork = didWork || readyMount(MountMaintenance.READYING, horseToGetReady);
            }
            
            if (didWork)
            {
                worker.getCitizenExperienceHandler().addExperience(XP_PER_ACTION);
                horseToGetReady = null;
            }

            return DECIDE;

        }

        for (final AnimalHerdingModule module : building.getModulesByType(AnimalHerdingModule.class))
        {
            final List<? extends Animal> animals = searchForAnimals(module::isCompatible);
            if (animals.isEmpty()) continue;

            for (final Animal a : animals)
            {
                if (a instanceof CavalryHorseEntity cav)
                {
                    if (!cav.isReadyForCombat() || cav.getHealth() < cav.getMaxHealth())
                    {
                        horseToGetReady = cav;
                        return HERDER_READY_FOR_COMBAT;
                    }
                }
            }
        }

        Log.getLogger().info("No mounts need to be readied.");
        return DECIDE;
    }

    /**
     * Prepares the AI to feed the horse. If the worker has feed items in their inventory, it will take one of them and feed the horse.
     * If the worker doesn't have feed items, it will request the building for one. If the building has one, it will be taken and the horse will be fed.
     * If the building doesn't have any feed items, the AI will set a flag to indicate that it needs to request a feed item from the inventory.
     */
    public boolean readyMount(MountMaintenance task, CavalryHorseEntity horse)
    {
        Log.getLogger().info("Checking mount readiness.");

        TagKey<Item> neededItem = task == MountMaintenance.FEEDING ? ModTags.feed : ModTags.leather;
        Component component = task == MountMaintenance.FEEDING ? Component.translatable(TranslationConstants.STABLEMASTER_NEEDED_FEEDITEMS) : Component.translatable(TranslationConstants.STABLEMASTER_NEEDED_READYITEMS);

        boolean hasNeededItem = false;
        boolean didWork = false;

        if (InventoryUtils.getItemCountInProvider(worker, i -> i.is(neededItem)) <= 0)
        {
            if (InventoryUtils.hasBuildingEnoughElseCount(building, i -> i.is(neededItem), 1) > 0)
            {
                walkToBuilding();

                int buildingSlot = InventoryUtils.findFirstSlotInProviderNotEmptyWith(building, itemStack -> itemStack.is(neededItem));
                if (buildingSlot >= 0)
                {
                    Log.getLogger().info("Taking needed item {} from building.", neededItem.toString());
                    this.takeItemStackFromProvider(building, buildingSlot);
                }
                else
                {
                    Log.getLogger().info("Building allegedly had enough, but we couldn't find the slot of needed item {}.", neededItem.toString());
                }
            }
            else
            {
                Log.getLogger().info("Building doesn't have {}.", neededItem.toString());
            }
        }
        else
        {
            hasNeededItem  = true;
        }

        int slotOfStack = InventoryUtils.findFirstSlotInItemHandlerNotEmptyWith(worker.getItemHandlerCitizen(), itemStack -> itemStack.is(neededItem));

        if (hasNeededItem || slotOfStack >= 0)
        {
            ItemStack stackToUse = worker.getItemHandlerCitizen().extractItem(slotOfStack, 1, false);
            worker.setItemInHand(InteractionHand.MAIN_HAND, stackToUse);

            if (stackToUse.isEmpty())
            {
                Log.getLogger().info("No mount readiness item available despite slot being identified.");
                return false;
            } 

            if (task == MountMaintenance.FEEDING)
            {
                Log.getLogger().info("Readying mount with food.");

                feedHorse(horse);
                StatsUtil.trackStatByStack(building, ITEM_USED, stackToUse, 1);
                stackToUse.shrink(1);
                effectsAtHorse(horse);
            }

            if (task == MountMaintenance.READYING)
            {   
                float combatCooldownBefore = horse.getCombatCooldown();

                // TODO: Reasearch to influence readiness recovery rate?
                if (stackToUse.getItem() == Items.SADDLE)
                {
                    horse.setCombatCooldown(0);
                }
                else
                {
                    float recovery = BASE_COMBAT_READINESS_RECOVERY * ((float) getPrimarySkillLevel() / (float) RECOVERY_SKILL_PAR);
                    horse.prepareForCombat(recovery);
                }

                Log.getLogger().info("Readied mount with {} from {} to {}.", stackToUse.getHoverName(), combatCooldownBefore, horse.getCombatCooldown());
                StatsUtil.trackStatByStack(building, ITEM_USED, stackToUse, 1);

                stackToUse.shrink(1);
                effectsAtHorse(horse);

                if (horse.isReadyForCombat())
                {
                    StatsUtil.trackStat(building, MOUNTS_READIED, 1);
                }
            }
            
            didWork = true;
            incrementActionsDone();
        }
        else
        {
            final ImmutableList<IRequest<? extends Stack>> openRequestList = building.getOpenRequestsOfType(worker.getCitizenData().getId(), TypeToken.of(Stack.class));

            StackList requestableItems = new StackList(neededItem, (ServerLevel) worker.getCitizenData().getColony().getWorld(), component.getString(), 8, 4, 0);
            boolean placeNewOrder = true;

            // If the openRequestList includes anything that is also in the StackList, do not create another request.
            for (IRequest<? extends Stack> request : openRequestList)
            {
                for (ItemStack newRequest : requestableItems.getStacks()) 
                {
                    if (request.getRequest().getStack().getItem() == newRequest.getItem())
                    {
                        placeNewOrder = false;
                        break;
                    }
                }
            }

            if (placeNewOrder)
            {
                worker.getCitizenData().createRequestAsync(requestableItems);

                Log.getLogger().info("Needed mount readiness item not available. Requesting delivery.");
            }

        }

        return didWork;
    }

    /** 
     * Feed a horse
     * */
    public static boolean feedHorse(AbstractHorse horse)
    {
        if (horse == null || horse.level().isClientSide()) return false;

        final float heal = 2.0F;
        final int temper = 3;

        boolean didSomething = false;

        if (horse.getHealth() < horse.getMaxHealth())
        {
            horse.heal(heal);
            didSomething = true;
        }

        if (temper != 0)
        {
            horse.setTemper(horse.getTemper() + temper);
        }

        if (didSomething)
        {
            horse.gameEvent(GameEvent.EAT);
            horse.level().playSound(null, horse, SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.8f, 1.0f);
        }
        return didSomething;
    }

    /**
     * Gather a wandering horse and bring it back to the stable with a leash.
     * @return the next state to go to.
     */
    public IAIState gatherMounts()
    {
        
        if (horseToRetrieve != null)
        {
            if (!horseToRetrieve.isAlive() 
                || horseToRetrieve.isRemoved() 
                || (horseToRetrieve instanceof CavalryHorseEntity cav && cav.hasCavalryRider()) 
                || (horseToRetrieve instanceof CavalryHorseEntity cav && cav.hasReservation()))
            {
                horseToRetrieve = null;
                return DECIDE;
            }

            for (final EquipmentTypeEntry tool : getExtraToolsNeeded())
            {
                // Verify that we still have the lead needed to gather this horse.
                if (checkForToolOrWeapon(tool))
                {
                    horseToRetrieve = null;
                    return PREPARING;
                }
            }

            if (horseToRetrieve.isLeashed())
            {
                if (!walkToBuilding()) 
                {
                    horseToRetrieve.clearRestriction();
                    horseToRetrieve.restrictTo(worker.blockPosition(), 3);
                    return HERDER_GATHER_MOUNTS;
                }
                
                detachHorse(horseToRetrieve);

                worker.getCitizenExperienceHandler().addExperience(XP_PER_ACTION);
                incrementActionsDoneAndDecSaturation();
                Log.getLogger().info("Fetched wandering horse.");

                if (wanderingHorses.contains(horseToRetrieve))
                {
                    wanderingHorses.remove(horseToRetrieve);
                }

                horseToRetrieve = null;

                if (!wanderingHorses.isEmpty())
                {
                    return HERDER_GATHER_MOUNTS;
                }
                
                StatsUtil.trackStat(building, ROUNDUPS_COMPLETED, 1);

                return DECIDE;
            }

            if (!walkToSafePos(horseToRetrieve.blockPosition()))
            {
                return HERDER_GATHER_MOUNTS;
            }

            if (attachHorse(horseToRetrieve))
            {
                return HERDER_GATHER_MOUNTS;
            }
            else
            {
                return DECIDE;
            }
        }

        if (wanderingHorses.isEmpty())
        {
            return DECIDE;
        }

        horseToRetrieve = wanderingHorses.get(0);

        return HERDER_GATHER_MOUNTS;
    }


    /**
     * Plays particles and sound effects at the given horse.
     * @param horse the horse to play effects at
     */
    protected void effectsAtHorse(CavalryHorseEntity horse) 
    {
        ServerLevel level = (ServerLevel) horse.level();
        level.sendParticles(ParticleTypes.WAX_ON, horse.getX(), horse.getY() + horse.getBbHeight() * 0.7, horse.getZ(), 12, 0.3, 0.4, 0.3, 0.02);

        level.playSound(
            null,
            BlockPos.containing(horse.getX(), horse.getY(), horse.getZ()),
            SoundEvents.HORSE_EAT,
            SoundSource.PLAYERS,
            0.8f,
            1.0f
        ); 
    }

    /**
     * Attempts to attach the given horse to the worker.
     * @param horse the horse to attach
     * @return true if the horse was successfully attached, false otherwise
     */
    public boolean attachHorse(AbstractHorse horse)
    {
        if (worker == null || horse == null) return false;

        if (!horse.isAlive() || horse.isRemoved()) return false;

        // If already leashed to this citizen, nothing to do.
        if (horse.isLeashed() && worker.equals(horse.getLeashHolder())) return true;

        // If leashed to somebody else, drop that leash first
        if (horse.isLeashed())
        {
            horse.dropLeash(true, true);
        }

        if (worker.getOffhandItem().isEmpty())
        {
            worker.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.LEAD));
        }

        horse.setLeashedTo(worker, true);
        horse.restrictTo(worker.blockPosition(), 3);

        return true;
    }


    /**
     * Detaches the given horse from the worker, removing the leash and dropping
     * the lead item if held by the worker. If the worker is holding a lead
     * item, it will be replaced with an empty hand.
     * 
     * @param horse the horse to detach
     */
    public void detachHorse(AbstractHorse horse)
    {
        if (worker == null || horse == null || horse.level().isClientSide)
        {
            Log.getLogger().info("Bailing early on detaching horse.");
            return;
        }

        if (!horse.isLeashed())
        {
            Log.getLogger().info("The horse to detach is not attached...");
            return;
        } 

        Log.getLogger().info("Dropping leash...");
        horse.dropLeash(true, false);
        horse.clearRestriction();

        if (worker.getOffhandItem().is(Items.LEAD))
        {
            worker.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

   /**
     * Finds all horses within 20 blocks of the given position, but
     * outside the stable.
     *
     * @param level The world/level to search in
     * @param center The center position
     * @return List of CavalryHorseEntity found within radius
     */
   public List<AbstractHorse> findNearbyUnstabledHorses()
   {
       // TODO: Research to increase the range of the stablemaster's round-up
       final BlockPos center = building.getPosition();
       final double radius = 20.0D;
       final double r2 = radius * radius;

       // Build a cube search box once; we'll do a spherical check in the predicate.
       final double cx = center.getX() + 0.5D;
       final double cy = center.getY() + 0.5D;
       final double cz = center.getZ() + 0.5D;
       final AABB searchBox = new AABB(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius);

       return worker.level()
           .getEntitiesOfClass(AbstractHorse.class,
               searchBox,
               horse -> !horse.isRemoved() &&
                   horse.isAlive() &&
                   !(horse instanceof CavalryHorseEntity cav && cav.hasCavalryRider()) &&
                   !(horse instanceof CavalryHorseEntity cav && cav.isInStable()) &&
                   !(horseToRetrieve instanceof CavalryHorseEntity cav && cav.hasReservation()) &&
                   horse.distanceToSqr(cx, cy, cz) <= r2);
   }

}
