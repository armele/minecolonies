package com.minecolonies.core.entity.other;

import com.minecolonies.api.util.Log;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.minecolonies.api.util.constant.Constants;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID)
public class CavalryHorseDebug
{
    /**
     * Logs when a cavalry horse is hurt.
     * @param e the hurt event.
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e)
    {
        if (e.getEntity() instanceof CavalryHorseEntity)
        {
            Log.getLogger()
                .info("CavHorse hurt: {} cause={} amount={}", e.getEntity().getUUID(), e.getSource().type().msgId(), e.getAmount());
        }
    }

    /**
     * Logs when a cavalry horse dies.
     * @param e the death event.
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e)
    {
        if (e.getEntity() instanceof CavalryHorseEntity)
        {
            Log.getLogger().warn("CavHorse died: {} cause={}", e.getEntity().getUUID(), e.getSource().type().msgId());
        }
    }

    /**
     * Logs when a cavalry horse leaves the level.
     * @param e the leave event.
     */
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent e)
    {
        if (e.getEntity() instanceof CavalryHorseEntity ch)
        {
            Log.getLogger().warn("CavHorse left level: {} reason={}", ch.getUUID(), ch.getRemovalReason()); // may be null sometimes
        }
    }
}
