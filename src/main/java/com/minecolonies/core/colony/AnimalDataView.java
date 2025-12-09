package com.minecolonies.core.colony;

import org.jetbrains.annotations.NotNull;

import com.minecolonies.api.colony.managers.interfaces.IAnimalDataView;

import net.minecraft.network.FriendlyByteBuf;

public class AnimalDataView implements IAnimalDataView
{
    /**
     * The id of the animal.
     */
    int id;

    /**
     * The colony view.
     */
    ColonyView colonyView;

    /**
     * Constructor
     * 
     * @param id
     * @param colonyView
     */
    public AnimalDataView(int id, ColonyView colonyView)
    {
        this.id = id;
        this.colonyView = colonyView;
    }

    /**
     * Deserialize the animal data view from a network buffer.
     *
     * @param buf the buffer to deserialize from
     */
    @Override
    public void deserialize(@NotNull FriendlyByteBuf buf)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deserialize'");
    }

    /**
     * Returns the id of the animal data view.
     *
     * @return the id of the animal data view
     */
    @Override
    public int getId()
    {
        return id;
    }
    
}
