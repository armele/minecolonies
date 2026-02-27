package com.minecolonies.core.colony.jobs;

import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.resources.ResourceLocation;
import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.entity.ai.workers.guard.EntityAICavalry;
import com.minecolonies.core.util.AttributeModifierUtils;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.research.util.ResearchConstants.SHIELD_USAGE;
import static com.minecolonies.api.util.constant.CitizenConstants.GUARD_HEALTH_MOD_LEVEL_NAME;
import static com.minecolonies.api.util.constant.GuardConstants.CAVALRY_HP_BONUS;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_BANNER_PATTERNS;

/**
 * The Knight's job class
 *
 * @author Asherslab
 */
public class JobCavalry extends AbstractJobGuard<JobCavalry>
{
    public static final float MOUNT_DAMAGE_SPLIT = .20f;

    protected boolean missingMount = false;

    /**
     * Initialize citizen data.
     *
     * @param entity the citizen data.
     */
    public JobCavalry(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public EntityAICavalry generateGuardAI()
    {
        return new EntityAICavalry(this);
    }

    @Override
    public void onLevelUp()
    {
        // Bonus Health for knights(gets reset upon Firing)
        if (getCitizen().getEntity().isPresent())
        {
            final AbstractEntityCitizen citizen = getCitizen().getEntity().get();

            // +1 Heart every 2 level
            final AttributeModifier healthModLevel =
              new AttributeModifier(GUARD_HEALTH_MOD_LEVEL_NAME,
                getCitizen().getCitizenSkillHandler().getLevel(Skill.Stamina) + CAVALRY_HP_BONUS,
                AttributeModifier.Operation.ADDITION);
            AttributeModifierUtils.addHealthModifier(citizen, healthModLevel);
        }
    }

    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.KNIGHT_GUARD_ID;
    }

    @Override
    public boolean ignoresDamage(@NotNull final DamageSource damageSource)
    {
        if(damageSource.is(DamageTypeTags.IS_EXPLOSION) && this.getColony().getResearchManager().getResearchEffects().getEffectStrength(SHIELD_USAGE) > 0
                && InventoryUtils.findFirstSlotInItemHandlerWith(this.getCitizen().getInventory(), Items.SHIELD) != -1)
        {
            if (!this.getCitizen().getEntity().isPresent())
            {
                return true;
            }
            final AbstractEntityCitizen worker = this.getCitizen().getEntity().get();
            CitizenItemUtils.setHeldItem(worker, InteractionHand.OFF_HAND, InventoryUtils.findFirstSlotInItemHandlerWith(this.getCitizen().getInventory(), Items.SHIELD));
            worker.startUsingItem(InteractionHand.OFF_HAND);

            // Apply the colony Flag to the shield
            ItemStack shieldStack = worker.getInventoryCitizen().getHeldItem(InteractionHand.OFF_HAND);
            CompoundTag nbt = shieldStack.getOrCreateTagElement("BlockEntityTag");
            nbt.put(TAG_BANNER_PATTERNS, worker.getCitizenColonyHandler().getColonyOrRegister().getColonyFlag());

            worker.decreaseSaturationForContinuousAction();
            return true;
        }
        return super.ignoresDamage(damageSource);
    }

    /**
     * If the knight is missing a mount.
     *
     * @return true if so.
     */
    public boolean isMissingMount()
    {
        return missingMount;
    }

    /**
     * Set whether the knight is missing a mount.
     *
     * @param missingMount whether the cavalry unit is missing a mount.
     */
    public void setMissingMount(final boolean missingMount)
    {
        this.missingMount = missingMount;
    }

    /**
     * The fraction of damage that is applied to the mount instead of the rider.
     * This is used to calculate the damage to apply to the mount when the rider is attacked.
     * @return the fraction of damage to apply to the mount.
     */
    public float getMountDamageSplit()
    {
        return MOUNT_DAMAGE_SPLIT;
    }

    /**
     * Gets the weapon type that the AI will look for when checking if it can attack.
     *
     * @return the weapon type.
     */
    public static EquipmentTypeEntry getWeaponType()
    {
        return ModEquipmentTypes.spear.get();
    }
}
