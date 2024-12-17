package com.minecolonies.core.entity.ai.minimal;

import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.entity.ai.IStateAI;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.entity.ai.workers.util.Patient.PatientType;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.network.chat.Component;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.IDLE;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.TranslationConstants.HURT_NO_HOSPITAL;

/**
 * The AI task for citizens to execute when they are supposed to heal up at the hospital.
 */
public class EntityAIHurtTask extends EntityAIBeAtHospitalTask implements IStateAI
{
    /**
     * Instantiates this task.
     *
     * @param citizen the citizen.
     */
    public EntityAIHurtTask(final EntityCitizen citizen)
    {
        super(citizen);

        citizen.getCitizenAI().addTransition(new TickingTransition<>(CitizenAIState.HURT, citizen.getCitizenHealthHandler()::isHurt, this::requiresHospital, TICKS_SECOND));
    }

    @Override
    protected void performActionsInHospital(final BuildingHospital hospital)
    {
        // No-op, healing automatically happens inside EntityCitizen
    }

    @Override
    protected IAIState nextAIStateWhenNoHospital()
    {
        citizenData.triggerInteraction(new StandardInteraction(Component.translatable(HURT_NO_HOSPITAL), Component.translatable(HURT_NO_HOSPITAL), ChatPriority.BLOCKING));
        return IDLE;
    }

    @Override
    protected PatientType getPatientType()
    {
        return PatientType.HURT;
    }
}
