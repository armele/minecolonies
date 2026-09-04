package com.minecolonies.apiimp.initializer;

import com.minecolonies.api.entity.citizen.happiness.ExpirationBasedHappinessModifier;
import com.minecolonies.api.entity.citizen.happiness.HappinessRegistry;
import com.minecolonies.api.entity.citizen.happiness.HappinessRegistry.HappinessFactorTypeEntry;
import com.minecolonies.api.entity.citizen.happiness.HappinessRegistry.HappinessFunctionEntry;
import com.minecolonies.api.entity.citizen.happiness.HappinessRegistry.HappinessFactorEntry;
import com.minecolonies.api.entity.citizen.happiness.RegisteredHappinessSupplier;
import com.minecolonies.api.entity.citizen.happiness.StaticHappinessModifier;
import com.minecolonies.api.entity.citizen.happiness.TimeBasedHappinessModifier;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.colony.jobs.JobPupil;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenHappinessHandler;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static com.minecolonies.api.entity.citizen.happiness.HappinessRegistry.*;
import static com.minecolonies.api.util.constant.HappinessConstants.*;
import static com.minecolonies.core.entity.citizen.citizenhandlers.CitizenHappinessHandler.*;

/**
 * Happiness factory initializer of the values.
 */
public final class ModHappinessFactorTypeInitializer
{
    public final static DeferredRegister<HappinessFactorTypeEntry>
                                                                 DEFERRED_REGISTER_HAPPINESS_FACTOR   = DeferredRegister.create(CommonMinecoloniesAPIImpl.HAPPINESS_FACTOR_TYPES, Constants.MOD_ID);
    public final static DeferredRegister<HappinessFunctionEntry> DEFERRED_REGISTER_HAPPINESS_FUNCTION = DeferredRegister.create(CommonMinecoloniesAPIImpl.HAPPINESS_FUNCTION, Constants.MOD_ID);
    public final static DeferredRegister<HappinessFactorEntry> DEFERRED_REGISTER_HAPPINESS_FACTORS = DeferredRegister.create(HappinessRegistry.HAPPINESS_FACTORS, Constants.MOD_ID);

    private ModHappinessFactorTypeInitializer()
    {
        throw new IllegalStateException("Tried to initialize: ModHappinessFactorTypeInitializer but this is a Utility class.");
    }

    static
    {
        HappinessRegistry.staticHappinessModifier = DEFERRED_REGISTER_HAPPINESS_FACTOR.register(STATIC_MODIFIER.getPath(), () -> new HappinessFactorTypeEntry(StaticHappinessModifier::new));

        HappinessRegistry.expirationBasedHappinessModifier = DEFERRED_REGISTER_HAPPINESS_FACTOR.register(EXPIRATION_MODIFIER.getPath(), () -> new HappinessFactorTypeEntry(ExpirationBasedHappinessModifier::new));

        HappinessRegistry.timeBasedHappinessModifier = DEFERRED_REGISTER_HAPPINESS_FACTOR.register(TIME_PERIOD_MODIFIER.getPath(), () -> new HappinessFactorTypeEntry(TimeBasedHappinessModifier::new));


        HappinessRegistry.schoolFunction = DEFERRED_REGISTER_HAPPINESS_FUNCTION.register(SCHOOL_FUNCTION.getPath(), () -> new HappinessFunctionEntry(data -> data.isChild() ? data.getJob() instanceof JobPupil ? 2.0 : 0.0 : 1.0));
        HappinessRegistry.securityFunction = DEFERRED_REGISTER_HAPPINESS_FUNCTION.register(SECURITY_FUNCTION.getPath(), () -> new HappinessFunctionEntry(data -> getGuardFactor(data.getColony())));
        HappinessRegistry.socialFunction = DEFERRED_REGISTER_HAPPINESS_FUNCTION.register(SOCIAL_FUNCTION.getPath(), () -> new HappinessFunctionEntry(data -> getSocialModifier(data.getColony())));
        HappinessRegistry.mysticalSiteFunction = DEFERRED_REGISTER_HAPPINESS_FUNCTION.register(MYSTICAL_SITE_FUNCTION.getPath(), () -> new HappinessFunctionEntry(data -> getMysticalSiteFactor(data.getColony())));

        HappinessRegistry.housingFunction = DEFERRED_REGISTER_HAPPINESS_FUNCTION.register(HOUSING_FUNCTION.getPath(), () -> new HappinessFunctionEntry(data -> data.getHomeBuilding() == null ? 0.0 : data.getHomeBuilding().getBuildingLevel() / 3.0));
        HappinessRegistry.unemploymentFunction = DEFERRED_REGISTER_HAPPINESS_FUNCTION.register(UNEMPLOYMENT_FUNCTION.getPath(), () -> new HappinessFunctionEntry(data -> data.isChild() ? 1.0 : (data.getWorkBuilding() == null ? 0.5 : data.getWorkBuilding().getBuildingLevel() > 3 ? 2.0 : 1.0)));
        HappinessRegistry.healthFunction = DEFERRED_REGISTER_HAPPINESS_FUNCTION.register(HEALTH_FUNCTION.getPath(),
            () -> new HappinessFunctionEntry(data -> data.getEntity().isPresent() ? (data.getCitizenDiseaseHandler().isSick() ? 0.5 : 1.0) : 1.0));
        HappinessRegistry.idleatjobFunction = DEFERRED_REGISTER_HAPPINESS_FUNCTION.register(IDLEATJOB_FUNCTION.getPath(), () -> new HappinessFunctionEntry(data -> data.isIdleAtJob() ? 0.5 : 1.0));

        HappinessRegistry.sleptTonightFunction = DEFERRED_REGISTER_HAPPINESS_FUNCTION.register(SLEPTTONIGHT_FUNCTION.getPath(), () -> new HappinessFunctionEntry(data -> data.getJob() instanceof AbstractJobGuard ? 1 : 0.5));
        HappinessRegistry.foodFunction = DEFERRED_REGISTER_HAPPINESS_FUNCTION.register(FOOD_FUNCTION.getPath(), () -> new HappinessFunctionEntry(CitizenHappinessHandler::getFoodFactor));

        registerDefault(SCHOOL, () -> new StaticHappinessModifier(SCHOOL, 1.0, registeredSupplier(SCHOOL)),
          data -> data.isChild() ? data.getJob() instanceof JobPupil ? 2.0 : 0.0 : 1.0);
        registerDefault(SECURITY, () -> new StaticHappinessModifier(SECURITY, 4.0, registeredSupplier(SECURITY)),
          data -> getGuardFactor(data.getColony()));
        registerDefault(SOCIAL, () -> new StaticHappinessModifier(SOCIAL, 2.0, registeredSupplier(SOCIAL)),
          data -> getSocialModifier(data.getColony()));
        registerDefault(MYSTICAL_SITE, () -> new StaticHappinessModifier(MYSTICAL_SITE, 1.0, registeredSupplier(MYSTICAL_SITE)),
          data -> getMysticalSiteFactor(data.getColony()));
        registerDefault(FOOD, () -> new StaticHappinessModifier(FOOD, 3.0, registeredSupplier(FOOD)), CitizenHappinessHandler::getFoodFactor);

        registerDefault(HOMELESSNESS, () -> new TimeBasedHappinessModifier(HOMELESSNESS, 3.0, registeredSupplier(HOMELESSNESS),
          new Tuple<>(COMPLAIN_DAYS_WITHOUT_HOUSE, 0.75), new Tuple<>(DEMANDS_DAYS_WITHOUT_HOUSE, 0.5)),
          data -> data.getHomeBuilding() == null ? 0.0 : data.getHomeBuilding().getBuildingLevel() / 3.0);
        registerDefault(UNEMPLOYMENT, () -> new TimeBasedHappinessModifier(UNEMPLOYMENT, 2.0, registeredSupplier(UNEMPLOYMENT),
          new Tuple<>(COMPLAIN_DAYS_WITHOUT_JOB, 0.75), new Tuple<>(DEMANDS_DAYS_WITHOUT_JOB, 0.5)),
          data -> data.isChild() ? 1.0 : data.getWorkBuilding() == null ? 0.5 : data.getWorkBuilding().getBuildingLevel() > 3 ? 2.0 : 1.0);
        registerDefault(HEALTH, () -> new TimeBasedHappinessModifier(HEALTH, 2.0, registeredSupplier(HEALTH),
          new Tuple<>(COMPLAIN_DAYS_SICK, 0.5), new Tuple<>(DEMANDS_CURE_SICK, 0.1)),
          data -> data.getEntity().isPresent() ? data.getCitizenDiseaseHandler().isSick() ? 0.5 : 1.0 : 1.0);
        registerDefault(IDLEATJOB, () -> new TimeBasedHappinessModifier(IDLEATJOB, 1.0, registeredSupplier(IDLEATJOB),
          new Tuple<>(IDLE_AT_JOB_COMPLAINS_DAYS, 0.5), new Tuple<>(IDLE_AT_JOB_DEMANDS_DAYS, 0.1)),
          data -> data.isIdleAtJob() ? 0.5 : 1.0);
        registerDefault(SLEPTTONIGHT, () -> new TimeBasedHappinessModifier(SLEPTTONIGHT, 1.5, registeredSupplier(SLEPTTONIGHT),
          (modifier, data) -> true, new Tuple<>(0, 2d), new Tuple<>(2, 1.6d), new Tuple<>(3, 1d)),
          data -> data.getJob() instanceof AbstractJobGuard ? 1.0 : 0.5);
    }

    /**
     * Register a native factor installed on every citizen.
     *
     * @param id modifier and registry path.
     * @param factory per-citizen modifier factory.
     * @param calculation server-side base calculation.
     */
    private static void registerDefault(
      final String id,
      final java.util.function.Supplier<com.minecolonies.api.entity.citizen.happiness.IHappinessModifier> factory,
      final java.util.function.Function<com.minecolonies.api.colony.ICitizenData, Double> calculation)
    {
        DEFERRED_REGISTER_HAPPINESS_FACTORS.register(id, () -> new HappinessFactorEntry(id, true, factory, calculation,
          Component.translatable("com.minecolonies.coremod.gui.townhall.happiness." + id),
          Component.translatable("com.minecolonies.coremod.gui.townhall.happiness.desc." + id)));
    }

    /**
     * Create a supplier backed by the native factor definition registry.
     *
     * @param id native factor path.
     * @return registry-backed supplier.
     */
    private static RegisteredHappinessSupplier registeredSupplier(final String id)
    {
        return new RegisteredHappinessSupplier(new ResourceLocation(Constants.MOD_ID, id));
    }
}
