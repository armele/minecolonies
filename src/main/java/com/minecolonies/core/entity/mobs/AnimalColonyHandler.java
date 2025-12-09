package com.minecolonies.core.entity.mobs;

import org.jetbrains.annotations.Nullable;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.managers.interfaces.IManagedAnimal;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.Log;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Handles all colony orchestration for this managed animal.
 */
public class AnimalColonyHandler implements IAnimalColonyHandler
{
    /**
     * Whether the entity is registered to the colony yet.
     */
    protected boolean registered = false;

    /**
     * It's colony id.
     */
    protected int colonyId = 0;

    /**
     * The colony reference.
     */
    @Nullable
    protected IColony colony;

    private boolean needsClientUpdate = false;

    /**
     * The animal assigned to this manager.
     */
    protected final IManagedAnimal <? extends Entity> animal;

    public AnimalColonyHandler(final IManagedAnimal <? extends Entity> managedAnimal)
    {
        this.animal = managedAnimal;
    }

    /**
     * Registers the managed animal with the colony.
     * If the animal ID is 0 or the colony ID is 0, the animal is removed from the world.
     * If the colony is not found, the animal is removed from the world and a warn message is logged.
     * @param colonyID the id of the colony.
     * @param animalID the id of the animal.
     */
    @Override
    public void registerWithColony(final int colonyID, final int animalID)
    {
        if (registered)
        {
            return;
        }

        this.colonyId = colonyID;
        animal.setId(animalID);

        if (colonyId == 0 || animal.getId() == 0)
        {
            animal.getEntity().remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        final IColony colony = IColonyManager.getInstance().getColonyByWorld(colonyId, animal.getEntity().level());

        if (colony == null)
        {
            Log.getLogger().warn(String.format("IManagedAnimal '%s' unable to find Colony #%d", animal.getEntity().getUUID(), colonyId));
            animal.getEntity().remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        this.colony = colony;
        colony.getAnimalManager().registerAnimal(animal);
        registered = true;
    }

    /**
     * Gets the colony reference of the managed animal.
     * May return null if the managed animal is not registered to the colony yet.
     * @return the colony reference.
     */
    @Override
    public @Nullable IColony getColony()
    {
        return colony;
    }

    /**
     * Getter for the colony id.
     *
     * @return the colony id.
     */
    @Override
    public int getColonyId()
    {
        return colonyId;
    }

    /**
     * Setter for the colony id.
     *
     * @param colonyId the new colonyId.
     */
    @Override
    public void setColonyId(final int colonyId)
    {
        this.colonyId = colonyId;
    }

    /**
     * Update the client side of the citizen entity.
     */
    @Override
    public void updateColonyClient()
    {
        if (needsClientUpdate)
        {
            if (colonyId == 0)
            {
                colonyId = animal.getEntity().getEntityData().get(animal.getColonyIdAccessor());
            }

            if (colonyId == 0)
            {
                animal.getEntity().discard();
                return;
            }
            colony = IColonyManager.getInstance().getColonyView(colonyId, animal.getEntity().level().dimension());

            if (animal.getId() == 0)
            {
                animal.setId(animal.getEntity().getEntityData().get(animal.getAnimalIdAccessor()));
            }

            needsClientUpdate = false;
        }
    }

}
