package com.minecolonies.api.research.costs;

import com.google.gson.JsonObject;
import com.minecolonies.api.research.IResearchCost;
import com.minecolonies.api.research.ModResearchCosts;
import com.minecolonies.core.util.GsonHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;

import static com.minecolonies.api.research.util.ResearchConstants.*;

/**
 * A plain item cost that takes a list of several items that have to be fulfilled.
 */
public class TagItemCost implements IResearchCost
{
    /**
     * The property name for tag.
     */
    private static final String JSON_PROP_TAG = "tag";

    /**
     * The property name for item quantity.
     */
    private static final String JSON_PROP_QUANTITY = "quantity";

    /**
     * The tag which contains all possible items.
     */
    private final TagKey<Item> tag;

    /**
     * The count of items.
     */
    private final int count;

    /**
     * Create a simple item cost.
     *
     * @param compound the nbt containing the relevant data.
     */
    public TagItemCost(final CompoundTag compound)
    {
        this.tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(compound.getString(TAG_COST_ITEM)));
        this.count = compound.getInt(TAG_COST_COUNT);
    }

    /**
     * Create a simple item cost.
     *
     * @param json the nbt containing the relevant data.
     */
    public TagItemCost(final JsonObject json)
    {
        this.tag = TagKey.create(Registries.ITEM, GsonHelper.getAsResourceLocation(json, JSON_PROP_TAG));
        this.count = Math.max(GsonHelper.getAsInt(json, JSON_PROP_QUANTITY, 1), 1);
    }

    @Override
    public ModResearchCosts.ResearchCostEntry getType()
    {
        return ModResearchCosts.tagItemCost.get();
    }

    @Override
    public List<Item> getItems()
    {
        return BuiltInRegistries.ITEM.getTag(this.tag).stream().flatMap(HolderSet.ListBacked::stream).map(Holder::value).toList();
    }

    @Override
    public int getCount()
    {
        return this.count;
    }

    @Override
    public Component getTranslatedName()
    {
        return Component.translatable(String.format("com.minecolonies.coremod.research.tags.%s", this.tag.location()));
    }

    @Override
    public CompoundTag writeToNBT()
    {
        final CompoundTag compound = new CompoundTag();
        compound.putInt(TAG_COST_COUNT, this.count);
        compound.putString(TAG_COST_TAG, this.tag.location().toString());
        return compound;
    }
}
