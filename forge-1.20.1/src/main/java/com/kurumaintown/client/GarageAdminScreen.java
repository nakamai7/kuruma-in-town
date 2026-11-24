package com.kurumaintown.client;
import com.kurumaintown.TownNetwork;
import com.kurumaintown.network.GarageAdminActionPacket;
import com.kurumaintown.network.GarageAdminRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
public class GarageAdminScreen extends Screen{
    private final CompoundTag players;
    private final String selected;
    private int scroll=0;
    private final List<String> keys=new ArrayList<>();
    public GarageAdminScreen(CompoundTag players,String selected){super(Component.literal("ガレージ管理"));this.players=players;this.selected=selected;}
    @Override protected void init(){
        keys.clear();
        if(selected.isEmpty())keys.addAll(players.getAllKeys());
        else if(players.contains(selected,10))keys.addAll(players.getCompound(selected).getCompound("cars").getAllKeys());
        keys.sort(String::compareTo);
        int w=Math.min(410,width-24),x=(width-w)/2,y=Math.max(24,(height-Math.min(350,height-30))/2);
        int visible=Math.max(1,(Math.min(350,height-30)-(selected.isEmpty()?85:50))/35);
        scroll=Math.min(scroll,Math.max(0,keys.size()-visible));
        if(selected.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("ジョブ1"),b->TownNetwork.CHANNEL.sendToServer(new GarageAdminRequestPacket("00000000-0000-0000-0000-000000000001"))).bounds(x+10,y+33,(w-30)/2,20).build());
            addRenderableWidget(Button.builder(Component.literal("ジョブ2"),b->TownNetwork.CHANNEL.sendToServer(new GarageAdminRequestPacket("00000000-0000-0000-0000-000000000002"))).bounds(x+20+(w-30)/2,y+33,(w-30)/2,20).build());
        }
        if(!selected.isEmpty())addRenderableWidget(Button.builder(Component.literal("戻る"),b->TownNetwork.CHANNEL.sendToServer(new GarageAdminRequestPacket(""))).bounds(x+w-65,y+5,55,20).build());
        for(int i=scroll;i<Math.min(keys.size(),scroll+visible);i++){
            String key=keys.get(i);int yy=y+(selected.isEmpty()?70:35)+(i-scroll)*35;
            if(selected.isEmpty())addRenderableWidget(Button.builder(Component.literal("閲覧"),b->TownNetwork.CHANNEL.sendToServer(new GarageAdminRequestPacket(key))).bounds(x+w-68,yy,58,20).build());
            else {
                CompoundTag carEntry=players.getCompound(selected).getCompound("cars").getCompound(key);
                CompoundTag carTown=carEntry.getCompound("vehicledata").getCompound("town");
                int inGarage=carEntry.contains("ingarage")?carEntry.getInt("ingarage"):carTown.getCompound("cardata").getInt("ingarage");
                Button transfer=Button.builder(Component.literal("ガレージへ転送"),b->act(key,false)).bounds(x+w-171,yy,104,20).build();
                transfer.active=inGarage!=1;
                addRenderableWidget(transfer);
                addRenderableWidget(Button.builder(Component.literal("削除"),b->act(key,true)).bounds(x+w-61,yy,51,20).build());
            }
        }
    }
    private void act(String key,boolean delete){TownNetwork.CHANNEL.sendToServer(new GarageAdminActionPacket(selected,key,delete));onClose();}
    @Override public boolean mouseScrolled(double mx,double my,double delta){int max=Math.max(0,keys.size()-Math.max(1,(Math.min(350,height-30)-(selected.isEmpty()?85:50))/35));int next=Math.max(0,Math.min(max,scroll-(int)Math.signum(delta)));if(next!=scroll){scroll=next;clearWidgets();init();}return true;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);int w=Math.min(410,width-24),x=(width-w)/2,y=Math.max(24,(height-Math.min(350,height-30))/2),h=Math.min(350,height-30);
        g.fill(x,y,x+w,y+h,0xEE19212B);g.drawString(font,selected.isEmpty()?"ガレージ管理 - 5m以内のプレイヤー":selected.endsWith("000000000001")?"ジョブ1 - 車両一覧":selected.endsWith("000000000002")?"ジョブ2 - 車両一覧":"所有車両一覧",x+10,y+11,0xFFFFFFFF,false);
        int visible=Math.max(1,(h-(selected.isEmpty()?85:50))/35);
        for(int i=scroll;i<Math.min(keys.size(),scroll+visible);i++){String key=keys.get(i);int yy=y+(selected.isEmpty()?70:35)+(i-scroll)*35;g.fill(x+8,yy-2,x+w-8,yy+26,0xFF303A48);
            String label=selected.isEmpty()?players.getCompound(key).getString("name")+" ("+key.substring(0,8)+")":key;
            int limit=selected.isEmpty()?w-90:w-190;while(font.width(label)>limit&&label.length()>2)label=label.substring(0,label.length()-2)+"…";
            g.drawString(font,label,x+14,yy+6,0xFFFFFFFF,false);
        }
        if(keys.isEmpty())g.drawCenteredString(font,"対象がありません",width/2,height/2,0xFFBBBBBB);
        super.render(g,mx,my,partial);
    }
}
