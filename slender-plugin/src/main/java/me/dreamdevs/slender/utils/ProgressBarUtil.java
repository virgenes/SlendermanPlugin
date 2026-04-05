package me.dreamdevs.slender.utils;

import me.dreamdevs.slender.api.utils.ColourUtil;

public class ProgressBarUtil {

    public static String getProgressBar(int current, int max, int totalBars, String symbol, String completedColor, String notCompletedColor) {
        float percent = (float) current / max;
        int progressBars = (int) (totalBars * percent);

        return ColourUtil.colorize(completedColor + symbol.repeat(Math.max(0, progressBars))
                + notCompletedColor + symbol.repeat(Math.max(0, totalBars - progressBars)));
    }
}
