package com.minecolonies.core.entity.ai.workers.crafting;

import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingGlassblower;
import com.minecolonies.core.colony.jobs.JobGlassblower;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_CRAFTED_DETAIL;
import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_SMELTED_DETAIL;

/**
 * Crafts glass relates things, crafts and smelts.
 */
public class EntityAIWorkGlassblower extends AbstractEntityAIRequestSmelter<JobGlassblower, BuildingGlassblower>
{
    /**
     * Initialize the glass blower AI.
     *
     * @param glassBlower the job he has.
     */
    public EntityAIWorkGlassblower(@NotNull final JobGlassblower glassBlower)
    {
        super(glassBlower);
    }

    @Override
    public Class<BuildingGlassblower> getExpectedBuildingClass()
    {
        return BuildingGlassblower.class;
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
        
        StatsUtil.trackStatByName(building, ITEMS_SMELTED_DETAIL, cookedStack.getHoverName(), cookedStack.getCount());
    }
}
