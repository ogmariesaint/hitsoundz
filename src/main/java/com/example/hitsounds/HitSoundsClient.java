package com.example.hitsounds;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class HitSoundsClient implements ClientModInitializer {
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        HitSoundsConfig.get();
        HitPredictor.init();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hitsounds.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.create(Identifier.of("hitsounds", "main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new HitSoundsScreen());
                }
            }
        });
    }
}
