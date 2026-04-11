package com.example.gpiobridge.renderer;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class CameraEntityRenderState extends EntityRenderState {
    public final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
    /** Camera ID, copied for nametag rendering */
    public int camId = 1;
}
