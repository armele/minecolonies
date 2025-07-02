package com.minecolonies.core.entity.ai.workers.crafting;

import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFletcher;
import com.minecolonies.core.colony.jobs.JobFletcher;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_CRAFTED_DETAIL;

/**
 * Crafts wood related block when needed.
 */
public class EntityAIWorkFletcher extends AbstractEntityAICrafting<JobFletcher, BuildingFletcher>
{
    /**
     * Initialize the fletcher and add all his tasks.
     *
     * @param fletcher the job he has.
     */
    public EntityAIWorkFletcher(@NotNull final JobFletcher fletcher)
    {
        super(fletcher);
    }

    @Override
    public Class<BuildingFletcher> getExpectedBuildingClass()
    {
        return BuildingFletcher.class;
    }

    /**
     * Records the crafting request in the building's statistics.
     * @param request the request to record.
     */
    @Override
    public void recordCraftingBuildingStats(IRequest<?> request, IRecipeStorage recipe)
    {
        if (recipe == null) 
        {
            return;
        }

        StatsUtil.trackStatByName(building, ITEMS_CRAFTED_DETAIL, recipe.getPrimaryOutput().getHoverName(), recipe.getPrimaryOutput().getCount());
    }
}
