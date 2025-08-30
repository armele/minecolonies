package com.minecolonies.api.research;


import net.minecraft.resources.ResourceLocation;

/**
 * Interface of research requirements that require a certain building.
 */
public interface IBuildingResearchRequirement extends IResearchRequirement
{
    /**
     * The building required for this research requirement.
     * @return resource location under which the building is registered.
     */
    ResourceLocation getBuilding();

    /**
     * The level of the building needed for this research requirement.
     * @return
     */
    int getBuildingLevel(); 
}
