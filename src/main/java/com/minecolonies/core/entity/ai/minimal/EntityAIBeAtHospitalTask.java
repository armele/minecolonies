package com.minecolonies.core.entity.ai.minimal;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.ai.IStateAI;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.entity.ai.workers.util.Patient;
import com.minecolonies.core.entity.ai.workers.util.Patient.PatientType;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;

import static com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState.IDLE;
import static com.minecolonies.core.entity.ai.minimal.EntityAIBeAtHospitalTask.HospitalAIState.*;

/**
 * The AI task for citizens to go to the hospital for any condition.
 */
public abstract class EntityAIBeAtHospitalTask implements IStateAI
{
    /**
     * Min distance to hospital before trying to find a bed.
     */
    private static final int MIN_DIST_TO_HOSPITAL = 3;

    /**
     * The citizen assigned to this task.
     */
    protected final EntityCitizen citizen;

    /**
     * Citizen data.
     */
    protected final ICitizenData citizenData;

    /**
     * Hospital to which the citizen should path.
     */
    private BlockPos hospitalPos;

    /**
     * Instantiates this task.
     *
     * @param citizen the citizen.
     */
    protected EntityAIBeAtHospitalTask(final EntityCitizen citizen)
    {
        this.citizen = citizen;
        this.citizenData = citizen.getCitizenData();

        citizen.getCitizenAI().addTransition(new TickingTransition<>(SEARCH_HOSPITAL, () -> true, this::searchHospital, 20));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(GO_TO_HOSPITAL, () -> true, this::goToHospital, 20));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(ARRIVE_AT_HOSPITAL, () -> true, this::arriveAtHospital, 20));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(WAIT_IN_HOSPITAL, () -> true, this::waitInHospital, 20));
    }

    /**
     * Return the AI state to start going to the hospital.
     *
     * @return the next AI state.
     */
    protected final IState requiresHospital()
    {
        return SEARCH_HOSPITAL;
    }

    /**
     * Actions to perform when laying inside the hospital bed.
     */
    protected abstract void performActionsInHospital(final BuildingHospital hospital);

    /**
     * Get the next AI state when no hospital could be found.
     *
     * @return the next AI state.
     */
    protected abstract IState nextAIStateWhenNoHospital();

    /**
     * @return
     */
    protected abstract PatientType getPatientType();

    /**
     * Search for a placeToPath within the colony of the citizen.
     *
     * @return the next state to go to.
     */
    private IState searchHospital()
    {
        final IColony colony = citizenData.getColony();
        hospitalPos = colony.getBuildingManager().getBestBuilding(citizen, BuildingHospital.class);
        if (hospitalPos == null)
        {
            return nextAIStateWhenNoHospital();
        }

        return GO_TO_HOSPITAL;
    }

    /**
     * Go to the previously found placeToPath to get cure.
     *
     * @return the next state to go to.
     */
    private IState goToHospital()
    {
        if (hospitalPos == null)
        {
            return nextAIStateWhenNoHospital();
        }

        if (citizen.isWorkerAtSiteWithMove(hospitalPos, MIN_DIST_TO_HOSPITAL))
        {
            return ARRIVE_AT_HOSPITAL;
        }
        return GO_TO_HOSPITAL;
    }

    /**
     * Arrive at the hospital
     *
     * @return the next state to go to.
     */
    private IState arriveAtHospital()
    {
        final IColony colony = citizen.getCitizenColonyHandler().getColonyOrRegister();
        final IBuilding building = colony.getBuildingManager().getBuilding(hospitalPos);

        if (building instanceof BuildingHospital hospital)
        {
            hospital.addPatient(citizen.getId(), getPatientType());
            return WAIT_IN_HOSPITAL;
        }

        return SEARCH_HOSPITAL;
    }

    private IState waitInHospital()
    {
        final IColony colony = citizen.getCitizenColonyHandler().getColonyOrRegister();
        final IBuilding building = colony.getBuildingManager().getBuilding(hospitalPos);

        if (building instanceof BuildingHospital hospital)
        {
            final Patient patientType = hospital.getPatient(citizenData.getId());
            if (patientType != null)
            {
                reset();
                return IDLE;
            }

            performActionsInHospital(hospital);
        }

        return WAIT_IN_HOSPITAL;
    }

    /**
     * Internal reset.
     */
    protected void reset()
    {
        this.hospitalPos = null;
    }

    /**
     * The different types of AIStates related to being in the hospital.
     */
    public enum HospitalAIState implements IState
    {
        SEARCH_HOSPITAL,
        GO_TO_HOSPITAL,
        ARRIVE_AT_HOSPITAL,
        WAIT_IN_HOSPITAL,
    }
}
