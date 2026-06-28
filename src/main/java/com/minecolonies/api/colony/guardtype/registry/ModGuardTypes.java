package com.minecolonies.api.colony.guardtype.registry;

import com.minecolonies.api.colony.guardtype.GuardType;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModGuardTypes
{

    public static final ResourceLocation KNIGHT_ID = new ResourceLocation(Constants.MOD_ID, "knight");
    public static final ResourceLocation RANGER_ID = new ResourceLocation(Constants.MOD_ID, "ranger");
    public static final ResourceLocation DRUID_ID  = new ResourceLocation(Constants.MOD_ID, "druid");
    public static final ResourceLocation CAVALRY_ID = new ResourceLocation(Constants.MOD_ID, "cavalry");
    public static final ResourceLocation HUSCARL_ID = new ResourceLocation(Constants.MOD_ID, "huscarl");
    public static final ResourceLocation MARKSMAN_ID  = new ResourceLocation(Constants.MOD_ID, "marksman");

    public static DeferredHolder<GuardType, GuardType> knight;
    public static DeferredHolder<GuardType, GuardType> ranger;
    public static DeferredHolder<GuardType, GuardType> druid;
    public static DeferredHolder<GuardType, GuardType> huscarl;
    public static DeferredHolder<GuardType, GuardType> marksman;
    public static DeferredHolder<GuardType, GuardType> cavalry;

    private ModGuardTypes()
    {
        throw new IllegalStateException("Tried to initialize: ModGuardTypes but this is a Utility class.");
    }
}
