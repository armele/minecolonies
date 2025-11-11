package com.minecolonies.core.generation.defaults.workers;

import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.core.generation.CustomRecipeProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

import static com.minecolonies.api.util.constant.BuildingConstants.MODULE_CRAFTING;

/**
 * Datagen for Baker
 */
public class DefaultDairyworkerCraftingProvider extends CustomRecipeProvider
{
    private static final String DAIRYWORKER = ModJobs.DAIRYWORKER_ID.getPath();

    public DefaultDairyworkerCraftingProvider(@NotNull final PackOutput packOutput)
    {
        super(packOutput);
    }

    @NotNull
    @Override
    public String getName()
    {
        return "DefaultDairyworkerCraftingProvider";
    }

    @Override
    protected void registerRecipes(@NotNull final Consumer<FinishedRecipe> consumer)
    {

        CustomRecipeBuilder.create(DAIRYWORKER, MODULE_CRAFTING, "large_milk_bottle")
          .inputs(List.of(new ItemStorage(new ItemStack(ModItems.large_empty_bottle))))
          .result(ModItems.large_milk_bottle.getDefaultInstance())
          .build(consumer);
    }
}
