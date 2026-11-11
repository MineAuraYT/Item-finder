package com.mineaura.itemfinder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ItemFinderClient implements ClientModInitializer {
    private static KeyMapping openItemFinder;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("itemfinder", "controls"));
        openItemFinder = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.itemfinder.open", InputConstants.Type.KEYSYM, InputConstants.KEY_G, category));

        ContainerHighlightRenderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ContainerHighlightRenderer.tick(client);
            while (openItemFinder.consumeClick()) {
                if (client.player != null && client.level != null && client.gui.screen() == null) {
                    client.gui.setScreen(new ItemFinderScreen());
                }
            }
        });
    }
}
