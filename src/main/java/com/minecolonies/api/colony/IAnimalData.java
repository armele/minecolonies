package com.minecolonies.api.colony;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.minecolonies.api.colony.managers.interfaces.IManagedAnimal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Data interface for animals managed by the Animal Manager.
 */
public interface IAnimalData extends INBTSerializable<CompoundTag>
{
    /**
     * Get the animal data ID.
     *
     * @return the animal data ID
    */
    public int getId();

    /**
     * Initializes the entities values from animal data.
     */
    public void initEntityValues();

    /**
     * Get the animal entity.
     *
     * @return the animal entity.
     */
    public Optional<IManagedAnimal <? extends Entity>> getEntity();

    /**
     * Set the animal entity.
     *
     * @param entity the animal entity.
     */
    public void setEntity(final IManagedAnimal<? extends Entity> entity);

    /**
     * Clear the dirty flag for this animal data.
     */
    public void clearDirty();

    /**
     * Check if this animal data is dirty and needs syncing.
     *
     * @return true if dirty, false otherwise
     */
    public boolean isDirty();

    /**
     * Writes the animal data to a byte buf for transition.
     *
     * @param buf Buffer to write to.
     */
    void serializeViewNetworkData(@NotNull FriendlyByteBuf buf);
}
