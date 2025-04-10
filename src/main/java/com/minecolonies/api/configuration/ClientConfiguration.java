package com.minecolonies.api.configuration;

import com.minecolonies.api.configuration.builders.ConfigSpecBuilder;
import com.minecolonies.api.configuration.builders.IConfigBuilder;
import com.minecolonies.api.configuration.builders.ValueHolder;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Mod client configuration. Loaded clientside, not synced.
 */
public class ClientConfiguration
{
    public ValueHolder<Boolean> citizenVoices;
    public ValueHolder<Boolean> neighborbuildingrendering;
    public ValueHolder<Integer> neighborbuildingrange;
    public ValueHolder<Integer> buildgogglerange;
    public ValueHolder<Boolean> colonyteamborders;
    public ValueHolder<Boolean> holidayFeatures;

    /**
     * Builds client configuration.
     *
     * @param builder config builder
     */
    public ClientConfiguration(final IConfigBuilder builder)
    {
        builder.createCategory("gameplay", gameplay -> {
            citizenVoices = gameplay.defineBoolean("enablecitizenvoices", true);
            neighborbuildingrendering = gameplay.defineBoolean("neighborbuildingrendering", true);
            neighborbuildingrange = gameplay.defineInteger("neighborbuildingrange", 4, -2, 16);
            buildgogglerange = gameplay.defineInteger("buildgogglerange", 50, 1, 250);
            colonyteamborders = gameplay.defineBoolean("colonyteamborders", true);
            holidayFeatures = gameplay.defineBoolean("holidayfeatures", true);
        });
    }

    /**
     * Generate the configuration for a Forge configuration builder.
     *
     * @param builder the Forge configuration spec builder.
     * @return the finalized configuration instance.
     */
    public static ClientConfiguration forConfigBuilder(final ForgeConfigSpec.Builder builder)
    {
        return new ClientConfiguration(new ConfigSpecBuilder(builder));
    }
}
