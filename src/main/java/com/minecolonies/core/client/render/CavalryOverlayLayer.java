package com.minecolonies.core.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.model.HorseModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;

import javax.annotation.Nonnull;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;

public class CavalryOverlayLayer extends RenderLayer<Horse, HorseModel<Horse>> 
{

    private static final ResourceLocation OVERLAY_TEX =
        new ResourceLocation(Constants.MOD_ID, "textures/entity/horse/cavalry_overlay_layer.png");

    public CavalryOverlayLayer(RenderLayerParent<Horse, HorseModel<Horse>> parent) 
    {
        super(parent);
    }

    @Override
    public void render(@Nonnull PoseStack pose, @Nonnull MultiBufferSource buffer, int packedLight,
                       @Nonnull Horse horse, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) 
    {
        if (!(horse instanceof CavalryHorseEntity)) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(OVERLAY_TEX));

        float r = 1f, g = 1f, b = 1f, a = .7f;

        this.getParentModel().renderToBuffer(
            pose, vc, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
            r, g, b, a
        );
    }
}
