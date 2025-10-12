package com.minecolonies.core.entity.pathfinding.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

/**
 * This interface should be applied only to classes whose navigation can be safely cast to MinecoloniesAdvancedPathNavigate
 */
public interface IMinecoloniesPather
{
    public PathNavigation getNavigation();
    public BlockPos blockPosition();
}
