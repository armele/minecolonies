package com.minecolonies.apiimp.initializer;

import com.minecolonies.api.research.ModResearchCostTypes;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.research.costs.ListItemCost;
import com.minecolonies.core.research.costs.SimpleItemCost;
import com.minecolonies.core.research.costs.TagItemCost;
import net.minecraftforge.registries.DeferredRegister;

import static com.minecolonies.api.research.ModResearchCostTypes.*;
import static com.minecolonies.apiimp.CommonMinecoloniesAPIImpl.REGISTRY_KEY_RESEARCH_COST_TYPES;

/**
 * Registry initializer for the {@link ModResearchCostTypes}.
 */
public class ModResearchCostTypeInitializer
{
    public static final DeferredRegister<ResearchCostType> DEFERRED_REGISTER = DeferredRegister.create(REGISTRY_KEY_RESEARCH_COST_TYPES, Constants.MOD_ID);
    static
    {
        ModResearchCostTypes.simpleItemCost =
          DEFERRED_REGISTER.register(SIMPLE_ITEM_COST_ID.getPath(), () -> new ResearchCostType(SIMPLE_ITEM_COST_ID, SimpleItemCost::new));

        ModResearchCostTypes.listItemCost =
          DEFERRED_REGISTER.register(LIST_ITEM_COST_ID.getPath(), () -> new ResearchCostType(LIST_ITEM_COST_ID, ListItemCost::new));

        ModResearchCostTypes.tagItemCost =
          DEFERRED_REGISTER.register(TAG_ITEM_COST_ID.getPath(), () -> new ResearchCostType(TAG_ITEM_COST_ID, TagItemCost::new));
    }
    private ModResearchCostTypeInitializer()
    {
        throw new IllegalStateException("Tried to initialize: ModResearchCostInitializer but this is a Utility class.");
    }
}
