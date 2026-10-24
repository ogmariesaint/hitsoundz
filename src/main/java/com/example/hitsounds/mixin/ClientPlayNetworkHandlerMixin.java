package com.example.hitsounds.mixin;

import com.example.hitsounds.HitPredictor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
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

    // Некоторые серверы привязывают звук удара к сущности. require = 0: если метод не найден, просто пропускаем.
    @Inject(method = "onPlaySoundFromEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void hitsounds$dropDuplicateEntity(PlaySoundFromEntityS2CPacket packet, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().isOnThread()) return;
        if (HitPredictor.shouldCancelServerSound(packet.getSound())) {
            ci.cancel();
        }
    }
}
