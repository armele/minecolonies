package com.minecolonies.core.entity.mobs;

import org.jetbrains.annotations.Nullable;

import com.minecolonies.api.colony.IColony;

public interface IAnimalColonyHandler 
{
    /**
     * Server-specific update for the EntityAnimal.
     * 
     * @param colonyID  the id of the colony.
     * @param citizenID the id of the citizen.
     */
    public void registerWithColony(final int colonyID, final int citizenID);

    /**
     * Getter for the colony.
     * 
     * @return the colony.
     */
    public @Nullable IColony getColony();

    /**
     * Getter for the colony id.
     *
     * @return the colony id.
     */
    public int getColonyId();

    /**
     * Setter for the colony id.
     *
     * @param colonyId the new colonyId.
     */
    public void setColonyId(final int colonyId);

    /**
     * Update the client side of the citizen entity.
     */
    public void updateColonyClient();

}
