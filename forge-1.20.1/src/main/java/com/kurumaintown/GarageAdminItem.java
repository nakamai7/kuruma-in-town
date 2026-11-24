package com.kurumaintown;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
public class GarageAdminItem extends Item {
    public GarageAdminItem(Properties p){super(p);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if (player.getTags().contains("admin")) player.getCooldowns().removeCooldown(player.getItemInHand(hand).getItem());
        if(!level.isClientSide && player instanceof ServerPlayer sp) {
            if (sp.isShiftKeyDown()) GarageAdminService.openRegistry(sp);
            else GarageAdminService.open(sp,null);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
