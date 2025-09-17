package com.minecolonies.core.entity.ai.workers.production.herders;

import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStable;
import com.minecolonies.core.colony.jobs.JobStablemaster;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Animal;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * The AI behind the {@link JobStablemaster} for Breeding Horses.
 */
public class EntityAIWorkStablemaster extends AbstractEntityAIHerder<JobStablemaster, BuildingStable>
{
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
}
