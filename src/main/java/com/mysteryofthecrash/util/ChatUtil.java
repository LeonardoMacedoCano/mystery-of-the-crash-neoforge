package com.mysteryofthecrash.util;

import com.mysteryofthecrash.entity.LifeStage;

public final class ChatUtil {

    private ChatUtil() {}

    public static String pickByStage(LifeStage stage, String childMsg, String youngMsg, String adultMsg) {
        return switch (stage) {
            case CHILD -> childMsg;
            case YOUNG -> youngMsg;
            case ADULT -> adultMsg;
        };
    }
}
