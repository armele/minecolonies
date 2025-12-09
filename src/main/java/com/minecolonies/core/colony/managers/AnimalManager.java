package com.minecolonies.core.colony.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.minecolonies.api.colony.IAnimalData;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.managers.interfaces.IAnimalManager;
import com.minecolonies.api.util.EntityUtils;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.Network;
import com.minecolonies.core.colony.AnimalData;
import com.minecolonies.core.colony.CitizenData;
import com.minecolonies.core.network.messages.client.colony.ColonyViewAnimalViewDataMessage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class AnimalManager implements IAnimalManager
{
    /**
     * NBT Tags
     */
    public static String TAG_ANIMAL_MANAGER = "animalManager";
    public static String TAG_ANIMALS        = "animals";
    public static String TAG_NEXTID         = "nextID";

    /**
     * Map with animal ID and data
     */
    private Map<Integer, IAnimalData> animalMap = new HashMap<>();

    /**
     * Whether this manager is dirty and needs re-serialize
     */
    private boolean isDirty = false;

    /**
     * The colony of the manager.
     */
    private final IColony colony;

    /**
     * The next free ID
     */
    private int nextAnimalID = -1;

    public AnimalManager(final IColony colony)
    {
        this.colony = colony;
    }


    /**
     * Get the current amount of citizens, might be bigger then {@link #getMaxCitizens()}
     *
     * @return The current amount of citizens in the colony.
     */
    @Override
    public int getCurrentAnimalCount()
    {
        return animalMap.size();
    }

    /**
     * Creates and registers a new animal data.
     * This method also ensures that IDs are getting reused.
     * That's needed to prevent bugs when calling IDs that are not used.
     *
     * @return The newly created animal data.
     */
    @Override
    public IAnimalData createAndRegisterAnimalData()
    {
        //This ensures that IDs are getting reused.
        //That's needed to prevent bugs when calling IDs that are not used.
        for (int i = 1; i <= this.getCurrentAnimalCount() + 1; i++)
        {
            if (this.getAnimal(i) == null)
            {
                nextAnimalID = i;
                break;
            }
        }

        final AnimalData animalData = new AnimalData(nextAnimalID, colony);
        animalMap.put(animalData.getId(), animalData);

        return animalData;
    }

    /**
     * Get the animal data by ID.
     *
     * @param id The animal ID.
     * @return The animal data, or null if not found.
     */
    @Override
    public IAnimalData getAnimal(final int id)
    {
        return animalMap.get(id);
    }

    /**
     * Read the animal data from nbt.
     *
     * @param compound the compound to read it from.
     */
    @Override
    public void read(@NotNull CompoundTag compound)
    {
        if (compound.contains(TAG_ANIMAL_MANAGER))
        {
            final CompoundTag animalManagerNBT = compound.getCompound(TAG_ANIMAL_MANAGER);
            final ListTag animalList = animalManagerNBT.getList(TAG_ANIMALS, Tag.TAG_COMPOUND);
            for (final Tag animal : animalList)
            {
                final IAnimalData data = AnimalData.loadAnimalFromNBT(colony, (CompoundTag) animal);
                animalMap.put(data.getId(), data);
            }

            nextAnimalID = animalManagerNBT.getInt(TAG_NEXTID);
        }
        markDirty();
    }

    /**
     * Write the animal information to nbt.
     *
     * @param compoundNBT the compound to write it to.
     * @throws UnsupportedOperationException if not implemented.
     */
    @Override
    public void write(@NotNull CompoundTag compoundNBT)
    {
        final CompoundTag animalManagerNBT = new CompoundTag();

        final ListTag animalList = new ListTag();
        for (Map.Entry<Integer, IAnimalData> entry : animalMap.entrySet())
        {
            animalList.add(entry.getValue().serializeNBT());
        }

        animalManagerNBT.put(TAG_ANIMALS, animalList);
        animalManagerNBT.putInt(TAG_NEXTID, nextAnimalID);
        compoundNBT.put(TAG_ANIMAL_MANAGER, animalManagerNBT);
    }

    @Override
    public void onColonyTick(IColony colony)
    {
        // No-Op scaffolding for future tick-based animal management.
    }

    /**
     * Marks the Animal Manager as dirty, which will cause it to update and save to disk when the colony is saved.
     */
    @Override
    public void markDirty()
    {
        this.isDirty = true;
    }

    /**
     * Clear the dirty flag of the Animal Manager, which will prevent it from re-saving to disk when the colony is saved.
     */
    @Override
    public void clearDirty()
    {
        this.isDirty = false;
    }

    /**
     * Send the necessary packets to subscribers.
     *
     * @param closeSubscribers players that were subscribed but are now out of range
     * @param newSubscribers   players that have just come into range and need data
     */
    @Override
    public void sendPackets(@NotNull final Set<ServerPlayer> closeSubscribers, @NotNull final Set<ServerPlayer> newSubscribers)
    {
        Set<IAnimalData> toSend = null;
        boolean refresh = !newSubscribers.isEmpty() || this.isDirty;

        if (refresh)
        {
            toSend = new HashSet<>(animalMap.values());
            for (final IAnimalData data : animalMap.values())
            {
                data.clearDirty();
            }
            this.clearDirty();
        }
        else
        {
            for (final IAnimalData data : animalMap.values())
            {
                if (data.isDirty())
                {
                    if (toSend == null)
                    {
                        toSend = new HashSet<>();
                    }

                    toSend.add(data);
                }
                data.clearDirty();
            }
        }

        if (toSend == null || toSend.isEmpty())
        {
            return;
        }

        Set<ServerPlayer> players = new HashSet<>(newSubscribers);
        players.addAll(closeSubscribers);

        final ColonyViewAnimalViewDataMessage message = new ColonyViewAnimalViewDataMessage(colony, toSend, refresh);

        for (final ServerPlayer player : players)
        {
            Network.getNetwork().sendToPlayer(message, player);
        }
    }
}
