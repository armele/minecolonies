package com.minecolonies.api.configuration;

import com.minecolonies.api.configuration.builders.ConfigSpecBuilder;
import com.minecolonies.api.configuration.builders.IConfigBuilder;
import com.minecolonies.api.configuration.builders.ValueHolder;
import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfiguration
{
    public ValueHolder<Boolean> generateSupplyLoot;
    public ValueHolder<Boolean> rsEnableDebugLogging;

    /**
     * Builds client configuration.
     *
     * @param builder config builder
     */
    public CommonConfiguration(final IConfigBuilder builder)
    {
        builder.createCategory("gameplay", gameplay -> generateSupplyLoot = gameplay.defineBoolean("generatesupplyloot", true));

        builder.createCategory("requestsystem", requestSystem -> rsEnableDebugLogging = requestSystem.defineBoolean("enabledebuglogging", false));
    }

    /**
     * Generate the configuration for a Forge configuration builder.
     *
     * @param builder the Forge configuration spec builder.
     * @return the finalized configuration instance.
     */
    public static CommonConfiguration forConfigBuilder(final ForgeConfigSpec.Builder builder)
    {
        return new CommonConfiguration(new ConfigSpecBuilder(builder));
    }
}
