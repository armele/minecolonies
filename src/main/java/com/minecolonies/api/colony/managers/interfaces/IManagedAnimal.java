package com.minecolonies.api.colony.managers.interfaces;

import com.minecolonies.api.colony.IAnimalData;

import net.minecraft.world.entity.Entity;

public interface IManagedAnimal<T extends Entity>
{
    /**
     * @return the backing entity
     */
    T getEntity();

    /**
     * Get the animal data associated with this managed animal.f
     */
    IAnimalData getAnimalData();

    /**
     * Set the animal data associated with this managed animal.
     */
    void setAnimalData(final IAnimalData data);
}