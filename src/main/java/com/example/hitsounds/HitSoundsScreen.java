package com.example.hitsounds;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.IntConsumer;

public class HitSoundsScreen extends Screen {
    // палитра
    private static final int BG = 0xF0141420;
    private static final int BORDER = 0xFF2E2E45;
    private static final int ACCENT = 0xFF8B6CFF;
    private static final int ACCENT_DIM = 0xFF4A3B96;
    private static final int TRACK = 0xFF2A2A3E;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9A9AB5;

    // размеры
    private static final int PANEL_W = 270;
    private static final int PANEL_H = 196;
    private static final int PAD = 16;
    private static final int INNER_W = PANEL_W - PAD * 2;

    private final HitSoundsConfig cfg = HitSoundsConfig.get();
    private int px, py;

    public HitSoundsScreen() {
        super(Text.translatable("hitsounds.title"));
    }

    @Override
    protected void init() {
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        int x = px + PAD;

        addRow(x, py + 54);

        addDrawableChild(ButtonWidget.builder(toggleText("hitsounds.visual", cfg.visual), b -> {
            cfg.visual = !cfg.visual;
            b.setMessage(toggleText("hitsounds.visual", cfg.visual));
        }).dimensions(x, py + 108, INNER_W, 20).build());

        addDrawableChild(ButtonWidget.builder(toggleText("hitsounds.enabled", cfg.enabled), b -> {
            cfg.enabled = !cfg.enabled;
            b.setMessage(toggleText("hitsounds.enabled", cfg.enabled));
        }).dimensions(x, py + 132, INNER_W, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("hitsounds.done"), b -> close())
                .dimensions(x, py + 160, INNER_W, 20).build());
    }

    private static Text toggleText(String key, boolean on) {
        return Text.translatable(key).append(": ")
                .append(Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED));
    }

    private void addRow(int x, int y) {
        int testW = 52;
        PingSlider slider = new PingSlider(x, y, INNER_W - testW - 6, 30, Text.translatable("hitsounds.delay"),
                cfg.delay, v -> cfg.delay = v);
        addDrawableChild(slider);
        addDrawableChild(ButtonWidget.builder(Text.translatable("hitsounds.test"), b -> {
            if (client != null && client.player != null) {
                // тест: звук крита с выбранной задержкой
                int delay = (int) Math.round(slider.getValue() * 100);
                double ex = client.player.getX(), ey = client.player.getY(), ez = client.player.getZ();
                HitPredictor.runAfter(delay, () ->
                        HitPredictor.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, ex, ey, ez));
            }
        }).dimensions(x + INNER_W - testW, y + 5, testW, 20).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        roundedFill(context, px - 1, py - 1, PANEL_W + 2, PANEL_H + 2, BORDER);
        roundedFill(context, px, py, PANEL_W, PANEL_H, BG);

        TextRenderer tr = textRenderer;
        context.drawTextWithShadow(tr, Text.translatable("hitsounds.title").formatted(Formatting.BOLD),
                px + PAD, py + 12, WHITE);
        context.drawTextWithShadow(tr, Text.translatable("hitsounds.subtitle"), px + PAD, py + 25, MUTED);
        context.fill(px + PAD, py + 38, px + PAD + 28, py + 40, ACCENT);

        context.drawTextWithShadow(tr, Text.translatable("hitsounds.hint"), px + PAD, py + 92, MUTED);
    }

    @Override
    public void removed() {
        cfg.save();
    }

    // ---------- рисование ----------

    static void roundedFill(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x + 2, y, x + w - 2, y + h, color);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
        c.fill(x, y + 2, x + w, y + h - 2, color);
    }

    // ---------- слайдер ----------

    private static class PingSlider extends SliderWidget {
        private final Text label;
        private final IntConsumer setter;

        PingSlider(int x, int y, int w, int h, Text label, int initial, IntConsumer setter) {
            super(x, y, w, h, Text.empty(), initial / 100.0);
            this.label = label;
            this.setter = setter;
            updateMessage();
        }

        double getValue() {
            return value;
        }

        @Override
        protected void updateMessage() {
            if (label == null) return; // вызов из конструктора родителя
            setMessage(label.copy().append(": " + Math.round(value * 100) + " ")
                    .append(Text.translatable("hitsounds.ms")));
        }

        @Override
        protected void applyValue() {
            setter.accept((int) Math.round(value * 100));
        }

        @Override
        public void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int x = getX(), y = getY(), w = getWidth();

            c.drawTextWithShadow(tr, label, x, y + 2, WHITE);
            Text val = Text.literal(Math.round(value * 100) + " ").append(Text.translatable("hitsounds.ms"));
            c.drawTextWithShadow(tr, val, x + w - tr.getWidth(val), y + 2, ACCENT);

            // дорожка совпадает с зоной клика ванильного слайдера (отступ 4 px)
            int tx = x + 4, tw = w - 8, ty = y + 17, th = 6;
            roundedFill(c, tx, ty, tw, th, TRACK);
            int filled = (int) Math.round(tw * value);
            if (filled > 0) roundedFill(c, tx, ty, Math.max(filled, 4), th, ACCENT_DIM);

            int kx = tx + filled - 3;
            int kc = (isHovered() || isFocused()) ? WHITE : 0xFFD8D0FF;
            roundedFill(c, kx, ty - 3, 6, 12, kc);
        }
    }
}
