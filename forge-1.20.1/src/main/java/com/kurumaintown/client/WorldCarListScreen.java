package com.kurumaintown.client;

import com.jdmmc.kurumamod.car.CarId;
import com.jdmmc.kurumamod.car.CarTypes;
import com.kurumaintown.TownNetwork;
import com.kurumaintown.network.WorldCarDeletePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** World-wide vehicle number registry opened with sneak + garage admin item. */
public class WorldCarListScreen extends Screen {
    private final CompoundTag registry;
    private final List<String> numbers = new ArrayList<>();
    private int scroll;

    public WorldCarListScreen(CompoundTag registry) {
        super(Component.literal("ワールド車両登録簿"));
        this.registry = registry;
        numbers.addAll(registry.getAllKeys());
        numbers.sort(String::compareTo);
    }

    private int rows() { return Math.max(1, (height - 80) / 34); }

    @Override protected void init() {
        scroll = Math.min(scroll, Math.max(0, numbers.size() - rows()));
        int w = Math.min(540, width - 20), x = (width - w) / 2;
        for (int i = scroll; i < Math.min(numbers.size(), scroll + rows()); i++) {
            String number = numbers.get(i);
            int yy = 50 + (i - scroll) * 34;
            addRenderableWidget(Button.builder(Component.literal("削除"), b -> confirmDelete(number))
                .bounds(x + w - 67, yy + 3, 56, 20).build());
        }
    }

    private void confirmDelete(String number) {
        Minecraft.getInstance().setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                TownNetwork.CHANNEL.sendToServer(new WorldCarDeletePacket(number));
                registry.remove(number);
                numbers.remove(number);
                scroll = Math.min(scroll, Math.max(0, numbers.size() - rows()));
                Minecraft.getInstance().setScreen(this);
            } else Minecraft.getInstance().setScreen(this);
        }, Component.literal("車両登録を削除しますか？"),
           Component.literal(number + " の登録情報を削除します。"),
           Component.literal("削除"), Component.literal("キャンセル")));
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        int next = Math.max(0, Math.min(Math.max(0, numbers.size() - rows()), scroll - (int)Math.signum(amount)));
        if (next != scroll) { scroll = next; clearWidgets(); init(); }
        return true;
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int w = Math.min(540, width - 20), x = (width - w) / 2;
        g.fill(x, 20, x + w, height - 20, 0xEE19212B);
        g.drawString(font, "ワールド車両登録簿 (" + numbers.size() + "台)", x + 10, 29, 0xFFFFFFFF, false);
        for (int i = scroll; i < Math.min(numbers.size(), scroll + rows()); i++) {
            String number = numbers.get(i);
            CompoundTag car = registry.getCompound(number);
            int yy = 50 + (i - scroll) * 34;
            g.fill(x + 7, yy, x + w - 7, yy + 30, 0xFF303A48);
            String carType = car.getString("cartype");
            try {
                CarId id = CarId.parse(carType);
                if (id != null) carType = CarTypes.get(id).displayName().getString();
            } catch (Exception ignored) { }
            String heading = number + "  " + carType;
            g.drawString(font, font.plainSubstrByWidth(heading, w - 92), x + 12, yy + 4, 0xFFFFFFFF, false);
            String owner = car.getString("ownername");
            if (owner.isBlank()) owner = car.getString("owner");
            g.drawString(font, "Owner: " + font.plainSubstrByWidth(owner, w - 105), x + 12, yy + 17, 0xFFCCCCCC, false);
        }
        if (numbers.isEmpty()) g.drawCenteredString(font, "登録された車両はありません", width / 2, height / 2, 0xFFBBBBBB);
        super.render(g, mx, my, partial);
    }
}
