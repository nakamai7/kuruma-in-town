package com.kurumaintown;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
public class GarageItem extends Item {
    public GarageItem(Properties p) { super(p); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.getTags().contains("admin")) player.getCooldowns().removeCooldown(player.getItemInHand(hand).getItem());
        if (level.isClientSide) com.kurumaintown.client.GaragePlacementClient.open();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
