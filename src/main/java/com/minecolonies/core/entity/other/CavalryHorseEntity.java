package com.minecolonies.core.entity.other;

import java.util.UUID;
import javax.annotation.Nonnull;
import com.minecolonies.api.entity.ModEntities;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.jobs.JobCavalry;
import com.minecolonies.core.entity.ai.minimal.CavalryHorseNavigationGoal;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStandGoal;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

public class CavalryHorseEntity extends Horse
{
    private static final float BASE_W = 1.3964844F;
    private static final float BASE_H = 1.6F;
    private static final float SLIM_W = 0.95F; // fits 1-wide corridors
    private static final float SEATING_OFFSET = 0.40F;
    public int logCooldown = 0;
    public static final int LOG_COOLDOWN_INTERVAL = 200;

    public CavalryHorseEntity(EntityType<? extends Horse> type, Level level)
    {
        super(type, level);
    }

    /**
     * Checks if the rider is a guard and if they have a JobCavalry job. If true, the horse will be slim.
     * 
     * @return true if the horse should be slim, false otherwise.
     */
    private boolean shouldBeSlim()
    {
        Entity rider = this.getControllingPassenger();

        if (rider == null)
        {
            return false;
        }

        if (!(rider instanceof EntityCitizen guard) || guard.getCitizenJobHandler() == null)
        {
            return false;
        }

        return guard.getCitizenJobHandler().getColonyJob() instanceof JobCavalry;
    }

    /**
     * Gets the dimensions of the horse entity, adjusting the width for slim poses (i.e. when the rider is a guard with a JobCavalry
     * job). The height is kept the same as the vanilla horse dimensions for the given pose.
     *
     * @param pose the pose of the horse
     * @return the adjusted dimensions of the horse entity
     */
    @Override
    public EntityDimensions getDimensions(@Nonnull Pose pose)
    {
        EntityDimensions base = super.getDimensions(pose);

        if (shouldBeSlim())
        {
            // Keep height from base (pose-aware), only shrink width
            return EntityDimensions.scalable(SLIM_W, base.height);
        }

        return base;
    }

    /**
     * Adjusts the standing eye height based on the pose and dimensions given. For cavalry horses, this height is the same as the
     * standard horse height, since the width change doesn’t affect the eye height.
     * 
     * @param pose the pose of the horse
     * @param dims the dimensions of the horse
     * @return the adjusted eye height
     */
    @Override
    protected float getStandingEyeHeight(@Nonnull Pose pose, @Nonnull EntityDimensions dims)
    {
        return super.getStandingEyeHeight(pose, dims);
    }

    /* Make sure we recompute AABB when the criterion changes */
    @Override
    protected void addPassenger(@Nonnull Entity passenger)
    {
        super.addPassenger(passenger);
        this.refreshDimensions();
    }

    /**
     * Called when a passenger is removed from this horse.
     * 
     * @param passenger the passenger being removed
     */
    @Override
    protected void removePassenger(@Nonnull Entity passenger)
    {
        super.removePassenger(passenger);
        this.refreshDimensions();
    }

    /**
     * Gets the offset that passengers are riding at relative to the horse's y position. In this case, we lower the seat by 0.35
     * units to make the rider's feet line up with the saddle. This is important for the cavalry horse model.
     * 
     * @return the double value of the y offset
     */
    @Override
    public double getPassengersRidingOffset()
    {
        double vanilla = super.getPassengersRidingOffset();
        double seatLowering = SEATING_OFFSET;

        return vanilla - seatLowering;
    }

    /**
     * Creates a new PathNavigation for this horse entity, overriding the default vanilla horse navigation. This allows the horse to
     * navigate the world in a way that is more suitable for guards.
     * 
     * @param level the level to spawn the new entity in
     * @return the new PathNavigation for this horse entity
     */
    @Override
    protected PathNavigation createNavigation(@Nonnull Level level)
    {
        MinecoloniesAdvancedPathNavigate pathNavigation = new MinecoloniesAdvancedPathNavigate(this, level);
        pathNavigation.getPathingOptions().setEnterDoors(false);
        pathNavigation.getPathingOptions().setCanOpenDoors(false);
        pathNavigation.getPathingOptions().withDropCost(1D);
        pathNavigation.getPathingOptions().withJumpCost(1D);
        pathNavigation.getPathingOptions().setPassDanger(false);
        pathNavigation.getPathingOptions().setCanSwim(true);
        pathNavigation.setCanFloat(true);

        return pathNavigation;
    }

    @Override
    public void registerGoals()
    {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // TODO: Create research that improves speed.
        this.goalSelector.addGoal(1, new CavalryHorseNavigationGoal(this, 1.2));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        if (this.canPerformRearing()) {
            this.goalSelector.addGoal(9, new RandomStandGoal(this));
        }
    }


    /**
     * Linearly interpolates between the current angle and the target angle, not
     * exceeding the maximum turn angle per tick.
     * 
     * @param current    the current angle
     * @param target     the target angle
     * @param maxTurnPerTick the maximum angle to turn per tick
     * @return the new angle after turning
     */
    private static float turnToward(float current, float target, float maxTurnPerTick) 
    {
        float delta = net.minecraft.util.Mth.wrapDegrees(target - current);
        if (delta >  maxTurnPerTick) delta =  maxTurnPerTick;
        if (delta < -maxTurnPerTick) delta = -maxTurnPerTick;
        return current + delta;
    }


    /**
     * Called every tick to update the horse. Steer the horse's facing toward the rider's head yaw.
     * This is a gentle, small turn, so as not to fight the navigator too hard.
     */
    @Override
    public void tick()
    {
        super.tick();
        logActiveGoals();

        // Server-side: gently steer facing toward the rider’s head yaw
        if (!level().isClientSide) 
        {
            Entity rider = this.getControllingPassenger();
            if (rider instanceof LivingEntity le) 
            {
                // Rider intent: where their head is facing
                float desiredYaw = le.getYHeadRot();
                // Don’t fight the navigator too hard; small, smooth turns
                float newYaw = turnToward(this.getYRot(), desiredYaw, 15.0f);
                this.setYRot(newYaw);
                this.setYBodyRot(newYaw);
                this.setYHeadRot(newYaw);
            }
        }
    }

    public void logActiveGoals()
    {

        if (logCooldown > 0)
        {
            logCooldown--;
            return;
        }

        logCooldown = LOG_COOLDOWN_INTERVAL;

        for (WrappedGoal wrapped : this.goalSelector.getAvailableGoals())
        {
            Goal goal = wrapped.getGoal();
            if (wrapped.isRunning())
            {
                Log.getLogger().info("Active Goal: " + goal.getClass().getSimpleName());
            }
        }

        for (WrappedGoal wrapped : this.targetSelector.getAvailableGoals())
        {
            Goal goal = wrapped.getGoal();
            if (wrapped.isRunning())
            {
                Log.getLogger().info("Active Target Goal: " + goal.getClass().getSimpleName());
            }
        }
    }

    /**
     * Creates a new CavalryHorseEntity from a vanilla AbstractHorse, attempting to preserve as much information as possible.
     * 
     * @param level   the level to spawn the new entity in
     * @param vanilla the vanilla horse to convert
     * @return the new CavalryHorseEntity, or null if the conversion failed
     */
    public static CavalryHorseEntity createFromVanilla(Level level, AbstractHorse vanilla)
    {
        if (level.isClientSide) return null;

        // If already a CavalryHorseEntity, return it
        if (vanilla instanceof CavalryHorseEntity) return (CavalryHorseEntity) vanilla;

        // If not a living vanilla horse, return null
        if (vanilla == null || !vanilla.isAlive() || vanilla.isVehicle()) return null;

        // --- Snapshot generic AbstractHorse state ---
        final boolean wasTamed = vanilla.isTamed();
        final UUID owner = vanilla.getOwnerUUID();
        final int temper = vanilla.getTemper();
        final double health = vanilla.getHealth();
        final String customName = vanilla.hasCustomName() ? vanilla.getName().getString() : null;

        AttributeInstance healthAttr = vanilla.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance speedAttr = vanilla.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance jumpAttr = vanilla.getAttribute(Attributes.JUMP_STRENGTH);

        double maxHealth = healthAttr != null ? healthAttr.getBaseValue() : 20.0D;
        double moveSpeed = speedAttr != null ? speedAttr.getBaseValue() : 0.25D;
        double jumpStrength = jumpAttr != null ? jumpAttr.getBaseValue() : 0.7D;

        // --- Snapshot Horse-specific state (variant/armor) if applicable ---
        Variant variant = null;
        if (vanilla instanceof Horse h)
        {
            variant = h.getVariant();
        }

        // Leash (if any)
        Entity leashHolder = vanilla.getLeashHolder();

        // --- Convert to our entity type ---
        CavalryHorseEntity cav = vanilla.convertTo(ModEntities.CAVALRY_HORSE, true);
        if (cav == null) return null;

        // --- Re-apply attributes & health (so it "feels" the same) ---
        AttributeInstance cavHealthAttr = cav.getAttribute(Attributes.MAX_HEALTH);
        if (cavHealthAttr != null)
        {
            cavHealthAttr.setBaseValue(maxHealth);
        }

        AttributeInstance cavSpeedAttr = cav.getAttribute(Attributes.MOVEMENT_SPEED);
        if (cavSpeedAttr != null)
        {
            cavSpeedAttr.setBaseValue(moveSpeed);
        }

        AttributeInstance cavJumpAttr = cav.getAttribute(Attributes.JUMP_STRENGTH);
        if (cavJumpAttr != null)
        {
            cavJumpAttr.setBaseValue(jumpStrength);
        }
        cav.setMaxUpStep(1.1F);

        cav.setHealth((float) Math.min(health, maxHealth));

        // --- Re-apply AbstractHorse state ---
        cav.setTamed(wasTamed);
        cav.setOwnerUUID(owner);
        cav.setTemper(temper);
        cav.setPersistenceRequired();

        // --- Re-apply Horse-specific visuals ---
        if (variant != null)
        {
            cav.setVariant(variant);
        }

        // --- Name & leash ---
        if (customName != null)
        {
            cav.setCustomName(Component.literal(customName));
        }
        if (leashHolder != null)
        {
            cav.setLeashedTo(leashHolder, true);
        }

        return cav;
    }

    @Override
    public void remove(@Nonnull RemovalReason reason)
    {
        if (!level().isClientSide && reason == RemovalReason.DISCARDED)
        {
            com.mojang.logging.LogUtils.getLogger()
                .warn("CavalryHorse DISCARDED. UUID={}, mounted={}, at={}",
                    this.getUUID(),
                    this.isVehicle() || this.isPassenger(),
                    this.blockPosition(),
                    new RuntimeException("who-discarded-me?"));
        }
        super.remove(reason);
    }
}
