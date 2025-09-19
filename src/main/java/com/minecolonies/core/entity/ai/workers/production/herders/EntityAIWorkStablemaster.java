package com.minecolonies.core.entity.ai.workers.production.herders;

import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.ColonyConstants;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildings.modules.AnimalHerdingModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStable;
import com.minecolonies.core.colony.jobs.JobStablemaster;
import com.minecolonies.core.entity.other.CavalryHorseEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.DECIDE;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.HERDER_TRAIN;

import java.util.List;

import org.jetbrains.annotations.NotNull;

public class EntityAIWorkStablemaster extends AbstractEntityAIHerder<JobStablemaster, BuildingStable>
{

    public static final double TRAINING_CHANCE = .25;
    public AbstractHorse horseToTrain = null;

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
          new AITarget(HERDER_TRAIN, this::trainMount, 20)
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

}
