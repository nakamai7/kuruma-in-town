package com.kurumaintown.client;

import com.jdmmc.kurumamod.render.KurumaRenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

/** Smooth ring segments rendered as quads, following Kuruma's GaugePainter approach. */
final class TownGaugePainter {
    private static final float ARC_STEP_DEGREES = 3.0F;
    private TownGaugePainter() {}

    static void arc(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius,
                    float fromDegrees, float toDegrees, int color) {
        if (toDegrees <= fromDegrees || outerRadius <= innerRadius) return;
        int steps = Math.max(1, (int)Math.ceil((toDegrees - fromDegrees) / ARC_STEP_DEGREES));
        float step = (toDegrees - fromDegrees) / steps;
        VertexConsumer consumer = graphics.bufferSource().getBuffer(KurumaRenderTypes.GUI_SHAPE);
        Matrix4f matrix = graphics.pose().last().pose();
        for (int i = 0; i < steps; i++) {
            float a0 = fromDegrees + step * i;
            float a1 = a0 + step;
            float c0 = (float)Math.cos(Math.toRadians(a0)), s0 = (float)Math.sin(Math.toRadians(a0));
            float c1 = (float)Math.cos(Math.toRadians(a1)), s1 = (float)Math.sin(Math.toRadians(a1));
            quad(consumer, matrix, color,
                    cx + c0 * innerRadius, cy + s0 * innerRadius,
                    cx + c1 * innerRadius, cy + s1 * innerRadius,
                    cx + c1 * outerRadius, cy + s1 * outerRadius,
                    cx + c0 * outerRadius, cy + s0 * outerRadius);
        }
    }

    static void radialBar(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius,
                          float degrees, float width, int color) {
        float c = (float)Math.cos(Math.toRadians(degrees)), s = (float)Math.sin(Math.toRadians(degrees));
        float nx = -s * width * 0.5F, ny = c * width * 0.5F;
        float ix = cx + c * innerRadius, iy = cy + s * innerRadius;
        float ox = cx + c * outerRadius, oy = cy + s * outerRadius;
        VertexConsumer consumer = graphics.bufferSource().getBuffer(KurumaRenderTypes.GUI_SHAPE);
        Matrix4f matrix = graphics.pose().last().pose();
        quad(consumer, matrix, color, ix - nx, iy - ny, ix + nx, iy + ny, ox + nx, oy + ny, ox - nx, oy - ny);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, int color,
                             float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
        int a = color >>> 24, r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        consumer.vertex(matrix, x0, y0, 0.0F).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, x1, y1, 0.0F).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, x2, y2, 0.0F).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, x3, y3, 0.0F).color(r, g, b, a).endVertex();
    }
}
