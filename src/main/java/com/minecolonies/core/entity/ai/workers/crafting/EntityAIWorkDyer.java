package com.minecolonies.core.entity.ai.workers.crafting;

import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDyer;
import com.minecolonies.core.colony.jobs.JobDyer;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_CRAFTED_DETAIL;
import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_SMELTED_DETAIL;

/**
 * Crafts dye related things.
 */
public class EntityAIWorkDyer extends AbstractEntityAIRequestSmelter<JobDyer, BuildingDyer>
{
    /**
     * Initialize the dyer.
     *
     * @param dyer the job he has.
     */
    public EntityAIWorkDyer(@NotNull final JobDyer dyer)
    {
        super(dyer);
    }

    @Override
    public Class<BuildingDyer> getExpectedBuildingClass()
    {
        return BuildingDyer.class;
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

        StatsUtil.trackStatByName(building, ITEMS_CRAFTED_DETAIL, recipe.getPrimaryOutput().getDescriptionId(), recipe.getPrimaryOutput().getCount());
    }

    /**
     * Records the smelting request in the building's statistics.
     *
     * @param cookedStack the item stack that has been smelted.
     */
    @Override
    protected void recordSmeltingBuildingStats(ItemStack cookedStack)
    {
        if (cookedStack == null) 
        {
            return;
        }
        
        StatsUtil.trackStatByName(building, ITEMS_SMELTED_DETAIL, cookedStack.getDescriptionId(),cookedStack.getCount());
    }
}
