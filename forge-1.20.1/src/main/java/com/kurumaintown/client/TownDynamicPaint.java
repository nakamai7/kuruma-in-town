package com.kurumaintown.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Generates neutral-base paint textures for arbitrary car models on demand.
 * Source pixels that do not look like the dominant body paint retain their original RGB.
 * This is heuristic; model-specific masks can provide greater accuracy. */
public final class TownDynamicPaint {
    private static final int[] DYES = {0xF9FFFE,0xF9801D,0xC74EBD,0x3AB3DA,0xFED83D,0x80C71F,0xF38BAA,0x474F52,
            0x9D9D97,0x169C9C,0x8932B8,0x3C44AA,0x835432,0x5E7C16,0xB02E26,0x1D1D21};
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private TownDynamicPaint() {}

    public static synchronized ResourceLocation texture(ResourceLocation original, int paint) {
        if (original == null || (paint < 0 && paint > -2) || paint >= DYES.length) return original;
        String key = original + "|" + paint;
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) return cached;
        try {
            Minecraft mc = Minecraft.getInstance();
            Resource resource = mc.getResourceManager().getResource(original).orElse(null);
            if (resource == null) return original;
            try (InputStream stream = resource.open(); NativeImage source = NativeImage.read(stream)) {
                int w = source.getWidth(), h = source.getHeight();
                if (w < 1 || h < 1) return original;
                // Dominant hue of saturated pixels, ignoring small bright highlights.
                double[] bins = new double[36];
                double[] neutralBins = new double[16];
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                    int c = source.getPixelRGBA(x,y);
                    int r=c&255,g=(c>>>8)&255,b=(c>>>16)&255,a=(c>>>24)&255;
                    int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
                    if (a < 160 || max < 35) continue;
                    if (max-min < 35) {
                        // A single-colour/grey body has no meaningful hue. Count neutral
                        // luminance clusters, including dark factory paint.
                        if (max >= 40) neutralBins[Math.min(15, max / 16)] += 1.0;
                        continue;
                    }
                    float[] hsv=java.awt.Color.RGBtoHSB(r,g,b,null);
                    if (hsv[1] > 0.20F) bins[Math.min(35,(int)(hsv[0]*36))] += hsv[1];
                }
                int dominant=0; for(int i=1;i<36;i++) if(bins[i]>bins[dominant]) dominant=i;
                boolean hasHue=bins[dominant]>Math.max(5,w*h*0.002);
                int dominantNeutral=0;
                for (int i=1;i<neutralBins.length;i++)
                    if (neutralBins[i]>neutralBins[dominantNeutral]) dominantNeutral=i;
                boolean hasNeutral=neutralBins[dominantNeutral]>Math.max(8,w*h*0.003);
                // A few coloured details (lights, badges) must not override a large
                // neutral body. Compare the actual coverage, not merely whether any
                // saturated hue exists. Prado has a large grey body and small amber/red details.
                double dominantHueCoverage = 0;
                for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
                    int c=source.getPixelRGBA(x,y);
                    int r=c&255,g=(c>>>8)&255,b=(c>>>16)&255,a=(c>>>24)&255;
                    int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
                    if(a<=80 || max-min<=25) continue;
                    float[] hsv=java.awt.Color.RGBtoHSB(r,g,b,null);
                    int bin=Math.min(35,(int)(hsv[0]*36));
                    int distance=Math.min(Math.abs(bin-dominant),36-Math.abs(bin-dominant));
                    if(hsv[1]>0.16F && distance<=3) dominantHueCoverage++;
                }
                boolean useNeutral = hasNeutral && (!hasHue || neutralBins[dominantNeutral] > dominantHueCoverage * 1.5);
                // Keep neutral paint near the primary brightness cluster so trim,
                // windows and wheels are less likely to be repainted.
                int neutralRadius = 1;
                // Determine a reference brightness for the factory paint. The target
                // colour is applied relative to this reference, not multiplied by the
                // factory RGB, so even dark red can become white.
                int[] brightnessHistogram = new int[256];
                for (int y=0;y<h;y++) for(int x=0;x<w;x++) {
                    int c=source.getPixelRGBA(x,y);
                    int r=c&255,g=(c>>>8)&255,b=(c>>>16)&255,a=(c>>>24)&255;
                    int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
                    float[] hsv=java.awt.Color.RGBtoHSB(r,g,b,null);
                    int bin=Math.min(35,(int)(hsv[0]*36));
                    int distance=Math.min(Math.abs(bin-dominant),36-Math.abs(bin-dominant));
                    boolean neutral=hasNeutral && max>=35 && max-min<40 && Math.abs(Math.min(15,max/16)-dominantNeutral)<=neutralRadius;
                    if (a>80 && (useNeutral ? neutral : hasHue && max-min>25 && hsv[1]>0.16F && distance<=3)) brightnessHistogram[max]++;
                }
                int count=0; for(int n:brightnessHistogram) count+=n;
                int targetCount=(int)(count*0.75);
                int reference=255, accumulated=0;
                for(int i=0;i<256;i++) {accumulated+=brightnessHistogram[i]; if(accumulated>=targetCount) {reference=Math.max(1,i);break;}}
                NativeImage output=new NativeImage(w,h,true);
                int rgb=paint <= -2 ? (-2-paint) & 0xFFFFFF : DYES[paint]; int tr=(rgb>>>16)&255,tg=(rgb>>>8)&255,tb=rgb&255;
                for (int y=0;y<h;y++) for(int x=0;x<w;x++) {
                    int c=source.getPixelRGBA(x,y);
                    int r=c&255,g=(c>>>8)&255,b=(c>>>16)&255,a=(c>>>24)&255;
                    int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
                    float[] hsv=java.awt.Color.RGBtoHSB(r,g,b,null);
                    int bin=Math.min(35,(int)(hsv[0]*36));
                    int distance=Math.min(Math.abs(bin-dominant),36-Math.abs(bin-dominant));
                    // Monochrome factory colours (including black/grey) need a luminance
                    // cluster rather than the saturated-hue detection path.
                    // Limit the neutral selection to the dominant cluster and its
                    // shaded neighbours to avoid recolouring most trim/glass.
                    int neutralBin=Math.min(15,max/16);
                    boolean neutralPaint=hasNeutral && max>=35 && max-min<40
                            && Math.abs(neutralBin-dominantNeutral)<=neutralRadius;
                    boolean isPaintPixel = a>80 && (useNeutral
                            ? neutralPaint
                            : hasHue && max-min>25 && hsv[1]>0.16F && distance<=3);
                    if (isPaintPixel) {
                        // Preserve shadows, but remove source hue. White paint is slightly subdued.
                        float shading=Math.max(0.10F, Math.min(1.0F, max/(float)reference));
                        r=Math.min(255,Math.round(tr*shading));
                        g=Math.min(255,Math.round(tg*shading));
                        b=Math.min(255,Math.round(tb*shading));
                    }
                    output.setPixelRGBA(x,y,(a<<24)|(b<<16)|(g<<8)|r);
                }
                ResourceLocation id=mc.getTextureManager().register("town_paint_"+Integer.toHexString(key.hashCode())+"_"+Integer.toHexString(paint),new DynamicTexture(output));
                CACHE.put(key,id);
                return id;
            }
        } catch (Exception ex) {
            return original;
        }
    }
}
