package com.minecolonies.core.entity.pathfinding.navigation;

import javax.annotation.Nonnull;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.other.AbstractFastMinecoloniesEntity;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveAwayFromLocation;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveCloseToXNearY;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveToLocation;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobRandomPos;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class EntityNavigationUtils
{
    /**
     * Distance to consider being near a building block as reached
     */
    public static int BUILDING_REACH_DIST = 4;

    /**
     * Distance to consider being near a block inside a building as reached
     */
    public static int WOKR_IN_BUILDING_DIST = 7;

    /**
     * Distance which counts as reached
     */
    public static double REACHED_DIST = 1.5;

    /**
     * Tries to walk close to a given pos, staying near another position.
     *
     * @param entity
     * @param desiredPosition
     * @param nearbyPosition
     * @param distToDesired
     * @return True when arrived
     */
    public static boolean walkCloseToXNearY(
        final AbstractFastMinecoloniesEntity entity, final BlockPos desiredPosition,
        final BlockPos nearbyPosition,
        final int distToDesired, final boolean safeDestination)
    {
        return walkCloseToXNearY(entity, desiredPosition, nearbyPosition, distToDesired, safeDestination, 1.0);
    }

    /**
     * Tries to walk close to a given pos, staying near another position.
     *
     * @return True when arrived
     */
    public static boolean walkCloseToXNearY(
        final AbstractFastMinecoloniesEntity entity, final BlockPos desiredPosition,
        final BlockPos nearbyPosition,
        final int distToDesired, final boolean safeDestination, final double speedFactor)
    {
        final MinecoloniesAdvancedPathNavigate nav = ((MinecoloniesAdvancedPathNavigate) entity.getNavigation());

        // Three cases
        // 1. Navigation Finished
        // 2. Navigation is progressing towards a previous task
        // 3. Navigation did not try once
        boolean isOnRightTask = nav.getPathResult() != null && PathJobMoveCloseToXNearY.isJobFor(nav.getPathResult().getJob(), desiredPosition, nearbyPosition, 1);

        if (nav.isDone() || !isOnRightTask)
        {
            if (isOnRightTask)
            {
                // Check distance once navigation is done, to let the entity walk
                if (BlockPosUtil.dist(entity.blockPosition(), desiredPosition) <= distToDesired)
                {
                    nav.stop();
                    return true;
                }
            }
            else if (BlockPosUtil.dist(entity.blockPosition(), desiredPosition) <= REACHED_DIST)
            {
                nav.stop();
                return true;
            }

            nav.walkCloseToXNearY(desiredPosition, nearbyPosition, 1, speedFactor, safeDestination);
        }

        return false;
    }

    /**
     * Walks to a position within a building
     *
     * @return True when arrived
     */
    public static boolean walkToPosInBuilding(
        final AbstractFastMinecoloniesEntity entity, final BlockPos destination, final IBuilding building, final int reachDistance)
    {
        if (building == null)
        {
            return walkToPos(entity, destination, reachDistance, true);
        }

        Tuple<BlockPos, BlockPos> corners = building.getCorners();
        final BlockPos center =
            new BlockPos((corners.getA().getX() + corners.getB().getX()) / 2, building.getPosition().getY(), (corners.getA().getZ() + corners.getB().getZ()) / 2);

        return walkCloseToXNearY(entity, destination, center, reachDistance, true);
    }

    /**
     * Walks to a position within a building
     *
     * @return True when arrived
     */
    public static boolean walkToBuilding(
        final AbstractFastMinecoloniesEntity entity, final IBuilding building)
    {
        if (building == null)
        {
            return true;
        }

        return walkToPosInBuilding(entity, building.getPosition(), building, BUILDING_REACH_DIST);
    }

    /**
     * Walks to a given position
     *
     * @return True when arrived
     */
    public static boolean walkToPos(
        final AbstractFastMinecoloniesEntity entity, final BlockPos desiredPosition, final boolean safeDestination)
    {
        return walkToPos(entity, desiredPosition, BUILDING_REACH_DIST, safeDestination, 1.0);
    }

    /**
     * Walks to a given position
     *
     * @return True when arrived
     */
    public static boolean walkToPos(
        final AbstractFastMinecoloniesEntity entity, final BlockPos desiredPosition,
        final int distToDesired, final boolean safeDestination)
    {
        return walkToPos(entity, desiredPosition, distToDesired, safeDestination, 1.0);
    }

    /**
     * Walks to a given position
     *
     * @return True when arrived
     */
    public static boolean walkToPos(
        final AbstractFastMinecoloniesEntity entity, final BlockPos desiredPosition,
        final int distToDesired, final boolean safeDestination, final double speedFactor)
    {
        final MinecoloniesAdvancedPathNavigate nav = ((MinecoloniesAdvancedPathNavigate) entity.getNavigation());

        return walkToPosHelper(nav, entity, desiredPosition, distToDesired, safeDestination, speedFactor);
    }

    /**
     * Walks to a given position
     *
     * @return True when arrived
     */
    public static boolean walkToPos(
        final IMinecoloniesPather entity, final BlockPos desiredPosition,
        final int distToDesired, final boolean safeDestination, final double speedFactor)
    {
        final MinecoloniesAdvancedPathNavigate nav = ((MinecoloniesAdvancedPathNavigate) entity.getNavigation());

        return walkToPosHelper(nav, (Entity) entity, desiredPosition, distToDesired, safeDestination, speedFactor);
    }

    /**
     * Helper function to walk to a certain position.
     *
     * @param nav the navigation to use
     * @param entity the entity to move
     * @param desiredPosition the position to move to
     * @param distToDesired the minimum distance to the desired position
     * @param safeDestination if the destination is safe and should be set
     * @param speedFactor the speed to move at
     * @return true if the navigation has been updated, false otherwise
     */
    protected static boolean walkToPosHelper(
        MinecoloniesAdvancedPathNavigate nav, Entity entity, final BlockPos desiredPosition,
        final int distToDesired, final boolean safeDestination, final double speedFactor)
    {
        boolean isOnRightTask = (nav.getPathResult() != null
            && PathJobMoveToLocation.isJobFor(nav.getPathResult().getJob(), desiredPosition));

        if (nav.isDone() || !isOnRightTask)
        {
            if (isOnRightTask)
            {
                // Check distance once navigation is done, to let the entity walk
                if (BlockPosUtil.dist(entity.blockPosition(), desiredPosition) <= distToDesired)
                {
                    nav.stop();
                    return true;
                }
            }
            else if (BlockPosUtil.dist(entity.blockPosition(), desiredPosition) <= REACHED_DIST)
            {
                nav.stop();
                return true;
            }

            nav.walkTo(desiredPosition, speedFactor, safeDestination);
        }

        return false;
    }

    /**
     * Walks away from a given position
     *
     * @return True when arrived
     */
    public static boolean walkAwayFrom(final AbstractFastMinecoloniesEntity entity, final BlockPos avoid, final int distance, final double speed)
    {
        final MinecoloniesAdvancedPathNavigate nav = ((MinecoloniesAdvancedPathNavigate) entity.getNavigation());
        boolean isOnRightTask = (nav.getPathResult() != null && PathJobMoveAwayFromLocation.isJobFor(nav.getPathResult().getJob(), distance, avoid));

        if (nav.isDone() || !isOnRightTask)
        {
            if (isOnRightTask)
            {
                // Check distance once navigation is done, to let the entity walk
                if (BlockPosUtil.dist(entity.blockPosition(), avoid) >= distance)
                {
                    nav.stop();
                    return true;
                }
            }

            nav.walkAwayFrom(avoid, distance, speed, false);
        }

        return false;
    }

    /**
     * Walks to a random position a given distance away
     *
     * @return True when arrived
     */
    public static boolean walkToRandomPos(final AbstractFastMinecoloniesEntity entity, final int range, final double speedFactor)
    {
        final MinecoloniesAdvancedPathNavigate nav = ((MinecoloniesAdvancedPathNavigate) entity.getNavigation());
        boolean isOnRightTask = (nav.getPathResult() != null && nav.getPathResult().getJob() instanceof PathJobRandomPos);

        if (nav.isDone() || !isOnRightTask)
        {
            if (isOnRightTask)
            {
                nav.stop();
                return true;
            }

            nav.walkToRandomPos(range, speedFactor);
        }

        return false;
    }

    /**
     * Walks to a random position a given distance away within the provided box
     *
     * @return True when arrived
     */
    public static boolean walkToRandomPosWithin(final AbstractFastMinecoloniesEntity entity, final int range, final double speedFactor, final Tuple<BlockPos, BlockPos> corners, final boolean preferInside)
    {
        final MinecoloniesAdvancedPathNavigate nav = ((MinecoloniesAdvancedPathNavigate) entity.getNavigation());
        boolean isOnRightTask = (nav.getPathResult() != null && nav.getPathResult().getJob() instanceof PathJobRandomPos);

        if (nav.isDone() || !isOnRightTask)
        {
            if (isOnRightTask)
            {
                nav.stop();
                return true;
            }

            nav.walkToRandomPos(range, speedFactor, corners, preferInside);
        }

        return false;
    }

    /**
     * Walks to a random position a given distance away within the provided box
     *
     * @return True when arrived
     */
    public static boolean walkToRandomPosWithin(final AbstractFastMinecoloniesEntity entity, final int range, final double speedFactor, final Tuple<BlockPos, BlockPos> corners)
    {
        return walkToRandomPosWithin(entity, range, speedFactor, corners, false);
    }

    /**
     * Walks to a random position a given distance away around the provided center
     *
     * @return True when arrived
     */
    public static boolean walkToRandomPosAround(final AbstractFastMinecoloniesEntity entity, final BlockPos center, final int range, final double speedFactor)
    {
        final MinecoloniesAdvancedPathNavigate nav = ((MinecoloniesAdvancedPathNavigate) entity.getNavigation());
        return walkToRandomPosHelper(nav, center, range, speedFactor);
    }


    /**
     * Walks to a random position a given distance away around the provided center
     *
     * @return True when arrived
     */
    public static boolean walkToRandomPosAround(IMinecoloniesPather entity, final BlockPos center, final int range, final double speedFactor)
    {
        final MinecoloniesAdvancedPathNavigate nav = ((MinecoloniesAdvancedPathNavigate) entity.getNavigation());
        return walkToRandomPosHelper(nav, center, range, speedFactor);
    }

    /**
     * Helper function to walk to a random position a given distance away around the provided center.
     *
     * @param nav the navigation to use
     * @param center the center of the random position
     * @param range the range of the random position
     * @param speedFactor the speed factor to use
     * @return true if an acceptible destination has been reached.
     */
    protected static boolean walkToRandomPosHelper(MinecoloniesAdvancedPathNavigate nav, final BlockPos center, final int range, final double speedFactor)
    {
        boolean isOnRightTask = (nav.getPathResult() != null && PathJobRandomPos.isJobFor(nav.getPathResult().getJob(), center, range));

        if (nav.isDone() || !isOnRightTask)
        {
            if (isOnRightTask)
            {
                nav.stop();
                return true;
            }

            nav.walkToRandomPosAround(range, speedFactor, center);
        }

        return false;
    }

    /**
     * Check if an entity can stand at the given BlockPos without clipping into any blocks or fluids or falling through the ground.
     *
     * @param level the level to check
     * @param feet the position to check
     * @return true if the entity can stand at the given position
     */
    public static boolean isStandable(Level level, BlockPos feet)
    {
        // Two blocks of headroom at feet and head
        if (!level.isEmptyBlock(feet) || !level.isEmptyBlock(feet.above())) return false;
        // Not in liquid
        if (!level.getFluidState(feet).isEmpty()) return false;

        // Solid (or at least collision) support under feet
        BlockPos below = feet.below();
        BlockState belowState = level.getBlockState(below);
        return !belowState.getCollisionShape(level, below).isEmpty();
    }

    /**
     * Find a reasonable "ground" position at the given X/Z. 
     * Starts at the heightmap surface and nudges up/down a few blocks if needed.
     */
    public static BlockPos surfaceAt(Level level, int x, int z)
    {
        // Heightmap gives first air block above motion-blocking (ignores leaves): good for pathing.
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);

        // Fast path
        if (isStandable(level, pos)) 
        {
            return pos;
        }

        // Tiny local search up/down to land on something standable
        final int minY = level.getMinBuildHeight() + 1;
        final int maxY = level.getMaxBuildHeight() - 2;

        // Try stepping downward first (e.g., if we’re on tall grass/bush)
        BlockPos.MutableBlockPos m = pos.mutable();
        for (int i = 0; i < 6 && m.getY() > minY; i++)
        {
            m.move(0, -1, 0);
            if (isStandable(level, m)) return m.immutable();
        }

        // If that failed, try stepping upward (e.g., if we landed inside a fence/crop)
        m.set(pos);
        for (int i = 0; i < 6 && m.getY() < maxY; i++)
        {
            m.move(0, 1, 0);
            if (isStandable(level, m)) return m.immutable();
        }

        // Fallback: return the original surface; caller can decide to skip if not standable
        return pos;
    }

    /**
     * Find the closest valid surface position outside the given building that is a corner of its footprint.
     * If the building does not have a valid corner, return the first corner of its footprint.
     * @param building the building to find a corner for
     * @param referencePos the position to find the closest corner to
     * @return the closest valid surface position outside the building that is a corner of its footprint
     */
    public static BlockPos closestOutsideCornerofBuilding(@Nonnull IBuilding building, @Nonnull BlockPos referencePos)
    {
        Tuple<BlockPos, BlockPos> box = building.getCorners();
        BlockPos a = box.getA();
        BlockPos b = box.getB();

        // All 4 corners of the building’s footprint
        BlockPos[] cornersXZ = new BlockPos[] {new BlockPos(a.getX(), 0, a.getZ()),
            new BlockPos(a.getX(), 0, b.getZ()),
            new BlockPos(b.getX(), 0, a.getZ()),
            new BlockPos(b.getX(), 0, b.getZ())};

        Level level = building.getColony().getWorld(); // or buildingGuards.getLevel()/getEntity().level()

        // Surface each corner
        BlockPos[] surfaced = new BlockPos[cornersXZ.length];
        for (int i = 0; i < cornersXZ.length; i++)
        {
            BlockPos xz = cornersXZ[i];
            surfaced[i] = EntityNavigationUtils.surfaceAt(level, xz.getX(), xz.getZ());
        }

        // Choose the closest valid surfaced corner to the guard

        BlockPos best = surfaced[0];
        double bestDist2 = Double.POSITIVE_INFINITY;

        for (BlockPos p : surfaced)
        {
            if (!EntityNavigationUtils.isStandable(level, p)) continue; // skip clearly bad spots
            double d2 = p.distSqr(referencePos);
            if (d2 < bestDist2)
            {
                bestDist2 = d2;
                best = p;
            }
        }

        return best;
    }

}
