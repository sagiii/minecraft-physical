package com.example.gpiobridge.renderer;

import com.example.gpiobridge.ModBlocks;
import com.example.gpiobridge.entity.CameraEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Renders CameraEntity as a 1×1×1 block using the "camera" block model
 * (assets/gpio_bridge/models/block/camera.json).
 * Mirrors FallingBlockRenderer's approach but uses our camera block state.
 */
public class CameraEntityRenderer extends EntityRenderer<CameraEntity, CameraEntityRenderState> {

    public CameraEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5f;
    }

    @Override
    public CameraEntityRenderState createRenderState() {
        return new CameraEntityRenderState();
    }

    @Override
    public void extractRenderState(CameraEntity entity, CameraEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.camId = entity.getCamId();

        if (!(entity.level() instanceof ClientLevel clientLevel)) return;

        BlockPos pos = entity.blockPosition();
        var mbrs = state.movingBlockRenderState;
        mbrs.randomSeedPos = pos;
        mbrs.blockPos      = pos;
        mbrs.blockState    = ModBlocks.CAMERA_DISPLAY.defaultBlockState();
        mbrs.biome         = clientLevel.getBiome(pos);
        mbrs.cardinalLighting = clientLevel.cardinalLighting();
        mbrs.lightEngine   = clientLevel.getLightEngine();
    }

    @Override
    public void submit(CameraEntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        // Block model origin is (0,0,0); offset so it centers on the entity's feet
        poseStack.translate(-0.5, 0.0, -0.5);
        collector.submitMovingBlock(poseStack, state.movingBlockRenderState);
        poseStack.popPose();

        // Nametag shows "Camera #id" — calls super which handles nametag rendering
        super.submit(state, poseStack, collector, cameraState);
    }

    @Override
    protected boolean shouldShowName(CameraEntity entity, double distanceSq) {
        // Always show the ID label when in range (8 blocks)
        return distanceSq < 64.0;
    }

    @Override
    protected Component getNameTag(CameraEntity entity) {
        return Component.literal("Camera #" + entity.getCamId());
    }
}
