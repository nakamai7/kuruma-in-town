package com.kurumaintown.client;

import com.kurumaintown.network.VehicleToolActionPacket;
import com.kurumaintown.TownNetwork;
import com.jdmmc.kurumamod.entity.CarEntity;
import com.jdmmc.kurumamod.physics.CarSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VehicleToolScreen extends Screen {
    private final int mode, id;
    private final float engine, body, battery, armor, fuel, maxFuel;
    private int page = 0;
    private final int[] custom, customMax;
    private final net.minecraft.client.gui.components.EditBox[] customInputs = new net.minecraft.client.gui.components.EditBox[6];
    private final boolean admin;
    private net.minecraft.client.gui.components.EditBox redInput, greenInput, blueInput;
    // Vanilla dye order (creative inventory): white, orange, magenta, light blue, yellow, lime, pink, gray,
    // light gray, cyan, purple, blue, brown, green, red, black.
    private static final int[] DYE_RGB = {0xF9FFFE,0xF9801D,0xC74EBD,0x3AB3DA,0xFED83D,0x80C71F,0xF38BAA,0x474F52,
            0x9D9D97,0x169C9C,0x8932B8,0x3C44AA,0x835432,0x5E7C16,0xB02E26,0x1D1D21};
    private final List<String> lines = new ArrayList<>();

    public VehicleToolScreen(com.kurumaintown.network.VehicleToolPacket p) {
        this(p.mode, p.entityId, p.engine, p.body, p.battery, p.armor, p.fuel, p.maxFuel, p.admin, p.custom, p.customMax);
    }

    private VehicleToolScreen(int mode, int id, float engine, float body, float battery, float armor, float fuel, float maxFuel, boolean admin, int[] custom, int[] customMax) {
        super(Component.literal(mode == 0 ? "Vehicle Tablet" : mode == 1 ? "Repair Kit" : mode == 2 ? "Gas Station" : mode == 4 ? "カスタムツール" : "Color Spray"));
        this.admin = admin; this.mode = mode; this.id = id; this.engine = engine; this.body = body; this.battery = battery;
        this.armor = armor; this.fuel = fuel; this.maxFuel = maxFuel; this.custom=custom.clone();this.customMax=customMax.clone();
    }

    @Override protected void init() {
        clearWidgets();
        lines.clear();
        if (mode == 0) {
            buildStats();
            addRenderableWidget(Button.builder(Component.literal("‹"), b -> changePage(-1))
                    .bounds(width / 2 - 100, height - 38, 35, 20).build());
            addRenderableWidget(Button.builder(Component.literal("›"), b -> changePage(1))
                    .bounds(width / 2 + 65, height - 38, 35, 20).build());
        } else if (mode == 1) {
            int panelW = Math.min(500, width - 20), x = (width - panelW) / 2, y = height / 2 - 70;
            int col = panelW / 3;
            float[] values = {engine, body, battery};
            for (int i = 0; i < 3; i++) {
                final int action = i;
                String label = "修理";
                Button button = Button.builder(Component.literal(label), bt -> startAction(action))
                        .bounds(x + i * col + 4, y + 78, col - 8, 22).build();
                button.active = values[i] < 100.0F;
                addRenderableWidget(button);
            }
        } else if (mode == 4) {
            int w=Math.min(520,width-20), x=(width-w)/2, y=(height-220)/2;
            int col=w/3;
            for(int i=0;i<6;i++){
                final int idx=i;
                int cx=x+(i%3)*col, cy=y+32+(i/3)*90;
                var field=new net.minecraft.client.gui.components.EditBox(font,cx+16,cy+34,col-32,20,Component.literal("custom"));
                field.setMaxLength(3);
                field.setFilter(t->t.matches("[0-9]{0,3}"));
                field.setValue(Integer.toString(custom[i]));
                customInputs[i]=field;
                addRenderableWidget(field);
                addRenderableWidget(Button.builder(Component.literal("適用"), b->applyCustom(idx))
                    .bounds(cx+16,cy+57,col-32,18).build());
            }
        } else if (mode == 3) {
            int sw = 27, gap = 5, sx = (width - (8*sw + 7*gap))/2, sy = height/2 - 70 + 68;
            for (int i = 0; i < 16; i++) {
                final int color = i;
                addRenderableWidget(Button.builder(Component.literal(" "), bt -> startAction(16 + color))
                        .bounds(sx + (i%8)*(sw+gap), sy + (i/8)*29, sw, 24).build());
            }
            if (admin) {
                int fieldY = sy + 71;
                redInput = rgbField(width/2-112, fieldY, "R", 255);
                greenInput = rgbField(width/2-35, fieldY, "G", 255);
                blueInput = rgbField(width/2+42, fieldY, "B", 255);
                addRenderableWidget(Button.builder(Component.literal("適用"), bt -> applyRgb())
                        .bounds(width/2-40, fieldY+25, 80, 20).build());
            }
        } else {
            Button button = Button.builder(Component.literal("給油"), b -> startAction(4))
                    .bounds(width / 2 - 45, height / 2 + 24, 90, 22).build();
            button.active = maxFuel > 0 && fuel < maxFuel;
            addRenderableWidget(button);
        }
    }

    private net.minecraft.client.gui.components.EditBox rgbField(int x, int y, String name, int value) {
        var field = new net.minecraft.client.gui.components.EditBox(font, x, y, 70, 20, Component.literal(name));
        field.setMaxLength(3);
        field.setFilter(text -> text.matches("[0-9]{0,3}"));
        field.setValue(Integer.toString(value));
        addRenderableWidget(field);
        return field;
    }

    private void applyRgb() {
        if (!admin || redInput == null || greenInput == null || blueInput == null) return;
        try {
            int r=Integer.parseInt(redInput.getValue()), g=Integer.parseInt(greenInput.getValue()), b=Integer.parseInt(blueInput.getValue());
            if (r<0 || r>255 || g<0 || g>255 || b<0 || b>255) return;
            TownNetwork.CHANNEL.sendToServer(new com.kurumaintown.network.VehiclePaintRgbPacket(id,r,g,b));
            Minecraft.getInstance().setScreen(null);
        } catch (NumberFormatException ignored) { }
    }

    private void applyCustom(int index) {
        if(index<0 || index>=6 || customInputs[index]==null)return;
        try {
            int value=Integer.parseInt(customInputs[index].getValue());
            if(value<0 || value>customMax[index])return;
            TownNetwork.CHANNEL.sendToServer(new com.kurumaintown.network.VehicleCustomPacket(id,index,value));
            Minecraft.getInstance().setScreen(null);
        } catch(NumberFormatException ignored) { }
    }

    private void startAction(int action) {
        TownNetwork.CHANNEL.sendToServer(new VehicleToolActionPacket(id, action));
        Minecraft.getInstance().setScreen(null);
    }

    private void changePage(int delta) {
        page = Mth.clamp(page + delta, 0, 2);
        init();
    }

    private void buildStats() {
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(id);
        if (!(entity instanceof CarEntity car)) { lines.add("車両性能を取得できません"); return; }
        CarSpec s = car.getSpec();
        if (page == 0) {
            lines.add("最高速度目安: " + String.format(Locale.ROOT, "%.1f km/h", Math.max(0.0, s.estimatedTopSpeed()) * 3.6));
            lines.add("駆動輪のトラクション上限速度: " + String.format(Locale.ROOT, "%.1f km/h", Math.max(0.0, s.tractionLimitedSpeed()) * 3.6));
            lines.add("ピークトルク: " + String.format(Locale.ROOT, "%.1f N·m", s.peakTorque()));
            lines.add("ピークトルク回転数: " + String.format(Locale.ROOT, "%.0f rpm", s.peakTorqueRpm()));
            lines.add("最高出力回転数: " + String.format(Locale.ROOT, "%.0f rpm", s.peakPowerRpm()));
            lines.add("レッドライン: " + String.format(Locale.ROOT, "%.0f rpm", s.redlineRpm()));
            lines.add("前進ギア数: " + s.forwardGears());
            lines.add("質量あたりのピークトルク目安: " + String.format(Locale.ROOT, "%.3f N·m/kg", s.peakTorque() / Math.max(1.0, s.mass())));
        } else if (page == 1) {
            addNumber("最大操舵角", s.maxSteerAngle(), "°");
            addNumber("操舵入力の応答時間", s.steerRateSeconds(), " 秒");
            addNumber("操舵戻り時間", s.steerReturnSeconds(), " 秒");
            addNumber("タイヤ摩擦係数", s.tireFriction(), "");
            addNumber("コーナリング剛性", s.corneringStiffness(), "");
            addNumber("縦方向剛性", s.longitudinalStiffness(), "");
            addNumber("転がり抵抗", s.rollingResistance(), "");
            addNumber("空気抵抗面積", s.effectiveDragArea(), " m²");
            addNumber("変速時間", s.shiftSeconds(), " 秒");
            addNumber("駆動系効率", s.drivetrainEfficiency() * 100.0, "%");
        } else {
            addNumber("ホイールベース", s.wheelBase(), " m");
            addNumber("トレッド幅", s.trackWidth(), " m");
            addNumber("車両質量", s.mass(), " kg");
            addNumber("ホイール半径", s.wheelRadius(), " m");
            addNumber("最低地上高", s.bodyClearance(), " m");
            addNumber("最大サスペンション長", s.suspensionMaxLength(), " m");
            addNumber("静止時サグ", s.staticSag(), " m");
            addNumber("前後重量配分（前）", s.weightBias() * 100.0, "%");
            addNumber("前輪アンチロール剛性", s.frontAntiRollStiffness(), "");
            addNumber("後輪アンチロール剛性", s.rearAntiRollStiffness(), "");
        }
    }

    private void addNumber(String label, double value, String unit) {
        lines.add(label + ": " + String.format(Locale.ROOT, "%.2f", value) + unit);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int w = Math.min(520, width - 20), x = (width - w) / 2, y = (height - 220) / 2;
        g.fill(x, y, x + w, y + 220, 0xF0181D25);
        g.drawCenteredString(font, title, width / 2, y + 12, 0xFFFFFFFF);
        if (mode == 0) {
            String[] titles = {"出力・速度", "ハンドリング・抵抗", "車体寸法・その他"};
            g.drawCenteredString(font, titles[page], width / 2, y + 34, 0xFF8CCBFF);
            for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x + 18, y + 56 + i * 14, 0xFFE8EDF4, false);
            g.drawCenteredString(font, (page + 1) + " / 3", width / 2, height - 51, 0xFFB9C7D8);
        } else if (mode == 1) {
            int col = w / 3;
            String[] names = {"エンジン", "ボディ", "バッテリー"};
            String[] vals = {String.format(Locale.ROOT, "%.0f / 100", engine), String.format(Locale.ROOT, "%.0f / 100", body),
                    String.format(Locale.ROOT, "%.0f / 100", battery)};
            for (int i = 0; i < 3; i++) { int cx = x + i * col; g.drawCenteredString(font, names[i], cx + col / 2, y + 55, 0xFFFFFFFF); g.drawCenteredString(font, vals[i], cx + col / 2, y + 69, 0xFFB9C7D8); }
        } else if (mode == 4) {
            String[] names={"エンジン","ステアリング","サスペンション","タンク","アーマー","ブレーキ"};
            int col=w/3;
            for(int i=0;i<6;i++){
                int cx=x+(i%3)*col+col/2, cy=y+32+(i/3)*90;
                g.drawCenteredString(font,names[i],cx,cy,0xFFFFFFFF);
                g.drawCenteredString(font,custom[i]+" / "+customMax[i],cx,cy+15,0xFFB9C7D8);
            }
        } else if (mode == 3) {
            if (admin) {
                // Labels align vertically with the centers of their input boxes.
                int fieldY = height/2 - 70 + 68 + 71;
                g.drawString(font, "R", width/2-123, fieldY+6, 0xFFFF7777);
                g.drawString(font, "G", width/2-46, fieldY+6, 0xFF77FF77);
                g.drawString(font, "B", width/2+31, fieldY+6, 0xFF7777FF);
            }
        } else {
            g.drawCenteredString(font, "燃料", width / 2, y + 54, 0xFFFFFFFF);
            g.drawCenteredString(font, String.format(Locale.ROOT, "%.1f / %.1f", fuel, maxFuel), width / 2, y + 76, 0xFFB9C7D8);
        }
        super.render(g, mx, my, pt);
        // Draw the swatches AFTER buttons; otherwise vanilla button backgrounds hide them.
        if (mode == 3) {
            int sw=27,gap=5,sx=(width-(8*sw+7*gap))/2,sy=height/2-70+68;
            for (int i=0;i<16;i++) {
                int bx=sx+(i%8)*(sw+gap), by=sy+(i/8)*29;
                g.fill(bx+5,by+5,bx+sw-5,by+19,0xFF000000|DYE_RGB[i]);
            }
        }
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (mode == 0 && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) { changePage(key == GLFW.GLFW_KEY_RIGHT ? 1 : -1); return true; }
        return super.keyPressed(key, scan, mods);
    }
    @Override public boolean isPauseScreen() { return false; }
}
