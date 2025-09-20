package com.minecolonies.core.entity.pathfinding.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

public interface IMinecoloniesPather
{
    // This interface should be applied only to classes whose navigation can be safely cast to MinecoloniesAdvancedPathNavigate
    public PathNavigation getNavigation();
    public BlockPos blockPosition();
}
