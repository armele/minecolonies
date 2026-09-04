package com.minecolonies.api.entity.citizen.happiness;

import com.minecolonies.api.colony.ICitizenData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_ID;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_VALUE;

/**
 * Happiness supplier which resolves calculation logic from a registered factor definition.
 */
public class RegisteredHappinessSupplier implements IHappinessSupplierWrapper
{
    private ResourceLocation factorId;
    private double lastValue;

    /**
     * Creates a supplier for a registered factor.
     *
     * @param factorId factor registry ID.
     */
    public RegisteredHappinessSupplier(final ResourceLocation factorId)
    {
        this.factorId = factorId;
    }

    /** Creates an empty supplier for deserialization. */
    public RegisteredHappinessSupplier()
    {
    }

    @Override
    public CompoundTag serializeNBT(@NotNull final HolderLookup.Provider provider)
    {
        final CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ID, factorId.toString());
        tag.putDouble(TAG_VALUE, lastValue);
        return tag;
    }

    @Override
    public void deserializeNBT(@NotNull final HolderLookup.Provider provider, final CompoundTag tag)
    {
        factorId = ResourceLocation.parse(tag.getString(TAG_ID));
        lastValue = tag.getDouble(TAG_VALUE);
    }

    @Override
    public double getValue(final ICitizenData citizenData)
    {
        final HappinessRegistry.HappinessFactorEntry definition = HappinessRegistry.getHappinessFactorRegistry().get(factorId);
        if (definition != null)
        {
            lastValue = definition.calculate(citizenData);
        }
        return lastValue;
    }

    @Override
    public double getLastCachedValue()
    {
        return lastValue;
    }
}
