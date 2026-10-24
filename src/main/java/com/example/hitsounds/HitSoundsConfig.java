package com.example.hitsounds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HitSoundsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("hitsounds.json");
    private static HitSoundsConfig instance;

    public boolean enabled = true;
    public int crit = 50;    // 0..100
    public int sweep = 50;
    public int sprint = 50;

    public static HitSoundsConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static HitSoundsConfig load() {
        try {
            if (Files.exists(PATH)) {
                HitSoundsConfig c = GSON.fromJson(Files.readString(PATH), HitSoundsConfig.class);
                if (c != null) return c;
            }
        } catch (Exception ignored) {}
        return new HitSoundsConfig();
    }

    public void save() {
        try {
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException ignored) {}
    }

    /** 0..100 -> pitch 0.5..2.0 (50 = ванильный 1.0) */
    public static float toPitch(int value) {
        int v = Math.max(0, Math.min(100, value));
        return v <= 50 ? 0.5f + v / 100f : 1.0f + (v - 50) / 50f;
    }
}
