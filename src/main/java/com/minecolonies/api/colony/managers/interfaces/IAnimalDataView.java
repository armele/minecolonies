package com.minecolonies.api.colony.managers.interfaces;

import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;

public interface IAnimalDataView 
{
    /**
     * Deserialize the attributes and variables from transition.
     *
     * @param buf Byte buffer to deserialize.
     */
    void deserialize(@NotNull FriendlyByteBuf buf);

    /**
     * Get the id of the animal.
     *
     * @return the animal id.
     */
    int getId();
}
