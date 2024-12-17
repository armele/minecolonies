package com.minecolonies.core.entity.ai.minimal;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.IStateAI;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.SoundUtils;
import com.minecolonies.core.Network;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.datalistener.model.Disease;
import com.minecolonies.core.entity.ai.workers.util.Patient.PatientType;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.network.messages.client.CircleParticleEffectMessage;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.minecolonies.api.util.constant.GuardConstants.BASIC_VOLUME;
import static com.minecolonies.api.util.constant.TranslationConstants.NO_HOSPITAL;
import static com.minecolonies.core.entity.ai.minimal.EntityAISickTask.DiseaseState.*;

/**
 * The AI task for citizens to execute when they are sick.
 */
public class EntityAISickTask extends EntityAIBeAtHospitalTask implements IStateAI
{
    /**
     * Min distance to hut before pathing to hospital.
     */
    private static final int MIN_DIST_TO_HUT = 5;

    /**
     * Required time to cure.
     */
    private static final int REQUIRED_TIME_TO_CURE = 60;

    /**
     * Chance for a random cure to happen.
     */
    private static final int CHANCE_FOR_RANDOM_CURE = 10;

    private int cureTicks = 0;

    /**
     * Instantiates this task.
     *
     * @param citizen the citizen.
     */
    public EntityAISickTask(final EntityCitizen citizen)
    {
        super(citizen);

        citizen.getCitizenAI().addTransition(new TickingTransition<>(CitizenAIState.SICK, this::isSick, () -> GO_TO_HUT, 20));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(GO_TO_HUT, () -> true, this::goToHut, 20));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(CHECK_FOR_CURE, () -> true, this::checkForCure, 20));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(APPLY_CURE, () -> true, this::applyCure, 20));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(WANDER, () -> true, this::wander, 200));
    }

    private boolean isSick()
    {
        if (citizen.getCitizenData().getCitizenDiseaseHandler().isSick())
        {
            reset();
            return true;
        }

        return false;
    }

    /**
     * Do a bit of wandering.
     *
     * @return start over.
     */
    public IState wander()
    {
        citizen.getNavigation().moveToRandomPos(10, 0.6D);
        return CHECK_FOR_CURE;
    }

    /**
     * Checks if the citizen has the cure in the inventory and makes a decision based on that.
     *
     * @return the next state to go to.
     */
    private IState checkForCure()
    {
        final Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();
        if (disease == null)
        {
            reset();
            return CitizenAIState.IDLE;
        }

        return hasCureItems(disease) ? APPLY_CURE : requiresHospital();
    }

    /**
     * Actual action for applying the cure.
     *
     * @return the next state to go to, if successful CitizenAIState.IDLE.
     */
    private IState applyCure()
    {
        final Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();
        if (disease == null)
        {
            return CitizenAIState.IDLE;
        }

        if (!hasCureItems(disease))
        {
            return CHECK_FOR_CURE;
        }

        final List<ItemStorage> list = disease.cureItems();
        if (!list.isEmpty())
        {
            citizen.setItemInHand(InteractionHand.MAIN_HAND, list.get(citizen.getRandom().nextInt(list.size())).getItemStack());
        }

        citizen.swing(InteractionHand.MAIN_HAND);
        citizen.playSound(SoundEvents.NOTE_BLOCK_HARP.get(), (float) BASIC_VOLUME, (float) SoundUtils.getRandomPentatonic(citizen.getRandom()));
        Network.getNetwork().sendToTrackingEntity(
          new CircleParticleEffectMessage(
            citizen.position().add(0, 2, 0),
            ParticleTypes.HAPPY_VILLAGER, 1), citizen);

        cureTicks++;
        if (cureTicks < REQUIRED_TIME_TO_CURE)
        {
            return APPLY_CURE;
        }

        cure();
        return CitizenAIState.IDLE;
    }

    /**
     * Cure the citizen.
     */
    private void cure()
    {
        final Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();
        if (disease != null)
        {
            for (final ItemStorage cure : disease.cureItems())
            {
                final int slot = InventoryUtils.findFirstSlotInProviderNotEmptyWith(citizen, Disease.hasCureItem(cure));
                if (slot != -1)
                {
                    citizenData.getInventory().extractItem(slot, 1, false);
                }
            }
        }

        citizen.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        citizen.getCitizenData().getCitizenDiseaseHandler().cure();
    }

    /**
     * Go to the hut to move to the hospital from there.
     *
     * @return the next state to go to.
     */
    private IState goToHut()
    {
        final IBuilding buildingWorker = citizenData.getWorkBuilding();
        if (buildingWorker == null)
        {
            return WANDER;
        }

        if (citizen.isWorkerAtSiteWithMove(buildingWorker.getPosition(), MIN_DIST_TO_HUT))
        {
            return CHECK_FOR_CURE;
        }
        return GO_TO_HUT;
    }

    @Override
    protected void performActionsInHospital(final BuildingHospital hospital)
    {
        final Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();
        if (disease == null)
        {
            return;
        }

        if (citizen.getRandom().nextInt(10000) < CHANCE_FOR_RANDOM_CURE)
        {
            cure();
            return;
        }

        applyCure();
    }

    @Override
    protected IState nextAIStateWhenNoHospital()
    {
        final Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();
        if (disease != null)
        {
            citizenData.triggerInteraction(new StandardInteraction(Component.translatable(NO_HOSPITAL, disease.name(), disease.getCureString()),
              Component.translatable(NO_HOSPITAL),
              ChatPriority.BLOCKING));
        }
        return WANDER;
    }

    @Override
    protected PatientType getPatientType()
    {
        return PatientType.SICK;
    }

    @Override
    protected void reset()
    {
        super.reset();
        cureTicks = 0;
    }

    /**
     * Check if the citizen has all required items to cure the disease in their inventory.
     *
     * @param disease the input disease.
     * @return true if all cure items are found in the citizen inventory.
     */
    private boolean hasCureItems(final @NotNull Disease disease)
    {
        for (final ItemStorage cure : disease.cureItems())
        {
            final int slot = InventoryUtils.findFirstSlotInProviderNotEmptyWith(citizen, Disease.hasCureItem(cure));
            if (slot == -1)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * The different types of AIStates related to being sick.
     */
    public enum DiseaseState implements IState
    {
        GO_TO_HUT,
        CHECK_FOR_CURE,
        APPLY_CURE,
        WANDER
    }
}
