package com.minecolonies.core.generation.defaults;

import com.minecolonies.api.entity.ModEntities;
import com.minecolonies.api.util.DamageSourceKeys;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static com.minecolonies.api.util.constant.Constants.MOD_ID;

public class DefaultDamageTypeProvider extends JsonCodecProvider<DamageType> {
    public DefaultDamageTypeProvider(@NotNull final PackOutput packOutput,
                                     @NotNull final ExistingFileHelper existingFileHelper,
                                     final CompletableFuture<Provider> lookupProvider) {
        super(packOutput, Target.DATA_PACK, "damage_type", PackType.SERVER_DATA, DamageType.DIRECT_CODEC, lookupProvider, MOD_ID, existingFileHelper);
    }

    @Override
    protected void gather() {
        unconditional(DamageSourceKeys.CONSOLE.location(), damage("console"));
        unconditional(DamageSourceKeys.DEFAULT.location(), damage("default"));
        unconditional(DamageSourceKeys.DESPAWN.location(), damage("despawn"));
        unconditional(DamageSourceKeys.NETHER.location(), damage("nether"));
        unconditional(DamageSourceKeys.GUARD.location(), damage("entity.minecolonies.guard"));
        unconditional(DamageSourceKeys.GUARD_PVP.location(), damage("entity.minecolonies.guardpvp"));
        unconditional(DamageSourceKeys.SLAP.location(), damage("entity.minecolonies.slap"));
        unconditional(DamageSourceKeys.STUCK_DAMAGE.location(), damage("entity.minecolonies.stuckdamage"));
        unconditional(DamageSourceKeys.TRAINING.location(), damage("entity.minecolonies.training"));
        unconditional(DamageSourceKeys.WAKEY.location(), damage("entity.minecolonies.wakeywakey"));
        unconditional(DamageSourceKeys.PIERCE.location(), damage("entity.minecolonies.pierce"));

        unconditional(DamageSourceKeys.AMAZON.location(), entityDamage(ModEntities.AMAZON));
        unconditional(DamageSourceKeys.AMAZONCHIEF.location(), entityDamage(ModEntities.AMAZONCHIEF));
        unconditional(DamageSourceKeys.AMAZONSPEARMAN.location(), entityDamage(ModEntities.AMAZONSPEARMAN));
        unconditional(DamageSourceKeys.ARCHERBARBARIAN.location(), entityDamage(ModEntities.ARCHERBARBARIAN));
        unconditional(DamageSourceKeys.ARCHERMUMMY.location(), entityDamage(ModEntities.ARCHERMUMMY));
        unconditional(DamageSourceKeys.ARCHERPIRATE.location(), entityDamage(ModEntities.ARCHERPIRATE));
        unconditional(DamageSourceKeys.BARBARIAN.location(), entityDamage(ModEntities.BARBARIAN));
        unconditional(DamageSourceKeys.CHIEFBARBARIAN.location(), entityDamage(ModEntities.CHIEFBARBARIAN));
        unconditional(DamageSourceKeys.CHIEFPIRATE.location(), entityDamage(ModEntities.CHIEFPIRATE));
        unconditional(DamageSourceKeys.MERCENARY.location(), entityDamage(ModEntities.MERCENARY));
        unconditional(DamageSourceKeys.MUMMY.location(), entityDamage(ModEntities.MUMMY));
        unconditional(DamageSourceKeys.NORSEMENARCHER.location(), entityDamage(ModEntities.NORSEMEN_ARCHER));
        unconditional(DamageSourceKeys.NORSEMENCHIEF.location(), entityDamage(ModEntities.NORSEMEN_CHIEF));
        unconditional(DamageSourceKeys.PHARAO.location(), entityDamage(ModEntities.PHARAO));
        unconditional(DamageSourceKeys.PIRATE.location(), entityDamage(ModEntities.PIRATE));
        unconditional(DamageSourceKeys.SHIELDMAIDEN.location(), entityDamage(ModEntities.SHIELDMAIDEN));
        unconditional(DamageSourceKeys.SPEAR.location(), entityDamage(ModEntities.SPEAR));
        unconditional(DamageSourceKeys.VISITOR.location(), entityDamage(ModEntities.VISITOR));
        unconditional(DamageSourceKeys.DROWNED_PIRATE.location(), entityDamage(ModEntities.DROWNED_PIRATE));
        unconditional(DamageSourceKeys.DROWNED_ARCHERPIRATE.location(), entityDamage(ModEntities.DROWNED_ARCHERPIRATE));
        unconditional(DamageSourceKeys.DROWNED_CHIEFPIRATE.location(), entityDamage(ModEntities.DROWNED_CHIEFPIRATE));

        unconditional(DamageSourceKeys.CAMP_AMAZON.location(), entityDamage(ModEntities.CAMP_AMAZON));
        unconditional(DamageSourceKeys.CAMP_AMAZONCHIEF.location(), entityDamage(ModEntities.CAMP_AMAZONCHIEF));
        unconditional(DamageSourceKeys.CAMP_AMAZONSPEARMAN.location(), entityDamage(ModEntities.CAMP_AMAZONSPEARMAN));

        unconditional(DamageSourceKeys.CAMP_BARBARIAN.location(), entityDamage(ModEntities.CAMP_BARBARIAN));
        unconditional(DamageSourceKeys.CAMP_CHIEFBARBARIAN.location(), entityDamage(ModEntities.CAMP_CHIEFBARBARIAN));
        unconditional(DamageSourceKeys.CAMP_ARCHERBARBARIAN.location(), entityDamage(ModEntities.CAMP_ARCHERBARBARIAN));

        unconditional(DamageSourceKeys.CAMP_MUMMY.location(), entityDamage(ModEntities.CAMP_MUMMY));
        unconditional(DamageSourceKeys.CAMP_ARCHERMUMMY.location(), entityDamage(ModEntities.CAMP_ARCHERMUMMY));
        unconditional(DamageSourceKeys.CAMP_PHARAO.location(), entityDamage(ModEntities.CAMP_PHARAO));

        unconditional(DamageSourceKeys.CAMP_PIRATE.location(), entityDamage(ModEntities.CAMP_PIRATE));
        unconditional(DamageSourceKeys.CAMP_ARCHERPIRATE.location(), entityDamage(ModEntities.CAMP_ARCHERPIRATE));
        unconditional(DamageSourceKeys.CAMP_CHIEFPIRATE.location(), entityDamage(ModEntities.CAMP_CHIEFPIRATE));

        unconditional(DamageSourceKeys.CAMP_NORSEMENARCHER.location(), entityDamage(ModEntities.CAMP_NORSEMEN_ARCHER));
        unconditional(DamageSourceKeys.CAMP_NORSEMENCHIEF.location(), entityDamage(ModEntities.CAMP_NORSEMEN_CHIEF));
        unconditional(DamageSourceKeys.CAMP_SHIELDMAIDEN.location(), entityDamage(ModEntities.CAMP_SHIELDMAIDEN));
    }

    @NotNull
    private static DamageType entityDamage(@NotNull final EntityType<?> entityType) {
        return damage(entityType.getDescriptionId());
    }

    @NotNull
    private static DamageType damage(@NotNull final String msgId) {
        return new DamageType(msgId, DamageScaling.ALWAYS, 0.1F);
    }
}