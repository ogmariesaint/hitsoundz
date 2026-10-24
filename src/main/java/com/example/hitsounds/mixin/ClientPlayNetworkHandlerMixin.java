package com.example.hitsounds.mixin;

import com.example.hitsounds.HitPredictor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    // Пакет сначала обрабатывается в сетевом потоке, потом в главном. Работаем только в главном.
    @Inject(method = "onPlaySound", at = @At("HEAD"), cancellable = true)
    private void hitsounds$dropDuplicate(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().isOnThread()) return;
        if (HitPredictor.shouldCancelServerSound(packet.getSound())) {
            ci.cancel();
        }
    }
}
