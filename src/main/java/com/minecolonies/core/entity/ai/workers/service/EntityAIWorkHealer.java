package com.minecolonies.core.entity.ai.workers.service;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.*;
import com.minecolonies.core.Network;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobHealer;
import com.minecolonies.core.datalistener.model.Disease;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.ai.workers.util.Patient;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.network.messages.client.CircleParticleEffectMessage;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.TranslationConstants.PATIENT_FULL_INVENTORY;

/**
 * Healer AI class.
 */
public class EntityAIWorkHealer extends AbstractEntityAIInteract<JobHealer, BuildingHospital>
{
    /**
     * Base xp gain for the smelter.
     */
    private static final double BASE_XP_GAIN = 2;

    /**
     * The current patient.
     */
    private Patient currentPatient = null;

    /**
     * Remote patient to treat.
     */
    private ICitizenData remotePatient;

    /**
     * Player to heal.
     */
    private Player playerToHeal;

    /**
     * Constructor for the Cook. Defines the tasks the cook executes.
     *
     * @param job a cook job to use.
     */
    public EntityAIWorkHealer(@NotNull final JobHealer job)
    {
        super(job);
        super.registerTargets(
          new AITarget(IDLE, START_WORKING, 1),
          new AITarget(START_WORKING, DECIDE, 1),
          new AITarget(DECIDE, this::decide, 20),
          new AITarget(BRING_ITEMS, this::cure, 20),
          new AITarget(HEAL_PLAYER, this::healPlayer, 20),
          new AITarget(REQUEST_ITEMS, this::requestItems, 20),
          new AITarget(WANDER, this::wander, 20)

        );
        worker.setCanPickUpLoot(true);
    }

    /**
     * Decide what to do next. Check if all patients are up date, else update their states. Then check if there is any patient we can cure or request things for.
     *
     * @return the next state to go to.
     */
    private IAIState decide()
    {
        if (walkToBuilding())
        {
            return DECIDE;
        }

        for (final Patient patient : building.getPatients())
        {
            final ICitizenData data = building.getColony().getCitizenManager().getCivilian(patient.getId());
            if (data == null || data.getEntity().isEmpty() || (data.getEntity().isPresent() && !data.getEntity().get().getCitizenData().getCitizenDiseaseHandler().isSick()))
            {
                building.finishPatient(patient.getId());
                continue;
            }
            final EntityCitizen citizen = (EntityCitizen) data.getEntity().get();
            final Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();

            if (patient.getState() == Patient.PatientState.REQUESTED)
            {
                if (disease == null)
                {
                    this.currentPatient = patient;
                    return BRING_ITEMS;
                }

                if (testRandomCureChance())
                {
                    this.currentPatient = patient;
                    return FREE_CURE;
                }

                if (!InventoryUtils.isItemHandlerFull(citizen.getInventoryCitizen()))
                {
                    if (hasCureInInventory(disease, worker.getInventoryCitizen()) ||
                          hasCureInInventory(disease, building.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(null)))
                    {
                        this.currentPatient = patient;
                        return BRING_ITEMS;
                    }

                    final ImmutableList<IRequest<? extends Stack>> list = building.getOpenRequestsOfType(worker.getCitizenData().getId(), TypeToken.of(Stack.class));
                    final ImmutableList<IRequest<? extends Stack>> completed = building.getCompletedRequestsOfType(worker.getCitizenData(), TypeToken.of(Stack.class));
                    for (final ItemStorage cure : disease.cureItems())
                    {
                        if (!InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(), Disease.hasCureItem(cure)))
                        {
                            if (InventoryUtils.getItemCountInItemHandler(building.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(null),
                              Disease.hasCureItem(cure)) >= cure.getAmount())
                            {
                                needsCurrently = new Tuple<>(Disease.hasCureItem(cure), cure.getAmount());
                                return GATHERING_REQUIRED_MATERIALS;
                            }
                            boolean hasCureRequested = false;
                            for (final IRequest<? extends Stack> request : list)
                            {
                                if (Disease.isCureItem(request.getRequest().getStack(), cure))
                                {
                                    hasCureRequested = true;
                                    break;
                                }
                            }
                            for (final IRequest<? extends Stack> request : completed)
                            {
                                if (Disease.isCureItem(request.getRequest().getStack(), cure))
                                {
                                    hasCureRequested = true;
                                    break;
                                }
                            }
                            if (!hasCureRequested)
                            {
                                patient.setState(Patient.PatientState.NEW);
                                break;
                            }
                        }
                    }
                }
                else
                {
                    data.triggerInteraction(new StandardInteraction(Component.translatable(PATIENT_FULL_INVENTORY), ChatPriority.BLOCKING));
                }
            }

            if (patient.getState() == Patient.PatientState.TREATED)
            {
                if (disease == null)
                {
                    this.currentPatient = patient;
                    return BRING_ITEMS;
                }

                if (!hasCureInInventory(disease, citizen.getInventoryCitizen()))
                {
                    patient.setState(Patient.PatientState.NEW);
                    return DECIDE;
                }
            }
        }

        for (final Player player : WorldUtil.getEntitiesWithinBuilding(world,
          Player.class,
          building,
          player -> player.getHealth() < player.getMaxHealth() - 10 - (2 * building.getBuildingLevel())))
        {
            playerToHeal = player;
            return HEAL_PLAYER;
        }

        return DECIDE;
    }

    /**
     * Request the cure for a given patient.
     *
     * @return the next state to go to.
     */
    private IAIState requestItems()
    {
        if (currentPatient == null)
        {
            return DECIDE;
        }

        final ICitizenData data = building.getColony().getCitizenManager().getCivilian(currentPatient.getId());
        if (data == null || !data.getEntity().isPresent() || !data.getEntity().get().getCitizenData().getCitizenDiseaseHandler().isSick())
        {
            currentPatient = null;
            return DECIDE;
        }

        final EntityCitizen citizen = (EntityCitizen) data.getEntity().get();
        if (walkToBlock(citizen.blockPosition()))
        {
            return REQUEST_ITEMS;
        }

        final Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();
        if (disease == null)
        {
            currentPatient = null;
            return DECIDE;
        }

        final ImmutableList<IRequest<? extends Stack>> list = building.getOpenRequestsOfType(worker.getCitizenData().getId(), TypeToken.of(Stack.class));
        final ImmutableList<IRequest<? extends Stack>> completed = building.getCompletedRequestsOfType(worker.getCitizenData(), TypeToken.of(Stack.class));

        for (final ItemStorage cure : disease.cureItems())
        {
            if (!InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(), Disease.hasCureItem(cure))
                  && !InventoryUtils.hasItemInItemHandler(building.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(null), Disease.hasCureItem(cure)))
            {
                boolean hasRequest = false;
                for (final IRequest<? extends Stack> request : list)
                {
                    if (Disease.isCureItem(request.getRequest().getStack(), cure))
                    {
                        hasRequest = true;
                        break;
                    }
                }
                for (final IRequest<? extends Stack> request : completed)
                {
                    if (Disease.isCureItem(request.getRequest().getStack(), cure))
                    {
                        hasRequest = true;
                        break;
                    }
                }
                if (!hasRequest)
                {
                    worker.getCitizenData().createRequestAsync(new Stack(cure));
                }
            }
        }

        currentPatient = null;
        return DECIDE;
    }

    /**
     * Give a citizen the cure.
     *
     * @return the next state to go to.
     */
    private IAIState cure()
    {
        if (currentPatient == null)
        {
            return DECIDE;
        }

        final ICitizenData data = building.getColony().getCitizenManager().getCivilian(currentPatient.getId());
        if (data == null || !data.getEntity().isPresent() || !data.getEntity().get().getCitizenData().getCitizenDiseaseHandler().isSick())
        {
            currentPatient = null;
            return DECIDE;
        }

        final EntityCitizen citizen = (EntityCitizen) data.getEntity().get();
        if (walkToBlock(data.getEntity().get().blockPosition()))
        {
            return BRING_ITEMS;
        }

        final Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();
        if (disease == null)
        {
            currentPatient = null;
            citizen.heal(10);
            worker.getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
            return DECIDE;
        }

        if (!hasCureInInventory(disease, worker.getInventoryCitizen()))
        {
            if (hasCureInInventory(disease, building.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(null)))
            {
                for (final ItemStorage cure : disease.cureItems())
                {
                    if (InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), Disease.hasCureItem(cure)) < cure.getAmount())
                    {
                        needsCurrently = new Tuple<>(Disease.hasCureItem(cure), 1);
                        return GATHERING_REQUIRED_MATERIALS;
                    }
                }
            }
            currentPatient = null;
            return DECIDE;
        }

        if (!hasCureInInventory(disease, citizen.getInventoryCitizen()))
        {
            for (final ItemStorage cure : disease.cureItems())
            {
                if (InventoryUtils.getItemCountInItemHandler(citizen.getInventoryCitizen(), Disease.hasCureItem(cure)) < cure.getAmount())
                {
                    if (InventoryUtils.isItemHandlerFull(citizen.getInventoryCitizen()))
                    {
                        data.triggerInteraction(new StandardInteraction(Component.translatable(PATIENT_FULL_INVENTORY), ChatPriority.BLOCKING));
                        currentPatient = null;
                        return DECIDE;
                    }
                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandler(
                      worker.getInventoryCitizen(),
                      Disease.hasCureItem(cure),
                      cure.getAmount(), citizen.getInventoryCitizen()
                    );
                }
            }
        }

        worker.getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
        currentPatient = null;
        return DECIDE;
    }

    /**
     * Heal the player.
     *
     * @return the next state to go to.
     */
    private IAIState healPlayer()
    {
        if (playerToHeal == null)
        {
            return DECIDE;
        }

        if (walkToBlock(playerToHeal.blockPosition()))
        {
            return getState();
        }

        playerToHeal.heal(playerToHeal.getMaxHealth() - playerToHeal.getHealth() - 5 - building.getBuildingLevel());
        worker.getCitizenExperienceHandler().addExperience(1);

        return DECIDE;
    }

    @Override
    public IAIState getStateAfterPickUp()
    {
        return BRING_ITEMS;
    }

    /**
     * Wander around in the colony from citizen to citizen.
     *
     * @return the next state to go to.
     */
    private IAIState wander()
    {
        if (remotePatient == null || remotePatient.getEntity().isEmpty())
        {
            return DECIDE;
        }

        final EntityCitizen citizen = (EntityCitizen) remotePatient.getEntity().get();
        if (walkToBlock(remotePatient.getEntity().get().blockPosition()))
        {
            return getState();
        }

        Network.getNetwork().sendToTrackingEntity(
          new CircleParticleEffectMessage(
            remotePatient.getEntity().get().position(),
            ParticleTypes.HEART,
            1), worker);

        citizen.heal(citizen.getMaxHealth() - citizen.getHealth() - 5 - building.getBuildingLevel());
        citizen.markDirty(10);
        worker.getCitizenExperienceHandler().addExperience(1);

        remotePatient = null;

        return START_WORKING;
    }

    /**
     * Check if we can cure a citizen randomly. Currently it is done workerLevel/10 times every hour (at least 1).
     *
     * @return true if so.
     */
    private boolean testRandomCureChance()
    {
        return worker.getRandom().nextInt(60 * 60) <= Math.max(1, getSecondarySkillLevel() / 20);
    }

    /**
     * Check if the cure for a certain illness is in the inv.
     *
     * @param disease the disease to check.
     * @param handler the inventory to check.
     * @return true if so.
     */
    private boolean hasCureInInventory(final Disease disease, final IItemHandler handler)
    {
        for (final ItemStorage cure : disease.cureItems())
        {
            if (InventoryUtils.getItemCountInItemHandler(handler, Disease.hasCureItem(cure)) < cure.getAmount())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public Class<BuildingHospital> getExpectedBuildingClass()
    {
        return BuildingHospital.class;
    }
}
