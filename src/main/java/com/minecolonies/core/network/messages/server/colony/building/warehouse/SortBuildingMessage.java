package com.minecolonies.core.network.messages.server.colony.building.warehouse;

import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.inventory.api.CombinedItemHandler;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import com.minecolonies.core.util.SortingUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sort the specified building inventory if level greater than or equal to requiredSortLevel.
 */
public class SortBuildingMessage extends AbstractBuildingServerMessage<IBuilding>
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "sort_building_message", SortBuildingMessage::new);

    /**
     * The required level to sort this building.
     */
    private int requiredSortLevel = 1;

    public SortBuildingMessage(final IBuildingView building, final int requiredSortLevel)
    {
        super(TYPE, building);
        this.requiredSortLevel = requiredSortLevel;
    }

    protected SortBuildingMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        requiredSortLevel = buf.readInt();
    }

    /**
     * Writes the required sort level to the buffer.
     *
     * @param buf The buffer being written to.
     */
    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
        buf.writeInt(requiredSortLevel);
    }

    /**
     * Sorts the item handler of the building if the building's level is bigger or equal to the required sort level.
     * 
     * @param ctxIn The context of the payload.
     * @param player The player that sent the message.
     * @param colony The colony that the building is in.
     * @param building The building that needs to be sorted.
     */
    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, final IColony colony, final IBuilding building)
    {
        if (building.getBuildingLevel() >= requiredSortLevel)
        {
            if (building.getItemHandlerCap() instanceof final CombinedItemHandler combinedInv)
            {
                SortingUtils.sort(player.level().registryAccess(), combinedInv);
            }
        }
    }
}
