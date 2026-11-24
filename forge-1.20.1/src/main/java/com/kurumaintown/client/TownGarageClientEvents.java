package com.kurumaintown.client;

import com.kurumaintown.KurumaInTown;
import com.kurumaintown.TownNetwork;
import com.kurumaintown.network.GarageOpenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=KurumaInTown.MODID, value=Dist.CLIENT)
public final class TownGarageClientEvents {
    private static boolean wasFDown;
    private TownGarageClientEvents() {}
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();
        if (mc.getWindow()==null) return;
        boolean down=GLFW.glfwGetKey(mc.getWindow().getWindow(),GLFW.GLFW_KEY_F)==GLFW.GLFW_PRESS;
        boolean pressed=down&&!wasFDown;
        wasFDown=down;
        if (pressed&&mc.player!=null&&mc.level!=null&&mc.screen==null) {
            // F is also Minecraft's default swap-hands key. The vanilla swap has already
            // been processed by this END phase, so swap the selected hotbar slot with
            // offhand once more through the normal menu packet to restore the original items.
            TownNetwork.CHANNEL.sendToServer(new GarageOpenPacket());
            if (mc.gameMode != null) {
                int hotbarMenuSlot = 36 + mc.player.getInventory().selected;
                mc.gameMode.handleInventoryMouseClick(
                        mc.player.containerMenu.containerId,
                        hotbarMenuSlot,
                        40,
                        ClickType.SWAP,
                        mc.player);
            }
        }
    }
}
