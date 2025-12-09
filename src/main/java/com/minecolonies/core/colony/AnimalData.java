package com.minecolonies.core.colony;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.minecolonies.api.colony.IAnimalData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.NbtTagConstants;
import com.minecolonies.api.colony.managers.interfaces.IManagedAnimal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;

public class AnimalData implements IAnimalData
{
    /**
     * The unique animal id.
     */
    private final int id;

    /**
     * The globally unique animal identifier.
     */
    private UUID uuid;

    /**
     * The colony the citizen belongs to.
     */
    private final IColony colony;

    /**
     * The entity associated with this citizen.
     */
    private boolean isDirty = false;

    /**
     * Its entitity.
     */
    @NotNull
    private WeakReference<IManagedAnimal <? extends Entity>> entity = new WeakReference<>(null);

    /**
     * Create an AnimalData given an ID and colony.
     *
     * @param id     ID of the Citizen.
     * @param colony Colony the Citizen belongs to.
     */
    public AnimalData(final int id, final IColony colony)
    {
        this.id = id;
        this.colony = colony;
    }

    /**
     * Initializes vital entity values from animal data.
     *
     */    
    @Override
    public void initEntityValues()
    {
        if (!getEntity().isPresent())
        {
            Log.getLogger().warn("Missing entity upon adding data to that entity!" + this, new Exception());
            return;
        }

        final IManagedAnimal <? extends Entity> animal = getEntity().get();

        // TODO: Implement initailization logic here
    }    

    /**
     * Loads this animal data from nbt
     *
     * @param colony colony to load for
     * @param nbt    nbt compound to read from
     * @return new AnimalData
     */
    public static IAnimalData loadAnimalFromNBT(final IColony colony, final CompoundTag nbt)
    {
        final IAnimalData data = new AnimalData(nbt.getInt(NbtTagConstants.TAG_ID), colony);
        data.deserializeNBT(nbt);
        return data;
    }

    /**
     * Serializes this animal data to nbt.
     *
     * @return A compound nbt containing the animal data.
     */
    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag compoundNBT = new CompoundTag();
        // TODO: Serialize any additional data here
        return compoundNBT;
    }

    /**
     * Writes the animal data to a byte buf for transition.
     * This method first calls the superclass's implementation, then writes
     * the recruit cost and the count of the recruit cost to the byte buf.
     *
     * @param buf Buffer to write to.
     */
    @Override
    public void serializeViewNetworkData(@NotNull final FriendlyByteBuf buf)
    {
        // TODO: Serialize any additional view-bound data here
        // Must match deserialization on the view side.
    }

    @Override
    public void deserializeNBT(final CompoundTag nbtTagCompound)
    {
        // TODO: Deserialize any additional data here
    }

    /**
     * Get the unique ID of this animal.
     *
     * @return the animal ID
     */
    @Override
    public int getId()
    {
        return id;
    }

    /**
     * Return the entity instance of the animal data. Respawn the animal if needed.
     *
     * @return {@link Entity} of the animal data.
     */
    @Override
    @NotNull
    public Optional<IManagedAnimal <? extends Entity>> getEntity()
    {
        final IManagedAnimal <? extends Entity> animal = entity.get();

        if (animal != null && animal.getEntity().isRemoved())
        {
            entity.clear();
            return Optional.empty();
        }

        return Optional.ofNullable(animal);
    }
    
    /**
     * Set the entity instance of the animal data.
     *
     * @param animal the animal entity.
     */
    @Override
    public void setEntity(@Nullable final IManagedAnimal <? extends Entity> animal)
    {
        if (entity.get() != null)
        {
            entity.clear();
        }

        if (animal != null)
        {
            entity = new WeakReference<IManagedAnimal <? extends Entity>>(animal);
        }
    }

    /**
     * Clears the dirty flag for this animal data.
     * <p>This method marks the animal data as not needing to be synced with the client.</p>
     */
    @Override
    public void clearDirty()
    {
        isDirty = false;
    }

    /**
     * Returns if the instance is dirty.
     *
     * @return True if dirty, otherwise false.
     */
    @Override
    public boolean isDirty()
    {
        return isDirty;
    }
}
