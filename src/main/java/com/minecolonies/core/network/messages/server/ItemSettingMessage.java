package com.minecolonies.core.network.messages.server;

import com.ldtteam.common.network.AbstractServerPlayMessage;
import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.items.ItemClipboard;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ItemSettingMessage extends AbstractServerPlayMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "item_setting", ItemSettingMessage::new);

    public String settingName;
    public int settingValue;

    public ItemSettingMessage(String name, int value)
    {
        super(TYPE);
        this.settingName = name;
        this.settingValue = value;
    }

    protected ItemSettingMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        this.settingName = buf.readUtf();
        this.settingValue = buf.readInt();
    }

    /**
     * Executes the onExecute method which sets the custom data of an ItemClipboard.
     * If the player or the stack is null, or the stack does not contain an ItemClipboard,
     * the method returns early. Otherwise, it reads the current custom data (or creates
     * an empty one if it doesn't exist), mutates a copy of it by setting the settingName
     * to the settingValue, sets the new custom data back to the stack, and notifies the
     * player's inventory and menu of the change.
     *
     * @param context the IPayloadContext object
     * @param player the ServerPlayer object
     */
    @Override
    protected void onExecute(IPayloadContext context, ServerPlayer player)
    {
        
        if (player == null) return;

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (stack == null || !(stack.getItem() instanceof ItemClipboard)) 
        {
            return;
        } 

        // Read current custom_data (or empty), mutate a copy, then set it back
        final CustomData current = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        final CompoundTag tag = current.copyTag();
        tag.putBoolean(settingName, settingValue > 0);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        
        // Make sure inventories/menus notice the change
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }


    @Override
    protected void toBytes(RegistryFriendlyByteBuf buf)
    {
        buf.writeUtf(settingName);
        buf.writeInt(settingValue);
    }
    
}
