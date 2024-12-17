package com.minecolonies.core.colony.buildings.workerbuildings;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.constant.NbtTagConstants;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.datalistener.model.Disease;
import com.minecolonies.core.datalistener.DiseasesListener;
import com.minecolonies.core.entity.ai.workers.util.Patient;
import com.minecolonies.core.entity.ai.workers.util.Patient.PatientType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

import static com.minecolonies.api.util.constant.NbtTagConstants.*;
import static com.minecolonies.api.util.constant.Suppression.OVERRIDE_EQUALS;

/**
 * Class of the hospital building.
 */
@SuppressWarnings(OVERRIDE_EQUALS)
public class BuildingHospital extends AbstractBuilding
{
    /**
     * The hospital string.
     */
    private static final String HOSPITAL_DESC = "hospital";

    /**
     * Max building level of the hospital.
     */
    private static final int MAX_BUILDING_LEVEL = 5;

    /**
     * Map from beds to patients, 0 is empty.
     */
    @NotNull
    private final BiMap<BlockPos, Integer> bedMap = HashBiMap.create();

    /**
     * Map of patients of this hospital.
     */
    @NotNull
    private final Map<Integer, Patient> patients = new TreeMap<>();

    /**
     * Instantiates a new hospital building.
     *
     * @param c the colony.
     * @param l the location
     */
    public BuildingHospital(final IColony c, final BlockPos l)
    {
        super(c, l);
    }

    @NotNull
    @Override
    public String getSchematicName()
    {
        return HOSPITAL_DESC;
    }

    @Override
    public int getMaxBuildingLevel()
    {
        return MAX_BUILDING_LEVEL;
    }

    @Override
    public void deserializeNBT(final CompoundTag compound)
    {
        super.deserializeNBT(compound);
        final Map<Integer, Patient> patients = new TreeMap<>();
        final ListTag patientTagList = compound.getList(TAG_PATIENTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < patientTagList.size(); ++i)
        {
            final CompoundTag patientCompound = patientTagList.getCompound(i);
            if (compound.contains(TAG_PATIENTS))
            {
                if (patientCompound.contains(TAG_PATIENT_TYPE))
                {
                    final int patientId = compound.getInt(TAG_ID);
                    final PatientType type = PatientType.valueOf(compound.getString(TAG_PATIENT_TYPE));
                    patients.put(patientId, type);
                }
                else
                {
                    // TODO: 1.22 Remove NBT migration
                    final int patientId = patientCompound.getInt(TAG_ID);
                    patients.put(patientId, PatientType.SICK);
                }
            }
        }
        this.patients.clear();
        this.patients.putAll(patients);

        final Map<BlockPos, Integer> beds = new TreeMap<>();
        final ListTag bedTagList = compound.getList(TAG_BEDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < bedTagList.size(); ++i)
        {
            final CompoundTag bedCompound = bedTagList.getCompound(i);
            final BlockPos bedPos = BlockPosUtil.read(bedCompound, TAG_POS);
            final int citizenId = bedCompound.getInt(TAG_ID);
            if (patients.containsKey(citizenId))
            {
                beds.put(bedPos, citizenId);
            }
        }
        bedMap.clear();
        bedMap.putAll(beds);
    }

    @Override
    public CompoundTag serializeNBT()
    {
        final CompoundTag compound = super.serializeNBT();
        @NotNull final ListTag bedTagList = new ListTag();
        for (@NotNull final Map.Entry<BlockPos, Integer> entry : bedMap.entrySet())
        {
            final CompoundTag bedCompound = new CompoundTag();
            BlockPosUtil.write(bedCompound, NbtTagConstants.TAG_POS, entry.getKey());
            bedCompound.putInt(TAG_ID, entry.getValue());
            bedTagList.add(bedCompound);
        }
        compound.put(TAG_BEDS, bedTagList);

        @NotNull final ListTag patientTagList = new ListTag();
        for (final Entry<Integer, PatientType> patient : patients.entrySet())
        {
            final CompoundTag patientCompound = new CompoundTag();
            patientCompound.putInt(TAG_ID, patient.getKey());
            patientCompound.putString(TAG_PATIENT_TYPE, patient.getValue().name());
            patientTagList.add(patientCompound);
        }
        compound.put(TAG_PATIENTS, patientTagList);
        return compound;
    }

    @Override
    public void registerBlockPosition(@NotNull final BlockState blockState, @NotNull final BlockPos pos, @NotNull final Level world)
    {
        super.registerBlockPosition(blockState, pos, world);

        if (blockState.getBlock() instanceof BedBlock)
        {
            if (blockState.getValue(BedBlock.PART) == BedPart.HEAD)
            {
                bedMap.put(pos, null);
            }
        }
    }

    /**
     * Get the list of beds.
     *
     * @return immutable copy
     */
    @NotNull
    public List<BlockPos> getBedList()
    {
        return ImmutableList.copyOf(bedMap.keySet());
    }

    /**
     * Get the list of patients.
     *
     * @return immutable copy.
     */
    public List<Patient> getPatients()
    {
        return ImmutableList.copyOf(patients.values());
    }

    public @Nullable Patient getPatient(final int citizenId)
    {
        return patients.get(citizenId);
    }

    /**
     * Register a patient to the hospital.
     *
     * @param citizenId the id of the citizen.
     * @param type      the patient type.
     */
    public void addPatient(final int citizenId, final PatientType type)
    {
        patients.put(citizenId, type);
        assignBed(citizenId);
    }

    /**
     * Remove a patient from the list.
     *
     * @param citizenId the id of the citizen.
     */
    public void finishPatient(final int citizenId)
    {
        patients.remove(citizenId);
        final BlockPos bedPos = bedMap.inverse().get(citizenId);
        if (bedPos != null)
        {
            setBedOccupation(bedPos, false);

            final ICitizenData citizen = colony.getCitizenManager().getCivilian(citizenId);
            if (citizen != null && citizen.getEntity().isPresent())
            {
                citizen.getEntity().get().getCitizenSleepHandler().onWakeUp();
            }
        }
    }

    @Override
    public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount()
    {
        final Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> map = super.getRequiredItemsAndAmount();
        map.put(BuildingHospital::isCureItem, new Tuple<>(10, false));
        return map;
    }

    /**
     * Check if the given itemstack is a cure item.
     *
     * @param stack the stack to test.
     * @return true if so.
     */
    private static boolean isCureItem(final ItemStack stack)
    {
        for (final Disease disease : DiseasesListener.getDiseases())
        {
            for (final ItemStorage cureItem : disease.cureItems())
            {
                return Disease.isCureItem(stack, cureItem);
            }
        }
        return false;
    }

    @Override
    public void onColonyTick(final IColony colony)
    {
        super.onColonyTick(colony);
        assignBeds();
    }

    /**
     * Attempt to assign a bed to a single patient.
     *
     * @param citizenId the id of the citizen.
     */
    private void assignBed(final int citizenId)
    {
        BlockPos bedToOccupy = null;
        for (final Entry<BlockPos, Integer> bedEntry : bedMap.entrySet())
        {
            if (bedEntry.getValue() == null)
            {
                bedToOccupy = bedEntry.getKey();
                break;
            }
        }

        if (bedToOccupy != null)
        {
            bedMap.forcePut(bedToOccupy, citizenId);
            setBedOccupation(bedToOccupy, false);

            final ICitizenData citizen = colony.getCitizenManager().getCivilian(citizenId);
            if (citizen != null && citizen.getEntity().isPresent())
            {
                citizen.getEntity().get().getCitizenSleepHandler().trySleep(bedToOccupy);
            }
        }
    }

    private void assignBeds()
    {
        for (final int patient : patients.keySet())
        {
            if (!bedMap.containsValue(patient))
            {
                assignBed(patient);
            }
        }
    }

    /**
     * Helper method to set bed occupation.
     *
     * @param bedPos   the position of the bed.
     * @param occupied if occupied.
     */
    private void setBedOccupation(final BlockPos bedPos, final boolean occupied)
    {
        final BlockState state = colony.getWorld().getBlockState(bedPos);
        if (state.is(BlockTags.BEDS))
        {
            colony.getWorld().setBlock(bedPos, state.setValue(BedBlock.OCCUPIED, occupied), 0x03);

            final BlockPos feetPos = bedPos.relative(state.getValue(BedBlock.FACING).getOpposite());
            final BlockState feetState = colony.getWorld().getBlockState(feetPos);

            if (feetState.is(BlockTags.BEDS))
            {
                colony.getWorld().setBlock(feetPos, feetState.setValue(BedBlock.OCCUPIED, occupied), 0x03);
            }
        }
    }

    @Override
    public void onWakeUp()
    {
        for (final Map.Entry<BlockPos, Integer> entry : new ArrayList<>(bedMap.entrySet()))
        {
            final BlockState state = colony.getWorld().getBlockState(entry.getKey());
            if (state.getBlock() instanceof BedBlock)
            {
                if (entry.getValue() == 0 && state.getValue(BedBlock.OCCUPIED))
                {
                    setBedOccupation(entry.getKey(), false);
                }
                else if (entry.getValue() != 0)
                {
                    final ICitizenData citizen = colony.getCitizenManager().getCivilian(entry.getValue());
                    if (citizen != null)
                    {
                        if (state.getValue(BedBlock.OCCUPIED))
                        {
                            if (!citizen.isAsleep() || citizen.getEntity().isEmpty() || citizen.getEntity().get().blockPosition().distSqr(entry.getKey()) > 2.0)
                            {
                                setBedOccupation(entry.getKey(), false);
                                bedMap.put(entry.getKey(), null);
                            }
                        }
                        else
                        {
                            if (citizen.isAsleep() && citizen.getEntity().isPresent() && citizen.getEntity().get().blockPosition().distSqr(entry.getKey()) < 2.0)
                            {
                                setBedOccupation(entry.getKey(), true);
                            }
                        }
                    }
                    else
                    {
                        bedMap.put(entry.getKey(), null);
                    }
                }
            }
            else
            {
                bedMap.remove(entry.getKey());
            }
        }
    }

    @Override
    public boolean canEat(final ItemStack stack)
    {
        if (isCureItem(stack))
        {
            return false;
        }

        return super.canEat(stack);
    }
}
