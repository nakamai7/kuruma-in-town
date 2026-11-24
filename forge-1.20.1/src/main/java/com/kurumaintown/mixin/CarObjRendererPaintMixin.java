package com.kurumaintown.mixin;

import com.jdmmc.kurumamod.render.CarObjRenderer;
import com.jdmmc.kurumamod.entity.CarEntity;
import com.kurumaintown.TownCarData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=CarObjRenderer.class, remap=false)
public abstract class CarObjRendererPaintMixin {
    @Unique private int town$paint = -1;
    @Unique private static final int[] TOWN_DYES = {0xF9FFFE,0xF9801D,0xC74EBD,0x3AB3DA,0xFED83D,0x80C71F,0xF38BAA,0x474F52,
            0x9D9D97,0x169C9C,0x8932B8,0x3C44AA,0x835432,0x5E7C16,0xB02E26,0x1D1D21};
    @Inject(method="renderBody", at=@At("HEAD"))
    private void town$beginPaint(CarEntity car, com.jdmmc.kurumamod.client.model.CarModel model,
        com.jdmmc.kurumamod.physics.CarSpec spec, com.mojang.blaze3d.vertex.PoseStack pose,
        net.minecraft.client.renderer.MultiBufferSource buffers, int light, int lod, CallbackInfo ci) {
        town$paint = ((TownCarData)car).town$getPaintColor();
    }
    @Inject(method="renderBody", at=@At("TAIL"))
    private void town$endPaint(CarEntity car, com.jdmmc.kurumamod.client.model.CarModel model,
        com.jdmmc.kurumamod.physics.CarSpec spec, com.mojang.blaze3d.vertex.PoseStack pose,
        net.minecraft.client.renderer.MultiBufferSource buffers, int light, int lod, CallbackInfo ci) { town$paint = -1; }
    // Recolor the body texture directly even when the vehicle spec has no factory paint option.
    // This uses the independent TownCarData paint value, not a Kuruma paint variant.
    // Replace the actual body texture with a neutral-base recolored texture.
    // Pixels outside the factory paint mask (windows, lights, trim) remain intact.
    @org.spongepowered.asm.mixin.injection.ModifyVariable(
        method="renderBody", at=@At(value="STORE"), ordinal=0)
    private net.minecraft.resources.ResourceLocation town$bodyPaintTexture(
            net.minecraft.resources.ResourceLocation original) {
        if (town$paint == -1 || town$paint >= 16 || original == null) return original;
        return com.kurumaintown.client.TownDynamicPaint.texture(original, town$paint);
    }

    @Inject(method="meshes", at=@At("HEAD"), cancellable=true)
    private void town$disableMeshForPaint(CallbackInfoReturnable<Boolean> cir) {
        if(town$paint!=-1) cir.setReturnValue(false);
    }
}
