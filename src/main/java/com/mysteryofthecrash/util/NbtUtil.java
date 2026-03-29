package com.mysteryofthecrash.util;

import net.minecraft.nbt.CompoundTag;

public final class NbtUtil {

    private NbtUtil() {}

    public static int getIntOr(CompoundTag tag, String key, int defaultValue) {
        return tag.contains(key) ? tag.getInt(key) : defaultValue;
    }

    public static float getFloatOr(CompoundTag tag, String key, float defaultValue) {
        return tag.contains(key) ? tag.getFloat(key) : defaultValue;
    }

    public static boolean getBoolOr(CompoundTag tag, String key, boolean defaultValue) {
        return tag.contains(key) ? tag.getBoolean(key) : defaultValue;
    }

    public static String getStringOr(CompoundTag tag, String key, String defaultValue) {
        return tag.contains(key) ? tag.getString(key) : defaultValue;
    }

    public static long getLongOr(CompoundTag tag, String key, long defaultValue) {
        return tag.contains(key) ? tag.getLong(key) : defaultValue;
    }
}
