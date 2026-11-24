package com.kurumaintown.client;

import com.kurumaintown.network.GarageRidePacket;
import com.jdmmc.kurumamod.car.CarTypes;
import com.jdmmc.kurumamod.car.CarId;
import com.kurumaintown.TownNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

/** Scrollable vehicle list for nearby cargarage markers. */
public class GarageScreen extends Screen {
    private final CompoundTag carlist;
    private final double markerX, markerY, markerZ;
    private final float markerYaw, markerPitch;
    private final List<String> numbers = new ArrayList<>();
    private int scroll;
    private int panelX, panelY, panelW, panelH;
    private static final int ROW_H = 58;
    private static final int CONTENT_TOP = 34;

    public GarageScreen(CompoundTag carlist, double x, double y, double z, float yaw, float pitch) {
        super(Component.literal("Car Garage"));
        this.carlist = carlist == null ? new CompoundTag() : carlist.copy();
        this.markerX=x; this.markerY=y; this.markerZ=z; this.markerYaw=yaw; this.markerPitch=pitch;
        this.numbers.addAll(this.carlist.getAllKeys());
        this.numbers.sort(String::compareTo);
    }

    @Override protected void init() {
        // About 60% of the previous 620px panel, while retaining a safe width on small screens.
        panelW = Math.min(372, this.width - 24);
        panelW = Math.max(280, panelW);
        panelW = Math.min(panelW, this.width - 8);
        panelH = Math.min(this.height - 24, 480);
        panelX = (this.width-panelW)/2; panelY=(this.height-panelH)/2;
        scroll = Mth.clamp(scroll, 0, Math.max(0, numbers.size()*ROW_H-(panelH-CONTENT_TOP-8)));
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(panelX,panelY,panelX+panelW,panelY+panelH,0xF0181D25);
        g.drawString(font, "Car Garage", panelX+12,panelY+11,0xFFFFFFFF,false);
        int top=panelY+CONTENT_TOP, bottom=panelY+panelH-8;
        g.enableScissor(panelX+6,top,panelX+panelW-6,bottom);
        for (int i=0;i<numbers.size();i++) {
            String number=numbers.get(i); int y=top+i*ROW_H-scroll;
            if(y+ROW_H<top||y>bottom) continue;
            CompoundTag entry=carlist.getCompound(number);
            CompoundTag vehicle=entry.getCompound("vehicledata");
            CompoundTag town=vehicle.getCompound("town");
            CompoundTag data=entry.contains("cardata",10)?entry.getCompound("cardata"):
                    town.contains("cardata",10)?town.getCompound("cardata"):vehicle.getCompound("cardata");
            int garage=entry.contains("ingarage")?entry.getInt("ingarage"):
                    town.contains("ingarage")?town.getInt("ingarage"):vehicle.getInt("ingarage");
            int left=panelX+8, right=panelX+panelW-8;
            g.fill(left,y,right,y+ROW_H-3,0xFF29323E);
            g.drawString(font, number, panelX+15,y+7,0xFFFFFFFF,false);
            String carId = vehicle.getString("CarId");
            String carType = "Unknown";
            if (!carId.isBlank()) {
                CarId parsed = CarId.parse(carId);
                if (parsed != null) carType = CarTypes.get(parsed).displayName().getString();
            }
            int typeX = panelX + 18 + font.width(number);
            int typeMaxWidth = Math.max(24, panelX + panelW - 94 - typeX);
            if (font.width(carType) > typeMaxWidth) {
                while (carType.length() > 1 && font.width(carType + "…") > typeMaxWidth) carType = carType.substring(0, carType.length()-1);
                carType = carType.isEmpty() ? "…" : carType + "…";
            }
            g.drawString(font, carType, typeX, y+7, 0xFFB9C7D8, false);
            int bx=panelX+panelW-72, by=y+4;
            g.fill(bx,by,bx+58,by+19,garage==1?0xFF279447:0xFF606873);
            g.drawCenteredString(font,"Ride",bx+29,by+5,0xFFFFFFFF);
            int usable=Math.max(1,panelW-24);
            int colW=usable/3;
            drawLevel(g,"Engine",data.getFloat("enginelevel"),panelX+12,y+32,colW);
            drawLevel(g,"Body",data.getFloat("bodylevel"),panelX+12+colW,y+32,colW);
            float maxFuel=Math.max(1.0F,data.getFloat("maxfuellevel"));
            float fuelPct=data.getFloat("fuellevel")*100.0F/maxFuel;
            drawLevel(g,"Fuel",fuelPct,panelX+12+colW*2,y+32,colW);
            if(garage==1 && mouseX>=bx&&mouseX<=bx+58&&mouseY>=by&&mouseY<=by+19) g.fill(bx,by,bx+58,by+19,0x55FFFFFF);
        }
        g.disableScissor();
        if(numbers.isEmpty()) g.drawCenteredString(font,"No saved vehicles",width/2,height/2,0xFFB9C7D8);
        super.render(g,mouseX,mouseY,partialTick);
    }

    /** Compact single-line label + bar + percentage, fitting three columns across the card. */
    private void drawLevel(GuiGraphics g,String label,float value,int x,int y,int colW){
        float pct=Mth.clamp(value,0,100);
        int labelW=font.width(label)+3;
        int pctW=font.width(String.format(java.util.Locale.ROOT,"%.0f%%",pct));
        int barW=Math.max(18, colW-labelW-pctW-12);
        int barX=x+labelW, barY=y+2;
        g.drawString(font,label,x,y+1,0xFFE6EAF0,false);
        g.fill(barX,barY,barX+barW,barY+7,0xFF11151B);
        int color=pct>=80?0xFF54C878:pct>=50?0xFFE7B34E:pct>=20?0xFFE99A42:0xFFE35A52;
        g.fill(barX,barY,barX+(int)(barW*pct/100f),barY+7,color);
        g.drawString(font,String.format(java.util.Locale.ROOT,"%.0f%%",pct),barX+barW+4,y+1,0xFFFFFFFF,false);
    }

    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(button==0){int top=panelY+CONTENT_TOP,bottom=panelY+panelH-8;
            if(mouseY>=top&&mouseY<bottom){int idx=(int)((mouseY-top+scroll)/ROW_H);if(idx>=0&&idx<numbers.size()){
                String num=numbers.get(idx);int y=top+idx*ROW_H-scroll;int bx=panelX+panelW-72,by=y+4;
                if(mouseX>=bx&&mouseX<=bx+58&&mouseY>=by&&mouseY<=by+19){
                    CompoundTag entry=carlist.getCompound(num);
                    CompoundTag vehicle=entry.getCompound("vehicledata");
                    CompoundTag town=vehicle.getCompound("town");
                    int garage=entry.contains("ingarage")?entry.getInt("ingarage"):
                            town.contains("ingarage")?town.getInt("ingarage"):vehicle.getInt("ingarage");
                    if(garage==1){TownNetwork.CHANNEL.sendToServer(new GarageRidePacket(num, markerX, markerY, markerZ));onClose();}
                    return true;
                }
            }} }
        return super.mouseClicked(mouseX,mouseY,button);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double delta){scroll=Mth.clamp(scroll-(int)(delta*ROW_H),0,Math.max(0,numbers.size()*ROW_H-(panelH-CONTENT_TOP-8)));return true;}
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){if(keyCode==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}return super.keyPressed(keyCode,scanCode,modifiers);}
    @Override public boolean isPauseScreen(){return false;}
}
