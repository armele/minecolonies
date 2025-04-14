package com.minecolonies.api.compatibility;

import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.ItemStorage;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for the new furnace recipes.
 */
public interface IFurnaceRecipes
{
    /**
     * Get the furnace recipes instance.
     *
     * @return the furnace recipes instance.
     */
    static IFurnaceRecipes getFurnaceRecipes()
    {
        return IMinecoloniesAPI.getInstance().getFurnaceRecipes();
    }

    /**
     * Get the smelting result for a certain itemStack.
     *
     * @param itemStack the itemStack to test.
     * @return the result or empty if not existent.
     */
    ItemStack getSmeltingResult(final ItemStack itemStack);

    /**
     * Get the first smelting recipe by result for a certain itemStorage.
     *
     * @param storage the itemStorage to test.
     * @return the result or null if not existent.
     */
    IRecipeStorage getFirstSmeltingRecipeByResult(final ItemStorage storage);
}
