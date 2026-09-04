package com.minecolonies.api.entity.citizen.happiness;

import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.constant.NbtTagConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Happiness forge registry to facilitate loading and saving to nbt.
 */
public class HappinessRegistry
{
    /** Registry key for complete happiness factor definitions. */
    public static final ResourceKey<Registry<HappinessFactorEntry>> HAPPINESS_FACTORS =
      ResourceKey.createRegistryKey(new ResourceLocation(Constants.MOD_ID, "happiness_factors"));

    /**
     * Get the happiness factor definition registry.
     *
     * @return the happiness factor registry.
     */
    public static Registry<HappinessFactorEntry> getHappinessFactorRegistry()
    {
        return IMinecoloniesAPI.getInstance().getHappinessFactorRegistry();
    }

    /**
     * Find the definition which owns a modifier instance ID.
     *
     * @param modifierId modifier instance ID.
     * @return its definition, or {@code null} when none is registered.
     */
    @Nullable
    public static HappinessFactorEntry getFactorByModifierId(final String modifierId)
    {
        final Registry<HappinessFactorEntry> registry = getHappinessFactorRegistry();
        if (registry == null)
        {
            return null;
        }

        for (final HappinessFactorEntry entry : registry)
        {
            if (entry.getModifierId().equals(modifierId))
            {
                return entry;
            }
        }
        return null;
    }

    /**
     * Complete definition of a logical happiness factor.
     */
    public static class HappinessFactorEntry
    {
        private final String modifierId;
        private final boolean defaultFactor;
        private final Supplier<IHappinessModifier> modifierFactory;
        private final Function<ICitizenData, Double> calculation;
        private final Component displayName;
        private final Component description;

        /**
         * Creates a happiness factor definition.
         *
         * @param modifierId modifier instance ID used in citizen data.
         * @param defaultFactor whether the factor is installed on every citizen.
         * @param modifierFactory factory producing a new modifier for one citizen.
         * @param calculation authoritative server-side factor calculation.
         * @param displayName factor name shown in the UI.
         * @param description factor description shown in the UI.
         */
        public HappinessFactorEntry(
          final String modifierId,
          final boolean defaultFactor,
          final Supplier<IHappinessModifier> modifierFactory,
          final Function<ICitizenData, Double> calculation,
          final Component displayName,
          final Component description)
        {
            this.modifierId = modifierId;
            this.defaultFactor = defaultFactor;
            this.modifierFactory = modifierFactory;
            this.calculation = calculation;
            this.displayName = displayName;
            this.description = description;
        }

        /** @return the modifier instance ID. */
        public String getModifierId()
        {
            return modifierId;
        }

        /** @return whether this definition is installed on every citizen. */
        public boolean isDefaultFactor()
        {
            return defaultFactor;
        }

        /** @return a new, independent modifier instance. */
        public IHappinessModifier createModifier()
        {
            return modifierFactory.get();
        }

        /**
         * Calculate this factor for a citizen.
         *
         * @param citizen citizen calculation context, which also exposes its colony.
         * @return current base factor.
         */
        public double calculate(final ICitizenData citizen)
        {
            return calculation.apply(citizen);
        }

        /** @return display name component. */
        public Component getDisplayName()
        {
            return displayName;
        }

        /** @return description component. */
        public Component getDescription()
        {
            return description;
        }
    }

    /**
     * Get the reward registry.
     *
     * @return the reward registry.
     */
    static Registry<HappinessFactorTypeEntry> getHappinessTypeRegistry()
    {
        return IMinecoloniesAPI.getInstance().getHappinessTypeRegistry();
    }

    /**
     * Get the reward registry.
     *
     * @return the reward registry.
     */
    static Registry<HappinessFunctionEntry> getHappinessFunctionRegistry()
    {
        return IMinecoloniesAPI.getInstance().getHappinessFunctionRegistry();
    }

    /**
     * Happiness Factor type.
     */
    public static class HappinessFactorTypeEntry
    {
        private final Supplier<IHappinessModifier> supplier;

        public HappinessFactorTypeEntry(final Supplier<IHappinessModifier> productionFunction)
        {
            this.supplier = productionFunction;
        }

        /**
         * Get the modifier.
         *
         * @return the modifier.
         */
        public IHappinessModifier create()
        {
            return supplier.get();
        }
    }

    /**
     * Static getter to load a happiness modifier from a compound.
     *
     * @param compound the compound to load it from.
     * @param persist  whether we're reading from persisted data or from networking.
     * @return the modifier instance.
     */
    @Nullable
    public static IHappinessModifier loadFrom(@NotNull final HolderLookup.Provider provider, @NotNull final CompoundTag compound, final boolean persist)
    {
        final ResourceLocation modifierType = ResourceLocation.tryParse(compound.getString(NbtTagConstants.TAG_MODIFIER_TYPE));
        if (modifierType == null || !getHappinessTypeRegistry().containsKey(modifierType))
        {
            Log.getLogger().warn("Unknown Happiness Modifier type '{}', its state cannot be restored.", modifierType);
            return null;
        }

        try
        {
            final HappinessFactorTypeEntry type = getHappinessTypeRegistry().get(modifierType);
            final IHappinessModifier modifier = type == null ? null : type.create();
            if (modifier == null)
            {
                Log.getLogger().warn("Happiness Modifier type '{}' has no usable factory, its state cannot be restored.", modifierType);
                return null;
            }

            modifier.read(provider, compound, persist);
            return modifier;
        }
        catch (final RuntimeException ex)
        {
            Log.getLogger().error("A Happiness Modifier of type '{}' threw during loading; its state cannot be restored.", modifierType, ex);
            return null;
        }
    }

    /**
     * Happiness Factor type.
     */
    public static class HappinessFunctionEntry
    {
        private final Function<ICitizenData, Double> doubleSupplier;

        /**
         * Create a new entry type.
         *
         * @param doubleSupplier th
         */
        public HappinessFunctionEntry(final Function<ICitizenData, Double> doubleSupplier)
        {
            this.doubleSupplier = doubleSupplier;
        }

        /**
         * Get the double supplier.
         *
         * @return the function.
         */
        public Function<ICitizenData, Double> getDoubleSupplier()
        {
            return doubleSupplier;
        }
    }

    public static ResourceLocation STATIC_MODIFIER      = new ResourceLocation(Constants.MOD_ID, "static");
    public static ResourceLocation EXPIRATION_MODIFIER  = new ResourceLocation(Constants.MOD_ID, "expiration");
    public static ResourceLocation TIME_PERIOD_MODIFIER = new ResourceLocation(Constants.MOD_ID, "time");

    public static ResourceLocation SCHOOL_FUNCTION        = new ResourceLocation(Constants.MOD_ID, "school");
    public static ResourceLocation SECURITY_FUNCTION      = new ResourceLocation(Constants.MOD_ID, "security");
    public static ResourceLocation SOCIAL_FUNCTION        = new ResourceLocation(Constants.MOD_ID, "social");
    public static ResourceLocation MYSTICAL_SITE_FUNCTION = new ResourceLocation(Constants.MOD_ID, "mystical");

    public static ResourceLocation HOUSING_FUNCTION      = new ResourceLocation(Constants.MOD_ID, "housing");
    public static ResourceLocation UNEMPLOYMENT_FUNCTION = new ResourceLocation(Constants.MOD_ID, "unemployment");
    public static ResourceLocation HEALTH_FUNCTION       = new ResourceLocation(Constants.MOD_ID, "health");
    public static ResourceLocation IDLEATJOB_FUNCTION    = new ResourceLocation(Constants.MOD_ID, "idleatjob");
    public static ResourceLocation SLEPTTONIGHT_FUNCTION = new ResourceLocation(Constants.MOD_ID, "slepttonight");
    public static ResourceLocation FOOD_FUNCTION         = new ResourceLocation(Constants.MOD_ID, "food");

    public static DeferredHolder<HappinessFactorTypeEntry, HappinessFactorTypeEntry> staticHappinessModifier;
    public static DeferredHolder<HappinessFactorTypeEntry, HappinessFactorTypeEntry> expirationBasedHappinessModifier;
    public static DeferredHolder<HappinessFactorTypeEntry, HappinessFactorTypeEntry> timeBasedHappinessModifier;

    public static DeferredHolder<HappinessFunctionEntry, HappinessFunctionEntry> schoolFunction;
    public static DeferredHolder<HappinessFunctionEntry, HappinessFunctionEntry> securityFunction;
    public static DeferredHolder<HappinessFunctionEntry, HappinessFunctionEntry> socialFunction;
    public static DeferredHolder<HappinessFunctionEntry, HappinessFunctionEntry> mysticalSiteFunction;

    public static DeferredHolder<HappinessFunctionEntry, HappinessFunctionEntry> housingFunction;
    public static DeferredHolder<HappinessFunctionEntry, HappinessFunctionEntry> unemploymentFunction;
    public static DeferredHolder<HappinessFunctionEntry, HappinessFunctionEntry> healthFunction;
    public static DeferredHolder<HappinessFunctionEntry, HappinessFunctionEntry> idleatjobFunction;
    public static DeferredHolder<HappinessFunctionEntry, HappinessFunctionEntry> sleptTonightFunction;
    public static DeferredHolder<HappinessFunctionEntry, HappinessFunctionEntry> foodFunction;
    public static DeferredHolder<HappinessFunctionEntry, HappinessFunctionEntry> greatFoodFunction;
}
