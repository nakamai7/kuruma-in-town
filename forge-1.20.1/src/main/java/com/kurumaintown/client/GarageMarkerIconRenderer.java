package com.kurumaintown.client;

import com.kurumaintown.KurumaInTown;
import com.kurumaintown.network.GarageMarkerSyncPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

/** Draws a camera-facing body icon above server-synchronized cargarage markers. */
@Mod.EventBusSubscriber(modid = KurumaInTown.MODID, value = Dist.CLIENT)
public final class GarageMarkerIconRenderer {
    private static final ResourceLocation BODY_ICON = new ResourceLocation(KurumaInTown.MODID, "textures/gui/hud/body.png");
    private static final double MAX_RENDER_DISTANCE_SQR = 64.0D * 64.0D;
    private static final float HALF_SIZE = 0.27F;
    private static volatile ResourceLocation markerDimension;
    private static volatile List<GarageMarkerSyncPacket.Marker> markers = List.of();

    private GarageMarkerIconRenderer() {}

    public static void setMarkers(ResourceLocation dimension, List<GarageMarkerSyncPacket.Marker> positions) {
        markerDimension = dimension;
        markers = List.copyOf(positions);
    }

    @SubscribeEvent
    public static void renderMarkers(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        ResourceLocation currentDimension = mc.level.dimension().location();
        if (markerDimension == null || !markerDimension.equals(currentDimension) || markers.isEmpty()) return;

        Camera camera = event.getCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, BODY_ICON);

        for (GarageMarkerSyncPacket.Marker marker : markers) {
            double dx = marker.x() - mc.player.getX();
            double dy = marker.y() - mc.player.getY();
            double dz = marker.z() - mc.player.getZ();
            if (dx * dx + dy * dy + dz * dz > MAX_RENDER_DISTANCE_SQR) continue;
            double x = marker.x() - camX;
            double y = marker.y() + 1.5D - camY;
            double z = marker.z() - camZ;

            event.getPoseStack().pushPose();
            event.getPoseStack().translate(x, y, z);
            event.getPoseStack().mulPose(camera.rotation());
            Matrix4f matrix = event.getPoseStack().last().pose();
            int color = marker.job() == 0 ? 255 : 255;
            int green = marker.job() == 0 ? 255 : 45;
            int blue = marker.job() == 0 ? 255 : 45;
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            buffer.vertex(matrix, -HALF_SIZE, -HALF_SIZE, 0.0F).uv(0.0F, 1.0F).color(color,green,blue,255).endVertex();
            buffer.vertex(matrix, -HALF_SIZE,  HALF_SIZE, 0.0F).uv(0.0F, 0.0F).color(color,green,blue,255).endVertex();
            buffer.vertex(matrix,  HALF_SIZE,  HALF_SIZE, 0.0F).uv(1.0F, 0.0F).color(color,green,blue,255).endVertex();
            buffer.vertex(matrix,  HALF_SIZE, -HALF_SIZE, 0.0F).uv(1.0F, 1.0F).color(color,green,blue,255).endVertex();
            BufferUploader.drawWithShader(buffer.end());
            event.getPoseStack().popPose();
        }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
