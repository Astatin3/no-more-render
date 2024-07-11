package com.example.addon.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.resource.ResourceManager;

public class HeadlessGameRenderer extends GameRenderer {

    public HeadlessGameRenderer(MinecraftClient client, HeldItemRenderer heldItemRenderer, ResourceManager resourceManager, BufferBuilderStorage buffers) {
        super(client, heldItemRenderer, resourceManager, buffers);
    }

    @Override
    public void render(RenderTickCounter tickCounter, boolean tick) {
        super.render(tickCounter, tick);
        System.out.println("Haha custom code");
    }
}
