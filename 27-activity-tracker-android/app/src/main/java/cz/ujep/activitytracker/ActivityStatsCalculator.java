package cz.ujep.activitytracker;

import java.util.List;
import java.util.Locale;

public class ActivityStatsCalculator {
    private ActivityStatsCalculator() {
    }

    public static ActivityStats summarize(long startedAt, long endedAt, List<ActivitySample> samples) {
        int totalSteps = 0;
        double intensitySum = 0.0;
        for (ActivitySample sample : samples) {
            totalSteps += sample.steps;
            intensitySum += sample.intensity;
        }
        int count = samples.size();
        double averageIntensity = count == 0 ? 0.0 : intensitySum / count;
        long duration = Math.max(0L, endedAt - startedAt);
        return new ActivityStats(duration, totalSteps, averageIntensity, count);
    }

    public static String formatDuration(long durationMillis) {
        long totalSeconds = Math.max(0L, durationMillis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String intensityLabel(double intensity) {
        if (intensity < 0.8) {
            return "Klidova";
        }
        if (intensity < 2.0) {
            return "Lehka";
        }
        if (intensity < 4.0) {
            return "Stredni";
        }
        return "Vysoka";
    }

    public static String formatIntensity(double intensity) {
        return String.format(Locale.US, "%s (%.1f)", intensityLabel(intensity), intensity);
    }
}
