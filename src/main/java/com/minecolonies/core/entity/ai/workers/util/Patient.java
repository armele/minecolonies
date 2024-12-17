package com.minecolonies.core.entity.ai.workers.util;

import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.crafting.ItemStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_ID;

/**
 * Class representing a patient.
 */
public class Patient
{
    /**
     * NBT tags.
     */
    private static final String TAG_TYPE     = "type";
    private static final String TAG_REQUESTS = "requests";

    /**
     * Citizen id of the patient.
     */
    private final int id;

    /**
     * The patient type.
     */
    private final PatientType type;

    /**
     * Items that this patient is requesting from the hospital.
     */
    private final List<ItemStorage> requests = new ArrayList<>();

    /**
     * Create a new patient file.
     *
     * @param id   the id of the patient.
     * @param type the type of patient.
     */
    public Patient(final int id, final PatientType type)
    {
        this.id = id;
        this.type = type;
    }

    /**
     * Load the Patient from nbt.
     *
     * @param patientCompound the nbt to load it from.
     */
    public Patient(final CompoundTag patientCompound)
    {
        this.id = patientCompound.getInt(TAG_ID);
        // TODO: Remove NBT migration (contains check)
        this.type = patientCompound.contains(TAG_TYPE) ? PatientType.valueOf(patientCompound.getString(TAG_TYPE)) : PatientType.SICK;

        final ListTag requestList = patientCompound.getList(TAG_REQUESTS, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < requestList.size(); i++)
        {
            this.requests.add(StandardFactoryController.getInstance().deserialize(requestList.getCompound(i)));
        }
    }

    /**
     * Get the citizen id of the patient.
     *
     * @return the int id.
     */
    public int getId()
    {
        return id;
    }

    /**
     * Get the patient type.
     *
     * @return the patient type.
     */
    public PatientType getType()
    {
        return type;
    }

    /**
     * Get the requests this patient is asking for.
     *
     * @return the list of requests.
     */
    public List<ItemStorage> getRequests()
    {
        return ImmutableList.copyOf(requests);
    }

    /**
     * Request a given item for this patient.
     *
     * @param item the item to deliver.
     */
    public void requestItem(final ItemStorage item)
    {
        this.requests.add(item);
    }

    /**
     * Finish a given request for this patient.
     *
     * @param requestIndex the index of the request to remove.
     */
    public void finishRequest(final int requestIndex)
    {
        this.requests.remove(requestIndex);
    }

    /**
     * Write the Patient to nbt.
     *
     * @param compoundNBT the compound to write it to.
     */
    public void write(final CompoundTag compoundNBT)
    {
        compoundNBT.putInt(TAG_ID, id);
        compoundNBT.putString(TAG_TYPE, type.name());
    }

    /**
     * The type of this patient.
     */
    public enum PatientType
    {
        SICK,
        HURT
    }
}
