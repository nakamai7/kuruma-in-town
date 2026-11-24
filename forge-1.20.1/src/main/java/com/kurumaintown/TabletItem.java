package com.kurumaintown;

import com.kurumaintown.network.VehicleToolPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TabletItem extends Item {
    public TabletItem(Properties p) { super(p); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.getTags().contains("admin")) player.getCooldowns().removeCooldown(player.getItemInHand(hand).getItem());
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp)
            VehicleToolPacket.openLookedAt(sp, 1.5, VehicleToolPacket.MODE_TABLET);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
