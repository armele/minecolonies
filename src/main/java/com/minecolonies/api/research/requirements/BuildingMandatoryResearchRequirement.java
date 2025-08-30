package com.minecolonies.api.research.requirements;

import com.google.gson.JsonObject;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.research.IBuildingResearchRequirement;
import com.minecolonies.api.research.ModResearchRequirements;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.util.GsonHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Certain building research requirements.
 */
public class BuildingMandatoryResearchRequirement implements IBuildingResearchRequirement
{
    /**
     * The NBT tag for an individual building's name.
     */
    private static final String TAG_BUILDING_NAME = "building-name";

    /**
     * The NBT tag for an individual building's required level.
     */
    private static final String TAG_BUILDING_LVL = "building-lvl";

    /**
     * The property name for the building.
     */
    private static final String RESEARCH_REQUIREMENT_BUILDING_PROP = "mandatory-building";

    /**
     * The property name for a numeric level.
     */
    private static final String RESEARCH_REQUIREMENT_BUILDING_LEVEL_PROP = "level";

    /**
     * The building level.
     */
    private final int buildingLevel;

    /**
     * The building desc.
     */
    private final String building;

    /**
     * Create a building research requirement.
     *
     * @param nbt the nbt containing the relevant data.
     */
    public BuildingMandatoryResearchRequirement(final CompoundTag nbt)
    {
        this.building = nbt.getString(TAG_BUILDING_NAME);
        this.buildingLevel = nbt.getInt(TAG_BUILDING_LVL);
    }

    /**
     * Create a building research requirement.
     *
     * @param json the json containing the relevant data.
     */
    public BuildingMandatoryResearchRequirement(final JsonObject json)
    {
        this.building = GsonHelper.getAsString(json, RESEARCH_REQUIREMENT_BUILDING_PROP);
        this.buildingLevel = GsonHelper.getAsInt(json, RESEARCH_REQUIREMENT_BUILDING_LEVEL_PROP);
    }

    /**
     * @return the building registry resource location
     */
    public ResourceLocation getBuilding()
    {
        ResourceLocation buldingResourceLocation = ResourceLocation.tryParse(building);
        
        // Try to maintain backwards compatibility with non-namespaced research entries.
        if (buldingResourceLocation != null)
        {
            return buldingResourceLocation;
        }

        return new ResourceLocation(Constants.MOD_ID, this.building);
    }

    /**
     * @return the building level
     */
    public int getBuildingLevel()
    {
        return buildingLevel;
    }

    @Override
    public ModResearchRequirements.ResearchRequirementEntry getRegistryEntry()
    {
        return ModResearchRequirements.buildingMandatoryResearchRequirement.get();
    }

    @Override
    public MutableComponent getDesc()
    {
        return Component.translatable("com." + this.getBuilding().getNamespace() + ".coremod.research.requirement.building.mandatory.level",
            Component.translatable("com." + this.getBuilding().getNamespace() + ".building." + this.getBuilding().getPath()),
            this.buildingLevel);
    }

    @Override
    public boolean isFulfilled(final IColony colony)
    {
        return colony.hasBuilding(this.building, this.buildingLevel, true);
    }

    @Override
    public CompoundTag writeToNBT()
    {
        final CompoundTag nbt = new CompoundTag();
        nbt.putString(TAG_BUILDING_NAME, building);
        nbt.putInt(TAG_BUILDING_LVL, buildingLevel);
        return nbt;
    }
}
