package com.minecolonies.core.entity.ai.workers.production.herders;

import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.items.ModTags;
import com.minecolonies.api.util.InventoryFunctions;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.ColonyConstants;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.constant.TranslationConstants;
import com.minecolonies.core.colony.buildings.modules.AnimalHerdingModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStable;
import com.minecolonies.core.colony.jobs.JobStablemaster;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.server.level.ServerLevel;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.DECIDE;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.HERDER_TRAIN;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.HERDER_READY_FOR_COMBAT;

import java.util.List;

import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;

public class EntityAIWorkStablemaster extends AbstractEntityAIHerder<JobStablemaster, BuildingStable>
{
    public enum MountMaintenance
    {
        FEEDING,
        READYING
    }

    public static final double TRAINING_CHANCE = .25;
    public static final double READY_MOUNT_FOR_COMBAT_CHANCE = .25;
    public static final float COMBAT_READINESS_RECOVERY = 2.0f;
    
    public AbstractHorse horseToTrain = null;
    public CavalryHorseEntity horseToGetReady = null;

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
    public EntityAIWorkStablemaster(@NotNull final JobStablemaster job)
    {
        super(job);
        super.registerTargets(
          new AITarget(HERDER_TRAIN, this::trainMount, 20),
          new AITarget(HERDER_READY_FOR_COMBAT, this::readyMountForCombat, 20)
        );
    }

    @Override
    public Class<BuildingStable> getExpectedBuildingClass()
    {
        return BuildingStable.class;
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
            final CavalryHorseEntity cav = CavalryHorseEntity.createFromVanilla(worker.level, horseToTrain);
            if (cav == null)
            {
                Log.getLogger().info("Could not convert candidate to CavalryHorseEntity");
            }
            else
            {
                cav.setStable(building.getPosition(), cav.level().dimension());
                worker.getCitizenExperienceHandler().addExperience(XP_PER_ACTION);
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
        TagKey<Item> neededItem = task == MountMaintenance.FEEDING ? ModTags.feed : ModTags.leather;
        Component component = task == MountMaintenance.FEEDING ? Component.translatable(TranslationConstants.STABLEMASTER_NEEDED_FEEDITEMS) : Component.translatable(TranslationConstants.STABLEMASTER_NEEDED_READYITEMS);

        boolean hasNeededItem = false;
        boolean didWork = false;

        if (InventoryUtils.getItemCountInProvider(worker, i -> i.is(neededItem)) <= 0)
        {
            if (InventoryUtils.hasBuildingEnoughElseCount(building, i -> i.is(neededItem), 1) == 0)
            {
                walkToBuilding();

                hasNeededItem = InventoryFunctions.matchFirstInProviderWithAction(
                    building,
                    stack -> stack.is(neededItem),
                    this::takeItemStackFromProvider
                    );
            }
        }
        else
        {
            hasNeededItem  = true;
        }

        int slotOfStack = InventoryUtils.findFirstSlotInItemHandlerNotEmptyWith(worker.getItemHandlerCitizen(), itemStack -> itemStack.is(neededItem));

        if (hasNeededItem || slotOfStack >= 0)
        {
            ItemStack reducedStack = worker.getItemHandlerCitizen().extractItem(slotOfStack, 1, false);
            worker.setItemInHand(InteractionHand.MAIN_HAND, reducedStack);

            if (task == MountMaintenance.FEEDING)
            {
                Log.getLogger().info("Readying mount with food.");

                feedHorse(horse);
            }

            if (task == MountMaintenance.READYING)
            {   
                Log.getLogger().info("Readying mount with leather.");

                // TODO: Reasearch to influence readiness recovery rate?
                horse.prepareForCombat(COMBAT_READINESS_RECOVERY);
            }
            
            didWork = true;
            incrementActionsDone();
        }
        else
        {
            StackList requestableItems = new StackList(neededItem, (ServerLevel) worker.getCitizenData().getColony().getWorld(), component.getString(), 8, 4, 0);

            worker.getCitizenData().createRequestAsync(requestableItems);

            Log.getLogger().info("Needed mount readiness item not available.");
        }

        return didWork;
    }

    /** 
     * Feed a horse
     * */
    public static boolean feedHorse(AbstractHorse horse)
    {
        if (horse == null || horse.level().isClientSide()) return false;

        final float heal = 2.0F;     // 1 heart
        final int temper = 3;        // vanilla wheat temper bump

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

}
