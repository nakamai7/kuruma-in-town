package com.kurumaintown.client;
import com.kurumaintown.TownNetwork;
import com.kurumaintown.network.GaragePlacePacket;
import com.kurumaintown.network.GarageRotatePacket;
import com.kurumaintown.network.GarageMarkerSyncPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public class GaragePlacementScreen extends Screen {
    private final boolean editing;
    private boolean job;
    private float yaw;
    private Button toggle;
    public GaragePlacementScreen(GarageMarkerSyncPacket.Marker marker) {
        super(Component.literal("ガレージ"));
        editing=marker!=null;
        yaw=marker==null?0:marker.yaw();
    }
    @Override protected void init() {
        int x=width/2-95, y=height/2-32;
        if(editing){
            addRenderableWidget(Button.builder(Component.literal("向き -15°"),b->changeYaw(-15)).bounds(x,y,92,22).build());
            addRenderableWidget(Button.builder(Component.literal("向き +15°"),b->changeYaw(15)).bounds(x+98,y,92,22).build());
            addRenderableWidget(Button.builder(Component.literal("向きを適用"),b->{TownNetwork.CHANNEL.sendToServer(new GarageRotatePacket(yaw));onClose();}).bounds(x,y+32,190,22).build());
            addRenderableWidget(Button.builder(Component.literal("削除"),b->{TownNetwork.CHANNEL.sendToServer(new com.kurumaintown.network.GarageDeletePacket());onClose();}).bounds(width-76,height-30,68,20).build());
        } else {
            toggle=addRenderableWidget(Button.builder(label(),b->{job=!job;toggle.setMessage(label());}).bounds(x,y,190,22).build());
            addRenderableWidget(Button.builder(Component.literal("設置"),b->{TownNetwork.CHANNEL.sendToServer(new GaragePlacePacket(job));onClose();}).bounds(x,y+32,190,22).build());
        }
    }
    private void changeYaw(float delta){yaw=(yaw+delta)%360;if(yaw<0)yaw+=360;}
    private Component label(){return Component.literal("jobガレージ: " + (job?"オン":"オフ"));}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderBackground(g);super.render(g,mx,my,pt);
        g.drawCenteredString(font,editing?"ガレージ編集":"ガレージ設置",width/2,height/2-58,0xFFFFFF);
        if(editing)g.drawCenteredString(font,String.format(java.util.Locale.ROOT,"向き: %.0f°",yaw),width/2,height/2+34,0xFFFFFF);
    }
    @Override public boolean isPauseScreen(){return false;}
}
