package com.example.hitsounds;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Только визуал и звук: показывает эффект удара сразу, не дожидаясь ответа сервера.
 * Ничего не отправляет на сервер и не меняет саму атаку.
 */
public final class HitPredictor {
    private enum Kind { CRIT, SWEEP, SPRINT }

    private static final Identifier CRIT_ID = Identifier.ofVanilla("entity.player.attack.crit");
    private static final Identifier SWEEP_ID = Identifier.ofVanilla("entity.player.attack.sweep");
    private static final Identifier KNOCKBACK_ID = Identifier.ofVanilla("entity.player.attack.knockback");
    private static final Identifier STRONG_ID = Identifier.ofVanilla("entity.player.attack.strong");

    /** Предсказанные удары, для которых ждём серверный звук (чтобы его заглушить). */
    private static final Map<Kind, Long> PENDING = new EnumMap<>(Kind.class);
    private static final long PENDING_TTL_MS = 2000;

    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "hitsounds-timer");
        t.setDaemon(true);
        return t;
    });

    /** Сервер при спринт-ударе шлёт ещё и звук "strong", его тоже нужно заглушить. */
    private static long pendingStrong;

    private static long lastHit;
    private static boolean serverSprint;
    private static boolean wasSprinting;

    private HitPredictor() {}

    public static void init() {
        AttackEntityCallback.EVENT.register(HitPredictor::onAttack);
        ClientTickEvents.END_CLIENT_TICK.register(HitPredictor::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null) {
            serverSprint = false;
            wasSprinting = false;
            return;
        }
        boolean sprinting = client.player.isSprinting();
        if (sprinting && !wasSprinting) serverSprint = true;
        if (!sprinting) serverSprint = false;
        wasSprinting = sprinting;
    }

    private static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HitSoundsConfig cfg = HitSoundsConfig.get();
        if (!cfg.enabled || mc.player == null || player != mc.player) return ActionResult.PASS;
        if (!(entity instanceof LivingEntity target) || entity instanceof ArmorStandEntity || !target.isAlive()) {
            return ActionResult.PASS;
        }

        long now = System.currentTimeMillis();
        // слишком частые удары урона не наносят
        if (now - lastHit < 475) return ActionResult.PASS;
        lastHit = now;

        float cooldown = player.getAttackCooldownProgress(0.5f);
        boolean full = cooldown > 0.9f;

        boolean knockback = full && serverSprint;
        boolean crit = full && !knockback
                && player.fallDistance > 0
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !player.hasVehicle();
        boolean sweep = full && !knockback && !crit
                && player.getMainHandStack().isIn(ItemTags.SWORDS)
                && player.isOnGround()
                && !mc.options.forwardKey.isPressed();

        Kind kind;
        if (knockback) {
            kind = Kind.SPRINT;
            serverSprint = false; // сервер сбрасывает спринт после такого удара
        } else if (crit) {
            kind = Kind.CRIT;
        } else if (sweep) {
            kind = Kind.SWEEP;
        } else {
            return ActionResult.PASS; // обычные удары не трогаем
        }

        PENDING.put(kind, now);
        if (kind == Kind.SPRINT) pendingStrong = now;
        final Kind k = kind;
        runAfter(cfg.delay, () -> play(target, k));
        return ActionResult.PASS;
    }

    private static void play(LivingEntity target, Kind kind) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HitSoundsConfig cfg = HitSoundsConfig.get();
        if (mc.world == null || mc.player == null) return;

        // как и сервер: звуки атаки играют в точке, где стоит атакующий (центр, без панорамирования)
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        if (kind == Kind.CRIT) {
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, x, y, z);
        } else if (kind == Kind.SWEEP) {
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, x, y, z);
        } else {
            // спринт-удар в ваниле = knockback + strong
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, x, y, z);
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, x, y, z);
        }

        if (cfg.visual && target.isAlive()) {
            target.hurtTime = 10;
            target.maxHurtTime = 10;
        }
    }

    public static void playSound(SoundEvent sound, double x, double y, double z) {
        MinecraftClient.getInstance().getSoundManager().play(new PositionedSoundInstance(
                sound, SoundCategory.PLAYERS, 1.0f, 1.0f, Random.create(), x, y, z));
    }

    /** Выполняет действие в главном потоке через delayMs миллисекунд (0 = сразу). */
    public static void runAfter(int delayMs, Runnable action) {
        if (delayMs <= 0) {
            action.run();
        } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            TIMER.schedule(() -> mc.execute(action), delayMs, TimeUnit.MILLISECONDS);
        }
    }

    /** Вызывается из миксина: true, если серверный звук нужно заглушить как дубль. */
    public static boolean shouldCancelServerSound(RegistryEntry<SoundEvent> sound) {
        if (!HitSoundsConfig.get().enabled) return false;

        if (sound.matchesId(STRONG_ID)) {
            boolean dup = pendingStrong != 0 && System.currentTimeMillis() - pendingStrong <= PENDING_TTL_MS;
            if (dup) pendingStrong = 0;
            return dup;
        }

        Kind kind = null;
        if (sound.matchesId(CRIT_ID)) kind = Kind.CRIT;
        else if (sound.matchesId(SWEEP_ID)) kind = Kind.SWEEP;
        else if (sound.matchesId(KNOCKBACK_ID)) kind = Kind.SPRINT;
        if (kind == null) return false;

        Long t = PENDING.remove(kind);
        return t != null && System.currentTimeMillis() - t <= PENDING_TTL_MS;
    }
}
