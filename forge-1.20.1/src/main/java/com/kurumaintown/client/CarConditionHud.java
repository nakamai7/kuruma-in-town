package com.kurumaintown.client;

import com.jdmmc.kurumamod.render.KurumaRenderTypes;
import com.jdmmc.kurumamod.entity.CarEntity;
import com.kurumaintown.KurumaInTown;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.Locale;

/** Fuel, speed, durability and armor HUD. Textures are packaged in this addon's assets. */
@Mod.EventBusSubscriber(modid = KurumaInTown.MODID, value = Dist.CLIENT)
public final class CarConditionHud {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int TRACK = 0x704A535E;
    private static final ResourceLocation ENGINE = tex("engine");
    private static final ResourceLocation BODY = tex("body");
    private static final ResourceLocation BATTERY = tex("battery");
    private static final ResourceLocation ARMOR = tex("armor");
    private static final ResourceLocation GASOLINE = tex("gasoline");
    private CarConditionHud() {}

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(KurumaInTown.MODID, "textures/gui/hud/" + name + ".png");
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof CarEntity car)) return;
        TownConditionCache.Data d = TownConditionCache.get(car.getId());
        if (d == null) return;

        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        // Match the reference HUD's screen-aware scale: shrink on compact GUI resolutions,
        // use quarter-step scales to avoid blurry text, and anchor the whole HUD bottom-right.
        float fit = Math.min(sw * 0.30F, sh * 0.34F) / (44.0F * 2.0F);
        float base = Math.max(0.5F, (float)Math.floor(Math.min(1.0F, fit) / 0.25F) * 0.25F);
        float scale = base * 1.15F;
        g.pose().pushPose();
        g.pose().translate(sw, sh, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.pose().translate(-sw, -sh, 0.0F);

        int kmh = Math.max(0, (int)Math.round(Math.abs(car.getRenderSpeed()) * 3.6D));
        int sr = 44;
        // Align the tachometer's right edge to the screen edge and its bottom edge
        // to the upper edge of the durability rings. drawSpeed uses a local 0.8 scale.
        float tachOuter = sr * 0.85F - 1.8F;
        float tachExtent = tachOuter * 0.8F;
        // Keep a right-side inset similar to the durability indicators instead of
        // touching the screen edge.
        int rightMargin = 7;
        int sx = Math.round(sw - rightMargin - tachExtent), sy = Math.round((sh - 39) - tachExtent);
        g.pose().pushPose();
        g.pose().translate(sx, sy, 0.0F);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.pose().translate(-sx, -sy, 0.0F);
        double redline = car.getSpec() == null ? 8000.0D : car.getSpec().redlineRpm();
        double rpm = Math.max(0.0D, car.getRenderRpm());
        float rpmFraction = (float)Math.max(0.0D, Math.min(1.0D, rpm / Math.max(1.0D, redline)));
        drawSpeed(g, mc, sx, sy, sr, kmh, rpmFraction, car.getRenderGear());
        g.pose().popPose();

        float fuel = d.maxFuel() <= 0 ? 0 : clamp(d.fuel() / d.maxFuel());
        int fuelColor = fuel <= .10F ? 0xFFFF4545 : fuel <= .25F ? 0xFFFF9A32 : WHITE;
        // Fuel gauge sits between the speedometer and the condition row; its lower edge
        // aligns with the top of the durability icons while keeping the icon at its prior size.
        // Leave a small horizontal gap between the fuel icon and the tachometer,
        // and lift the fuel display slightly while retaining the condition-row alignment.
        int fuelGap = 6;
        // Move the complete fuel gauge left by about 90% of its ring diameter.
        int fuelLeftShift = Math.round(2.0F * 11.5F * 0.9F);
        int fuelX = Math.round(sx - tachExtent + 8.0F - fuelGap - fuelLeftShift), fuelY = sh - 51;
        drawRing(g, fuelX, fuelY, 11.5F, fuel, fuelColor);
        g.flush();
        drawIcon(g, GASOLINE, fuelX - 8, fuelY - 8, 16);

        int rowY = sh - 37;
        int[] xs = {sw - 101, sw - 73, sw - 45, sw - 17};
        drawCondition(g, mc, xs[0], rowY, ENGINE, d.engine());
        drawCondition(g, mc, xs[1], rowY, BODY, d.body());
        drawCondition(g, mc, xs[2], rowY, BATTERY, d.battery());
        drawArmor(g, mc, xs[3], rowY, d.armor(), d.maxArmor());
        g.flush();
        g.pose().popPose();
    }

    private static void drawSpeed(GuiGraphics g, Minecraft mc, int cx, int cy, int r, int kmh, float rpmFraction, int gear) {
        // The RPM ring diameter is 85% of its original (r-based) size, while the speed readout
        // keeps its existing size. The ring is twice as thick as before and grows clockwise.
        float start = 135.0F, sweep = 270.0F;
        float ringRadius = r * 0.85F;
        float outer = ringRadius - 1.8F;
        float inner = Math.max(0.0F, outer - 4.4F); // doubled from 2.2 px to 4.4 px
        TownGaugePainter.arc(g, cx, cy, inner, outer, start, start + sweep, TRACK);
        if (rpmFraction > 0.0F) {
            int rpmColor = rpmFraction >= 0.95F ? 0xFFFF4545 : rpmFraction >= 0.75F ? 0xFFFF9A32 : WHITE;
            TownGaugePainter.arc(g, cx, cy, inner, outer, start, start + sweep * rpmFraction, rpmColor);
        }
        // Gear is shown above the unchanged speed readout, at about 60% text scale.
        g.flush();
        g.pose().pushPose();
        g.pose().translate(cx, cy - 17.0F, 0.0F);
        g.pose().scale(0.6F, 0.6F, 1.0F);
        g.drawCenteredString(mc.font, gearLabel(gear), 0, 0, 0xFFD8DEE3);
        g.pose().popPose();
        g.drawCenteredString(mc.font, Integer.toString(kmh), cx, cy - 5, WHITE);
        g.drawCenteredString(mc.font, "km/h", cx, cy + 8, 0xFFD8DEE3);
    }

    private static String gearLabel(int gear) {
        if (gear < 0) return "R";
        if (gear == 0) return "N";
        return Integer.toString(gear);
    }

    private static void drawRing(GuiGraphics g, int cx, int cy, float r, float ratio, int color) {
        TownGaugePainter.arc(g, cx, cy, r - 1.8F, r, 150.0F, 390.0F, TRACK);
        if (ratio > 0.0F) TownGaugePainter.arc(g, cx, cy, r - 1.8F, r, 150.0F, 150.0F + 240.0F * ratio, color);
    }

    private static void drawCondition(GuiGraphics g, Minecraft mc, int cx, int y, ResourceLocation icon, float value) {
        float pct = clamp(value / 100.0F);
        int color = color(pct);
        drawIconRing(g, cx, y + 8, 10.0F, pct, color);
        g.flush();
        drawIcon(g, icon, cx - 6.5F, y + 1.5F, 13);
        g.flush();
        String text = String.format(Locale.ROOT, "%.0f%%", pct * 100.0F);
        g.drawCenteredString(mc.font, text, cx, y + 19, color);
    }

    private static void drawArmor(GuiGraphics g, Minecraft mc, int cx, int y, float value, float maxValue) {
        float ratio = maxValue <= 0.0F ? 0.0F : clamp(value / maxValue);
        drawIconRing(g, cx, y + 8, 10.0F, ratio, WHITE);
        g.flush();
        drawIcon(g, ARMOR, cx - 6.5F, y + 1.5F, 13);
        g.flush();
        g.drawCenteredString(mc.font, String.format(Locale.ROOT, "%.1f", Math.max(0, value)), cx, y + 19, WHITE);
    }

    private static void drawIconRing(GuiGraphics g, int cx, int cy, float r, float ratio, int color) {
        TownGaugePainter.arc(g, cx, cy, r - 1.5F, r, -90.0F, 270.0F, TRACK);
        if (ratio > 0.0F) TownGaugePainter.arc(g, cx, cy, r - 1.5F, r, -90.0F, -90.0F + 360.0F * ratio, color);
    }

    /** Mirrors the reference HUD's pose-scale + full texture blit technique. */
    private static void drawIcon(GuiGraphics g, ResourceLocation texture, float x, float y, int size) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(size / 32.0F, size / 32.0F, 1.0F);
        g.blit(texture, 0, 0, 0, 0, 32, 32, 32, 32);
        g.pose().popPose();
    }

    private static float clamp(float v) { return Math.max(0, Math.min(1, v)); }
    private static int color(float p) {
        return p >= .8F ? 0xFF55DD77 : p >= .5F ? 0xFFFFD84A : p >= .2F ? 0xFFFF9A32 : 0xFFFF4545;
    }
}
