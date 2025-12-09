package com.minecolonies.api.colony.managers.interfaces;

import org.jetbrains.annotations.NotNull;

import com.minecolonies.api.colony.IAnimalData;
import com.minecolonies.api.util.constant.CitizenConstants;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;

public interface IManagedAnimal<T extends Entity>
{   
    /**
     * @return the backing entity
     */
    T getEntity();

    /**
     * Get the accessor for the colony ID.
     */
    public @NotNull EntityDataAccessor<Integer> getColonyIdAccessor();

    /**
     * Get the accessor for the citizen ID.
     */
    public @NotNull EntityDataAccessor<Integer> getAnimalIdAccessor();

    /**
     * Get the unique ID of this managed animal.
     */
    int getId();

    /**
     * Set the unique ID of this managed animal.
     */
    void setId(final int id);

    /**
     * Get the animal data associated with this managed animal.
     */
    IAnimalData getAnimalData();

    /**
     * Set the animal data associated with this managed animal.
     */
    void setAnimalData(final IAnimalData data);

    /**
     * Get the offset ticks for this managed animal.
     */
    default int getOffsetTicks()
    {
        return this.getEntity().tickCount + CitizenConstants.OFFSET_TICK_MULTIPLIER * this.getId();
    }
}